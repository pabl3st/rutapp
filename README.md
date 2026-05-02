# RutasApp Android

App nativa Android para gestión de rutas y visitas de campo.

**Stack:** Kotlin 2 · Jetpack Compose · Material 3 · Clean Architecture + MVVM · Hilt · Room · WorkManager  
**API:** `https://mejoresiagratis.com/rutasproapk/api.php`  
**BD:** `cqvkelal_rutasapp_android`

---

## Setup del repositorio

### 1. Clonar

```bash
git clone https://github.com/pabl3st/rutapp.git
cd rutapp
```

### 2. Android SDK

Crea `local.properties` en la raíz del proyecto:

```properties
sdk.dir=/ruta/a/tu/Android/Sdk
```

> En macOS suele ser `~/Library/Android/sdk`  
> En Windows `C:\\Users\\TuUsuario\\AppData\\Local\\Android\\Sdk`  
> En Linux `~/Android/Sdk`

### 3. JDK 17

El proyecto requiere JDK 17 (igual que el CI). Verifica con:

```bash
java -version   # debe mostrar version "17"
```

Si tienes varias versiones instaladas, configura `JAVA_HOME` o ajústalo en Android Studio:  
`File → Project Structure → SDK Location → JDK location`

### 4. Pre-push hook (obligatorio)

Instala el hook que verifica la compilación **antes** de cada push:

```bash
# Opción A — script directo
bash .githooks/install.sh

# Opción B — via Gradle
./gradlew installGitHooks
```

Una vez instalado, cada `git push` ejecutará automáticamente:
- `kspDebugKotlin` — valida Hilt, KSP y Room
- `compileDebugKotlin` — compila todo el Kotlin

Si hay errores, **el push se cancela** antes de llegar a GitHub.  
Para saltarlo en caso de emergencia: `git push --no-verify`

> **Nota:** el hook también se instala automáticamente al hacer Gradle Sync en Android Studio.

### 5. Abrir en Android Studio

Android Studio Ladybug 2024.2+ o superior. Al abrir el proyecto haz **Sync Project with Gradle Files** — esto instala el hook automáticamente.

### 6. Google Maps (opcional)

El mapa usa **MapLibre + OpenFreeMap** por defecto (sin key, sin cuenta). Si quieres activar Google Maps, añade tu key en `local.properties`:

```properties
MAPS_API_KEY=AIza...
```

Y actualiza el `AndroidManifest.xml` con esa key.

---

## Credenciales necesarias

| Servicio | Configuración | Obligatorio |
|---|---|---|
| Android SDK | `local.properties` → `sdk.dir` | ✅ Sí |
| JDK 17 | `JAVA_HOME` o Android Studio | ✅ Sí |
| Firebase (FCM) | `google-services.json` incluido en repo | ✅ Ya configurado |
| API propia | `https://mejoresiagratis.com` hardcoded | ✅ Ya configurado |
| MapLibre/OpenFreeMap | Sin key — público | ✅ Automático |
| OSRM routing | Sin key — público | ✅ Automático |
| Nominatim geocoding | Sin key — público | ✅ Automático |
| Google Maps | `local.properties` → `MAPS_API_KEY` | ⚪ Opcional |
| Gemini / Groq (S15) | El usuario introduce su propia key en la app | ⚪ Futuro |

---

## CI/CD — GitHub Actions

### Build automático
Cada push a `main` dispara compilación + APK debug. Disponible en **Actions → Artifacts** en ~8 min.

### Entrega de sprint
```bash
git tag sprint-s10
git push origin sprint-s10
```
Genera APK release y lo publica como GitHub Release automáticamente.

También desde: `Actions → Build Release APK → Run workflow`

---

## Estructura de paquetes

```
app/src/main/kotlin/com/pabl3st/rutapp/
├── core/
│   ├── location/       LocationManager, PermissionHandler
│   ├── map/            MapLibreProvider, MapProvider, OSRM routing
│   └── ui/             Theme, Spacing, componentes base
├── data/
│   ├── local/          Room DB v6 — 7 entidades + 7 DAOs
│   ├── network/        RutasApiService (Retrofit)
│   ├── repository/     6 repositorios offline-first
│   └── session/        SessionManager (EncryptedSharedPreferences)
├── di/                 Hilt modules (Database, Network, Location, Map)
├── fcm/                Firebase Cloud Messaging
├── feature/
│   ├── auth/           Login + registro individual/empresa
│   ├── home/           Dashboard + JornadaBar
│   ├── rutas/          Lista rutas, detalle, mapa, creador de paradas
│   ├── visita/         Formulario visita + fotos CameraX + KPIs dinámicos
│   ├── kpis/           Estadísticas + gráficas 6 meses + filtro por ruta
│   ├── mapa/           Mapa global con routing OSRM
│   ├── calendario/     Planificación mensual
│   ├── perfil/         Perfil usuario + perfil de negocio + KPI editor
│   └── admin/          Panel de administración
├── navigation/         Screen.kt, RutasNavGraph, BottomNavBar
└── sync/               SyncWorker (WorkManager, offline-first)
```

---

## Sprints

| Sprint | Estado | Contenido |
|--------|--------|-----------|
| S01 — Auth + BD | ✅ Completado | Auth completo, SessionManager cifrado, CI/CD, API PHP, BD MySQL |
| S02 — Room + Sync | ✅ Completado | Room DB, SyncWorker offline-first, HomeScreen, Firebase FCM |
| S03 — Rutas + GPS | ✅ Completado | RutasScreen, RouteDetail, RouteMap (MapLibre), GPS |
| S04 — Mapa global | ✅ Completado | GlobalMapScreen, filtros estado, routing OSRM |
| S05 — Visitas + Fotos | ✅ Completado | VisitaScreen, CameraX, KPIs dinámicos, PDV abierto/cerrado |
| S06 — KPIs | ✅ Completado | KpisScreen, 6 meses, filtro por ruta, sector KPIs |
| S07 — Creador paradas | ✅ Completado | CrearParadaScreen, geocoding, routing OSRM |
| S08 — Jornada laboral | ✅ Completado | Timer, km Haversine, GPS trail, DaySession Room |
| S09 — Perfil de negocio | ✅ Completado | BusinessProfile, KpiDefinition, KpiValue, CalendarioScreen, AdminScreen |
| S10 — Formulario extendido | ⏳ Pendiente | Secciones colapsables, estado PDV completo |
| S11 — KPIs biblioteca | ⏳ Pendiente | BibliotecaScreen, gráficas avanzadas |
| S12 — XLS Import | ⏳ Pendiente | Import Excel → KpiDefinition |
| S13 — Roles | ⏳ Pendiente | empViewPrefs, perfil por empleado |
| S14 — IA | ⏳ Pendiente | Gemini/Groq — reoptimización + asesor PDV |
| S15 — Polish | ⏳ Pendiente | UI/UX refinado |
| S16 — Play Store | ⏳ Pendiente | Publicación producción |
