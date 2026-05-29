package com.sistemasgaia.atlas.msautenticacion.repositories;

import com.sistemasgaia.atlas.msautenticacion.enums.TipoToken;
import com.sistemasgaia.atlas.msautenticacion.models.TokenActivacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la gestión de tokens de activación y recuperación de contraseña.
 */
@Repository
public interface TokenActivacionRepository extends JpaRepository<TokenActivacion, UUID> {

    /**
     * Busca un token válido (no usado) por su valor.
     */
    Optional<TokenActivacion> findByTokenAndUsadoFalse(String token);

    /**
     * Busca un token por su valor (independientemente del estado).
     */
    Optional<TokenActivacion> findByToken(String token);

    /**
     * Invalida (marca como usados) todos los tokens pendientes de un usuario para un tipo específico.
     * Se usa antes de generar un nuevo token para evitar que coexistan múltiples tokens activos.
     *
     * @param usuarioId ID del usuario
     * @param tipoToken tipo de token a invalidar
     * @return cantidad de tokens invalidados
     */
    @Modifying
    @Query("UPDATE TokenActivacion t SET t.usado = true, t.fechaUso = :ahora " +
            "WHERE t.usuario.id = :usuarioId AND t.tipoToken = :tipoToken AND t.usado = false")
    int invalidarTokensPendientes(
            @Param("usuarioId") UUID usuarioId,
            @Param("tipoToken") TipoToken tipoToken,
            @Param("ahora") LocalDateTime ahora
    );

    /**
     * Elimina tokens expirados y usados para mantener la tabla limpia.
     *
     * @param fechaLimite fecha antes de la cual se eliminan tokens expirados
     * @return cantidad de registros eliminados
     */
    @Modifying
    @Query("DELETE FROM TokenActivacion t WHERE t.fechaExpiracion < :fechaLimite AND t.usado = true")
    int eliminarTokensExpiradosUsados(@Param("fechaLimite") LocalDateTime fechaLimite);
}
