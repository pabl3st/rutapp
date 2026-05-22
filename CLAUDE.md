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
| AGP                | 8.7.3            | Compatible con compileSdk 35                |
| Kotlin             | 2.0.21           | Con Compose compiler plugin integrado       |
| KSP                | 2.0.21-1.0.25    | Prefijo DEBE coincidir con versión Kotlin   |
| Hilt               | 2.52             | —                                           |
| Compose BOM        | 2024.10.00       | Gestiona todas las versiones Compose        |
| Navigation Compose | 2.8.3            | —                                           |
| Retrofit           | 2.11.0           | —                                           |
| OkHttp             | 4.12.0           | —                                           |
| Moshi              | 1.15.1           | Codegen vía KSP                             |
| security-crypto    | 1.1.0            | Ver ERROR 6 abajo                           |
| MapLibre           | 11.5.1           | —                                           |
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
│   ├── location/           LocationManager.kt, PermissionHandler.kt,
│   │                       LocationForegroundService.kt
│   ├── map/                MapLibreProvider.kt, MapProvider.kt, MapProviderFactory.kt, OtherProviders.kt
│   └── ui/theme/           Theme.kt, ThemeRepository.kt, ThemeViewModel.kt
├── data/
│   ├── local/
│   │   ├── RutasDatabase.kt   (version=12, migrations 1→12)
│   │   ├── dao/               RouteDao, StopDao, SyncQueueDao, DaySessionDao,
│   │   │                      KpiDefinitionDao, BusinessProfileDao, KpiValueDao,
│   │   │                      VisitPhotoDao
│   │   └── entity/            RouteEntity, StopEntity, SyncQueueEntity, DaySessionEntity,
│   │                          KpiDefinitionEntity, KpiValueEntity, BusinessProfileEntity,
│   │                          VisitPhotoEntity, StopTagConfig, KpiCatalog
│   ├── network/               RutasApiService.kt + DTOs
│   └── repository/            AuthRepository, RouteRepository, StopRepository,
│                              SyncRepository, JornadaRepository, BusinessProfileRepository,
│                              UserPrefsRepository, PhotoRepository, AdminRepository,
│                              DtoMappers
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
│   ├── perfil/            PerfilScreen, PerfilViewModel, BusinessProfileScreen,
│   │                      BusinessProfileViewModel
│   ├── rutas/             RutasScreen, RutasViewModel, RouteDetailScreen, RouteDetailViewModel,
│   │                      RouteMapScreen, RouteMapViewModel, CrearParadaScreen, CrearParadaViewModel
│   └── visita/            VisitaScreen, VisitaViewModel
├── navigation/            Screen.kt, RutasNavGraph.kt, BottomNavBar.kt
└── sync/                  SyncWorker.kt
```

### Room — versión actual: 12
Migrations:
- 1→2: stops campos visita (externalId, contactName, contactPhone, visitResult, nextAction)
- 2→3: stops campos comerciales (visitFrequency, priority, segment, accountStatus, openingHours)
- 3→4: day_sessions table
- 4→5: kpi_definitions + business_profiles tables
- 5→6: kpi_values table
- 6→7: stops.pdvOpen
- 7→8: stops.pdvInactive
- 8→9: routes.scheduledDates
- 9→10: índices de performance (routes, stops, kpi_values, sync_queue)
- 10→11: sync_queue UNIQUE INDEX
- 11→12: visit_photos table

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
- SessionManager con EncryptedSharedPreferences (MasterKey.Builder — no usar MasterKeys deprecated)
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
- PhotoRepository: savePhotos() persiste URIs, uploadPending() sube al servidor vía multipart
- VisitPhotoEntity + VisitPhotoDao (Room v11→v12)
- api.php: endpoint file_upload (POST multipart, 10MB max, MIME validation)

### S06 — COMPLETADO ✅
- KpisScreen: estadísticas de visitas, tendencia semanal/mensual, filtro por ruta, KPIs del sector
- KpisViewModel: cálculos sobre Room, SIX_MONTHS, buildSectorKpis() con getByStops() batch query
- stats_month endpoint (GET, manager+): agrega visitas + KPIs + desglose por agente del mes
- KpisScreen sección "Equipo este mes" visible solo para manager/admin/owner/god

### S07 — COMPLETADO ✅
- CrearParadaScreen: formulario completo (nombre, código, dirección, GPS, contacto, prioridad, notas)
- OSRM routing en MapLibreProvider

### S08 — COMPLETADO ✅
- DaySessionEntity + JornadaRepository + JornadaViewModel + JornadaBar
- HomeScreen con JornadaBar cuando hay exactamente 1 ruta hoy
- RutasDatabase v3→v4
- LocationForegroundService: GPS tracking en background durante jornada activa
  (startForegroundService desde JornadaViewModel, notificación persistente)

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

### S12 — COMPLETADO ✅ (Calendario)
- CalendarioScreen: grid mensual, selección de día, festivos ES vía date.nager.at
- CalendarioViewModel: pulsación larga → selector de rutas para asignar al día; unassignDate/assignDate
- RouteRepository: assignDate(uid, dateStr) + unassignDate(uid)
- UserPrefsRepository.vacation_days sincronizado con servidor vía update_user_prefs
- verifySession() restaura vacation_days del servidor al arrancar (recuperación tras reinstalación)

### S13 — COMPLETADO ✅ (Import CSV)
- CsvParser: auto-detecta separador, maneja comillas, UTF-8
- GeoCluster: K-means++ geográfico con estrategias AUTO/FIXED_K/RADIUS
- ImportarScreen: stepper 4 pasos (Seleccionar → Mapear → Preview+Clustering → Guardar)
- ImportarViewModel: autoMap(), buildClusters(), onSaveConfirm() crea rutas+stops en Room
- RutasScreen: botón import en TopAppBar → navega a Screen.Importar

### S14 — COMPLETADO ✅ (Admin panel)
- AdminScreen: sesión activa, estadísticas Room, gestión de usuarios, invitaciones
- AdminViewModel + AdminRepository: listUsers, inviteUser, updateRole, deactivateUser,
  reactivateUser, listInvites, deleteInvite, godSetRole
- Flujo de invitación: genera código (no envía email) → InviteCodeDialog con copiar al portapapeles
- Lista de invitaciones activas con InviteCard (código, rol, usos, expiración, botón eliminar)
- RolePicker con descripciones de cada rol
- account_config_save: owner/admin puede editar nombre de empresa desde PerfilScreen
- push_register: registra/actualiza token FCM en sesión activa del dispositivo
- api.php: users_list, invite_user, update_role, deactivate_user, reactivate_user,
  invite_list, invite_delete, account_config_save, push_register, stats_month, file_upload

### S15 — COMPLETADO ✅ (Jerarquía de roles y permisos)
- Roles: god > owner > admin > manager > agent > viewer con permisos en cascada
- owner/admin: crear/eliminar rutas y paradas, importar CSV, asignar supervisores
- manager: ve solo sus agentes (managedAgentIds vía delta_sync), asigna fechas
- agent: ejecuta rutas propias, no puede crear/eliminar
- viewer: solo Perfil — 10 guardias en NavGraph bloquean todo lo demás
- assign_manager endpoint: jerarquía fija con detección de ciclos transitivos
- Supervisor picker en AdminScreen con ManagerPickerDialog
- FCM push al asignar ruta (pushToUser helper en api.php)
- Sync inmediato al recibir route_assigned/route_reassigned
- Race condition deeplink: retry 10s en RouteDetailViewModel

### S16 — COMPLETADO ✅ (Bugs de jornada y KPIs)
- Timer jornada: 3 bugs encadenados corregidos (startedAt/pausedAt/snapshot)
- distanceKm: Haversine acumulado desde LocationForegroundService
- KPIs: filtros de período usan scheduledDates + dateAssigned
- HomeScreen: label fecha "Hoy" cuando today ∈ scheduledDates
- MapLibreProvider: fit bounds robusto (reset por ruta, 3 reintentos con style check)
- Pins de mapa con color por estado (done=verde, pending=azul, visiting=ámbar)
- KpisScreen: aviso cuando ruta seleccionada está fuera del período
- RouteMapScreen: "Visitado" en lugar de km para stops done
- JornadaBar: reabrir jornada finalizada con AlertDialog

### PENDIENTES ⏳

### GRUPOS UX A-D — COMPLETADOS ✅ (builds 74-89)
**Grupo A (datos en Room, solo UI):**
  StopCard: visitResult coloreado, nextAction, badge prioridad, icono PDV inactivo/cerrado
  BibliotecaStopCard: visitResult + próxima visita calculada desde visitFrequency
  CalendarioScreen: nombre de ruta en celda + altura mínima 44dp
  VisitaScreen: cabecera completa (navegar Maps, horario, segmento, prioridad)
  KpisScreen: MetricCard con trend "+N vs anterior" vs período anterior

**Grupo B (ViewModel/data):**
  HomeScreen: nextPendingStop en RouteProgressCard
  route.status sincronizado con DaySession al finalizar (markDone si todos done/skipped)
  GlobalMapScreen: popup BottomSheet al tocar pin con info PDV + botón registrar visita
  VisitaScreen: GPS automático al guardar + check-in/out timestamps

**Grupo C (pantallas nuevas):**
  EditarParadaScreen: editar todos los campos del PDV (owner/admin/god)
  VisitaScreen: historial de visitas anteriores por externalId (hasta 20)

**Grupo D (sync con API web):**
  StopEntity: checkInTs, checkOutTs, gpsLatVisit, gpsLngVisit
  Room migration 12→13, api.php batch_sync incluye los nuevos campos
  migration_v12.sql para aplicar en phpMyAdmin

**Informes diarios independientes (build 87):**
  1 StopEntity por fecha por PDV (dateAssigned). Room migration 13→14.
  Selector de fecha chips en RouteDetailScreen. Upsert en import sin duplicados.
  Deduplicación Biblioteca por externalId.

**Operaciones de cuenta (builds 88-89):**
  Borrar todas las rutas y paradas (owner/god). Endpoint clear_routes.
  Dedup Biblioteca. Upsert import. Roles en import corregidos.

### PENDIENTES ⏳

#### S18 — Play Store
- Keystore firma release, build-release.yml, proguard-rules.pro
- Subida al track de pruebas cerrado de Play Store

#### S19 — Manager operativo al 100%
- assign_route endpoint en api.php (reasignar ruta a otro agente)
- Reasignar ruta en RouteDetailScreen (todos los roles que pueden gestionar)
- Guardia EditarParada: manager puede editar paradas de sus agentes
- Onboarding de permisos GPS (LocationPermissionScreen)

#### S20 — Visibilidad del equipo
- team_overview endpoint (lista agentes + estado jornada + stops hoy + GPS)
- TeamScreen: lista de agentes con progreso de hoy
- AgentDetailScreen: actividad detallada de un agente

#### S21 — KPIs por agente + mapa mejorado
- KPIsScreen: selector de agente para ver KPIs de reportador
- GlobalMapScreen: marcadores de posición de agentes activos (polling 30s)
- AdminScreen: gráfica de tendencia semanal (librería Vico o MPAndroidChart)

#### S22 — Calidad del agente
- OnboardingGPSScreen: explicar por qué se necesita background location
- ResumenJornadaScreen: pantalla al finalizar con km, tiempo, resultados
- Offline retry: WorkManager SyncWorker con exponential backoff

#### S23 — Agenda equipo + exportar
- AgendaEquipoScreen: grid agentes × días (solo manager con sus directos)
- Exportar informe mensual PDF (PdfDocument Android nativo)
- CalendarioScreen modo equipo para manager

#### S17 — IA (depende S08+S09+S11)
- Reoptimización de ruta en tiempo real (Gemini/Groq con clave usuario)
- Asesor pre-visita por PDV: risk score + objetivo
- Contexto: historial KpiDefinition + JornadaSession

#### S18 — Play Store
- Firma release: keystore, secrets GitHub (KEYSTORE_BASE64, KEY_ALIAS, KEY_PASSWORD, STORE_PASSWORD)
- build-release.yml: assembleRelease + zipalign + apksigner
- Ficha Play Store: descripción ES+EN, capturas, icono 512×512, política privacidad

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
session.managedAgentIds    // List<Int> — agentes bajo supervisión directa (manager)
    // Se actualiza en cada delta_sync desde managed_agent_ids del servidor
session.saveAuth(...), session.clear()
// accountName es var — se puede actualizar tras account_config_save
```

