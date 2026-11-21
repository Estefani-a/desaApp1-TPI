# UniRide · Convenciones de código y arquitectura

Este documento fija cómo escribimos el código de UniRide, tanto la app Android como el backend. Si algo no está acá, se decide en el grupo y se agrega en un PR a este archivo. Si un PR no cumple una regla, se corrige en el PR o se discute el cambio de la regla, pero no se mergea "por esta vez".

La base es lo que comprometimos en la preentrega (MVVM + Clean Architecture, Room como fuente única de verdad, Offline First con WorkManager, una única API REST con token Bearer), la estructura inicial que ya está en el repo y el estilo de los proyectos de la cátedra (`PokedexAcademy-MVVM-Clean`, `CleanAndMorty`).

---

## 1. Stack y versiones

### App Android

| Tema | Decisión |
| --- | --- |
| Lenguaje | Kotlin 2.2, con el estilo oficial de Kotlin de Android Studio (Settings → Code Style → Kotlin → "Set from… Kotlin style guide") |
| UI | Jetpack Compose + Material 3 |
| SDK | `minSdk 30`, `targetSdk`/`compileSdk 37` (lo que ya tiene el proyecto) |
| Arquitectura | MVVM + Clean Architecture, un solo módulo `app` separado por paquetes |
| DI | Hilt (con KSP, no kapt) |
| Asincronía | Coroutines + Flow / StateFlow |
| Persistencia | Room (datos), DataStore Preferences (preferencias), Keystore + Tink sobre DataStore (token) |
| Red | Retrofit + Moshi (codegen con KSP) + OkHttp (interceptor de auth y logging solo en debug) |
| Segundo plano | WorkManager (sincronización de pendientes) |
| Push | Firebase Cloud Messaging |
| Ubicación | Fused Location Provider, una sola lectura |
| Navegación | Navigation Compose con rutas tipadas (`@Serializable`) |

### Backend

| Tema | Decisión |
| --- | --- |
| Framework | Spring Boot 3.5, Java 21, Maven |
| Base de datos | PostgreSQL 16. Las tablas las crea Hibernate a partir de las entidades JPA (§10.5) |
| Seguridad | Spring Security con JWT (token Bearer firmado con HMAC) |
| Correo | Spring Mail (SMTP). En desarrollo, Mailpit captura los correos |
| Push | Firebase Admin SDK para Java |
| Documentación | springdoc-openapi (Swagger UI en `/swagger-ui.html`) |
| Entorno local | Un solo `docker compose up --build` levanta PostgreSQL, Mailpit y el backend juntos (§10.6) |

¿Por qué Spring Boot y no Gin (Go)? El equipo ya trabaja con Spring Boot 3.5 y JWT en Desarrollo de Aplicaciones II, el Firebase Admin SDK para Java es el oficial más completo y Spring trae resuelto el envío de correo, la validación y la seguridad. Gin es más liviano, pero tendríamos que aprender Go y armar a mano varias de esas piezas.

En la app, todas las dependencias van en `mobile/gradle/libs.versions.toml`; en el backend, en el `pom.xml` con la versión administrada por el parent de Spring Boot cuando exista. Una dependencia nueva que no esté en estas tablas se discute antes en el grupo: tenemos que poder justificarla en la defensa.

---

## 2. Estructura del repositorio

Monorepo con la app móvil y el backend separados en dos carpetas, cada una con su propio proyecto:

```
desaApp1-TPI/
├── mobile/                 // proyecto Android (Gradle). En Android Studio se abre ESTA carpeta, no la raíz
│   ├── app/
│   ├── gradle/libs.versions.toml
│   └── settings.gradle.kts
├── backend/                // proyecto Spring Boot (Maven). En IntelliJ se abre esta carpeta
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
├── docs/                   // CONVENCIONES.md, DIVISION_TRABAJO.md, API.md, diagramas
├── docker-compose.yml      // levanta PostgreSQL + Mailpit + backend con un solo comando
├── .env.example            // variables del docker compose, sin valores reales
├── CLAUDE.md               // resumen de reglas para la revisión con IA
└── README.md
```

- Un PR toca `mobile/` o `backend/`, idealmente no los dos. Si un RF necesita los dos, primero va el PR del backend (con `docs/API.md` actualizado) y después el de la app.
- Cada carpeta tiene su propio `.gitignore`.

## 3. Estructura de paquetes de la app

Todo esto vive en `mobile/app/src/main/java/`. Paquete raíz: `com.example.uniride` (el que ya tiene el proyecto). Primero la capa, después la funcionalidad. Las carpetas marcadas con `*` ya existen en el repo; el resto se agrega cuando haga falta.

