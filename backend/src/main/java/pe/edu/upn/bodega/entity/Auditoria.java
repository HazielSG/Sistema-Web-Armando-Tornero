/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : Auditoria.java
 * Descripcion   : Entidad de la bitacora de auditoria. Registra quien, que,
 *                 cuando y desde donde se ejecuto cada operacion relevante
 *                 (RNF-02 y restriccion etica de trazabilidad).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(length = 20)
    private String rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccionAuditoria accion;

    @Column(nullable = false, length = 50)
    private String entidad;

    @Column(name = "entidad_id", length = 50)
    private String entidadId;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(name = "valor_anterior", columnDefinition = "TEXT")
    private String valorAnterior;

    @Column(name = "valor_nuevo", columnDefinition = "TEXT")
    private String valorNuevo;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    @Column(length = 255)
    private String origen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ResultadoAuditoria resultado = ResultadoAuditoria.EXITO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public AccionAuditoria getAccion() { return accion; }
    public void setAccion(AccionAuditoria accion) { this.accion = accion; }

    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }

    public String getEntidadId() { return entidadId; }
    public void setEntidadId(String entidadId) { this.entidadId = entidadId; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getValorAnterior() { return valorAnterior; }
    public void setValorAnterior(String valorAnterior) { this.valorAnterior = valorAnterior; }

    public String getValorNuevo() { return valorNuevo; }
    public void setValorNuevo(String valorNuevo) { this.valorNuevo = valorNuevo; }

    public String getDireccionIp() { return direccionIp; }
    public void setDireccionIp(String direccionIp) { this.direccionIp = direccionIp; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    public ResultadoAuditoria getResultado() { return resultado; }
    public void setResultado(ResultadoAuditoria resultado) { this.resultado = resultado; }
}
