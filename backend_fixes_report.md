# MS_AUTH_ATLAS — Reporte de Correcciones Backend

## Causa raíz de cada problema encontrado

| # | Problema | Causa raíz |
|---|----------|-----------|
| 1 | No hay envío real de correos | La configuración SMTP usa defaults placeholder (`user@smtp-brevo.com` / `password`). Sin credenciales reales, el envío falla |
| 2 | Error de correo no lanza excepción | Los métodos `enviarCorreoActivacion()` y `enviarCorreoRecuperacion()` tenían `@Async`, lo cual ejecuta en otro hilo y **silencia las excepciones** — el hilo principal nunca se entera del fallo |
| 3 | API responde 200 OK en errores de correo | Consecuencia directa de `@Async`: como la excepción se pierde en el hilo async, el controller siempre responde 200. Además, se lanzaba `RuntimeException` genérica que no tenía handler dedicado en `GlobalExceptionHandler` |
| 4 | Endpoint de roles no existía / no excluía ADMIN | Ya existían `RolController`, `RolService` y `RolRepository.findByTipoRolNot()` correctos. Solo faltaba el campo `activo` en `RolResponseDto` |
| 5 | Crear/actualizar usuarios permite asignar rol ADMIN | `UsuarioService.crear()` y `actualizar()` no validaban si el rol solicitado era ADMIN antes de persistir |
| 6 | Políticas tenían CRUD completo desde API | `PoliticaController` exponía POST, PUT, DELETE para crear/editar/eliminar políticas. `PoliticaService` tenía métodos `crear()`, `actualizar()`, `eliminar()` |
| 7 | ADMIN podía editarse/eliminarse a sí mismo | `UsuarioController` no pasaba el ID del usuario autenticado al service. `UsuarioService.actualizar()` y `eliminar()` no recibían ni comparaban IDs |
| 8 | Frontend no ve políticas para asignar/desasignar | No existían endpoints `GET /api/politicas/rol/{rolId}`, `POST .../asignar`, `DELETE .../desasignar`. La asignación era bulk por usuario y no había desasignación |
| 9 | `ForbiddenOperationException` devolvía 500 | Aunque tenía `@ResponseStatus(403)`, el `GlobalExceptionHandler` lo interceptaba primero con su handler genérico `Exception` y devolvía 500 |

---

## Archivos creados

| Archivo | Descripción |
|---------|-------------|
| `exceptions/EmailSendException.java` | Excepción para fallos SMTP (→ HTTP 502) |
| `dto/politica/AsignarPoliticaRequestDto.java` | DTO para asignar una política individual a un rol |
| `test/.../services/UsuarioServiceTest.java` | 6 tests: ADMIN role blocked, self-edit/delete blocked |
| `test/.../services/PoliticaServiceTest.java` | 8 tests: listar, listar por rol, asignar, duplicado, desasignar |
| `test/.../services/RolServiceTest.java` | 1 test: roles no incluyen ADMIN |
| `test/.../services/EmailServiceTest.java` | 2 tests: SMTP fallo → excepción, envío exitoso |

## Archivos modificados

| Archivo | Cambios |
|---------|---------|
| `services/EmailService.java` | Removido `@Async`, ahora lanza `EmailSendException` en fallo |
| `exceptions/GlobalExceptionHandler.java` | Handlers para `ForbiddenOperationException` (403) y `EmailSendException` (502) |
| `services/UsuarioService.java` | Validación ADMIN en `crear()`, self-edit/delete en `actualizar()`/`eliminar()` |
| `controllers/UsuarioController.java` | Inyecta `JwtService`, extrae UUID del token para actualizar/eliminar |
| `services/PoliticaService.java` | Removido CRUD. Nuevos métodos: `listarPorRol()`, `asignarPoliticaARol()`, `desasignarPoliticaDeRol()` |
| `controllers/PoliticaController.java` | Removidos endpoints CRUD. Nuevos: `GET /rol/{rolId}`, `POST /rol/{rolId}/asignar`, `DELETE /rol/{rolId}/desasignar/{politicaId}` |
| `repositories/DetallePoliticaRepository.java` | Nuevos métodos: `existsByPoliticaIdAndRolId()`, `deleteByPoliticaIdAndRolId()` |
| `dto/rol/RolResponseDto.java` | Agregado campo `activo` |
| `services/RolService.java` | Incluye `activo` en el mapping |
| `services/RegistroService.java` | Validación ADMIN en registro público |
| `application.yaml` | Variables de correo `MAIL_*` (con fallback a `SMTP_*`) |

---

## Endpoints nuevos o corregidos

