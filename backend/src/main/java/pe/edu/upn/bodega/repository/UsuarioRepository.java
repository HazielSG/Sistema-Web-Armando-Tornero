/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : UsuarioRepository.java
 * Descripcion   : Repositorio de acceso a datos de la entidad Usuario.
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upn.bodega.entity.Rol;
import pe.edu.upn.bodega.entity.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    List<Usuario> findByActivoTrueOrderByApellidosAsc();

    List<Usuario> findByRol(Rol rol);
}
