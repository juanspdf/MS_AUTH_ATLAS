package com.sistemasgaia.atlas.msautenticacion.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO para la solicitud de inicio de sesión.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDto {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 10, message = "El nombre de usuario no puede exceder 10 caracteres")
    private String nombreUsuario;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 255, message = "La contraseña no puede exceder 255 caracteres")
    private String contrasenia;
}
