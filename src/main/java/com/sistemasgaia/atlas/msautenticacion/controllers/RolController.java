package com.sistemasgaia.atlas.msautenticacion.controllers;

import com.sistemasgaia.atlas.msautenticacion.dto.ApiResponseDto;
import com.sistemasgaia.atlas.msautenticacion.dto.rol.RolResponseDto;
import com.sistemasgaia.atlas.msautenticacion.services.RolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para consultar roles asignables.
 * Excluye el rol ADMIN del listado (Regla 1).
 */
@Slf4j
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Roles", description = "Consulta de roles asignables del sistema")
public class RolController {

    private final RolService rolService;

    /**
     * Lista todos los roles asignables (sin ADMIN).
     * Requiere: autenticación con JWT válido.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar roles asignables",
            description = "Obtiene todos los roles excepto ADMIN. Requiere ROLE_ADMIN")
    public ResponseEntity<ApiResponseDto<List<RolResponseDto>>> listarRolesAsignables() {
        log.info("Request: GET /api/roles");
        List<RolResponseDto> roles = rolService.listarRolesAsignables();
        return ResponseEntity.ok(
                ApiResponseDto.success(roles, "Roles obtenidos correctamente"));
    }
}