### `RutasApiService` — BASE_URL
```kotlin
private const val BASE_URL = "https://mejoresiagratis.com/"
// API_PATH = "rutasproapk/api.php" — todos los endpoints usan @Query("action")
// Header de autenticación: @Header("X-Auth-Token") token: String
// NUNCA usar "Bearer {token}" — el servidor espera el token limpio
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
stopRepo.reorderStops(stops)                  // suspend — bulk update orderIndex
// Nota: sorting GPS/Greedy se implementa en RouteDetailViewModel
```

### `RouteRepository` — API disponible
```kotlin
routeRepo.createRoute(name, dateAssigned)     // suspend → RouteEntity
routeRepo.observeAll()                        // Flow — bifurca: isFullAccountView|isManagedView|agent
    // isFullAccountView: owner/admin/god → observeByAccount (toda la cuenta)
    // isManagedView: manager → observeByUserIds(managedAgentIds + userId)
    // agent/viewer → observeByUser(userId)
routeRepo.observeToday()                      // Flow<List<RouteEntity>>
routeRepo.getByUid(uid)                       // suspend → RouteEntity?
routeRepo.assignDate(uid, dateStr)            // suspend
routeRepo.unassignDate(uid, dateStr)          // suspend
```

### `JornadaRepository` — API disponible
```kotlin
jornadaRepo.observe(routeUid, dateStr)        // Flow<DaySessionEntity?>
jornadaRepo.get(routeUid, dateStr)            // suspend → DaySessionEntity?
jornadaRepo.start(routeUid, dateStr)          // suspend
jornadaRepo.pause(routeUid, dateStr)          // suspend
jornadaRepo.resume(routeUid, dateStr)         // suspend
jornadaRepo.finish(routeUid, dateStr)         // suspend
jornadaRepo.updateGps(routeUid, dateStr, lat, lng) // suspend — llamado desde LocationForegroundService
jornadaRepo.elapsedMs(session)                // Long — ms transcurridos corregidos por pausas
jornadaRepo.todayStr()                        // String ISO "2026-05-13"
```

