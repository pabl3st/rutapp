# Historial de Reasignación de Rutas — Plan de Implementación

> **Para quien ejecute:** las tareas usan checkbox `- [ ]` para seguimiento. Cada tarea produce un cambio autocontenido y commiteable.

**Objetivo:** Registrar cada reasignación de ruta (de quién, a quién, cuándo, quién lo hizo, por qué), permitir consultar el historial de cada ruta, y reasignar varias rutas a la vez.

**Arquitectura:** Tabla nueva `route_assignments` como log append-only en el servidor (nunca se borra ni edita — es un registro de auditoría). El endpoint `assign_route` existente escribe una fila ahí en cada reasignación. Un endpoint nuevo `assign_routes_bulk` reasigna en lote. El cliente Android lee el historial vía un endpoint `route_history` y lo muestra en RouteDetailScreen. La reasignación masiva vive en RutasScreen con modo selección.

**Stack:** PHP 8 + MySQL (servidor `api.php`), Kotlin + Jetpack Compose + Room + Retrofit + Hilt (Android).

**Decisiones de diseño para escalar:**
- `route_assignments` es append-only: no hay UPDATE ni DELETE. Esto la hace inmune a corrupción y trivial de auditar/replicar.
- Índice por `route_uid` y por `account_id, created_at` para que las consultas de historial y los listados por cuenta no degraden con millones de filas.
- El log guarda IDs Y nombres desnormalizados (`from_user_name`, `to_user_name`): un usuario puede borrarse o renombrarse y el historial debe seguir siendo legible. Es un registro histórico, no una vista en vivo.
- La reasignación masiva reutiliza exactamente la misma validación de jerarquía que la individual — una sola fuente de verdad para los permisos.

---

## Task 1: Migración SQL — tabla `route_assignments`

**Files:**
- Create: `rutasproapk/migration_v14_route_assignments.sql`

- [ ] **Step 1: Crear el fichero de migración**

```sql
-- migration_v14_route_assignments.sql
-- Log append-only de reasignaciones de ruta. Nunca se hace UPDATE/DELETE aquí.
CREATE TABLE IF NOT EXISTS route_assignments (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_id      INT             NOT NULL,
    route_uid       VARCHAR(50)     NOT NULL,
    route_name      VARCHAR(120)    NOT NULL,            -- desnormalizado: legible aunque la ruta se borre
    from_user_id    INT             NULL,                -- NULL = la ruta no tenia asignacion previa
    from_user_name  VARCHAR(120)    NULL,
    to_user_id      INT             NOT NULL,
    to_user_name    VARCHAR(120)    NOT NULL,
    assigned_by_id  INT             NOT NULL,            -- quien ejecuto la reasignacion
    assigned_by_name VARCHAR(120)   NOT NULL,
    reason          VARCHAR(255)    NULL,                -- motivo opcional
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_route   (route_uid),
    INDEX idx_account (account_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: Verificar la sintaxis del SQL**

Revisar a ojo: tipos coherentes con `routes` (`route_uid VARCHAR(50)` igual que en `routes.uid`), `account_id INT` igual que en el resto de tablas. No hay FK a `users` a propósito — el log debe sobrevivir al borrado de un usuario.

- [ ] **Step 3: Commit**

```bash
git add rutasproapk/migration_v14_route_assignments.sql
git commit -m "feat: migración v14 — tabla route_assignments (log de reasignaciones)"
```

---

## Task 2: API — `assign_route` registra en el log

**Files:**
- Modify: `rutasproapk/api.php` (endpoint `assign_route`, ~líneas 1960-2010)

- [ ] **Step 1: Localizar el UPDATE de la ruta en assign_route**

El endpoint ya hace `UPDATE routes SET user_id=?...`. Justo ANTES de ese UPDATE, `$routeRow` contiene la ruta (con `user_id` actual = el `from`) y `$targetRow` el destino (el `to`). Hay que capturar el nombre del `from` antes de sobrescribir.

- [ ] **Step 2: Insertar la escritura del log**

Añadir, justo después del `UPDATE routes SET user_id=?...` y su `execute`:

```php
    // ── Registrar en el historial de reasignaciones (append-only) ──
    // Nombre del agente anterior — puede ser NULL si la ruta no tenia asignacion
    $fromName = null;
    if (!empty($routeRow['user_id'])) {
        $fu = db()->prepare('SELECT name FROM users WHERE id=? LIMIT 1');
        $fu->execute([(int)$routeRow['user_id']]);
        $fromName = $fu->fetchColumn() ?: null;
    }
    $reason = isset($body['reason']) ? san($body['reason'], 255) : null;
    db()->prepare(
        'INSERT INTO route_assignments
            (account_id, route_uid, route_name,
             from_user_id, from_user_name, to_user_id, to_user_name,
             assigned_by_id, assigned_by_name, reason)
         VALUES (?,?,?,?,?,?,?,?,?,?)'
    )->execute([
        $aid, $routeUid, $routeRow['name'],
        $routeRow['user_id'] ?: null, $fromName,
        $newUserId, $targetRow['name'],
        $callerId, ($sess['name'] ?? 'Usuario'),
        $reason,
    ]);
