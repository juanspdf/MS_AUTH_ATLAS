package com.sistemasgaia.atlas.msautenticacion.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO para solicitar la recuperación de contraseña.
 * El usuario proporciona su correo electrónico y recibe un enlace para restablecer su contraseña.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecuperarContraseniaRequestDto {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    private String correo;
}
