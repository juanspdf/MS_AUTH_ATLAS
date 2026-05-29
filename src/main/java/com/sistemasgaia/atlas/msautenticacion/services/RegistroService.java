package com.sistemasgaia.atlas.msautenticacion.services;

import com.sistemasgaia.atlas.msautenticacion.dto.auth.*;
import com.sistemasgaia.atlas.msautenticacion.enums.TipoRol;
import com.sistemasgaia.atlas.msautenticacion.enums.TipoToken;
import com.sistemasgaia.atlas.msautenticacion.exceptions.BusinessException;
import com.sistemasgaia.atlas.msautenticacion.exceptions.ResourceNotFoundException;
import com.sistemasgaia.atlas.msautenticacion.models.Rol;
import com.sistemasgaia.atlas.msautenticacion.models.TokenActivacion;
import com.sistemasgaia.atlas.msautenticacion.models.Usuario;
import com.sistemasgaia.atlas.msautenticacion.repositories.RolRepository;
import com.sistemasgaia.atlas.msautenticacion.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio de registro de usuarios y gestión de contraseñas.
 *
 * Flujo de registro:
 * 1. Validar datos y unicidad de username/correo
 * 2. Crear usuario con estado inactivo (sin contraseña real)
 * 3. Generar token de activación
 * 4. Enviar correo con enlace de activación
 *
 * Flujo de activación/recuperación:
 * 1. Validar token (existencia, expiración, uso)
 * 2. Establecer nueva contraseña (BCrypt)
 * 3. Activar usuario si es primera activación
 * 4. Invalidar token
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistroService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenActivacionService tokenActivacionService;
    private final EmailService emailService;

    /**
     * Registra un nuevo usuario en el sistema.
     * El usuario se crea inactivo y recibe un correo para establecer su contraseña.
     */
    @Transactional
    public RegistroResponseDto registrar(RegistroRequestDto request) {
        log.info("Registro de nuevo usuario: {}", request.getNombreUsuario());

        // 1. Validar unicidad del nombre de usuario
        if (usuarioRepository.existsByNombreUsuario(request.getNombreUsuario())) {
            throw new BusinessException(
                    "El nombre de usuario '" + request.getNombreUsuario() + "' ya está en uso");
        }

        // 2. Validar unicidad del correo
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new BusinessException(
                    "El correo electrónico '" + request.getCorreo() + "' ya está registrado");
        }

        // 3. Resolver rol (por defecto CLIENTE)
        final Integer requestRolId = request.getRolId();
        Rol rol;
        if (requestRolId != null) {
            rol = rolRepository.findById(requestRolId)
                    .orElseThrow(() -> new ResourceNotFoundException("Rol", "id", requestRolId));
        } else {
            rol = rolRepository.findByTipoRol(TipoRol.CLIENTE)
                    .orElseThrow(() -> new ResourceNotFoundException("Rol", "tipo", "CLIENTE"));
        }
        final Integer rolId = rol.getId();

        // 4. Crear usuario inactivo con contraseña temporal no usable
        String contraseniaTemporal = passwordEncoder.encode(UUID.randomUUID().toString());

        Usuario usuario = Usuario.builder()
                .nombreUsuario(request.getNombreUsuario())
                .correo(request.getCorreo())
                .contrasenia(contraseniaTemporal)
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .rolId(rolId)
                .build();
        usuario.setActivo(false); // Inactivo hasta que active su cuenta

        usuario = usuarioRepository.save(usuario);
        usuario.setRol(rol);
        log.info("Usuario creado (inactivo): {} con rol: {}", usuario.getNombreUsuario(), rol.getTipoRol());

        // 5. Generar token de activación
        TokenActivacion tokenActivacion = tokenActivacionService.generarToken(usuario, TipoToken.ACTIVACION);

        // 6. Enviar correo de activación (asíncrono)
        String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
        emailService.enviarCorreoActivacion(usuario.getCorreo(), nombreCompleto, tokenActivacion.getToken());

        // 7. Construir respuesta
        return RegistroResponseDto.builder()
                .idUsuario(usuario.getId())
                .nombreUsuario(usuario.getNombreUsuario())
                .correo(usuario.getCorreo())
                .nombreCompleto(nombreCompleto)
                .rol(rol.getTipoRol().name())
                .mensaje("Usuario registrado. Se ha enviado un correo a " + usuario.getCorreo()
                         + " para establecer la contraseña")
                .fechaCreacion(usuario.getFechaCreacion())
                .build();
    }

    /**
     * Valida un token de activación/recuperación.
     * Permite al frontend verificar si el token es válido antes de mostrar el formulario.
     */
    @Transactional(readOnly = true)
    public ValidarTokenResponseDto validarToken(String token) {
        TokenActivacion tokenActivacion = tokenActivacionService.validarToken(token);
        Usuario usuario = tokenActivacion.getUsuario();

        return ValidarTokenResponseDto.builder()
                .valido(true)
                .tipoToken(tokenActivacion.getTipoToken().name())
                .nombreUsuario(usuario.getNombreUsuario())
                .correo(usuario.getCorreo())
                .fechaExpiracion(tokenActivacion.getFechaExpiracion())
                .mensaje("Token válido")
                .build();
    }

    /**
     * Establece la contraseña del usuario usando un token de activación o recuperación.
     * Activa el usuario si es una activación de cuenta nueva.
     */
    @Transactional
    public void establecerContrasenia(EstablecerContraseniaRequestDto request) {
        // 1. Validar que las contraseñas coincidan
        if (!request.getNuevaContrasenia().equals(request.getConfirmarContrasenia())) {
            throw new BusinessException("Las contraseñas no coinciden");
        }

        // 2. Validar token
        TokenActivacion tokenActivacion = tokenActivacionService.validarToken(request.getToken());
        Usuario usuario = tokenActivacion.getUsuario();

        // 3. Establecer nueva contraseña encriptada
        usuario.setContrasenia(passwordEncoder.encode(request.getNuevaContrasenia()));

        // 4. Activar usuario si es token de activación
        if (tokenActivacion.getTipoToken() == TipoToken.ACTIVACION) {
            usuario.setActivo(true);
            log.info("Cuenta activada para usuario: {}", usuario.getNombreUsuario());
        }

        usuarioRepository.save(usuario);

        // 5. Marcar token como usado
        tokenActivacionService.marcarComoUsado(tokenActivacion);

        log.info("Contraseña establecida exitosamente para usuario: {} (tipo: {})",
                usuario.getNombreUsuario(), tokenActivacion.getTipoToken());
    }

    /**
     * Solicita la recuperación de contraseña para un usuario existente.
     * Envía un correo con un enlace para restablecer la contraseña.
     *
     * Nota: Por seguridad, siempre retorna éxito aunque el correo no exista,
     * para evitar la enumeración de usuarios.
     */
    @Transactional
    public void solicitarRecuperacion(RecuperarContraseniaRequestDto request) {
        log.info("Solicitud de recuperación de contraseña para: {}", request.getCorreo());

        // Buscar usuario por correo (activo)
        Usuario usuario = usuarioRepository.findByCorreoAndActivoTrue(request.getCorreo())
                .orElse(null);

        // Por seguridad, no revelamos si el correo existe o no
        if (usuario == null) {
            log.warn("Solicitud de recuperación para correo no registrado: {}", request.getCorreo());
            return; // Silenciosamente no hace nada
        }

        // Generar token de recuperación
        TokenActivacion tokenRecuperacion = tokenActivacionService.generarToken(
                usuario, TipoToken.RECUPERACION);

        // Enviar correo de recuperación (asíncrono)
        String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
        emailService.enviarCorreoRecuperacion(
                usuario.getCorreo(), nombreCompleto, tokenRecuperacion.getToken());
    }

    /**
     * Reenvía el correo de activación para un usuario que aún no ha activado su cuenta.
     */
    @Transactional
    public void reenviarActivacion(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "correo", correo));

        if (usuario.getActivo()) {
            throw new BusinessException("La cuenta ya se encuentra activa");
        }

        // Generar nuevo token de activación
        TokenActivacion tokenActivacion = tokenActivacionService.generarToken(
                usuario, TipoToken.ACTIVACION);

        // Reenviar correo
        String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
        emailService.enviarCorreoActivacion(usuario.getCorreo(), nombreCompleto, tokenActivacion.getToken());

        log.info("Correo de activación reenviado a: {}", correo);
    }
}
