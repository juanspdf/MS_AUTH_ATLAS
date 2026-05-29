package com.sistemasgaia.atlas.msautenticacion.enums;

/**
 * Tipos de tokens de activación/recuperación.
 *
 * ACTIVACION  → Token enviado al registrar un nuevo usuario para que establezca su contraseña.
 * RECUPERACION → Token enviado cuando un usuario existente solicita restablecer su contraseña.
 */
public enum TipoToken {
    ACTIVACION,
    RECUPERACION
}
