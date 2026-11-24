/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : ProductoRepository.java
 * Descripcion   : Repositorio de acceso a datos de la entidad Producto, incluye las consultas de busqueda y de stock bajo.
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upn.bodega.entity.Producto;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Producto> findByActivoTrueOrderByNombreAsc();

    /** RF-06: consulta de productos por codigo o nombre. */
    @Query("SELECT p FROM Producto p "
         + "WHERE p.activo = true "
         + "  AND (LOWER(p.codigo) LIKE LOWER(CONCAT('%', :texto, '%')) "
         + "   OR  LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))) "
         + "ORDER BY p.nombre ASC")
    List<Producto> buscar(@Param("texto") String texto);

    /** RF-12: productos cuyo stock alcanzo el minimo configurado. */
    @Query("SELECT p FROM Producto p "
         + "WHERE p.activo = true AND p.stockActual <= p.stockMinimo "
         + "ORDER BY (p.stockActual - p.stockMinimo) ASC")
    List<Producto> listarStockBajo();

    long countByActivoTrue();

    @Query("SELECT COUNT(p) FROM Producto p WHERE p.activo = true AND p.stockActual <= p.stockMinimo")
    long contarStockBajo();
}
