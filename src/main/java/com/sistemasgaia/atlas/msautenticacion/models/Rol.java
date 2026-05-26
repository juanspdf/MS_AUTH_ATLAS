package com.sistemasgaia.atlas.msautenticacion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sistemasgaia.atlas.msautenticacion.enums.TipoRol;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un rol de seguridad.
 * Esquema: sec
 */
@Entity
@Table(name = "roles", schema = "sec")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rol_id")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_rol", nullable = false, length = 20)
    private TipoRol tipoRol;

    @Column(name = "descripcion_rol", length = 100)
    private String descripcionRol;

    @OneToMany(mappedBy = "rol", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Usuario> usuarios = new ArrayList<>();

    @OneToMany(mappedBy = "rol", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<DetallePolitica> detallesPoliticas = new ArrayList<>();
}
