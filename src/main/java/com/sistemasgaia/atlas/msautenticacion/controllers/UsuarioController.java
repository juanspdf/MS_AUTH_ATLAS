package com.sistemasgaia.atlas.msautenticacion.controllers;

import com.sistemasgaia.atlas.msautenticacion.dto.ApiResponseDto;
import com.sistemasgaia.atlas.msautenticacion.dto.usuario.UsuarioRequestDto;
import com.sistemasgaia.atlas.msautenticacion.dto.usuario.UsuarioResponseDto;
import com.sistemasgaia.atlas.msautenticacion.security.JwtService;
import com.sistemasgaia.atlas.msautenticacion.services.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
@Tag(name = "Usuarios", description = "CRUD de usuarios del sistema")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuarios", description = "Obtiene todos los usuarios registrados")
    public ResponseEntity<ApiResponseDto<List<UsuarioResponseDto>>> listarTodos() {
        List<UsuarioResponseDto> usuarios = usuarioService.listarTodos();
        return ResponseEntity.ok(ApiResponseDto.success(usuarios, "Usuarios obtenidos correctamente"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Buscar usuario por ID")
    public ResponseEntity<ApiResponseDto<UsuarioResponseDto>> buscarPorId(@PathVariable UUID id) {
        UsuarioResponseDto usuario = usuarioService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDto.success(usuario, "Usuario encontrado"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear usuario")
    public ResponseEntity<ApiResponseDto<UsuarioResponseDto>> crear(
            @Valid @RequestBody UsuarioRequestDto request) {
        UsuarioResponseDto usuario = usuarioService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.created(usuario, "Usuario creado correctamente"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar usuario")
    public ResponseEntity<ApiResponseDto<UsuarioResponseDto>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody UsuarioRequestDto request,
            HttpServletRequest httpRequest) {
        UUID idUsuarioAutenticado = extraerIdUsuarioDelToken(httpRequest);
        UsuarioResponseDto usuario = usuarioService.actualizar(id, request, idUsuarioAutenticado);
        return ResponseEntity.ok(ApiResponseDto.success(usuario, "Usuario actualizado correctamente"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar usuario (soft delete)")
    public ResponseEntity<ApiResponseDto<Void>> eliminar(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID idUsuarioAutenticado = extraerIdUsuarioDelToken(httpRequest);
        usuarioService.eliminar(id, idUsuarioAutenticado);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Usuario eliminado correctamente"));
    }

    /**
     * Extrae el UUID del usuario autenticado desde el token JWT en el header Authorization.
     */
    private UUID extraerIdUsuarioDelToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.substring(7); // "Bearer " = 7 chars
        return jwtService.extraerIdUsuario(token);
    }
}
