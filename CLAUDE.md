# RutasApp Android — Manual de contexto para Claude

> **LEER COMPLETO antes de escribir cualquier línea de código.**
> Este fichero es la fuente de verdad del proyecto. Cualquier instancia de Claude
> que trabaje en este repo debe arrancar leyendo este fichero íntegramente.

---

## Qué es este proyecto

**RutasApp Android** es una app nativa Android (Kotlin + Jetpack Compose) para gestión
de rutas comerciales de campo. Es independiente de la PWA existente en
`mejoresiagratis.com/rutas-backup/`.

- **Repo GitHub**: `pabl3st/rutapp` (privado, rama `main`)
- **API backend**: `https://mejoresiagratis.com/rutasproapk/api.php`
- **Base de datos**: `cqvkelal_rutasapp_android` (MySQL, MariaDB 10.6, cPanel)
- **Despliegue API**: subida manual de `rutasproapk/api.php` por cPanel/FTP

---

## Stack técnico — versiones fijadas

| Dependencia        | Versión          | Nota                                        |
|--------------------|------------------|---------------------------------------------|
| AGP                | 8.5.2            | Compatible con Gradle 8.9                   |
| Kotlin             | 2.0.21           | Con Compose compiler plugin integrado       |
| KSP                | 2.0.21-1.0.25    | Prefijo DEBE coincidir con versión Kotlin   |
| Hilt               | 2.52             | —                                           |
| Compose BOM        | 2024.10.00       | Gestiona todas las versiones Compose        |
| Navigation Compose | 2.8.3            | —                                           |
| Retrofit           | 2.11.0           | —                                           |
| OkHttp             | 4.12.0           | —                                           |
| Moshi              | 1.15.1           | Codegen vía KSP (migrar de kapt en S02)     |
| security-crypto    | 1.1.0            | Ver ERROR 6 abajo                           |
| minSdk             | 26               | Android 8.0+                                |
| compileSdk         | 35               | —                                           |

> **No actualizar versiones sin revisar compatibilidad cruzada.**
> Especialmente: KSP siempre debe empezar con el mismo prefijo que Kotlin.

---

## Arquitectura del proyecto

```
app/src/main/kotlin/com/pabl3st/rutapp/
├── RutasApp.kt
├── MainActivity.kt
├── core/
│   ├── importer/           CsvParser.kt, GeoCluster.kt
│   ├── location/           LocationManager.kt, PermissionHandler.kt
│   ├── map/                MapLibreProvider.kt, MapProvider.kt, MapProviderFactory.kt, OtherProviders.kt
│   └── ui/theme/           Theme.kt, ThemeRepository.kt, ThemeViewModel.kt
├── data/
│   ├── local/
│   │   ├── RutasDatabase.kt   (version=6, migrations 1→6)
│   │   ├── dao/               RouteDao, StopDao, SyncQueueDao, DaySessionDao, KpiDefinitionDao, BusinessProfileDao, KpiValueDao
│   │   └── entity/            RouteEntity, StopEntity, SyncQueueEntity, DaySessionEntity, KpiDefinitionEntity, KpiValueEntity, BusinessProfileEntity, KpiCatalog
│   ├── network/               RutasApiService.kt + DTOs
│   └── repository/            AuthRepository, RouteRepository, StopRepository, SyncRepository, JornadaRepository, BusinessProfileRepository, DtoMappers
├── di/                    DatabaseModule, NetworkModule, LocationModule, MapModule
├── fcm/                   FcmTokenRepository, RutasMessagingService
├── feature/
│   ├── admin/             AdminScreen, AdminViewModel
│   ├── auth/              AuthScreens, AuthViewModel
│   ├── biblioteca/        BibliotecaScreen, BibliotecaViewModel
│   ├── calendario/        CalendarioScreen, CalendarioViewModel
│   ├── home/              HomeScreen, HomeViewModel, JornadaBar, JornadaViewModel
│   ├── importar/          ImportarScreen, ImportarViewModel
│   ├── kpis/              KpisScreen, KpisViewModel
│   ├── mapa/              GlobalMapScreen, GlobalMapViewModel
│   ├── perfil/            PerfilScreen, PerfilViewModel, BusinessProfileScreen, BusinessProfileViewModel
│   ├── rutas/             RutasScreen, RutasViewModel, RouteDetailScreen, RouteDetailViewModel,
│   │                      RouteMapScreen, RouteMapViewModel, CrearParadaScreen, CrearParadaViewModel
│   └── visita/            VisitaScreen, VisitaViewModel
├── navigation/            Screen.kt, RutasNavGraph.kt, BottomNavBar.kt
└── sync/                  SyncWorker.kt
```