```

- [ ] **Step 3: Verificar que `$sess['name']` existe**

`requireAuth()` devuelve el array de sesión. Comprobar con `grep -n "function requireAuth" rutasproapk/api.php` que incluye `name`. Si no lo incluye, leerlo con `SELECT name FROM users WHERE id=$callerId`. Usar el patrón que ya exista.

- [ ] **Step 4: Commit**

```bash
git add rutasproapk/api.php
git commit -m "feat: assign_route registra cada reasignación en route_assignments"
```

---

## Task 3: API — endpoint `route_history`

**Files:**
- Modify: `rutasproapk/api.php` (añadir endpoint nuevo, junto a `assign_route`)

- [ ] **Step 1: Añadir el endpoint**

Insertar como bloque `if ($action === 'route_history')` nuevo, después del cierre de `assign_route`:

```php
// ══════════════════════════════════════════════════════════════
// route_history — historial de reasignaciones de una ruta.
// Roles: manager+ (mismo umbral que assign_route).
// ══════════════════════════════════════════════════════════════
if ($action === 'route_history') {
    $sess = requireAuth();
    $aid  = (int)$sess['account_id'];
    $role = $sess['role'];
    if (roleLevel($role) < 3) err('Sin permisos', 403, $action);

    $routeUid = san($_GET['route_uid'] ?? ($body['route_uid'] ?? ''), 50);
    if ($routeUid === '') err('route_uid requerido', 400, $action);

    $st = db()->prepare(
        'SELECT id, route_uid, route_name,
                from_user_id, from_user_name, to_user_id, to_user_name,
                assigned_by_id, assigned_by_name, reason, created_at
         FROM route_assignments
         WHERE account_id=? AND route_uid=?
         ORDER BY created_at DESC, id DESC
         LIMIT 100'
    );
    $st->execute([$aid, $routeUid]);
    ok(['history' => $st->fetchAll(PDO::FETCH_ASSOC)]);
}
```

- [ ] **Step 2: Commit**

```bash
git add rutasproapk/api.php
git commit -m "feat: endpoint route_history — historial de reasignaciones de una ruta"
```

---

## Task 4: API — endpoint `assign_routes_bulk`

**Files:**
- Modify: `rutasproapk/api.php` (añadir endpoint nuevo, junto a `assign_route`)

- [ ] **Step 1: Extraer la validación de jerarquía a una función reutilizable**

Antes del bloque `if ($action === 'assign_route')`, definir un helper para no duplicar las reglas de permiso:

```php
/**
 * Valida que $caller puede reasignar a $targetUserId dentro de $aid.
 * Devuelve la fila del usuario destino, o llama a err() y corta.
 */
function validateAssignTarget(int $targetUserId, int $aid, string $callerRole, int $callerId): array {
    $st = db()->prepare('SELECT id, name, role, manager_id FROM users WHERE id=? AND account_id=? AND active=1 LIMIT 1');
    $st->execute([$targetUserId, $aid]);
    $row = $st->fetch();
    if (!$row) err('Usuario destino no encontrado', 404);
    if ($callerRole === 'manager' && (int)$row['manager_id'] !== $callerId) {
        err('Solo puedes asignar rutas a tus agentes directos', 403);
    }
    if ($callerRole === 'admin' && in_array($row['role'], ['owner','god'], true)) {
        err('No puedes asignar rutas a owner o god', 403);
    }
    return $row;
}
```

- [ ] **Step 2: Sustituir la validación inline de `assign_route` por la llamada al helper**

En `assign_route`, reemplazar el bloque que hace el `SELECT ... FROM users` del destino y los dos `if` de rol por:

```php
    $targetRow = validateAssignTarget($newUserId, $aid, $callerRole, $callerId);
