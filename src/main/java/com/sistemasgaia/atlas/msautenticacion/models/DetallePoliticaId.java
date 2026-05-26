package com.sistemasgaia.atlas.msautenticacion.models;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Clave compuesta para la entidad DetallePolitica.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DetallePoliticaId implements Serializable {

    private UUID politicaId;
    private Integer rolId;
}
