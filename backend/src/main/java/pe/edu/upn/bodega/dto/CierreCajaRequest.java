/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : CierreCajaRequest.java
 * Descripcion   : DTO para registrar el cierre de caja del turno (RF-19).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CierreCajaRequest {

    @NotNull(message = "El monto declarado es obligatorio")
    @DecimalMin(value = "0.00", message = "El monto declarado no puede ser negativo")
    private BigDecimal montoDeclarado;

    private String observaciones;

    public BigDecimal getMontoDeclarado() { return montoDeclarado; }
    public void setMontoDeclarado(BigDecimal montoDeclarado) { this.montoDeclarado = montoDeclarado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
