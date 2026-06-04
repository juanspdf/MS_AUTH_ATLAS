# ATLAS — Guía de Integración Frontend con msautenticacion

## Conexión al Backend

| Concepto | Valor |
|----------|-------|
| **Base URL** | `http://localhost:8081/ms-autenticacion` |
| **Swagger UI** | `http://localhost:8081/ms-autenticacion/swagger-ui.html` |
| **Protocolo** | HTTP REST + JSON |
| **CORS** | Abierto (`*`) en desarrollo |
| **Autenticación** | JWT Bearer Token (RS256 asimétrico) |
| **Expiración JWT** | 24 horas |

---

## Formato de Respuesta Estándar

**Todas** las respuestas del backend usan este wrapper:

```typescript
interface ApiResponse<T> {
  status:    number;        // Código HTTP (200, 201, 400, 401, 403, 404, 409, 502)
  mensaje:   string;        // Mensaje legible en español
  datos:     T | null;      // Payload (puede ser null)
  errores:   string[] | null; // Solo presente en errores 400 (validación)
  timestamp: string;        // ISO 8601 (ej: "2026-06-03T23:35:41.360708")
}
```

> **Nota:** Los campos `datos` y `errores` se omiten del JSON cuando son `null` (no aparecen en la respuesta).

---

## Códigos de Error

| HTTP | Cuándo ocurre | Acción del Frontend |
|------|---------------|---------------------|
| **400** | Validación de campos (campos vacíos, formato inválido) | Mostrar `errores[]` al usuario bajo cada campo |
| **401** | Token JWT ausente, expirado o inválido / Credenciales incorrectas | Redirigir a `/login`, limpiar token del storage |
| **403** | Usuario autenticado pero sin permisos para la operación | Mostrar "No tienes permisos para esta acción" |
| **404** | Recurso no encontrado (usuario, rol, política) | Mostrar "Recurso no encontrado" |
| **409** | Conflicto de negocio (username/correo duplicado, política ya asignada, token ya usado) | Mostrar `mensaje` al usuario |
| **502** | Fallo del servidor SMTP al enviar correo | Mostrar "Error al enviar correo, intenta más tarde" |
| **500** | Error interno inesperado | Mostrar "Error del servidor, intenta más tarde" |

### Ejemplo de error 400 (validación):
```json
{
  "status": 400,
  "mensaje": "Error de validación",
  "errores": [
    "El nombre de usuario es obligatorio",
    "La contraseña debe contener al menos una mayúscula, una minúscula y un dígito"
  ],
  "timestamp": "2026-06-03T23:35:41"
}
```

---

## Autenticación y Headers

### Header requerido en endpoints protegidos
```
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
Content-Type: application/json
```

### Contenido del JWT (payload decodificado)
```json
{
  "sub": "admin",
  "rol": "ADMIN",
  "politicas": ["POLITICA_VER_REPORTES", "POLITICA_CARGAR_NORMAS", "POLITICA_CARGAR_ARCHIVO_A_NORMA"],
  "idUsuario": "ca91d474-5736-4c78-af5f-c08fa2f4a9ff",
  "iat": 1780529555,
  "exp": 1780615955
}
```

El frontend puede decodificar el payload del JWT (Base64) para obtener `rol` y `politicas` sin necesidad de otro endpoint.

---

## Roles del Sistema

| ID | Nombre | Descripción |
|----|--------|-------------|
| 1 | `ADMIN` | Administrador total — **no aparece en `GET /api/roles`**, no se puede asignar |
| 2 | `CONSULTOR` | Realiza evaluaciones a normas (rol por defecto en registro público) |
| 3 | `GESTOR_NORMAS` | Gestiona normas |

---

## Validaciones del Backend

### Contraseña
- Mínimo **8 caracteres**
- Al menos una **mayúscula**, una **minúscula** y un **dígito**
- Regex: `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$`