```

(El resto de `assign_route` no cambia — `$targetRow['name']` etc. siguen disponibles.)

- [ ] **Step 3: Añadir el endpoint `assign_routes_bulk`**

Insertar después de `assign_route`:

```php
// ══════════════════════════════════════════════════════════════
// assign_routes_bulk — reasigna varias rutas al mismo usuario.
// Roles: manager+. Cada ruta genera su fila en route_assignments.
// ══════════════════════════════════════════════════════════════
if ($action === 'assign_routes_bulk') {
    $sess     = requireAuth();
    $callerId = (int)$sess['uid'];
    $callerRole = $sess['role'];
    $aid      = (int)$sess['account_id'];
    if (roleLevel($callerRole) < 3) err('Sin permisos para reasignar rutas', 403, $action);

    $routeUids = $body['route_uids'] ?? [];
    $newUserId = (int)($body['new_user_id'] ?? 0);
    $reason    = isset($body['reason']) ? san($body['reason'], 255) : null;
    if (!is_array($routeUids) || count($routeUids) === 0) err('route_uids requerido', 400, $action);
    if (!$newUserId) err('new_user_id requerido', 400, $action);
    if (count($routeUids) > 200) err('Máximo 200 rutas por lote', 400, $action);

    // Validar destino UNA vez — mismas reglas que la reasignación individual
    $targetRow = validateAssignTarget($newUserId, $aid, $callerRole, $callerId);
    $callerName = $sess['name'] ?? 'Usuario';

    $reassigned = 0; $skipped = [];
    foreach ($routeUids as $ru) {
        $ru = san($ru, 50);
        if ($ru === '') { continue; }
        $r = db()->prepare('SELECT id, user_id, name FROM routes WHERE uid=? AND account_id=? AND deleted_at IS NULL LIMIT 1');
        $r->execute([$ru, $aid]);
        $routeRow = $r->fetch();
        if (!$routeRow) { $skipped[] = $ru; continue; }

        db()->prepare('UPDATE routes SET user_id=?, updated_at=NOW() WHERE uid=? AND account_id=?')
            ->execute([$newUserId, $ru, $aid]);
        db()->prepare('UPDATE stops s JOIN routes r ON r.id=s.route_id SET s.updated_at=NOW() WHERE r.uid=? AND r.account_id=?')
            ->execute([$ru, $aid]);

        $fromName = null;
        if (!empty($routeRow['user_id'])) {
            $fu = db()->prepare('SELECT name FROM users WHERE id=? LIMIT 1');
            $fu->execute([(int)$routeRow['user_id']]);
            $fromName = $fu->fetchColumn() ?: null;
        }
        db()->prepare(
            'INSERT INTO route_assignments
                (account_id, route_uid, route_name, from_user_id, from_user_name,
                 to_user_id, to_user_name, assigned_by_id, assigned_by_name, reason)
             VALUES (?,?,?,?,?,?,?,?,?,?)'
        )->execute([
            $aid, $ru, $routeRow['name'], $routeRow['user_id'] ?: null, $fromName,
            $newUserId, $targetRow['name'], $callerId, $callerName, $reason,
        ]);
        $reassigned++;
    }

    apiLog($action, $callerId, $aid);
    ok(['reassigned' => $reassigned, 'skipped' => $skipped]);
}
```

- [ ] **Step 4: Commit**

```bash
git add rutasproapk/api.php
git commit -m "feat: endpoint assign_routes_bulk + helper validateAssignTarget reutilizable"
```

---

## Task 5: Android — DTOs y métodos de API

**Files:**
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/data/network/RutasApiService.kt`

- [ ] **Step 1: Añadir el DTO del registro de historial**

Junto a los otros DTOs (cerca de `KpiValueDto`):

```kotlin
@JsonClass(generateAdapter = true)
data class RouteAssignmentDto(
    val id:                Long   = 0,
    @Json(name = "route_uid")        val routeUid:       String = "",
    @Json(name = "route_name")       val routeName:      String = "",
    @Json(name = "from_user_id")     val fromUserId:     Int?   = null,
    @Json(name = "from_user_name")   val fromUserName:   String? = null,
    @Json(name = "to_user_id")       val toUserId:       Int    = 0,
    @Json(name = "to_user_name")     val toUserName:     String = "",
    @Json(name = "assigned_by_id")   val assignedById:   Int    = 0,
    @Json(name = "assigned_by_name") val assignedByName: String = "",
    val reason:            String? = null,
    @Json(name = "created_at")       val createdAt:      String = "",
)

@JsonClass(generateAdapter = true)
data class RouteHistoryResponse(
    val ok: Boolean,
    val history: List<RouteAssignmentDto> = emptyList(),
    val error: String? = null,
)

@JsonClass(generateAdapter = true)
data class BulkAssignResponse(
    val ok: Boolean,
    val reassigned: Int = 0,
    val skipped: List<String> = emptyList(),
    val error: String? = null,
)
```

