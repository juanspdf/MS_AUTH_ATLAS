package com.sistemasgaia.atlas.msautenticacion.dto.politica;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * DTO de request para asignar políticas a un usuario.
 *
 * Las políticas se asignan al ROL del usuario, no directamente al usuario.
 * Se valida que la lista de IDs no esté vacía.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignarPoliticasRequestDto {

    @NotEmpty(message = "Debe proporcionar al menos un ID de política")
    private List<UUID> politicasIds;
}
