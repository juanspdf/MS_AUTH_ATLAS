package com.sistemasgaia.atlas.msautenticacion.controllers;

import com.sistemasgaia.atlas.msautenticacion.dto.ApiResponseDto;
import com.sistemasgaia.atlas.msautenticacion.dto.auth.*;
import com.sistemasgaia.atlas.msautenticacion.services.AuthService;
import com.sistemasgaia.atlas.msautenticacion.services.RegistroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de autenticación, registro, logout y gestión de contraseñas")
public class AuthController {

    private final AuthService authService;
    private final RegistroService registroService;

    // ==================== LOGIN / LOGOUT ====================

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario y retorna un token JWT")
    public ResponseEntity<ApiResponseDto<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(ApiResponseDto.success(response, "Autenticación exitosa"));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Cerrar sesión",
            description = "Invalida el token JWT actual. El token no podrá ser utilizado en requests futuros.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponseDto<Void>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Sesión cerrada exitosamente"));
    }

    // ==================== REGISTRO ====================

    @PostMapping("/register")
    @Operation(
            summary = "Registrar usuario",
            description = "Registra un nuevo usuario en el sistema. El usuario recibirá un correo para establecer su contraseña."
    )
    public ResponseEntity<ApiResponseDto<RegistroResponseDto>> registrar(
            @Valid @RequestBody RegistroRequestDto request) {
        RegistroResponseDto response = registroService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.created(response, "Usuario registrado exitosamente"));
    }

    // ==================== ACTIVACIÓN / CONTRASEÑA ====================

    @GetMapping("/validar-token")
    @Operation(
            summary = "Validar token de activación/recuperación",
            description = "Verifica si un token es válido antes de mostrar el formulario de contraseña."
    )
    public ResponseEntity<ApiResponseDto<ValidarTokenResponseDto>> validarToken(
            @RequestParam String token) {
        ValidarTokenResponseDto response = registroService.validarToken(token);
        return ResponseEntity.ok(ApiResponseDto.success(response, "Token válido"));
    }

    @PostMapping("/establecer-contrasenia")
    @Operation(
            summary = "Establecer contraseña",
            description = "Establece o restablece la contraseña usando un token de activación o recuperación."
    )
    public ResponseEntity<ApiResponseDto<Void>> establecerContrasenia(
            @Valid @RequestBody EstablecerContraseniaRequestDto request) {
        registroService.establecerContrasenia(request);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Contraseña establecida exitosamente"));
    }

    @PostMapping("/recuperar-contrasenia")
    @Operation(
            summary = "Solicitar recuperación de contraseña",
            description = "Envía un correo con enlace para restablecer la contraseña. Por seguridad, siempre retorna éxito."
    )
    public ResponseEntity<ApiResponseDto<Void>> recuperarContrasenia(
            @Valid @RequestBody RecuperarContraseniaRequestDto request) {
        registroService.solicitarRecuperacion(request);
        return ResponseEntity.ok(ApiResponseDto.success(null,
                "Si el correo está registrado, recibirás un enlace para restablecer tu contraseña"));
    }

    @PostMapping("/reenviar-activacion")
    @Operation(
            summary = "Reenviar correo de activación",
            description = "Reenvía el correo de activación para un usuario que aún no ha activado su cuenta."
    )
    public ResponseEntity<ApiResponseDto<Void>> reenviarActivacion(
            @Valid @RequestBody RecuperarContraseniaRequestDto request) {
        registroService.reenviarActivacion(request.getCorreo());
        return ResponseEntity.ok(ApiResponseDto.success(null, "Correo de activación reenviado"));
    }
}
