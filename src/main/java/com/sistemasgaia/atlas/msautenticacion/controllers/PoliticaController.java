package com.sistemasgaia.atlas.msautenticacion.controllers;

import com.sistemasgaia.atlas.msautenticacion.dto.ApiResponseDto;
import com.sistemasgaia.atlas.msautenticacion.dto.politica.AsignarPoliticaRequestDto;
import com.sistemasgaia.atlas.msautenticacion.dto.politica.PoliticaResponseDto;
import com.sistemasgaia.atlas.msautenticacion.services.PoliticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para gestión de Políticas (permisos RBAC).
 *
 * Las políticas NO tienen CRUD desde API — solo se listan, asignan y desasignan.
 * La asignación/desasignación es por ROL (no por usuario).
 *
 * Endpoints:
 * - GET  /api/politicas                                → Listar todas las políticas activas
 * - GET  /api/politicas/rol/{rolId}                    → Listar políticas asignadas a un rol
 * - POST /api/politicas/rol/{rolId}/asignar            → Asignar política a un rol
 * - DELETE /api/politicas/rol/{rolId}/desasignar/{politicaId} → Desasignar política de un rol
 */
@Slf4j
@RestController
@RequestMapping("/api/politicas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Políticas", description = "Listado y gestión de asignación de permisos RBAC por rol")
public class PoliticaController {

    private final PoliticaService politicaService;

    /**
     * Lista todas las políticas activas del sistema.
     * Requiere: autenticación (cualquier usuario autenticado).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar políticas",
            description = "Obtiene todas las políticas activas del sistema. Requiere ROLE_ADMIN")
    public ResponseEntity<ApiResponseDto<List<PoliticaResponseDto>>> listarTodas() {
        log.info("Request: GET /api/politicas");
        List<PoliticaResponseDto> politicas = politicaService.listarTodas();
        return ResponseEntity.ok(
                ApiResponseDto.success(politicas, "Políticas obtenidas correctamente"));
    }

    /**
     * Lista las políticas asignadas a un rol específico.
     * Requiere: ROLE_ADMIN.
     */
    @GetMapping("/rol/{rolId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar políticas por rol",
            description = "Obtiene las políticas asignadas a un rol específico. Requiere ROLE_ADMIN")
    public ResponseEntity<ApiResponseDto<List<PoliticaResponseDto>>> listarPorRol(
            @PathVariable Integer rolId) {
        log.info("Request: GET /api/politicas/rol/{}", rolId);
        List<PoliticaResponseDto> politicas = politicaService.listarPorRol(rolId);
        return ResponseEntity.ok(
                ApiResponseDto.success(politicas, "Políticas del rol obtenidas correctamente"));
    }

    /**
     * Asigna una política a un rol.
     * Requiere: ROLE_ADMIN.
     */
    @PostMapping("/rol/{rolId}/asignar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Asignar política a rol",
            description = "Asigna una política específica a un rol. Requiere ROLE_ADMIN")
    public ResponseEntity<ApiResponseDto<Void>> asignarPoliticaARol(
            @PathVariable Integer rolId,
            @Valid @RequestBody AsignarPoliticaRequestDto request) {
        log.info("Request: POST /api/politicas/rol/{}/asignar | Política: {}", rolId, request.getPoliticaId());
        politicaService.asignarPoliticaARol(rolId, request.getPoliticaId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.created(null, "Política asignada correctamente al rol"));
    }

    /**
     * Desasigna una política de un rol.
     * Requiere: ROLE_ADMIN.
     */
    @DeleteMapping("/rol/{rolId}/desasignar/{politicaId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desasignar política de rol",
            description = "Desasigna una política específica de un rol. Requiere ROLE_ADMIN")
    public ResponseEntity<ApiResponseDto<Void>> desasignarPoliticaDeRol(
            @PathVariable Integer rolId,
            @PathVariable UUID politicaId) {
        log.info("Request: DELETE /api/politicas/rol/{}/desasignar/{}", rolId, politicaId);
        politicaService.desasignarPoliticaDeRol(rolId, politicaId);
        return ResponseEntity.ok(
                ApiResponseDto.success(null, "Política desasignada correctamente del rol"));
    }
}
