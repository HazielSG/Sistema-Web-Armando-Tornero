/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : AuditoriaRepository.java
 * Descripcion   : Repositorio de acceso a datos de la bitacora de auditoria.
 *                 El filtrado dinamico (RF-20) se construye con
 *                 Specifications desde la capa de servicio.
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upn.bodega.entity.Auditoria;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long>,
                                             JpaSpecificationExecutor<Auditoria> {

    List<Auditoria> findTop10ByOrderByFechaHoraDesc();

    @Query("SELECT a.accion, COUNT(a) FROM Auditoria a "
         + "WHERE a.fechaHora BETWEEN :desde AND :hasta "
         + "GROUP BY a.accion ORDER BY COUNT(a) DESC")
    List<Object[]> resumenPorAccion(@Param("desde") LocalDateTime desde,
                                    @Param("hasta") LocalDateTime hasta);

    @Query("SELECT DISTINCT a.entidad FROM Auditoria a ORDER BY a.entidad ASC")
    List<String> listarEntidades();

    @Query("SELECT DISTINCT a.username FROM Auditoria a ORDER BY a.username ASC")
    List<String> listarUsuarios();
}
