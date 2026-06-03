# AGENTS.md — MS_AUTH_ATLAS

## Resumen

**MS_AUTH_ATLAS** es un microservicio de autenticación y autorización para el sistema ATLAS, construido con Spring Boot 3.5.14 + Java 21 + PostgreSQL 16. Gestiona registro de usuarios, login/logout con JWT asimétrico (RS256) y control de acceso RBAC.

---

## Stack

| Tecnología | Versión |
|-----------|---------|
| Java | 21 |
| Spring Boot | 3.5.14 |
| PostgreSQL | 16 |
| JWT | jjwt 0.12.6 (RS256, RSA 2048 bits asimétrico) |
| Flyway | 11.8.1 (migraciones) |
| Swagger | springdoc-openapi 2.8.8 |
| Hibernate | 6.x (ddl-auto: validate) |

---

## Arranque del proyecto

### Requisitos
- Java 21
- PostgreSQL 16 corriendo en `localhost:5432`
- Base de datos `atlas_db` creada y vacía

### Arranque local
```bash
./mvnw spring-boot:run
```

Flyway crea automáticamente las tablas (`V1__init_schema.sql`) y los datos semilla (`V2__seed_data.sql`).

### Docker Compose
```bash
docker-compose up --build
```
Levanta PostgreSQL + la app. Las variables de entorno se cargan del `.env`.

### Base URL
```
http://localhost:8081/ms-autenticacion
```

### Swagger
```
http://localhost:8081/ms-autenticacion/swagger-ui.html
```

### Credenciales por defecto
- Usuario: `admin`
- Contraseña: `admin123`

---

## Arquitectura de seguridad

### JWT — RS256 asimétrico
- **Llave privada**: firma los tokens (solo el servidor la tiene)
- **Llave pública**: verifica los tokens (puede distribuirse a otros microservicios)
- Expiración: 24 horas (configurable en `${JWT_EXPIRATION}`)
- Claims incluidos: `sub` (username), `idUsuario` (UUID), `rol` (String), `politicas` (List\<String\>)

### RBAC — Roles + Políticas
Cada usuario tiene un **rol** (ADMIN, CONSULTOR, GESTOR_NORMAS). Cada rol tiene **políticas** asignadas que representan permisos granulares.

| Rol | ID | Políticas asignadas |
|-----|----|---------------------|
| ADMIN | 1 | Todas (POLITICA_CREAR_USUARIO, POLITICA_ELIMINAR_USUARIO, POLITICA_VER_REPORTES, POLITICA_CARGAR_NORMAS, POLITICA_CARGAR_ARCHIVO_A_NORMA) |
| CONSULTOR | 2 | POLITICA_CARGAR_ARCHIVO_A_NORMA |
| GESTOR_NORMAS | 3 | POLITICA_CARGAR_NORMAS |

La autorización se implementa con `@PreAuthorize` combinando `hasRole` + `hasAuthority`:
```java
@PreAuthorize("hasRole('ADMIN') and hasAuthority('POLITICA_ASIGNAR')")
```

### Seguridad HTTP
- Sesiones **stateless** (sin estado en servidor)
- CSRF **deshabilitado** (API REST)
- Login y registro son **públicos**
- Logout **requiere autenticación** (invalida el JWT en blacklist)
- CRUD de usuarios y políticas: solo ADMIN (con política específica según endpoint)

---

## Endpoints de la API

### Autenticación (`/api/auth`)

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| POST | `/api/auth/login` | Público | Iniciar sesión, retorna JWT |
| POST | `/api/auth/logout` | Bearer | Invalida el JWT actual |
| POST | `/api/auth/register` | Público | Registrar nuevo usuario (sin contraseña) |
| GET | `/api/auth/validar-token?token=` | Público | Validar token de activación/recuperación |
| POST | `/api/auth/establecer-contrasenia` | Público | Establecer contraseña con token |
| POST | `/api/auth/recuperar-contrasenia` | Público | Solicitar recuperación por correo |
| POST | `/api/auth/reenviar-activacion` | Público | Reenviar correo de activación |

#### POST /api/auth/login

Request:
```json
{
    "nombreUsuario": "admin",
    "contrasenia": "admin123"
}
```

Response (200):
```json
{
    "status": 200,
    "mensaje": "Autenticación exitosa",
    "datos": {
        "token": "eyJhbGciOiJSUzI1NiJ9...",
        "tipo": "Bearer",
        "idUsuario": "uuid-aqui",
        "nombreUsuario": "admin",
        "rol": "ADMIN"
    },
    "timestamp": "2026-05-30T20:00:00"
}
```

Errores:
- **401** — Credenciales inválidas
- **400** — Campos vacíos (validación)