### Nombre de usuario
- Máximo **10 caracteres**
- Obligatorio, no puede estar vacío

### Correo
- Formato email válido
- Máximo **100 caracteres**

### Nombre / Apellido
- Máximo **40 caracteres** cada uno

---

## Endpoints

### 1. Autenticación (`/api/auth`) — Públicos

#### POST `/api/auth/login`
Inicia sesión y retorna el JWT.

**Request:**
```json
{
  "nombreUsuario": "admin",
  "contrasenia": "admin123"
}
```

**Response (200):**
```json
{
  "status": 200,
  "mensaje": "Autenticación exitosa",
  "datos": {
    "token": "eyJhbGciOiJSUzI1NiJ9...",
    "tipo": "Bearer",
    "idUsuario": "ca91d474-5736-4c78-af5f-c08fa2f4a9ff",
    "nombreUsuario": "admin",
    "rol": "ADMIN"
  },
  "timestamp": "2026-06-03T23:32:35"
}
```

**Errores posibles:** `400` (campos vacíos), `401` (credenciales inválidas)

**Acción del frontend:**
1. Guardar `datos.token` en `localStorage` o `sessionStorage`
2. Guardar `datos.rol`, `datos.idUsuario`, `datos.nombreUsuario` para la UI
3. Redirigir al dashboard según el rol

---

#### POST `/api/auth/register`
Registra un nuevo usuario. **No pide contraseña** — el usuario la establece por correo.

**Request:**
```json
{
  "nombreUsuario": "juanp",
  "correo": "juan@email.com",
  "nombre": "Juan",
  "apellido": "Pasquel",
  "rolId": 2
}
```

> `rolId` es opcional. Si no se envía o es `null`, se asigna `CONSULTOR` (id=2) por defecto. **No se permite `rolId: 1` (ADMIN)** → retorna 403.

**Response (201):**
```json
{
  "status": 201,
  "mensaje": "Usuario registrado exitosamente",
  "datos": {
    "idUsuario": "26694fc5-287f-4e5d-97d6-52bf7d55a110",
    "nombreUsuario": "juanp",
    "correo": "juan@email.com",
    "nombreCompleto": "Juan Pasquel",
    "rol": "CONSULTOR",
    "mensaje": "Usuario registrado. Se ha enviado un correo a juan@email.com para establecer la contraseña",
    "fechaCreacion": "2026-06-03T23:35:35"
  },
  "timestamp": "2026-06-03T23:35:41"
}
```

**Errores posibles:** `400` (validación), `403` (rolId=1 prohibido), `409` (username o correo duplicado), `502` (fallo SMTP)

---

#### GET `/api/auth/validar-token?token={token}`
Valida un token de activación o recuperación **antes** de mostrar el formulario de contraseña.

**Response (200) — Token válido:**
```json
{
  "status": 200,
  "mensaje": "Token válido",
  "datos": {
    "valido": true,
    "tipoToken": "ACTIVACION",
    "nombreUsuario": "juanp",
    "correo": "juan@email.com",
    "fechaExpiracion": "2026-06-04T23:35:35",
    "mensaje": "Token válido"
  }
}
```

**Errores posibles:** `404` (token no existe), `409` (token expirado o ya usado)

**Acción del frontend:**
- Si `200` → mostrar formulario de establecer contraseña
- Si error → mostrar mensaje del campo `mensaje` y ofrecer "Reenviar correo"

---

#### POST `/api/auth/establecer-contrasenia`
Establece la contraseña usando el token recibido por correo. Activa la cuenta del usuario.

**Request:**
```json
{
  "token": "160a3760-9ce1-4297-a9f5-cb5fd6124b8c",
  "nuevaContrasenia": "Password123",
  "confirmarContrasenia": "Password123"
}
```

**Response (200):**
```json
{
  "status": 200,
  "mensaje": "Contraseña establecida exitosamente"
}
```

**Errores posibles:** `400` (validación de contraseña), `404` (token no existe), `409` (token expirado/usado, contraseñas no coinciden)

