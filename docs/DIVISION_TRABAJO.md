# UniRide · División del trabajo

## Criterio

En la preentrega (sección 14) cada uno quedó a cargo de una **capa**. Si repartimos el código solo por capas, nadie puede terminar nada hasta que los otros tres terminen la suya, y en la defensa cada uno conoce un cuarto de cada funcionalidad. Por eso trabajamos en dos niveles:

1. **Fase 0 · Cimientos (semana 1).** Cada uno arma la base de **su capa**, en paralelo. Al final de la semana hay una app que compila, navega y muestra datos falsos, y un backend que arranca con `docker compose up`.
2. **Fases 1 y 2 · Funcionalidades (semanas 2 a 5).** Cada uno es dueño de **un requisito funcional de punta a punta**: endpoints del backend, dominio, datos, ViewModel y pantallas. Se eligió el RF que más usa su especialidad.

El rol de la preentrega se mantiene como **responsable de capa**: revisa todo PR que toque su capa y decide las dudas sobre ella. Así se cumple lo que declaramos y, a la vez, todos pasan por todas las capas, que es lo que piden en la defensa ("todos los integrantes deberán poder explicar el funcionamiento general").

| Integrante | Responsable de capa (revisa) | Dueño de funcionalidad |
| --- | --- | --- |
| Maria Estefanía Sassone | Arquitectura, Room, Offline First y sincronización | **RF02** · Publicación de viaje + sincronización offline |
| Alvaro Manuel Herrera | Red y backend (Retrofit, API, Spring Boot) | **RF01** · Autenticación institucional y perfil + push |
| Nicolás Agustín Benitez | UI (tema, componentes, estados de pantalla) | **RF03** · Explorar y filtrar viajes |
| Gustavo Ariel Forcherio | Navegación, ViewModels y dominio | **RF04** · Reserva y confirmación de vacante |

Las semanas son relativas: completar las fechas reales cuando tengamos la fecha de entrega final.

---

## Fase 0 · Cimientos (semana 1)

El proyecto Android ya existe en `develop` con las carpetas vacías. El día 1 **Estefanía** le agrega las dependencias (tarea E0) y **Alvaro** crea el backend (A1). Desde ahí los cuatro trabajan en paralelo. Lo que cada uno entrega acá es lo que los demás van a usar, así que la prioridad es que **la interfaz (firmas, nombres, contrato) esté pronto**, aunque la implementación sea provisoria.

### Estefanía · Arquitectura y persistencia

- **E0 (día 1)** Completar el proyecto Android: dependencias de CONVENCIONES §1 en `libs.versions.toml` (Hilt, KSP, Room, Navigation, Retrofit, Moshi, DataStore, WorkManager, Tink, tests), `UniRideApp` con `@HiltAndroidApp`, mover `ui/theme` a `presentation/theme`, `.github/pull_request_template.md`, workflow de GitHub Actions (build + Lint + tests) y protección de ramas. Corregir en el README el prefijo de ramas (`feat/`, no `feature/`).
- **E1** Room: `UniRideDatabase`, `UserEntity`, `TripEntity`, `ReservationEntity` (campos de la preentrega 11.1, ids `String`), DAOs con las consultas básicas, `TypeConverters`, `DatabaseModule`.
- **E2** `core/result` (`AppResult`, `DataError`) y `DispatchersProvider`.
- **E3** Tests de DAO en memoria para insertar, observar y la consulta de pendientes (`isSync = false`).

### Alvaro · Red y backend

