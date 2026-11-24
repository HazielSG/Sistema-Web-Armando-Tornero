/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : CierreCaja.java
 * Descripcion   : Entidad que registra la apertura y el cierre de caja del
 *                 turno del cajero (RF-19).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cierre_caja")
public class CierreCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "passwordHash"})
    private Usuario usuario;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura = LocalDateTime.now();

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "monto_inicial", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoInicial = BigDecimal.ZERO;

    @Column(name = "total_efectivo", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalEfectivo = BigDecimal.ZERO;

    @Column(name = "total_tarjeta", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalTarjeta = BigDecimal.ZERO;

    @Column(name = "total_digital", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalDigital = BigDecimal.ZERO;

    @Column(name = "total_ventas", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalVentas = BigDecimal.ZERO;

    @Column(name = "cantidad_ventas", nullable = false)
    private Integer cantidadVentas = 0;

    @Column(name = "monto_declarado", precision = 10, scale = 2)
    private BigDecimal montoDeclarado;

    @Column(precision = 10, scale = 2)
    private BigDecimal diferencia;

    @Column(length = 255)
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoCaja estado = EstadoCaja.ABIERTA;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(LocalDateTime fechaApertura) { this.fechaApertura = fechaApertura; }

    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }

    public BigDecimal getMontoInicial() { return montoInicial; }
    public void setMontoInicial(BigDecimal montoInicial) { this.montoInicial = montoInicial; }

    public BigDecimal getTotalEfectivo() { return totalEfectivo; }
    public void setTotalEfectivo(BigDecimal totalEfectivo) { this.totalEfectivo = totalEfectivo; }

    public BigDecimal getTotalTarjeta() { return totalTarjeta; }
    public void setTotalTarjeta(BigDecimal totalTarjeta) { this.totalTarjeta = totalTarjeta; }

    public BigDecimal getTotalDigital() { return totalDigital; }
    public void setTotalDigital(BigDecimal totalDigital) { this.totalDigital = totalDigital; }

    public BigDecimal getTotalVentas() { return totalVentas; }
    public void setTotalVentas(BigDecimal totalVentas) { this.totalVentas = totalVentas; }

    public Integer getCantidadVentas() { return cantidadVentas; }
    public void setCantidadVentas(Integer cantidadVentas) { this.cantidadVentas = cantidadVentas; }

    public BigDecimal getMontoDeclarado() { return montoDeclarado; }
    public void setMontoDeclarado(BigDecimal montoDeclarado) { this.montoDeclarado = montoDeclarado; }

    public BigDecimal getDiferencia() { return diferencia; }
    public void setDiferencia(BigDecimal diferencia) { this.diferencia = diferencia; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public EstadoCaja getEstado() { return estado; }
    public void setEstado(EstadoCaja estado) { this.estado = estado; }
}
