/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : CategoriaController.java
 * Descripcion   : Controlador REST de las categorias de productos (RF-07).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upn.bodega.dto.ApiResponse;
import pe.edu.upn.bodega.entity.Categoria;
import pe.edu.upn.bodega.service.CategoriaService;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Categoria>>> listar() {
        List<Categoria> categorias = categoriaService.listar();
        return ResponseEntity.ok(ApiResponse.exito(categorias.size() + " categoria(s).", categorias));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Categoria>> registrar(@RequestBody Categoria categoria) {
        return ResponseEntity.ok(ApiResponse.exito("Categoria registrada correctamente.",
                categoriaService.registrar(categoria)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Categoria>> actualizar(@PathVariable Long id,
                                                             @RequestBody Categoria categoria) {
        return ResponseEntity.ok(ApiResponse.exito("Categoria actualizada correctamente.",
                categoriaService.actualizar(id, categoria)));
    }
}
