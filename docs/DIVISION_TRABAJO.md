# UniRide · División del trabajo

**Tenemos una semana.** El plan está en días (D1 a D7, donde D7 es el día de la entrega); completar las fechas reales en la tabla de abajo. La prioridad es la del enunciado: **cuatro RF completos y funcionando valen más que ocho a medias** (4.5 y 10).

| Día | Fecha | Objetivo |
| --- | --- | --- |
| D1 | | Cimientos: backend que levanta con Docker, app con dependencias, modelos y contrato de la API acordados |
| D2–D4 | | Cada uno hace su RF de punta a punta |
| D5 | | Integración: la app contra el backend real, pruebas sin conexión |
| D6 | | Push (si se llegó), arreglos, README y diagramas |
| D7 | | Ensayo de la demo y entrega |

## Criterio

En la preentrega (sección 14) cada uno quedó a cargo de una **capa**. Si repartimos el código solo por capas, nadie puede terminar nada hasta que los otros tres terminen la suya, y en la defensa cada uno conoce un cuarto de cada funcionalidad. Por eso:

1. **D1 · Cimientos.** Cada uno arma la base de **su capa**, en paralelo.
2. **D2 a D4 · Funcionalidades.** Cada uno es dueño de **un requisito funcional de punta a punta**: endpoints del backend, dominio, datos, ViewModel y pantallas.

El rol de la preentrega se mantiene como **responsable de capa**: revisa todo PR que toque su capa. Así se cumple lo que declaramos y, a la vez, todos pasan por todas las capas, que es lo que piden en la defensa.

| Integrante | Responsable de capa (revisa) | Dueño de funcionalidad |
| --- | --- | --- |
| Maria Estefanía Sassone | Arquitectura de la app, Room, Offline First y sincronización | **RF02** · Publicación de viaje + sincronización offline |
| Alvaro Manuel Herrera | Red de la app (Retrofit, contrato de la API, errores de red) | **RF01** · Autenticación institucional y perfil |
| Nicolás Agustín Benitez | UI (tema, componentes, estados de pantalla) | **RF03** · Explorar y filtrar viajes |
| Gustavo Ariel Forcherio | Backend (base del proyecto), navegación, ViewModels y dominio | **RF04** · Reserva y confirmación de vacante |

> La preentrega no le asignaba el backend a nadie. Lo inicializa Gustavo y cada uno hace los endpoints de su RF. En la entrega final hay que declararlo como cambio de roles (4.15 del enunciado).

---

## D1 · Cimientos

Todo esto se hace en paralelo y se mergea en el día. Lo que cada uno entrega acá es lo que los demás usan, así que la prioridad es que **las firmas, los nombres y el contrato estén pronto**, aunque la implementación sea provisoria.

### Gustavo · Backend base y dominio

- **G1** Backend en `backend/`: Spring Boot 3.5 + Java 21 + Maven, `Dockerfile` de dos etapas, `docker-compose.yml` en la raíz (PostgreSQL + Mailpit + backend, CONVENCIONES §10.7), `.env.example`, `application.yml`, entidades JPA de `users`, `trips` y `reservations`, `data.sql` con sedes y usuarios de prueba, `GlobalExceptionHandler` con el formato de error, Spring Security + JWT (`JwtService`, filtro, `CurrentUser`) y springdoc. **Meta: `docker compose up --build` y Swagger respondiendo en `http://localhost:8080/swagger-ui.html`.**
- **G2** App: modelos de dominio (`User`, `Trip`, `Reservation`, `TripFilters`, enums del glosario) e **interfaces de repositorio** (`AuthRepository`, `TripRepository`, `ReservationRepository`, `UserPreferencesRepository`), con los mismos nombres de campo que `docs/API.md`.
- **G3** App: `UniRideNavHost`, rutas `@Serializable`, subgrafos vacíos por área y barra inferior con el selector de modo. Las pantallas son placeholders.

### Estefanía · Arquitectura y persistencia de la app

- **E0** Dependencias de CONVENCIONES §1 en `mobile/gradle/libs.versions.toml` (Hilt, KSP, Room, Navigation, Retrofit, Moshi, DataStore, WorkManager, Tink, tests), `UniRideApp` con `@HiltAndroidApp`, mover `ui/theme` a `presentation/theme`, plantilla de PR y workflow de GitHub Actions (build de la app y del backend). Corregir el README: prefijo de ramas `feat/` y abrir `mobile/` en Android Studio.
- **E1** Room: `UniRideDatabase`, `UserEntity`, `TripEntity`, `ReservationEntity` (campos de la preentrega 11.1, ids `String`), DAOs básicos, `TypeConverters`, `DatabaseModule`.
- **E2** `core/result` (`AppResult`, `DataError`) y `DispatchersProvider`.

