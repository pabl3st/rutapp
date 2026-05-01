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
├── RutasApp.kt                          @HiltAndroidApp
├── MainActivity.kt                      @AndroidEntryPoint, onExitApp = { finish() }
├── core/ui/theme/Theme.kt               Material3, colores, Spacing object
├── di/
│   └── NetworkModule.kt                 Hilt: Retrofit, OkHttp, Moshi, RutasApiService
├── data/
│   ├── network/RutasApiService.kt       Retrofit interface + todos los DTOs S01
│   ├── repository/AuthRepository.kt     Login, register, logout, verifySession offline-first
│   └── session/SessionManager.kt        Token cifrado (EncryptedSharedPrefs) + datos de sesión
├── feature/
│   └── auth/
│       ├── AuthViewModel.kt             MVVM, handleBack() único punto de verdad
│       └── AuthScreens.kt              Splash, ChooseType, Login, RegisterIndividual, RegisterCompany
└── navigation/
    ├── Screen.kt                        sealed class con rutas
    └── RutasNavGraph.kt                 NavHost, BackHandler en Home, PlaceholderScreen
```

### Módulos actuales
Solo existe `:app`. Los módulos feature se añaden por sprint cuando se crea su `build.gradle.kts`.
**No declarar módulos en `settings.gradle.kts` que no existan físicamente.**

---

## Estado por sprint

### S01 — COMPLETADO ✅
- Auth completo: register_individual, register_company, register_with_invite, login, logout, me, token_refresh, health
- SessionManager con EncryptedSharedPreferences
- Clean Architecture + MVVM base
- Navegación con BackHandler correcto en todos los casos
- CI/CD GitHub Actions: push → assembleDebug → artifact
- API PHP en servidor operativa, BD `cqvkelal_rutasapp_android` creada y migrada
- Usuarios en BD: `Pablo` (id=1, owner) y `god` (id=2, owner, para testing)

### S02 — COMPLETADO ✅
- Room: RouteEntity, StopEntity, SyncQueueEntity + DAOs + RutasDatabase
- SyncWorker (WorkManager + HiltWorker) + SyncRepository
- Endpoints API: routes_list, delta_sync, batch_sync + migration_v2.sql
- HomeScreen real con lista de rutas del día + estado sync
- RouteRepository + SyncRepository offline-first
- Firebase FCM + Crashlytics + Analytics + RutasMessagingService + FcmTokenRepository

### S03+ — PENDIENTE
- S03: RutasScreen + GPS ForegroundService
- S04: Formularios de visita con fotos
- S05: KPIs
- S06: Calendario
- S07: Admin panel
- S08+: Multi-tenancy, roles en UI, FCM push

---

## Clases y contratos existentes — NO duplicar

### `AuthResult<T>` (en AuthRepository.kt)
```kotlin
sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val code: Int = 0) : AuthResult<Nothing>()
}
```
Usar este mismo sealed class para todos los repositorios nuevos.

### `SessionManager` — propiedades disponibles
```kotlin
session.token           // String? — token JWT cifrado
session.isLoggedIn      // Boolean
session.userId          // Int
session.userName        // String
session.userEmail       // String
session.userRole        // String ("owner"|"admin"|"manager"|"agent"|"viewer")
session.userDisplayName // String
session.accountId       // Int
session.accountType     // String ("individual"|"company")
session.accountName     // String
session.isCompany       // Boolean
session.deviceId        // String — ANDROID_ID
session.lastSyncTimestamp // String — ISO8601, usado para delta_sync
session.saveAuth(...)   // persiste todos los campos de sesión
session.clear()         // logout completo
```

### `RutasApiService` — endpoints existentes S01
```kotlin
api.registerIndividual(action, body)
api.registerCompany(action, body)
api.registerWithInvite(action, body)
api.login(action, body)
api.logout(action, token, body)
api.me(action, token)
api.health(action)
```
La constante `API_PATH = "rutasproapk/api.php"` está en `RutasApiService.kt`.
Todos los nuevos endpoints usan el mismo path con `@Query("action")`.

### `Screen` — rutas de navegación existentes
```kotlin
Screen.Auth, Screen.Home, Screen.Rutas, Screen.Mapa,
Screen.Kpis, Screen.Calendario, Screen.Perfil, Screen.Admin,
Screen.Visita (con param stopUid)
```

### `NetworkModule` — BASE_URL
```kotlin
private const val BASE_URL = "https://mejoresiagratis.com/"
```
No cambiar. Los paths de endpoint son relativos desde aquí.

---

## Reglas de desarrollo

### Patrón offline-first obligatorio
Todo repositorio que acceda a datos de negocio DEBE:
1. Leer primero de Room (respuesta inmediata)
2. Lanzar sync en background si hay red
3. Emitir `Flow<T>` para que la UI se actualice automáticamente cuando llegan datos del servidor

### Inyección de dependencias
- Todos los repositorios: `@Singleton` con `@Inject constructor`
- I/O en constructores Singleton: siempre `by lazy {}` (ver ERROR 2)
- Room database: `by lazy {}` en el módulo Hilt

### Convenciones de código
- Un ViewModel por feature screen
- `UiState` data class por ViewModel (igual que `AuthUiState`)
- `StateFlow<UiState>` expuesto como `val ui: StateFlow<UiState>`
- Funciones de mutación en ViewModel, nunca en Composable
- `@JsonClass(generateAdapter = true)` en todos los DTOs de Moshi
- `@Json(name = "snake_case")` para campos con nombre distinto al JSON

---

## Errores históricos de build — NO repetir

### ERROR 1: @OptIn faltante en APIs experimentales de Material 3
APIs que requieren `@OptIn(ExperimentalMaterial3Api::class)`:
`TopAppBar`, `LargeTopAppBar`, `MediumTopAppBar`, `ModalBottomSheet`,
`rememberModalBottomSheetState`, `SearchBar`, `DockedSearchBar`,
`SwipeToDismissBox`, `rememberSwipeToDismissBoxState`, `TooltipBox`,
`DatePicker`, `TimePicker`, `ExposedDropdownMenuBox`

```kotlin
// Opción preferida — nivel de fichero:
@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.mipantalla
```

### ERROR 2: I/O en constructor de @Singleton
```kotlin
// NUNCA — crash en Application.onCreate() en hilo principal:
@Singleton class MiRepo @Inject constructor(ctx: Context) {
    private val db = Room.databaseBuilder(...).build()  // CRASH
}

