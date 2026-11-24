/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : VentaController.java
 * Descripcion   : Controlador REST del registro de ventas y la emision del comprobante interno (RF-13 a RF-18).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upn.bodega.dto.ApiResponse;
import pe.edu.upn.bodega.dto.VentaRequest;
import pe.edu.upn.bodega.entity.Venta;
import pe.edu.upn.bodega.service.VentaService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Venta>> registrar(@Valid @RequestBody VentaRequest peticion) {
        Venta venta = ventaService.registrar(peticion);
        return ResponseEntity.ok(ApiResponse.exito(
                "Venta " + venta.getNumeroComprobante() + " registrada por S/ " + venta.getTotal() + ".", venta));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Venta>>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        List<Venta> ventas = (desde == null && hasta == null)
                ? ventaService.ultimasVentas()
                : ventaService.listarPorRango(desde, hasta);

        return ResponseEntity.ok(ApiResponse.exito(ventas.size() + " venta(s).", ventas));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Venta>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.exito("Venta encontrada.", ventaService.obtener(id)));
    }

    /** RF-18: comprobante interno de la venta registrada. */
    @GetMapping("/{id}/comprobante")
    public ResponseEntity<ApiResponse<Venta>> comprobante(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.exito("Comprobante generado.",
                ventaService.emitirComprobante(id)));
    }

    @PutMapping("/{id}/anular")
    public ResponseEntity<ApiResponse<Venta>> anular(@PathVariable Long id,
                                                     @RequestBody Map<String, String> cuerpo) {
        Venta venta = ventaService.anular(id, cuerpo.get("motivo"));
        return ResponseEntity.ok(ApiResponse.exito(
                "Venta " + venta.getNumeroComprobante() + " anulada y stock repuesto.", venta));
    }
}
