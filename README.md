# UniRide · Carpool universitario

**UniRide** conecta a estudiantes de UADE que van en auto a la facultad con compañeros que hacen el mismo trayecto. El conductor publica un viaje a o desde una sede, los pasajeros lo encuentran filtrando por sede, sentido, fecha y turno, piden un asiento y el conductor acepta o rechaza. La app funciona sin conexión: lo que se hace offline se guarda y se manda solo cuando vuelve la red.

Trabajo Práctico Obligatorio de **Desarrollo de Aplicaciones I**, Universidad Argentina de la Empresa (UADE).

---

## Equipo

| Integrante | Legajo | Responsable de capa | Requisito funcional |
| --- | --- | --- | --- |
| Sassone, Maria Estefanía | 1145414 | Arquitectura de la app, Room, Offline First y sincronización | RF02 · Publicación de viaje |
| Herrera, Alvaro Manuel | 1164449 | Red de la app (Retrofit, contrato de la API, errores de red) | RF01 · Autenticación institucional y perfil |
| Benitez, Nicolás Agustín | 1137895 | UI (tema, componentes, estados de pantalla) | RF03 · Explorar y filtrar viajes |
| Forcherio, Gustavo Ariel | 1200714 | Backend, navegación, ViewModels y dominio | RF04 · Reserva y confirmación de vacante |

**Docentes:** Peña, Alejandro Francisco · Narducci, Adrian Alberto

Cada integrante es dueño de un requisito funcional de punta a punta (endpoints, dominio, datos, ViewModel y pantallas) y además revisa todo lo que toque su capa. El reparto completo está en [`docs/DIVISION_TRABAJO.md`](docs/DIVISION_TRABAJO.md).

---

## Repositorio

Es un monorepo con la app y el backend:

```text
.
├── mobile/               App Android (Kotlin, Jetpack Compose)
├── backend/              API REST (Spring Boot 3.5, Java 21)
├── docs/
│   ├── API.md            Contrato de la API (fuente de verdad)
│   ├── CONVENCIONES.md   Reglas de arquitectura, nombres, errores, tests y Git
│   └── DIVISION_TRABAJO.md
├── docker-compose.yml    PostgreSQL + Mailpit + backend
└── .env.example          Variables del backend (se copia a .env, que no se commitea)
```

---

## Cómo levantarlo

### 1. Backend

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
| Correos (Mailpit) | `http://localhost:8025`: ahí llegan los enlaces de activación y de recuperación de contraseña |

Con el perfil `demo`, que `docker compose` activa por defecto, se cargan tres usuarios ya activados con la contraseña `UniRide2026`:

| Correo | Modo | Vehículo |
| --- | --- | --- |
| `conductora.demo@uade.edu.ar` | Conductor y pasajero | Gol Trend gris, `AB123CD` |
| `pasajero.demo@uade.edu.ar` | Pasajero | — |
| `conductor2.demo@uade.edu.ar` | Conductor | Cronos blanco, `AE456FG` |

`docker compose down` apaga todo y `docker compose down -v` además borra la base. Para programar el backend desde IntelliJ y correr sus tests, ver [`backend/README.md`](backend/README.md).

### 2. App

1. Abrir la carpeta `mobile/` (no la raíz) en Android Studio. Gradle usa el JDK que trae Android Studio.
2. Correr la configuración `app` en un emulador o un teléfono con Android 11 (API 30) o superior.

Por defecto, la versión debug usa un **backend falso en memoria**, así se puede recorrer la app sin levantar nada. Para usar el backend real, agregar en `mobile/local.properties`:

```properties
useFakeBackend=false
# Emulador: 10.0.2.2 es la máquina donde corre Docker (es el valor por defecto).
# Teléfono en el mismo Wi-Fi: la IP de esa máquina, por ejemplo http://192.168.0.10:8080/api/v1/
apiBaseUrl=http://10.0.2.2:8080/api/v1/
```

### Tests

| Qué | Comando |
| --- | --- |
| App: build, lint y tests unitarios | `./gradlew assembleDebug lintDebug testDebugUnitTest` (desde `mobile/`) |
| App: tests instrumentados (con un emulador abierto) | `./gradlew connectedDebugAndroidTest` (desde `mobile/`) |
| Backend | `./mvnw verify` (desde `backend/`; en Windows, `mvnw.cmd verify`) |

GitHub Actions corre los tests unitarios y el lint de la app y `mvn verify` del backend en cada PR a `develop` o `main`.

---

## Arquitectura

### App

**MVVM + Clean Architecture**, en un solo módulo `app` separado en capas. Las dependencias van siempre hacia el dominio:

```text
UI (Compose) → ViewModel → UseCase → Repository (interfaz) → RepositoryImpl → Room / API
```

