package com.sistemasgaia.atlas.msautenticacion.repositories;

import com.sistemasgaia.atlas.msautenticacion.enums.TipoRol;
import com.sistemasgaia.atlas.msautenticacion.models.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Rol.
 */
@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {

    /**
     * Busca un rol por su tipo (ADMIN, CONSULTOR, GESTOR_NORMAS).
     */
    Optional<Rol> findByTipoRol(TipoRol tipoRol);

    /**
     * Busca todos los roles excepto el tipo especificado.
     */
    List<Rol> findByTipoRolNot(TipoRol tipoRol);
}
