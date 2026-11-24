/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : VentaRepository.java
 * Descripcion   : Repositorio de acceso a datos de las ventas y de los totales usados en reportes y cierre de caja.
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upn.bodega.entity.Venta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    Optional<Venta> findByNumeroComprobante(String numeroComprobante);

    List<Venta> findTop50ByOrderByFechaDesc();

    @Query("SELECT v FROM Venta v WHERE v.fecha BETWEEN :desde AND :hasta ORDER BY v.fecha DESC")
    List<Venta> listarPorRango(@Param("desde") LocalDateTime desde,
                               @Param("hasta") LocalDateTime hasta);

    List<Venta> findByCierreCajaIdOrderByFechaAsc(Long idCierre);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v "
         + "WHERE v.estado = pe.edu.upn.bodega.entity.EstadoVenta.EMITIDA "
         + "  AND v.fecha BETWEEN :desde AND :hasta")
    BigDecimal totalVendido(@Param("desde") LocalDateTime desde,
                            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(v) FROM Venta v "
         + "WHERE v.estado = pe.edu.upn.bodega.entity.EstadoVenta.EMITIDA "
         + "  AND v.fecha BETWEEN :desde AND :hasta")
    long contarVentas(@Param("desde") LocalDateTime desde,
                      @Param("hasta") LocalDateTime hasta);

    /** Productos mas vendidos dentro de un rango de fechas (reporte). */
    @Query("SELECT d.producto.nombre, SUM(d.cantidad), SUM(d.subtotal) "
         + "FROM DetalleVenta d "
         + "WHERE d.venta.estado = pe.edu.upn.bodega.entity.EstadoVenta.EMITIDA "
         + "  AND d.venta.fecha BETWEEN :desde AND :hasta "
         + "GROUP BY d.producto.id, d.producto.nombre "
         + "ORDER BY SUM(d.cantidad) DESC")
    List<Object[]> productosMasVendidos(@Param("desde") LocalDateTime desde,
                                        @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(v) FROM Venta v")
    long contarTodas();
}
