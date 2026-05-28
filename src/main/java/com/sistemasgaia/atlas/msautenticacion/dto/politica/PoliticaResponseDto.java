package com.sistemasgaia.atlas.msautenticacion.dto.politica;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para representar una política.
 * Incluye información de auditoría.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoliticaResponseDto {

    private UUID id;
    private String nombrePolitica;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
