package com.sistemasgaia.atlas.msautenticacion.services;

import com.sistemasgaia.atlas.msautenticacion.models.TokenInvalidado;
import com.sistemasgaia.atlas.msautenticacion.repositories.TokenInvalidadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Servicio para la gestión de tokens JWT invalidados (blacklist).
 *
 * Responsabilidades:
 * - Invalidar tokens (agregarlos a la blacklist)
 * - Verificar si un token está invalidado
 * - Limpieza automática de tokens expirados (cada hora)
 *
 * Seguridad: Los tokens se almacenan como hash SHA-256,
 * nunca se persiste el token JWT en texto plano.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenInvalidadoService {

    private final TokenInvalidadoRepository tokenInvalidadoRepository;

    /**
     * Invalida un token JWT agregándolo a la blacklist.
     *
     * @param token           el token JWT en texto plano
     * @param nombreUsuario   username del usuario que hace logout
     * @param fechaExpiracion fecha de expiración original del token
     */
    @Transactional
    public void invalidarToken(String token, String nombreUsuario, LocalDateTime fechaExpiracion) {
        String tokenHash = hashToken(token);

        // Evitar duplicados si el usuario intenta logout múltiples veces con el mismo token
        if (tokenInvalidadoRepository.existsByTokenHash(tokenHash)) {
            log.debug("Token ya se encuentra invalidado para usuario: {}", nombreUsuario);
            return;
        }

        TokenInvalidado tokenInvalidado = TokenInvalidado.builder()
                .tokenHash(tokenHash)
                .nombreUsuario(nombreUsuario)
                .fechaInvalidacion(LocalDateTime.now())
                .fechaExpiracion(fechaExpiracion)
                .build();

        tokenInvalidadoRepository.save(tokenInvalidado);
        log.info("Token invalidado exitosamente para usuario: {}", nombreUsuario);
    }

    /**
     * Verifica si un token JWT se encuentra en la blacklist.
     *
     * @param token el token JWT en texto plano
     * @return true si el token está invalidado
     */
    @Transactional(readOnly = true)
    public boolean isTokenInvalidado(String token) {
        String tokenHash = hashToken(token);
        return tokenInvalidadoRepository.existsByTokenHash(tokenHash);
    }

    /**
     * Limpieza programada de tokens expirados.
     * Se ejecuta cada hora para mantener la tabla limpia.
     *
     * Los tokens expirados ya no pueden ser usados de todas formas,
     * por lo que se eliminan de la blacklist para optimizar el rendimiento.
     */
    @Scheduled(fixedRate = 3600000) // Cada 1 hora
    @Transactional
    public void limpiarTokensExpirados() {
        int eliminados = tokenInvalidadoRepository.deleteTokensExpirados(LocalDateTime.now());
        if (eliminados > 0) {
            log.info("Limpieza de blacklist: {} tokens expirados eliminados", eliminados);
        }
    }

    /**
     * Genera un hash SHA-256 del token JWT.
     * Se almacena el hash en lugar del token por seguridad.
     *
     * @param token el token JWT en texto plano
     * @return hash SHA-256 en formato hexadecimal (64 caracteres)
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 siempre está disponible en la JVM, esto nunca debería ocurrir
            throw new IllegalStateException("Algoritmo SHA-256 no disponible", e);
        }
    }
}