```
com.example.uniride
├── UniRideApp.kt                 // @HiltAndroidApp
├── MainActivity.kt               // solo setContent { UniRideTheme { UniRideNavHost() } }
│
├── core/                         // lo que usan todas las capas, sin lógica de negocio
│   ├── result/                   // AppResult, DataError
│   └── dispatcher/               // DispatchersProvider (para tests)
│
├── domain/ *                     // Kotlin puro: CERO imports de android.* / androidx.* / retrofit / room
│   ├── model/ *                  // User, Trip, Reservation, Campus, TripFilters, enums
│   ├── repository/ *             // interfaces: AuthRepository, TripRepository, ReservationRepository…
│   ├── usecase/ *
│   │   ├── auth/                 // RegisterUseCase, LoginUseCase, RequestPasswordResetUseCase…
│   │   ├── trip/                 // PublishTripUseCase, ObserveTripsUseCase, RefreshTripsUseCase…
│   │   └── reservation/          // RequestSeatUseCase, RespondToRequestUseCase…
│   └── validation/               // reglas puras: EmailValidator (@uade.edu.ar), PasswordPolicy, SeatsRule
│
├── data/ *
│   ├── datastore/ *              // UserPreferencesDataStore, SessionTokenStore (Tink)
│   ├── local/ *
│   │   ├── database/ *           // UniRideDatabase, TypeConverters
│   │   ├── entity/ *             // UserEntity, TripEntity, ReservationEntity
│   │   └── dao/ *                // UserDao, TripDao, ReservationDao
│   ├── remote/ *
│   │   ├── api/ *                // AuthApi, TripApi, ReservationApi (interfaces Retrofit)
│   │   ├── dto/ *                // *Dto / *Request
│   │   ├── interceptor/          // AuthInterceptor
│   │   └── datasource/           // TripRemoteDataSource (+ Fake* para trabajar sin backend)
│   ├── mapper/                   // TripMapper.kt: Dto ↔ Entity ↔ Domain
│   ├── repository/ *             // *RepositoryImpl
│   ├── sync/                     // SyncWorker, SyncScheduler
│   ├── connectivity/             // NetworkMonitor (ConnectivityManager)
│   ├── location/                 // LocationProvider (Fused)
│   └── push/                     // UniRideMessagingService (FCM)
│
├── di/                           // un módulo Hilt por área: DatabaseModule, NetworkModule, AuthModule, TripModule…
│
└── presentation/ *
    ├── theme/                    // Color, Type, Theme. Hoy está en ui/theme: se mueve acá en la tarea E0
    ├── common/ *                 // componentes reutilizables (ver §7.4)
    ├── navigation/ *             // UniRideNavHost, rutas, barra inferior
    ├── auth/ *                   // login/, register/, resetpassword/ (RF01)
    ├── home/                     // inicio dinámico según modo y estado
    ├── feed/ *                   // listado y filtros (RF03)
    ├── detail/ *                 // detalle y "Solicitar asiento" (RF04)
    ├── publish/ *                // formulario de publicación (RF02)
    ├── reservations/ *           // driverpanel/ (panel del conductor) y mytrip/ (viaje del pasajero) (RF04)
    └── profile/                  // perfil y cierre de sesión (RF01)
```

Cada pantalla vive en su carpeta con estos archivos:

```
presentation/feed/
├── FeedScreen.kt        // FeedRoute (con ViewModel) + FeedScreen (sin estado) + Previews
├── FeedViewModel.kt
├── FeedUiState.kt       // UiState + Action + Effect
└── components/          // composables que solo usa esta pantalla
```

---

## 4. Regla de dependencias entre capas

```
presentation  ──►  domain  ◄──  data
      │                           ▲
      └───────── di (Hilt) ───────┘
```

- `domain` no depende de nadie. No importa Android, Room, Retrofit, Moshi ni Hilt (solo `javax.inject.Inject`, que es Java estándar).
- `presentation` solo conoce `domain`. **Un ViewModel nunca importa nada de `data`**: ni un DAO, ni una Entity, ni una API, ni un Dto.
- `data` implementa las interfaces de `domain/repository`. Las Entities y los Dtos no salen de `data`: el repositorio devuelve modelos de dominio.
- Un Composable no conoce el ViewModel salvo en la función `*Route` (ver §7.3).

Flujo de una acción: **UI → ViewModel → UseCase → Repository (interfaz) → RepositoryImpl → DAO / RemoteDataSource**. Los datos vuelven por `Flow` desde Room hasta la UI (flujo unidireccional, como dice la preentrega en 10.2).

### ¿Cuándo hace falta un UseCase?

Siempre que haya una regla de negocio o se combine más de un repositorio. Para una lectura directa sin reglas (por ejemplo, observar el perfil) igual creamos el UseCase, para que todos los ViewModels se vean iguales y la defensa sea más fácil de explicar. Un UseCase:

