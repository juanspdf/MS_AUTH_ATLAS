package com.sistemasgaia.atlas.msautenticacion.security;

import com.sistemasgaia.atlas.msautenticacion.models.Usuario;
import com.sistemasgaia.atlas.msautenticacion.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación personalizada de UserDetailsService.
 * Carga los datos del usuario desde la base de datos para la autenticación de Spring Security.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombreUsuarioAndActivoTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + username));

        return new User(
                usuario.getNombreUsuario(),
                usuario.getContrasenia(),
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getTipoRol().name()))
        );
    }
}
