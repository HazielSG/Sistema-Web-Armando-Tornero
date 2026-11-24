/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : ReporteService.java
 * Descripcion   : Servicio de negocio de los indicadores del panel principal y de los reportes de ventas, inventario y auditoria, con exportacion a CSV (RF-20, RF-21, RF-22, CU-06 a CU-09).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.bodega.dto.ResumenDashboard;
import pe.edu.upn.bodega.entity.*;
import pe.edu.upn.bodega.repository.AuditoriaRepository;
import pe.edu.upn.bodega.repository.ProductoRepository;
import pe.edu.upn.bodega.repository.VentaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReporteService {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    public ReporteService(VentaRepository ventaRepository,
                          ProductoRepository productoRepository,
                          AuditoriaRepository auditoriaRepository,
                          UsuarioService usuarioService,
                          AuditoriaService auditoriaService) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioService = usuarioService;
        this.auditoriaService = auditoriaService;
    }

    // -----------------------------------------------------------------
    // Panel principal
    // -----------------------------------------------------------------
    @Transactional(readOnly = true)
    public ResumenDashboard resumen() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioDia = hoy.atStartOfDay();
        LocalDateTime finDia = hoy.atTime(LocalTime.MAX);
        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();

        ResumenDashboard resumen = new ResumenDashboard();
        resumen.setProductosRegistrados(productoRepository.countByActivoTrue());
        resumen.setProductosStockBajo(productoRepository.contarStockBajo());
        resumen.setVentasDelDia(ventaRepository.contarVentas(inicioDia, finDia));
        resumen.setTotalVendidoDia(ventaRepository.totalVendido(inicioDia, finDia));
        resumen.setTotalVendidoMes(ventaRepository.totalVendido(inicioMes, finDia));
        resumen.setUsuariosActivos(usuarioService.contarActivos());
        resumen.setOperacionesAuditadasHoy(
                auditoriaRepository.resumenPorAccion(inicioDia, finDia).stream()
                        .mapToLong(fila -> (Long) fila[1])
                        .sum());
        return resumen;
    }

    // -----------------------------------------------------------------
    // Reportes
    // -----------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> reporteVentas(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = (desde == null ? LocalDate.now().minusDays(30) : desde).atStartOfDay();
        LocalDateTime fin = (hasta == null ? LocalDate.now() : hasta).atTime(LocalTime.MAX);

        List<Map<String, Object>> filas = new ArrayList<>();
        for (Venta venta : ventaRepository.listarPorRango(inicio, fin)) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("comprobante", venta.getNumeroComprobante());
            fila.put("fecha", venta.getFecha().format(FORMATO_FECHA));
            fila.put("cajero", venta.getUsuario().getNombreCompleto());
            fila.put("cliente", venta.getClienteNombre());
            fila.put("productos", venta.getDetalles().size());
            fila.put("subtotal", venta.getSubtotal());
            fila.put("igv", venta.getIgv());
            fila.put("total", venta.getTotal());
            fila.put("metodoPago", venta.getMetodoPago().name());
            fila.put("estado", venta.getEstado().name());
            filas.add(fila);
        }
        return filas;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> productosMasVendidos(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = (desde == null ? LocalDate.now().minusDays(30) : desde).atStartOfDay();
        LocalDateTime fin = (hasta == null ? LocalDate.now() : hasta).atTime(LocalTime.MAX);

        List<Map<String, Object>> filas = new ArrayList<>();
        for (Object[] registro : ventaRepository.productosMasVendidos(inicio, fin)) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("producto", registro[0]);
            fila.put("unidades", registro[1]);
            fila.put("importe", registro[2]);
            filas.add(fila);
        }
        return filas;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> reporteInventario() {
        List<Map<String, Object>> filas = new ArrayList<>();
        for (Producto producto : productoRepository.findByActivoTrueOrderByNombreAsc()) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("codigo", producto.getCodigo());
            fila.put("producto", producto.getNombre());
            fila.put("categoria", producto.getCategoria().getNombre());
            fila.put("unidad", producto.getUnidadMedida());
            fila.put("precioVenta", producto.getPrecioVenta());
            fila.put("stockActual", producto.getStockActual());
            fila.put("stockMinimo", producto.getStockMinimo());
            fila.put("alerta", producto.isStockBajo() ? "STOCK BAJO" : "NORMAL");
            filas.add(fila);
        }
        return filas;
    }

    // -----------------------------------------------------------------
    // Exportacion a CSV (RF-22)
    // -----------------------------------------------------------------
    @Transactional
    public String exportarVentasCsv(LocalDate desde, LocalDate hasta) {
        List<Map<String, Object>> filas = reporteVentas(desde, hasta);
        auditoriaService.registrar(AccionAuditoria.EXPORTAR, "REPORTE_VENTAS", null,
                "Se exporto el reporte de ventas del " + desde + " al " + hasta
                        + " (" + filas.size() + " registro(s))", null, null);
        return construirCsv(filas, "No hay ventas en el rango indicado");
    }

    @Transactional
    public String exportarInventarioCsv() {
        List<Map<String, Object>> filas = reporteInventario();
        auditoriaService.registrar(AccionAuditoria.EXPORTAR, "REPORTE_INVENTARIO", null,
                "Se exporto el reporte de inventario (" + filas.size() + " producto(s))", null, null);
        return construirCsv(filas, "No hay productos registrados");
    }

    @Transactional
    public String exportarAuditoriaCsv(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = (desde == null ? LocalDate.now().minusDays(30) : desde).atStartOfDay();
        LocalDateTime fin = (hasta == null ? LocalDate.now() : hasta).atTime(LocalTime.MAX);

        List<Map<String, Object>> filas = new ArrayList<>();
        for (Auditoria registro : auditoriaRepository.findAll()) {
            if (registro.getFechaHora().isBefore(inicio) || registro.getFechaHora().isAfter(fin)) {
                continue;
            }
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", registro.getId());
            fila.put("fechaHora", registro.getFechaHora().format(FORMATO_FECHA));
            fila.put("usuario", registro.getUsername());
            fila.put("rol", registro.getRol());
            fila.put("accion", registro.getAccion().name());
            fila.put("entidad", registro.getEntidad());
            fila.put("entidadId", registro.getEntidadId());
            fila.put("descripcion", registro.getDescripcion());
            fila.put("valorAnterior", registro.getValorAnterior());
            fila.put("valorNuevo", registro.getValorNuevo());
            fila.put("ip", registro.getDireccionIp());
            fila.put("resultado", registro.getResultado().name());
            filas.add(fila);
        }

        auditoriaService.registrar(AccionAuditoria.EXPORTAR, "AUDITORIA", null,
                "Se exporto la bitacora de auditoria del " + desde + " al " + hasta
                        + " (" + filas.size() + " registro(s))", null, null);

        return construirCsv(filas, "No hay registros de auditoria en el rango indicado");
    }

    private String construirCsv(List<Map<String, Object>> filas, String mensajeVacio) {
        StringBuilder csv = new StringBuilder();
        if (filas.isEmpty()) {
            return mensajeVacio;
        }
        csv.append(String.join(";", filas.get(0).keySet())).append("\n");
        for (Map<String, Object> fila : filas) {
            List<String> valores = new ArrayList<>();
            for (Object valor : fila.values()) {
                valores.add(escapar(valor));
            }
            csv.append(String.join(";", valores)).append("\n");
        }
        return csv.toString();
    }

    private static String escapar(Object valor) {
        if (valor == null) {
            return "";
        }
        String texto = String.valueOf(valor).replace("\"", "'").replace("\n", " ");
        if (texto.contains(";")) {
            return "\"" + texto + "\"";
        }
        return texto;
    }
}
