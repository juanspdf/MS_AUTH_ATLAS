package com.sistemasgaia.atlas.msautenticacion.services;

import com.sistemasgaia.atlas.msautenticacion.dto.rol.RolResponseDto;
import com.sistemasgaia.atlas.msautenticacion.enums.TipoRol;
import com.sistemasgaia.atlas.msautenticacion.repositories.RolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio para gestión de roles.
 * Expone solo roles asignables (excluye ADMIN).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository rolRepository;

    /**
     * Lista los roles asignables (todos excepto ADMIN).
     */
    @Transactional(readOnly = true)
    public List<RolResponseDto> listarRolesAsignables() {
        log.info("Consultando roles asignables (sin ADMIN)");
        return rolRepository.findByTipoRolNot(TipoRol.ADMIN).stream()
                .map(rol -> RolResponseDto.builder()
                        .id(rol.getId())
                        .tipoRol(rol.getTipoRol().name())
                        .descripcionRol(rol.getDescripcionRol())
                        .activo(rol.getActivo())
                        .build())
                .toList();
    }
}