// SIEMPRE lazy:
@Singleton class MiRepo @Inject constructor(ctx: Context) {
    private val db by lazy { Room.databaseBuilder(...).build() }
}
```

### ERROR 3: Retrofit baseUrl sin trailing slash
```kotlin
// ROMPE: .baseUrl("https://mejoresiagratis.com/rutasproapk/api.php")
// CORRECTO: .baseUrl("https://mejoresiagratis.com/")
```

### ERROR 4: Regex con backslash en .kts
```kotlin
// ROMPE: includeGroupByRegex("com\.android.*")
// CORRECTO: includeGroupByRegex("""com\.android.*""")
```

### ERROR 5: Módulos en settings.gradle.kts sin build.gradle.kts
Solo declarar `include(":modulo")` cuando existe `modulo/build.gradle.kts`.

### ERROR 6: security-crypto versión incorrecta
```toml
securityCrypto = "1.1.0"   # CORRECTO
# NO: 1.0.0 (sin MasterKeys), NO: alphas
```

### ERROR 7: Iconos mipmap faltantes
Densidades requeridas: `mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi` + `anydpi-v26`

### ERROR 8: gradle-wrapper.jar faltante
Subir el binario real (43KB), no un stub.

### ERROR 9: Caracteres UTF-8 no estándar en PHP
Non-breaking spaces (`U+00A0`, bytes `\xc2\xa0`) y em-dashes (`U+2014`) en comentarios
o alineación de código PHP causan `PHP Parse error: syntax error, unexpected identifier`.
Al generar o editar `api.php`, siempre limpiar con:
```python
content = content.replace(b'\xc2\xa0', b' ')
content = content.replace(b'\xe2\x80\x94', b'--')
```

### ERROR 10: prefs como array en lugar de objeto (PHP → Kotlin)
`json_decode($json, true)` en PHP convierte `{}` en `array()`, que serializa como `[]`.
Moshi en Kotlin espera `Map<String,Any>` (objeto JSON `{}`).
```php
// CORRECTO — forzar cast a objeto:
'prefs' => $row['prefs'] ? (object)json_decode($row['prefs'], true) : (object)[],
```

---

### ERROR 11: Plugins TOML fuera de su sección
**Build Firebase** — `Invalid TOML catalog definition: Unexpected type for bundle`
Al añadir entries al final de `libs.versions.toml` con scripts, pueden acabar
fuera de `[plugins]` si el script concatena después de `[bundles]`.

```toml
# ROMPE — si esto queda en [bundles]:
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }

