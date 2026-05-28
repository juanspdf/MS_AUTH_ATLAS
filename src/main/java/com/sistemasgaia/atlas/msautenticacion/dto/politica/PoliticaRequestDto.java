package com.sistemasgaia.atlas.msautenticacion.dto.politica;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO de request para crear o actualizar una política.
 *
 * Validaciones:
 * - nombrePolitica: obligatorio, máximo 24 caracteres, solo letras mayúsculas y guiones bajos
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoliticaRequestDto {

    @NotBlank(message = "El nombre de la política es obligatorio")
    @Size(max = 24, message = "El nombre de la política no debe exceder 24 caracteres")
    @Pattern(regexp = "^[A-Z_]+$", message = "El nombre de la política solo debe contener letras mayúsculas y guiones bajos (ej: CREAR_POLITICA)")
    private String nombrePolitica;
}
