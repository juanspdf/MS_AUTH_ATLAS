package com.sistemasgaia.atlas.msautenticacion.services;

import com.sistemasgaia.atlas.msautenticacion.dto.politica.AsignarPoliticasRequestDto;
import com.sistemasgaia.atlas.msautenticacion.dto.politica.AsignarPoliticasResponseDto;
import com.sistemasgaia.atlas.msautenticacion.dto.politica.PoliticaRequestDto;
import com.sistemasgaia.atlas.msautenticacion.dto.politica.PoliticaResponseDto;
import com.sistemasgaia.atlas.msautenticacion.exceptions.BusinessException;
import com.sistemasgaia.atlas.msautenticacion.exceptions.ResourceNotFoundException;
import com.sistemasgaia.atlas.msautenticacion.models.DetallePolitica;
import com.sistemasgaia.atlas.msautenticacion.models.DetallePoliticaId;
import com.sistemasgaia.atlas.msautenticacion.models.Politica;
import com.sistemasgaia.atlas.msautenticacion.models.Usuario;
import com.sistemasgaia.atlas.msautenticacion.repositories.DetallePoliticaRepository;
import com.sistemasgaia.atlas.msautenticacion.repositories.PoliticaRepository;
import com.sistemasgaia.atlas.msautenticacion.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de gestión de Políticas (permisos del sistema).
 *
 * Responsabilidades:
 * - CRUD completo de políticas
 * - Asignación de políticas a roles de usuario (vía DetallePolitica)
 * - Validación de duplicados y existencia de entidades
 *
 * Todas las políticas se asignan al ROL del usuario, NO directamente al usuario.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PoliticaService {

    private final PoliticaRepository politicaRepository;
    private final DetallePoliticaRepository detallePoliticaRepository;
    private final UsuarioRepository usuarioRepository;

    // ==================== CRUD POLÍTICAS ====================

    /**
     * Lista todas las políticas activas del sistema.
     */
    @Transactional(readOnly = true)
    public List<PoliticaResponseDto> listarTodas() {
        log.info("Consultando todas las políticas del sistema");
        return politicaRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    /**
     * Busca una política por su ID.
     *
     * @param id UUID de la política
     * @return PoliticaResponseDto con los datos de la política
     * @throws ResourceNotFoundException si la política no existe
     */
    @Transactional(readOnly = true)
    public PoliticaResponseDto buscarPorId(UUID id) {
        log.info("Buscando política con ID: {}", id);
        Politica politica = politicaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Política", "id", id));
        return toResponseDto(politica);
    }

    /**
     * Crea una nueva política en el sistema.
     *
     * Validaciones:
     * - El nombre de la política no debe existir previamente
     *
     * @param request datos de la política a crear
     * @return PoliticaResponseDto con la política creada
     * @throws BusinessException si el nombre de la política ya existe
     */
    @Transactional
    public PoliticaResponseDto crear(PoliticaRequestDto request) {
        log.info("Creando nueva política: {}", request.getNombrePolitica());

        // Validar que no exista una política con el mismo nombre
        if (politicaRepository.existsByNombrePolitica(request.getNombrePolitica())) {
            throw new BusinessException(
                    "Ya existe una política con el nombre: " + request.getNombrePolitica());
        }

        Politica politica = Politica.builder()
                .nombrePolitica(request.getNombrePolitica())
                .build();

        politica = politicaRepository.save(politica);
        log.info("Política creada exitosamente: {} con ID: {}", politica.getNombrePolitica(), politica.getId());

        return toResponseDto(politica);
    }

    /**
     * Actualiza una política existente.
     *
     * Validaciones:
     * - La política debe existir
     * - Si el nombre cambia, el nuevo nombre no debe estar en uso
     *
     * @param id      UUID de la política a actualizar
     * @param request nuevos datos de la política
     * @return PoliticaResponseDto con la política actualizada
     */
    @Transactional
    public PoliticaResponseDto actualizar(UUID id, PoliticaRequestDto request) {
        log.info("Actualizando política con ID: {}", id);

        Politica politica = politicaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Política", "id", id));

        // Validar nombre único si cambió
        if (!politica.getNombrePolitica().equals(request.getNombrePolitica())
                && politicaRepository.existsByNombrePolitica(request.getNombrePolitica())) {
            throw new BusinessException(
                    "Ya existe una política con el nombre: " + request.getNombrePolitica());
        }

        politica.setNombrePolitica(request.getNombrePolitica());
        politica = politicaRepository.save(politica);

        log.info("Política actualizada exitosamente: {} con ID: {}", politica.getNombrePolitica(), politica.getId());

        return toResponseDto(politica);
    }

    /**
     * Elimina (soft delete) una política del sistema.
     *
     * @param id UUID de la política a eliminar
     * @throws ResourceNotFoundException si la política no existe
     */
    @Transactional
    public void eliminar(UUID id) {
        log.info("Eliminando política con ID: {}", id);

        Politica politica = politicaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Política", "id", id));

        politica.setActivo(false);
        politicaRepository.save(politica);

        log.info("Política desactivada (soft delete): {} con ID: {}", politica.getNombrePolitica(), id);
    }

    // ==================== ASIGNACIÓN DE POLÍTICAS ====================

    /**
     * Asigna políticas al ROL del usuario especificado.
     *
     * Flujo empresarial:
     * 1. Obtener el usuario y validar su existencia
     * 2. Obtener el rol del usuario
     * 3. Validar que todas las políticas solicitadas existan
     * 4. Verificar cuáles ya están asignadas (evitar duplicados)
     * 5. Crear solo las asignaciones nuevas vía DetallePolitica
     * 6. Retornar resumen detallado de la operación
     *
     * @param usuarioId UUID del usuario
     * @param request   lista de IDs de políticas a asignar
     * @return AsignarPoliticasResponseDto con el resumen de la operación
     */
    @Transactional
    public AsignarPoliticasResponseDto asignarPoliticas(UUID usuarioId, AsignarPoliticasRequestDto request) {
        log.info("Asignando {} políticas al usuario con ID: {}", request.getPoliticasIds().size(), usuarioId);

        // 1. Obtener y validar usuario
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", usuarioId));

        Integer rolId = usuario.getRolId();
        String tipoRol = usuario.getRol().getTipoRol().name();
        log.info("Usuario: {} | Rol: {} (ID: {})", usuario.getNombreUsuario(), tipoRol, rolId);

        // 2. Validar que todas las políticas existan
        List<Politica> politicas = politicaRepository.findAllById(request.getPoliticasIds());
        if (politicas.size() != request.getPoliticasIds().size()) {
            List<UUID> encontrados = politicas.stream().map(Politica::getId).toList();
            List<UUID> noEncontrados = request.getPoliticasIds().stream()
                    .filter(id -> !encontrados.contains(id))
                    .toList();
            throw new ResourceNotFoundException(
                    "Políticas no encontradas con IDs: " + noEncontrados);
        }

        // 3. Obtener políticas ya asignadas al rol
        List<DetallePolitica> existentes = detallePoliticaRepository.findByRolIdWithPolitica(rolId);
        List<UUID> idsYaAsignados = existentes.stream()
                .map(DetallePolitica::getPoliticaId)
                .toList();

        // 4. Separar nuevas y duplicadas
        List<String> politicasNuevas = new ArrayList<>();
        List<String> politicasYaExistentes = new ArrayList<>();

        for (Politica politica : politicas) {
            if (idsYaAsignados.contains(politica.getId())) {
                politicasYaExistentes.add(politica.getNombrePolitica());
                log.debug("Política ya asignada al rol {}: {}", tipoRol, politica.getNombrePolitica());
            } else {
                // Crear nueva asignación
                DetallePolitica detalle = DetallePolitica.builder()
                        .politicaId(politica.getId())
                        .rolId(rolId)
                        .build();
                detallePoliticaRepository.save(detalle);
                politicasNuevas.add(politica.getNombrePolitica());
                log.info("Política asignada al rol {}: {}", tipoRol, politica.getNombrePolitica());
            }
        }

        log.info("Asignación completada para usuario {} | Nuevas: {} | Duplicadas: {}",
                usuario.getNombreUsuario(), politicasNuevas.size(), politicasYaExistentes.size());

        // 5. Construir respuesta
        return AsignarPoliticasResponseDto.builder()
                .nombreUsuario(usuario.getNombreUsuario())
                .tipoRol(tipoRol)
                .politicasAsignadas(politicasNuevas.size())
                .politicasDuplicadas(politicasYaExistentes.size())
                .politicasNuevas(politicasNuevas)
                .politicasYaExistentes(politicasYaExistentes)
                .build();
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
