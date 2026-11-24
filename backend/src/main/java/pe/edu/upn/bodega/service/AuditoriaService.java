/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : AuditoriaService.java
 * Descripcion   : Servicio de la capa de negocio que escribe y consulta la bitacora de auditoria. Toda operacion relevante del sistema pasa por aqui: deja constancia de quien la ejecuto, sobre que registro, con que valores anteriores y nuevos, desde que direccion IP y con que resultado.
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.service;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pe.edu.upn.bodega.entity.AccionAuditoria;
import pe.edu.upn.bodega.entity.Auditoria;
import pe.edu.upn.bodega.entity.ResultadoAuditoria;
import pe.edu.upn.bodega.entity.Usuario;
import pe.edu.upn.bodega.repository.AuditoriaRepository;
import pe.edu.upn.bodega.repository.UsuarioRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntityManager entityManager;

    public AuditoriaService(AuditoriaRepository auditoriaRepository,
                            UsuarioRepository usuarioRepository,
                            EntityManager entityManager) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.entityManager = entityManager;
    }

    // -----------------------------------------------------------------
    // Escritura de la bitacora
    // -----------------------------------------------------------------

    /** Registra una operacion dentro de la transaccion del servicio que la invoca. */
    @Transactional
    public void registrar(AccionAuditoria accion,
                          String entidad,
                          Object entidadId,
                          String descripcion,
                          String valorAnterior,
                          String valorNuevo) {
        guardar(accion, entidad, entidadId, descripcion, valorAnterior, valorNuevo,
                ResultadoAuditoria.EXITO, usernameActual(), rolActual());
    }

    /**
     * Registra una operacion en una transaccion independiente. Se utiliza para los
     * intentos de acceso fallidos y los accesos denegados, de modo que el registro
     * se conserve aunque la operacion principal termine en error.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIndependiente(AccionAuditoria accion,
                                       String entidad,
                                       Object entidadId,
                                       String descripcion,
                                       ResultadoAuditoria resultado,
                                       String username,
                                       String rol) {
        guardar(accion, entidad, entidadId, descripcion, null, null, resultado, username, rol);
    }

    private void guardar(AccionAuditoria accion,
                         String entidad,
                         Object entidadId,
                         String descripcion,
                         String valorAnterior,
                         String valorNuevo,
                         ResultadoAuditoria resultado,
                         String username,
                         String rol) {

        Auditoria registro = new Auditoria();
        registro.setFechaHora(LocalDateTime.now());
        registro.setUsername(username == null ? "ANONIMO" : username);
        registro.setRol(rol);
        registro.setAccion(accion);
        registro.setEntidad(entidad);
        registro.setEntidadId(entidadId == null ? null : String.valueOf(entidadId));
        registro.setDescripcion(recortar(descripcion, 500));
        registro.setValorAnterior(valorAnterior);
        registro.setValorNuevo(valorNuevo);
        registro.setResultado(resultado);

        if (username != null) {
            Optional<Usuario> usuario = usuarioRepository.findByUsername(username);
            usuario.ifPresent(u -> registro.setIdUsuario(u.getId()));
        }

        HttpServletRequest request = requestActual();
        if (request != null) {
            registro.setDireccionIp(direccionIp(request));
            registro.setOrigen(recortar(request.getMethod() + " " + request.getRequestURI(), 255));
        } else {
            registro.setOrigen("PROCESO_INTERNO");
        }

        auditoriaRepository.save(registro);
    }

    /**
     * Informa a MySQL el usuario autenticado mediante la variable de sesion
     * @usuario_app, para que los triggers de auditoria de la base de datos
     * registren al responsable del cambio. Debe invocarse dentro de la misma
     * transaccion de la operacion que se va a auditar.
     */
    @Transactional
    public void marcarUsuarioBd() {
        String username = usernameActual();
        entityManager.createNativeQuery("SET @usuario_app = :usuario")
                .setParameter("usuario", username == null ? "ANONIMO" : username)
                .executeUpdate();
    }

    // -----------------------------------------------------------------
    // Consulta de la bitacora (RF-20)
    // -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<Auditoria> filtrar(LocalDate desde,
                                   LocalDate hasta,
                                   String username,
                                   AccionAuditoria accion,
                                   String entidad,
                                   int pagina,
                                   int tamano) {

        LocalDateTime inicio = (desde == null ? LocalDate.now().minusDays(30) : desde).atStartOfDay();
        LocalDateTime fin = (hasta == null ? LocalDate.now() : hasta).atTime(LocalTime.MAX);

        List<Specification<Auditoria>> condiciones = new ArrayList<>();
        condiciones.add((raiz, consulta, cb) -> cb.between(raiz.get("fechaHora"), inicio, fin));

        if (username != null && !username.isBlank()) {
            condiciones.add((raiz, consulta, cb) ->
                    cb.equal(cb.lower(raiz.get("username")), username.trim().toLowerCase()));
        }
        if (accion != null) {
            condiciones.add((raiz, consulta, cb) -> cb.equal(raiz.get("accion"), accion));
        }
        if (entidad != null && !entidad.isBlank()) {
            condiciones.add((raiz, consulta, cb) ->
                    cb.equal(cb.lower(raiz.get("entidad")), entidad.trim().toLowerCase()));
        }

        Specification<Auditoria> filtro = condiciones.get(0);
        for (int i = 1; i < condiciones.size(); i++) {
            filtro = filtro.and(condiciones.get(i));
        }

        Pageable paginado = PageRequest.of(Math.max(pagina, 0),
                                           tamano <= 0 ? 25 : Math.min(tamano, 200),
                                           Sort.by(Sort.Direction.DESC, "fechaHora"));
        return auditoriaRepository.findAll(filtro, paginado);
    }

    @Transactional(readOnly = true)
    public List<Auditoria> ultimosMovimientos() {
        return auditoriaRepository.findTop10ByOrderByFechaHoraDesc();
    }

    @Transactional(readOnly = true)
    public List<Object[]> resumenPorAccion(LocalDate desde, LocalDate hasta) {
        return auditoriaRepository.resumenPorAccion(
                (desde == null ? LocalDate.now().minusDays(30) : desde).atStartOfDay(),
                (hasta == null ? LocalDate.now() : hasta).atTime(LocalTime.MAX));
    }

    @Transactional(readOnly = true)
    public List<String> listarEntidades() {
        return auditoriaRepository.listarEntidades();
    }

    @Transactional(readOnly = true)
    public List<String> listarUsuarios() {
        return auditoriaRepository.listarUsuarios();
    }

    @Transactional(readOnly = true)
    public long contarOperacionesDeHoy() {
        return filtrar(LocalDate.now(), LocalDate.now(), null, null, null, 0, 1)
                .getTotalElements();
    }

    // -----------------------------------------------------------------
    // Utilitarios
    // -----------------------------------------------------------------

    public String usernameActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }

    public String rolActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return null;
        }
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElse(null);
    }

    private HttpServletRequest requestActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            return atributos.getRequest();
        }
        return null;
    }

    public static String direccionIp(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String recortar(String texto, int maximo) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
