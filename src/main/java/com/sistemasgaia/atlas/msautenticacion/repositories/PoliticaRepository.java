package com.sistemasgaia.atlas.msautenticacion.repositories;

import com.sistemasgaia.atlas.msautenticacion.models.Politica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositorio JPA para la entidad Politica.
 */
@Repository
public interface PoliticaRepository extends JpaRepository<Politica, UUID> {
}
