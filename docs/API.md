# UniRide · Contrato de la API REST

> **Contrato aprobado (A0).** Este archivo es la fuente de verdad de la API REST de UniRide: si el código del backend o de la app no coincide con él, el error está en el código. Cualquier cambio posterior requiere aprobación del responsable de red (Alvaro) y debe actualizar este archivo en el mismo PR.

---

## 1. Generalidades

| Tema | Regla |
| --- | --- |
| URL base | `{BASE_URL}/api/v1`. Desarrollo con el emulador: `http://10.0.2.2:8080/api/v1` |
| Formato | JSON en UTF-8, campos en camelCase |
| Autenticación | `Authorization: Bearer <jwt>` en todo salvo `/auth/**`, `/campuses` y `/links/**` |
| Fechas | `departureDate` como `"2026-11-20"`, horas como `"22:15"`, instantes en ISO-8601 con zona: `"2026-11-20T21:40:12-03:00"`. Zona de negocio: `America/Argentina/Buenos_Aires` |
| Dinero | Enteros en pesos, sin decimales (`contributionPerSeat: 3500`). No se usan `float`/`double` para montos |
| Ids | UUID en texto. Los de viajes, reservas y calificaciones los **genera la app** (Offline First); los de usuarios y notificaciones, el servidor |
| Enums | En mayúsculas y en español, iguales que en Room y en la base: ver §3 |
| Campos opcionales | Si no hay valor, el campo viene en `null` (no se omite) |

### Idempotencia

Crear un viaje, una reserva o una calificación es un `PUT` sobre el id que generó la app. Si `SyncWorker` reintenta el mismo envío:

- y el recurso ya existe con el mismo autor, el servidor responde **200** con el recurso tal como está (sin duplicarlo ni modificarlo);
- si el id ya existe pero es de otro usuario, responde **409** `ID_CONFLICT`.

Las transiciones de estado (`accept`, `reject`, `cancel`, `finish`) también son idempotentes: repetir la misma transición sobre un recurso que ya está en ese estado responde **200** sin cambios.

---

## 2. Errores

Toda respuesta de error tiene este cuerpo:

```json
{
  "code": "NO_SEATS_AVAILABLE",
  "message": "El viaje ya no tiene plazas disponibles.",
  "fields": null
}
```

