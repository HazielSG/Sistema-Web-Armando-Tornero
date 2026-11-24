/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : AuthController.java
 * Descripcion   : Controlador de inicio y cierre de sesion. Registra en la bitacora de auditoria los accesos correctos y los intentos fallidos (RF-01, RF-02, CP-01).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import pe.edu.upn.bodega.dto.ApiResponse;
import pe.edu.upn.bodega.dto.LoginRequest;
import pe.edu.upn.bodega.dto.SesionResponse;
import pe.edu.upn.bodega.entity.AccionAuditoria;
import pe.edu.upn.bodega.entity.ResultadoAuditoria;
import pe.edu.upn.bodega.entity.Usuario;
import pe.edu.upn.bodega.service.AuditoriaService;
import pe.edu.upn.bodega.service.UsuarioService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          UsuarioService usuarioService,
                          AuditoriaService auditoriaService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.usuarioService = usuarioService;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SesionResponse>> login(@Valid @RequestBody LoginRequest peticion,
                                                             HttpServletRequest request,
                                                             HttpServletResponse response) {
        try {
            Authentication autenticacion = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(peticion.getUsername(), peticion.getPassword()));

            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(autenticacion);
            SecurityContextHolder.setContext(contexto);
            securityContextRepository.saveContext(contexto, request, response);

            Usuario usuario = usuarioService.obtenerPorUsername(autenticacion.getName());
            usuarioService.registrarUltimoAcceso(usuario.getUsername());

            auditoriaService.registrarIndependiente(AccionAuditoria.LOGIN, "SESION", usuario.getId(),
                    "Inicio de sesion correcto del usuario " + usuario.getUsername(),
                    ResultadoAuditoria.EXITO, usuario.getUsername(), usuario.getRol().name());

            SesionResponse sesion = new SesionResponse(usuario.getId(), usuario.getUsername(),
                    usuario.getNombreCompleto(), usuario.getRol().name());

            return ResponseEntity.ok(ApiResponse.exito("Bienvenido(a) " + usuario.getNombres() + ".", sesion));

        } catch (AuthenticationException ex) {
            // PF-01 / trazabilidad: se registra el intento fallido
            auditoriaService.registrarIndependiente(AccionAuditoria.LOGIN_FALLIDO, "SESION", null,
                    "Intento de inicio de sesion fallido con el usuario " + peticion.getUsername(),
                    ResultadoAuditoria.ERROR, peticion.getUsername(), null);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Usuario o contrasena incorrectos, o la cuenta esta desactivada."));
        }
    }

    @GetMapping("/sesion")
    public ResponseEntity<ApiResponse<SesionResponse>> sesionActual() {
        String username = auditoriaService.usernameActual();
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("No existe una sesion activa."));
        }
        Usuario usuario = usuarioService.obtenerPorUsername(username);
        SesionResponse sesion = new SesionResponse(usuario.getId(), usuario.getUsername(),
                usuario.getNombreCompleto(), usuario.getRol().name());
        return ResponseEntity.ok(ApiResponse.exito("Sesion activa.", sesion));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String username = auditoriaService.usernameActual();
        String rol = auditoriaService.rolActual();

        if (username != null) {
            auditoriaService.registrarIndependiente(AccionAuditoria.LOGOUT, "SESION", null,
                    "Cierre de sesion del usuario " + username,
                    ResultadoAuditoria.EXITO, username, rol);
        }

        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(ApiResponse.exito("Sesion finalizada correctamente.", null));
    }
}
