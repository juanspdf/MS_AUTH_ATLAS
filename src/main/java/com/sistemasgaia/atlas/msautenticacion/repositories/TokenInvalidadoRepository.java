package com.sistemasgaia.atlas.msautenticacion.repositories;

import com.sistemasgaia.atlas.msautenticacion.models.TokenInvalidado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Repositorio JPA para la gestión de tokens JWT invalidados (blacklist).
 *
 * Operaciones principales:
 * - Verificar si un token está en la blacklist (por hash)
 * - Eliminar tokens expirados para mantener la tabla limpia
 */
@Repository
public interface TokenInvalidadoRepository extends JpaRepository<TokenInvalidado, UUID> {

    /**
     * Verifica si un token (por su hash) se encuentra en la blacklist.
     *
     * @param tokenHash hash SHA-256 del token JWT
     * @return true si el token está invalidado
     */
    boolean existsByTokenHash(String tokenHash);

    /**
     * Elimina todos los tokens cuya fecha de expiración ya pasó.
     * Esto permite mantener la tabla limpia y evitar crecimiento indefinido.
     *
     * @param fechaLimite fecha a partir de la cual se eliminan tokens expirados
     * @return cantidad de registros eliminados
     */
    @Modifying
    @Query("DELETE FROM TokenInvalidado t WHERE t.fechaExpiracion < :fechaLimite")
    int deleteTokensExpirados(@Param("fechaLimite") LocalDateTime fechaLimite);
}
