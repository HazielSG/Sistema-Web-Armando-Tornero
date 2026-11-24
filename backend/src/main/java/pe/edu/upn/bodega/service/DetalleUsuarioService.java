/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : DetalleUsuarioService.java
 * Descripcion   : Implementacion de UserDetailsService: entrega a Spring Security las credenciales y el rol del usuario almacenado en MySQL (RF-01, RF-03).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pe.edu.upn.bodega.entity.Usuario;
import pe.edu.upn.bodega.repository.UsuarioRepository;

import java.util.List;

@Service
public class DetalleUsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public DetalleUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("El usuario indicado no existe."));

        return User.withUsername(usuario.getUsername())
                .password(usuario.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())))
                .accountLocked(false)
                .disabled(Boolean.FALSE.equals(usuario.getActivo()))
                .build();
    }
}