**Acción del frontend:** Redirigir a `/login` con mensaje de éxito.

---

#### POST `/api/auth/recuperar-contrasenia`
Solicita un correo para restablecer la contraseña. **Siempre retorna 200** por seguridad (no revela si el correo existe).

**Request:**
```json
{
  "correo": "juan@email.com"
}
```

**Response (200):**
```json
{
  "status": 200,
  "mensaje": "Si el correo está registrado, recibirás un enlace para restablecer tu contraseña"
}
```

**Errores posibles:** `400` (formato de correo inválido), `502` (fallo SMTP)

---

#### POST `/api/auth/reenviar-activacion`
Reenvía el correo de activación si el usuario no lo recibió o el token expiró.

**Request:**
```json
{
  "correo": "juan@email.com"
}
```

**Response (200):**
```json
{
  "status": 200,
  "mensaje": "Correo de activación reenviado"
}
```

---

#### POST `/api/auth/logout` 🔒
Invalida el JWT actual (lo agrega a una blacklist). **Requiere `Authorization: Bearer <token>`**.

**Response (200):**
```json
{
  "status": 200,
  "mensaje": "Sesión cerrada exitosamente"
}
```

**Acción del frontend:** Limpiar token del storage y redirigir a `/login`.

---

### 2. Usuarios CRUD (`/api/usuarios`) 🔒 — Requiere ROLE_ADMIN

#### GET `/api/usuarios`
Lista todos los usuarios activos.

**Response (200):**
```json
{
  "status": 200,
  "mensaje": "Usuarios obtenidos correctamente",
  "datos": [
    {
      "id": "ca91d474-5736-4c78-af5f-c08fa2f4a9ff",
      "nombreUsuario": "admin",
      "correo": "admin@sistemasgaia.com",
      "nombre": "Administrador",
      "apellido": "Sistema",
      "rolId": 1,
      "tipoRol": "ADMIN",
      "descripcionRol": "Administrador del sistema con acceso total",
      "ultimaEvaluacion": null,
      "ultimaModificacion": null,
      "fechaCreacion": "2026-06-03T23:31:59",
      "activo": true
    }
  ]
}
```

---

#### GET `/api/usuarios/{id}`
Busca un usuario por UUID.

---

#### POST `/api/usuarios`
Crea un usuario (con contraseña directa). **No se permite `rolId: 1` (ADMIN)** → retorna 403.

**Request:**
```json
{
  "nombreUsuario": "consultor1",
  "contrasenia": "Password123",
  "correo": "consultor@email.com",
  "nombre": "Carlos",
  "apellido": "López",
  "rolId": 2
}
```

> A diferencia de `/api/auth/register`, este endpoint **sí pide contraseña** y crea al usuario ya activo. Es para uso interno del ADMIN.

**Response (201):** Igual estructura que GET usuario individual.

**Errores posibles:** `400` (validación), `403` (rolId=1), `409` (username/correo duplicado)

---

#### PUT `/api/usuarios/{id}`
Actualiza un usuario. La contraseña es **opcional** — si no se envía, no se modifica.

**Request (sin cambiar contraseña):**
```json
{
  "nombreUsuario": "consultor1",
  "correo": "nuevo@email.com",
  "nombre": "Carlos Modificado",
  "apellido": "López",
  "rolId": 2
}
```

**Reglas de negocio:**
- No se puede cambiar a `rolId: 1` (ADMIN) → 403
- El ADMIN no puede editarse a sí mismo → 403

---

#### DELETE `/api/usuarios/{id}`
Soft delete (marca `activo=false`). El usuario no se elimina realmente de la BD.

**Regla de negocio:** El ADMIN no puede eliminarse a sí mismo → 403.

**Response (200):**
```json
{
  "status": 200,
  "mensaje": "Usuario eliminado correctamente"
}
```

---