- **A0 (día 1–2) Contrato de la API** en `docs/API.md`: endpoints, request/response en JSON, códigos de error (`ErrorCode`) y deep links. **Es lo primero que necesita todo el equipo.** Lo revisan los cuatro antes de seguir.
- **A1** Backend base en `backend/`: Spring Boot 3.5 + Java 21, `docker-compose.yml` (PostgreSQL + Mailpit + backend), `application.yml` con variables de entorno y `.env.example`, Flyway con la primera migración (`users`, `trips`, `reservations`), `GlobalExceptionHandler` con el formato de error, Spring Security + JWT, springdoc, job `mvn verify` en CI.
- **A2** App: `NetworkModule` (Retrofit + Moshi + OkHttp), `AuthInterceptor`, logging solo en debug, URL base en `BuildConfig`, `network_security_config` de debug para `10.0.2.2`.
- **A3** App: `safeApiCall` (excepción/HTTP → `DataError`) con tests.
- **A4** App: `Fake*RemoteDataSource` para auth, viajes y reservas, con datos de ejemplo y demoras simuladas, y el flag `USE_FAKE_BACKEND`. Permite que todos avancen en la app sin depender del backend.

### Nicolás · UI

- **N1** `presentation/theme` con los tokens de la guía de alta fidelidad de Figma (colores, tipografía, formas, modo claro/oscuro).
- **N2** Componentes comunes de CONVENCIONES §7.4 en `presentation/common`: `ContentState`, `ContentStateHost`, `EmptyState`, `ErrorState`, `LoadingState`, `OfflineBanner`, `PrimaryButton`, `UniRideTopBar`, `TripCard`, `StatusChip`, `LabeledTextField`, `SelectorField`. Cada uno con `@Preview`.
- **N3** `strings.xml` base (mensajes de error genéricos por `DataError`, textos del banner, estados vacíos).

### Gustavo · Navegación y dominio

- **G1** Modelos de dominio (`User`, `Trip`, `Reservation`, `TripFilters`, enums del glosario) e **interfaces de repositorio** (`AuthRepository`, `TripRepository`, `ReservationRepository`, `UserPreferencesRepository`). Se acuerdan con Estefanía y Alvaro antes de mergear: son el contrato entre capas, y los nombres de campos tienen que coincidir con `docs/API.md`.
- **G2** Validaciones de dominio con tests: `EmailValidator` (@uade.edu.ar), `PasswordPolicy`, regla de plazas 1–4, fecha/hora futura.
- **G3** Navegación: `UniRideNavHost`, rutas `@Serializable`, subgrafos vacíos por área (`authGraph`, `passengerGraph`, `driverGraph`), barra inferior y selector de modo pasajero/conductor. Las pantallas son placeholders hasta la fase 1.
- **G4** `NetworkMonitor` (ConnectivityManager) + `ObserveConnectivityUseCase`, que usan todas las pantallas para el banner.

**Hito de fin de semana 1:** la app abre, navega entre placeholders de todas las pantallas, muestra el banner al activar el modo avión y lista viajes del fake; el backend arranca con `docker compose up` y responde en Swagger. Merge de `develop` a `main` con tag `v0.1`.

---

## Fase 1 · Funcionalidades principales (semanas 2 a 4)

Cada uno hace su RF completo: **endpoints del backend**, UseCases, repositorio, ViewModel, pantallas y tests. En la app se trabaja contra los fakes hasta que el endpoint real esté mergeado; después se cambia el flag y se prueba de punta a punta.

### Alvaro · RF01 Autenticación institucional y perfil

Pantallas: A1 bienvenida, A2 login, registro, "revisá tu correo", restablecer contraseña, perfil.

- Backend: `POST /auth/register` (solo @uade.edu.ar, cuenta `PENDIENTE`, correo con enlace), `POST /auth/activate`, `POST /auth/login` (JWT), `POST /auth/password-reset` y su confirmación, `GET/PUT /users/me` (perfil y vehículo).
- App: `RegisterUseCase`, `LoginUseCase`, `ActivateAccountUseCase`, `RequestPasswordResetUseCase`, `LogoutUseCase`, `ObserveCurrentUserUseCase`.
- `SessionTokenStore` con Keystore + Tink sobre DataStore (11.3). Inicio automático si hay sesión; cierre de sesión ante 401 (borra token y Room).
- Deep links de activación y de restablecer contraseña.
- Criterios de aceptación de RF01 cubiertos con tests (validación en el cliente y en el servidor, cuenta pendiente, sesión persistida, mensaje que no revela si el correo existe).