### `UserPrefsRepository` — API disponible
```kotlin
prefsRepo.prefs                               // Flow<UserPrefs>
prefsRepo.update(transform)                   // suspend — actualiza DataStore + server
prefsRepo.toggleVacationDay(dateStr)          // suspend
prefsRepo.restoreFromServer(serverPrefs)      // suspend — merge vacation_days desde servidor
```

### `PhotoRepository` — API disponible
```kotlin
photoRepo.observeByStop(stopUid)              // Flow<List<VisitPhotoEntity>>
photoRepo.savePhotos(stopUid, uris)           // suspend — persiste URIs en Room
photoRepo.uploadPending()                     // suspend — sube fotos pendientes al servidor
```

### `AdminRepository` — API disponible
```kotlin
adminRepo.isOwnerOrAdmin, adminRepo.isGod, adminRepo.canManageUsers, adminRepo.isOwner
adminRepo.listUsers()                         // suspend → AuthResult<List<AccountUserDto>>
adminRepo.inviteUser(email, role)             // suspend → AuthResult<String> (devuelve código)
adminRepo.listInvites()                       // suspend → AuthResult<List<InviteDto>>
adminRepo.deleteInvite(inviteId)              // suspend → AuthResult<String>
adminRepo.updateRole(targetUserId, role)      // suspend → AuthResult<String>
adminRepo.deactivateUser(targetUserId)        // suspend → AuthResult<String>
adminRepo.reactivateUser(targetUserId)        // suspend → AuthResult<String>
adminRepo.godSetRole(targetUserId, role)      // suspend → AuthResult<String>
adminRepo.availableRoles                      // List<String> — roles asignables (SIN god/owner)
adminRepo.roleLabel(role)                     // String localizado
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

### ERROR 13: Imports duplicados por scripts `sed` con múltiples matches
`sed -i 's/^import kotlinx.coroutines/NUEVO_IMPORT\nimport kotlinx.coroutines/'` inserta el import
una vez POR CADA LÍNEA que empieza por ese prefijo. Si hay 4 líneas `import kotlinx.coroutines.*`
el resultado son 4 imports duplicados → error de compilación.
Solución: usar Python para manipular imports, nunca sed para insertar.
```python
# CORRECTO: deduplicar después de insertar
from collections import Counter
lines = content.split('\n')
seen = set()
cleaned = [l for l in lines if not l.strip().startswith('import ') or l.strip() not in seen and not seen.add(l.strip())]
```

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

### ERROR 17: Bearer token en X-Auth-Token header
```kotlin
// ROMPE — servidor devuelve 401:
token = "Bearer ${session.token}"
// CORRECTO:
token = session.token ?: ""
```
El servidor usa `requireAuth()` que lee `X-Auth-Token` directamente, sin prefijo Bearer.

### ERROR 18: Campo en `.copy()` que no existe en `data class UiState`
Si un método del ViewModel hace `_ui.update { it.copy(showSettingsRationale = true) }` pero
`showSettingsRationale` no está declarado en el `data class UiState`, el compilador da
"No parameter with name 'showSettingsRationale' found" — error que no siempre se detecta
en auditoría estática porque el data class puede estar en otro archivo.
Solución: verificar que TODOS los campos usados en `.copy()` existen en el UiState antes de commitear.

### ERROR 19: Roles no asignables en availableRoles
El servidor solo acepta `admin/manager/agent/viewer` en `update_role`.
`god` y `owner` son roles de sistema no asignables vía API.
`availableRoles` NUNCA debe incluir "god" ni "owner" — el servidor devuelve "Rol inválido 400".

### ERROR 20: Migration FK en cPanel MySQL
`CONSTRAINT FOREIGN KEY ... REFERENCES` puede fallar con Error 150 en cPanel aunque los tipos
coincidan. Solución: añadir `SET FOREIGN_KEY_CHECKS=0;` al inicio de la migration y
`SET FOREIGN_KEY_CHECKS=1;` al final. Alternativa: eliminar las FK y usar solo índices.

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

### ERROR 21: `routeOutOfPeriod` en data class equivocada
```kotlin
// ROMPE — routeOutOfPeriod no existe en KpiMetrics:
_ui.update { it.copy(metrics = KpiMetrics(..., routeOutOfPeriod = x)) }
// CORRECTO — pertenece a KpisUiState, no a KpiMetrics:
_ui.update { it.copy(metrics = KpiMetrics(...), routeOutOfPeriod = x) }
```
Regla general: antes de añadir un campo a un `.copy()`, verificar en qué `data class` está declarado.

### ERROR 22: `startTick()` con snapshot congelado de session
```kotlin
// ROMPE — session es el objeto del momento del collect, nunca se actualiza:
private fun startTick(session: DaySessionEntity) {
    tickJob = launch { while(true) { elapsedMs(session); delay(1000) } }
}
// CORRECTO — leer la sesión más fresca del UiState en cada tick:
private fun startTick(session: DaySessionEntity) {
    tickJob = launch { while(true) { val current = _ui.value.session ?: session; elapsedMs(current); delay(1000) } }
}
```

### ERROR 23: `pause()` calcula elapsed desde `pausedAt` en lugar de `startedAt`
```kotlin
// ROMPE en 2º pause — pausedAt tiene el timestamp del 1er pause:
val elapsed = elapsedMs + (now - (session.pausedAt ?: session.startedAt ?: now))
// CORRECTO — siempre usar startedAt como base; limpiar startedAt al pausar:
val elapsed = elapsedMs + (now - (session.startedAt ?: now))
dao.updateState(..., startedAt = null, pausedAt = now, elapsedMs = elapsed)
```
Y `resume()` debe actualizar `startedAt = now` y limpiar `pausedAt = null`.

### ERROR 24: `return@composable` dentro de bloque `navArgument { }` 
El script de inserción automática buscaba el primer `{` tras `composable(` y lo encontraba
dentro de `navArgument("uid") { type = NavType.StringType }` en lugar del cuerpo del composable.
```kotlin
// ROMPE — return@composable en bloque de configuración de argumento:
composable(arguments = listOf(navArgument("uid") {
    if (cond) { return@composable }   // ← ilegal aquí
    type = NavType.StringType
})) { ... }
// CORRECTO — guardia dentro del lambda del composable:
composable(arguments = listOf(navArgument("uid") { type = NavType.StringType })) { backStackEntry ->
    if (cond) { navController.popBackStack(); return@composable }
    ...
}
```

### ERROR 25: `{ { lambda } } else null` produce `Nothing?` solo con KFunction references
```kotlin
// ROMPE — KFunction no es compatible con Function0 en contexto nullable:
trailingIcon = if (canEdit) vm::doSomething else null
// OK — lambda normal sí se infiere como @Composable () -> Unit? :
trailingIcon = if (canEdit) { { Icon(...) } } else null
// MEJOR siempre — variable local explícita, nunca ambigüedad:
val icon: @Composable (() -> Unit)? = if (canEdit) { { Icon(...) } } else null
trailingIcon = icon
```

### ERROR 26: Race condition FCM deeplink — ruta no existe en Room al navegar
Cuando FCM llega con `route_assigned` y se navega a `RouteDetail(uid)`, el sync
ondemand se dispara pero Room puede estar vacío. `getByUid()` devuelve null → pantalla vacía.
Solución: retry con polling hasta 10s en `RouteDetailViewModel`:
```kotlin
var route = routeRepo.getByUid(routeUid)
if (route == null) { repeat(10) { delay(1000); route = routeRepo.getByUid(routeUid) } }
```

### ERROR 27: `scheduledDates` no consultado en filtros de período de KPIs
`KpisViewModel` filtraba rutas solo por `dateAssigned`. Una ruta con `dateAssigned='2026-05-06'`
y `scheduledDates=['2026-05-18']` aparecía vacía en KPIs el 18 de mayo.
Solución: helpers `routeDatesSet()`, `isRouteActiveOn()`, `isRouteActiveInRange()` que consultan
ambos campos. Lo mismo aplica a cualquier query temporal que use `dateAssigned`.

---

### ERROR 28: `Spacing.X` sin import en archivo nuevo
Cuando se añade código que usa `Spacing.md/sm/xs` en un archivo que no tenía el import, el compilador
falla con "Unresolved reference: Spacing". `Spacing` NO es parte de Material3 — es un objeto propio del proyecto.
```kotlin
// SIEMPRE añadir:
import com.pabl3st.rutapp.core.ui.theme.Spacing
// O mejor: sustituir por dp literales para evitar la dependencia
```

### ERROR 29: `LocalContext.current` en lambda `onClick` (no @Composable)
```kotlin
// ROMPE — onClick es una lambda normal, no un contexto @Composable:
IconButton(onClick = { LocalContext.current.startActivity(...) })
// CORRECTO — capturar fuera del lambda:
val ctx = LocalContext.current
IconButton(onClick = { ctx.startActivity(...) })
```

### ERROR 30: Nombre de método incorrecto en LocationManager
LocationManager expone `getLastLocation()` (suspend), no `lastKnownLocation()`.
```kotlin
// ROMPE: locationMgr.lastKnownLocation()
// CORRECTO: locationMgr.getLastLocation()
```

### ERROR 31: Variable local en corrutina no accesible desde otra función
```kotlin
// ROMPE — checkInTs solo existe en el scope de loadStop():
fun loadStop() { viewModelScope.launch { val checkInTs = System.currentTimeMillis() } }
fun saveVisit() { ... checkInTs ... }  // ← Unresolved reference
// CORRECTO — guardarlo en UiState:
data class UiState(val checkInTs: Long? = null, ...)
fun loadStop() { _ui.update { it.copy(checkInTs = System.currentTimeMillis()) } }
fun saveVisit() { ... _ui.value.checkInTs ... }
```

### ERROR 32: `@Query` Room separado de `suspend fun` por bloque insertado
Al insertar código entre el `@Query(...)` y el `suspend fun` que lo implementa, KSP no los asocia.
Room requiere que `@Query` y `fun` sean contiguos (solo líneas en blanco entre ellos, no código).
```
// ROMPE:
@Query("SELECT ...")
/** nuevo comentario */
@Query("otra query")      ← esto "roba" la primera @Query
suspend fun metodoOriginal()  ← queda sin @Query

// CORRECTO: insertar ANTES del @Query existente, no entre @Query y fun
```

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
- [ ] Campos de `.copy()` verificados contra el `data class` correcto (ERROR 21)
- [ ] `startTick()` usa `_ui.value.session` no snapshot congelado (ERROR 22)
- [ ] `pause()`/`resume()` actualizan `startedAt` correctamente (ERROR 23)
- [ ] Guardias de viewer en NavGraph dentro del lambda, no en `navArgument{}` (ERROR 24)
- [ ] `scheduledDates` consultado en cualquier filtro temporal de rutas (ERROR 27)
- [ ] NO crear carpetas con `import` en el nombre del package
- [ ] Imports duplicados: `grep "^import" fichero | sort | uniq -d`
- [ ] X-Auth-Token sin prefijo Bearer
- [ ] Todos los campos de `.copy()` existen en el UiState correspondiente
- [ ] availableRoles no incluye "god" ni "owner"
- [ ] Migrations con FK usan SET FOREIGN_KEY_CHECKS=0/1

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
