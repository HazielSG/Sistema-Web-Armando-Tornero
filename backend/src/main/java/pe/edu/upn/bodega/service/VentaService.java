/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : VentaService.java
 * Descripcion   : Servicio de negocio del registro de ventas. Valida el stock, calcula los importes, guarda el comprobante y actualiza el inventario dentro de una sola transaccion (RF-13 a RF-18, RNF-03).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.bodega.dto.ItemVentaRequest;
import pe.edu.upn.bodega.dto.VentaRequest;
import pe.edu.upn.bodega.entity.*;
import pe.edu.upn.bodega.repository.*;
import pe.edu.upn.bodega.util.NegocioException;
import pe.edu.upn.bodega.util.RecursoNoEncontradoException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class VentaService {

    /** Tasa de IGV vigente en el Peru. */
    private static final BigDecimal FACTOR_IGV = new BigDecimal("1.18");
    private static final BigDecimal TASA_IGV = new BigDecimal("0.18");

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CierreCajaRepository cierreCajaRepository;
    private final InventarioService inventarioService;
    private final AuditoriaService auditoriaService;

    public VentaService(VentaRepository ventaRepository,
                        ProductoRepository productoRepository,
                        UsuarioRepository usuarioRepository,
                        CierreCajaRepository cierreCajaRepository,
                        InventarioService inventarioService,
                        AuditoriaService auditoriaService) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.cierreCajaRepository = cierreCajaRepository;
        this.inventarioService = inventarioService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public Venta registrar(VentaRequest peticion) {
        auditoriaService.marcarUsuarioBd();

        Usuario cajero = usuarioAutenticado();

        Venta venta = new Venta();
        venta.setUsuario(cajero);
        venta.setFecha(LocalDateTime.now());
        venta.setClienteNombre(peticion.getClienteNombre() == null || peticion.getClienteNombre().isBlank()
                ? "Cliente varios" : peticion.getClienteNombre().trim());
        venta.setClienteDocumento(peticion.getClienteDocumento());
        venta.setMetodoPago(peticion.getMetodoPago());
        venta.setEstadoPago(peticion.getEstadoPago() == null ? EstadoPago.PAGADO : peticion.getEstadoPago());
        venta.setEstado(EstadoVenta.EMITIDA);
        venta.setNumeroComprobante(generarNumeroComprobante());

        // Caja abierta del cajero: la venta se asocia al turno en curso (RF-19).
        cierreCajaRepository
                .findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(cajero.getId(), EstadoCaja.ABIERTA)
                .ifPresent(venta::setCierreCaja);

        BigDecimal total = BigDecimal.ZERO;

        for (ItemVentaRequest item : peticion.getItems()) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new NegocioException("Uno de los productos seleccionados no existe."));

            if (Boolean.FALSE.equals(producto.getActivo())) {
                throw new NegocioException("El producto " + producto.getNombre() + " esta desactivado.");
            }

            // RF-14: validacion de stock antes de confirmar la venta.
            if (item.getCantidad() > producto.getStockActual()) {
                throw new NegocioException("Stock insuficiente para " + producto.getNombre()
                        + ". Disponible: " + producto.getStockActual()
                        + ", solicitado: " + item.getCantidad() + ".");
            }

            BigDecimal subtotalLinea = producto.getPrecioVenta()
                    .multiply(BigDecimal.valueOf(item.getCantidad()))
                    .setScale(2, RoundingMode.HALF_UP);

            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(producto.getPrecioVenta());
            detalle.setSubtotal(subtotalLinea);
            venta.agregarDetalle(detalle);

            total = total.add(subtotalLinea);
        }

        // RF-15: los precios de la bodega incluyen IGV, por lo que este se desagrega.
        BigDecimal valorVenta = total.divide(FACTOR_IGV, 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(valorVenta).setScale(2, RoundingMode.HALF_UP);

        venta.setSubtotal(valorVenta);
        venta.setIgv(igv);
        venta.setTotal(total.setScale(2, RoundingMode.HALF_UP));

        if (venta.getMetodoPago() == MetodoPago.EFECTIVO && peticion.getMontoRecibido() != null) {
            if (peticion.getMontoRecibido().compareTo(venta.getTotal()) < 0) {
                throw new NegocioException("El monto recibido es menor que el total de la venta.");
            }
            venta.setMontoRecibido(peticion.getMontoRecibido());
            venta.setVuelto(peticion.getMontoRecibido().subtract(venta.getTotal())
                    .setScale(2, RoundingMode.HALF_UP));
        }

        Venta guardada = ventaRepository.save(venta);

        // RF-17: actualizacion del inventario en la misma transaccion.
        for (DetalleVenta detalle : guardada.getDetalles()) {
            inventarioService.descontarPorVenta(detalle.getProducto(),
                                                detalle.getCantidad(),
                                                guardada.getNumeroComprobante());
        }

        auditoriaService.registrar(AccionAuditoria.CREAR, "VENTA", guardada.getId(),
                "Se registro la venta " + guardada.getNumeroComprobante()
                        + " por S/ " + guardada.getTotal()
                        + " (" + guardada.getDetalles().size() + " producto(s))",
                null,
                "{\"comprobante\":\"" + guardada.getNumeroComprobante()
                        + "\",\"total\":" + guardada.getTotal()
                        + ",\"metodo_pago\":\"" + guardada.getMetodoPago() + "\"}");

        return guardada;
    }

    @Transactional(readOnly = true)
    public Venta obtener(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro la venta solicitada."));
    }

    @Transactional(readOnly = true)
    public Venta obtenerPorComprobante(String numero) {
        return ventaRepository.findByNumeroComprobante(numero)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el comprobante " + numero + "."));
    }

    @Transactional(readOnly = true)
    public List<Venta> ultimasVentas() {
        return ventaRepository.findTop50ByOrderByFechaDesc();
    }

    /** RF-20: historial de ventas filtrado por rango de fechas. */
    @Transactional(readOnly = true)
    public List<Venta> listarPorRango(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = (desde == null ? LocalDate.now().minusDays(30) : desde).atStartOfDay();
        LocalDateTime fin = (hasta == null ? LocalDate.now() : hasta).atTime(LocalTime.MAX);
        return ventaRepository.listarPorRango(inicio, fin);
    }

    /** Anulacion del comprobante con reposicion de stock y registro en auditoria. */
    @Transactional
    public Venta anular(Long id, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new NegocioException("Debe indicar el motivo de la anulacion.");
        }
        auditoriaService.marcarUsuarioBd();

        Venta venta = obtener(id);
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new NegocioException("La venta ya se encuentra anulada.");
        }

        venta.setEstado(EstadoVenta.ANULADA);
        Venta guardada = ventaRepository.save(venta);

        for (DetalleVenta detalle : guardada.getDetalles()) {
            inventarioService.reponerPorAnulacion(detalle.getProducto(),
                                                  detalle.getCantidad(),
                                                  guardada.getNumeroComprobante());
        }

        auditoriaService.registrar(AccionAuditoria.ANULAR, "VENTA", guardada.getId(),
                "Se anulo la venta " + guardada.getNumeroComprobante() + ". Motivo: " + motivo,
                "{\"estado\":\"EMITIDA\"}", "{\"estado\":\"ANULADA\"}");

        return guardada;
    }

    /** RF-18: deja constancia de la emision del comprobante interno. */
    @Transactional
    public Venta emitirComprobante(Long id) {
        Venta venta = obtener(id);
        auditoriaService.registrar(AccionAuditoria.EXPORTAR, "VENTA", venta.getId(),
                "Se genero el comprobante interno " + venta.getNumeroComprobante(), null, null);
        return venta;
    }

    private String generarNumeroComprobante() {
        String prefijo = "B001-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        long correlativo = ventaRepository.contarTodas() + 1;
        String numero = String.format("%s-%04d", prefijo, correlativo);
        while (ventaRepository.findByNumeroComprobante(numero).isPresent()) {
            correlativo++;
            numero = String.format("%s-%04d", prefijo, correlativo);
        }
        return numero;
    }

    private Usuario usuarioAutenticado() {
        String username = auditoriaService.usernameActual();
        return usuarioRepository.findByUsername(username == null ? "" : username)
                .orElseThrow(() -> new NegocioException("No se pudo identificar al cajero de la sesion."));
    }
}
