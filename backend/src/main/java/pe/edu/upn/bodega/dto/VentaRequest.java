/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : VentaRequest.java
 * Descripcion   : DTO para registrar una venta con uno o mas productos (RF-13 a RF-17).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import pe.edu.upn.bodega.entity.EstadoPago;
import pe.edu.upn.bodega.entity.MetodoPago;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class VentaRequest {

    private String clienteNombre;

    private String clienteDocumento;

    @NotNull(message = "El metodo de pago es obligatorio")
    private MetodoPago metodoPago = MetodoPago.EFECTIVO;

    private EstadoPago estadoPago = EstadoPago.PAGADO;

    private BigDecimal montoRecibido;

    @NotEmpty(message = "La venta debe tener al menos un producto")
    @Valid
    private List<ItemVentaRequest> items = new ArrayList<>();

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getClienteDocumento() { return clienteDocumento; }
    public void setClienteDocumento(String clienteDocumento) { this.clienteDocumento = clienteDocumento; }

    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago metodoPago) { this.metodoPago = metodoPago; }

    public EstadoPago getEstadoPago() { return estadoPago; }
    public void setEstadoPago(EstadoPago estadoPago) { this.estadoPago = estadoPago; }

    public BigDecimal getMontoRecibido() { return montoRecibido; }
    public void setMontoRecibido(BigDecimal montoRecibido) { this.montoRecibido = montoRecibido; }

    public List<ItemVentaRequest> getItems() { return items; }
    public void setItems(List<ItemVentaRequest> items) { this.items = items; }
}