### Alvaro · Red de la app

- **A0** Revisar y aprobar `docs/API.md` (hoy es un borrador) y cerrar con el grupo los puntos [A DEFINIR]. **Es lo primero, porque lo usan los cuatro.**
- **A1** `NetworkModule` (Retrofit + Moshi + OkHttp), `AuthInterceptor`, logging solo en debug, URL base en `BuildConfig`, `network_security_config` de debug para `10.0.2.2`, `safeApiCall`.
- **A2** `Fake*RemoteDataSource` con datos de ejemplo y el flag `USE_FAKE_BACKEND`, para que nadie espere al backend para hacer sus pantallas.
- **A3** `NetworkMonitor` (ConnectivityManager) + `ObserveConnectivityUseCase`, para el banner de todas las pantallas.

### Nicolás · UI

- **N1** `presentation/theme` con los tokens de la guía de alta fidelidad de Figma.
- **N2** Componentes comunes de CONVENCIONES §7.4 en `presentation/common`, con `@Preview`: primero `ContentStateHost`, `EmptyState`, `ErrorState`, `OfflineBanner`, `PrimaryButton` y `TripCard`, que son los que usan todas las pantallas.
- **N3** `strings.xml` base (errores por `DataError`, banner, estados vacíos).

**Fin de D1:** el backend levanta con Docker; la app compila, navega entre placeholders y lista viajes del fake.

---

## D2 a D4 · Funcionalidades

Cada uno hace su RF completo: **endpoints del backend**, UseCases, repositorio, ViewModel, pantallas y los tests de los criterios de aceptación. Orden sugerido dentro del RF: endpoint → datos y dominio de la app → pantalla. Las pantallas se pueden empezar contra el fake el D2 y pasarse al backend real cuando el endpoint esté mergeado.

### Alvaro · RF01 Autenticación institucional y perfil

Pantallas: A1 bienvenida, A2 login, registro, "revisá tu correo", restablecer contraseña, perfil.

- Backend: registro (solo @uade.edu.ar, cuenta `PENDIENTE`, correo con enlace), activación, login, recuperación de contraseña, `GET/PUT /users/me`. Los correos se prueban en Mailpit.
- App: `RegisterUseCase`, `LoginUseCase`, `ActivateAccountUseCase`, `RequestPasswordResetUseCase`, `LogoutUseCase`, `ObserveCurrentUserUseCase`, validaciones (`EmailValidator`, `PasswordPolicy`).
- `SessionTokenStore` con Keystore + Tink (11.3). Inicio automático si hay sesión; cierre de sesión ante 401.
- Deep links de activación y de restablecer contraseña.

### Estefanía · RF02 Publicación de viaje + sincronización

Pantallas: C1 inicio del conductor sin viaje, C2 formulario de publicación.

- Backend: `PUT /trips/{tripId}` idempotente, finalizar y cancelar, validaciones (plazas 1–4, fecha futura, aporte ≥ 0).
- App: `PublishTripUseCase` y el flujo offline completo: guardar con `isSync = false`, `SyncScheduler`, `SyncWorker` (restricción de red, backoff), marcar sincronizado, manejar el rechazo del servidor. **RF04 lo reutiliza para las reservas, así que tiene que estar mergeado a más tardar el D3.**
- Ubicación con Fused Location: solo si sobra tiempo; el campo manual alcanza.

### Nicolás · RF03 Explorar y filtrar viajes

Pantalla: B1 Explorar (feed + filtros) con estados vacío, error y sin conexión.

- Backend: `GET /trips` con filtros y `updatedSince`; `GET /campuses`. Listados sin patente ni teléfono (RNF07).
- App: `ObserveTripsUseCase(filters)`, `RefreshTripsUseCase`, filtros reactivos, últimos filtros en DataStore, *pull to refresh*, "Actualizado HH:MM", primer inicio sin datos ni conexión.
- Regla de retención del catálogo (11.2) en `TripDao.replaceCatalog`, coordinada con Estefanía.

### Gustavo · RF04 Reserva y confirmación de vacante

Pantallas: B3 Detalle del viaje, B5 Asiento confirmado, B6 Mi viaje, C4 Panel del conductor, inicio dinámico.

