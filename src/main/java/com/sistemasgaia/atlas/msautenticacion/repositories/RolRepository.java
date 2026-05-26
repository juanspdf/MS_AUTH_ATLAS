package com.sistemasgaia.atlas.msautenticacion.repositories;

import com.sistemasgaia.atlas.msautenticacion.models.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para la entidad Rol.
 */
@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {
}
