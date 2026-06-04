package com.sistemasgaia.atlas.msautenticacion.repositories;

import com.sistemasgaia.atlas.msautenticacion.models.DetallePolitica;
import com.sistemasgaia.atlas.msautenticacion.models.DetallePoliticaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad DetallePolitica.
 */
@Repository
public interface DetallePoliticaRepository extends JpaRepository<DetallePolitica, DetallePoliticaId> {

    /**
     * Obtiene los nombres de las políticas asignadas a un rol específico.
     * Usa JOIN FETCH para evitar N+1 queries.
     */
    @Query("SELECT dp FROM DetallePolitica dp JOIN FETCH dp.politica WHERE dp.rolId = :rolId")
    List<DetallePolitica> findByRolIdWithPolitica(@Param("rolId") Integer rolId);

    /**
     * Verifica si una política ya está asignada a un rol.
     */
    boolean existsByPoliticaIdAndRolId(UUID politicaId, Integer rolId);

    /**
     * Elimina la asignación de una política a un rol.
     */
    void deleteByPoliticaIdAndRolId(UUID politicaId, Integer rolId);
}