### Room — versión actual: 6
Migrations: 1→2 (stops campos visita), 2→3 (stops campos comerciales), 3→4 (day_sessions),
4→5 (kpi_definitions + business_profiles), 5→6 (kpi_values).

### Screen routes disponibles
```kotlin
Screen.Auth, Screen.Home, Screen.Rutas, Screen.Mapa,
Screen.Kpis, Screen.Calendario, Screen.Perfil, Screen.Admin,
Screen.Visita          // visita/{stopUid}
Screen.RouteDetail     // route/{routeUid}
Screen.RouteMap        // map/{routeUid}
Screen.CrearParada     // crear-parada/{routeUid}
Screen.BusinessProfile // business-profile
Screen.Biblioteca      // biblioteca
Screen.Importar        // importar
```

---

## Estado por sprint

### S01 — COMPLETADO ✅
- Auth completo: register_individual, register_company, register_with_invite, login, logout, me, token_refresh, health
- SessionManager con EncryptedSharedPreferences
- Clean Architecture + MVVM base
- CI/CD GitHub Actions: push → assembleDebug → artifact

### S02 — COMPLETADO ✅
- Room: RouteEntity, StopEntity, SyncQueueEntity + DAOs + RutasDatabase v1→v2
- SyncWorker (WorkManager + HiltWorker) + SyncRepository
- HomeScreen real con lista de rutas del día
- Firebase FCM + Crashlytics + Analytics

### S03 — COMPLETADO ✅
- RutasScreen, RouteDetailScreen, RouteMapScreen (MapLibre)
- GPS: LocationManager + PermissionHandler

### S04 — COMPLETADO ✅
- GlobalMapScreen: mapa global con todas las rutas del día, filtros PENDING/DONE/NO_GPS

### S05 — COMPLETADO ✅
- VisitaScreen: formulario de visita con resultado, notas, próxima acción, estado PDV
- VisitaViewModel: actualiza stop en Room + KpiValues
- Fotos en visita: CameraX captura + Coil preview

### S06 — COMPLETADO ✅
- KpisScreen: estadísticas de visitas, tendencia semanal/mensual, filtro por ruta, KPIs del sector
- KpisViewModel: cálculos sobre Room, SIX_MONTHS, buildSectorKpis()

### S07 — COMPLETADO ✅
- CrearParadaScreen: formulario completo (nombre, código, dirección, GPS, contacto, prioridad, notas)
- OSRM routing en MapLibreProvider

### S08 — COMPLETADO ✅
- DaySessionEntity + JornadaRepository + JornadaViewModel + JornadaBar
- HomeScreen con JornadaBar cuando hay exactamente 1 ruta hoy
- RutasDatabase v3→v4

### S09 — COMPLETADO ✅
- BusinessProfileEntity + KpiDefinitionEntity + KpiValueEntity (Room v4→v6)
- BusinessProfileRepository + KpiDefinitionDao + KpiValueDao + BusinessProfileDao
- KpiCatalog: perfiles predefinidos (telco, farma, distribución, retail, common)
- BusinessProfileScreen + BusinessProfileViewModel: selector de sector + editor de KPIs
- VisitaScreen: campos dinámicos del sector (KpiField con tipos number/boolean/select/text)
- VisitaViewModel: loadKpiFields() + onKpiValueChange() + saveVisit() guarda KpiValueEntity
- KpisScreen: buildSectorKpis() agrega valores por KpiDefinition activo del perfil
- Sync: DaySessionDto, KpiValueDto en RutasApiService; SyncRepository procesa kpi_values
- api.php: delta_sync + batch_sync con day_sessions y kpi_values

