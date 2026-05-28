package com.sistemasgaia.atlas.msautenticacion.dto.politica;

import lombok.*;

import java.util.List;

/**
 * DTO de respuesta para la operación de asignación de políticas.
 *
 * Informa al cliente:
 * - Cuántas políticas fueron asignadas exitosamente
 * - Cuáles ya estaban asignadas (duplicados evitados)
 * - A qué rol se asignaron
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignarPoliticasResponseDto {

    private String nombreUsuario;
    private String tipoRol;
    private int politicasAsignadas;
    private int politicasDuplicadas;
    private List<String> politicasNuevas;
    private List<String> politicasYaExistentes;
}
