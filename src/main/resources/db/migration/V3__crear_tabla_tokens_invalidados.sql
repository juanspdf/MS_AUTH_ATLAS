-- ============================================================
-- MIGRACIÓN: Tabla de Tokens Invalidados (Blacklist para Logout)
-- Esquema: usr
-- ============================================================
-- Ejecutar este script en PostgreSQL antes de usar la funcionalidad de logout.
-- La tabla almacena hashes SHA-256 de tokens JWT que han sido invalidados.
-- Los tokens expirados se limpian automáticamente por un proceso programado.
-- ============================================================

CREATE TABLE IF NOT EXISTS usr.tokens_invalidados (
    id_token_invalidado UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    token_hash          VARCHAR(64) NOT NULL UNIQUE,
    nombre_usuario      VARCHAR(10) NOT NULL,
    fecha_invalidacion  TIMESTAMP   NOT NULL DEFAULT NOW(),
    fecha_expiracion    TIMESTAMP   NOT NULL
);

-- Índice para búsqueda rápida por hash del token (verificación en cada request)
CREATE INDEX IF NOT EXISTS idx_token_invalidado_token_hash
    ON usr.tokens_invalidados (token_hash);

-- Índice para limpieza eficiente de tokens expirados (proceso programado)
CREATE INDEX IF NOT EXISTS idx_token_invalidado_expiracion
    ON usr.tokens_invalidados (fecha_expiracion);

-- Comentarios descriptivos en la tabla
COMMENT ON TABLE usr.tokens_invalidados IS 'Blacklist de tokens JWT invalidados por logout';
COMMENT ON COLUMN usr.tokens_invalidados.token_hash IS 'Hash SHA-256 del token JWT (nunca se almacena el token en texto plano)';
COMMENT ON COLUMN usr.tokens_invalidados.nombre_usuario IS 'Username del usuario que realizó el logout';
COMMENT ON COLUMN usr.tokens_invalidados.fecha_invalidacion IS 'Fecha y hora en que se invalidó el token';
COMMENT ON COLUMN usr.tokens_invalidados.fecha_expiracion IS 'Fecha de expiración original del token JWT (para limpieza automática)';
