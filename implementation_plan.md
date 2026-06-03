# Reglas de Negocio para el Sistema de Autenticación ATLAS

Plan para implementar 4 reglas de negocio que restringen el comportamiento del ADMIN y protegen la integridad del sistema ABAC.

## Resumen de Cambios

| Regla | Descripción |
|-------|-------------|
| **1** | Endpoint de obtener roles excluye el rol ADMIN |
| **2** | Crear usuario no permite asignar rol ADMIN |
| **3** | Políticas: solo listar y asignar/desasignar (sin CRUD de políticas) |
| **4** | Un ADMIN no puede editarse ni eliminarse a sí mismo |

---

## User Review Required

> [!IMPORTANT]
> **Regla 1 — Exclusión de ADMIN en listar roles**: El endpoint `GET /api/roles` retornará todos los roles excepto ADMIN. Esto significa que desde la UI solo se podrán asignar roles no-ADMIN a usuarios.

> [!IMPORTANT]
> **Regla 3 — Políticas de solo lectura**: Se crearán endpoints para:
> - `GET /api/politicas` → Listar todas las políticas (solo lectura)
> - `GET /api/politicas/rol/{rolId}` → Ver políticas asignadas a un rol
> - `POST /api/politicas/rol/{rolId}/asignar` → Asignar política a un rol
> - `DELETE /api/politicas/rol/{rolId}/desasignar/{politicaId}` → Desasignar política de un rol
>
> **No** se crearán endpoints de crear, actualizar o eliminar políticas (eso lo hace un desarrollador directamente en BD).

> [!IMPORTANT]
> **Regla 4 — Auto-protección del ADMIN**: Para determinar "sí mismo" se usará el `idUsuario` extraído del JWT del request. Si el UUID del token coincide con el UUID del usuario objetivo en `PUT` o `DELETE`, se lanzará `ForbiddenOperationException` (403).

## Open Questions

> [!NOTE]
> **Regla 2 — Creación con rol ADMIN**: La validación se hará tanto en `crear` como en `actualizar` del `UsuarioService`. Si alguien intenta crear o actualizar un usuario asignándole el rol ADMIN, se rechazará con 403. ¿Esto es correcto? ¿O solo debería aplicar al crear?

> [!NOTE]
> **Asignar/desasignar políticas**: ¿Estas operaciones deben restringirse solo al rol ADMIN vía `@PreAuthorize("hasRole('ADMIN')")`? Actualmente todos los endpoints protegidos solo requieren autenticación genérica.

---

## Proposed Changes

### Excepción nueva: ForbiddenOperationException

#### [NEW] [ForbiddenOperationException.java](file:///home/rubenbenavides/MS_AUTH_ATLAS/src/main/java/com/sistemasgaia/atlas/msautenticacion/exceptions/ForbiddenOperationException.java)

Nueva excepción `@ResponseStatus(HttpStatus.FORBIDDEN)` para manejar operaciones prohibidas (auto-edición de ADMIN, creación de ADMIN, etc.).

---

#### [MODIFY] [GlobalExceptionHandler.java](file:///home/rubenbenavides/MS_AUTH_ATLAS/src/main/java/com/sistemasgaia/atlas/msautenticacion/exceptions/GlobalExceptionHandler.java)

Agregar handler para `ForbiddenOperationException` que retorne HTTP 403 con `ApiResponseDto.error(403, mensaje, null)`.

---

### Componente de Roles (Regla 1)

#### [NEW] [RolResponseDto.java](file:///home/rubenbenavides/MS_AUTH_ATLAS/src/main/java/com/sistemasgaia/atlas/msautenticacion/dto/rol/RolResponseDto.java)

DTO de respuesta con campos: `id`, `tipoRol`, `descripcionRol`, `activo`.

---

#### [MODIFY] [RolRepository.java](file:///home/rubenbenavides/MS_AUTH_ATLAS/src/main/java/com/sistemasgaia/atlas/msautenticacion/repositories/RolRepository.java)

Agregar método:
```java
List<Rol> findByTipoRolNot(TipoRol tipoRol);
```
Esto retorna todos los roles excepto el tipo especificado (ADMIN).

---

#### [NEW] [RolService.java](file:///home/rubenbenavides/MS_AUTH_ATLAS/src/main/java/com/sistemasgaia/atlas/msautenticacion/services/RolService.java)

Servicio con método `listarRolesAsignables()` que usa `rolRepository.findByTipoRolNot(TipoRol.ADMIN)` y mapea a `RolResponseDto`.

---

#### [NEW] [RolController.java](file:///home/rubenbenavides/MS_AUTH_ATLAS/src/main/java/com/sistemasgaia/atlas/msautenticacion/controllers/RolController.java)

Controller con endpoint:
- `GET /api/roles` → retorna roles asignables (sin ADMIN)

---

### Componente de Políticas (Regla 3)

