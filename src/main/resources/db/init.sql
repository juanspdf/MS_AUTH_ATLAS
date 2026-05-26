-- ============================================================
-- ATLAS - Script de inicialización de Base de Datos
-- Microservicio: msautenticacion
-- Motor: PostgreSQL
-- ============================================================

-- ============================================================
-- 1. CREACIÓN DE SCHEMAS
-- ============================================================
CREATE SCHEMA IF NOT EXISTS sec;  -- Seguridad (roles, políticas)
CREATE SCHEMA IF NOT EXISTS usr;  -- Usuarios

-- ============================================================
-- 2. TABLA DE ROLES (sec.roles)
-- ============================================================
CREATE TABLE IF NOT EXISTS sec.roles (
    rol_id       SERIAL       PRIMARY KEY,
    tipo_rol     VARCHAR(20)  NOT NULL,
    descripcion_rol VARCHAR(100),
    activo       BOOLEAN      DEFAULT TRUE,
    fecha_creacion    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 3. TABLA DE POLÍTICAS (sec.politicas)
-- ============================================================
CREATE TABLE IF NOT EXISTS sec.politicas (
    id_politica  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    politica     VARCHAR(24)  NOT NULL,
    activo       BOOLEAN      DEFAULT TRUE,
    fecha_creacion    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 4. TABLA DE DETALLE POLÍTICAS (sec.detalles_politicas)
-- ============================================================
CREATE TABLE IF NOT EXISTS sec.detalles_politicas (
    id_politica  UUID         NOT NULL,
    rol_id       INTEGER      NOT NULL,
    activo       BOOLEAN      DEFAULT TRUE,
    fecha_creacion    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_politica, rol_id),
    CONSTRAINT fk_detalle_politica FOREIGN KEY (id_politica) REFERENCES sec.politicas(id_politica),
    CONSTRAINT fk_detalle_rol      FOREIGN KEY (rol_id)      REFERENCES sec.roles(rol_id)
);

-- ============================================================
-- 5. TABLA DE USUARIOS (usr.usuarios)
-- ============================================================
CREATE TABLE IF NOT EXISTS usr.usuarios (
    id_usuario          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre_usuario      VARCHAR(10)  NOT NULL UNIQUE,
    contrasenia         VARCHAR(255) NOT NULL,
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

-- ============================================================
-- 6. DATOS DE EJEMPLO
-- ============================================================

-- ROLES
INSERT INTO sec.roles (tipo_rol, descripcion_rol) VALUES
    ('ADMIN',      'Administrador del sistema con acceso total'),
    ('SUPERVISOR', 'Supervisor con acceso a reportes y gestión'),
    ('CLIENTE',    'Cliente con acceso limitado')
ON CONFLICT DO NOTHING;

-- POLÍTICAS
INSERT INTO sec.politicas (id_politica, politica) VALUES
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'CREAR_USUARIO'),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'ELIMINAR_USUARIO'),
    ('c3d4e5f6-a7b8-9012-cdef-123456789012', 'VER_REPORTES')
ON CONFLICT DO NOTHING;

-- DETALLE POLÍTICAS: ADMIN tiene todas las políticas
INSERT INTO sec.detalles_politicas (id_politica, rol_id) VALUES
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 1),  -- ADMIN -> CREAR_USUARIO
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 1),  -- ADMIN -> ELIMINAR_USUARIO
    ('c3d4e5f6-a7b8-9012-cdef-123456789012', 1)   -- ADMIN -> VER_REPORTES
ON CONFLICT DO NOTHING;

-- DETALLE POLÍTICAS: SUPERVISOR tiene VER_REPORTES
INSERT INTO sec.detalles_politicas (id_politica, rol_id) VALUES
    ('c3d4e5f6-a7b8-9012-cdef-123456789012', 2)   -- SUPERVISOR -> VER_REPORTES
ON CONFLICT DO NOTHING;

-- USUARIO: admin / admin123 (BCrypt hash)
-- Hash BCrypt generado para: admin123
INSERT INTO usr.usuarios (nombre_usuario, contrasenia, nombre, apellido, rol_id) VALUES
    ('admin', '$2a$10$JSkoX7Ch7atRCNpPMmmI.OXhLSNsSmBUELu8q9MRmlrkD3H2eO/ya', 'Administrador', 'Sistema', 1)
ON CONFLICT (nombre_usuario) DO NOTHING;