### S10b — COMPLETADO ✅
- RouteDetailScreen: ordenación Manual/GPS/Greedy con 3 chips
- StopDao: queries con ORDER BY flexible
- StopRepository: reorderByGps(), reorderGreedy()
- RouteDetailViewModel: activeSort enum (MANUAL/GPS/GREEDY) + onReorder()

### S11b — COMPLETADO ✅ (Biblioteca de paradas)
- BibliotecaScreen: tabs Todas/Sin GPS/Sin ruta, búsqueda en tiempo real, bulk actions
- BibliotecaViewModel: combine(_tab, _query.debounce(200)) + flatMapLatest

### S13 — COMPLETADO ✅ (Import CSV + XLSX)
- CsvParser: auto-detecta separador, maneja comillas, UTF-8
- XlsxParser: parser OOXML sin dependencias externas (ZIP+XML, primera hoja, shared strings)
- GeoCluster: K-means++ geográfico con estrategias AUTO/FIXED_K/RADIUS
- ImportarScreen: stepper 4 pasos (Seleccionar → Mapear → Preview+Clustering → Guardar)
- ImportarViewModel: autoMap(), buildClusters(), onSaveConfirm() crea rutas+stops en Room
- Soporta .csv, .txt, .xlsx — selección automática de parser por extensión
- RutasScreen: botón import en TopAppBar → navega a Screen.Importar

### S12 — COMPLETADO ✅ (Calendario)
- CalendarioScreen (305 líneas): grid mensual, selección de día, lista de rutas del día con estado
- CalendarioViewModel: observeAll() + groupBy fecha + festivos nacionales via date.nager.at/api/v3
- Festivos: cache por año, merge de múltiples años, fallback silencioso si no hay red

### S14 — COMPLETADO ✅ (Admin panel + roles)
- AdminScreen (373 líneas): stats de cuenta, lista de usuarios, invite + cambio de rol, deactivate
- AdminViewModel: loadUsers(), inviteUser(), updateRole(), deactivateUser()
- AdminRepository: listUsers, inviteUser, updateRole, deactivateUser + roleLabel()
- Roles: owner/admin/manager/agent/viewer — canManageUsers = userRole in {owner, admin}
- RutasApiService: DTOs AccountUserDto, UsersListResponse, InviteUserRequest, UpdateRoleRequest, DeactivateUserRequest
- api.php: users_list, invite_user (invite_codes), update_role, deactivate_user — roleLevel guards, owner protegido

### PENDIENTE ⏳

#### S15 — IA (depende S08+S09+S11)
- Reoptimización de ruta en tiempo real (Gemini/Groq con clave usuario)
- Asesor pre-visita por PDV: risk score + objetivo
- Contexto: historial KpiDefinition + JornadaSession

#### RELEASE — Firmado + icono definitivo + Play Store
- Keystore de producción + build release firmado
- Icono y splash definitivos (reemplazar placeholders)
- Privacy policy + ficha Play Store

---

## Clases y contratos existentes — NO duplicar

### `AuthResult<T>` (en AuthRepository.kt)
```kotlin
sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val code: Int = 0) : AuthResult<Nothing>()
}
```

### `SessionManager` — propiedades disponibles
```kotlin
session.token, session.isLoggedIn, session.userId, session.userName
session.userEmail, session.userRole  // "owner"|"admin"|"manager"|"agent"|"viewer"
session.userDisplayName, session.accountId, session.accountType
session.accountName, session.isCompany, session.deviceId
session.lastSyncTimestamp  // ISO8601, usado para delta_sync
session.saveAuth(...), session.clear()
```

### `RutasApiService` — BASE_URL
```kotlin
private const val BASE_URL = "https://mejoresiagratis.com/"
// API_PATH = "rutasproapk/api.php" — todos los endpoints usan @Query("action")
```