- tiene una sola responsabilidad y se nombra verbo + sustantivo: `PublishTripUseCase`;
- expone `suspend operator fun invoke(...)` (acciones) o `operator fun invoke(...): Flow<...>` (observaciones);
- valida las reglas de dominio antes de llamar al repositorio (dominio @uade.edu.ar, plazas de 1 a 4, fecha futura…).

---

## 5. Nombres

El **código va en inglés** (clases, funciones, variables). Los **textos de la UI van en español** en `res/values/strings.xml`. Los **valores de los enums** son los que documentamos en el modelo de datos de la preentrega, para que coincidan con lo que defendemos, y son los mismos en la app, en la API y en la base del backend.

| Elemento | Convención | Ejemplo |
| --- | --- | --- |
| Modelo de dominio | Sustantivo, sin sufijo | `Trip`, `Reservation` |
| Entity de Room | `*Entity` | `TripEntity` |
| DAO | `*Dao` | `TripDao` |
| API de Retrofit | `*Api` | `TripApi` |
| Dto de respuesta | `*Dto` | `TripDto` |
| Cuerpo de request | `*Request` | `PublishTripRequest` |
| Interfaz de repositorio | `*Repository` (en domain) | `TripRepository` |
| Implementación | `*RepositoryImpl` (en data) | `TripRepositoryImpl` |
| Fuente remota | `*RemoteDataSource` | `TripRemoteDataSource` |
| Fake para tests / sin backend | `Fake*` | `FakeTripRemoteDataSource` |
| UseCase | Verbo + sustantivo + `UseCase` | `RequestSeatUseCase` |
| ViewModel | Pantalla + `ViewModel` | `FeedViewModel` |
| Estado de pantalla | Pantalla + `UiState` | `FeedUiState` |
| Acciones del usuario | Pantalla + `Action` | `FeedAction.ChangeFilters` |
| Efectos de una vez | Pantalla + `Effect` | `FeedEffect.ShowMessage` |
| Composable con ViewModel | Pantalla + `Route` | `FeedRoute` |
| Composable sin estado | Pantalla + `Screen` | `FeedScreen` |
| Ruta de navegación | Sustantivo + `Route` (objeto/clase `@Serializable`) | `TripDetailRoute(tripId)` |
| Worker | `*Worker` | `SyncWorker` |
| Módulo Hilt | Área + `Module` | `TripModule` |
| String resource | `pantalla_elemento_descripcion` en snake_case | `feed_empty_title` |
| Funciones de mapeo | `toDomain()`, `toEntity()`, `toDto()` como extensiones | `fun TripDto.toEntity(): TripEntity` |

### Glosario (para no tener tres nombres para lo mismo)

| Español (UI y documento) | Código y API |
| --- | --- |
| Viaje / trayecto / oferta | `Trip` |
| Reserva / solicitud / postulación | `Reservation` |
| Sede | `Campus` |
| Sentido (ida a la facultad / vuelta) | `Direction { IDA, VUELTA }` |
| Punto de encuentro | `meetingPoint` |
| Barrio de destino | `destinationNeighborhood` |
| Aporte por asiento | `contributionPerSeat` |
| Modo habitual | `PreferredMode { PASAJERO, CONDUCTOR, AMBOS }` |
| Estado del viaje | `TripStatus { ABIERTO, COMPLETO, FINALIZADO, CANCELADO }` |
| Estado de la reserva | `ReservationStatus { PENDIENTE, ACEPTADA, RECHAZADA, CANCELADA }` |
| Conductor / pasajero | `driver` / `passenger` |

---

## 6. Capa de datos (app)

### 6.1 Room es la fuente única de verdad

- La UI **siempre** lee de Room, nunca directamente de la API. El repositorio expone `Flow` que vienen del DAO.
- La API solo se usa para **refrescar** Room (`suspend fun refresh...()`) y para **enviar** pendientes.
- Las consultas de un DAO que la UI observa devuelven `Flow<...>`. Las escrituras son `suspend`.
- Operaciones de varios pasos sobre la base (por ejemplo, reemplazar el catálogo respetando la regla de retención de 11.2) van en un método `@Transaction` del DAO.

```kotlin
class TripRepositoryImpl @Inject constructor(
    private val tripDao: TripDao,
    private val remote: TripRemoteDataSource,
    private val syncScheduler: SyncScheduler,
) : TripRepository {

    override fun observeTrips(filters: TripFilters): Flow<List<Trip>> =
        tripDao.observeOpenTrips(filters.campus, filters.direction, filters.date)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun refreshTrips(): AppResult<Unit> =
        safeApiCall { remote.getTrips() }
            .onSuccess { dtos -> tripDao.replaceCatalog(dtos.map { it.toEntity() }) }
            .map { }
}
```

### 6.2 Escrituras offline (isSync)

Toda acción del usuario que crea o modifica algo en el servidor sigue los mismos pasos:

