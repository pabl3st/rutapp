# RutasApp Android

App nativa Android para gestión de rutas y visitas de campo para equipos comerciales. Arquitectura offline-first con sincronización incremental.

**Stack:** Kotlin 2 · Jetpack Compose · Material 3 · Clean Architecture + MVVM · Hilt · Room v14 · WorkManager  
**API:** `https://mejoresiagratis.com/rutasproapk/api.php`  
**BD servidor:** `cqvkelal_rutasapp_android` (MySQL, cPanel)  
**Build actual:** 89 · Room v14 · API v1.2

---

## Setup del repositorio

### 1. Clonar
```bash
git clone https://github.com/pabl3st/rutapp.git
cd rutapp
```

### 2. Android SDK
Crea `local.properties` en la raíz:
```properties
sdk.dir=/ruta/a/tu/Android/Sdk
```
> macOS: `~/Library/Android/sdk` · Windows: `C:\Users\TuUsuario\AppData\Local\Android\Sdk` · Linux: `~/Android/Sdk`

### 3. JDK 17
```bash
java -version   # debe mostrar version "17"
```

### 4. Pre-push hook (obligatorio)
```bash
bash .githooks/install.sh
```
Ejecuta `kspDebugKotlin` + `compileDebugKotlin` antes de cada push. Para saltar en emergencia: `git push --no-verify`

### 5. Android Studio Ladybug 2024.2+
Al abrir → **Sync Project with Gradle Files** (instala el hook automáticamente).

---

## Credenciales necesarias

| Servicio | Configuración | Estado |
|---|---|---|
| Android SDK | `local.properties → sdk.dir` | ✅ Obligatorio |
| JDK 17 | `JAVA_HOME` o Android Studio | ✅ Obligatorio |
| Firebase FCM | `google-services.json` incluido | ✅ Ya configurado |
| API backend | `https://mejoresiagratis.com` hardcoded | ✅ Ya configurado |
| MapLibre/OpenFreeMap | Sin key, público | ✅ Automático |
| OSRM routing | Sin key, público | ✅ Automático |
| Nominatim geocoding | Sin key, público | ✅ Automático |
| Google Maps | `local.properties → MAPS_API_KEY` | ⚪ Opcional |
| Gemini / Groq | El usuario introduce su key en la app | ⚪ S17 |

---

## CI/CD — GitHub Actions

Cada push a `main` → APK debug disponible en **Actions → Artifacts** en ~8 min.

```bash
# APK release manualmente:
git tag sprint-sXX && git push origin sprint-sXX
```

---

## Jerarquía de roles

| Rol | Quién | Nivel | Ve |
|-----|-------|-------|----|
| god | Pabl3st — control cross-account | 6 | Todas las cuentas |
| owner | Empresa cliente (ej: Telco SA) | 5 | Toda la cuenta |
| admin | Jefe territorial | 4 | Toda la cuenta |
| manager | Supervisor de zona | 3 | Solo sus agentes directos |
| agent | Comercial en campo | 2 | Solo sus rutas |
| viewer | Solo lectura | 1 | Solo Perfil |

**Regla de asignación:** cada rol solo puede asignar rutas a roles inferiores dentro de su ámbito.

---

## Modelo de datos clave

**Informes diarios independientes:** cada fecha en `scheduledDates` de una ruta genera un `StopEntity` propio con `dateAssigned`. Un PDV con visitas el 12/5 y el 24/5 → 2 stops independientes, cada uno con su propio `status` y ciclo de vida. Igual que el modelo de `reports` de la versión web.

**Room v14:** migrations 1→14. Última migration: `dateAssigned` en stops para informes independientes por fecha.

---

## Estructura de paquetes

```
app/src/main/kotlin/com/pabl3st/rutapp/
├── core/
│   ├── location/       LocationManager, PermissionHandler
│   ├── map/            MapLibreProvider, MapProvider, OSRM routing
│   └── ui/             Theme, Spacing, componentes base
├── data/
│   ├── local/          Room DB v14 — entidades + DAOs + migrations 1→14
│   ├── network/        RutasApiService (Retrofit)
│   ├── repository/     Repositorios offline-first
│   └── session/        SessionManager (EncryptedSharedPreferences)
├── di/                 Hilt modules (Database, Network, Location, Map)
├── fcm/                Firebase Cloud Messaging + deeplinks
├── feature/
│   ├── auth/           Login + registro individual/empresa
│   ├── home/           Dashboard + JornadaBar (timer, km GPS)
│   ├── rutas/          Lista, detalle, mapa, crear/editar paradas
│   ├── visita/         Formulario + historial + check-in/out + KPIs
│   ├── kpis/           Estadísticas + gráficas + filtro agente (S21)
│   ├── mapa/           Mapa global con routing OSRM + popup PDV
│   ├── calendario/     Planificación mensual
│   ├── biblioteca/     PDVs deduplicados por externalId
│   ├── perfil/         Perfil usuario + negocio + KPI editor
│   ├── admin/          Panel de administración por rol
│   ├── importar/       Wizard CSV/XLSX + upsert sin duplicados
│   └── team/           (S20) TeamScreen + AgentDetailScreen
├── navigation/         Screen.kt, RutasNavGraph, BottomNavBar
└── sync/               SyncWorker (WorkManager, offline-first)
```