### 3. Roles (`/api/roles`) 🔒 — Requiere ROLE_ADMIN

#### GET `/api/roles`
Lista los roles asignables. **Excluye ADMIN** del resultado.

**Response (200):**
```json
{
  "status": 200,
  "mensaje": "Roles obtenidos correctamente",
  "datos": [
    {
      "id": 2,
      "tipoRol": "CONSULTOR",
      "descripcionRol": "Consultor que realiza evaluaciones a normas",
      "activo": true
    },
    {
      "id": 3,
      "tipoRol": "GESTOR_NORMAS",
      "descripcionRol": "Gestor encargado de la gestión de normas",
      "activo": true
    }
  ]
}
```

---

### 4. Políticas (`/api/politicas`) 🔒 — Requiere ROLE_ADMIN

> **Las políticas NO tienen CRUD desde la API.** Solo se listan, asignan y desasignan por rol.

#### GET `/api/politicas`
Lista todas las políticas activas del sistema.

**Response (200):**
```json
{
  "status": 200,
  "mensaje": "Políticas obtenidas correctamente",
  "datos": [
    {
      "id": "70df9fcf-a57f-44ce-831e-2cdfd5d57aba",
      "nombrePolitica": "POLITICA_VER_REPORTES",
      "activo": true,
      "fechaCreacion": "2026-06-03T23:31:59",
      "fechaActualizacion": "2026-06-03T23:31:59"
    },
    {
      "id": "eb2d3535-9719-40a1-928f-630cb862f235",
      "nombrePolitica": "POLITICA_CARGAR_NORMAS",
      "activo": true,
      "fechaCreacion": "2026-06-03T23:31:59",
      "fechaActualizacion": "2026-06-03T23:31:59"
    },
    {
      "id": "01e7a4ac-3b8e-46bb-b54c-a4d303982483",
      "nombrePolitica": "POLITICA_CARGAR_ARCHIVO_A_NORMA",
      "activo": true,
      "fechaCreacion": "2026-06-03T23:31:59",
      "fechaActualizacion": "2026-06-03T23:31:59"
    }
  ]
}
```

---

#### GET `/api/politicas/rol/{rolId}`
Lista las políticas asignadas a un rol específico.

**Ejemplo:** `GET /api/politicas/rol/2` (políticas del rol CONSULTOR)

**Response (200):**
```json
{
  "status": 200,
  "mensaje": "Políticas del rol obtenidas correctamente",
  "datos": [
    {
      "id": "01e7a4ac-3b8e-46bb-b54c-a4d303982483",
      "nombrePolitica": "POLITICA_CARGAR_ARCHIVO_A_NORMA",
      "activo": true,
      "fechaCreacion": "2026-06-03T23:31:59",
      "fechaActualizacion": "2026-06-03T23:31:59"
    }
  ]
}
```

**Errores posibles:** `404` (rol no encontrado)

---

#### POST `/api/politicas/rol/{rolId}/asignar`
Asigna una política a un rol.

**Request:**
```json
{
  "politicaId": "70df9fcf-a57f-44ce-831e-2cdfd5d57aba"
}
```

**Response (201):**
```json
{
  "status": 201,
  "mensaje": "Política asignada correctamente al rol"
}
```

**Errores posibles:** `404` (rol o política no encontrada), `409` (política ya asignada a ese rol)

---

#### DELETE `/api/politicas/rol/{rolId}/desasignar/{politicaId}`
Desasigna una política de un rol.

**Response (200):**
```json
{
  "status": 200,
  "mensaje": "Política desasignada correctamente del rol"
}
```

**Errores posibles:** `404` (asignación no encontrada)

---

## Flujos del Frontend

### Flujo 1: Login

```
1. Usuario llena formulario (nombreUsuario, contrasenia)
2. POST /api/auth/login
3. Si 200 → guardar token, redirigir a dashboard
4. Si 401 → mostrar "Credenciales inválidas"
5. Si 400 → mostrar errores de validación
```

