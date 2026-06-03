package com.sistemasgaia.atlas.msautenticacion.dto.rol;

import lombok.*;

/**
 * DTO de respuesta para representar un rol asignable.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolResponseDto {

    private Integer id;
    private String tipoRol;
    private String descripcionRol;
}
