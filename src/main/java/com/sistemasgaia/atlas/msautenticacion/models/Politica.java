package com.sistemasgaia.atlas.msautenticacion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad que representa una política/permiso del sistema.
 * Esquema: sec
 */
@Entity
@Table(name = "politicas", schema = "sec")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Politica extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_politica", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "politica", nullable = false, length = 24)
    private String nombrePolitica;

    @OneToMany(mappedBy = "politica", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<DetallePolitica> detallesPoliticas = new ArrayList<>();
}