1. Se genera el id en el cliente (`UUID.randomUUID().toString()`), así el registro tiene un id estable aunque se cree sin conexión. **Los ids de `Trip` y `Reservation` son `String` UUID**, y el backend acepta el id que manda el cliente (§10.3).
2. Se guarda en Room con `isSync = false`. La UI lo muestra de inmediato (como "Pendiente de sincronizar").
3. Se llama a `syncScheduler.schedule()`, que encola un trabajo único de WorkManager (`enqueueUniqueWork("sync", ExistingWorkPolicy.KEEP, ...)`) con la restricción de red y backoff exponencial.
4. El `SyncWorker` lee los pendientes, los envía con `PUT` (idempotente) y marca `isSync = true`. Si el servidor rechaza (por ejemplo, 409 sin vacantes), actualiza la fila local con el estado real y deja un mensaje para la UI.

Nadie llama a la API para escribir desde fuera de este mecanismo.

### 6.3 Errores

Los repositorios **no lanzan excepciones**: devuelven `AppResult`.

```kotlin
// core/result/AppResult.kt
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: DataError) : AppResult<Nothing>
}

enum class DataError {
    NO_CONNECTION,     // IOException / UnknownHostException
    TIMEOUT,
    UNAUTHORIZED,      // 401 → se cierra la sesión (11.3)
    NOT_FOUND,         // 404
    CONFLICT,          // 409 (por ejemplo, sin vacantes)
    VALIDATION,        // 400/422
    SERVER,            // 5xx
    UNKNOWN,
}
```

- La traducción de excepción/código HTTP a `DataError` está en un solo lugar: `safeApiCall { }` en `data/remote`. Cuando importa el motivo exacto, se lee el `code` del cuerpo de error del backend (§10.4).
- Nunca se atrapa `CancellationException` (se relanza), porque rompe la cancelación de coroutines.
- El ViewModel convierte `DataError` en un texto de `strings.xml`. El dominio y los datos no tienen textos para el usuario.

### 6.4 Sesión y datos sensibles

- El token va **solo** en `SessionTokenStore` (Keystore + Tink + DataStore). Nunca en Room, en DataStore sin cifrar ni en logs.
- `AuthInterceptor` agrega `Authorization: Bearer <token>`. Ante un 401, se borra el token y Room (`clearAllTables()`) y la app vuelve al login.
- El `HttpLoggingInterceptor` se agrega solo en `BuildConfig.DEBUG` y con el header `Authorization` redactado.
- La patente y el teléfono solo existen en la respuesta de la API cuando hay una reserva aceptada (RNF07, lo garantiza el backend). La app no intenta "ocultarlos": si llegan, se pueden mostrar.
- La URL base de la API va en `BuildConfig` (`buildConfigField`), no hardcodeada. En el emulador, el backend local es `http://10.0.2.2:8080/`. HTTP sin TLS se permite solo en debug, con un `network_security_config` del build de debug.

---

## 7. Capa de presentación (app)

### 7.1 Estado de pantalla

Cada pantalla tiene **un** `data class XxxUiState` inmutable. El contenido principal usa un `ContentState` común y, aparte, los indicadores que pueden convivir con él (sin conexión, refrescando, última actualización). Así una pantalla puede mostrar datos guardados y el banner de "Sin conexión" al mismo tiempo, como dice 12.1.

```kotlin
// presentation/common/ContentState.kt
sealed interface ContentState<out T> {
    data object Loading : ContentState<Nothing>
    data class Success<T>(val data: T) : ContentState<T>
    data object Empty : ContentState<Nothing>
    data class Error(@StringRes val messageRes: Int, val canRetry: Boolean = true) : ContentState<Nothing>
}

// presentation/feed/FeedUiState.kt
data class FeedUiState(
    val content: ContentState<List<Trip>> = ContentState.Loading,
    val filters: TripFilters = TripFilters(),
    val isOffline: Boolean = false,
    val isRefreshing: Boolean = false,
    val lastUpdatedAt: LocalTime? = null,
)

sealed interface FeedAction {
    data class ChangeFilters(val filters: TripFilters) : FeedAction
    data object Refresh : FeedAction
    data class OpenTrip(val tripId: String) : FeedAction
}

sealed interface FeedEffect {
    data class NavigateToDetail(val tripId: String) : FeedEffect
    data class ShowMessage(@StringRes val messageRes: Int) : FeedEffect
}
```

Formularios: el estado tiene los valores de los campos y un error por campo (`emailError: Int? = null`), más `isSubmitting`.

### 7.2 ViewModel

```kotlin
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val observeTrips: ObserveTripsUseCase,
    private val refreshTrips: RefreshTripsUseCase,
    private val observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _effects = Channel<FeedEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onAction(action: FeedAction) {
        when (action) {
            is FeedAction.ChangeFilters -> ...
            FeedAction.Refresh -> refresh()
            is FeedAction.OpenTrip -> viewModelScope.launch {
                _effects.send(FeedEffect.NavigateToDetail(action.tripId))
            }
        }
    }
}
```

