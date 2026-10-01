# UniRide - Carpool Universitario

**UniRide** es una plataforma móvil nativa diseñada para conectar a estudiantes universitarios que viajan en auto con compañeros que comparten rutas y horarios similares hacia y desde la facultad, optimizando costos, tiempos de traslado y seguridad.

Trabajo Práctico Obligatorio para la materia **Desarrollo de Aplicaciones I** — **Universidad Argentina de la Empresa (UADE)**.

---

## Integrantes del Equipo

| Integrante | Legajo | Rol Principal | Áreas Técnicas |
| :--- | :--- | :--- | :--- |
| **Sassone, Maria Estefanía** | 1145414 | Arquitectura y Persistencia Local | Clean Architecture, Room Database, DAO/Entidades, Estrategia Offline First |
| **Herrera, Alvaro Manuel** | 1164449 | Capa de Red y Servicios Web | Cliente HTTP Retrofit, Endpoints REST, serialización JSON, Coroutines I/O |
| **Benitez, Nicolás Agustín** | 1137895 | Diseño UI/UX y Jetpack Compose | Prototipado en Figma, UI declarativa Jetpack Compose, Material Design 3, estados UI |
| **Forcherio, Gustavo Ariel** | 1200714 | Navegación, Estado y Dominio | Navigation Compose, ViewModel con StateFlow, casos de uso y reglas de negocio |

* **Docentes:** Peña, Alejandro Francisco | Narducci, Adrian Alberto

---

## Arquitectura y Organización del Proyecto

El proyecto sigue una arquitectura **MVVM (Model-View-ViewModel)** fundamentada en los principios de **Clean Architecture**, dividiendo el código en tres capas principales con flujo unidireccional de datos (UDF):

```text
app/src/main/java/com/example/uniride/
├── data/                      # Capa de Datos (Data Layer)
│   ├── datastore/             # Gestión de preferencias y sesión activa
│   ├── local/                 # Persistencia local (Room Database)
│   │   ├── dao/               # Data Access Objects (UserDao, TripDao, ReservationDao)
│   │   ├── database/          # AppDatabase (Room)
│   │   └── entity/            # Entidades SQLite (UserEntity, TripEntity, ReservationEntity)
│   ├── remote/                # Fuentes remotas (Retrofit REST API)
│   │   ├── api/               # Interfaces de servicios web
│   │   └── dto/               # Data Transfer Objects
│   └── repository/            # Implementaciones de repositorios (Single Source of Truth)
│
├── domain/                    # Capa de Dominio (Domain Layer - Kotlin puro)
│   ├── model/                 # Modelos de negocio (User, Trip, Reservation)
│   ├── repository/            # Interfaces de repositorios
│   └── usecase/               # Casos de uso / Interactors (SearchTrips, PublishTrip, BookTrip)
│
├── presentation/              # Capa de Presentación (Presentation Layer)
│   ├── auth/                  # Login y registro institucional (@uade.edu.ar)
│   ├── common/                # Componentes reutilizables (TopBar, banner offline, loaders)
│   ├── detail/                # Ficha de detalle de viaje y solicitud de asiento
│   ├── feed/                  # Feed de viajes con filtros por sede y horarios
│   ├── navigation/            # Configuración de Navigation Compose y rutas
│   ├── publish/               # Publicación de viajes (Conductor)
│   ├── reservations/          # Gestión de reservas y viaje activo
│   └── theme/                 # Paleta de colores, tipografía y estilos Material 3
│
└── MainActivity.kt            # Entry point de la aplicación Android
```

---

## Tecnologías Previstas

* **Lenguaje:** Kotlin
* **UI:** Jetpack Compose + Material Design 3
* **Arquitectura:** MVVM + Clean Architecture
* **Navegación:** Navigation Compose
* **Gestión de Estado:** ViewModel + StateFlow / Flow
* **Asincronismo:** Kotlin Coroutines (Dispatchers.IO / Dispatchers.Main)
* **Persistencia Local:** Room Database (SQLite) + DataStore Preferences
* **Estrategia de Datos:** *Offline First* con sincronización reactiva
* **Red:** Retrofit + Moshi / Kotlinx Serialization

---

## Flujo de Trabajo y Ramas (Git Flow)

Seguimos una metodología de **Git Flow simplificado**:

* **`main`**: Rama de producción y entregas estables evaluables. Solo recibe código testeado y aprobado mediante Pull Requests.
* **`develop`**: Rama principal de integración continua donde converge el desarrollo diario.
* **`feature/<nombre-feature>`**: Ramas individuales creadas a partir de `develop` para desarrollar una funcionalidad o pantalla específica (ej. `feature/login-institucional`, `feature/feed-viajes`, `feature/room-persistence`).

### Convención de Commits
Se utiliza la convención de [Conventional Commits](https://www.conventionalcommits.org/):
* `feat:` nueva funcionalidad o pantalla.
* `fix:` corrección de un bug o error.
* `refactor:` refactorización de código sin cambio de comportamiento externo.
* `style:` cambios de estilo visual o formato sin lógica.
* `docs:` cambios en documentación o README.
* `chore:` tareas de mantenimiento, dependencias o configuración Gradle.

---

## Requisitos y Configuración del Entorno

* **IDE:** Android Studio (versión compatible con Gradle 9+)
* **Java / JDK:** OpenJDK 17 o superior
* **Android SDK:**
  * `minSdk`: 30 (Android 11.0)
  * `compileSdk` / `targetSdk`: 37
  * **Kotlin:** 2.2.10
  * **Jetpack Compose:** Compose BOM 2026.02.01

### Ejecución
1. Clonar el repositorio:
   ```bash
   git clone https://github.com/Estefani-a/desaApp1-TPI.git
   ```
2. Abrir la carpeta `UniRide` en Android Studio.
3. Permitir la sincronización de Gradle y ejecutar en un emulador con API 30+ o dispositivo físico.
