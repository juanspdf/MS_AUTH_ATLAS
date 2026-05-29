package com.sistemasgaia.atlas.msautenticacion.repositories;

import com.sistemasgaia.atlas.msautenticacion.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad Usuario.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /**
     * Busca un usuario activo por su nombre de usuario.
     * Utiliza JOIN FETCH para cargar el rol en una sola consulta.
     */
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol WHERE u.nombreUsuario = :nombreUsuario AND u.activo = true")
    Optional<Usuario> findByNombreUsuarioAndActivoTrue(@Param("nombreUsuario") String nombreUsuario);

    /**
     * Busca un usuario por su nombre de usuario (sin filtrar por activo).
     */
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    /**
     * Verifica si existe un usuario con el nombre de usuario dado.
     */
    boolean existsByNombreUsuario(String nombreUsuario);

    /**
     * Verifica si existe un usuario con el correo dado.
     */
    boolean existsByCorreo(String correo);

    /**
     * Busca un usuario activo por su correo electrónico.
     */
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol WHERE u.correo = :correo AND u.activo = true")
    Optional<Usuario> findByCorreoAndActivoTrue(@Param("correo") String correo);

    /**
     * Busca un usuario por su correo electrónico (sin filtrar por activo).
     */
    Optional<Usuario> findByCorreo(String correo);
}