#### POST /api/auth/register

Request (rol por defecto CONSULTOR si no se envía rolId):
```json
{
    "nombreUsuario": "nuevo_usuario",
    "correo": "usuario@email.com",
    "nombre": "Nombre",
    "apellido": "Apellido",
    "rolId": 2
}
```
`rolId` es opcional. Valores: `1`=ADMIN, `2`=CONSULTOR, `3`=GESTOR_NORMAS.

Response (201):
```json
{
    "status": 201,
    "mensaje": "Usuario registrado exitosamente",
    "datos": {
        "idUsuario": "uuid-aqui",
        "nombreUsuario": "nuevo_usuario",
        "correo": "usuario@email.com",
        "nombreCompleto": "Nombre Apellido",
        "rol": "CONSULTOR",
        "mensaje": "Usuario registrado. Se ha enviado un correo a usuario@email.com para establecer la contraseña",
        "fechaCreacion": "2026-05-30T20:00:00"
    },
    "timestamp": "2026-05-30T20:00:00"
}
```

Errores:
- **409** — Username o correo ya existe
- **400** — Validación de campos

#### POST /api/auth/establecer-contrasenia

Request:
```json
{
    "token": "token-uuid-recibido-por-correo",
    "nuevaContrasenia": "Password123",
    "confirmarContrasenia": "Password123"
}
```
Validación de contraseña: mínimo 8 caracteres, al menos una mayúscula, una minúscula y un dígito.

Response (200):
```json
{
    "status": 200,
    "mensaje": "Contraseña establecida exitosamente",
    "datos": null,
    "timestamp": "2026-05-30T20:00:00"
}
```

#### POST /api/auth/recuperar-contrasenia

Request:
```json
{
    "correo": "usuario@email.com"
}
```
Siempre retorna 200 (no revela si el correo existe o no).

#### POST /api/auth/reenviar-activacion

Request:
```json
{
    "correo": "usuario@email.com"
}
```

#### POST /api/auth/logout

Requiere header `Authorization: Bearer <token>`. Invalida el JWT (blacklist).

---

### Usuarios CRUD (`/api/usuarios`)

