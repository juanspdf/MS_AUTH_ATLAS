package com.sistemasgaia.atlas.msautenticacion.dto.auth;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para la validación de un token de activación/recuperación.
 * Permite al frontend saber si el token es válido antes de mostrar el formulario.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidarTokenResponseDto {

    private boolean valido;
    private String tipoToken;
    private String nombreUsuario;
    private String correo;
    private LocalDateTime fechaExpiracion;
    private String mensaje;
}