Reglas:

- Se expone **un** `StateFlow<XxxUiState>` y, si hace falta, un `Flow<XxxEffect>`. Nunca `MutableStateFlow` público.
- **Una** función pública de entrada: `onAction(action)`. Así la UI no conoce los detalles del ViewModel.
- Se actualiza con `_uiState.update { it.copy(...) }`.
- Los efectos de una sola vez (navegar, Snackbar) van por `Channel` / `effects`, no dentro del estado, para que no se repitan al rotar.
- Un ViewModel no recibe `Context`, `NavController` ni nada de Compose. Si necesita un argumento de navegación, lo lee de `SavedStateHandle` (`savedStateHandle.toRoute<TripDetailRoute>()`).
- Para combinar flujos: `combine(...).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`.

### 7.3 Composables

- Cada pantalla tiene dos funciones:
  - `XxxRoute(onNavigate..., viewModel: XxxViewModel = hiltViewModel())`: obtiene el ViewModel, junta el estado con `collectAsStateWithLifecycle()`, escucha `effects` en un `LaunchedEffect` y llama a la navegación.
  - `XxxScreen(state: XxxUiState, onAction: (XxxAction) -> Unit, modifier: Modifier = Modifier)`: **sin estado**, solo dibuja. Es la que tiene `@Preview`, y por eso se puede revisar sin backend.
- Todo composable reutilizable recibe `modifier: Modifier = Modifier` como primer parámetro opcional y lo aplica al elemento raíz.
- Sin lógica de negocio en composables: nada de filtrar listas, validar emails ni calcular vacantes. Eso viene calculado en el estado.
- Cada pantalla resuelve los cuatro estados de `ContentState` con el componente común `ContentStateHost` y muestra `OfflineBanner` cuando `isOffline`.
- Una `@Preview` por estado relevante (cargando, con datos, vacío, error, sin conexión).

### 7.4 Componentes comunes (`presentation/common`)

Los hace Nicolás en los primeros días y todos los usan. Antes de crear un componente nuevo, revisar si ya existe:

`ContentStateHost`, `LoadingState`, `EmptyState(title, message, actionLabel, onAction)`, `ErrorState(message, onRetry)`, `OfflineBanner(lastUpdatedAt)`, `PrimaryButton` (al pie, 48 dp mínimo), `UniRideTopBar`, `TripCard`, `StatusChip`, `LabeledTextField`, `SelectorField` (sede, turno, sentido).

### 7.5 Accesibilidad y diseño (RNF01)

- Colores, tipografías y formas solo desde `MaterialTheme` (tokens de la guía de alta fidelidad). Nada de `Color(0xFF...)` en una pantalla.
- Objetivos táctiles de al menos 48 × 48 dp. Acción principal en la mitad inferior.
- Todo ícono accionable tiene `contentDescription` con un string resource. Los decorativos, `contentDescription = null`.
- Ningún texto visible queda hardcodeado: todo va en `strings.xml`.
- Contraste mínimo 4,5:1.

### 7.6 Navegación

- Rutas tipadas con `@Serializable` (Navigation Compose 2.8+). **Solo se pasan ids** (`TripDetailRoute(tripId: String)`), nunca objetos: la pantalla destino lee el resto de Room.
- Cada área registra su propio subgrafo como extensión: `fun NavGraphBuilder.authGraph(navController: NavController)`. `UniRideNavHost` solo llama a esos subgrafos. Así no nos pisamos todos en el mismo archivo.
- Los deep links (activación de cuenta, restablecer contraseña, tocar una push) se declaran en la ruta correspondiente y se documentan en `docs/API.md`.
- Los códigos de pantalla de Figma (A2 Login, B1 Explorar, B3 Detalle, C4 Panel del conductor, B5 Asiento confirmado, B6 Mi viaje…) se mencionan en el KDoc de cada `*Screen` para poder rastrear diseño ↔ código.

### 7.7 Permisos (RNF08)

- Se piden en contexto, con una explicación previa, nunca al abrir la app: notificaciones al solicitar el primer asiento o publicar el primer viaje; ubicación al tocar "Usar mi ubicación" en el formulario de publicación.
- Si el usuario rechaza, la app sigue funcionando (campo manual, bandeja en lugar de push) y la pantalla lo comunica.

---

## 8. Coroutines e inyección de dependencias (app)

