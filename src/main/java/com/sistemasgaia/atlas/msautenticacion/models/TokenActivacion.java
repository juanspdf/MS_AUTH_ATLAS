package com.sistemasgaia.atlas.msautenticacion.models;

import com.sistemasgaia.atlas.msautenticacion.enums.TipoToken;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa un token de activación o recuperación de contraseña.
 *
 * Ciclo de vida:
 * 1. Se crea al registrar usuario (ACTIVACION) o al solicitar recuperación (RECUPERACION)
 * 2. Se envía por correo electrónico como parte de un enlace seguro
 * 3. Se valida al acceder al enlace (verificación de expiración y estado)
 * 4. Se marca como usado al establecer la nueva contraseña
 *
 * Seguridad:
 * - Tokens UUID v4 (criptográficamente seguros)
 * - Expiración configurable (default: 24 horas)
 * - Single-use: se invalidan después del primer uso
 *
 * Esquema: usr
 */
@Entity
@Table(name = "tokens_activacion", schema = "usr", indexes = {
        @Index(name = "idx_token_activacion_token", columnList = "token", unique = true),
        @Index(name = "idx_token_activacion_usuario", columnList = "id_usuario"),
        @Index(name = "idx_token_activacion_expiracion", columnList = "fecha_expiracion")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenActivacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_token_activacion", columnDefinition = "uuid")
    private UUID id;

    /**
     * Token único seguro enviado por correo electrónico.
     * Se genera con UUID v4 para garantizar unicidad y seguridad criptográfica.
     */
    @Column(name = "token", nullable = false, unique = true, length = 36)
    private String token;

    /**
     * Tipo de token: ACTIVACION (registro nuevo) o RECUPERACION (reset password).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_token", nullable = false, length = 20)
    private TipoToken tipoToken;

    /**
     * Usuario al que pertenece este token.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    /**
     * Fecha y hora de creación del token.
     */
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Fecha y hora de expiración del token.
     */
    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    /**
     * Indica si el token ya fue utilizado.
     * Un token usado no puede volver a utilizarse.
     */
    @Column(name = "usado", nullable = false)
    @Builder.Default
    private Boolean usado = false;

    /**
     * Fecha y hora en que se usó el token (null si no ha sido usado).
     */
    @Column(name = "fecha_uso")
    private LocalDateTime fechaUso;

    /**
     * Verifica si el token ha expirado.
     */
    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(this.fechaExpiracion);
    }

    /**
     * Verifica si el token es válido (no usado y no expirado).
     */
    public boolean isValido() {
        return !this.usado && !isExpirado();
    }

    /**
     * Marca el token como usado.
     */
    public void marcarComoUsado() {
        this.usado = true;
        this.fechaUso = LocalDateTime.now();
    }
}
