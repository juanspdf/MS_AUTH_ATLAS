package com.sistemasgaia.atlas.msautenticacion.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO estándar para respuestas de la API.
 * Envuelve todos los responses para mantener consistencia.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseDto<T> {

    private int status;
    private String mensaje;
    private T datos;
    private List<String> errores;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponseDto<T> success(T datos, String mensaje) {
        return ApiResponseDto.<T>builder()
                .status(200)
                .mensaje(mensaje)
                .datos(datos)
                .build();
    }

    public static <T> ApiResponseDto<T> created(T datos, String mensaje) {
        return ApiResponseDto.<T>builder()
                .status(201)
                .mensaje(mensaje)
                .datos(datos)
                .build();
    }

    public static <T> ApiResponseDto<T> error(int status, String mensaje, List<String> errores) {
        return ApiResponseDto.<T>builder()
                .status(status)
                .mensaje(mensaje)
                .errores(errores)
                .build();
    }
}
