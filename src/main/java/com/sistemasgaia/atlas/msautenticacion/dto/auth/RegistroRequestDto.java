package com.sistemasgaia.atlas.msautenticacion.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO para la solicitud de registro de un nuevo usuario.
 *
 * El registro NO requiere contraseña: el usuario la establece
 * desde el enlace de activación enviado por correo electrónico.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroRequestDto {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 10, message = "El nombre de usuario no puede exceder 10 caracteres")
    private String nombreUsuario;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String correo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 40, message = "El nombre no puede exceder 40 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 40, message = "El apellido no puede exceder 40 caracteres")
    private String apellido;

    /**
     * Rol a asignar al usuario. Si es null, se asigna el rol por defecto (CLIENTE).
     */
    private Integer rolId;
}