`fields` solo viene en los errores de validación:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Hay campos con errores.",
  "fields": { "totalSeats": "Debe estar entre 1 y 4.", "departureDate": "Debe ser una fecha futura." }
}
```

| HTTP | `code` | Cuándo |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Algún campo no cumple las reglas |
| 400 | `INVALID_EMAIL_DOMAIN` | El correo no es `@uade.edu.ar` |
| 400 | `WEAK_PASSWORD` | La contraseña no cumple la política (§4.1) |
| 400 | `INVALID_OR_EXPIRED_TOKEN` | Enlace de activación o de recuperación inválido o vencido |
| 401 | `UNAUTHENTICATED` | Falta el token, es inválido o venció |
| 401 | `INVALID_CREDENTIALS` | Correo o contraseña incorrectos |
| 401 | `ACCOUNT_NOT_ACTIVATED` | La cuenta sigue `PENDIENTE` |
| 403 | `FORBIDDEN` | El recurso no es del usuario (por ejemplo, aceptar una reserva de un viaje ajeno) |
| 403 | `CANNOT_RESERVE_OWN_TRIP` | El conductor intenta reservar en su propio viaje |
| 404 | `NOT_FOUND` | El recurso no existe |
| 405 | `METHOD_NOT_ALLOWED` | La ruta existe pero no admite ese método HTTP |
| 409 | `EMAIL_ALREADY_REGISTERED` | Registro con un correo que ya existe |
| 409 | `ID_CONFLICT` | El UUID ya existe y pertenece a otro usuario |
| 409 | `NO_SEATS_AVAILABLE` | No quedan plazas al reservar o al aceptar |
| 409 | `TRIP_NOT_OPEN` | El viaje está `FINALIZADO` o `CANCELADO`, o ya salió |
| 409 | `ALREADY_RESERVED` | El pasajero ya tiene una reserva `PENDIENTE` o `ACEPTADA` en ese viaje |
| 409 | `TRIP_ALREADY_DEPARTED` | El viaje ya empezó (fecha y hora de salida en el pasado): sus reservas no se pueden aceptar, rechazar ni cancelar |
| 409 | `INVALID_STATUS_TRANSITION` | Por ejemplo, aceptar una reserva `CANCELADA` |
| 409 | `ALREADY_RATED` | Ya calificó a esa persona en ese viaje |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | El cuerpo no es `application/json` |
| 500 | `INTERNAL_ERROR` | Error inesperado (nunca se devuelve el stack trace) |

En la app, `safeApiCall` traduce el HTTP a `DataError` (CONVENCIONES §6.3) y, cuando la pantalla necesita distinguir el motivo, lee el `code`.

---

## 3. Modelos

### 3.1 Enums

| Enum | Valores |
| --- | --- |
| `PreferredMode` | `PASAJERO`, `CONDUCTOR`, `AMBOS` |
| `Direction` | `IDA` (hacia la sede), `VUELTA` (desde la sede) |
| `Shift` | `MANANA` (`[06:00, 13:00)`), `TARDE` (`[13:00, 18:00)`), `NOCHE` (`[18:00, 06:00)`) — no viaja en `TripDto`, se calcula a partir de `departureTime` |
| `TripStatus` | `ABIERTO`, `COMPLETO`, `FINALIZADO`, `CANCELADO` |
| `ReservationStatus` | `PENDIENTE`, `ACEPTADA`, `RECHAZADA`, `CANCELADA` |
| `NotificationType` | `NUEVA_SOLICITUD`, `SOLICITUD_ACEPTADA`, `SOLICITUD_RECHAZADA`, `CALIFICAR_VIAJE` |

### 3.2 `UserDto` (perfil propio)

```json
{
  "id": "6f1c2a4e-1b0e-4a8e-9a55-2c1f3e7b9d10",
  "fullName": "Lucía Fernández",
  "email": "lfernandez@uade.edu.ar",
  "career": "Ingeniería en Informática",
  "phone": "+5491155550000",
  "preferredMode": "AMBOS",
  "ratingAvg": 4.8,
  "ratingCount": 12,
  "vehicle": {
    "brand": "Volkswagen",
    "model": "Gol Trend",
    "color": "Gris",
    "plate": "AB123CD"
  }
}
```

`vehicle` es `null` si el usuario no cargó un vehículo. Solo `GET/PUT /users/me` devuelven este modelo completo; en el resto de las respuestas los otros usuarios aparecen con nombre e inicial del apellido (RNF07).

### 3.3 `TripDto`

Los campos son los de `TripEntity` (preentrega 11.1), así el mapper Dto → Entity es directo.

```json
{
  "tripId": "0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31",
  "driverId": "6f1c2a4e-1b0e-4a8e-9a55-2c1f3e7b9d10",
  "driverName": "Lucía F.",
  "driverRating": 4.8,
  "direction": "VUELTA",
  "campus": "MONSERRAT",
  "meetingPoint": "Salida Lima 717",
  "destinationNeighborhood": "Ramos Mejía",
  "destinationReference": "Estación de tren",
  "departureDate": "2026-11-20",
  "departureTime": "22:15",
  "estimatedArrivalTime": "23:00",
  "totalSeats": 3,
  "availableSeats": 2,
  "contributionPerSeat": 3500,
  "vehicleModel": "Gol Trend",
  "vehicleColor": "Gris",
  "vehiclePlate": null,
  "driverPhone": null,
  "status": "ABIERTO",
  "myReservation": null,
  "updatedAt": "2026-11-20T21:40:12-03:00"
}
```

- `vehiclePlate` y `driverPhone` solo vienen con valor si quien consulta es el conductor o tiene una reserva `ACEPTADA` en el viaje (RNF07). En cualquier otro caso vienen en `null`.
- `myReservation` es la reserva del usuario que consulta en ese viaje (`ReservationDto`, §3.4) o `null`. Le sirve a la app para la regla de retención de 11.2 y para la tarjeta del inicio.
- `campus` es el código de una sede de `GET /campuses` (§5.2).

### 3.4 `ReservationDto`

```json
{
  "reservationId": "c3d9a8e2-7f41-4e0b-b1a6-5f2e9c4d8a17",
  "tripId": "0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31",
  "passengerId": "9a2e5c7b-3d8f-4b1a-a6e0-1c4f7d9b2e58",
  "passengerName": "Martín G.",
  "passengerRating": 4.5,
  "passengerPhone": null,
  "status": "PENDIENTE",
  "reservedAt": "2026-11-20T21:41:03-03:00",
  "updatedAt": "2026-11-20T21:41:03-03:00"
}
```

`passengerPhone` solo viene con valor para el conductor, y solo cuando la reserva está `ACEPTADA`.

### 3.5 `NotificationDto`

```json
{
  "notificationId": "e1f4b7a0-2c5d-4e8f-9b3a-6d0c1e4f7a29",
  "type": "SOLICITUD_ACEPTADA",
  "title": "¡Tenés lugar!",
  "body": "Lucía aceptó tu solicitud para el viaje de las 22:15.",
  "tripId": "0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31",
  "createdAt": "2026-11-20T21:45:30-03:00",
  "isRead": false
}
```

---

## 4. Autenticación y perfil (RF01)

### 4.1 Registro

`POST /auth/register` · público

```json
{
  "email": "lfernandez@uade.edu.ar",
  "password": "Secreta123",
  "fullName": "Lucía Fernández",
  "career": "Ingeniería en Informática",
  "phone": "+5491155550000",
  "preferredMode": "AMBOS",
  "vehicle": null
}
```

- **201** `{ "message": "Te enviamos un correo para activar la cuenta." }`. La cuenta queda `PENDIENTE` y se envía el correo con el enlace de activación (§8).
- **400** `INVALID_EMAIL_DOMAIN`, `WEAK_PASSWORD`, `VALIDATION_ERROR` · **409** `EMAIL_ALREADY_REGISTERED`.

Política de contraseña: mínimo 8 caracteres, con al menos una letra y un número. La app valida lo mismo antes de enviar (RF01), pero el servidor no confía en eso.

### 4.2 Activación

`POST /auth/activate` · público

```json
{ "token": "<token del enlace>" }
```

- **200** `AuthResponse` (§4.4): activar deja la sesión iniciada automáticamente, para no pedir la contraseña otra vez.
- **400** `INVALID_OR_EXPIRED_TOKEN`. El token vence a las 24 h y es de un solo uso.

`POST /auth/resend-activation` · público · `{ "email": "..." }` → **202** siempre, exista o no la cuenta.

### 4.3 Inicio de sesión

`POST /auth/login` · público

```json
{ "email": "lfernandez@uade.edu.ar", "password": "Secreta123" }
```

- **200** `AuthResponse`.
- **401** `INVALID_CREDENTIALS`, `ACCOUNT_NOT_ACTIVATED`.

### 4.4 `AuthResponse`

```json
{
  "accessToken": "<jwt>",
  "expiresAt": "2026-12-20T21:40:12-03:00",
  "user": { "...": "UserDto" }
}
```

El JWT se firma con HS256, lleva el id del usuario en `sub` y dura 30 días sin refresh token: cuando vence (o ante cualquier 401), la app borra el token y vuelve al login (preentrega 11.3).

> **Simplificación de alcance del TP:** se utiliza un único JWT de 30 días para acotar la implementación y evitar renovaciones durante la demo. En un entorno productivo, lo óptimo es emitir un **access token JWT de corta duración (15–30 minutos)** combinado con un **refresh token de mayor duración (7–30 días)** almacenado y revocable en base de datos con rotación en cada uso, lo que permite revocar sesiones comprometidas o cerradas sin esperar al vencimiento del JWT.

### 4.5 Recuperación de contraseña

`POST /auth/password-reset` · público · `{ "email": "..." }`

- **202** `{ "message": "Si el correo está registrado, te enviamos un enlace para restablecer la contraseña." }` siempre, para no revelar si el correo existe (RF01).

`POST /auth/password-reset/confirm` · público

```json
{ "token": "<token del enlace>", "newPassword": "OtraSecreta456" }
```

- **204**. El token vence a la hora y es de un solo uso.
- **400** `INVALID_OR_EXPIRED_TOKEN`, `WEAK_PASSWORD`.

### 4.6 Perfil

| Método y ruta | Cuerpo | Respuesta |
| --- | --- | --- |
| `GET /users/me` | — | **200** `UserDto` |
| `PUT /users/me` | `fullName`, `career`, `phone`, `preferredMode`, `vehicle` (el correo no se cambia) | **200** `UserDto` · **400** `VALIDATION_ERROR` |
| `PUT /users/me/device-token` | `{ "fcmToken": "..." }` | **204**. Se llama al iniciar sesión y cuando FCM renueva el token |
| `DELETE /users/me/device-token` | — | **204**. Se llama al cerrar sesión |

---

## 5. Viajes (RF02 y RF03)

### 5.1 Listado con filtros (RF03)

`GET /trips?campus=MONSERRAT&direction=VUELTA&date=2026-11-20&shift=NOCHE&updatedSince=2026-11-20T21:00:00-03:00`

Todos los parámetros son opcionales y se combinan con Y.

- **200** `TripDto[]` ordenado por `departureDate` y `departureTime`.
- Devuelve los viajes `ABIERTO` y `COMPLETO` que todavía no salieron. Con `updatedSince`, solo los que cambiaron desde ese momento, **incluidos** los que pasaron a `FINALIZADO` o `CANCELADO`, para que la app los saque de Room.
- El parámetro `shift` se evalúa sobre `departureTime` con los intervalos semiabiertos de §3.1 (`shift` no forma parte de `TripDto`; la app aplica la misma regla al filtrar en Room sin conexión).
- Sin paginación: se devuelven como máximo 100 viajes.

### 5.2 Sedes y puntos de encuentro

`GET /campuses` · público

```json
[
  {
    "code": "MONSERRAT",
    "name": "Sede Monserrat",
    "meetingPoints": [
      "Salida Lima 717",
      "Salida Lima 757",
      "Salida Chile 1142",
      "Salida Independencia 1072"
    ]
  },
  {
    "code": "BELGRANO",
    "name": "Sede Belgrano",
    "meetingPoints": [
      "Salida 11 de Septiembre 1990",
      "Salida Echeverría"
    ]
  },
  {
    "code": "RECOLETA",
    "name": "Sede Recoleta",
    "meetingPoints": [
      "Salida Libertad 1340"
    ]
  }
]
```

Son las "salidas predefinidas de cada sede como puntos de encuentro" de la preentrega (2.3). Se cargan desde `backend/src/main/resources/data.sql` y la app las guarda en caché.

### 5.3 Detalle

`GET /trips/{tripId}` → **200** `TripDto` · **404** `NOT_FOUND`.

### 5.4 Publicar (RF02)

`PUT /trips/{tripId}` · lo envía `SyncWorker`

```json
{
  "direction": "VUELTA",
  "campus": "MONSERRAT",
  "meetingPoint": "Salida Lima 717",
  "destinationNeighborhood": "Ramos Mejía",
  "destinationReference": "Estación de tren",
  "departureDate": "2026-11-20",
  "departureTime": "22:15",
  "estimatedArrivalTime": "23:00",
  "totalSeats": 3,
  "contributionPerSeat": 3500
}
```

- **201** `TripDto` (creado) · **200** `TripDto` (reintento del mismo envío, §1).
- **400** `VALIDATION_ERROR`: `totalSeats` entre 1 y 4, salida en el futuro, `contributionPerSeat` ≥ 0, `meetingPoint` de la sede elegida.
- **400** `VALIDATION_ERROR` si el usuario no tiene vehículo cargado en su perfil.
- **409** `ID_CONFLICT`.

El servidor completa `driverName`, `driverRating` y los datos del vehículo desde el perfil, y `availableSeats = totalSeats`.

### 5.5 Mis viajes

`GET /users/me/trips` → **200** `TripDto[]` con los viajes que publiqué y aquellos en los que tengo una reserva, incluidos los de los últimos 7 días ya finalizados (para calificar). La app lo llama después del login y al refrescar el inicio, para reconstruir Room si se reinstaló.

### 5.6 Finalizar y cancelar

| Método y ruta | Quién | Efecto |
| --- | --- | --- |
| `POST /trips/{tripId}/finish` | Conductor | `FINALIZADO` |
| `POST /trips/{tripId}/cancel` | Conductor | `CANCELADO`. Las reservas `PENDIENTE` y `ACEPTADA` pasan a `CANCELADA` |

Respuesta: **200** `TripDto` · **403** `FORBIDDEN` · **409** `INVALID_STATUS_TRANSITION`.

En esta entrega cancelar un viaje **no** envía notificación push (se mantienen únicamente los 3 tipos de RF04 definidos en §9); el pasajero con reserva `ACEPTADA` ve el cambio de estado al abrir la app. Esta simplificación queda documentada en "Limitaciones conocidas" del README.

---

## 6. Reservas (RF04)

### 6.1 Solicitar un asiento

`PUT /trips/{tripId}/reservations/{reservationId}` · pasajero · sin cuerpo · lo envía `SyncWorker`

- **201** `ReservationDto` en `PENDIENTE` · **200** en un reintento.
- **403** `CANNOT_RESERVE_OWN_TRIP` · **404** `NOT_FOUND` · **409** `ALREADY_RESERVED` (el pasajero ya tiene una reserva `PENDIENTE` o `ACEPTADA` en el viaje), `TRIP_NOT_OPEN`, `NO_SEATS_AVAILABLE` (el viaje está `COMPLETO`), `ID_CONFLICT`.
- Envía `NUEVA_SOLICITUD` al conductor.

Solicitar no descuenta la vacante: se descuenta al aceptar.

### 6.2 Solicitudes de un viaje (panel del conductor)

`GET /trips/{tripId}/reservations` · conductor → **200** `ReservationDto[]` · **403** `FORBIDDEN` · **404** `NOT_FOUND`.

### 6.3 Responder y cancelar

| Método y ruta | Quién | Efecto |
| --- | --- | --- |
| `POST /trips/{tripId}/reservations/{reservationId}/accept` | Conductor | `PENDIENTE` → `ACEPTADA`. Descuenta una vacante en la misma transacción, con bloqueo de la fila del viaje; si llega a 0, el viaje pasa a `COMPLETO`. Envía `SOLICITUD_ACEPTADA` |
| `POST /trips/{tripId}/reservations/{reservationId}/reject` | Conductor | `PENDIENTE` → `RECHAZADA`. Envía `SOLICITUD_RECHAZADA` |
| `POST /trips/{tripId}/reservations/{reservationId}/cancel` | Pasajero | `PENDIENTE` o `ACEPTADA` → `CANCELADA`. Si estaba aceptada, devuelve la vacante y el viaje vuelve a `ABIERTO` |

Respuesta: **200** `ReservationDto` · **403** `FORBIDDEN` · **404** `NOT_FOUND` (el viaje no existe o la reserva no es de ese viaje) · **409** `NO_SEATS_AVAILABLE` (al aceptar), `TRIP_ALREADY_DEPARTED`, `INVALID_STATUS_TRANSITION` (estado de origen incorrecto, o viaje `FINALIZADO` o `CANCELADO`).

Una vez que el viaje empezó (fecha y hora de salida en el pasado), sus reservas ya no cambian: aceptar, rechazar o cancelar responde **409** `TRIP_ALREADY_DEPARTED`, salvo que se repita una transición que ya ocurrió (idempotencia). Al cancelar, el mensaje es "No se puede cancelar un viaje que ya empezó.": el pasajero conserva el lugar y la reserva sigue `ACEPTADA` (en una versión futura, el aporte se cobra igual).

Cómo lo maneja la app, sobre todo con una cancelación hecha sin conexión que se sincroniza después de la salida:
- El `SyncWorker` no reintenta: es un rechazo definitivo. Marca la reserva como sincronizada y le devuelve el estado real (`ACEPTADA`), pidiendo otra vez el viaje para actualizar Room.
- La pantalla muestra el mensaje "No se puede cancelar un viaje que ya empezó" y el viaje sigue en "Mi viaje".
- Antes de enviar, la app ya oculta "Cancelar" cuando el viaje empezó; el 409 cubre el caso sin conexión.

Cuando la app recibe 409 `NO_SEATS_AVAILABLE` o `TRIP_NOT_OPEN` al sincronizar una reserva hecha sin conexión, muestra el mensaje de la preentrega (12.1) y vuelve a pedir el viaje para actualizar Room.

---

## 7. Calificaciones y bandeja (alcance 7 y 8)

**Fuera de esta entrega** (ver "Qué se recorta" en DIVISION_TRABAJO.md). Se deja el contrato para una versión siguiente; el tipo `CALIFICAR_VIAJE` de las push tampoco se envía.

### 7.1 Calificar

`PUT /trips/{tripId}/ratings/{ratingId}`

```json
{ "toUserId": "9a2e5c7b-3d8f-4b1a-a6e0-1c4f7d9b2e58", "stars": 5 }
```

- **201** / **200** en un reintento.
- **400** `VALIDATION_ERROR` (`stars` entre 1 y 5) · **403** `FORBIDDEN` (no participó del viaje) · **409** `TRIP_NOT_OPEN` (si el viaje no está `FINALIZADO`), `ALREADY_RATED`.
- Actualiza `ratingAvg` y `ratingCount` del usuario calificado.

### 7.2 Bandeja

| Método y ruta | Respuesta |
| --- | --- |
| `GET /notifications?since=<instante>` | **200** `NotificationDto[]`, las más nuevas primero, como máximo 50 |
| `POST /notifications/{notificationId}/read` | **204** |

Según la preentrega (12.1), la bandeja no se guarda en el teléfono: sin conexión, la pantalla muestra el estado de error con reintento.

---

## 8. Enlaces de los correos y deep links

Los clientes de correo (Gmail incluido) no permiten tocar enlaces con un esquema propio como `uniride://`. Por eso el correo lleva un enlace HTTP al backend, y el backend responde una página mínima que abre la app:

