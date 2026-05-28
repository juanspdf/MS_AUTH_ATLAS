package com.sistemasgaia.atlas.msautenticacion.controllers;

import com.sistemasgaia.atlas.msautenticacion.dto.ApiResponseDto;
import com.sistemasgaia.atlas.msautenticacion.dto.politica.AsignarPoliticasRequestDto;
import com.sistemasgaia.atlas.msautenticacion.dto.politica.AsignarPoliticasResponseDto;
import com.sistemasgaia.atlas.msautenticacion.dto.usuario.UsuarioRequestDto;
import com.sistemasgaia.atlas.msautenticacion.dto.usuario.UsuarioResponseDto;
import com.sistemasgaia.atlas.msautenticacion.services.PoliticaService;
import com.sistemasgaia.atlas.msautenticacion.services.UsuarioService;
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

@Slf4j
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuarios", description = "CRUD de usuarios del sistema y asignación de políticas")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PoliticaService politicaService;

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Obtiene todos los usuarios registrados")
    public ResponseEntity<ApiResponseDto<List<UsuarioResponseDto>>> listarTodos() {
        List<UsuarioResponseDto> usuarios = usuarioService.listarTodos();
        return ResponseEntity.ok(ApiResponseDto.success(usuarios, "Usuarios obtenidos correctamente"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuario por ID")
    public ResponseEntity<ApiResponseDto<UsuarioResponseDto>> buscarPorId(@PathVariable UUID id) {
        UsuarioResponseDto usuario = usuarioService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDto.success(usuario, "Usuario encontrado"));
    }

    @PostMapping
    @Operation(summary = "Crear usuario")
    public ResponseEntity<ApiResponseDto<UsuarioResponseDto>> crear(
            @Valid @RequestBody UsuarioRequestDto request) {
        UsuarioResponseDto usuario = usuarioService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.created(usuario, "Usuario creado correctamente"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario")
    public ResponseEntity<ApiResponseDto<UsuarioResponseDto>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody UsuarioRequestDto request) {
        UsuarioResponseDto usuario = usuarioService.actualizar(id, request);
        return ResponseEntity.ok(ApiResponseDto.success(usuario, "Usuario actualizado correctamente"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario (soft delete)")
    public ResponseEntity<ApiResponseDto<Void>> eliminar(@PathVariable UUID id) {
        usuarioService.eliminar(id);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Usuario eliminado correctamente"));
    }

    /**
     * Asigna políticas al ROL del usuario especificado.
     * Endpoint: POST /api/usuarios/{usuarioId}/politicas
     *
     * Requiere: ROLE_ADMIN + ASIGNAR_POLITICAS
     *
     * Las políticas se asignan al ROL del usuario, no directamente al usuario.
     * Se evitan duplicados automáticamente.
     */
    @PostMapping("/{usuarioId}/politicas")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ASIGNAR_POLITICAS')")
    @Operation(summary = "Asignar políticas a usuario",
            description = "Asigna políticas al ROL del usuario. Requiere ROLE_ADMIN y permiso ASIGNAR_POLITICAS")
    public ResponseEntity<ApiResponseDto<AsignarPoliticasResponseDto>> asignarPoliticas(
            @PathVariable UUID usuarioId,
            @Valid @RequestBody AsignarPoliticasRequestDto request) {
        log.info("Request: POST /api/usuarios/{}/politicas | Políticas: {}", usuarioId, request.getPoliticasIds().size());
        AsignarPoliticasResponseDto response = politicaService.asignarPoliticas(usuarioId, request);
        return ResponseEntity.ok(
                ApiResponseDto.success(response, "Políticas asignadas correctamente"));
    }
}