- Room y Retrofit con `suspend` ya cambian de hilo solos. No envolver sus llamadas en `withContext(Dispatchers.IO)`.
- Si hace falta un dispatcher (trabajo pesado propio), se inyecta `DispatchersProvider`; no se escribe `Dispatchers.IO` en la clase, para poder testearla.
- Prohibido `GlobalScope` y `runBlocking` fuera de tests. En un ViewModel se usa `viewModelScope`.
- Las funciones de un repositorio son `suspend` o devuelven `Flow`, nunca callbacks.
- Hilt: las interfaces se vinculan con `@Binds` en un módulo abstracto; lo que se construye (Room, Retrofit, OkHttp, DataStore) con `@Provides`. Un módulo por área (`DatabaseModule`, `NetworkModule`, `AuthModule`, `TripModule`, `ReservationModule`, `SyncModule`).
- Las fuentes remotas tienen una implementación real y una `Fake*`. Un flag en `BuildConfig` (`USE_FAKE_BACKEND`) elige cuál se inyecta, para poder trabajar y hacer demos sin servidor.
- `SyncWorker` usa `@HiltWorker` + `HiltWorkerFactory`.

---

## 9. Tests de la app

Mínimo exigido por PR que agrega lógica:

| Qué | Cómo | Dónde |
| --- | --- | --- |
| UseCases y validaciones | JUnit + repositorios `Fake*` | `src/test` |
| ViewModels | `kotlinx-coroutines-test` (`runTest`, `MainDispatcherRule`) + Turbine para `StateFlow` | `src/test` |
| Mappers | JUnit, ida y vuelta Dto → Entity → Domain | `src/test` |
| DAOs | Room en memoria (`Room.inMemoryDatabaseBuilder`) | `src/androidTest` |

- Nombre de los tests con backticks y en español, describiendo el caso: ``fun `publicar sin conexión deja el viaje con isSync false`()``.
- Los fakes viven en `src/test/.../fake/` y se comparten. No se usan librerías de mocks salvo que el grupo lo decida.
- Los criterios de aceptación de cada RF (sección 5 de la preentrega) tienen al menos un test que los verifique.

---

## 10. Backend

### 10.1 Paquetes

Paquete raíz `com.example.uniride.backend`, organizado **por funcionalidad** y, adentro, por responsabilidad:

```
backend/src/main/java/com/example/uniride/backend/
├── UniRideBackendApplication.java
├── common/
│   ├── config/          // SecurityConfig, FirebaseConfig, OpenApiConfig
│   ├── security/        // JwtService, JwtAuthenticationFilter, CurrentUser
│   └── error/           // ApiError, ErrorCode, GlobalExceptionHandler, excepciones de negocio
├── auth/                // registro, activación, login, recuperación de contraseña
├── user/                // perfil, vehículo, token FCM
├── campus/              // sedes y puntos de encuentro
├── trip/                // publicar, listar, finalizar, cancelar
├── reservation/         // solicitar, aceptar/rechazar, cancelar
└── notification/        // envío por FCM y bandeja
```

Cada funcionalidad tiene `XxxController`, `XxxService`, `XxxRepository` (Spring Data JPA), `Xxx` (entidad JPA) y `dto/` con `XxxResponse` / `XxxRequest`.

### 10.2 Reglas de capas

- El **Controller** solo recibe, valida (`@Valid`) y delega. Nada de lógica de negocio ni acceso a repositorios.
- El **Service** tiene las reglas de negocio y las transacciones (`@Transactional`). Es lo que se testea.
- Las entidades JPA **nunca** se devuelven en un controller: siempre se mapean a un `*Response`. Los DTOs son `record` de Java.
- Inyección por constructor (campos `private final`). Sin `@Autowired` en campos.
- Reglas que exige la preentrega y que viven en el Service, no en la app:
  - registro solo con correo `@uade.edu.ar` y cuenta `PENDIENTE` hasta activar (RF01);
  - el mensaje de recuperación de contraseña no revela si el correo existe (RF01);
  - patente y teléfono solo en la respuesta si quien consulta comparte una reserva `ACEPTADA` con ese usuario (RNF07);
  - aceptar una reserva descuenta una vacante en la misma transacción, con bloqueo de la fila del viaje para que dos aceptaciones simultáneas no dejen vacantes negativas (RF04).

### 10.3 API REST

- Prefijo `/api/v1`. Recursos en plural y en inglés: `/api/v1/trips`, `/api/v1/trips/{tripId}/reservations`.
- JSON en camelCase, con los mismos nombres de campo que los Dtos de la app. Fechas en ISO-8601 (`2026-11-20`, `22:15`, `2026-11-20T22:15:00-03:00`); zona `America/Argentina/Buenos_Aires`.
- Los ids de viajes y reservas son UUID **generados por el cliente**. Crear es `PUT /api/v1/trips/{tripId}` y es **idempotente**: si WorkManager reintenta, el segundo envío no duplica nada y devuelve el mismo resultado.
- `GET /api/v1/trips` acepta los filtros de RF03 como query params (`campus`, `direction`, `date`, `shift`) y `updatedSince` para refrescos parciales.
- Autenticación: `Authorization: Bearer <jwt>` en todo salvo `/api/v1/auth/**`, `/api/v1/campuses` y `/api/v1/links/**` (enlaces de los correos). El JWT lleva el id del usuario en `sub`.
- El contrato se escribe primero en `docs/API.md` y se acuerda antes de programar. Swagger (`/swagger-ui.html`) sirve para probar, pero el que manda es `docs/API.md`.