| Correo | Enlace del correo | Abre en la app |
| --- | --- | --- |
| Activación | `{APP_LINK_BASE_URL}/api/v1/links/activate?token=...` | `uniride://activate?token=...` → llama a `POST /auth/activate` |
| Recuperación | `{APP_LINK_BASE_URL}/api/v1/links/reset-password?token=...` | `uniride://reset-password?token=...` → pantalla de nueva contraseña |

Se utiliza esta página HTML intermedia en el backend (`GET /api/v1/links/activate` y `GET /api/v1/links/reset-password`), con un botón "Abrir UniRide" hacia `uniride://…` y un texto para el caso en que la app no esté instalada. Los Android App Links verificados sobre HTTPS quedan fuera de esta entrega.

En desarrollo, los correos se ven en Mailpit (`http://localhost:8025`), y `APP_LINK_BASE_URL` es `http://10.0.2.2:8080` para abrirlos desde el emulador.

---

## 9. Notificaciones push (FCM)

El backend envía mensajes **de datos** (no de notificación), así la app decide qué mostrar y adónde navegar aunque esté en segundo plano:

```json
{
  "type": "SOLICITUD_ACEPTADA",
  "notificationId": "e1f4b7a0-2c5d-4e8f-9b3a-6d0c1e4f7a29",
  "tripId": "0b7c4f0e-5d1a-4f5a-8a52-7c9e5e0d2b31",
  "reservationId": "c3d9a8e2-7f41-4e0b-b1a6-5f2e9c4d8a17",
  "title": "¡Tenés lugar!",
  "body": "Lucía aceptó tu solicitud para el viaje de las 22:15."
}
```

