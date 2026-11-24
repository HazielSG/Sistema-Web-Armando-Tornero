/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : InventarioController.java
 * Descripcion   : Controlador REST del control de inventario: movimientos, existencias y alertas (RF-09 a RF-12).
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
import pe.edu.upn.bodega.dto.MovimientoRequest;
import pe.edu.upn.bodega.entity.MovimientoInventario;
import pe.edu.upn.bodega.service.InventarioService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping("/movimientos")
    public ResponseEntity<ApiResponse<List<MovimientoInventario>>> movimientos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        List<MovimientoInventario> movimientos = (desde == null && hasta == null)
                ? inventarioService.ultimosMovimientos()
                : inventarioService.movimientosPorRango(desde, hasta);

        return ResponseEntity.ok(ApiResponse.exito(movimientos.size() + " movimiento(s).", movimientos));
    }

    @GetMapping("/movimientos/producto/{idProducto}")
    public ResponseEntity<ApiResponse<List<MovimientoInventario>>> porProducto(@PathVariable Long idProducto) {
        List<MovimientoInventario> movimientos = inventarioService.movimientosPorProducto(idProducto);
        return ResponseEntity.ok(ApiResponse.exito(movimientos.size() + " movimiento(s).", movimientos));
    }

    @PostMapping("/movimientos")
    public ResponseEntity<ApiResponse<MovimientoInventario>> registrar(
            @Valid @RequestBody MovimientoRequest peticion) {
        MovimientoInventario movimiento = inventarioService.registrarMovimiento(peticion);
        return ResponseEntity.ok(ApiResponse.exito(
                "Movimiento registrado. Stock actualizado a " + movimiento.getStockResultante()
                        + " " + movimiento.getProducto().getUnidadMedida().toLowerCase() + ".",
                movimiento));
    }
}
