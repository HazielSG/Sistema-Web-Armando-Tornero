/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : ReporteController.java
 * Descripcion   : Controlador REST de los reportes de ventas, inventario y auditoria, con exportacion a CSV (RF-21, RF-22, CU-08, CU-09).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upn.bodega.dto.ApiResponse;
import pe.edu.upn.bodega.service.ReporteService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/ventas")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> ventas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        List<Map<String, Object>> filas = reporteService.reporteVentas(desde, hasta);
        return ResponseEntity.ok(ApiResponse.exito(filas.size() + " venta(s) en el periodo.", filas));
    }

    @GetMapping("/productos-mas-vendidos")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> masVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(ApiResponse.exito("Productos mas vendidos del periodo.",
                reporteService.productosMasVendidos(desde, hasta)));
    }

    @GetMapping("/inventario")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> inventario() {
        List<Map<String, Object>> filas = reporteService.reporteInventario();
        return ResponseEntity.ok(ApiResponse.exito(filas.size() + " producto(s) en el inventario.", filas));
    }

    // -----------------------------------------------------------------
    // Exportacion CSV (RF-22)
    // -----------------------------------------------------------------

    @GetMapping("/ventas/csv")
    public ResponseEntity<byte[]> ventasCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return archivoCsv(reporteService.exportarVentasCsv(desde, hasta), "reporte_ventas.csv");
    }

    @GetMapping("/inventario/csv")
    public ResponseEntity<byte[]> inventarioCsv() {
        return archivoCsv(reporteService.exportarInventarioCsv(), "reporte_inventario.csv");
    }

    @GetMapping("/auditoria/csv")
    public ResponseEntity<byte[]> auditoriaCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return archivoCsv(reporteService.exportarAuditoriaCsv(desde, hasta), "bitacora_auditoria.csv");
    }

    private ResponseEntity<byte[]> archivoCsv(String contenido, String nombreArchivo) {
        byte[] datos = ("﻿" + contenido).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(datos);
    }
}