- [ ] **Step 2: Añadir los métodos al interface RutasApiService**

Junto a `assignRoute`:

```kotlin
    @GET(API_PATH)
    suspend fun routeHistory(
        @Query("action")        action: String = "route_history",
        @Query("route_uid")     routeUid: String,
        @Header("X-Auth-Token") token: String,
    ): retrofit2.Response<RouteHistoryResponse>

    @POST(API_PATH)
    suspend fun assignRoutesBulk(
        @Query("action")        action: String = "assign_routes_bulk",
        @Header("X-Auth-Token") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
    ): retrofit2.Response<BulkAssignResponse>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/data/network/RutasApiService.kt
git commit -m "feat: DTOs y métodos API para historial y reasignación masiva"
```

---

## Task 6: Android — RouteRepository: historial y reasignación masiva

**Files:**
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/data/repository/RouteRepository.kt`

- [ ] **Step 1: Añadir `reason` opcional a `reassignRoute`**

`reassignRoute` (línea ~243) cambia su firma para aceptar un motivo opcional, y lo pasa en el body:

```kotlin
    suspend fun reassignRoute(
        routeUid: String,
        newUserId: Int,
        reason: String? = null,
    ): Result<Unit> = runCatching {
        val token = session.token ?: error("Sin sesión activa")
        val route = routeDao.getByUid(routeUid) ?: error("Ruta no encontrada: $routeUid")

        val response = api.assignRoute(
            token = token,
            body  = buildMap {
                put("route_uid", routeUid)
                put("new_user_id", newUserId)
                reason?.takeIf { it.isNotBlank() }?.let { put("reason", it) }
            },
        )
        if (!response.isSuccessful || response.body()?.ok != true) {
            error(response.body()?.error ?: "Error al reasignar en el servidor")
        }
        val now = Instant.now().atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        routeDao.upsert(route.copy(userId = newUserId, updatedAt = now, syncStatus = "synced"))
    }
```

(El cuerpo a partir de `val now` debe coincidir con lo que ya hace el método — ver líneas 256+ del fichero actual y conservar el `routeDao.upsert`/update existente.)

- [ ] **Step 2: Añadir `fetchRouteHistory`**

```kotlin
    /** Historial de reasignaciones de una ruta. Solo lectura — viene del servidor. */
    suspend fun fetchRouteHistory(routeUid: String): Result<List<RouteAssignmentDto>> = runCatching {
        val token = session.token ?: error("Sin sesión activa")
        val resp  = api.routeHistory(routeUid = routeUid, token = token)
        if (!resp.isSuccessful || resp.body()?.ok != true) {
            error(resp.body()?.error ?: "No se pudo cargar el historial")
        }
        resp.body()?.history ?: emptyList()
    }
```

- [ ] **Step 3: Añadir `reassignRoutesBulk`**

```kotlin
    /** Reasigna varias rutas al mismo usuario. Devuelve cuántas se reasignaron. */
    suspend fun reassignRoutesBulk(
        routeUids: List<String>,
        newUserId: Int,
        reason: String? = null,
    ): Result<Int> = runCatching {
        val token = session.token ?: error("Sin sesión activa")
        val resp  = api.assignRoutesBulk(
            token = token,
            body  = buildMap {
                put("route_uids", routeUids)
                put("new_user_id", newUserId)
                reason?.takeIf { it.isNotBlank() }?.let { put("reason", it) }
            },
        )
        if (!resp.isSuccessful || resp.body()?.ok != true) {
            error(resp.body()?.error ?: "Error en la reasignación masiva")
        }
        // Tras reasignar en el servidor, traer el estado actualizado a Room
        fetchDelta(forceFull = true)
        resp.body()?.reassigned ?: 0
    }
