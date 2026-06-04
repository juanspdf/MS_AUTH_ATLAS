package com.sistemasgaia.atlas.msautenticacion.services;

import com.sistemasgaia.atlas.msautenticacion.dto.usuario.UsuarioRequestDto;
import com.sistemasgaia.atlas.msautenticacion.enums.TipoRol;
import com.sistemasgaia.atlas.msautenticacion.exceptions.BusinessException;
import com.sistemasgaia.atlas.msautenticacion.exceptions.ForbiddenOperationException;
import com.sistemasgaia.atlas.msautenticacion.exceptions.ResourceNotFoundException;
import com.sistemasgaia.atlas.msautenticacion.models.Rol;
import com.sistemasgaia.atlas.msautenticacion.models.Usuario;
import com.sistemasgaia.atlas.msautenticacion.repositories.RolRepository;
import com.sistemasgaia.atlas.msautenticacion.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para UsuarioService.
 * Verifica las reglas de negocio:
 * - No crear/actualizar usuarios con rol ADMIN
 * - ADMIN no puede editarse ni eliminarse a sí mismo
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Rol rolAdmin;
    private Rol rolConsultor;
    private UUID adminId;
    private UUID otroUsuarioId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        otroUsuarioId = UUID.randomUUID();

        rolAdmin = Rol.builder().id(1).tipoRol(TipoRol.ADMIN).descripcionRol("Administrador").build();
        rolConsultor = Rol.builder().id(2).tipoRol(TipoRol.CONSULTOR).descripcionRol("Consultor").build();
    }

    // ==================== CREAR ====================

    @Nested
    @DisplayName("Crear usuario")
    class CrearUsuario {

        @Test
        @DisplayName("POST /api/usuarios con rol ADMIN debe devolver 403")
        void crearUsuarioConRolAdmin_debeLanzarForbidden() {
            UsuarioRequestDto request = UsuarioRequestDto.builder()
                    .nombreUsuario("nuevo")
                    .contrasenia("Password1")
                    .correo("nuevo@test.com")
                    .nombre("Nuevo")
                    .apellido("Usuario")
                    .rolId(1)
                    .build();

            when(usuarioRepository.existsByNombreUsuario("nuevo")).thenReturn(false);
            when(usuarioRepository.existsByCorreo("nuevo@test.com")).thenReturn(false);
            when(rolRepository.findById(1)).thenReturn(Optional.of(rolAdmin));

            ForbiddenOperationException ex = assertThrows(
                    ForbiddenOperationException.class,
                    () -> usuarioService.crear(request));

            assertEquals("No se permite crear usuarios con rol ADMIN", ex.getMessage());
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("POST /api/usuarios con rol CONSULTOR debe crear correctamente")
        void crearUsuarioConRolConsultor_debeCrearExitosamente() {
            UsuarioRequestDto request = UsuarioRequestDto.builder()
                    .nombreUsuario("consultor")
                    .contrasenia("Password1")
                    .correo("consultor@test.com")
                    .nombre("Con")
                    .apellido("Sultor")
                    .rolId(2)
                    .build();

            Usuario savedUsuario = Usuario.builder()
                    .id(UUID.randomUUID())
                    .nombreUsuario("consultor")
                    .correo("consultor@test.com")
                    .nombre("Con")
                    .apellido("Sultor")
                    .rolId(2)
                    .build();
            savedUsuario.setRol(rolConsultor);

            when(usuarioRepository.existsByNombreUsuario("consultor")).thenReturn(false);
            when(usuarioRepository.existsByCorreo("consultor@test.com")).thenReturn(false);
            when(rolRepository.findById(2)).thenReturn(Optional.of(rolConsultor));
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(usuarioRepository.save(any())).thenReturn(savedUsuario);

            var result = usuarioService.crear(request);

            assertNotNull(result);
            assertEquals("CONSULTOR", result.getTipoRol());
            verify(usuarioRepository).save(any());
        }
    }

    // ==================== ACTUALIZAR ====================

    @Nested
    @DisplayName("Actualizar usuario")
    class ActualizarUsuario {

        @Test
        @DisplayName("PUT /api/usuarios/{id} intentando asignar ADMIN debe devolver 403")
        void actualizarConRolAdmin_debeLanzarForbidden() {
            Usuario usuario = Usuario.builder()
                    .id(otroUsuarioId)
                    .nombreUsuario("user1")
                    .correo("user1@test.com")
                    .nombre("User")
                    .apellido("One")
                    .rolId(2)
                    .build();
            usuario.setRol(rolConsultor);

            UsuarioRequestDto request = UsuarioRequestDto.builder()
                    .nombreUsuario("user1")
                    .correo("user1@test.com")
                    .nombre("User")
                    .apellido("One")
                    .rolId(1) // Intentando asignar ADMIN
                    .build();

            when(usuarioRepository.findById(otroUsuarioId)).thenReturn(Optional.of(usuario));
            when(rolRepository.findById(1)).thenReturn(Optional.of(rolAdmin));

            ForbiddenOperationException ex = assertThrows(
                    ForbiddenOperationException.class,
                    () -> usuarioService.actualizar(otroUsuarioId, request, adminId));

            assertEquals("No se permite asignar el rol ADMIN", ex.getMessage());
        }

        @Test
        @DisplayName("PUT /api/usuarios/{adminId} con token del mismo admin debe devolver 403")
        void adminEditandoseASiMismo_debeLanzarForbidden() {
            Usuario admin = Usuario.builder()
                    .id(adminId)
                    .nombreUsuario("admin")
                    .correo("admin@test.com")
                    .nombre("Admin")
                    .apellido("System")
                    .rolId(1)
                    .build();
            admin.setRol(rolAdmin);

            UsuarioRequestDto request = UsuarioRequestDto.builder()
                    .nombreUsuario("admin")
                    .correo("admin@test.com")
                    .nombre("Admin Modificado")
                    .apellido("System")
                    .rolId(2)
                    .build();

            when(usuarioRepository.findById(adminId)).thenReturn(Optional.of(admin));

            ForbiddenOperationException ex = assertThrows(
                    ForbiddenOperationException.class,
                    () -> usuarioService.actualizar(adminId, request, adminId));

            assertEquals("Un administrador no puede editarse a sí mismo", ex.getMessage());
        }
    }

    // ==================== ELIMINAR ====================

    @Nested
    @DisplayName("Eliminar usuario")
    class EliminarUsuario {

        @Test
        @DisplayName("DELETE /api/usuarios/{adminId} con token del mismo admin debe devolver 403")
        void adminEliminandoseASiMismo_debeLanzarForbidden() {
            Usuario admin = Usuario.builder()
                    .id(adminId)
                    .nombreUsuario("admin")
                    .correo("admin@test.com")
                    .nombre("Admin")
                    .apellido("System")
                    .rolId(1)
                    .build();
            admin.setRol(rolAdmin);

            when(usuarioRepository.findById(adminId)).thenReturn(Optional.of(admin));

            ForbiddenOperationException ex = assertThrows(
                    ForbiddenOperationException.class,
                    () -> usuarioService.eliminar(adminId, adminId));

            assertEquals("Un administrador no puede eliminarse a sí mismo", ex.getMessage());
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("DELETE /api/usuarios/{otroId} con token admin debe eliminar correctamente")
        void adminEliminandoOtroUsuario_debeEliminarExitosamente() {
            Usuario otroUsuario = Usuario.builder()
                    .id(otroUsuarioId)
                    .nombreUsuario("user1")
                    .correo("user1@test.com")
                    .nombre("User")
                    .apellido("One")
                    .rolId(2)
                    .build();
            otroUsuario.setRol(rolConsultor);

            when(usuarioRepository.findById(otroUsuarioId)).thenReturn(Optional.of(otroUsuario));
            when(usuarioRepository.save(any())).thenReturn(otroUsuario);

            assertDoesNotThrow(() -> usuarioService.eliminar(otroUsuarioId, adminId));
            assertFalse(otroUsuario.getActivo());
            verify(usuarioRepository).save(any());
        }
    }
}