---

## Roadmap de sprints

### Completados ✅

| Sprint | Contenido |
|--------|-----------|
| S01–S09 | Auth, Room, Sync, GPS, Mapas, Visitas, KPIs, Jornada, Perfil negocio |
| S10b | Ordenación paradas Manual/GPS/Greedy |
| S11b | Biblioteca paradas con búsqueda |
| S12 | Calendario mensual con festivos ES |
| S13 | Import CSV/XLSX + GeoCluster K-means++ |
| S14 | Admin panel completo |
| S15 | Jerarquía de roles god→owner→admin→manager→agent→viewer |
| S16 | Bugs jornada (timer, km), KPIs scheduledDates, pins de color en mapa |

### Grupo A — UX datos en pantallas ✅ (builds 74-75)
StopCard con visitResult/nextAction/prioridad/estado PDV. BibliotecaStopCard con próxima visita. Calendario con nombre de ruta en celda. VisitaScreen cabecera completa. KPIs con trend vs período anterior.

### Grupo B — ViewModel/data ✅ (builds 76-77)
Siguiente parada pendiente en HomeScreen. route.status sincronizado con DaySession. Popup al tocar pin en GlobalMap. GPS automático al guardar visita.

### Grupo C — Pantallas nuevas ✅ (builds 78-83)
EditarParadaScreen (owner/admin/god). Historial de visitas anteriores en VisitaScreen.

### Grupo D — Sincronización API ✅ (builds 84-86)
checkInTs/checkOutTs/gpsLatVisit/gpsLngVisit en StopEntity + Room migration 12→13 + api.php.

### Informes diarios independientes ✅ (build 87)
1 StopEntity por fecha por PDV. Room migration 13→14. Selector de fecha en RouteDetailScreen. Deduplicación en Biblioteca.

### Operaciones de cuenta ✅ (builds 88-89)
Dedup Biblioteca por externalId. Upsert import sin duplicados. Borrar todas las rutas (owner/god). Endpoint clear_routes.

---

### Pendientes ⏳

| Sprint | Contenido | Esfuerzo |
|--------|-----------|----------|
| **S18** | Play Store: keystore, build-release.yml, proguard | 1 día |
| **S19** | Manager crea rutas + assign_route + reasignar en app | 1 día |
| **S20** | team_overview endpoint + TeamScreen + AgentDetailScreen | 2 días |
| **S21** | KPIsScreen selector agente + GlobalMap marcadores agentes + AdminScreen gráficas | 1.5 días |
| **S22** | OnboardingGPS + ResumenJornada + offline retry WorkManager | 1.5 días |
| **S23** | AgendaEquipoScreen + exportar PDF + Calendario modo equipo | 2 días |

### Acciones manuales pendientes en servidor
- Ejecutar `rutasproapk/migration_v12.sql` en phpMyAdmin (checkIn/checkOut/GPS + dateAssigned)

---

## Errores históricos — resumen rápido

Ver `CLAUDE.md` sección "Errores documentados" para código de ejemplo de cada error.

| # | Descripción |
|---|-------------|
| 1–20 | Auth, Room KSP, Compose, Hilt, Roles |
| 21 | `.copy()` en data class equivocada |
| 22 | `startTick()` con snapshot congelado de session |
| 23 | `pause()` usa `pausedAt` en lugar de `startedAt` |
| 24 | `return@composable` dentro de `navArgument{}` |
| 25 | KFunction nullable vs lambda nullable |
| 26 | Race condition FCM deeplink |
| 27 | `scheduledDates` no consultado en filtros temporales |
| 28 | `Spacing.X` sin import en archivo nuevo |
| 29 | `LocalContext.current` en lambda `onClick` (no @Composable) |
| 30 | Nombre de método incorrecto en LocationManager (`lastKnownLocation` → `getLastLocation`) |
| 31 | Variable local en corrutina no accesible desde otra función — usar UiState |
| 32 | `@Query` Room separado de `suspend fun` por bloque insertado — KSP no los une |
