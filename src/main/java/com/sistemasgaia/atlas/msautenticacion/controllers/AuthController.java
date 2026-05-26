package com.sistemasgaia.atlas.msautenticacion.controllers;

import com.sistemasgaia.atlas.msautenticacion.dto.ApiResponseDto;
import com.sistemasgaia.atlas.msautenticacion.dto.auth.LoginRequestDto;
import com.sistemasgaia.atlas.msautenticacion.dto.auth.LoginResponseDto;
import com.sistemasgaia.atlas.msautenticacion.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de autenticación y generación de JWT")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario y retorna un token JWT")
    public ResponseEntity<ApiResponseDto<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(ApiResponseDto.success(response, "Autenticación exitosa"));
    }
}
