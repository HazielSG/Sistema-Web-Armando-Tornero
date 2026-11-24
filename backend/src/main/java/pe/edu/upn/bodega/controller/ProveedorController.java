/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : ProveedorController.java
 * Descripcion   : Controlador REST de los proveedores (RF-08, CU-04).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upn.bodega.dto.ApiResponse;
import pe.edu.upn.bodega.entity.Proveedor;
import pe.edu.upn.bodega.service.ProveedorService;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Proveedor>>> listar() {
        List<Proveedor> proveedores = proveedorService.listar();
        return ResponseEntity.ok(ApiResponse.exito(proveedores.size() + " proveedor(es).", proveedores));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Proveedor>> registrar(@RequestBody Proveedor proveedor) {
        return ResponseEntity.ok(ApiResponse.exito("Proveedor registrado correctamente.",
                proveedorService.registrar(proveedor)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Proveedor>> actualizar(@PathVariable Long id,
                                                             @RequestBody Proveedor proveedor) {
        return ResponseEntity.ok(ApiResponse.exito("Proveedor actualizado correctamente.",
                proveedorService.actualizar(id, proveedor)));
    }
}
