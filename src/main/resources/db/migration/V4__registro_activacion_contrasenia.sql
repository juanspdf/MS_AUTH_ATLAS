-- ============================================================
-- MIGRACIÓN: Registro de Usuarios con Activación por Correo
-- Esquema: usr
-- ============================================================
-- Ejecutar este script en PostgreSQL para habilitar:
-- 1. Campo de correo electrónico en usuarios
-- 2. Tabla de tokens de activación/recuperación de contraseña
-- ============================================================

-- 1. Agregar columna de correo electrónico a la tabla de usuarios
ALTER TABLE usr.usuarios
    ADD COLUMN IF NOT EXISTS correo VARCHAR(100);

-- Asignar correo temporal a usuarios existentes para cumplir NOT NULL
UPDATE usr.usuarios
SET correo = CONCAT(nombre_usuario, '@temporal.sistemasgaia.com')
WHERE correo IS NULL;

-- Aplicar restricciones después de poblar los datos
ALTER TABLE usr.usuarios
    ALTER COLUMN correo SET NOT NULL;

-- Índice único para el correo
CREATE UNIQUE INDEX IF NOT EXISTS idx_usuario_correo
    ON usr.usuarios (correo);

-- 2. Crear tabla de tokens de activación/recuperación
CREATE TABLE IF NOT EXISTS usr.tokens_activacion (
    id_token_activacion UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    token               VARCHAR(36) NOT NULL UNIQUE,
    tipo_token          VARCHAR(20) NOT NULL,
    id_usuario          UUID        NOT NULL,
    fecha_creacion      TIMESTAMP   NOT NULL DEFAULT NOW(),
    fecha_expiracion    TIMESTAMP   NOT NULL,
    usado               BOOLEAN     NOT NULL DEFAULT FALSE,
    fecha_uso           TIMESTAMP,

    CONSTRAINT fk_token_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usr.usuarios (id_usuario)
        ON DELETE CASCADE,

    CONSTRAINT chk_tipo_token
        CHECK (tipo_token IN ('ACTIVACION', 'RECUPERACION'))
);

-- Índice para búsqueda rápida por token
CREATE INDEX IF NOT EXISTS idx_token_activacion_token
    ON usr.tokens_activacion (token);

-- Índice para búsqueda por usuario
CREATE INDEX IF NOT EXISTS idx_token_activacion_usuario
    ON usr.tokens_activacion (id_usuario);

-- Índice para limpieza de tokens expirados
CREATE INDEX IF NOT EXISTS idx_token_activacion_expiracion
    ON usr.tokens_activacion (fecha_expiracion);

-- Comentarios descriptivos
COMMENT ON TABLE usr.tokens_activacion IS 'Tokens de activación de cuenta y recuperación de contraseña';
COMMENT ON COLUMN usr.tokens_activacion.token IS 'Token UUID único enviado por correo electrónico';
COMMENT ON COLUMN usr.tokens_activacion.tipo_token IS 'Tipo: ACTIVACION (registro) o RECUPERACION (reset password)';
COMMENT ON COLUMN usr.tokens_activacion.usado IS 'Indica si el token ya fue utilizado (single-use)';
COMMENT ON COLUMN usr.tokens_activacion.fecha_uso IS 'Fecha en que se usó el token para establecer la contraseña';
COMMENT ON COLUMN usr.usuarios.correo IS 'Correo electrónico del usuario para activación y recuperación';
