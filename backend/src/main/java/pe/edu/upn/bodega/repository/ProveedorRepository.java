/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : ProveedorRepository.java
 * Descripcion   : Repositorio de acceso a datos de la entidad Proveedor.
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upn.bodega.entity.Proveedor;

import java.util.List;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    List<Proveedor> findByActivoTrueOrderByRazonSocialAsc();

    boolean existsByRuc(String ruc);
}
