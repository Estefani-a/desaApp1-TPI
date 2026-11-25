# UniRide · Backend

API REST de UniRide: Spring Boot 3.5, Java 21, PostgreSQL 16. El contrato está en [`docs/API.md`](../docs/API.md) y las reglas en [`docs/CONVENCIONES.md`](../docs/CONVENCIONES.md) §10.

## Levantar todo con Docker

Solo hace falta Docker. Desde la raíz del repo:

```bash
cp .env.example .env      # completar DB_PASSWORD y JWT_SECRET
docker compose up --build
```

| Qué | Dónde |
| --- | --- |
| API | `http://localhost:8080/api/v1` (desde el emulador: `http://10.0.2.2:8080/api/v1`) |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Estado | `http://localhost:8080/actuator/health` |
| Correos (Mailpit) | `http://localhost:8025` |

`docker compose down` apaga todo; `docker compose down -v` además borra la base (hace falta si se renombra una columna de una entidad, CONVENCIONES §10.5).

## Programar desde IntelliJ

1. Abrir la carpeta `backend/`.
2. `docker compose up db mailpit` desde la raíz.
3. Correr `UniRideBackendApplication` con el perfil `local` (variable `SPRING_PROFILES_ACTIVE=local`). El perfil lee el `.env` de la raíz.

Tests y build: `./mvnw verify` (en Windows, `mvnw.cmd verify`).

## Usuarios de prueba

`data.sql` carga las sedes con sus puntos de encuentro. Con el perfil `demo` (lo activan `docker compose` y el perfil `local`), `data-demo.sql` agrega tres usuarios ya activados con la contraseña `UniRide2026`. Como la contraseña es pública, un despliegue real no tiene que activar `demo`:

| Correo | Modo | Vehículo |
| --- | --- | --- |
| `conductora.demo@uade.edu.ar` | `AMBOS` | Gol Trend gris, `AB123CD` |
| `pasajero.demo@uade.edu.ar` | `PASAJERO` | — |
| `conductor2.demo@uade.edu.ar` | `CONDUCTOR` | Cronos blanco, `AE456FG` |

## Estructura

Paquetes por capa (CONVENCIONES §10.1): `controller/` → `service/` → `repository/` → `model/`, con los DTOs en `dto/`.

- `exception/`: `ErrorCode` (los códigos de docs/API.md §2), `ApiException` para lanzar errores de negocio desde un Service y `GlobalExceptionHandler`, que arma siempre `ApiErrorResponseDTO` (`{ code, message, fields }`).
- `security/`: `JwtService` (HS256, id del usuario en `sub`, 30 días), `JwtAuthenticationFilter` y `CurrentUser`, que el Controller usa para obtener el id del usuario y pasárselo al Service.
- `config/`: rutas públicas (`SecurityConfig`), Swagger con el botón "Authorize" para el JWT (`OpenApiConfig`) y formato de fechas (`TimeConfig`: horas `"22:15"`, instantes con `-03:00`, y un `Clock` inyectable para los tests).
- `model/`: las entidades no tienen setters de estado. Las vacantes y las transiciones cambian con métodos que validan (`Trip.takeSeat()`, `Reservation.accept()`…).