### Estefanía · RF02 Publicación de viaje + sincronización

Pantallas: C1 inicio del conductor sin viaje, C2 formulario de publicación.

- Backend: `PUT /trips/{tripId}` idempotente, `POST /trips/{tripId}/finish`, `POST /trips/{tripId}/cancel`, validaciones (plazas 1–4, fecha futura, aporte ≥ 0).
- App: `PublishTripUseCase` y `TripRepository.publish`.
- Flujo offline completo: guardar con `isSync = false`, `SyncScheduler`, `SyncWorker` (`@HiltWorker`, restricción de red, backoff), marcar sincronizado, manejar el rechazo del servidor. **Este mecanismo lo usa después RF04 para las reservas**, así que se diseña genérico.
- Ubicación: botón "Usar mi ubicación" con Fused Location (una lectura, permiso en contexto, fallback manual).
- Tests del worker, del caso "publicar sin conexión" (criterio de aceptación de RF02) y de la idempotencia del `PUT` en el backend.

### Nicolás · RF03 Explorar y filtrar viajes

Pantallas: B1 Explorar (feed + filtros), estados vacío/error/sin conexión.

- Backend: `GET /trips` con filtros (`campus`, `direction`, `date`, `shift`) y `updatedSince`; listados con nombre e inicial del apellido, sin patente ni teléfono (RNF07).
- App: `ObserveTripsUseCase(filters)`, `RefreshTripsUseCase`.
- Filtros combinados (sede, sentido, turno/franja, fecha) que actualizan la lista de forma reactiva.
- Últimos filtros y última sede en DataStore (`UserPreferencesRepository`).
- Refresco al abrir, al volver a primer plano, con *pull to refresh* y al recuperar conexión. "Actualizado HH:MM".
- Regla de retención del catálogo (11.2) en `TripDao.replaceCatalog` (`@Transaction`), coordinada con Estefanía.
- Primer inicio sin datos ni conexión (12.1).

### Gustavo · RF04 Reserva y confirmación de vacante

Pantallas: B3 Detalle del viaje, B5 Asiento confirmado, B6 Mi viaje, C4 Panel del conductor.

- Backend: `GET /trips/{tripId}` (con patente y teléfono solo si hay reserva aceptada), `PUT /trips/{tripId}/reservations/{reservationId}` idempotente, `POST .../accept`, `POST .../reject`, `POST .../cancel`. Aceptar descuenta la vacante en la misma transacción con bloqueo de fila; sin vacantes devuelve 409 `NO_SEATS_AVAILABLE`.
- App: `RequestSeatUseCase`, `RespondToRequestUseCase` (aceptar/rechazar), `CancelReservationUseCase`, `ObserveMyActiveTripUseCase`, `ObserveDriverPanelUseCase`.
- Reserva offline reutilizando el mecanismo de sincronización de RF02.
- Descuento de vacante en modelo y UI; conflicto "sin plazas" con el mensaje de 12.1.
- Viaje confirmado disponible sin conexión con patente, teléfono y punto de encuentro.
- Inicio dinámico (`presentation/home`) según modo y estado: tarjeta de viaje activo o feed / formulario o panel.

---

## Fase 2 · Integración y alcance extendido (semanas 4 y 5)

| Tarea | Responsable | Apoyo |
| --- | --- | --- |
| Probar de punta a punta contra el backend real (flag `USE_FAKE_BACKEND = false`) | todos, cada uno su RF | Alvaro |
| Push con FCM: Firebase Admin en el backend, `UniRideMessagingService`, registro del token del dispositivo, abrir la pantalla al tocar, permiso en contexto (RNF08) | Alvaro | Gustavo (deep links y navegación) |
| Bandeja de notificaciones (alcance 8): tabla y endpoint en el backend, pantalla en la app | Gustavo | Nicolás (UI) |
| Calificación mutua 1–5 estrellas al finalizar el viaje (alcance 7): endpoint y pantalla | Estefanía | Nicolás (UI) |
| Revisión de accesibilidad (contraste, TalkBack, 48 dp) y pulido visual | Nicolás | todos |
| Prueba manual de los escenarios offline de 12.1 en un dispositivo | Estefanía | todos |
| Decidir y preparar dónde corre el backend para la demo (CONVENCIONES §14) | Alvaro | todos |

