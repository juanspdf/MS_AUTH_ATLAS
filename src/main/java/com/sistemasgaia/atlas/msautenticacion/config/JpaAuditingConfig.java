package com.sistemasgaia.atlas.msautenticacion.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Habilita la auditoría JPA para @CreatedDate y @LastModifiedDate.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