### Nuevos

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| `GET` | `/api/politicas/rol/{rolId}` | ADMIN | Listar políticas asignadas a un rol |
| `POST` | `/api/politicas/rol/{rolId}/asignar` | ADMIN | Asignar una política a un rol |
| `DELETE` | `/api/politicas/rol/{rolId}/desasignar/{politicaId}` | ADMIN | Desasignar una política de un rol |

### Corregidos

| Método | Ruta | Cambio |
|--------|------|--------|
| `GET` | `/api/politicas` | Solo lectura (removidos POST/PUT/DELETE) |
| `PUT` | `/api/usuarios/{id}` | Valida ADMIN role + self-edit protection |
| `DELETE` | `/api/usuarios/{id}` | Valida self-delete protection |
| `POST` | `/api/usuarios` | Rechaza rol ADMIN (403) |
| `POST` | `/api/auth/register` | Rechaza rol ADMIN (403) |

### Removidos (por diseño)

| Método | Ruta | Razón |
|--------|------|-------|
| `POST` | `/api/politicas` | Políticas se administran en BD |
| `PUT` | `/api/politicas/{id}` | Políticas se administran en BD |
| `DELETE` | `/api/politicas/{id}` | Políticas se administran en BD |
| `GET` | `/api/politicas/{id}` | No necesario para el frontend |
| `POST` | `/api/politicas/usuarios/{usuarioId}` | Reemplazado por asignación por rol |
| `POST` | `/api/usuarios/{usuarioId}/politicas` | Reemplazado por asignación por rol |

---

## Variables de entorno necesarias para correos

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `MAIL_HOST` | Servidor SMTP | `smtp-relay.brevo.com` |
| `MAIL_PORT` | Puerto SMTP | `587` |
| `MAIL_USERNAME` | Usuario SMTP | `tu-api-key@smtp-brevo.com` |
| `MAIL_PASSWORD` | Password/API key SMTP | `xsmtpsib-...` |
| `MAIL_FROM` | Remitente (email) | `noreply@tudominio.com` |
| `EMAIL_FROM` | Remitente (alternativo, ya existente) | `noreply@sistemasgaia.com` |
| `EMAIL_FROM_NAME` | Nombre del remitente | `Sistema ATLAS` |
| `FRONTEND_URL` | URL base del frontend (para enlaces en emails) | `http://localhost:4200` |

> [!NOTE]
> Las variables `MAIL_*` tienen prioridad. Si no están definidas, se usan `SMTP_*` como fallback para compatibilidad.

---

## Ejemplos de respuestas exitosas

### GET /api/politicas
```json
{
  "status": 200,
  "mensaje": "Políticas obtenidas correctamente",
  "datos": [
    {
      "id": "uuid-politica-1",
      "nombrePolitica": "POLITICA_CREAR_USUARIO",
      "activo": true,
      "fechaCreacion": "2026-06-01T10:00:00",
      "fechaActualizacion": null
    }
  ],
  "timestamp": "2026-06-03T18:00:00"
}
```

### GET /api/politicas/rol/2
```json
{
  "status": 200,
  "mensaje": "Políticas del rol obtenidas correctamente",
  "datos": [
    {
      "id": "uuid-politica-5",
      "nombrePolitica": "POLITICA_CARGAR_ARCHIVO_A_NORMA",
      "activo": true,
      "fechaCreacion": "2026-06-01T10:00:00",
      "fechaActualizacion": null
    }
  ],
  "timestamp": "2026-06-03T18:00:00"
}
```

### POST /api/politicas/rol/2/asignar
```json
// Request:
{ "politicaId": "uuid-politica-1" }

// Response (201):
{
  "status": 201,
  "mensaje": "Política asignada correctamente al rol",
  "datos": null,
  "timestamp": "2026-06-03T18:00:00"
}
```

### DELETE /api/politicas/rol/2/desasignar/{politicaId}
```json
{
  "status": 200,
  "mensaje": "Política desasignada correctamente del rol",
  "datos": null,
  "timestamp": "2026-06-03T18:00:00"
}
```

### GET /api/roles
```json
{
  "status": 200,
  "mensaje": "Roles obtenidos correctamente",
  "datos": [
    { "id": 2, "tipoRol": "CONSULTOR", "descripcionRol": "Realiza evaluaciones", "activo": true },
    { "id": 3, "tipoRol": "GESTOR_NORMAS", "descripcionRol": "Gestiona normas", "activo": true }
  ],
  "timestamp": "2026-06-03T18:00:00"
}
```

---

## Ejemplos de respuestas de error

### Crear usuario con rol ADMIN (403)
```json
{
  "status": 403,
  "mensaje": "No se permite crear usuarios con rol ADMIN",
  "timestamp": "2026-06-03T18:00:00"
}
```