Si el tiempo no alcanza, **bandeja y calificación son lo primero que se recorta**: el enunciado valora más cuatro RF completos que ocho a medias. En ese caso se documenta en "Limitaciones conocidas" del README.

---

## Fase 3 · Entrega (semana 6)

| Tarea | Responsable |
| --- | --- |
| README: qué hace, cómo ejecutar app y backend, arquitectura final, tecnologías, RF alcanzados, decisiones, limitaciones (7.4 del enunciado) | Gustavo coordina, cada uno escribe la sección de su RF |
| Diagramas de arquitectura y topología actualizados al código final | Estefanía |
| Guion de la demo: Problema → Escenario → Acción → Respuesta → Valor (7.5) | Nicolás |
| Ensayo de defensa: cada uno explica un RF que **no** hizo | todos |
| Merge `develop` → `main`, tag `v1.0`, APK de release | Estefanía |

---

## Dependencias entre tareas

```
A0 contrato API ───► A1 backend base ──► endpoints de cada RF
       │
       └─────────► A4 fakes ──► pantallas de RF01, RF02, RF03, RF04
E0 dependencias Android ──► todo lo de la app
G1 modelos + interfaces ──► E1 Room (mappers), A4 fakes, todos los RF
N2 componentes ────► pantallas de todos los RF
RF02 mecanismo de sync ──► reserva offline de RF04
RF01 sesión y JWT ──► todos los endpoints autenticados, push, 401 global
```

Si alguien queda bloqueado esperando a otro, avisa en el grupo el mismo día. Lo normal es avanzar con un fake o una interfaz provisoria y ajustar después.

---

## Archivos compartidos (donde más vamos a chocar)

| Archivo | Dueño | Regla |
| --- | --- | --- |
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | Estefanía | PR propio de tipo `chore(build)`, no mezclado con una funcionalidad |
| `UniRideDatabase.kt` (entities, versión, migraciones) | Estefanía | Un cambio de esquema sube la versión; avisar antes en el grupo |
| `docs/API.md` | Alvaro | Cambiar el contrato requiere aprobación de Alvaro, en el mismo PR que el código |
| `backend/pom.xml`, `backend/.../common/**` | Alvaro | Seguridad, errores y configuración los revisa Alvaro |
| `backend/src/main/resources/db/migration` | Alvaro | Nombres con fecha (`V2026_11_21_1__...`); nunca se edita una migración mergeada |
| `UniRideNavHost.kt` | Gustavo | Cada área agrega su ruta en su propio subgrafo, no en el NavHost |
| `presentation/theme`, `presentation/common` | Nicolás | Un componente nuevo compartido lo revisa Nicolás |
| `domain/model`, `domain/repository` | Gustavo | Cambiar una interfaz la revisan Gustavo y el dueño de la implementación |
| `strings.xml` | todos | Agregar al final de la sección de tu pantalla, con prefijo `pantalla_` |

---

## Ritmo de trabajo

- **Sincronización corta 2 veces por semana** (15 min): qué terminé, qué sigo, en qué estoy bloqueado.
- Tablero (GitHub Projects o Trello) con una tarjeta por tarea de este documento (E1, A0, RF03…). La rama y el PR mencionan el código de la tarea.
- Commits todos los días que se trabaja; PRs chicos. Mejor tres PRs de 200 líneas que uno de 1500 el día antes de la entrega.
- Cada uno revisa al menos un PR de otro por semana, aunque no sea de su capa: es la mejor preparación para la defensa.