```

- [ ] **Step 4: Verificar imports**

`RouteAssignmentDto` viene de `com.pabl3st.rutapp.data.network`. Comprobar que el paquete está importado (probablemente ya, vía `api`/DTOs existentes).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/data/repository/RouteRepository.kt
git commit -m "feat: RouteRepository — fetchRouteHistory + reassignRoutesBulk + reason"
```

---

## Task 7: Android — historial en RouteDetailScreen

**Files:**
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RouteDetailViewModel.kt`
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RouteDetailScreen.kt`

- [ ] **Step 1: Añadir estado de historial al RouteDetailViewModel**

En el UiState del ViewModel, añadir:

```kotlin
    val history:        List<RouteAssignmentDto> = emptyList(),
    val showHistory:    Boolean                  = false,
    val loadingHistory: Boolean                  = false,
```

Y las funciones:

```kotlin
    fun onShowHistory() {
        _ui.update { it.copy(showHistory = true, loadingHistory = true) }
        viewModelScope.launch {
            routeRepo.fetchRouteHistory(routeUid)
                .onSuccess { h -> _ui.update { it.copy(history = h, loadingHistory = false) } }
                .onFailure { _ui.update { it.copy(loadingHistory = false) } }
        }
    }
    fun onDismissHistory() = _ui.update { it.copy(showHistory = false) }
```

- [ ] **Step 2: Añadir el botón "Historial" en el TopAppBar de RouteDetailScreen**

En `actions` del TopAppBar, junto a los iconos existentes, solo si el rol puede ver el equipo:

```kotlin
                if (UserRole.from(ui.userRole).canViewTeam) {
                    IconButton(onClick = vm::onShowHistory) {
                        Icon(Icons.Default.History, contentDescription = "Historial de asignación")
                    }
                }
```

- [ ] **Step 3: Añadir el diálogo de historial al final del Composable**

```kotlin
    if (ui.showHistory) {
        AlertDialog(
            onDismissRequest = vm::onDismissHistory,
            icon  = { Icon(Icons.Default.History, null) },
            title = { Text("Historial de asignación") },
            text  = {
                when {
                    ui.loadingHistory -> Box(
                        Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(Modifier.size(28.dp)) }
                    ui.history.isEmpty() -> Text("Esta ruta no se ha reasignado nunca.")
                    else -> Column(
                        Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ui.history.forEach { h ->
                            Column {
                                Text(
                                    "${h.fromUserName ?: "Sin asignar"} → ${h.toUserName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    "Por ${h.assignedByName} · ${h.createdAt.take(10)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                h.reason?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        "Motivo: $it",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = vm::onDismissHistory) { Text("Cerrar") }
            },
        )
    }
```

- [ ] **Step 4: Verificar el icono e imports**

`Icons.Default.History` está en el set core. Comprobar que `verticalScroll`, `rememberScrollState` y `RouteAssignmentDto` están importados (añadirlos si no).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RouteDetailViewModel.kt \
        app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RouteDetailScreen.kt
git commit -m "feat: RouteDetailScreen — diálogo de historial de reasignación"
```

---

## Task 8: Android — motivo opcional al reasignar (individual)

**Files:**
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RouteDetailViewModel.kt`
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RouteDetailScreen.kt`

- [ ] **Step 1: Localizar el diálogo de reasignación existente**

`grep -n "reassign\|Reasignar" RouteDetailScreen.kt` — hay un diálogo o selector que ya llama a `reassignRoute`. Añadir un `OutlinedTextField` opcional de motivo dentro de ese diálogo, con estado en el ViewModel:

```kotlin
    val reassignReason: String = "",
```
```kotlin
    fun onReassignReasonChange(v: String) = _ui.update { it.copy(reassignReason = v) }
```

- [ ] **Step 2: Pasar el motivo a reassignRoute**

Donde el ViewModel llama a `routeRepo.reassignRoute(routeUid, newUserId)`, cambiar a:

```kotlin
            routeRepo.reassignRoute(routeUid, newUserId, _ui.value.reassignReason)
