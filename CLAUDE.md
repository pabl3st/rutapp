# RutasApp Android — Reglas de build para Claude

Leer SIEMPRE antes de escribir cualquier fichero Kotlin, Compose o Gradle.
Este fichero acumula todos los errores reales que han roto builds anteriores.

---

## Errores que han ocurrido — no repetir

### ERROR 1: @OptIn faltante en APIs experimentales de Material 3
**Build #29-31** — `This material API is experimental and is likely to change`
El compilador Kotlin trata `@Experimental` como ERROR, no warning.

APIs de Material 3 que requieren `@OptIn(ExperimentalMaterial3Api::class)`:
- `TopAppBar` / `LargeTopAppBar` / `MediumTopAppBar`
- `ModalBottomSheet` / `rememberModalBottomSheetState`
- `SearchBar` / `DockedSearchBar`
- `SwipeToDismissBox` / `rememberSwipeToDismissBoxState`
- `TooltipBox`
- `DatePicker` / `TimePicker`
- `ExposedDropdownMenuBox`

Solución — poner en cada función que la use, O a nivel de fichero:
```kotlin
// Opción A: en la función
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiPantalla() { TopAppBar(...) }

// Opción B: nivel de fichero (mejor cuando hay varias en el mismo fichero)
@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.auth
```

---

### ERROR 2: I/O en constructor de @Singleton (Hilt)
**Build #22** — crash instantáneo al arrancar la app
Hilt construye @Singleton durante `Application.onCreate()` en el hilo principal.
`MasterKeys.getOrCreate()` y `EncryptedSharedPreferences.create()` hacen I/O de Keystore.

```kotlin
// NUNCA en el cuerpo del constructor:
@Singleton
class SessionManager @Inject constructor(ctx: Context) {
    private val prefs = EncryptedSharedPreferences.create(...)  // CRASH
}

// SIEMPRE lazy — se ejecuta la primera vez que se accede, desde coroutine:
@Singleton
class SessionManager @Inject constructor(ctx: Context) {
    private val prefs by lazy {
        try { EncryptedSharedPreferences.create(...) }
        catch (e: Exception) { ctx.getSharedPreferences("fallback", MODE_PRIVATE) }
    }
}
```

Mismo patrón para cualquier otra operación lenta en un Singleton:
Room.databaseBuilder(), OkHttpClient con certificados, etc.

---

### ERROR 3: Retrofit baseUrl sin trailing slash
**Build #22** — `IllegalArgumentException` al crear el Singleton de Retrofit

```kotlin
// ROMPE:
.baseUrl("https://mejoresiagratis.com/rutasproapk/api.php")

// CORRECTO — baseUrl siempre termina en "/":
.baseUrl("https://mejoresiagratis.com/")
// Los endpoints usan path relativo:
@GET("rutasproapk/api.php")
```

---

### ERROR 4: Regex con backslash en .kts (Kotlin DSL)
**Build #28** — `Illegal escape: '\.'`
En strings `"..."` de Kotlin, `\` es escape de Kotlin. Para regex usar raw strings.

```kotlin
// ROMPE en settings.gradle.kts:
includeGroupByRegex("com\.android.*")

// CORRECTO — raw string triple quote:
includeGroupByRegex("""com\.android.*""")
```

---

### ERROR 5: Módulos declarados sin build.gradle.kts
**Build #21** — `Project ':feature:auth' not found`
Cada `include(":modulo")` en settings.gradle.kts requiere que exista
el fichero `modulo/build.gradle.kts` en el repo.

Regla: solo declarar módulos cuando se crea su build.gradle.kts.
En S01 solo existe `:app`.

---

### ERROR 6: security-crypto versión incorrecta
**Build #29** — `Unresolved reference: MasterKeys`
`security-crypto 1.0.0` no tiene `MasterKeys` ni `EncryptedSharedPreferences`.

```toml
# CORRECTO:
securityCrypto = "1.1.0"

# API para 1.1.0:
val alias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
EncryptedSharedPreferences.create(name, alias, context, keyScheme, valueScheme)

# NO usar — requiere alpha:
MasterKey.Builder(context).setKeyScheme(...).build()
```

---

### ERROR 7: Iconos mipmap faltantes
**Build #16** — `resource mipmap/ic_launcher not found`
El Manifest referencia `@mipmap/ic_launcher` que debe existir en todas las densidades.

Densidades requeridas: mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi + anydpi-v26 (adaptive).

---

### ERROR 8: gradle-wrapper.jar faltante
**Build #21** — wrapper no puede arrancar
`gradlew` apunta a `gradle/wrapper/gradle-wrapper.jar` que debe existir en el repo.
Subir el binario real de Gradle 8.9 (43KB), no un stub.

---

## Versiones fijadas del proyecto

| Dependencia      | Versión       | Nota                                    |
|------------------|---------------|-----------------------------------------|
| AGP              | 8.5.2         | Compatible con Gradle 8.9               |
| Kotlin           | 2.0.21        | Con Compose compiler plugin integrado   |
| KSP              | 2.0.21-1.0.25 | DEBE coincidir prefijo con Kotlin       |
| Hilt             | 2.52          | —                                       |
| Compose BOM      | 2024.10.00    | Gestiona todas las versiones Compose    |
| security-crypto  | 1.1.0         | Ver ERROR 6                             |
| minSdk           | 26            | Android 8.0+                            |
| compileSdk       | 35            | —                                       |

---

## Checklist pre-push (ejecutar mentalmente antes de cada commit)

- [ ] Toda función con `TopAppBar`, `ModalBottomSheet`, `SearchBar`, `SwipeToDismissBox`
      tiene `@OptIn(ExperimentalMaterial3Api::class)`
- [ ] Ningún `@Singleton` hace I/O en el constructor — usar `by lazy {}`
- [ ] `baseUrl` de Retrofit termina en `/`
- [ ] Regex en `.kts` usan triple-quote `"""..."""`, no `"\\."` 
- [ ] Solo los módulos con `build.gradle.kts` están en `settings.gradle.kts`
- [ ] `security-crypto = "1.1.0"` (no 1.0.0, no alpha)
- [ ] KSP version empieza igual que Kotlin version
- [ ] Commit es atómico (Git Data API: blob → tree → commit → ref)

---

## Flujo de commit obligatorio

NUNCA usar `PUT /contents/{file}` en bucle — genera N builds, uno por fichero.

SIEMPRE usar Git Data API:
1. `POST /git/blobs` para cada fichero → obtener sha de blob
2. `POST /git/trees` con `base_tree` = SHA del HEAD actual + todos los blobs
3. `POST /git/commits` apuntando al nuevo tree
4. `PATCH /git/refs/heads/main` actualizando la rama

Resultado: 1 commit, 1 build.