### Flujo 2: Registro y Activación de Cuenta

```
1. Admin o formulario público → POST /api/auth/register
2. Backend envía correo con enlace: {FRONTEND_URL}/activar-cuenta?token={token}
3. Frontend recibe el query param "token" en la ruta /activar-cuenta
4. GET /api/auth/validar-token?token={token}
   - Si 200 → mostrar formulario de contraseña
   - Si error → mostrar mensaje y botón "Reenviar correo"
5. Usuario llena contraseña → POST /api/auth/establecer-contrasenia
6. Si 200 → redirigir a /login con mensaje "Cuenta activada"
```

### Flujo 3: Recuperación de Contraseña

```
1. Usuario en /login → click "Olvidé mi contraseña"
2. Formulario pide correo → POST /api/auth/recuperar-contrasenia
3. Siempre mostrar: "Si el correo está registrado, recibirás un enlace"
4. Backend envía correo con enlace: {FRONTEND_URL}/restablecer-contrasenia?token={token}
5. Frontend en /restablecer-contrasenia → GET /api/auth/validar-token?token={token}
6. Si válido → mostrar formulario de nueva contraseña
7. POST /api/auth/establecer-contrasenia
8. Redirigir a /login
```

### Flujo 4: Gestión de Políticas por Rol (ADMIN)

```
1. Cargar roles disponibles:     GET /api/roles
2. Cargar todas las políticas:    GET /api/politicas
3. Al seleccionar un rol:         GET /api/politicas/rol/{rolId}
4. Calcular "disponibles" = todas - asignadas (diferencia de listas por id)
5. Botón "Asignar":              POST /api/politicas/rol/{rolId}/asignar
6. Botón "Desasignar":           DELETE /api/politicas/rol/{rolId}/desasignar/{politicaId}
7. Recargar políticas del rol después de cada operación
```

### Flujo 5: Logout

```
1. POST /api/auth/logout (con header Authorization: Bearer <token>)
2. Limpiar token del storage
3. Redirigir a /login
```

---

## Rutas Sugeridas para el Frontend

| Ruta | Acceso | Descripción |
|------|--------|-------------|
| `/login` | Público | Formulario de inicio de sesión |
| `/activar-cuenta?token=` | Público | Formulario para establecer contraseña (activación) |
| `/restablecer-contrasenia?token=` | Público | Formulario para restablecer contraseña (recuperación) |
| `/dashboard` | Autenticado | Panel principal según rol |
| `/usuarios` | ADMIN | Listado y CRUD de usuarios |
| `/politicas` | ADMIN | Gestión de asignación de políticas por rol |

---

## Interfaces TypeScript (modelos)

