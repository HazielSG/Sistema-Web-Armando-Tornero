/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : AperturaCajaRequest.java
 * Descripcion   : DTO para aperturar la caja del turno (RF-19).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AperturaCajaRequest {

    @NotNull(message = "El monto inicial es obligatorio")
    @DecimalMin(value = "0.00", message = "El monto inicial no puede ser negativo")
    private BigDecimal montoInicial = BigDecimal.ZERO;

    public BigDecimal getMontoInicial() { return montoInicial; }
    public void setMontoInicial(BigDecimal montoInicial) { this.montoInicial = montoInicial; }
}
