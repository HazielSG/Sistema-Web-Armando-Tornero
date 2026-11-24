/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : UsuarioController.java
 * Descripcion   : Controlador REST de la gestion de cuentas de usuario (RF-04, CU-01, CU-02).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upn.bodega.dto.ApiResponse;
import pe.edu.upn.bodega.dto.UsuarioRequest;
import pe.edu.upn.bodega.entity.Usuario;
import pe.edu.upn.bodega.service.UsuarioService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Usuario>>> listar() {
        List<Usuario> usuarios = usuarioService.listar();
        usuarios.forEach(u -> u.setPasswordHash(null));
        return ResponseEntity.ok(ApiResponse.exito(usuarios.size() + " usuario(s).", usuarios));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Usuario>> obtener(@PathVariable Long id) {
        Usuario usuario = usuarioService.obtener(id);
        usuario.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.exito("Usuario encontrado.", usuario));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Usuario>> registrar(@Valid @RequestBody UsuarioRequest peticion) {
        Usuario usuario = usuarioService.registrar(peticion);
        usuario.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.exito("Usuario registrado correctamente.", usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Usuario>> actualizar(@PathVariable Long id,
                                                           @Valid @RequestBody UsuarioRequest peticion) {
        Usuario usuario = usuarioService.actualizar(id, peticion);
        usuario.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.exito("Usuario actualizado correctamente.", usuario));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<Usuario>> cambiarEstado(@PathVariable Long id,
                                                              @RequestBody Map<String, Boolean> cuerpo) {
        boolean activo = Boolean.TRUE.equals(cuerpo.get("activo"));
        Usuario usuario = usuarioService.cambiarEstado(id, activo);
        usuario.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.exito(
                activo ? "Cuenta reactivada." : "Cuenta desactivada.", usuario));
    }

    /** CU-02: restablecer el acceso de una cuenta. */
    @PutMapping("/{id}/clave")
    public ResponseEntity<ApiResponse<Void>> restablecerClave(@PathVariable Long id,
                                                              @RequestBody Map<String, String> cuerpo) {
        usuarioService.restablecerClave(id, cuerpo.get("password"));
        return ResponseEntity.ok(ApiResponse.exito("Contrasena restablecida correctamente.", null));
    }
}
