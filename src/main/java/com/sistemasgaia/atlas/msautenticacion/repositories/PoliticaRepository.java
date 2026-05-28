package com.sistemasgaia.atlas.msautenticacion.repositories;

import com.sistemasgaia.atlas.msautenticacion.models.Politica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad Politica.
 */
@Repository
public interface PoliticaRepository extends JpaRepository<Politica, UUID> {

    /**
     * Verifica si existe una política con el nombre dado.
     * Se utiliza para validar unicidad al crear/actualizar.
     */
    boolean existsByNombrePolitica(String nombrePolitica);

    /**
     * Busca una política por su nombre exacto.
     */
    Optional<Politica> findByNombrePolitica(String nombrePolitica);
}
