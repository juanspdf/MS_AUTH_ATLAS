package com.sistemasgaia.atlas.msautenticacion.dto.usuario;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para representar un usuario.
 * Excluye información sensible como la contraseña.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponseDto {

    private UUID id;
    private String nombreUsuario;
    private String nombre;
    private String apellido;
    private Integer rolId;
    private String tipoRol;
    private String descripcionRol;
    private LocalDateTime ultimaEvaluacion;
    private LocalDateTime ultimaModificacion;
    private LocalDateTime fechaCreacion;
    private Boolean activo;
}