```

Y limpiar `reassignReason = ""` tras el éxito.

- [ ] **Step 3: Añadir el campo de motivo en el diálogo de reasignación**

Dentro del `text` del diálogo de reasignar, debajo del selector de agente:

```kotlin
                OutlinedTextField(
                    value         = ui.reassignReason,
                    onValueChange = vm::onReassignReasonChange,
                    label         = { Text("Motivo (opcional)") },
                    placeholder   = { Text("Baja, vacaciones, reequilibrio…") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RouteDetailViewModel.kt \
        app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RouteDetailScreen.kt
git commit -m "feat: motivo opcional al reasignar una ruta"
```

---

## Task 9: Android — modo selección y reasignación masiva en RutasScreen

**Files:**
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RutasViewModel.kt`
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RutasScreen.kt`

- [ ] **Step 1: Añadir estado de selección al RutasViewModel**

En el UiState:

```kotlin
    val selectionMode:   Boolean        = false,
    val selectedUids:    Set<String>    = emptySet(),
    val showBulkDialog:  Boolean        = false,
    val bulkReason:      String         = "",
    val bulkBusy:        Boolean        = false,
```

Funciones:

```kotlin
    fun toggleSelectionMode() = _ui.update {
        it.copy(selectionMode = !it.selectionMode, selectedUids = emptySet())
    }
    fun toggleSelected(uid: String) = _ui.update {
        val s = it.selectedUids.toMutableSet()
        if (!s.add(uid)) s.remove(uid)
        it.copy(selectedUids = s)
    }
    fun onShowBulkDialog()    = _ui.update { it.copy(showBulkDialog = true) }
    fun onDismissBulkDialog() = _ui.update { it.copy(showBulkDialog = false, bulkReason = "") }
    fun onBulkReasonChange(v: String) = _ui.update { it.copy(bulkReason = v) }

    fun confirmBulkReassign(newUserId: Int) {
        val uids = _ui.value.selectedUids.toList()
        if (uids.isEmpty()) return
        _ui.update { it.copy(bulkBusy = true) }
        viewModelScope.launch {
            routeRepo.reassignRoutesBulk(uids, newUserId, _ui.value.bulkReason)
                .onSuccess { n ->
                    _ui.update { it.copy(
                        bulkBusy = false, showBulkDialog = false, selectionMode = false,
                        selectedUids = emptySet(), bulkReason = "",
                        message = "$n ruta(s) reasignada(s)",
                    ) }
                }
                .onFailure { e ->
                    _ui.update { it.copy(bulkBusy = false, error = e.message) }
                }
        }
    }
```

(Si el UiState no tiene campo `message`, usar el campo de feedback que ya exista, p. ej. `error`, o añadir `message: String?`.)

- [ ] **Step 2: Cargar la lista de agentes asignables**

El diálogo masivo necesita a quién asignar. Reutilizar el mismo origen que el importador (`teamRepo.teamOverview()`). Añadir al ViewModel un `availableAgents` cargado en `init`, idéntico al patrón de `KpisViewModel.loadAvailableAgents`. Si `RutasViewModel` no tiene `TeamRepository` inyectado, añadirlo al constructor.

- [ ] **Step 3: Botón de modo selección en el TopAppBar de RutasScreen**

Junto al icono de importar:

```kotlin
                if (UserRole.from(ui.userRole).canViewTeam) {
                    IconButton(onClick = vm::toggleSelectionMode) {
                        Icon(
                            if (ui.selectionMode) Icons.Default.Close else Icons.Default.Checklist,
                            contentDescription = if (ui.selectionMode) "Salir de selección" else "Seleccionar rutas",
                        )
                    }
                }
```

- [ ] **Step 4: Checkbox en cada tarjeta de ruta cuando selectionMode está activo**

En el item de ruta de la lista, cuando `ui.selectionMode` es true, mostrar un `Checkbox` a la izquierda; el click de la tarjeta pasa a alternar selección en vez de abrir la ruta:

```kotlin
        val isSelected = route.uid in ui.selectedUids
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (ui.selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { vm.toggleSelected(route.uid) },
                )
            }
            // ... tarjeta existente; en selectionMode su onClick = { vm.toggleSelected(route.uid) }
        }
