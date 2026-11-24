/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : ResumenDashboard.java
 * Descripcion   : DTO con los indicadores que se muestran en el panel principal.
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.dto;

import java.math.BigDecimal;

public class ResumenDashboard {

    private long productosRegistrados;
    private long productosStockBajo;
    private long ventasDelDia;
    private BigDecimal totalVendidoDia = BigDecimal.ZERO;
    private BigDecimal totalVendidoMes = BigDecimal.ZERO;
    private long usuariosActivos;
    private long operacionesAuditadasHoy;

    public long getProductosRegistrados() { return productosRegistrados; }
    public void setProductosRegistrados(long productosRegistrados) { this.productosRegistrados = productosRegistrados; }

    public long getProductosStockBajo() { return productosStockBajo; }
    public void setProductosStockBajo(long productosStockBajo) { this.productosStockBajo = productosStockBajo; }

    public long getVentasDelDia() { return ventasDelDia; }
    public void setVentasDelDia(long ventasDelDia) { this.ventasDelDia = ventasDelDia; }

    public BigDecimal getTotalVendidoDia() { return totalVendidoDia; }
    public void setTotalVendidoDia(BigDecimal totalVendidoDia) { this.totalVendidoDia = totalVendidoDia; }

    public BigDecimal getTotalVendidoMes() { return totalVendidoMes; }
    public void setTotalVendidoMes(BigDecimal totalVendidoMes) { this.totalVendidoMes = totalVendidoMes; }

    public long getUsuariosActivos() { return usuariosActivos; }
    public void setUsuariosActivos(long usuariosActivos) { this.usuariosActivos = usuariosActivos; }

    public long getOperacionesAuditadasHoy() { return operacionesAuditadasHoy; }
    public void setOperacionesAuditadasHoy(long operacionesAuditadasHoy) { this.operacionesAuditadasHoy = operacionesAuditadasHoy; }
}
