package com.sistemasgaia.atlas.msautenticacion.services;

import com.sistemasgaia.atlas.msautenticacion.enums.TipoToken;
import com.sistemasgaia.atlas.msautenticacion.exceptions.BusinessException;
import com.sistemasgaia.atlas.msautenticacion.exceptions.ResourceNotFoundException;
import com.sistemasgaia.atlas.msautenticacion.models.TokenActivacion;
import com.sistemasgaia.atlas.msautenticacion.models.Usuario;
import com.sistemasgaia.atlas.msautenticacion.repositories.TokenActivacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servicio para la gestión del ciclo de vida de tokens de activación y recuperación.
 *
 * Responsabilidades:
 * - Generar tokens seguros (UUID v4) con expiración configurable
 * - Validar tokens (existencia, expiración, estado de uso)
 * - Invalidar tokens previos antes de generar nuevos
 * - Marcar tokens como usados después de establecer contraseña
 * - Limpieza programada de tokens expirados
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenActivacionService {

    private final TokenActivacionRepository tokenActivacionRepository;

    @Value("${atlas.activacion.token-expiration-hours:24}")
    private int tokenExpirationHours;

    /**
     * Genera un nuevo token de activación/recuperación para un usuario.
     *
     * Antes de generar, invalida todos los tokens pendientes del mismo tipo
     * para evitar que coexistan múltiples tokens activos.
     *
     * @param usuario   el usuario al que se asocia el token
     * @param tipoToken tipo de token (ACTIVACION o RECUPERACION)
     * @return el token generado
     */
    @Transactional
    public TokenActivacion generarToken(Usuario usuario, TipoToken tipoToken) {
        // Invalidar tokens previos del mismo tipo para este usuario
        int invalidados = tokenActivacionRepository.invalidarTokensPendientes(
                usuario.getId(), tipoToken, LocalDateTime.now()
        );
        if (invalidados > 0) {
            log.debug("Se invalidaron {} tokens previos de tipo {} para usuario: {}",
                    invalidados, tipoToken, usuario.getNombreUsuario());
        }

        // Generar nuevo token seguro
        TokenActivacion tokenActivacion = TokenActivacion.builder()
                .token(UUID.randomUUID().toString())
                .tipoToken(tipoToken)
                .usuario(usuario)
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusHours(tokenExpirationHours))
                .usado(false)
                .build();

        tokenActivacion = tokenActivacionRepository.save(tokenActivacion);
        log.info("Token de {} generado para usuario: {} (expira en {} horas)",
                tipoToken, usuario.getNombreUsuario(), tokenExpirationHours);

        return tokenActivacion;
    }

    /**
     * Valida un token y retorna la entidad si es válido.
     *
     * @param token el valor del token a validar
     * @return la entidad TokenActivacion si el token es válido
     * @throws ResourceNotFoundException si el token no existe
     * @throws BusinessException si el token ya fue usado o ha expirado
     */
    @Transactional(readOnly = true)
    public TokenActivacion validarToken(String token) {
        TokenActivacion tokenActivacion = tokenActivacionRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token", "valor", token));

        if (tokenActivacion.getUsado()) {
            throw new BusinessException("Este enlace ya fue utilizado. Solicite uno nuevo si es necesario");
        }

        if (tokenActivacion.isExpirado()) {
            throw new BusinessException("Este enlace ha expirado. Solicite uno nuevo");
        }

        return tokenActivacion;
    }

    /**
     * Marca un token como usado después de que el usuario establece su contraseña.
     *
     * @param tokenActivacion el token a marcar como usado
     */
    @Transactional
    public void marcarComoUsado(TokenActivacion tokenActivacion) {
        tokenActivacion.marcarComoUsado();
        tokenActivacionRepository.save(tokenActivacion);
        log.info("Token de {} usado por usuario: {}",
                tokenActivacion.getTipoToken(), tokenActivacion.getUsuario().getNombreUsuario());
    }

    /**
     * Limpieza programada de tokens expirados y usados.
     * Se ejecuta cada 6 horas para mantener la tabla limpia.
     */
    @Scheduled(fixedRate = 21600000) // Cada 6 horas
    @Transactional
    public void limpiarTokensExpirados() {
        int eliminados = tokenActivacionRepository.eliminarTokensExpiradosUsados(LocalDateTime.now());
        if (eliminados > 0) {
            log.info("Limpieza de tokens: {} tokens expirados y usados eliminados", eliminados);
        }
    }
}