### 10.4 Errores

Todas las respuestas de error tienen el mismo cuerpo, armado en `GlobalExceptionHandler`:

```json
{ "code": "NO_SEATS_AVAILABLE", "message": "El viaje ya no tiene plazas disponibles." }
```

| HTTP | Cuándo | `DataError` en la app |
| --- | --- | --- |
| 400 | Validación de campos (`code: VALIDATION_ERROR`, más `fields`) | `VALIDATION` |
| 401 | Sin token, token vencido o cuenta no activada | `UNAUTHORIZED` |
| 403 | El recurso no es del usuario (por ejemplo, aceptar una reserva de un viaje ajeno) | `UNKNOWN` |
| 404 | No existe | `NOT_FOUND` |
| 409 | Conflicto de negocio (`NO_SEATS_AVAILABLE`, `EMAIL_ALREADY_REGISTERED`, `TRIP_NOT_OPEN`) | `CONFLICT` |
| 500 | Error inesperado; nunca se devuelve el stack trace | `SERVER` |

Los `code` son un enum `ErrorCode` en el backend y se listan en `docs/API.md`.

### 10.5 Base de datos

- Las tablas las crea Hibernate a partir de las entidades JPA (`spring.jpa.hibernate.ddl-auto=update`): agregar una tabla o una columna es agregarla a la entidad. No usamos herramientas de migraciones (como Flyway), porque con una semana de desarrollo y sin datos reales que cuidar no se justifican.
- `update` agrega tablas y columnas, pero **no borra ni renombra**. Si cambiás un nombre o un tipo, avisá en el grupo y que cada uno reinicie su base con `docker compose down -v` (borra el volumen de PostgreSQL).
- Los datos iniciales (sedes y puntos de encuentro, usuarios de prueba para la demo) van en `src/main/resources/data.sql`, con `INSERT ... ON CONFLICT DO NOTHING` para que se pueda ejecutar en cada arranque (`spring.sql.init.mode=always` y `spring.jpa.defer-datasource-initialization=true`).
- Tablas y columnas en snake_case (`trips`, `departure_date`). Los enums se guardan como texto (`@Enumerated(EnumType.STRING)`).

### 10.6 Configuración y secretos

- Configuración en `application.yml` con variables de entorno: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `MAIL_HOST`, `MAIL_PORT`, `APP_LINK_BASE_URL`, `FIREBASE_CREDENTIALS_PATH`.
- `.env.example` (en la raíz) documenta todas, sin valores reales. Cada uno lo copia a `.env`. `.env`, la cuenta de servicio de Firebase y cualquier secreto **nunca** se commitean.
- Sin credenciales de Firebase el backend arranca igual y registra en el log las push que hubiera enviado. Así nadie depende de tener la cuenta de servicio para trabajar.

### 10.7 Levantar todo con Docker

`docker-compose.yml` está en la raíz y levanta los tres servicios juntos. No hace falta tener Java, Maven ni PostgreSQL instalados: solo Docker.

```bash
cp .env.example .env
docker compose up --build
```

| Servicio | Qué es | Puerto |
| --- | --- | --- |
| `db` | PostgreSQL 16, con los datos en un volumen (`pgdata`) para que no se pierdan al reiniciar | 5432 |
| `mailpit` | Recibe los correos que manda el backend y los muestra en una web, sin enviarlos de verdad | 8025 (web), 1025 (SMTP) |
| `backend` | La API, construida desde `backend/Dockerfile` | 8080 |

- `backend/Dockerfile` es de dos etapas: una imagen de Maven compila el `.jar` y una imagen liviana de Java 21 lo ejecuta. Así el build no depende de lo que tenga instalado cada uno.
- `backend` arranca recién cuando `db` está lista (`depends_on` con `condition: service_healthy` y un `healthcheck` con `pg_isready`).
- Dentro de Docker, el backend se conecta a `db:5432` y a `mailpit:1025` (los nombres de los servicios), no a `localhost`.
- La app en el emulador llega al backend por `http://10.0.2.2:8080`; los correos se ven en `http://localhost:8025`.
- Para programar el backend con recarga rápida se puede levantar solo la base y el correo (`docker compose up db mailpit`) y correr el backend desde IntelliJ con el perfil `local`.
- `docker compose down` apaga todo; `docker compose down -v` además borra la base.

### 10.8 Tests del backend

| Qué | Cómo |
| --- | --- |
| Services | JUnit 5 + Mockito |
| Controllers | `@WebMvcTest` (validación, códigos HTTP, formato de error) |

