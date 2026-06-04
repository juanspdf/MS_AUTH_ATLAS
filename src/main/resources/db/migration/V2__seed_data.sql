-- Roles del sistema
INSERT INTO sec.roles (tipo_rol, descripcion_rol) VALUES
    ('ADMIN',         'Administrador del sistema con acceso total'),
    ('CONSULTOR',     'Consultor que realiza evaluaciones a normas'),
    ('GESTOR_NORMAS', 'Gestor encargado de la gestión de normas')
ON CONFLICT (tipo_rol) DO NOTHING;

-- Políticas del sistema
INSERT INTO sec.politicas (politica) VALUES
    ('POLITICA_VER_REPORTES'),
    ('POLITICA_CARGAR_NORMAS'),
    ('POLITICA_CARGAR_ARCHIVO_A_NORMA')
ON CONFLICT (politica) DO NOTHING;

-- ADMIN tiene todas las políticas activas
INSERT INTO sec.detalles_politicas (id_politica, rol_id)
SELECT p.id_politica, r.rol_id FROM sec.politicas p, sec.roles r
WHERE r.tipo_rol = 'ADMIN' AND p.activo = true
ON CONFLICT (id_politica, rol_id) DO NOTHING;

-- CONSULTOR tiene POLITICA_CARGAR_ARCHIVO_A_NORMA
INSERT INTO sec.detalles_politicas (id_politica, rol_id)
SELECT p.id_politica, r.rol_id FROM sec.politicas p, sec.roles r
WHERE r.tipo_rol = 'CONSULTOR' AND p.politica = 'POLITICA_CARGAR_ARCHIVO_A_NORMA'
ON CONFLICT (id_politica, rol_id) DO NOTHING;

-- GESTOR_NORMAS tiene POLITICA_CARGAR_NORMAS
INSERT INTO sec.detalles_politicas (id_politica, rol_id)
SELECT p.id_politica, r.rol_id FROM sec.politicas p, sec.roles r
WHERE r.tipo_rol = 'GESTOR_NORMAS' AND p.politica = 'POLITICA_CARGAR_NORMAS'
ON CONFLICT (id_politica, rol_id) DO NOTHING;

-- Usuario admin por defecto (admin / admin123)
INSERT INTO usr.usuarios (nombre_usuario, contrasenia, correo, nombre, apellido, rol_id)
SELECT 'admin', '$2a$10$JSkoX7Ch7atRCNpPMmmI.OXhLSNsSmBUELu8q9MRmlrkD3H2eO/ya', 'admin@sistemasgaia.com', 'Administrador', 'Sistema', r.rol_id
FROM sec.roles r WHERE r.tipo_rol = 'ADMIN'
ON CONFLICT (nombre_usuario) DO NOTHING;
