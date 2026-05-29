package com.sistemasgaia.atlas.msautenticacion.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa un token JWT invalidado (blacklisted).
 *
 * Almacena los tokens que han sido revocados mediante logout.
 * Los tokens expirados se limpian automáticamente mediante un proceso programado.
 *
 * Esquema: usr
 */
@Entity
@Table(name = "tokens_invalidados", schema = "usr", indexes = {
        @Index(name = "idx_token_invalidado_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_token_invalidado_expiracion", columnList = "fecha_expiracion")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenInvalidado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_token_invalidado", columnDefinition = "uuid")
    private UUID id;

    /**
     * Hash SHA-256 del token JWT.
     * Se almacena el hash en lugar del token completo por seguridad.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /**
     * Username del usuario que realizó el logout.
     */
    @Column(name = "nombre_usuario", nullable = false, length = 10)
    private String nombreUsuario;

    /**
     * Fecha y hora en que se invalidó el token (logout).
     */
    @Column(name = "fecha_invalidacion", nullable = false)
    private LocalDateTime fechaInvalidacion;

    /**
     * Fecha y hora de expiración original del token JWT.
     * Se usa para la limpieza automática de registros expirados.
     */
    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;
}