Con una semana, el mínimo es un test por regla de negocio de §10.2 (dominio del correo, vacantes al aceptar, privacidad de patente y teléfono, idempotencia del `PUT`).

Nombres en español: `void aceptarReservaSinVacantesDevuelveConflicto()`.

---

## 11. Git y forma de trabajo

Resumen de la sección 13 de la preentrega, que es la que manda.

- Ramas: `main` (entregas), `develop` (integración). Ramas de trabajo desde `develop` con el prefijo del tipo de commit: `feat/`, `fix/`, `refactor/`, `test/`, `docs/`, `chore/` + código de tarea + descripción en kebab-case: `feat/rf03-filtros-feed`. El README todavía dice `feature/`: se corrige en la tarea E0.
- Commits en [Conventional Commits](https://www.conventionalcommits.org/es/): `tipo(alcance): descripción en minúscula y en imperativo`. Alcances: `auth`, `feed`, `publish`, `reservation`, `driver-panel`, `my-trip`, `sync`, `db`, `network`, `nav`, `ui`, `push`, `build`, `backend`.
  - `feat(feed): agregar filtro por sede y turno`
  - `fix(sync): reintentar con backoff cuando la API devuelve 5xx`
  - `feat(backend): agregar endpoint para aceptar reservas`
- Un cambio lógico por commit. Commits chicos y seguidos: el docente mira la evolución del repo (4.14 del enunciado).
- PRs chicos (idealmente menos de 400 líneas). Si una funcionalidad es grande, se parte: primero backend, después datos y dominio de la app, después la UI.
- Plantilla de PR (`.github/pull_request_template.md`): qué cambia, qué RF implementa, cómo probarlo, capturas si toca UI, checklist de §12.
- Antes de pedir revisión: el autor corre `/code-review` de Claude Code (o equivalente) sobre su rama y resuelve o responde los hallazgos. Las reglas que la revisión tiene en cuenta están en `CLAUDE.md`.
- Para mergear: aprobación de al menos otro integrante, CI en verde (build, Lint y tests de la app; `mvn verify` del backend; CodeQL) y conversaciones resueltas. **Si el PR toca una capa que no es la tuya, el responsable de esa capa tiene que estar entre los revisores** (ver DIVISION_TRABAJO.md).
- Merge a `develop` con *squash* si el historial de la rama es desprolijo, o *merge commit* si los commits cuentan la historia. No se reescribe historia de `develop` ni `main`.
- Nunca se commitea: `local.properties`, `google-services.json`, `.env`, keystores, cuentas de servicio, tokens ni URLs con credenciales.

---

## 12. Checklist de revisión (Definition of Done)

Un PR está terminado cuando:

- [ ] Compila y pasan Lint y los tests en CI.
- [ ] App: respeta la regla de capas (`domain` sin Android; el ViewModel no importa nada de `data`).
- [ ] App: la UI lee de Room (Flow); las escrituras siguen el flujo `isSync` + WorkManager.
- [ ] App: la pantalla resuelve cargando, contenido, vacío, error y sin conexión (o el PR explica por qué alguno no aplica).
- [ ] App: sin textos hardcodeados ni colores fuera del tema; íconos accionables con `contentDescription`.
- [ ] App: errores manejados con `AppResult`; no hay `!!` ni `catch (e: Exception)` que se trague la cancelación.
- [ ] Backend: el controller solo delega; las reglas están en el Service; no se devuelven entidades JPA.
- [ ] Backend: si cambia el contrato, `docs/API.md` está actualizado en el mismo PR.
- [ ] Hay tests para la lógica nueva.
- [ ] El autor puede explicar cada línea, incluida la generada con IA (§13).

---

## 13. Uso de IA

Se permite (sección 8 del enunciado), con estas reglas:

- Antes de incorporar código sugerido por una IA, el autor tiene que poder responder: qué problema resuelve, en qué capa está, qué dependencias agrega, si hay algo más simple, cómo se prueba y qué pasa si falla.
- Una dependencia nueva sugerida por la IA pasa por el grupo igual que cualquier otra (§1).
- En la defensa cualquiera puede ser consultado por cualquier parte del código: lo que no entendés, no lo mergees.

---

## 14. Decisiones abiertas

Se resuelven en el grupo y se actualiza este archivo:

| # | Decisión | Impacto |
| --- | --- | --- |
| 1 | Dónde corre el backend en la demo final: en una notebook en la misma red Wi-Fi que el teléfono, o en un servicio con plan gratuito (Render, Railway, Fly.io) | URL base de release, HTTPS, deep links de los correos |
| 2 | Cuenta de Firebase del equipo para FCM (quién la crea y cómo se comparte la cuenta de servicio sin subirla al repo) | Push (alcance 8) |
| 3 | ¿Mantenemos `com.example.uniride` o pasamos a un paquete propio? Para el TP no hace falta cambiarlo; Google Play no acepta `com.example` | Paquete de la app y del backend |
