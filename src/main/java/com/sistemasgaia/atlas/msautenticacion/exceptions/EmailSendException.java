package com.sistemasgaia.atlas.msautenticacion.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando el envío de correo electrónico falla.
 * Retorna HTTP 502 Bad Gateway (error en servicio SMTP externo).
 */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message) {
        super(message);
    }

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
