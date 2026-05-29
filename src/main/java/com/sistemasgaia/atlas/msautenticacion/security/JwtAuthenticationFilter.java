package com.sistemasgaia.atlas.msautenticacion.security;

import com.sistemasgaia.atlas.msautenticacion.services.TokenInvalidadoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtro JWT empresarial con soporte RBAC completo.
 *
 * Se ejecuta UNA sola vez por request gracias a OncePerRequestFilter.
 *
 * Flujo de autenticación y autorización:
 * 1. Extrae el token del header Authorization (Bearer ...)
 * 2. Valida la firma y expiración del token
 * 3. Extrae el ROL del token → GrantedAuthority con prefijo "ROLE_"
 * 4. Extrae las POLÍTICAS del token → GrantedAuthority sin prefijo
 * 5. Establece la autenticación completa en el SecurityContext
 *
 * Ventaja: Las authorities se resuelven directamente del JWT,
 * sin consultar la base de datos en cada request.
 * El CustomUserDetailsService solo se usa durante el login.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenInvalidadoService tokenInvalidadoService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Si no hay header Authorization o no empieza con "Bearer ", continuar sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String username = jwtService.extraerUsername(jwt);

            // Si el username existe y no hay autenticación previa en el contexto
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Verificar si el token está en la blacklist (logout previo)
                if (tokenInvalidadoService.isTokenInvalidado(jwt)) {
                    log.warn("Token invalidado (logout) detectado para usuario: {}", username);
                    filterChain.doFilter(request, response);
                    return;
                }

                // Validar firma y expiración del token
                if (jwtService.validarToken(jwt, username)) {

                    // Extraer rol y políticas del JWT
                    String rol = jwtService.extraerRol(jwt);
                    List<String> politicas = jwtService.extraerPoliticas(jwt);

                    // Construir authorities desde el JWT (sin acceder a BD)
                    List<GrantedAuthority> authorities = new ArrayList<>();

                    // 1. Agregar ROL (con prefijo ROLE_ para hasRole())
                    if (rol != null && !rol.isEmpty()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + rol));
                    }

                    // 2. Agregar cada POLÍTICA como authority (para hasAuthority())
                    if (politicas != null) {
                        for (String politica : politicas) {
                            authorities.add(new SimpleGrantedAuthority(politica));
                        }
                    }

                    log.debug("JWT autenticado para usuario '{}' | Rol: {} | Políticas: {} | Authorities: {}",
                            username, rol, politicas, authorities);

                    // Crear token de autenticación con authorities completas
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    authorities
                            );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("Error procesando token JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
