/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : CajaController.java
 * Descripcion   : Controlador REST de la apertura y el cierre de caja del turno (RF-19).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upn.bodega.dto.ApiResponse;
import pe.edu.upn.bodega.dto.AperturaCajaRequest;
import pe.edu.upn.bodega.dto.CierreCajaRequest;
import pe.edu.upn.bodega.entity.CierreCaja;
import pe.edu.upn.bodega.entity.Venta;
import pe.edu.upn.bodega.service.CajaService;

import java.util.List;

@RestController
@RequestMapping("/api/caja")
public class CajaController {

    private final CajaService cajaService;

    public CajaController(CajaService cajaService) {
        this.cajaService = cajaService;
    }

    @GetMapping("/estado")
    public ResponseEntity<ApiResponse<CierreCaja>> estado() {
        return cajaService.cajaAbierta()
                .map(caja -> ResponseEntity.ok(ApiResponse.exito("Caja abierta.", caja)))
                .orElseGet(() -> ResponseEntity.ok(
                        ApiResponse.exito("No tiene una caja abierta.", null)));
    }

    @PostMapping("/apertura")
    public ResponseEntity<ApiResponse<CierreCaja>> aperturar(@Valid @RequestBody AperturaCajaRequest peticion) {
        return ResponseEntity.ok(ApiResponse.exito("Caja aperturada correctamente.",
                cajaService.aperturar(peticion)));
    }

    @GetMapping("/resumen")
    public ResponseEntity<ApiResponse<CierreCaja>> resumen() {
        return ResponseEntity.ok(ApiResponse.exito("Resumen del turno.", cajaService.resumenTurno()));
    }

    @PostMapping("/cierre")
    public ResponseEntity<ApiResponse<CierreCaja>> cerrar(@Valid @RequestBody CierreCajaRequest peticion) {
        CierreCaja caja = cajaService.cerrar(peticion);
        return ResponseEntity.ok(ApiResponse.exito(
                "Cierre registrado. Diferencia: S/ " + caja.getDiferencia() + ".", caja));
    }

    @GetMapping("/historial")
    public ResponseEntity<ApiResponse<List<CierreCaja>>> historial() {
        List<CierreCaja> cierres = cajaService.historial();
        return ResponseEntity.ok(ApiResponse.exito(cierres.size() + " registro(s) de caja.", cierres));
    }

    @GetMapping("/{id}/ventas")
    public ResponseEntity<ApiResponse<List<Venta>>> ventasDelTurno(@PathVariable Long id) {
        List<Venta> ventas = cajaService.ventasDelTurno(id);
        return ResponseEntity.ok(ApiResponse.exito(ventas.size() + " venta(s) en el turno.", ventas));
    }
}
