package com.sistemasgaia.atlas.msautenticacion.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidad de relación muchos-a-muchos entre Política y Rol.
 * Usa clave compuesta (id_politica, rol_id).
 * Esquema: sec
 */
@Entity
@Table(name = "detalles_politicas", schema = "sec")
@IdClass(DetallePoliticaId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetallePolitica extends AuditableEntity {

    @Id
    @Column(name = "id_politica")
    private UUID politicaId;

    @Id
    @Column(name = "rol_id")
    private Integer rolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_politica", insertable = false, updatable = false)
    private Politica politica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", insertable = false, updatable = false)
    private Rol rol;
}