| `type` | Destinatario | Al tocarla abre | Además, la app |
| --- | --- | --- | --- |
| `NUEVA_SOLICITUD` | Conductor | C4 Panel del conductor | Refresca las solicitudes del viaje |
| `SOLICITUD_ACEPTADA` | Pasajero | B6 Mi viaje | Refresca el viaje (ahora con patente y teléfono) |
| `SOLICITUD_RECHAZADA` | Pasajero | B6 Mi viaje | Refresca el viaje |
Si el usuario no dio permiso de notificaciones (RNF08), no recibe la push, pero ve el estado actualizado al abrir la app (tarjeta del inicio y panel del conductor). La bandeja (§7.2) y el tipo `CALIFICAR_VIAJE` quedan fuera de esta entrega.

---

## 10. Otros

- `GET /actuator/health` · público → **200** `{ "status": "UP" }`. Para comprobar que el backend está levantado antes de la demo.
- Swagger UI en `/swagger-ui.html` (solo en desarrollo).

---

## 11. Decisiones cerradas en A0

1. **Franjas horarias de `Shift`:** intervalos semiabiertos `MANANA` (`[06:00, 13:00)`), `TARDE` (`[13:00, 18:00)`), `NOCHE` (`[18:00, 06:00)`), calculado desde `departureTime` (no viaja en `TripDto`).
2. **Sedes y salidas (`GET /campuses`):** `MONSERRAT`, `BELGRANO` y `RECOLETA` con sus puntos de encuentro unificados en formato `"Salida …"` (§5.2).
3. **Política de contraseña y JWT:** mínimo 8 caracteres con al menos 1 letra y 1 número; JWT HS256 de 30 días sin refresh token como simplificación del TP (en producción lo óptimo es access token corto de 15–30 min + refresh token rotativo y revocable en base de datos, ver §4.4).
4. **Activación de cuenta:** `POST /auth/activate` devuelve `AuthResponse` y deja la sesión iniciada automáticamente.
5. **Listado de viajes (`GET /trips`):** sin paginación, tope de 100 resultados.
6. **Push al cancelar un viaje:** fuera de alcance en esta entrega (se mantienen los 3 tipos de RF04; documentado en "Limitaciones conocidas" del README).
7. **Enlaces de los correos:** página HTML intermedia del backend en `/api/v1/links/activate` y `/api/v1/links/reset-password` redirigiendo a `uniride://…`.
