package com.sistemasgaia.atlas.msautenticacion.dto.auth;

import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * DTO para la respuesta de autenticación exitosa.
 * Incluye el token JWT y la información esencial del usuario autenticado.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {

    private String token;
    private String tipo;
    private UUID idUsuario;
    private String nombreUsuario;
    private String nombreCompleto;
    private String rol;
    private List<String> politicas;
}