# CORRECTO — siempre dentro de [plugins], antes de [bundles]:
[plugins]
...
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }

[bundles]
...
```
Regla: al modificar el TOML con scripts, verificar siempre la posición relativa
a las secciones `[versions]`, `[libraries]`, `[plugins]`, `[bundles]`.

---

### ERROR 12: Composables privados no pueden capturar lambdas del scope exterior
**Build S03** — `Unresolved reference 'onRouteClick'`
Una función `@Composable private fun` es un scope cerrado. No puede capturar
variables o lambdas del composable padre. Toda dependencia debe declararse como parámetro.

```kotlin
// ROMPE — RouteCard captura onRouteClick del padre sin recibirlo:
@Composable
fun HomeScreen(onRouteClick: (String) -> Unit) {
    RoutesList(routes)
}
@Composable
private fun RoutesList(routes: List<RouteEntity>) {
    RouteCard(route)           // RouteCard usa onRouteClick — ERROR
}
@Composable
private fun RouteCard(route: RouteEntity) {
    Card(modifier = Modifier.clickable { onRouteClick(route.uid) })  // FALLA
}

// CORRECTO — propagar el lambda por toda la cadena:
@Composable
fun HomeScreen(onRouteClick: (String) -> Unit) {
    RoutesList(routes, onRouteClick)
}
@Composable
private fun RoutesList(routes: List<RouteEntity>, onRouteClick: (String) -> Unit) {
    RouteCard(route, onClick = { onRouteClick(route.uid) })
}
@Composable
private fun RouteCard(route: RouteEntity, onClick: () -> Unit) {
    Card(modifier = Modifier.clickable(onClick = onClick))  // OK
}
```

Regla: al añadir un lambda a una función, buscar TODOS los composables privados
que la llaman en la cadena y propagar el parámetro hasta el último que lo use.

---

## Protocolo de diagnóstico ante errores de build

Cuando se recibe un log con errores de compilación o runtime:

**1. Leer TODOS los errores del log, no solo el primero.**
Un log puede contener N errores. Leer el fichero completo antes de aplicar cualquier fix.

**2. Antes de fixear, auditar el patrón en todos los ficheros afectados.**
Si el error es "Unresolved reference X en FileA.kt", buscar el mismo patrón en
FileB.kt, FileC.kt y todos los ficheros modificados en el mismo commit.

**3. Propagar fixes derivados.**
Si un fix cambia la firma de una función (añade parámetro, cambia tipo), buscar
TODAS las llamadas a esa función en el proyecto y actualizarlas en el mismo commit.
No hacer commits parciales que rompan otros ficheros.

**4. Un solo commit con todos los fixes.**
Nunca subir un fix que resuelve el error reportado pero deja otros errores latentes.
Verificar con grep/auditoría antes de commitear.

**Checklist de auditoría ante error de compilación:**
- [ ] Leer log completo (no solo primera línea de error)
- [ ] Grep del patrón roto en todos los .kt del proyecto
- [ ] Si se cambia firma de función: grep de todas sus llamadas
- [ ] Si es error de scope en Composable: auditar toda la cadena padre→hijo
- [ ] Verificar balance de llaves `{` vs `}` en ficheros modificados
- [ ] Un commit con TODOS los fixes, no uno por error

---

### ERROR 13: Imports duplicados por edición con scripts replace
**Build S04** — `Conflicting import: imported name 'Font' is ambiguous`
Editar imports con scripts `replace` encadenados puede insertar duplicados
cuando el import ya existe en el fichero.

```kotlin
// ROMPE — si Font ya existe y el script añade otro:
import androidx.compose.ui.text.font.Font   // ya estaba
import androidx.compose.ui.text.font.Font   // añadido por script → conflicto
```

**Regla:** cuando un fichero necesita cambios en imports Y en el cuerpo,
reescribirlo COMPLETO desde cero en lugar de aplicar múltiples `replace`.
Los scripts de replace encadenados acumulan errores invisibles.

**Checklist antes de commit con ficheros editados por script:**
- Leer el fichero resultante completo
- `grep "^import" fichero | sort | uniq -d` para detectar duplicados
- Si hay más de 3 replace sobre el mismo fichero → reescribir completo

---

## Flujo de commit obligatorio — Git Data API

**NUNCA** `PUT /contents/{file}` en bucle (genera N commits, N builds CI).

**SIEMPRE** Git Data API atómica:
```
1. POST /git/blobs      → sha por cada fichero modificado
2. POST /git/trees      → base_tree = SHA HEAD actual + todos los blobs
3. POST /git/commits    → apunta al nuevo tree
4. PATCH /git/refs/heads/main → actualiza la rama
```
Resultado: 1 commit, 1 build CI.

---

## Checklist pre-commit

- [ ] Toda función con API experimental Material3 tiene `@OptIn` (o `@file:OptIn`)
- [ ] Ningún `@Singleton` hace I/O en constructor — usar `by lazy {}`
- [ ] `BASE_URL` de Retrofit termina en `/`
- [ ] Regex en `.kts` usan triple-quote
- [ ] Solo módulos con `build.gradle.kts` están en `settings.gradle.kts`
- [ ] `security-crypto = "1.1.0"`
- [ ] KSP version empieza igual que Kotlin version
- [ ] `api.php` generado sin caracteres UTF-8 no estándar
- [ ] `prefs` en PHP devuelve `(object)` no `array()`
- [ ] Commit es atómico (Git Data API, no PUT en bucle)
- [ ] No hay clases/funciones duplicadas respecto al código existente
- [ ] Nuevos repositorios usan `AuthResult<T>` del mismo sealed class

---

## Metodología de trabajo

1. **Antes de cada sprint**: leer todos los `.kt` relevantes del repo + `api.php` completo
2. **Escribir plan detallado** con rutas exactas de ficheros antes de ejecutar
3. **Revisión del plan** por el usuario antes de escribir código
4. **Commits atómicos** por tarea lógica
5. **Verificar CI verde** antes de dar tarea por terminada




---

### ERROR 14: isZoomControlsEnabled / isZoomButtonsEnabled no existen en MapLibre 11.x
**Build S05** — `Unresolved reference 'isZoomControlsEnabled'` / `isZoomButtonsEnabled`
MapLibre Android 11.x **no tiene botones de zoom** en `UiSettings`. Ni `isZoomControlsEnabled`
ni `isZoomButtonsEnabled` son propiedades válidas.

El control de zoom del usuario se hace exclusivamente con:
```kotlin
map.uiSettings.isZoomGesturesEnabled = true   // pinch, doble tap
```

Si se necesitan botones +/- visuales, deben implementarse como Composables propios
sobre el mapa, llamando a `map.animateCamera(CameraUpdateFactory.zoomIn())`.

---

### ERROR 15: Archivos de fuente corruptos en res/font/ → crash "Could not load font"
**Runtime crash al inicio** — `IllegalStateException: Could not load font`
Los archivos `dm_sans_medium.ttf`, `dm_sans_semibold.ttf`, `dm_sans_bold.ttf` eran stubs
con bytes `0a0a0a0a` (newlines), no TTFs reales. Android los carga síncronamente en el
primer frame de Compose y crashea inmediatamente.

**Diagnóstico:**
```python
with open("res/font/archivo.ttf", "rb") as f:
    magic = f.read(4).hex()
# TTF válido: 00010000 o 74727565 (true) o 4f54544f (OTF)
# Corrupto:   0a0a0a0a (newlines/stub) → CRASH
```

**Fix:** sustituir por el variable font real con eje `wght`.
DM Sans variable: `https://github.com/google/fonts/raw/main/ofl/dmsans/DMSans%5Bopsz%2Cwght%5D.ttf`

**Regla:** al añadir fuentes al proyecto, verificar siempre los magic bytes antes de commit.
