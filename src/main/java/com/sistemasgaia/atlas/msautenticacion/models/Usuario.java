package com.sistemasgaia.atlas.msautenticacion.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa un usuario del sistema.
 * Esquema: usr
 */
@Entity
@Table(name = "usuarios", schema = "usr")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_usuario", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "nombre_usuario", nullable = false, unique = true, length = 10)
    private String nombreUsuario;

    @Column(name = "contrasenia", nullable = false, length = 255)
    private String contrasenia;

    @Column(name = "nombre", nullable = false, length = 40)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 40)
    private String apellido;

    @Column(name = "rol_id", nullable = false)
    private Integer rolId;

    @Column(name = "ultima_evaluacion")
    private LocalDateTime ultimaEvaluacion;

    @Column(name = "ultima_modificacion")
    private LocalDateTime ultimaModificacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id", insertable = false, updatable = false)
    private Rol rol;
}
