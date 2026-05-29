package com.sistemasgaia.atlas.msautenticacion.services;

import com.sistemasgaia.atlas.msautenticacion.dto.auth.LoginRequestDto;
import com.sistemasgaia.atlas.msautenticacion.dto.auth.LoginResponseDto;
import com.sistemasgaia.atlas.msautenticacion.exceptions.UnauthorizedException;
import com.sistemasgaia.atlas.msautenticacion.models.Usuario;
import com.sistemasgaia.atlas.msautenticacion.repositories.DetallePoliticaRepository;
import com.sistemasgaia.atlas.msautenticacion.repositories.UsuarioRepository;
import com.sistemasgaia.atlas.msautenticacion.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de autenticación.
 *
 * Flujo de login:
 * 1. Busca el usuario activo por nombre de usuario
 * 2. Valida la contraseña con BCrypt
 * 3. Obtiene el rol del usuario
 * 4. Obtiene las políticas asociadas al rol
 * 5. Genera el JWT con todos los claims
 * 6. Retorna LoginResponseDto
 *
 * Flujo de logout:
 * 1. Extrae el token JWT del header Authorization
 * 2. Extrae username y fecha de expiración del token
 * 3. Agrega el token a la blacklist (hash SHA-256)
 * 4. El token queda invalidado para cualquier request futuro
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final DetallePoliticaRepository detallePoliticaRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final TokenInvalidadoService tokenInvalidadoService;

    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        log.info("Intento de login para usuario: {}", request.getNombreUsuario());

        // 1. Buscar usuario activo
        Usuario usuario = usuarioRepository
                .findByNombreUsuarioAndActivoTrue(request.getNombreUsuario())
                .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

        // 2. Validar contraseña
        if (!passwordEncoder.matches(request.getContrasenia(), usuario.getContrasenia())) {
            log.warn("Contraseña incorrecta para usuario: {}", request.getNombreUsuario());
            throw new UnauthorizedException("Credenciales inválidas");
        }

        // 3. Obtener rol
        String rol = usuario.getRol().getTipoRol().name();

        // 4. Obtener políticas del rol
        List<String> politicas = detallePoliticaRepository
                .findByRolIdWithPolitica(usuario.getRolId())
                .stream()
                .map(dp -> dp.getPolitica().getNombrePolitica())
                .toList();

        // 5. Generar JWT
        String token = jwtService.generarToken(
                usuario.getId(),
                usuario.getNombreUsuario(),
                rol,
                politicas
        );

        log.info("Login exitoso para usuario: {} con rol: {}", request.getNombreUsuario(), rol);

        // 6. Construir respuesta
        return LoginResponseDto.builder()
                .token(token)
                .tipo("Bearer")
                .idUsuario(usuario.getId())
                .nombreUsuario(usuario.getNombreUsuario())
                .nombreCompleto(usuario.getNombre() + " " + usuario.getApellido())
                .rol(rol)
                .politicas(politicas)
                .build();
    }

    /**
     * Cierra la sesión del usuario invalidando su token JWT.
     *
     * El token se agrega a una blacklist (tabla tokens_invalidados) y será
     * rechazado por el JwtAuthenticationFilter en cualquier request posterior.
     *
     * @param authHeader el header Authorization completo (e.g., "Bearer eyJhbGc...")
     * @throws UnauthorizedException si el header es nulo, vacío o no tiene formato Bearer
     */
    @Transactional
    public void logout(String authHeader) {
        // Validar que el header Authorization esté presente y tenga formato correcto
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Token JWT no proporcionado o formato inválido");
        }

        String token = authHeader.substring(7);

        // Extraer datos del token necesarios para la blacklist
        String username = jwtService.extraerUsername(token);
        LocalDateTime fechaExpiracion = jwtService.extraerExpiracion(token);

        // Agregar el token a la blacklist
        tokenInvalidadoService.invalidarToken(token, username, fechaExpiracion);

        log.info("Logout exitoso para usuario: {}", username);
    }
}