```text
mobile/app/src/main/java/com/example/uniride/
├── core/            AppResult y DataError: los repositorios devuelven errores, no lanzan excepciones
├── domain/          Kotlin puro, sin Android
│   ├── model/       Trip, Reservation, User, Campus y los enums del glosario
│   ├── repository/  Interfaces de los repositorios
│   ├── usecase/     Un caso de uso por acción o consulta
│   └── validation/  Reglas que la app comparte con el servidor (por ejemplo, SeatRequestRule)
├── data/
│   ├── local/       Room: entities, DAOs y la base
│   ├── remote/      Retrofit: DTOs, APIs y los fakes en memoria del backend
│   ├── datastore/   Preferencias y token cifrado (Keystore + Tink)
│   ├── sync/        SyncWorker y SyncScheduler (WorkManager)
│   ├── mapper/      Entity ↔ dominio ↔ DTO
│   └── repository/  Implementaciones
├── di/              Módulos de Hilt
└── presentation/    Una carpeta por área (auth, feed, detail, publish, reservations…),
                     más common (componentes), navigation y theme
```

- **Offline First.** Room es la única fuente de verdad: las pantallas observan un `Flow` de Room y nunca la respuesta de la API. La API solo actualiza Room.
- **Escrituras sin conexión.** Publicar un viaje, pedir un asiento, aceptar, rechazar o cancelar sigue siempre los mismos pasos:
  1. El id es un UUID generado en el teléfono.
  2. Se guarda en Room como pendiente.
  3. WorkManager lo manda con un `PUT` idempotente cuando hay red.

  Si el servidor lo rechaza (por ejemplo, ya no quedan asientos), la pantalla muestra el motivo.
- **Pantallas.** Cada pantalla tiene un `XxxUiState` expuesto como `StateFlow`, una entrada única `onAction(XxxAction)` y efectos de una vez (navegar, mostrar un mensaje) por `Channel`. `XxxRoute` obtiene el ViewModel; `XxxScreen` no tiene estado y tiene `@Preview`. Todas resuelven cargando, contenido, vacío, error y sin conexión.
- **Navegación.** Navigation Compose con rutas tipadas (`@Serializable`) que solo llevan ids. Un subgrafo por área y una barra inferior que cambia según el modo, pasajero o conductor. `AppViewModel` decide dónde arranca la app según haya sesión o no.
- **Sesión.** El JWT vive solo en `SessionTokenStore`, cifrado con Keystore + Tink. Un 401 en cualquier pedido, también desde el `SyncWorker`, cierra la sesión y borra los datos locales.

### Backend

Spring Boot 3.5 con Java 21, PostgreSQL 16 y paquetes por capa:

```text
controller → service → repository → model (entidades JPA)
```

- El Controller solo valida y delega. Las reglas de negocio y las transacciones están en el Service, y las entidades validan sus propias transiciones (`Trip.takeSeat()`, `Reservation.accept()`…).
- Nunca se devuelve una entidad: siempre un `record` DTO.
- Los errores salen todos con el formato `{ code, message }` desde `GlobalExceptionHandler`, con los códigos de [`docs/API.md`](docs/API.md) §2.
- Seguridad con Spring Security y JWT (HS256, id del usuario en `sub`).
- Las tablas las crea Hibernate desde las entidades. Los datos iniciales están en `data.sql` y los usuarios de prueba en `data-demo.sql`.

Las reglas completas de las dos partes están en [`docs/CONVENCIONES.md`](docs/CONVENCIONES.md).

---

## Requisitos funcionales

<!-- Cada dueño completa o corrige la sección de su RF: qué se puede hacer, en qué pantallas y qué escenarios sin conexión cubre. -->

### RF01 · Autenticación institucional y perfil (Alvaro)

- **Registro y validaciones.** Pantallas **A1 Bienvenida** y **A3 Registro**: solo acepta correos `@uade.edu.ar`, exige contraseña de al menos 8 caracteres con letra y número (hasta 72 bytes UTF-8, tope de BCrypt), teléfono válido y, si el modo elegido es Conductor, datos completos del vehículo con patente normalizada. Las mismas reglas se validan en la app y en el servidor.
- **Activación por correo y deep link.** Al registrarse la cuenta queda `PENDIENTE` y pasa a **A4 Revisá tu correo** (con opción de reenviar el enlace). El backend envía el correo post-commit (`AFTER_COMMIT`) con un enlace de un solo uso válido por 24 horas (`/api/v1/links/activate?token=...`) que redirige a `uniride://activate?token=...` (**A5 Activar cuenta**), activa la cuenta e inicia sesión automáticamente.
- **Login y recuperación de contraseña.** **A2 Login** valida dominio y distingue credenciales incorrectas de cuenta todavía sin activar (ofreciendo ir directo a reenviar la activación). **A6 Recuperar contraseña** responde de forma idéntica exista o no el correo y envía un enlace de un solo uso válido por 1 hora (`/api/v1/links/reset-password?token=...` → `uniride://reset-password?token=...`, **A7 Nueva contraseña**).
- **Sesión segura.** El JWT (HS256) se guarda solo en `SessionTokenStore` (Keystore + Tink) y el arranque es automático si hay sesión. Ante un `401` en cualquier pedido o al cerrar sesión, se limpia el token, Room (`clearAllTables`) y las preferencias, y el shell vuelve a Bienvenida. Al iniciar sesión o activar otra cuenta también se limpia cualquier estado local previo.
- **Perfil y modo sin conexión.** **D1 Perfil** lee el usuario guardado en Room (funciona offline con banner de sin conexión), refresca en segundo plano conservando ediciones sin guardar, permite editar datos personales, modo habitual y vehículo, y cerrar sesión previa confirmación (incluyendo baja best-effort del token FCM en `DELETE /users/me/device-token`).

