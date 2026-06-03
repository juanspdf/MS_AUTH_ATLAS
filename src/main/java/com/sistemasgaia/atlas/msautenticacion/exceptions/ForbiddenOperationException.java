package com.sistemasgaia.atlas.msautenticacion.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción para operaciones prohibidas por reglas de negocio.
 * Retorna HTTP 403 Forbidden.
 *
 * Casos de uso:
 * - Un ADMIN intenta editarse o eliminarse a sí mismo
 * - Se intenta crear/asignar un usuario con rol ADMIN
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
