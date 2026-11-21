# UniRide · Reglas del proyecto para revisión con IA

App Android de viajes compartidos para estudiantes de UADE (Kotlin, Jetpack Compose, MVVM + Clean Architecture, Offline First) con un backend REST en Spring Boot. Es un monorepo: la app Android está en `frontend/` y el backend en `backend/`. Las reglas completas están en `docs/CONVENCIONES.md` y la división del trabajo en `docs/DIVISION_TRABAJO.md`. El contrato de la API está en `docs/API.md` (borrador hasta que se apruebe en la tarea A0). Al revisar un cambio, marcar como problema todo lo que viole estas reglas:

## App: capas

- `domain/` es Kotlin puro: ningún import de `android.*`, `androidx.*`, Room, Retrofit, Moshi ni Hilt (salvo `javax.inject.Inject`).
- Un ViewModel no importa nada de `data/` (DAO, Entity, Api, Dto). Habla solo con UseCases.
- Entities y Dtos no salen de `data/`; los repositorios devuelven modelos de dominio.
- Flujo: UI → ViewModel → UseCase → Repository (interfaz) → RepositoryImpl → DAO / RemoteDataSource.

## App: datos y Offline First

- Room es la fuente única de verdad: la UI observa `Flow` del DAO, nunca la respuesta de la API.
- Toda escritura del usuario: id UUID generado en el cliente, guardar con `isSync = false`, `SyncScheduler.schedule()`. No se llama a la API para escribir desde otro lado.
- Los repositorios no lanzan excepciones: devuelven `AppResult` con `DataError`. Nunca se atrapa `CancellationException` sin relanzarla.
- El token solo vive en `SessionTokenStore` (Keystore + Tink). Nunca en Room, logs ni DataStore sin cifrar.

## App: presentación

- Un `data class XxxUiState` por pantalla expuesto como `StateFlow`; entrada única `onAction(XxxAction)`; efectos de una vez por `Channel`.
- `XxxRoute` obtiene el ViewModel; `XxxScreen(state, onAction, modifier)` no tiene estado y tiene `@Preview`.
- Cada pantalla resuelve cargando, contenido, vacío, error y sin conexión.
- Sin textos hardcodeados (todo en `strings.xml`), sin colores fuera de `MaterialTheme`, íconos accionables con `contentDescription`, objetivos táctiles de 48 dp.
- Navegación con rutas `@Serializable` que solo llevan ids. El ViewModel no recibe `Context` ni `NavController`.
- Sin `!!`, sin `GlobalScope`, sin `runBlocking` fuera de tests, sin `Dispatchers.IO` hardcodeado (se inyecta `DispatchersProvider`).

## Backend

- El Controller solo valida y delega; las reglas de negocio y las transacciones están en el Service.
- Nunca se devuelve una entidad JPA: siempre un `record` `*Response`. Inyección por constructor.
- Errores con el formato `{ code, message }` desde `GlobalExceptionHandler`, con los códigos HTTP de CONVENCIONES §10.4.
- Patente y teléfono solo en la respuesta si quien consulta comparte una reserva `ACEPTADA` (RNF07). Aceptar una reserva descuenta la vacante en la misma transacción, con bloqueo.
- Crear viajes y reservas es `PUT` idempotente con el UUID del cliente.
- Las tablas las crea Hibernate desde las entidades (`ddl-auto=update`); los datos iniciales van en `data.sql` y se pueden ejecutar más de una vez. Sin secretos en el repo (`.env` no se commitea).
- Un PR toca `frontend/` o `backend/`, idealmente no los dos.
- Si cambia el contrato, `docs/API.md` se actualiza en el mismo PR.

## General

- Código en inglés, UI en español. Enums con los valores del modelo de la preentrega (`IDA`, `VUELTA`, `ABIERTO`, `PENDIENTE`…), iguales en app, API y base.
- Lógica nueva con tests.
- Commits en Conventional Commits; ramas `feat/`, `fix/`, `refactor/`, `test/`, `docs/`, `chore/` desde `develop`.
