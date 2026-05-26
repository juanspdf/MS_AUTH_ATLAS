package com.sistemasgaia.atlas.msautenticacion.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO para la creación y actualización de usuarios.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioRequestDto {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 10, message = "El nombre de usuario no puede exceder 10 caracteres")
    private String nombreUsuario;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 255, message = "La contraseña debe tener entre 6 y 255 caracteres")
    private String contrasenia;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 40, message = "El nombre no puede exceder 40 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 40, message = "El apellido no puede exceder 40 caracteres")
    private String apellido;

    @NotNull(message = "El rol es obligatorio")
    private Integer rolId;
}