### `BusinessProfileRepository` — API disponible
```kotlin
profileRepo.getOrCreateProfile()              // suspend → BusinessProfileEntity
profileRepo.setSector(sector)                 // suspend — cambia sector + siembra KPIs
profileRepo.getVisibleKpisForSector(sector)   // suspend → List<KpiDefinitionEntity>
profileRepo.observeProfile()                  // Flow<BusinessProfileEntity?>
profileRepo.observeActiveKpis(sector)         // Flow<List<KpiDefinitionEntity>>
profileRepo.seedKpisIfNeeded(sector)          // suspend — inserta KPIs si no existen
profileRepo.setKpiVisible(id, visible)        // suspend
profileRepo.addCustomKpi(...)                 // suspend
profileRepo.deleteCustomKpi(id)              // suspend
profileRepo.sectors                           // listOf("telco","farma","distribucion","retail","custom")
profileRepo.sectorLabel(sector)               // String localizado
```

### `StopRepository` — API disponible
```kotlin
stopRepo.createStop(routeUid, name, externalId?, address?, lat?, lng?, orderIndex, notes?, contactName?, contactPhone?)
stopRepo.observeByRoute(routeUid)             // Flow<List<StopEntity>>
stopRepo.observeByRouteUids(routeUids)        // Flow<List<StopEntity>>
stopRepo.observeAll(accountId)                // Flow<List<StopEntity>>
stopRepo.observeWithoutGps(accountId)         // Flow<List<StopEntity>>
stopRepo.observeOrphaned(accountId)           // Flow<List<StopEntity>>
stopRepo.getByUid(uid)                        // suspend → StopEntity?
stopRepo.saveVisitResult(uid, result, notes?, nextAction?)
stopRepo.reorderStops(stops)                  // suspend
stopRepo.reorderByGps(routeUid, userLat, userLng) // suspend
stopRepo.reorderGreedy(routeUid, userLat, userLng) // suspend
```

### `RouteRepository` — API disponible
```kotlin
routeRepo.createRoute(name, dateAssigned)     // suspend → RouteEntity
routeRepo.observeAll()                        // Flow<List<RouteEntity>>
routeRepo.observeToday()                      // Flow<List<RouteEntity>>
routeRepo.getByUid(uid)                       // suspend → RouteEntity?
```

---

## Reglas de desarrollo

### Patrón offline-first obligatorio
1. Leer primero de Room (respuesta inmediata)
2. Lanzar sync en background si hay red
3. Emitir `Flow<T>` para que la UI se actualice automáticamente

### Inyección de dependencias
- Todos los repositorios: `@Singleton` con `@Inject constructor`
- I/O en constructores Singleton: siempre `by lazy {}`
- Room database: `by lazy {}` en el módulo Hilt

### Convenciones de código
- Un ViewModel por feature screen
- `UiState` data class por ViewModel
- `StateFlow<UiState>` expuesto como `val ui: StateFlow<UiState>`
- `@JsonClass(generateAdapter = true)` en todos los DTOs de Moshi
- `@Json(name = "snake_case")` para campos con nombre distinto al JSON

---

## Errores históricos — NO repetir

### ERROR 1: @OptIn faltante en APIs experimentales de Material 3
```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)
```
APIs que lo requieren: `TopAppBar`, `ModalBottomSheet`, `SearchBar`, `SwipeToDismissBox`, `DatePicker`, `TimePicker`, `ExposedDropdownMenuBox`

### ERROR 2: I/O en constructor de @Singleton
```kotlin
// SIEMPRE lazy:
private val db by lazy { Room.databaseBuilder(...).build() }
```

### ERROR 3: Retrofit baseUrl sin trailing slash
```kotlin
.baseUrl("https://mejoresiagratis.com/")  // CORRECTO — termina en /
```

### ERROR 4: Regex con backslash en .kts → usar triple-quote

### ERROR 5: Módulos en settings.gradle.kts sin build.gradle.kts

