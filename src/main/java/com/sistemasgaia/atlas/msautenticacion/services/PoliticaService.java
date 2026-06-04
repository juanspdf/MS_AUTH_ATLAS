package com.sistemasgaia.atlas.msautenticacion.services;

import com.sistemasgaia.atlas.msautenticacion.dto.politica.PoliticaResponseDto;
import com.sistemasgaia.atlas.msautenticacion.exceptions.BusinessException;
import com.sistemasgaia.atlas.msautenticacion.exceptions.ResourceNotFoundException;
import com.sistemasgaia.atlas.msautenticacion.models.DetallePolitica;
import com.sistemasgaia.atlas.msautenticacion.models.Politica;
import com.sistemasgaia.atlas.msautenticacion.models.Rol;
import com.sistemasgaia.atlas.msautenticacion.repositories.DetallePoliticaRepository;
import com.sistemasgaia.atlas.msautenticacion.repositories.PoliticaRepository;
import com.sistemasgaia.atlas.msautenticacion.repositories.RolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de gestión de Políticas (permisos del sistema).
 *
 * Responsabilidades:
 * - Listar todas las políticas activas
 * - Listar políticas asignadas a un rol
 * - Asignar una política a un rol
 * - Desasignar una política de un rol
 *
 * Las políticas NO tienen CRUD desde API — se administran directamente en BD.
 * Todas las políticas se asignan al ROL, NO directamente al usuario.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PoliticaService {

    private final PoliticaRepository politicaRepository;
    private final DetallePoliticaRepository detallePoliticaRepository;
    private final RolRepository rolRepository;

    // ==================== LECTURA ====================

    /**
     * Lista todas las políticas activas del sistema.
     */
    @Transactional(readOnly = true)
    public List<PoliticaResponseDto> listarTodas() {
        log.info("Consultando todas las políticas del sistema");
        return politicaRepository.findByActivoTrue().stream()
                .map(this::toResponseDto)
                .toList();
    }

    /**
     * Lista las políticas asignadas a un rol específico.
     *
     * @param rolId ID del rol
     * @return Lista de políticas asignadas al rol
     * @throws ResourceNotFoundException si el rol no existe
     */
    @Transactional(readOnly = true)
    public List<PoliticaResponseDto> listarPorRol(Integer rolId) {
        log.info("Consultando políticas asignadas al rol con ID: {}", rolId);

        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", "id", rolId));

        return detallePoliticaRepository.findByRolIdWithPolitica(rolId).stream()
                .map(dp -> toResponseDto(dp.getPolitica()))
                .toList();
    }

    // ==================== ASIGNACIÓN / DESASIGNACIÓN ====================

    /**
     * Asigna una política a un rol.
     *
     * @param rolId      ID del rol
     * @param politicaId UUID de la política
     * @throws ResourceNotFoundException si el rol o la política no existen
     * @throws BusinessException si la política ya está asignada al rol o está inactiva
     */
    @Transactional
    public void asignarPoliticaARol(Integer rolId, UUID politicaId) {
        log.info("Asignando política {} al rol {}", politicaId, rolId);

        // Validar existencia del rol
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", "id", rolId));

        // Validar existencia de la política
        Politica politica = politicaRepository.findById(politicaId)
                .orElseThrow(() -> new ResourceNotFoundException("Política", "id", politicaId));

        // Validar que la política esté activa
        if (!politica.getActivo()) {
            throw new BusinessException(
                    "No se puede asignar la política inactiva: " + politica.getNombrePolitica());
        }

        // Validar que no esté ya asignada
        if (detallePoliticaRepository.existsByPoliticaIdAndRolId(politicaId, rolId)) {
            throw new BusinessException(
                    "La política '" + politica.getNombrePolitica() + "' ya está asignada al rol '" + rol.getTipoRol() + "'");
        }

        // Crear asignación
        DetallePolitica detalle = DetallePolitica.builder()
                .politicaId(politicaId)
                .rolId(rolId)
                .build();
        detallePoliticaRepository.save(detalle);

        log.info("Política '{}' asignada exitosamente al rol '{}'",
                politica.getNombrePolitica(), rol.getTipoRol());
    }

    /**
     * Desasigna una política de un rol.
     *
     * @param rolId      ID del rol
     * @param politicaId UUID de la política
     * @throws ResourceNotFoundException si el rol o la política no existen, o si la asignación no existe
     */
    @Transactional
    public void desasignarPoliticaDeRol(Integer rolId, UUID politicaId) {
        log.info("Desasignando política {} del rol {}", politicaId, rolId);

        // Validar existencia del rol
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", "id", rolId));

        // Validar existencia de la política
        Politica politica = politicaRepository.findById(politicaId)
                .orElseThrow(() -> new ResourceNotFoundException("Política", "id", politicaId));

        // Validar que la asignación existe
        if (!detallePoliticaRepository.existsByPoliticaIdAndRolId(politicaId, rolId)) {
            throw new ResourceNotFoundException(
                    "La política '" + politica.getNombrePolitica() + "' no está asignada al rol '" + rol.getTipoRol() + "'");
        }

        detallePoliticaRepository.deleteByPoliticaIdAndRolId(politicaId, rolId);

        log.info("Política '{}' desasignada exitosamente del rol '{}'",
                politica.getNombrePolitica(), rol.getTipoRol());
    }

    // ==================== MAPPERS ====================

    private PoliticaResponseDto toResponseDto(Politica politica) {
        return PoliticaResponseDto.builder()
                .id(politica.getId())
                .nombrePolitica(politica.getNombrePolitica())
                .activo(politica.getActivo())
                .fechaCreacion(politica.getFechaCreacion())
                .fechaActualizacion(politica.getFechaActualizacion())
                .build();
    }
}
