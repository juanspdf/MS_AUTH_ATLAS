package com.sistemasgaia.atlas.msautenticacion.services;

import com.sistemasgaia.atlas.msautenticacion.enums.TipoRol;
import com.sistemasgaia.atlas.msautenticacion.exceptions.BusinessException;
import com.sistemasgaia.atlas.msautenticacion.exceptions.ResourceNotFoundException;
import com.sistemasgaia.atlas.msautenticacion.models.DetallePolitica;
import com.sistemasgaia.atlas.msautenticacion.models.Politica;
import com.sistemasgaia.atlas.msautenticacion.models.Rol;
import com.sistemasgaia.atlas.msautenticacion.repositories.DetallePoliticaRepository;
import com.sistemasgaia.atlas.msautenticacion.repositories.PoliticaRepository;
import com.sistemasgaia.atlas.msautenticacion.repositories.RolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para PoliticaService.
 * Verifica:
 * - Listar todas las políticas
 * - Listar políticas por rol
 * - Asignar política a rol (éxito y duplicado)
 * - Desasignar política de rol (éxito y no asignada)
 */
@ExtendWith(MockitoExtension.class)
class PoliticaServiceTest {

    @Mock
    private PoliticaRepository politicaRepository;

    @Mock
    private DetallePoliticaRepository detallePoliticaRepository;

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private PoliticaService politicaService;

    private Rol rolConsultor;
    private Politica politicaActiva;
    private Politica politicaInactiva;
    private UUID politicaId;
    private UUID politicaInactivaId;

    @BeforeEach
    void setUp() {
        politicaId = UUID.randomUUID();
        politicaInactivaId = UUID.randomUUID();

        rolConsultor = Rol.builder().id(2).tipoRol(TipoRol.CONSULTOR).descripcionRol("Consultor").build();

        politicaActiva = Politica.builder()
                .id(politicaId)
                .nombrePolitica("POLITICA_TEST")
                .build();
        politicaActiva.setActivo(true);

        politicaInactiva = Politica.builder()
                .id(politicaInactivaId)
                .nombrePolitica("POLITICA_INACTIVA")
                .build();
        politicaInactiva.setActivo(false);
    }

    // ==================== LISTAR ====================

    @Nested
    @DisplayName("Listar políticas")
    class ListarPoliticas {

        @Test
        @DisplayName("GET /api/politicas debe devolver lista")
        void listarTodas_debeRetornarListaPoliticas() {
            when(politicaRepository.findByActivoTrue()).thenReturn(List.of(politicaActiva));

            var result = politicaService.listarTodas();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("POLITICA_TEST", result.get(0).getNombrePolitica());
        }

        @Test
        @DisplayName("GET /api/politicas/rol/{rolId} debe devolver políticas asignadas")
        void listarPorRol_debeRetornarPoliticasDelRol() {
            DetallePolitica detalle = DetallePolitica.builder()
                    .politicaId(politicaId)
                    .rolId(2)
                    .build();
            detalle.setPolitica(politicaActiva);

            when(rolRepository.findById(2)).thenReturn(Optional.of(rolConsultor));
            when(detallePoliticaRepository.findByRolIdWithPolitica(2)).thenReturn(List.of(detalle));

            var result = politicaService.listarPorRol(2);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("POLITICA_TEST", result.get(0).getNombrePolitica());
        }

        @Test
        @DisplayName("GET /api/politicas/rol/{rolId} con rol inexistente debe devolver 404")
        void listarPorRolInexistente_debeLanzar404() {
            when(rolRepository.findById(99)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> politicaService.listarPorRol(99));
        }
    }

    // ==================== ASIGNAR ====================

    @Nested
    @DisplayName("Asignar política a rol")
    class AsignarPolitica {

        @Test
        @DisplayName("POST /api/politicas/rol/{rolId}/asignar debe asignar correctamente")
        void asignarPolitica_debeAsignarExitosamente() {
            when(rolRepository.findById(2)).thenReturn(Optional.of(rolConsultor));
            when(politicaRepository.findById(politicaId)).thenReturn(Optional.of(politicaActiva));
            when(detallePoliticaRepository.existsByPoliticaIdAndRolId(politicaId, 2)).thenReturn(false);

            assertDoesNotThrow(() -> politicaService.asignarPoliticaARol(2, politicaId));
            verify(detallePoliticaRepository).save(any(DetallePolitica.class));
        }

        @Test
        @DisplayName("POST duplicado de la misma política debe devolver 409")
        void asignarPoliticaDuplicada_debeLanzar409() {
            when(rolRepository.findById(2)).thenReturn(Optional.of(rolConsultor));
            when(politicaRepository.findById(politicaId)).thenReturn(Optional.of(politicaActiva));
            when(detallePoliticaRepository.existsByPoliticaIdAndRolId(politicaId, 2)).thenReturn(true);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> politicaService.asignarPoliticaARol(2, politicaId));

            assertTrue(ex.getMessage().contains("ya está asignada"));
            verify(detallePoliticaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Asignar política inactiva debe devolver 409")
        void asignarPoliticaInactiva_debeLanzar409() {
            when(rolRepository.findById(2)).thenReturn(Optional.of(rolConsultor));
            when(politicaRepository.findById(politicaInactivaId)).thenReturn(Optional.of(politicaInactiva));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> politicaService.asignarPoliticaARol(2, politicaInactivaId));

            assertTrue(ex.getMessage().contains("inactiva"));
        }
    }

    // ==================== DESASIGNAR ====================

    @Nested
    @DisplayName("Desasignar política de rol")
    class DesasignarPolitica {

        @Test
        @DisplayName("DELETE /api/politicas/rol/{rolId}/desasignar/{politicaId} debe desasignar correctamente")
        void desasignarPolitica_debeDesasignarExitosamente() {
            when(rolRepository.findById(2)).thenReturn(Optional.of(rolConsultor));
            when(politicaRepository.findById(politicaId)).thenReturn(Optional.of(politicaActiva));
            when(detallePoliticaRepository.existsByPoliticaIdAndRolId(politicaId, 2)).thenReturn(true);

            assertDoesNotThrow(() -> politicaService.desasignarPoliticaDeRol(2, politicaId));
            verify(detallePoliticaRepository).deleteByPoliticaIdAndRolId(politicaId, 2);
        }

        @Test
        @DisplayName("Desasignar política no asignada debe devolver 404")
        void desasignarNoAsignada_debeLanzar404() {
            when(rolRepository.findById(2)).thenReturn(Optional.of(rolConsultor));
            when(politicaRepository.findById(politicaId)).thenReturn(Optional.of(politicaActiva));
            when(detallePoliticaRepository.existsByPoliticaIdAndRolId(politicaId, 2)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class,
                    () -> politicaService.desasignarPoliticaDeRol(2, politicaId));
        }
    }
}