- Backend: detalle del viaje (patente y teléfono solo con reserva aceptada), solicitar (`PUT` idempotente), aceptar, rechazar y cancelar. Aceptar descuenta la vacante en la misma transacción con bloqueo; sin vacantes, 409 `NO_SEATS_AVAILABLE`.
- App: `RequestSeatUseCase`, `RespondToRequestUseCase`, `CancelReservationUseCase`, `ObserveMyActiveTripUseCase`, `ObserveDriverPanelUseCase`; reserva offline con el mecanismo de RF02; mensaje de "sin plazas" de 12.1; viaje confirmado legible sin conexión.

---

## D5 a D7 · Integración y entrega

| Día | Tarea | Responsable |
| --- | --- | --- |
| D5 | Cada uno prueba su RF de punta a punta contra el backend real (`USE_FAKE_BACKEND = false`) | todos |
| D5 | Prueba en un dispositivo de los escenarios sin conexión de 12.1 (modo avión, publicar y reservar offline, reconexión) | Estefanía |
| D5–D6 | Push con FCM para RF04: Firebase Admin en el backend, `UniRideMessagingService`, permiso en contexto (RNF08) | Alvaro, con Gustavo para la navegación |
| D6 | README: qué hace, cómo levantar backend y app, arquitectura, RF alcanzados, decisiones, limitaciones (7.4) | Gustavo coordina, cada uno su RF |
| D6 | Diagramas de arquitectura y topología actualizados | Estefanía |
| D6 | Revisión de accesibilidad y guion de la demo (Problema → Escenario → Acción → Respuesta → Valor) | Nicolás |
| D7 | Ensayo: cada uno explica un RF que **no** hizo. Merge `develop` → `main`, tag `v1.0`, APK | todos |

## Qué se recorta

Con una semana, esto queda fuera y se declara en "Limitaciones conocidas" del README:

1. **Calificación mutua** (alcance 7) y **bandeja de notificaciones** (alcance 8): no se implementan.
2. **Push con FCM**: se intenta en D5–D6. Es parte de los criterios de aceptación de RF04, así que si no llega hay que decirlo explícitamente en la defensa; el resto de RF04 funciona igual (el pasajero ve el estado al abrir la app).
3. **Ubicación** para sugerir el punto de partida: opcional.

Si en D4 un RF está atrasado, se recorta en este orden antes de tocar los cuatro RF.

---

## Dependencias

```
A0 contrato API ──► G1 backend base ──► endpoints de cada RF
       └────────► A2 fakes ──► pantallas de cada RF (desde D2)
E0 dependencias Android ──► todo lo de la app
G2 modelos + interfaces ──► E1 Room, A2 fakes, todos los RF
N2 componentes ──► pantallas de todos los RF
RF02 mecanismo de sync (D3) ──► reserva offline de RF04
RF01 login y JWT ──► probar los endpoints autenticados de punta a punta
```

Hasta que RF01 esté listo, `data.sql` trae usuarios de prueba ya activados para pedir un token desde Swagger. Si alguien queda bloqueado, avisa en el grupo **el mismo día**.

---

## Archivos compartidos

| Archivo | Dueño | Regla |
| --- | --- | --- |
| `mobile/gradle/libs.versions.toml`, `mobile/app/build.gradle.kts` | Estefanía | PR propio de tipo `chore(build)`, no mezclado con una funcionalidad |
| `UniRideDatabase.kt` (entities, versión) | Estefanía | Un cambio de esquema sube la versión; avisar en el grupo |
| `docs/API.md` | Alvaro | Cambiar el contrato requiere aprobación de Alvaro, en el mismo PR que el endpoint |
| `backend/pom.xml`, `backend/.../config/**`, `security/**`, `exception/**`, `docker-compose.yml` | Gustavo | Seguridad, errores y configuración los revisa Gustavo |
| `backend/src/main/resources/data.sql` | todos | Agregar al final de la sección de tu RF; renombrar una columna en una entidad se avisa (CONVENCIONES §10.5) |
| `UniRideNavHost.kt` | Gustavo | Cada área agrega su ruta en su propio subgrafo, no en el NavHost |
| `presentation/theme`, `presentation/common` | Nicolás | Un componente nuevo compartido lo revisa Nicolás |
| `domain/model`, `domain/repository` | Gustavo | Cambiar una interfaz la revisan Gustavo y el dueño de la implementación |
| `strings.xml` | todos | Agregar al final de la sección de tu pantalla, con prefijo `pantalla_` |

---

## Ritmo de trabajo

- **Mensaje diario en el grupo** (o 10 minutos de llamada): qué terminé, qué sigo, en qué estoy bloqueado.
- Commits todos los días y PRs chicos, revisados en el día. Un PR que espera revisión más de medio día frena a alguien.
- Cada uno revisa al menos un PR de otro RF: es la mejor preparación para la defensa.