### RF02 · Publicación de viaje y sincronización (Estefanía)

El conductor publica un trayecto (sede, sentido, fecha, hora, plazas y aporte). Sin conexión el viaje se guarda y se publica solo cuando vuelve la red. RF04 reutiliza este mecanismo de sincronización para las reservas.

<!-- Estefanía: pantallas, validaciones, comportamiento offline, estado. -->

### RF03 · Explorar y filtrar viajes (Nicolás)

Feed de viajes filtrable por sede, sentido, fecha y turno. Recuerda los últimos filtros, se actualiza deslizando hacia abajo y muestra cuándo se actualizó por última vez. Sin conexión se ve el último catálogo guardado.

<!-- Nicolás: pantallas, regla de retención del catálogo, estado. -->

### RF04 · Reserva y confirmación de vacante (Gustavo)

- **Detalle del viaje.** El pasajero ve el viaje y pide un asiento; la app explica por qué no puede cuando corresponde (sin vacantes, viaje propio, ya salió, ya tiene una reserva).
- **Mi viaje.** Muestra el estado de la solicitud (pendiente, aceptada, rechazada, cancelada) y permite cancelarla, previa confirmación, hasta la salida.
- **Panel del conductor.** Lista las solicitudes de un viaje, primero las pendientes, para aceptarlas o rechazarlas hasta la salida.
- **Inicio.** El del pasajero muestra su próximo viaje; el del conductor, sus viajes publicados.
- **Datos de contacto.** La patente y los teléfonos solo se muestran entre el conductor y los pasajeros con reserva aceptada (RNF07): el backend no los manda a nadie más.
- **Vacantes.** Aceptar una solicitud descuenta la vacante en la misma transacción, con bloqueo, así dos aceptaciones simultáneas no dejan el viaje con plazas negativas. Sin vacantes, el servidor responde `409 NO_SEATS_AVAILABLE`.
- **Sin conexión.** Pedir, aceptar, rechazar y cancelar funcionan offline y se mandan al volver la red. Si el servidor rechaza el cambio (por ejemplo, se llenó el viaje mientras tanto), la pantalla dice por qué y vuelve al estado real. El viaje confirmado se puede consultar sin conexión.

---

## Decisiones

- **Reparto por funcionalidad y no solo por capa.** En la preentrega cada uno tenía una capa. Para que nadie dependa de que los otros tres terminen y para que todos conozcan todas las capas en la defensa, cada uno hace un RF completo y su rol de la preentrega queda como revisor de esa capa.
- **Cambio de roles (punto 4.15).** La preentrega no le asignaba el backend a nadie. Gustavo armó la base (Docker, seguridad, errores) y cada integrante hizo los endpoints de su RF.
- **Spring Boot y no Gin.** El equipo ya usa Spring Boot 3.5 con JWT en Desarrollo de Aplicaciones II, el SDK de Firebase Admin para Java es el oficial más completo y Spring resuelve correo, validación y seguridad.
- **Ids generados en el teléfono y `PUT` idempotente.** Un viaje o una reserva creados sin conexión tienen id desde el primer momento, y reintentar el envío nunca crea un duplicado.
- **El dominio de la app repite las reglas del servidor** (`SeatRequestRule`) para avisar antes de mandar algo que va a ser rechazado. La decisión final siempre es del servidor.
- **Backend falso en debug.** Las pantallas se pudieron hacer en paralelo con los endpoints, sin esperar a que el backend de cada RF estuviera mergeado.

---

## Limitaciones conocidas

<!-- Completar al cerrar D6 con lo que efectivamente quedó afuera. -->

- **Calificación mutua y bandeja de notificaciones** (alcances 7 y 8): no se implementan. La calificación que se muestra viene de los datos de prueba.
- **Notificaciones push con FCM:** <!-- Alvaro y Gustavo: actualizar cuando se sepa si llegan. --> sin ellas, el pasajero ve el estado de su solicitud cuando abre la app o actualiza la pantalla.
- **Ubicación** para sugerir el punto de partida: no se implementa; el punto se elige a mano.
- **Paquete `com.example.uniride`:** alcanza para el TP, pero Google Play no lo acepta.

---

## Forma de trabajo

- Ramas `feat/`, `fix/`, `refactor/`, `test/`, `docs/` y `chore/` desde `develop`, con Pull Request y revisión. `main` recibe solo las entregas.
- Commits en [Conventional Commits](https://www.conventionalcommits.org/).
- Un PR toca `mobile/` o `backend/`. Si cambia el contrato, `docs/API.md` se actualiza en el mismo PR.
- El checklist de revisión está en [`docs/CONVENCIONES.md`](docs/CONVENCIONES.md) §12 y en la plantilla de PR.