### ADMIN editándose a sí mismo (403)
```json
{
  "status": 403,
  "mensaje": "Un administrador no puede editarse a sí mismo",
  "timestamp": "2026-06-03T18:00:00"
}
```

### ADMIN eliminándose a sí mismo (403)
```json
{
  "status": 403,
  "mensaje": "Un administrador no puede eliminarse a sí mismo",
  "timestamp": "2026-06-03T18:00:00"
}
```

### Política ya asignada (409)
```json
{
  "status": 409,
  "mensaje": "La política 'POLITICA_CREAR_USUARIO' ya está asignada al rol 'CONSULTOR'",
  "timestamp": "2026-06-03T18:00:00"
}
```

### Fallo SMTP (502)
```json
{
  "status": 502,
  "mensaje": "No se pudo enviar el correo a usuario@email.com. Por favor, intenta nuevamente más tarde.",
  "timestamp": "2026-06-03T18:00:00"
}
```

---

## Cómo probar cada endpoint

### Con curl

```bash
# Login (obtener token)
curl -X POST http://localhost:8081/ms-autenticacion/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"nombreUsuario":"admin","contrasenia":"admin123"}'

# Guardar token
TOKEN="eyJhbGciOiJSUz..."

# GET /api/roles (sin ADMIN)
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/ms-autenticacion/api/roles

# POST /api/usuarios con rol ADMIN (debe dar 403)
curl -X POST http://localhost:8081/ms-autenticacion/api/usuarios \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nombreUsuario":"test","contrasenia":"Password1","correo":"test@t.com","nombre":"T","apellido":"T","rolId":1}'

# GET /api/politicas (listar todas)
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/ms-autenticacion/api/politicas

# GET /api/politicas/rol/2 (políticas del rol CONSULTOR)
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/ms-autenticacion/api/politicas/rol/2

# POST /api/politicas/rol/2/asignar
curl -X POST http://localhost:8081/ms-autenticacion/api/politicas/rol/2/asignar \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"politicaId":"uuid-de-la-politica"}'

# DELETE /api/politicas/rol/2/desasignar/{politicaId}
curl -X DELETE http://localhost:8081/ms-autenticacion/api/politicas/rol/2/desasignar/uuid-de-la-politica \
  -H "Authorization: Bearer $TOKEN"
```

### En Swagger
```
http://localhost:8081/ms-autenticacion/swagger-ui.html
```

---

## Comandos para levantar el backend

```bash
# Definir JAVA_HOME si no está configurado
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10+7'

# Compilar
.\mvnw.cmd compile

# Correr tests
.\mvnw.cmd test -Dtest="UsuarioServiceTest,PoliticaServiceTest,RolServiceTest,EmailServiceTest"

# Levantar el backend
.\mvnw.cmd spring-boot:run
```

---

## Qué debe revisar el frontend para consumir políticas

### Endpoints disponibles para políticas

| Acción | Endpoint | Método | Body |
|--------|----------|--------|------|
| Listar todas las políticas | `GET /api/politicas` | GET | — |
| Listar políticas asignadas a un rol | `GET /api/politicas/rol/{rolId}` | GET | — |
| Asignar política a un rol | `POST /api/politicas/rol/{rolId}/asignar` | POST | `{"politicaId":"uuid"}` |
| Desasignar política de un rol | `DELETE /api/politicas/rol/{rolId}/desasignar/{politicaId}` | DELETE | — |

### Flujo recomendado en el frontend

1. **Pantalla de gestión de políticas por rol:**
   - Cargar roles con `GET /api/roles` (solo CONSULTOR y GESTOR_NORMAS)
   - Al seleccionar un rol, cargar sus políticas con `GET /api/politicas/rol/{rolId}`
   - Cargar todas las políticas disponibles con `GET /api/politicas`
   - Mostrar las no asignadas como "disponibles" (diferencia entre ambas listas)
   - Botón "Asignar" → `POST /api/politicas/rol/{rolId}/asignar`
   - Botón "Desasignar" → `DELETE /api/politicas/rol/{rolId}/desasignar/{politicaId}`

2. **Headers requeridos:**
   - `Authorization: Bearer <token>` (usuario con ROLE_ADMIN)
   - `Content-Type: application/json` (para POST)

3. **Manejo de errores:**
   - `401` → Redirigir a login
   - `403` → Mostrar "Sin permisos"
   - `404` → Rol o política no encontrada
   - `409` → Política ya asignada (duplicado)
   - `502` → Error enviando correo (mostrar mensaje al usuario)

4. **Campos del DTO de política (PoliticaResponseDto):**
   - `id` (UUID)
   - `nombrePolitica` (String)
   - `activo` (Boolean)
   - `fechaCreacion` (LocalDateTime)
   - `fechaActualizacion` (LocalDateTime)
