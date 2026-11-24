/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : InventarioService.java
 * Descripcion   : Servicio de negocio del control de inventario: entradas, salidas, ajustes, consulta de existencias y alertas de stock bajo (RF-09 a RF-12, CU-12 a CU-14).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.bodega.dto.MovimientoRequest;
import pe.edu.upn.bodega.entity.*;
import pe.edu.upn.bodega.repository.MovimientoInventarioRepository;
import pe.edu.upn.bodega.repository.ProductoRepository;
import pe.edu.upn.bodega.repository.UsuarioRepository;
import pe.edu.upn.bodega.util.NegocioException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class InventarioService {

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public InventarioService(ProductoRepository productoRepository,
                             MovimientoInventarioRepository movimientoRepository,
                             UsuarioRepository usuarioRepository,
                             AuditoriaService auditoriaService) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    /**
     * RF-09 y RF-10: registra el movimiento, actualiza el stock del producto y
     * deja constancia del usuario responsable. Entrada, salida y ajuste se
     * procesan en una sola transaccion para mantener la consistencia (RNF-03).
     */
    @Transactional
    public MovimientoInventario registrarMovimiento(MovimientoRequest peticion) {
        auditoriaService.marcarUsuarioBd();

        Producto producto = productoRepository.findById(peticion.getIdProducto())
                .orElseThrow(() -> new NegocioException("El producto indicado no existe."));

        if (Boolean.FALSE.equals(producto.getActivo())) {
            throw new NegocioException("No se pueden registrar movimientos de un producto desactivado.");
        }

        int stockAnterior = producto.getStockActual();
        int stockResultante;

        switch (peticion.getTipo()) {
            case ENTRADA -> stockResultante = stockAnterior + peticion.getCantidad();
            case SALIDA -> {
                if (peticion.getCantidad() > stockAnterior) {
                    throw new NegocioException("La salida no puede ser mayor que el stock disponible ("
                            + stockAnterior + " " + producto.getUnidadMedida().toLowerCase() + ").");
                }
                stockResultante = stockAnterior - peticion.getCantidad();
            }
            case AJUSTE -> stockResultante = peticion.getCantidad();
            default -> throw new NegocioException("Tipo de movimiento no reconocido.");
        }

        producto.setStockActual(stockResultante);
        productoRepository.save(producto);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setTipo(peticion.getTipo());
        movimiento.setCantidad(peticion.getCantidad());
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockResultante(stockResultante);
        movimiento.setMotivo(peticion.getMotivo());
        movimiento.setReferencia(peticion.getReferencia());
        movimiento.setUsuario(usuarioAutenticado());
        movimiento.setFecha(LocalDateTime.now());

        MovimientoInventario guardado = movimientoRepository.save(movimiento);

        auditoriaService.registrar(AccionAuditoria.CREAR, "MOVIMIENTO_INVENTARIO", guardado.getId(),
                "Movimiento " + guardado.getTipo() + " de " + guardado.getCantidad()
                        + " unidad(es) del producto " + producto.getCodigo()
                        + ". Motivo: " + guardado.getMotivo(),
                "{\"stock\":" + stockAnterior + "}",
                "{\"stock\":" + stockResultante + "}");

        return guardado;
    }

    /** Descuento de stock utilizado por el registro de ventas (RF-17). */
    @Transactional
    public void descontarPorVenta(Producto producto, int cantidad, String numeroComprobante) {
        int stockAnterior = producto.getStockActual();
        if (cantidad > stockAnterior) {
            throw new NegocioException("Stock insuficiente para el producto " + producto.getNombre()
                    + ". Disponible: " + stockAnterior + ", solicitado: " + cantidad + ".");
        }
        int stockResultante = stockAnterior - cantidad;
        producto.setStockActual(stockResultante);
        productoRepository.save(producto);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setTipo(TipoMovimiento.SALIDA);
        movimiento.setCantidad(cantidad);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockResultante(stockResultante);
        movimiento.setMotivo("Salida por venta " + numeroComprobante);
        movimiento.setReferencia(numeroComprobante);
        movimiento.setUsuario(usuarioAutenticado());
        movimiento.setFecha(LocalDateTime.now());
        movimientoRepository.save(movimiento);
    }

    /** Reposicion de stock al anular una venta. */
    @Transactional
    public void reponerPorAnulacion(Producto producto, int cantidad, String numeroComprobante) {
        int stockAnterior = producto.getStockActual();
        int stockResultante = stockAnterior + cantidad;
        producto.setStockActual(stockResultante);
        productoRepository.save(producto);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setTipo(TipoMovimiento.ENTRADA);
        movimiento.setCantidad(cantidad);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockResultante(stockResultante);
        movimiento.setMotivo("Reposicion por anulacion de la venta " + numeroComprobante);
        movimiento.setReferencia(numeroComprobante);
        movimiento.setUsuario(usuarioAutenticado());
        movimiento.setFecha(LocalDateTime.now());
        movimientoRepository.save(movimiento);
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventario> ultimosMovimientos() {
        return movimientoRepository.findTop50ByOrderByFechaDesc();
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventario> movimientosPorProducto(Long idProducto) {
        return movimientoRepository.findByProductoIdOrderByFechaDesc(idProducto);
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventario> movimientosPorRango(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = (desde == null ? LocalDate.now().minusDays(30) : desde).atStartOfDay();
        LocalDateTime fin = (hasta == null ? LocalDate.now() : hasta).atTime(LocalTime.MAX);
        return movimientoRepository.listarPorRango(inicio, fin);
    }

    private Usuario usuarioAutenticado() {
        String username = auditoriaService.usernameActual();
        return usuarioRepository.findByUsername(username == null ? "" : username)
                .orElseThrow(() -> new NegocioException("No se pudo identificar al usuario de la sesion."));
    }
}