```

- [ ] **Step 5: BottomBar de acción cuando hay selección**

Cuando `ui.selectionMode && ui.selectedUids.isNotEmpty()`, mostrar una barra inferior:

```kotlin
        if (ui.selectionMode && ui.selectedUids.isNotEmpty()) {
            BottomAppBar {
                Text(
                    "${ui.selectedUids.size} seleccionada(s)",
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                )
                Button(onClick = vm::onShowBulkDialog) {
                    Icon(Icons.Default.SwapHoriz, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Reasignar")
                }
            }
        }
```

- [ ] **Step 6: Diálogo de reasignación masiva**

```kotlin
    if (ui.showBulkDialog) {
        var picked by remember { mutableStateOf<Int?>(null) }
        AlertDialog(
            onDismissRequest = vm::onDismissBulkDialog,
            icon  = { Icon(Icons.Default.SwapHoriz, null) },
            title = { Text("Reasignar ${ui.selectedUids.size} ruta(s)") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Asignar a:", style = MaterialTheme.typography.labelMedium)
                    ui.availableAgents.forEach { agent ->
                        FilterChip(
                            selected = picked == agent.userId,
                            onClick  = { picked = agent.userId },
                            label    = { Text("${agent.name} (${agent.role})") },
                        )
                    }
                    OutlinedTextField(
                        value         = ui.bulkReason,
                        onValueChange = vm::onBulkReasonChange,
                        label         = { Text("Motivo (opcional)") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { picked?.let { vm.confirmBulkReassign(it) } },
                    enabled = picked != null && !ui.bulkBusy,
                ) { Text(if (ui.bulkBusy) "Reasignando…" else "Reasignar") }
            },
            dismissButton = {
                TextButton(onClick = vm::onDismissBulkDialog) { Text("Cancelar") }
            },
        )
    }
```

- [ ] **Step 7: Verificar iconos**

`Icons.Default.Checklist`, `Close`, `SwapHoriz` — comprobar que están en el set core usado (si `Checklist` no estuviera, usar `Icons.Default.DoneAll`).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RutasViewModel.kt \
        app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RutasScreen.kt
git commit -m "feat: reasignación masiva de rutas con modo selección en RutasScreen"
```

---

## Task 10: Verificación end-to-end y bump de versión

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Auditoría estática**

Revisar en los ficheros tocados: sin imports duplicados, `when` exhaustivos, sin símbolos sin definir, los DTOs `RouteAssignmentDto`/`BulkAssignResponse` usados coinciden con lo declarado en Task 5.

- [ ] **Step 2: Subir la migración al servidor**

El usuario ejecuta `migration_v14_route_assignments.sql` en phpMyAdmin. Verificar con `SHOW TABLES LIKE 'route_assignments'` y `DESCRIBE route_assignments`.

- [ ] **Step 3: Subir `api.php` al hosting cPanel**

- [ ] **Step 4: Prueba funcional**

Con un owner: reasignar una ruta individual con motivo → abrir Historial, ver la entrada. Activar modo selección, marcar 3 rutas, reasignar en masa → ver "3 rutas reasignadas" y comprobar el historial de cada una.

- [ ] **Step 5: Bump versionCode y commit final**

```bash
# subir versionCode en app/build.gradle.kts
git add app/build.gradle.kts
git commit -m "chore: bump versionCode — feature historial de reasignación"
git push origin main
```

---

## Auto-revisión del plan

**Cobertura del spec:**
- Log de reasignación → Task 1 (tabla) + Task 2 (escritura).
- Ver historial de cada ruta → Task 3 (API) + Task 7 (UI).
- Motivo de reasignación → Task 2/4 (servidor) + Task 8 (UI individual) + Task 9 (UI masiva).
- Quién la hizo → `assigned_by_id/name` en Task 1, capturado en Task 2/4, mostrado en Task 7.
- Filtros → el historial por ruta ya está acotado por `route_uid`; un filtro global por agente/fecha sería una vista nueva, fuera del alcance de "historial por ruta" — no incluido a propósito para no inflar el plan; se puede añadir como plan aparte.
- Reasignación masiva → Task 4 (API) + Task 9 (UI).

**Escalabilidad:** tabla append-only con índices por `route_uid` y `(account_id, created_at)`; nombres desnormalizados para sobrevivir a borrados; validación de jerarquía centralizada en `validateAssignTarget` (una sola fuente de verdad); lote limitado a 200 para no bloquear la BD.

**Consistencia de tipos:** `RouteAssignmentDto` definido en Task 5 y usado en Tasks 6-7; `reassignRoute`/`reassignRoutesBulk`/`fetchRouteHistory` definidos en Task 6 y usados en 7-9; `validateAssignTarget` definida en Task 4 Step 1 y usada en Task 4 Step 2.

**Nota de gap conocida:** un panel de auditoría global (todas las reasignaciones de la cuenta con filtros por agente y rango de fechas) NO está en este plan. La tabla `route_assignments` ya lo soporta a nivel de datos — sería solo un endpoint `assignments_list` + pantalla. Si se quiere, va como plan separado.
