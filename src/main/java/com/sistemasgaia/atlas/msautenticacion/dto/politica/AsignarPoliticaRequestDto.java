package com.sistemasgaia.atlas.msautenticacion.dto.politica;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * DTO de request para asignar una sola política a un rol.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignarPoliticaRequestDto {

    @NotNull(message = "El ID de la política es obligatorio")
    private UUID politicaId;
}
