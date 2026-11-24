/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : MovimientoRequest.java
 * Descripcion   : DTO para registrar entradas, salidas y ajustes de inventario (RF-09, RF-10).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.edu.upn.bodega.entity.TipoMovimiento;

public class MovimientoRequest {

    @NotNull(message = "El producto es obligatorio")
    private Long idProducto;

    @NotNull(message = "El tipo de movimiento es obligatorio")
    private TipoMovimiento tipo;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor que cero")
    private Integer cantidad;

    @NotBlank(message = "El motivo del movimiento es obligatorio")
    private String motivo;

    private String referencia;

    public Long getIdProducto() { return idProducto; }
    public void setIdProducto(Long idProducto) { this.idProducto = idProducto; }

    public TipoMovimiento getTipo() { return tipo; }
    public void setTipo(TipoMovimiento tipo) { this.tipo = tipo; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
}
