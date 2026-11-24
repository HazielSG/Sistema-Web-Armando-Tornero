/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : UsuarioService.java
 * Descripcion   : Servicio de negocio para la gestion de cuentas de usuario y sus permisos (RF-04, CU-01, CU-02). Cada alta, cambio o desactivacion queda registrada en la bitacora de auditoria.
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.bodega.dto.UsuarioRequest;
import pe.edu.upn.bodega.entity.AccionAuditoria;
import pe.edu.upn.bodega.entity.Usuario;
import pe.edu.upn.bodega.repository.UsuarioRepository;
import pe.edu.upn.bodega.util.NegocioException;
import pe.edu.upn.bodega.util.RecursoNoEncontradoException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro el usuario solicitado."));
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro el usuario " + username + "."));
    }

    @Transactional
    public Usuario registrar(UsuarioRequest peticion) {
        if (usuarioRepository.existsByUsername(peticion.getUsername())) {
            throw new NegocioException("El nombre de usuario ya se encuentra registrado.");
        }
        if (peticion.getPassword() == null || peticion.getPassword().isBlank()) {
            throw new NegocioException("La contrasena es obligatoria para registrar un usuario.");
        }

        auditoriaService.marcarUsuarioBd();

        Usuario usuario = new Usuario();
        usuario.setUsername(peticion.getUsername().trim());
        usuario.setPasswordHash(passwordEncoder.encode(peticion.getPassword()));
        usuario.setNombres(peticion.getNombres());
        usuario.setApellidos(peticion.getApellidos());
        usuario.setDocumento(peticion.getDocumento());
        usuario.setEmail(peticion.getEmail());
        usuario.setTelefono(peticion.getTelefono());
        usuario.setRol(peticion.getRol());
        usuario.setActivo(peticion.getActivo() == null ? Boolean.TRUE : peticion.getActivo());
        usuario.setFechaCreacion(LocalDateTime.now());

        Usuario guardado = usuarioRepository.save(usuario);

        auditoriaService.registrar(AccionAuditoria.CREAR, "USUARIO", guardado.getId(),
                "Se registro la cuenta " + guardado.getUsername() + " con rol " + guardado.getRol(),
                null,
                "{\"username\":\"" + guardado.getUsername() + "\",\"rol\":\"" + guardado.getRol() + "\"}");

        return guardado;
    }

    @Transactional
    public Usuario actualizar(Long id, UsuarioRequest peticion) {
        Usuario usuario = obtener(id);
        auditoriaService.marcarUsuarioBd();

        String anterior = "{\"nombres\":\"" + usuario.getNombres() + " " + usuario.getApellidos()
                + "\",\"rol\":\"" + usuario.getRol()
                + "\",\"activo\":" + usuario.getActivo() + "}";

        usuario.setNombres(peticion.getNombres());
        usuario.setApellidos(peticion.getApellidos());
        usuario.setDocumento(peticion.getDocumento());
        usuario.setEmail(peticion.getEmail());
        usuario.setTelefono(peticion.getTelefono());
        usuario.setRol(peticion.getRol());
        if (peticion.getActivo() != null) {
            usuario.setActivo(peticion.getActivo());
        }

        boolean claveCambiada = false;
        if (peticion.getPassword() != null && !peticion.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(peticion.getPassword()));
            claveCambiada = true;
        }

        Usuario guardado = usuarioRepository.save(usuario);

        String nuevo = "{\"nombres\":\"" + guardado.getNombres() + " " + guardado.getApellidos()
                + "\",\"rol\":\"" + guardado.getRol()
                + "\",\"activo\":" + guardado.getActivo()
                + ",\"clave_cambiada\":" + claveCambiada + "}";

        auditoriaService.registrar(AccionAuditoria.ACTUALIZAR, "USUARIO", guardado.getId(),
                "Se actualizo la cuenta " + guardado.getUsername(), anterior, nuevo);

        return guardado;
    }

    /** CU-05 aplicado a usuarios: baja logica para conservar el historial. */
    @Transactional
    public Usuario cambiarEstado(Long id, boolean activo) {
        Usuario usuario = obtener(id);
        auditoriaService.marcarUsuarioBd();

        if (Boolean.TRUE.equals(usuario.getActivo()) && !activo
                && usuario.getUsername().equals(auditoriaService.usernameActual())) {
            throw new NegocioException("No puede desactivar la cuenta con la que esta trabajando.");
        }

        boolean estadoAnterior = Boolean.TRUE.equals(usuario.getActivo());
        usuario.setActivo(activo);
        Usuario guardado = usuarioRepository.save(usuario);

        auditoriaService.registrar(activo ? AccionAuditoria.ACTUALIZAR : AccionAuditoria.DESACTIVAR,
                "USUARIO", guardado.getId(),
                (activo ? "Se reactivo" : "Se desactivo") + " la cuenta " + guardado.getUsername(),
                "{\"activo\":" + estadoAnterior + "}",
                "{\"activo\":" + activo + "}");

        return guardado;
    }

    /** CU-02: restablecer la contrasena de una cuenta. */
    @Transactional
    public void restablecerClave(Long id, String claveNueva) {
        if (claveNueva == null || claveNueva.length() < 6) {
            throw new NegocioException("La nueva contrasena debe tener al menos 6 caracteres.");
        }
        Usuario usuario = obtener(id);
        auditoriaService.marcarUsuarioBd();
        usuario.setPasswordHash(passwordEncoder.encode(claveNueva));
        usuarioRepository.save(usuario);

        auditoriaService.registrar(AccionAuditoria.ACTUALIZAR, "USUARIO", usuario.getId(),
                "Se restablecio la contrasena de la cuenta " + usuario.getUsername(),
                null, "{\"clave_cambiada\":true}");
    }

    @Transactional
    public void registrarUltimoAcceso(String username) {
        usuarioRepository.findByUsername(username).ifPresent(usuario -> {
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);
        });
    }

    @Transactional(readOnly = true)
    public long contarActivos() {
        return usuarioRepository.findByActivoTrueOrderByApellidosAsc().size();
    }
}
