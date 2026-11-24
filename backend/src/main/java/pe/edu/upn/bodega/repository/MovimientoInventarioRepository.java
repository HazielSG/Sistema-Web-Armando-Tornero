/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : MovimientoInventarioRepository.java
 * Descripcion   : Repositorio de acceso a datos de los movimientos de inventario.
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upn.bodega.entity.MovimientoInventario;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findTop50ByOrderByFechaDesc();

    List<MovimientoInventario> findByProductoIdOrderByFechaDesc(Long idProducto);

    @Query("SELECT m FROM MovimientoInventario m "
         + "WHERE m.fecha BETWEEN :desde AND :hasta "
         + "ORDER BY m.fecha DESC")
    List<MovimientoInventario> listarPorRango(@Param("desde") LocalDateTime desde,
                                              @Param("hasta") LocalDateTime hasta);
}
