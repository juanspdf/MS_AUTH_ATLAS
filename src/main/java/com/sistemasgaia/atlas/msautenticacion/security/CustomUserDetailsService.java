package com.sistemasgaia.atlas.msautenticacion.security;

import com.sistemasgaia.atlas.msautenticacion.models.Usuario;
import com.sistemasgaia.atlas.msautenticacion.repositories.UsuarioRepository;
import com.sistemasgaia.atlas.msautenticacion.services.AutorizacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación personalizada de UserDetailsService con soporte RBAC completo.
 *
 * Carga los datos del usuario desde la base de datos y construye las authorities
 * con el siguiente esquema:
 *
 * 1. ROLE_<tipo_rol> → para hasRole('ADMIN')
 * 2. <nombre_politica> → para hasAuthority('CREAR_POLITICA')
 *
 * Esto permite usar en @PreAuthorize:
 *   hasRole('ADMIN') and hasAuthority('CREAR_POLITICA')
 *
 * Spring Security internamente prefija "ROLE_" para hasRole(),
 * mientras que hasAuthority() busca el valor exacto.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final AutorizacionService autorizacionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombreUsuarioAndActivoTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + username));

        // Construir lista de authorities: ROL + POLÍTICAS
        List<GrantedAuthority> authorities = new ArrayList<>();

        // 1. Agregar el rol como authority (con prefijo ROLE_ para hasRole())
        String rolAuthority = "ROLE_" + usuario.getRol().getTipoRol().name();
        authorities.add(new SimpleGrantedAuthority(rolAuthority));

        // 2. Agregar cada política como authority individual (para hasAuthority())
        List<String> politicas = autorizacionService.obtenerPoliticasPorRolId(usuario.getRolId());
        for (String politica : politicas) {
            authorities.add(new SimpleGrantedAuthority(politica));
        }

        log.debug("Usuario '{}' cargado con authorities: {}", username, authorities);

        return new User(
                usuario.getNombreUsuario(),
                usuario.getContrasenia(),
                authorities
        );
    }
}
