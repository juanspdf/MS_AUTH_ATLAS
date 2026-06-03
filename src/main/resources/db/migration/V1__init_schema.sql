CREATE SCHEMA IF NOT EXISTS sec;
CREATE SCHEMA IF NOT EXISTS usr;

-- Tabla de roles
CREATE TABLE IF NOT EXISTS sec.roles (
    rol_id              SERIAL       PRIMARY KEY,
    tipo_rol            VARCHAR(20)  NOT NULL UNIQUE,
    descripcion_rol     VARCHAR(100),
    activo              BOOLEAN      DEFAULT TRUE,
    fecha_creacion      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de politicas
CREATE TABLE IF NOT EXISTS sec.politicas (
    id_politica         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    politica            VARCHAR(40)  NOT NULL UNIQUE,
    activo              BOOLEAN      DEFAULT TRUE,
    fecha_creacion      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de detalle politicas (N:N entre roles y politicas)
CREATE TABLE IF NOT EXISTS sec.detalles_politicas (
    id_politica         UUID         NOT NULL,
    rol_id              INTEGER      NOT NULL,
    activo              BOOLEAN      DEFAULT TRUE,
    fecha_creacion      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_politica, rol_id),
    CONSTRAINT fk_detalle_politica FOREIGN KEY (id_politica) REFERENCES sec.politicas(id_politica),
    CONSTRAINT fk_detalle_rol      FOREIGN KEY (rol_id)      REFERENCES sec.roles(rol_id)
);

-- Tabla de usuarios
CREATE TABLE IF NOT EXISTS usr.usuarios (
    id_usuario          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre_usuario      VARCHAR(10)  NOT NULL UNIQUE,
    contrasenia         VARCHAR(255) NOT NULL,
    correo              VARCHAR(100) NOT NULL,
    nombre              VARCHAR(40)  NOT NULL,
    apellido            VARCHAR(40)  NOT NULL,
    rol_id              INTEGER      NOT NULL,
    ultima_evaluacion   TIMESTAMP,
    ultima_modificacion TIMESTAMP,
    activo              BOOLEAN      DEFAULT TRUE,
    fecha_creacion      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuario_rol FOREIGN KEY (rol_id) REFERENCES sec.roles(rol_id)
);

-- Tabla de tokens de activacion/recuperacion
CREATE TABLE IF NOT EXISTS usr.tokens_activacion (
    id_token_activacion UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    token               VARCHAR(36) NOT NULL UNIQUE,
    tipo_token          VARCHAR(20) NOT NULL,
    id_usuario          UUID        NOT NULL,
    fecha_creacion      TIMESTAMP   NOT NULL DEFAULT NOW(),
    fecha_expiracion    TIMESTAMP   NOT NULL,
    usado               BOOLEAN     NOT NULL DEFAULT FALSE,
    fecha_uso           TIMESTAMP,
    CONSTRAINT fk_token_usuario FOREIGN KEY (id_usuario) REFERENCES usr.usuarios (id_usuario) ON DELETE CASCADE,
    CONSTRAINT chk_tipo_token CHECK (tipo_token IN ('ACTIVACION', 'RECUPERACION'))
);

-- Tabla de tokens JWT invalidados (blacklist para logout)
CREATE TABLE IF NOT EXISTS usr.tokens_invalidados (
    id_token_invalidado UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    token_hash          VARCHAR(64) NOT NULL UNIQUE,
    nombre_usuario      VARCHAR(10) NOT NULL,
    fecha_invalidacion  TIMESTAMP   NOT NULL DEFAULT NOW(),
    fecha_expiracion    TIMESTAMP   NOT NULL
);

-- Indices
CREATE INDEX IF NOT EXISTS idx_usuario_correo ON usr.usuarios (correo);
CREATE INDEX IF NOT EXISTS idx_token_activacion_token ON usr.tokens_activacion (token);
CREATE INDEX IF NOT EXISTS idx_token_activacion_usuario ON usr.tokens_activacion (id_usuario);
CREATE INDEX IF NOT EXISTS idx_token_activacion_expiracion ON usr.tokens_activacion (fecha_expiracion);
CREATE INDEX IF NOT EXISTS idx_token_invalidado_token_hash ON usr.tokens_invalidados (token_hash);
CREATE INDEX IF NOT EXISTS idx_token_invalidado_expiracion ON usr.tokens_invalidados (fecha_expiracion);
