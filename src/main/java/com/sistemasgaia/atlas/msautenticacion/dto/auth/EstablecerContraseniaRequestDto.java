package com.sistemasgaia.atlas.msautenticacion.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO para establecer o cambiar contraseña mediante un token de activación/recuperación.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstablecerContraseniaRequestDto {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 255, message = "La contraseña debe tener entre 8 y 255 caracteres")
    private String nuevaContrasenia;

    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    @Size(min = 8, max = 255, message = "La confirmación debe tener entre 8 y 255 caracteres")
    private String confirmarContrasenia;
}