### ERROR 6: security-crypto = "1.1.0" — no usar alphas ni 1.0.0

### ERROR 7: Iconos mipmap — densidades: mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi + anydpi-v26

### ERROR 8: gradle-wrapper.jar — subir binario real (43KB), no stub

### ERROR 9: PHP — caracteres UTF-8 no estándar (U+00A0, em-dash) causan parse error
```python
content = content.replace(b'\xc2\xa0', b' ')
content = content.replace(b'\xe2\x80\x94', b'--')
```

### ERROR 10: PHP — prefs como array en lugar de objeto
```php
'prefs' => $row['prefs'] ? (object)json_decode($row['prefs'], true) : (object)[],
```

### ERROR 11: Plugins TOML fuera de su sección `[plugins]`

### ERROR 12: Composables privados no pueden capturar lambdas del scope exterior — propagar por toda la cadena

### ERROR 13: Imports duplicados por edición con scripts replace — reescribir el fichero completo si >3 replace

### ERROR 14: MapLibre 11.x — isZoomControlsEnabled no existe. Usar `isZoomGesturesEnabled = true`

### ERROR 15: Archivos TTF corruptos → crash "Could not load font". Verificar magic bytes antes de commit.

### ERROR 15b: MapLibreConfigurationException — inicializar en Application.onCreate(), NO en LaunchedEffect

### ERROR 16: `import` es palabra reservada Kotlin — NUNCA usar como segmento de package
```
// ROMPE:
package com.pabl3st.rutapp.core.import
// CORRECTO:
package com.pabl3st.rutapp.core.importer
```
Si un script genera ficheros en `core/import/`, el build falla con "Expecting a top level declaration".
La carpeta `core/import/` fue eliminada. Solo existe `core/importer/`.

---

## Flujo de commit obligatorio — Git Data API atómica

**NUNCA** `PUT /contents/{file}` en bucle.

```
1. POST /git/blobs      → sha por cada fichero modificado
2. POST /git/trees      → base_tree = SHA HEAD actual
3. POST /git/commits    → apunta al nuevo tree
4. PATCH /git/refs/heads/main → actualiza la rama
```
Resultado: 1 commit, 1 build CI.

---

## Checklist pre-commit

- [ ] `@file:OptIn(ExperimentalMaterial3Api::class)` en todas las screens con TopAppBar/ModalBottomSheet/etc.
- [ ] Ningún `@Singleton` hace I/O en constructor — usar `by lazy {}`
- [ ] `BASE_URL` de Retrofit termina en `/`
- [ ] Regex en `.kts` usan triple-quote
- [ ] Solo módulos con `build.gradle.kts` están en `settings.gradle.kts`
- [ ] `security-crypto = "1.1.0"`
- [ ] KSP version empieza igual que Kotlin version
- [ ] `api.php` sin caracteres UTF-8 no estándar
- [ ] `prefs` en PHP devuelve `(object)` no `array()`
- [ ] Commit atómico (Git Data API, no PUT en bucle)
- [ ] No hay clases/funciones duplicadas
- [ ] Nuevos repositorios usan `AuthResult<T>` del mismo sealed class
- [ ] NO crear carpetas con `import` en el nombre del package
- [ ] Imports duplicados: `grep "^import" fichero | sort | uniq -d`

---

## Protocolo de diagnóstico ante errores de build

1. Leer TODOS los errores del log, no solo el primero
2. Auditar el patrón en todos los ficheros afectados antes de fixear
3. Propagar fixes derivados (si cambia firma de función, actualizar todas las llamadas)
4. Un solo commit con todos los fixes

---

## Metodología de trabajo

1. **Antes de cada sprint**: leer CLAUDE.md + todos los `.kt` relevantes + `api.php`
2. **Escribir plan detallado** con rutas exactas antes de ejecutar
3. **Revisión del plan** por el usuario antes de escribir código
4. **Commits atómicos** por tarea lógica
5. **Verificar CI verde** antes de dar tarea por terminada
