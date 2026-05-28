package com.sistemasgaia.atlas.msautenticacion.services;

import com.sistemasgaia.atlas.msautenticacion.models.DetallePolitica;
import com.sistemasgaia.atlas.msautenticacion.repositories.DetallePoliticaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de Autorización RBAC empresarial.
 *
 * Proporciona métodos de consulta para el sistema de autorización basado en roles y políticas.
 * Este servicio es utilizado por el filtro JWT y los componentes de seguridad para
 * resolver las políticas/permisos de un usuario a partir de su rol.
 *
 * Flujo RBAC:
 * 1. Un usuario tiene un ROL
 * 2. Un ROL tiene N políticas (vía DetallePolitica)
 * 3. Cada política representa un permiso granular (ej: CREAR_POLITICA, EDITAR_POLITICA)
 * 4. La autorización valida: hasRole('ADMIN') AND hasAuthority('CREAR_POLITICA')
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutorizacionService {

    private final DetallePoliticaRepository detallePoliticaRepository;

    /**
     * Obtiene la lista de nombres de políticas asignadas a un rol específico.
     *
     * Este método es invocado desde CustomUserDetailsService para construir
     * la lista de GrantedAuthority del usuario autenticado.
     *
     * @param rolId ID del rol
     * @return lista de nombres de políticas (ej: ["CREAR_POLITICA", "VER_POLITICAS"])
     */
    @Transactional(readOnly = true)
    public List<String> obtenerPoliticasPorRolId(Integer rolId) {
        log.debug("Obteniendo políticas para rol ID: {}", rolId);

        List<String> politicas = detallePoliticaRepository.findByRolIdWithPolitica(rolId)
                .stream()
                .map(dp -> dp.getPolitica().getNombrePolitica())
                .toList();

        log.debug("Políticas encontradas para rol ID {}: {}", rolId, politicas);
        return politicas;
    }

    /**
     * Verifica si un rol tiene una política específica asignada.
     *
     * @param rolId           ID del rol
     * @param nombrePolitica  nombre de la política a verificar
     * @return true si el rol tiene la política, false en caso contrario
     */
    @Transactional(readOnly = true)
    public boolean rolTienePolitica(Integer rolId, String nombrePolitica) {
        List<String> politicas = obtenerPoliticasPorRolId(rolId);
        boolean tiene = politicas.contains(nombrePolitica);
        log.debug("Rol ID {} {} la política '{}'", rolId, tiene ? "TIENE" : "NO TIENE", nombrePolitica);
        return tiene;
    }
}
