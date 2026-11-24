/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : CierreCajaRepository.java
 * Descripcion   : Repositorio de acceso a datos del cierre de caja.
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upn.bodega.entity.CierreCaja;
import pe.edu.upn.bodega.entity.EstadoCaja;

import java.util.List;
import java.util.Optional;

public interface CierreCajaRepository extends JpaRepository<CierreCaja, Long> {

    Optional<CierreCaja> findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(Long idUsuario,
                                                                              EstadoCaja estado);

    List<CierreCaja> findTop30ByOrderByFechaAperturaDesc();
}
