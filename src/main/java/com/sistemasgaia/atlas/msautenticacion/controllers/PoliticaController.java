package com.sistemasgaia.atlas.msautenticacion.controllers;

import com.sistemasgaia.atlas.msautenticacion.dto.ApiResponseDto;
import com.sistemasgaia.atlas.msautenticacion.dto.politica.AsignarPoliticasRequestDto;
import com.sistemasgaia.atlas.msautenticacion.dto.politica.AsignarPoliticasResponseDto;
import com.sistemasgaia.atlas.msautenticacion.dto.politica.PoliticaRequestDto;
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
 * Controlador REST para la gestión de Políticas (permisos RBAC).
 *
 * Todos los endpoints requieren:
 * - Token JWT válido
 * - ROLE_ADMIN
 * - Política específica según la operación
 *
 * Seguridad implementada con @PreAuthorize (hasRole + hasAuthority).
 */
@Slf4j
@RestController
@RequestMapping("/api/politicas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Políticas", description = "CRUD de políticas y gestión de permisos RBAC")
public class PoliticaController {

    private final PoliticaService politicaService;

    /**
     * Lista todas las políticas del sistema.
     * Requiere: ROLE_ADMIN + VER_POLITICAS
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('VER_POLITICAS')")
    @Operation(summary = "Listar políticas",
            description = "Obtiene todas las políticas registradas. Requiere ROLE_ADMIN y permiso VER_POLITICAS")
    public ResponseEntity<ApiResponseDto<List<PoliticaResponseDto>>> listarTodas() {
        log.info("Request: GET /api/politicas");
        List<PoliticaResponseDto> politicas = politicaService.listarTodas();
        return ResponseEntity.ok(
                ApiResponseDto.success(politicas, "Políticas obtenidas correctamente"));
    }

    /**
     * Busca una política por su ID.
     * Requiere: ROLE_ADMIN + VER_POLITICAS
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('VER_POLITICAS')")
    @Operation(summary = "Buscar política por ID",
            description = "Obtiene una política específica. Requiere ROLE_ADMIN y permiso VER_POLITICAS")
    public ResponseEntity<ApiResponseDto<PoliticaResponseDto>> buscarPorId(@PathVariable UUID id) {
        log.info("Request: GET /api/politicas/{}", id);
        PoliticaResponseDto politica = politicaService.buscarPorId(id);
        return ResponseEntity.ok(
                ApiResponseDto.success(politica, "Política encontrada"));
    }

    /**
     * Crea una nueva política.
     * Requiere: ROLE_ADMIN + CREAR_POLITICA
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('CREAR_POLITICA')")
    @Operation(summary = "Crear política",
            description = "Crea una nueva política/permiso. Requiere ROLE_ADMIN y permiso CREAR_POLITICA")
    public ResponseEntity<ApiResponseDto<PoliticaResponseDto>> crear(
            @Valid @RequestBody PoliticaRequestDto request) {
        log.info("Request: POST /api/politicas | Nombre: {}", request.getNombrePolitica());
        PoliticaResponseDto politica = politicaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.created(politica, "Política creada correctamente"));
    }

    /**
     * Actualiza una política existente.
     * Requiere: ROLE_ADMIN + EDITAR_POLITICA
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('EDITAR_POLITICA')")
    @Operation(summary = "Actualizar política",
            description = "Modifica una política existente. Requiere ROLE_ADMIN y permiso EDITAR_POLITICA")
    public ResponseEntity<ApiResponseDto<PoliticaResponseDto>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PoliticaRequestDto request) {
        log.info("Request: PUT /api/politicas/{} | Nombre: {}", id, request.getNombrePolitica());
        PoliticaResponseDto politica = politicaService.actualizar(id, request);
        return ResponseEntity.ok(
                ApiResponseDto.success(politica, "Política actualizada correctamente"));
    }

    /**
     * Elimina (soft delete) una política.
     * Requiere: ROLE_ADMIN + ELIMINAR_POLITICA
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ELIMINAR_POLITICA')")
    @Operation(summary = "Eliminar política",
            description = "Desactiva una política (soft delete). Requiere ROLE_ADMIN y permiso ELIMINAR_POLITICA")
    public ResponseEntity<ApiResponseDto<Void>> eliminar(@PathVariable UUID id) {
        log.info("Request: DELETE /api/politicas/{}", id);
        politicaService.eliminar(id);
        return ResponseEntity.ok(
                ApiResponseDto.success(null, "Política eliminada correctamente"));
    }

    /**
     * Asigna políticas al ROL del usuario especificado.
     * Requiere: ROLE_ADMIN + ASIGNAR_POLITICAS
     *
     * Las políticas se asignan al ROL, no directamente al usuario.
     * Se evitan duplicados automáticamente.
     */
    @PostMapping("/usuarios/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ASIGNAR_POLITICAS')")
    @Operation(summary = "Asignar políticas a usuario",
            description = "Asigna políticas al ROL del usuario. Requiere ROLE_ADMIN y permiso ASIGNAR_POLITICAS")
    public ResponseEntity<ApiResponseDto<AsignarPoliticasResponseDto>> asignarPoliticas(
            @PathVariable UUID usuarioId,
            @Valid @RequestBody AsignarPoliticasRequestDto request) {
        log.info("Request: POST /api/politicas/usuarios/{} | Políticas: {}", usuarioId, request.getPoliticasIds().size());
        AsignarPoliticasResponseDto response = politicaService.asignarPoliticas(usuarioId, request);
        return ResponseEntity.ok(
                ApiResponseDto.success(response, "Políticas asignadas correctamente"));
    }
}