```typescript
// === Wrapper de respuesta ===
export interface ApiResponse<T> {
  status: number;
  mensaje: string;
  datos?: T;
  errores?: string[];
  timestamp: string;
}

// === Auth ===
export interface LoginRequest {
  nombreUsuario: string;
  contrasenia: string;
}

export interface LoginResponse {
  token: string;
  tipo: string;          // siempre "Bearer"
  idUsuario: string;     // UUID
  nombreUsuario: string;
  rol: string;           // "ADMIN" | "CONSULTOR" | "GESTOR_NORMAS"
}

export interface RegistroRequest {
  nombreUsuario: string;
  correo: string;
  nombre: string;
  apellido: string;
  rolId?: number;        // Opcional, default CONSULTOR. No se puede enviar 1 (ADMIN)
}

export interface RegistroResponse {
  idUsuario: string;
  nombreUsuario: string;
  correo: string;
  nombreCompleto: string;
  rol: string;
  mensaje: string;
  fechaCreacion: string;
}

export interface EstablecerContraseniaRequest {
  token: string;
  nuevaContrasenia: string;
  confirmarContrasenia: string;
}

export interface RecuperarContraseniaRequest {
  correo: string;
}

export interface ValidarTokenResponse {
  valido: boolean;
  tipoToken: string;      // "ACTIVACION" | "RECUPERACION"
  nombreUsuario: string;
  correo: string;
  fechaExpiracion: string;
  mensaje: string;
}

// === Usuarios ===
export interface UsuarioRequest {
  nombreUsuario: string;
  contrasenia?: string;   // Obligatorio en crear, opcional en actualizar
  correo: string;
  nombre: string;
  apellido: string;
  rolId: number;          // No se puede enviar 1 (ADMIN)
}

export interface UsuarioResponse {
  id: string;             // UUID
  nombreUsuario: string;
  correo: string;
  nombre: string;
  apellido: string;
  rolId: number;
  tipoRol: string;
  descripcionRol: string;
  ultimaEvaluacion?: string;
  ultimaModificacion?: string;
  fechaCreacion: string;
  activo: boolean;
}

// === Roles ===
export interface RolResponse {
  id: number;
  tipoRol: string;
  descripcionRol: string;
  activo: boolean;
}

// === Políticas ===
export interface PoliticaResponse {
  id: string;             // UUID
  nombrePolitica: string;
  activo: boolean;
  fechaCreacion: string;
  fechaActualizacion?: string;
}

export interface AsignarPoliticaRequest {
  politicaId: string;     // UUID
}
```

---

## Interceptor HTTP Recomendado

```typescript
// Adjuntar token JWT a todas las requests (excepto las públicas)
intercept(req, next) {
  const token = localStorage.getItem('atlas_token');
  if (token) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }
  return next(req).pipe(
    catchError(error => {
      if (error.status === 401) {
        // Token expirado o inválido
        localStorage.clear();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
}
```

---

## Resumen Rápido de Endpoints

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| `POST` | `/api/auth/login` | ❌ | Iniciar sesión |
| `POST` | `/api/auth/logout` | ✅ Bearer | Cerrar sesión |
| `POST` | `/api/auth/register` | ❌ | Registrar usuario (sin contraseña) |
| `GET` | `/api/auth/validar-token?token=` | ❌ | Validar token de activación/recuperación |
| `POST` | `/api/auth/establecer-contrasenia` | ❌ | Establecer contraseña con token |
| `POST` | `/api/auth/recuperar-contrasenia` | ❌ | Solicitar recuperación por correo |
| `POST` | `/api/auth/reenviar-activacion` | ❌ | Reenviar correo de activación |
| `GET` | `/api/usuarios` | ✅ ADMIN | Listar usuarios activos |
| `GET` | `/api/usuarios/{id}` | ✅ ADMIN | Buscar usuario por UUID |
| `POST` | `/api/usuarios` | ✅ ADMIN | Crear usuario (con contraseña) |
| `PUT` | `/api/usuarios/{id}` | ✅ ADMIN | Actualizar usuario |
| `DELETE` | `/api/usuarios/{id}` | ✅ ADMIN | Soft delete usuario |
| `GET` | `/api/roles` | ✅ ADMIN | Listar roles (sin ADMIN) |
| `GET` | `/api/politicas` | ✅ ADMIN | Listar todas las políticas |
| `GET` | `/api/politicas/rol/{rolId}` | ✅ ADMIN | Listar políticas de un rol |
| `POST` | `/api/politicas/rol/{rolId}/asignar` | ✅ ADMIN | Asignar política a rol |
| `DELETE` | `/api/politicas/rol/{rolId}/desasignar/{politicaId}` | ✅ ADMIN | Desasignar política de rol |

---

## Credenciales por Defecto

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| `admin` | `admin123` | ADMIN |

---

## Políticas Semilla

| Política | Asignada a |
|----------|------------|
| `POLITICA_VER_REPORTES` | ADMIN |
| `POLITICA_CARGAR_NORMAS` | ADMIN, GESTOR_NORMAS |
| `POLITICA_CARGAR_ARCHIVO_A_NORMA` | ADMIN, CONSULTOR |
