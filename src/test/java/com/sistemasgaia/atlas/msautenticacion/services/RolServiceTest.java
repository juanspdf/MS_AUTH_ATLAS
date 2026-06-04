package com.sistemasgaia.atlas.msautenticacion.services;

import com.sistemasgaia.atlas.msautenticacion.dto.rol.RolResponseDto;
import com.sistemasgaia.atlas.msautenticacion.enums.TipoRol;
import com.sistemasgaia.atlas.msautenticacion.models.Rol;
import com.sistemasgaia.atlas.msautenticacion.repositories.RolRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para RolService.
 * Verifica que el listado de roles excluye ADMIN.
 */
@ExtendWith(MockitoExtension.class)
class RolServiceTest {

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private RolService rolService;

    @Test
    @DisplayName("GET /api/roles no debe devolver ADMIN")
    void listarRolesAsignables_noDebeIncluirAdmin() {
        Rol consultor = Rol.builder().id(2).tipoRol(TipoRol.CONSULTOR).descripcionRol("Consultor").build();
        consultor.setActivo(true);
        Rol gestorNormas = Rol.builder().id(3).tipoRol(TipoRol.GESTOR_NORMAS).descripcionRol("Gestor de Normas").build();
        gestorNormas.setActivo(true);

        when(rolRepository.findByTipoRolNot(TipoRol.ADMIN))
                .thenReturn(List.of(consultor, gestorNormas));

        List<RolResponseDto> result = rolService.listarRolesAsignables();

        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(r -> "ADMIN".equals(r.getTipoRol())));
        assertTrue(result.stream().anyMatch(r -> "CONSULTOR".equals(r.getTipoRol())));
        assertTrue(result.stream().anyMatch(r -> "GESTOR_NORMAS".equals(r.getTipoRol())));
    }
}
