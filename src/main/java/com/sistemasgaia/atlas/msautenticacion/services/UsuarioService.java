package com.sistemasgaia.atlas.msautenticacion.services;

import com.sistemasgaia.atlas.msautenticacion.dto.usuario.UsuarioRequestDto;
import com.sistemasgaia.atlas.msautenticacion.dto.usuario.UsuarioResponseDto;
import com.sistemasgaia.atlas.msautenticacion.exceptions.BusinessException;
import com.sistemasgaia.atlas.msautenticacion.exceptions.ResourceNotFoundException;
import com.sistemasgaia.atlas.msautenticacion.models.Rol;
import com.sistemasgaia.atlas.msautenticacion.models.Usuario;
import com.sistemasgaia.atlas.msautenticacion.repositories.RolRepository;
import com.sistemasgaia.atlas.msautenticacion.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioResponseDto> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDto buscarPorId(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
        return toResponseDto(usuario);
    }

    @Transactional
    public UsuarioResponseDto crear(UsuarioRequestDto request) {
        if (usuarioRepository.existsByNombreUsuario(request.getNombreUsuario())) {
            throw new BusinessException("El nombre de usuario '" + request.getNombreUsuario() + "' ya existe");
        }

        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new BusinessException("El correo '" + request.getCorreo() + "' ya está registrado");
        }

        Rol rol = rolRepository.findById(request.getRolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol", "id", request.getRolId()));

        Usuario usuario = Usuario.builder()
                .nombreUsuario(request.getNombreUsuario())
                .contrasenia(passwordEncoder.encode(request.getContrasenia()))
                .correo(request.getCorreo())
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .rolId(request.getRolId())
                .build();

        usuario = usuarioRepository.save(usuario);
        usuario.setRol(rol);
        log.info("Usuario creado: {}", usuario.getNombreUsuario());
        return toResponseDto(usuario);
    }

    @Transactional
    public UsuarioResponseDto actualizar(UUID id, UsuarioRequestDto request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));

        if (!usuario.getNombreUsuario().equals(request.getNombreUsuario())
                && usuarioRepository.existsByNombreUsuario(request.getNombreUsuario())) {
            throw new BusinessException("El nombre de usuario '" + request.getNombreUsuario() + "' ya existe");
        }

        if (!usuario.getCorreo().equals(request.getCorreo())
                && usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new BusinessException("El correo '" + request.getCorreo() + "' ya está registrado");
        }

        Rol rol = rolRepository.findById(request.getRolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol", "id", request.getRolId()));

        usuario.setNombreUsuario(request.getNombreUsuario());
        usuario.setContrasenia(passwordEncoder.encode(request.getContrasenia()));
        usuario.setCorreo(request.getCorreo());
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setRolId(request.getRolId());
        usuario.setUltimaModificacion(LocalDateTime.now());

        usuario = usuarioRepository.save(usuario);
        usuario.setRol(rol);
        log.info("Usuario actualizado: {}", usuario.getNombreUsuario());
        return toResponseDto(usuario);
    }

    @Transactional
    public void eliminar(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
        usuario.setActivo(false);
        usuario.setUltimaModificacion(LocalDateTime.now());
        usuarioRepository.save(usuario);
        log.info("Usuario desactivado (soft delete): {}", usuario.getNombreUsuario());
    }

    private UsuarioResponseDto toResponseDto(Usuario u) {
        return UsuarioResponseDto.builder()
                .id(u.getId())
                .nombreUsuario(u.getNombreUsuario())
                .correo(u.getCorreo())
                .nombre(u.getNombre())
                .apellido(u.getApellido())
                .rolId(u.getRolId())
                .tipoRol(u.getRol() != null ? u.getRol().getTipoRol().name() : null)
                .descripcionRol(u.getRol() != null ? u.getRol().getDescripcionRol() : null)
                .ultimaEvaluacion(u.getUltimaEvaluacion())
                .ultimaModificacion(u.getUltimaModificacion())
                .fechaCreacion(u.getFechaCreacion())
                .activo(u.getActivo())
                .build();
    }
}
