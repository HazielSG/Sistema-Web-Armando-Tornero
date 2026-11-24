/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : ProductoController.java
 * Descripcion   : Controlador REST del catalogo de productos (RF-05, RF-06, RF-12).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upn.bodega.dto.ApiResponse;
import pe.edu.upn.bodega.dto.ProductoRequest;
import pe.edu.upn.bodega.entity.Producto;
import pe.edu.upn.bodega.service.ProductoService;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Producto>>> listar(
            @RequestParam(required = false) String buscar) {
        List<Producto> productos = productoService.buscar(buscar);
        return ResponseEntity.ok(ApiResponse.exito(productos.size() + " producto(s) encontrado(s).", productos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Producto>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.exito("Producto encontrado.", productoService.obtener(id)));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<ApiResponse<Producto>> porCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(ApiResponse.exito("Producto encontrado.",
                productoService.obtenerPorCodigo(codigo)));
    }

    /** RF-12: alerta de productos que alcanzaron el stock minimo. */
    @GetMapping("/stock-bajo")
    public ResponseEntity<ApiResponse<List<Producto>>> stockBajo() {
        List<Producto> productos = productoService.listarStockBajo();
        return ResponseEntity.ok(ApiResponse.exito(
                productos.isEmpty() ? "No hay productos con stock bajo."
                                    : productos.size() + " producto(s) requieren reposicion.", productos));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Producto>> registrar(@Valid @RequestBody ProductoRequest peticion) {
        Producto producto = productoService.registrar(peticion);
        return ResponseEntity.ok(ApiResponse.exito("Producto registrado correctamente.", producto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Producto>> actualizar(@PathVariable Long id,
                                                            @Valid @RequestBody ProductoRequest peticion) {
        Producto producto = productoService.actualizar(id, peticion);
        return ResponseEntity.ok(ApiResponse.exito("Producto actualizado correctamente.", producto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Producto>> desactivar(@PathVariable Long id) {
        Producto producto = productoService.desactivar(id);
        return ResponseEntity.ok(ApiResponse.exito("Producto desactivado. El historial se conserva.", producto));
    }

    @PutMapping("/{id}/reactivar")
    public ResponseEntity<ApiResponse<Producto>> reactivar(@PathVariable Long id) {
        Producto producto = productoService.reactivar(id);
        return ResponseEntity.ok(ApiResponse.exito("Producto reactivado.", producto));
    }
}