#### [NEW] [PoliticaResponseDto.java](file:///home/rubenbenavides/MS_AUTH_ATLAS/src/main/java/com/sistemasgaia/atlas/msautenticacion/dto/politica/PoliticaResponseDto.java)

DTO de respuesta con campos: `id`, `nombrePolitica`, `activo`.

---

#### [NEW] [AsignarPoliticaRequestDto.java](file:///home/rubenbenavides/MS_AUTH_ATLAS/src/main/java/com/sistemasgaia/atlas/msautenticacion/dto/politica/AsignarPoliticaRequestDto.java)

DTO de request con campo: `politicaId` (UUID).

---

#### [MODIFY] [DetallePoliticaRepository.java](file:///home/rubenbenavides/MS_AUTH_ATLAS/src/main/java/com/sistemasgaia/atlas/msautenticacion/repositories/DetallePoliticaRepository.java)

Agregar métodos:
```java
boolean existsByPoliticaIdAndRolId(UUID politicaId, Integer rolId);
void deleteByPoliticaIdAndRolId(UUID politicaId, Integer rolId);
```

---

#### [NEW] [PoliticaService.java](file:///home/rubenbenavides/MS_AUTH_ATLAS/src/main/java/com/sistemasgaia/atlas/msautenticacion/services/PoliticaService.java)

Servicio con métodos:
- `listarTodas()` → lista todas las políticas
- `listarPorRol(Integer rolId)` → lista políticas asignadas a un rol
- `asignarPoliticaARol(Integer rolId, UUID politicaId)` → crea un `DetallePolitica`
- `desasignarPoliticaDeRol(Integer rolId, UUID politicaId)` → elimina el `DetallePolitica`

---

#### [NEW] [PoliticaController.java](file:///home/rubenbenavides/MS_AUTH_ATLAS/src/main/java/com/sistemasgaia/atlas/msautenticacion/controllers/PoliticaController.java)

Controller con endpoints:
- `GET /api/politicas` → listar todas
- `GET /api/politicas/rol/{rolId}` → listar por rol
- `POST /api/politicas/rol/{rolId}/asignar` → asignar política a rol
- `DELETE /api/politicas/rol/{rolId}/desasignar/{politicaId}` → desasignar

---

### Componente de Usuarios (Reglas 2 y 4)

#### [MODIFY] [UsuarioService.java](file:///home/rubenbenavides/MS_AUTH_ATLAS/src/main/java/com/sistemasgaia/atlas/msautenticacion/services/UsuarioService.java)

Cambios en `crear()`:
- Después de buscar el `Rol`, verificar si `rol.getTipoRol() == TipoRol.ADMIN`. Si es así, lanzar `ForbiddenOperationException("No se permite crear usuarios con rol ADMIN")`.

Cambios en `actualizar(UUID id, UsuarioRequestDto request)`:
- Añadir parámetro `UUID idUsuarioAutenticado` (extraído del JWT).
- Si `id.equals(idUsuarioAutenticado)` y el usuario tiene rol ADMIN, lanzar `ForbiddenOperationException("Un administrador no puede editarse a sí mismo")`.
- Verificar si el nuevo rol es ADMIN: lanzar `ForbiddenOperationException("No se permite asignar el rol ADMIN")`.

Cambios en `eliminar(UUID id)`:
- Añadir parámetro `UUID idUsuarioAutenticado`.
- Si `id.equals(idUsuarioAutenticado)`, lanzar `ForbiddenOperationException("Un administrador no puede eliminarse a sí mismo")`.

---

#### [MODIFY] [UsuarioController.java](file:///home/rubenbenavides/MS_AUTH_ATLAS/src/main/java/com/sistemasgaia/atlas/msautenticacion/controllers/UsuarioController.java)

Cambios en `actualizar()` y `eliminar()`:
- Extraer el `idUsuario` del JWT usando `JwtService` desde el header `Authorization`.
- Pasar el UUID como parámetro adicional al service.

Se inyectará `JwtService` y se extraerá el token del `HttpServletRequest`.

---

## Verification Plan

### Automated Tests
```bash
./mvnw compile
```
Verificar que el proyecto compila sin errores.

### Manual Verification
- Probar en Swagger UI o Postman:
  1. `GET /api/roles` → No debe incluir ADMIN
  2. `POST /api/usuarios` con `rolId` de ADMIN → debe retornar 403
  3. `PUT /api/usuarios/{adminId}` donde el token pertenece al mismo admin → debe retornar 403
  4. `DELETE /api/usuarios/{adminId}` donde el token pertenece al mismo admin → debe retornar 403
  5. `GET /api/politicas` → debe listar todas las políticas
  6. `POST /api/politicas/rol/{rolId}/asignar` → debe asignar política
  7. `DELETE /api/politicas/rol/{rolId}/desasignar/{politicaId}` → debe desasignar política