**Todos los endpoints requieren `ROLE_ADMIN`.**

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/usuarios` | Listar todos los usuarios activos |
| GET | `/api/usuarios/{id}` | Buscar usuario por UUID |
| POST | `/api/usuarios` | Crear usuario (requiere contrasenia en el body) |
| PUT | `/api/usuarios/{id}` | Actualizar usuario (contrasenia opcional en update) |
| DELETE | `/api/usuarios/{id}` | Soft-delete (activo=false) |

#### POST /api/usuarios
Requiere header `Authorization: Bearer <token>`.

Request:
```json
{
    "nombreUsuario": "nuevo_admin",
    "contrasenia": "Admin123",
    "correo": "admin2@gmail.com",
    "nombre": "Admin",
    "apellido": "Dos",
    "rolId": 1
}
```

Response (201):
```json
{
    "status": 201,
    "mensaje": "Usuario creado correctamente",
    "datos": {
        "id": "uuid-aqui",
        "nombreUsuario": "nuevo_admin",
        "correo": "admin2@gmail.com",
        "nombre": "Admin",
        "apellido": "Dos",
        "rolId": 1,
        "tipoRol": "ADMIN",
        "descripcionRol": "Administrador del sistema con acceso total",
        "activo": true,
        "fechaCreacion": "2026-05-30T20:00:00",
        "ultimaModificacion": null,
        "ultimaEvaluacion": null
    },
    "timestamp": "2026-05-30T20:00:00"
}
```

#### PUT /api/usuarios/{id}
La contraseña es **opcional** en updates. Si no se envía o está vacía, no se modifica.

Request (sin cambiar contraseña):
```json
{
    "nombreUsuario": "nuevo_admin",
    "correo": "nuevo_correo@gmail.com",
    "nombre": "Admin Modificado",
    "apellido": "Modificado",
    "rolId": 1
}
```

Errores:
- **404** — Usuario no encontrado
- **409** — Username o correo ya existe
- **400** — Validación

---

### Políticas CRUD (`/api/politicas`)

**Todos los endpoints requieren `ROLE_ADMIN` + política específica.**

| Método | Ruta | Política requerida | Descripción |
|--------|------|--------------------|-------------|
| GET | `/api/politicas` | POLITICA_VER | Listar políticas activas |
| GET | `/api/politicas/{id}` | POLITICA_VER | Buscar política por UUID |
| POST | `/api/politicas` | POLITICA_CREAR | Crear política |
| PUT | `/api/politicas/{id}` | POLITICA_EDITAR | Actualizar política |
| DELETE | `/api/politicas/{id}` | POLITICA_ELIMINAR | Soft-delete |

#### POST /api/politicas

Request:
```json
{
    "nombrePolitica": "POLITICA_EVALUAR_NORMA"
}
```
Validación: solo mayúsculas y guiones bajos (`^[A-Z_]+$`), máximo 40 caracteres. La convención es usar prefijo `POLITICA_`.

Response (201):
```json
{
    "status": 201,
    "mensaje": "Política creada correctamente",
    "datos": {
        "id": "uuid-aqui",
        "nombrePolitica": "POLITICA_EVALUAR_NORMA",
        "activo": true,
        "fechaCreacion": "2026-05-30T20:00:00",
        "fechaActualizacion": null
    },
    "timestamp": "2026-05-30T20:00:00"
}
```

---

### Asignación de Políticas

| Método | Ruta | Política requerida |
|--------|------|--------------------|
| POST | `/api/usuarios/{usuarioId}/politicas` | POLITICA_ASIGNAR |
| POST | `/api/politicas/usuarios/{usuarioId}` | POLITICA_ASIGNAR |

Ambos endpoints hacen lo mismo (el segundo es ruta alternativa). Las políticas se asignan **al rol del usuario**, no al usuario directamente.

#### POST /api/usuarios/{usuarioId}/politicas

Request:
```json
{
    "politicasIds": [
        "uuid-politica-1",
        "uuid-politica-2"
    ]
}
```

Response (200):
```json
{
    "status": 200,
    "mensaje": "Políticas asignadas correctamente",
    "datos": {
        "nombreUsuario": "admin",
        "tipoRol": "ADMIN",
        "politicasAsignadas": 2,
        "politicasDuplicadas": 0,
        "politicasNuevas": ["POLITICA_CREAR_USUARIO", "POLITICA_ELIMINAR_USUARIO"],
        "politicasYaExistentes": []
    },
    "timestamp": "2026-05-30T20:00:00"
}
```

No se pueden asignar políticas inactivas (soft-deleteadas) — retorna 409.

---

## Flujo completo de registro y activación

```
1. POST /api/auth/register              → Usuario creado (activo=false), token enviado por correo
2. GET  /api/auth/validar-token?token=   → Frontend valida que el token es válido
3. POST /api/auth/establecer-contrasenia → Usuario activo, puede hacer login
4. POST /api/auth/login                  → Retorna JWT con rol y políticas
```

Si el token expira (24h):
```
POST /api/auth/reenviar-activacion → Nuevo token generado, correo reenviado
```

---

## Formato de respuestas

Todas las respuestas usan el wrapper `ApiResponseDto<T>`:

```json
{
    "status": 200,
    "mensaje": "Operación exitosa",
    "datos": { ... },
    "errores": null,
    "timestamp": "2026-05-30T20:00:00"
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| status | int | Código HTTP |
| mensaje | string | Descripción legible |
| datos | T/array/null | Payload de la respuesta |
| errores | array/null | Lista de errores de validación (solo en 400) |
| timestamp | string | ISO 8601 |

---

## Roles del sistema

| Rol | ID | Descripción |
|-----|----|-------------|
| ADMIN | 1 | Administrador con acceso total |
| CONSULTOR | 2 | Realiza evaluaciones a normas (rol por defecto en registro sin rolId) |
| GESTOR_NORMAS | 3 | Gestiona normas |

---

## Políticas semilla

| Política | Asignada a |
|----------|------------|
| POLITICA_CREAR_USUARIO | ADMIN |
| POLITICA_ELIMINAR_USUARIO | ADMIN |
| POLITICA_VER_REPORTES | ADMIN |
| POLITICA_CARGAR_NORMAS | ADMIN, GESTOR_NORMAS |
| POLITICA_CARGAR_ARCHIVO_A_NORMA | ADMIN, CONSULTOR |

---

## Validaciones

### Contraseña
- Mínimo 8 caracteres
- Al menos una mayúscula, una minúscula y un dígito
- Regex: `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$`

### Username
- Máximo 10 caracteres
- Obligatorio

### Políticas
- Solo mayúsculas y guiones bajos: `^[A-Z_]+$`
- Máximo 40 caracteres
- Convención: usar prefijo `POLITICA_`

---

## Configuración importante

### Variables de entorno (`.env` para Docker)

| Variable | Descripción |
|----------|-------------|
| JWT_PRIVATE_KEY | Llave privada RSA PKCS#8 en Base64 |
| JWT_PUBLIC_KEY | Llave pública RSA X.509 en Base64 |
| JWT_EXPIRATION | Expiración del JWT en ms (default: 86400000 = 24h) |
| FRONTEND_URL | URL del frontend para enlaces en correos |
| SMTP_HOST | Servidor SMTP |
| SMTP_PORT | Puerto SMTP |
| SMTP_USERNAME | Usuario SMTP |
| SMTP_PASSWORD | Password SMTP |
| EMAIL_FROM | Remitente de correos |
| EMAIL_FROM_NAME | Nombre del remitente |
| TOKEN_EXPIRATION_HOURS | Horas de expiración de tokens de activación |

### application.yaml

```yaml
server:
  port: 8081
  servlet:
    context-path: /ms-autenticacion

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/atlas_db
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    schemas: usr
    baseline-on-migrate: true

jwt:
  private-key: ${JWT_PRIVATE_KEY:<default-base64-key>}
  public-key: ${JWT_PUBLIC_KEY:<default-base64-key>}
  expiration: ${JWT_EXPIRATION:86400000}
```

---

## Estructura del proyecto

```
src/main/java/com/sistemasgaia/atlas/msautenticacion/
├── config/
│   ├── JpaAuditingConfig.java
│   └── OpenApiConfig.java
├── controllers/
│   ├── AuthController.java
│   ├── PoliticaController.java
│   └── UsuarioController.java
├── dto/
│   ├── ApiResponseDto.java
│   ├── auth/
│   │   ├── EstablecerContraseniaRequestDto.java
│   │   ├── LoginRequestDto.java
│   │   ├── LoginResponseDto.java
│   │   ├── RecuperarContraseniaRequestDto.java
│   │   ├── RegistroRequestDto.java
│   │   ├── RegistroResponseDto.java
│   │   └── ValidarTokenResponseDto.java
│   ├── politica/
│   │   ├── AsignarPoliticasRequestDto.java
│   │   ├── AsignarPoliticasResponseDto.java
│   │   ├── PoliticaRequestDto.java
│   │   └── PoliticaResponseDto.java
│   └── usuario/
│       ├── UsuarioRequestDto.java
│       └── UsuarioResponseDto.java
├── enums/
│   ├── TipoRol.java
│   └── TipoToken.java
├── exceptions/
│   ├── BusinessException.java
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── UnauthorizedException.java
├── models/
│   ├── AuditableEntity.java
│   ├── DetallePolitica.java
│   ├── DetallePoliticaId.java
│   ├── Politica.java
│   ├── Rol.java
│   ├── TokenActivacion.java
│   ├── TokenInvalidado.java
│   └── Usuario.java
├── repositories/
│   ├── DetallePoliticaRepository.java
│   ├── PoliticaRepository.java
│   ├── RolRepository.java
│   ├── TokenActivacionRepository.java
│   ├── TokenInvalidadoRepository.java
│   └── UsuarioRepository.java
├── security/
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtService.java
│   └── SecurityConfig.java
└── services/
    ├── AuthService.java
    ├── AutorizacionService.java
    ├── EmailService.java
    ├── PoliticaService.java
    ├── RegistroService.java
    ├── TokenActivacionService.java
    ├── TokenInvalidadoService.java
    └── UsuarioService.java
```

---

## Postman

Hay una colección Postman en `postman/ATLAS_msautenticacion.postman_collection.json` con ~40 requests organizados en 8 secciones:
1. Auth - Login / Logout
2. Registro de Usuarios
3. Activacion / Recuperacion
4. Usuarios CRUD (requiere ADMIN)
5. Politicas CRUD (requiere ADMIN + politica especifica)
6. Asignacion de Politicas
7. Health y Documentacion
8. Errores 401 / 403

---

## Notas para el frontend

1. **Login guarda el token** en `localStorage`/`sessionStorage` y lo envía en cada request como header `Authorization: Bearer <token>`. El token expira en 24h.

2. **Registro** no pide contraseña. El usuario recibe un correo con un token. El frontend debe tener una ruta `/activar-cuenta?token=<token>` que renderice el formulario de establecer contraseña.

3. **Recuperación de contraseña** usa el mismo flujo: el usuario recibe un correo con token y va a `/restablecer-contrasenia?token=<token>`.

4. **Validar token** antes de mostrar el formulario: llamar a `GET /api/auth/validar-token?token=<token>`. Si retorna 200, mostrar el formulario. Si retorna 409/404, mostrar mensaje de error (token expirado o ya usado).

5. **Roles y UI**: después del login, el JWT contiene el `rol` del usuario. El frontend puede decodificar el payload del JWT (Base64) para obtener `rol` y `politicas` y así mostrar/ocultar secciones de la UI. No es necesario otro endpoint.

6. **401/403 handling**: si cualquier request retorna 401, redirigir a login. Si retorna 403, mostrar mensaje de "sin permisos".

7. **CORS** está configurado para aceptar cualquier origen (`*`) en desarrollo.
