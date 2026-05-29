package com.sistemasgaia.atlas.msautenticacion.dto.auth;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para el registro de un nuevo usuario.
 * Confirma la creación del usuario y el envío del correo de activación.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroResponseDto {

    private UUID idUsuario;
    private String nombreUsuario;
    private String correo;
    private String nombreCompleto;
    private String rol;
    private String mensaje;
    private LocalDateTime fechaCreacion;
}
