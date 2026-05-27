# Modelo Híbrido de Delegación de Rutas — Implementation Plan

**Goal:** Implementar el modelo de propiedad por nivel + delegación en cadena: rutas con dueño actual (owner/admin/manager/agent), delegables al siguiente nivel inferior, con visibilidad ascendente y edición descendente. Las rutas importadas entran como `unassigned` y se delegan paso a paso.

**Architecture:** No se duplica la ruta por nivel — sigue siendo una sola fila en `routes`. Se añade un campo `assignment_state` que indica en qué punto del flujo está la ruta. El `user_id` actual mantiene siempre al "dueño actual" del nivel más bajo al que se ha delegado. Cuando se delega, el `user_id` pasa al nuevo dueño, el `assignment_state` avanza, y queda traza en `route_assignments` (tabla que ya existe desde Task 10). La visibilidad por jerarquía ya está implementada en `RouteRepository.observeAll()` — solo se complementa con el filtro por `assignment_state` cuando se quiere ver una bandeja concreta ("sin asignar", "delegadas", "en ejecución").

**Tech Stack:** Kotlin + Jetpack Compose + Room + Retrofit (cliente). PHP 8.1 + MariaDB 10.6 (servidor). Las decisiones del modelo descansan sobre tres puntos firmes ya verificados: `route_assignments` existe en producción (migración v14), la jerarquía `users.manager_id` está consistente, y el filtro `observeAll` por rol funciona como se espera.

**Decisiones cerradas con el producto:**
- Modelo aprobado: híbrido con dueño actual + delegación por nivel.
- "Editar en general" interpretado como: reasignar destinatario, mover stops entre rutas, cambiar fechas, modificar orden de visita, marcar como cancelada. Dividir/fusionar NO entra en este plan.
- La importación se hace DESPUÉS de implementar este modelo, no antes.

---

## Estados de `assignment_state`

| Estado | Significado | Quién es el `user_id` |
|---|---|---|
| `unassigned` | Recién creada/importada por admin u owner. Pendiente de bajar a un manager. | El admin/owner que la creó |
| `delegated_to_manager` | Delegada por admin a un manager. Pendiente de bajar a un agente. | El manager |
| `assigned_to_agent` | Delegada por manager a un agente. Lista para ejecutar. | El agente |
| `in_execution` | El agente ya ha empezado a visitar stops. | El agente |
| `done` | Todos los stops visitados o ruta cerrada. | El agente |
| `cancelled` | Cancelada por cualquier nivel superior. | Último dueño conocido |

**Regla de transición:** solo se puede avanzar al estado siguiente, salvo `cancelled` que se puede aplicar desde cualquier estado. Devolver una ruta a un nivel superior (rechazo) no entra en este plan — se gestiona con una nueva delegación del nivel superior al destino correcto.

---

## File Structure

**Servidor (toca `api.php`):**
- `rutasproapk/api.php` — endpoint `delegate_route` nuevo, ajuste de `create_route` y `routes_list` para incluir `assignment_state`.
- `rutasproapk/migration_v15_assignment_state.sql` — nuevo, añade columna `assignment_state` a `routes`.

**Android — capa de datos:**
- `RouteEntity.kt` — añade campo `assignmentState`.
- `RutasDatabase.kt` — versión 15, `MIGRATION_14_15` con `ALTER TABLE`.
- `DatabaseModule.kt` — registrar la migración.
- `RouteDao.kt` — queries de bandeja por estado.
- `RouteDto.kt` (dentro de `RutasApiService.kt`) — añadir campo del DTO.
- `DtoMappers.kt` — mapear el nuevo campo.
- `RouteRepository.kt` — método `delegateRoute(uid, newAssigneeId, reason)`.
- `RutasApiService.kt` — endpoint `delegateRoute`.

**Android — UI:**
- `RutasScreen.kt` y `RutasViewModel.kt` — filtros de bandeja por estado.
- `RouteDetailScreen.kt` — botón "Delegar" según rol y estado.
- Nuevo `DelegateRouteDialog.kt` — selector del destinatario del siguiente nivel.

**Importador:**
- `ImportarViewModel.kt` línea 744-746 — cambiar default: si quien importa es admin/owner y no eligió destinatario, la ruta entra como `unassigned`, no auto-asignada.

---

## Task 1: Esquema de servidor — columna `assignment_state`

**Files:**
- Create: `rutasproapk/migration_v15_assignment_state.sql`

- [ ] **Step 1: Escribir la migración SQL**

```sql
-- migration_v15_assignment_state.sql
-- Añade el estado de asignación al flujo de delegación de rutas.

ALTER TABLE routes
ADD COLUMN IF NOT EXISTS assignment_state
  ENUM('unassigned','delegated_to_manager','assigned_to_agent','in_execution','done','cancelled')
  NOT NULL DEFAULT 'assigned_to_agent'
  AFTER status;

-- Migración de datos existentes:
-- Rutas que ya tienen un user_id asignado se consideran 'assigned_to_agent'
-- por defecto (es lo que ya estaba pasando antes con la cascada del importer).
-- Las rutas en estado 'done' o 'cancelled' del campo status mantienen ese estado
-- también en assignment_state.
UPDATE routes
SET assignment_state = 'done'
WHERE status = 'done';

UPDATE routes
SET assignment_state = 'cancelled'
WHERE status = 'cancelled';

INSERT INTO schema_migrations (version, applied_at)
VALUES ('v15', NOW())
ON DUPLICATE KEY UPDATE applied_at = applied_at;
```

- [ ] **Step 2: Subir el fichero al servidor**

Ejecutar la migración en phpMyAdmin sobre la BD `cqvkelal_rutasapp_android`.
Verificar:

```sql
SHOW COLUMNS FROM routes LIKE 'assignment_state';
-- Debe devolver: assignment_state, enum(...), NO, '', 'assigned_to_agent', ''

SELECT assignment_state, COUNT(*) FROM routes GROUP BY assignment_state;
-- Debe devolver al menos 1 fila con assigned_to_agent.
```

- [ ] **Step 3: Commit**

```bash
git add rutasproapk/migration_v15_assignment_state.sql
git commit -m "feat(db): migration v15 — assignment_state para flujo de delegación de rutas"
```

---

## Task 2: API — exponer `assignment_state` en `routes_list` y `create_route`

**Files:**
- Modify: `rutasproapk/api.php`

- [ ] **Step 1: Añadir `assignment_state` al SELECT de `routes_list`**

Localizar en `api.php` el endpoint `routes_list` (busca `if ($action === 'routes_list')`). En la query SELECT, añadir el nuevo campo:

```php
// Antes:
$sql = "SELECT id, uid, account_id, user_id, name, date_assigned, status,
               notes, created_at, updated_at, scheduled_dates
        FROM routes
        WHERE account_id = ? AND deleted_at IS NULL";

// Después:
$sql = "SELECT id, uid, account_id, user_id, name, date_assigned, status,
               assignment_state, notes, created_at, updated_at, scheduled_dates
        FROM routes
        WHERE account_id = ? AND deleted_at IS NULL";
```

Y en el array de respuesta, añadir el campo:

```php
$routes[] = [
    'id'               => (int)$row['id'],
    'uid'              => $row['uid'],
    // ... campos existentes ...
    'assignment_state' => $row['assignment_state'],  // <-- añadir
    // ... resto ...
];
```

- [ ] **Step 2: Mismo cambio en `delta_sync`**

Buscar el endpoint `delta_sync` en `api.php` y aplicar el mismo añadido en el SELECT y en el array de respuesta de rutas.

- [ ] **Step 3: Ajustar `create_route` y `assign_route` para gestionar el estado**

En el endpoint `create_route`, determinar el estado inicial según quién crea la ruta:

```php
// Lógica: si el creador es owner/admin y no se especifica destinatario,
// la ruta entra como 'unassigned'. Si se especifica destinatario, depende
// del rol del destinatario.
$creatorRole = $user['role'];
$targetUserId = $input['target_user_id'] ?? null;

if ($targetUserId === null || $targetUserId === $user['id']) {
    // Sin destinatario o auto-asignación
    $assignmentState = ($creatorRole === 'agent') ? 'assigned_to_agent' : 'unassigned';
    $userIdToAssign = $user['id'];
} else {
    // Hay destinatario explícito — determinar estado por su rol
    $stmt = $pdo->prepare("SELECT role FROM users WHERE id = ? AND account_id = ?");
    $stmt->execute([$targetUserId, $user['account_id']]);
    $targetRole = $stmt->fetchColumn();
    $assignmentState = match($targetRole) {
        'manager' => 'delegated_to_manager',
        'agent'   => 'assigned_to_agent',
        default   => 'unassigned',  // si destino es admin/owner, sigue sin asignar
    };
    $userIdToAssign = $targetUserId;
}
```

Y en el INSERT añadir `assignment_state`:

```php
$stmt = $pdo->prepare("
    INSERT INTO routes (uid, account_id, user_id, name, date_assigned, status,
                        assignment_state, notes, scheduled_dates)
    VALUES (?, ?, ?, ?, ?, 'pending', ?, ?, ?)
");
$stmt->execute([
    $uid, $user['account_id'], $userIdToAssign, $name, $dateAssigned,
    $assignmentState, $notes, $scheduledDates,
]);
```

- [ ] **Step 4: Verificar manualmente con curl**

```bash
# Reemplazar TOKEN por uno válido de demo_owner
curl -s "https://mejoresiagratis.com/rutas-backup/api.php?action=routes_list&token=TOKEN" \
  | python3 -c "import sys,json; r=json.load(sys.stdin); print(r['routes'][0])"
```

Debe aparecer la clave `assignment_state` en cada ruta.

- [ ] **Step 5: Commit + ZIP**

```bash
git add rutasproapk/api.php
git commit -m "feat(api): exponer assignment_state en routes_list, delta_sync y create_route"
```

Y empaquetar `rutasproapk/api.php` + `migration_v15_assignment_state.sql` en un ZIP para que Pablo lo despliegue en el servidor.

---

## Task 3: Cliente — `RouteEntity` y migración Room

**Files:**
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/data/local/entity/RouteEntity.kt`
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/data/local/RutasDatabase.kt`
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/di/DatabaseModule.kt`

- [ ] **Step 1: Añadir `assignmentState` a `RouteEntity`**

En `RouteEntity.kt`, añadir el campo antes de `scheduledDates`:

```kotlin
val syncStatus: String = "pending",
val syncedAt: String?  = null,
/** Estado de delegación: unassigned, delegated_to_manager, assigned_to_agent,
 *  in_execution, done, cancelled. Default 'assigned_to_agent' por compatibilidad
 *  con rutas creadas antes de v15. */
val assignmentState: String = "assigned_to_agent",
val scheduledDates: List<String>? = null,
```

- [ ] **Step 2: Versión Room a 15 + `MIGRATION_14_15`**

En `RutasDatabase.kt`:

```kotlin
@Database(
    entities = [...],
    version  = 15,            // <-- antes 14
    exportSchema = false,
)
```

Añadir la migración:

```kotlin
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            ALTER TABLE routes
            ADD COLUMN assignmentState TEXT NOT NULL DEFAULT 'assigned_to_agent'
        """.trimIndent())
    }
}
```

- [ ] **Step 3: Registrar la migración en `DatabaseModule`**

```kotlin
.addMigrations(
    RutasDatabase.MIGRATION_10_11,
    RutasDatabase.MIGRATION_11_12,
    RutasDatabase.MIGRATION_12_13,
    RutasDatabase.MIGRATION_13_14,
    RutasDatabase.MIGRATION_14_15,   // <-- añadir
)
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/data/local/entity/RouteEntity.kt \
        app/src/main/kotlin/com/pabl3st/rutapp/data/local/RutasDatabase.kt \
        app/src/main/kotlin/com/pabl3st/rutapp/di/DatabaseModule.kt
git commit -m "feat(db): Room v15 — assignmentState en RouteEntity"
```

---

## Task 4: Mapeo DTO ↔ Entity

**Files:**
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/data/network/RutasApiService.kt`
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/data/repository/DtoMappers.kt`

- [ ] **Step 1: Añadir `assignmentState` al `RouteDto`**

En `RutasApiService.kt`, localizar la `data class RouteDto`:

```kotlin
data class RouteDto(
    val id:             Int,
    val uid:            String,
    // ... campos existentes ...
    @Json(name = "assignment_state") val assignmentState: String = "assigned_to_agent",
    // ... resto ...
)
```

- [ ] **Step 2: Copiar el campo en `toEntity()`**

En `DtoMappers.kt`:

```kotlin
fun RouteDto.toEntity(userId: Int, accountId: Int) = RouteEntity(
    // ... campos existentes ...
    assignmentState = assignmentState,
    // ... resto ...
)
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/data/network/RutasApiService.kt \
        app/src/main/kotlin/com/pabl3st/rutapp/data/repository/DtoMappers.kt
git commit -m "feat(data): mapear assignment_state DTO↔Entity"
```

---

## Task 5: API endpoint `delegate_route` y método cliente

**Files:**
- Modify: `rutasproapk/api.php`
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/data/network/RutasApiService.kt`
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/data/repository/RouteRepository.kt`

- [ ] **Step 1: Endpoint `delegate_route` en `api.php`**

Añadir al final de la cadena de `if ($action === ...)`:

```php
// ── Delegar ruta al siguiente nivel ──────────────────────────
if ($action === 'delegate_route') {
    requireAuth($pdo);
    $input = json_decode(file_get_contents('php://input'), true);
    $routeUid     = $input['route_uid']     ?? null;
    $newUserId    = (int)($input['new_user_id'] ?? 0);
    $reason       = $input['reason']        ?? null;
    if (!$routeUid || !$newUserId) jsonError('Faltan parámetros', 400);

    // Cargar ruta
    $stmt = $pdo->prepare("
        SELECT id, account_id, user_id, name, assignment_state
        FROM routes WHERE uid = ? AND deleted_at IS NULL
    ");
    $stmt->execute([$routeUid]);
    $route = $stmt->fetch();
    if (!$route) jsonError('Ruta no encontrada', 404);
    if ((int)$route['account_id'] !== (int)$user['account_id']) {
        jsonError('Ruta de otra cuenta', 403);
    }

    // Validar jerarquía: el delegante debe ser superior al actual dueño
    // o al menos del mismo nivel administrativo (admin/owner pueden saltar manager)
    $currentOwnerRole = $pdo->prepare("SELECT role FROM users WHERE id = ?");
    $currentOwnerRole->execute([$route['user_id']]);
    $currentRole = $currentOwnerRole->fetchColumn();

    $newOwnerRole = $pdo->prepare("SELECT role, account_id FROM users WHERE id = ?");
    $newOwnerRole->execute([$newUserId]);
    $newRow = $newOwnerRole->fetch();
    if (!$newRow || (int)$newRow['account_id'] !== (int)$user['account_id']) {
        jsonError('Destinatario inválido', 400);
    }
    $newRole = $newRow['role'];

    // Calcular nuevo estado según rol del destinatario
    $newState = match($newRole) {
        'manager' => 'delegated_to_manager',
        'agent'   => 'assigned_to_agent',
        default   => jsonError('No se puede delegar a ' . $newRole, 400),
    };

    // Aplicar
    $pdo->beginTransaction();
    try {
        $pdo->prepare("
            UPDATE routes
            SET user_id = ?, assignment_state = ?, updated_at = NOW()
            WHERE id = ?
        ")->execute([$newUserId, $newState, $route['id']]);

        // Trazabilidad — reutilizar route_assignments existente
        $pdo->prepare("
            INSERT INTO route_assignments
              (account_id, route_uid, route_name, from_user_id, to_user_id,
               assigned_by_id, reason)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        ")->execute([
            $user['account_id'], $routeUid, $route['name'],
            $route['user_id'], $newUserId, $user['id'], $reason,
        ]);

        $pdo->commit();
        jsonResponse(['ok' => true, 'new_state' => $newState]);
    } catch (Exception $e) {
        $pdo->rollBack();
        jsonError('Error al delegar: ' . $e->getMessage(), 500);
    }
}
```

- [ ] **Step 2: Endpoint cliente `delegateRoute` en `RutasApiService`**

```kotlin
@POST("api.php?action=delegate_route")
suspend fun delegateRoute(
    @Header("X-Auth-Token") token: String,
    @Body body: Map<String, Any?>,
): Response<DelegateResp>

data class DelegateResp(val ok: Boolean, val new_state: String? = null)
```

- [ ] **Step 3: Método `delegateRoute` en `RouteRepository`**

```kotlin
suspend fun delegateRoute(
    routeUid: String,
    newUserId: Int,
    reason: String? = null,
): Result<String> = runCatching {
    val token = session.token ?: error("Sin sesión activa")
    val resp = api.delegateRoute(
        token = token,
        body  = buildMap {
            put("route_uid", routeUid)
            put("new_user_id", newUserId)
            reason?.takeIf { it.isNotBlank() }?.let { put("reason", it) }
        },
    )
    if (!resp.isSuccessful) error("HTTP ${resp.code()}")
    val body = resp.body() ?: error("Respuesta vacía")
    if (!body.ok) error("Delegación rechazada")
    val newState = body.new_state ?: error("Estado no devuelto")
    // Actualizar Room local: cambiar user_id y assignment_state
    routeDao.updateAssignment(routeUid, newUserId, newState)
    triggerSync()
    newState
}
```

- [ ] **Step 4: Query `updateAssignment` en `RouteDao`**

```kotlin
@Query("""
    UPDATE routes
    SET userId = :newUserId,
        assignmentState = :newState,
        updatedAt = strftime('%Y-%m-%dT%H:%M:%fZ', 'now'),
        syncStatus = 'synced'
    WHERE uid = :routeUid
""")
suspend fun updateAssignment(routeUid: String, newUserId: Int, newState: String)
```

- [ ] **Step 5: Commit + ZIP del api.php**

```bash
git add rutasproapk/api.php \
        app/src/main/kotlin/com/pabl3st/rutapp/data/network/RutasApiService.kt \
        app/src/main/kotlin/com/pabl3st/rutapp/data/repository/RouteRepository.kt \
        app/src/main/kotlin/com/pabl3st/rutapp/data/local/dao/RouteDao.kt
git commit -m "feat: endpoint delegate_route y delegateRoute() en RouteRepository"
```

Empaquetar `rutasproapk/api.php` en un ZIP nuevo para Pablo.

---

## Task 6: Diálogo de delegación en UI

**Files:**
- Create: `app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/DelegateRouteDialog.kt`

- [ ] **Step 1: Crear el diálogo**

```kotlin
package com.pabl3st.rutapp.feature.rutas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pabl3st.rutapp.data.network.AccountUserDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelegateRouteDialog(
    routeName: String,
    candidates: List<AccountUserDto>,   // usuarios del siguiente nivel inferior
    candidateLevelLabel: String,        // "manager" o "agente"
    isLoading: Boolean,
    onConfirm: (userId: Int, reason: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedUserId by remember { mutableStateOf<Int?>(null) }
    var reason         by remember { mutableStateOf("") }
    var expanded       by remember { mutableStateOf(false) }

    val selectedName = candidates.firstOrNull { it.userId == selectedUserId }
        ?.let { "${it.displayName} (${it.role})" }
        ?: "Selecciona $candidateLevelLabel"

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Delegar \"$routeName\"") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Destinatario ($candidateLevelLabel)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        candidates.forEach { user ->
                            DropdownMenuItem(
                                text = { Text("${user.displayName} (${user.role})") },
                                onClick = {
                                    selectedUserId = user.userId
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo (opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedUserId?.let { onConfirm(it, reason.trim().ifBlank { null }) } },
                enabled = !isLoading && selectedUserId != null,
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Delegar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancelar") }
        },
    )
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/DelegateRouteDialog.kt
git commit -m "feat(ui): DelegateRouteDialog para delegación al siguiente nivel"
```

---

## Task 7: Integrar diálogo en `RouteDetailScreen`

**Files:**
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RouteDetailScreen.kt`
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RouteDetailViewModel.kt`

- [ ] **Step 1: Lógica de "¿puede delegar?" en el ViewModel**

En `RouteDetailViewModel`, añadir:

```kotlin
// El usuario puede delegar si:
//  - Es owner/admin y la ruta está unassigned o delegated_to_manager
//  - Es manager y la ruta está delegated_to_manager con él como dueño actual
// En ambos casos: el destinatario es del nivel inmediatamente inferior.
data class DelegationContext(
    val canDelegate:     Boolean,
    val nextLevelRole:   String?,            // "manager" o "agent"
    val candidates:      List<AccountUserDto>,
    val candidateLabel:  String,             // "manager" o "agente"
)

fun computeDelegationContext(
    route: RouteEntity,
    myRole: String,
    myId: Int,
    teamMembers: List<AccountUserDto>,
): DelegationContext {
    val isOwnerOrAdmin = myRole in listOf("owner", "admin", "god")
    val isManager      = myRole == "manager"
    val state          = route.assignmentState

    return when {
        // Owner/admin pueden delegar rutas unassigned a managers
        isOwnerOrAdmin && state == "unassigned" -> DelegationContext(
            canDelegate     = true,
            nextLevelRole   = "manager",
            candidates      = teamMembers.filter { it.role == "manager" },
            candidateLabel  = "manager",
        )
        // Owner/admin pueden re-delegar entre managers
        isOwnerOrAdmin && state == "delegated_to_manager" -> DelegationContext(
            canDelegate     = true,
            nextLevelRole   = "manager",
            candidates      = teamMembers.filter { it.role == "manager" },
            candidateLabel  = "manager",
        )
        // Manager dueño puede delegar a uno de sus agentes
        isManager && state == "delegated_to_manager" && route.userId == myId -> DelegationContext(
            canDelegate     = true,
            nextLevelRole   = "agent",
            candidates      = teamMembers.filter { it.role == "agent" && it.managerId == myId },
            candidateLabel  = "agente",
        )
        else -> DelegationContext(false, null, emptyList(), "")
    }
}
```

- [ ] **Step 2: Botón "Delegar" en el TopAppBar de `RouteDetailScreen`**

En el `TopAppBar` actions del detalle, añadir:

```kotlin
if (ui.delegationContext.canDelegate) {
    IconButton(onClick = vm::onShowDelegateDialog) {
        Icon(Icons.Default.PersonAdd, contentDescription = "Delegar ruta")
    }
}
```

Y la integración del diálogo al final del `Scaffold`:

```kotlin
if (ui.showDelegateDialog) {
    DelegateRouteDialog(
        routeName            = ui.route?.name.orEmpty(),
        candidates           = ui.delegationContext.candidates,
        candidateLevelLabel  = ui.delegationContext.candidateLabel,
        isLoading            = ui.isDelegating,
        onConfirm            = { userId, reason -> vm.delegateRoute(userId, reason) },
        onDismiss            = vm::onDismissDelegateDialog,
    )
}
```

- [ ] **Step 3: Métodos en el ViewModel**

```kotlin
fun onShowDelegateDialog()    = _ui.update { it.copy(showDelegateDialog = true) }
fun onDismissDelegateDialog() = _ui.update { it.copy(showDelegateDialog = false) }

fun delegateRoute(newUserId: Int, reason: String?) {
    val uid = _ui.value.route?.uid ?: return
    viewModelScope.launch {
        _ui.update { it.copy(isDelegating = true) }
        routeRepo.delegateRoute(uid, newUserId, reason).fold(
            onSuccess = { newState ->
                _ui.update { it.copy(
                    isDelegating       = false,
                    showDelegateDialog = false,
                    // El observe se encarga del refresco
                ) }
            },
            onFailure = { t ->
                _ui.update { it.copy(
                    isDelegating       = false,
                    error              = t.message ?: "Error al delegar",
                ) }
            },
        )
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RouteDetailScreen.kt \
        app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RouteDetailViewModel.kt
git commit -m "feat(ui): botón Delegar en RouteDetail según rol y estado"
```

---

## Task 8: Ajustar el importador — `unassigned` por defecto para admin/owner

**Files:**
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/feature/importar/ImportarViewModel.kt` líneas 744-746

- [ ] **Step 1: Cambiar el default del importador**

Antes:

```kotlin
val targetUserId = (_ui.value.targetUser
    ?: _ui.value.selectedManager
    ?: _ui.value.selectedAdmin)?.userId ?: session.userId
```

Después:

```kotlin
// Si el importador es admin/owner y no eligió destinatario, la ruta entra
// 'unassigned' (sin auto-asignarse) — habrá que delegarla manualmente.
// Si es manager, lo lógico es que la ruta sea para uno de sus agentes;
// si no eligió, se auto-asigna a sí mismo (estado delegated_to_manager).
val explicitTarget = _ui.value.targetUser
    ?: _ui.value.selectedManager
    ?: _ui.value.selectedAdmin
val targetUserId   = explicitTarget?.userId ?: session.userId
val isAdminOrOwner = session.userRole in listOf("owner", "admin", "god")
val shouldAutoAssign = !(isAdminOrOwner && explicitTarget == null)
```

Y en la llamada a `createRoute`:

```kotlin
routeRepo.createRoute(
    name           = entry.routeName,
    dateAssigned   = dateAssigned,
    scheduledDates = if (allDates.size > 1) allDates.drop(1) else null,
    forUserId      = if (shouldAutoAssign) targetUserId else null,
    leaveUnassigned = !shouldAutoAssign,   // <-- nuevo parámetro
)
```

- [ ] **Step 2: Soportar `leaveUnassigned` en `createRoute`**

En `RouteRepository.createRoute`:

```kotlin
suspend fun createRoute(
    name: String,
    dateAssigned: String,
    notes: String? = null,
    scheduledDates: List<String>? = null,
    forUserId: Int? = null,
    leaveUnassigned: Boolean = false,    // <-- nuevo
): RouteEntity {
    // ...
    val assignmentState = if (leaveUnassigned) "unassigned" else "assigned_to_agent"
    val route = RouteEntity(
        // ...
        assignmentState = assignmentState,
        // ...
    )
    // ...
}
```

Y reflejar en `routeToMap` para que el servidor reciba el estado correcto.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/feature/importar/ImportarViewModel.kt \
        app/src/main/kotlin/com/pabl3st/rutapp/data/repository/RouteRepository.kt
git commit -m "feat(importer): admin/owner importan rutas como unassigned por defecto"
```

---

## Task 9: Bandejas por estado en `RutasScreen`

**Files:**
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RutasScreen.kt`
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RutasViewModel.kt`

- [ ] **Step 1: Filtros de bandeja en el `RutasUiState`**

```kotlin
enum class RouteBucket(val label: String) {
    ALL("Todas"),
    UNASSIGNED("Sin asignar"),
    DELEGATED("Delegadas"),
    IN_EXECUTION("En ejecución"),
    DONE("Completadas"),
}

data class RutasUiState(
    // ... campos existentes ...
    val bucket: RouteBucket = RouteBucket.ALL,
)
```

- [ ] **Step 2: Filtrar `routes` por bucket en el VM antes de exponerlo a la UI**

```kotlin
private val filteredRoutes: List<RouteEntity> get() {
    val all = _ui.value.routes
    return when (_ui.value.bucket) {
        RouteBucket.ALL          -> all
        RouteBucket.UNASSIGNED   -> all.filter { it.assignmentState == "unassigned" }
        RouteBucket.DELEGATED    -> all.filter { it.assignmentState == "delegated_to_manager" }
        RouteBucket.IN_EXECUTION -> all.filter { it.assignmentState in listOf("assigned_to_agent", "in_execution") }
        RouteBucket.DONE         -> all.filter { it.assignmentState == "done" }
    }
}
```

(En la práctica esto se calcula al renderizar para mantener Flow reactivo, pero el patrón es ese.)

- [ ] **Step 3: Selector de bucket en `RutasScreen`**

Una `Row` horizontal con `FilterChip` justo debajo del TopAppBar:

```kotlin
Row(
    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
) {
    RouteBucket.entries.forEach { bucket ->
        FilterChip(
            selected = ui.bucket == bucket,
            onClick  = { vm.onBucketChange(bucket) },
            label    = { Text(bucket.label) },
        )
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RutasScreen.kt \
        app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RutasViewModel.kt
git commit -m "feat(ui): filtros de bandeja por assignment_state en RutasScreen"
```

---

## Task 10: Bump de versión y deploy

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Bump**

```kotlin
versionCode = 127
versionName = "1.0.0-s30"
```

- [ ] **Step 2: Commit + push de todo**

```bash
git add app/build.gradle.kts
git commit -m "chore: bump 127 — modelo híbrido de delegación"
git push origin main
```

- [ ] **Step 3: Verificación end-to-end**

1. Pablo despliega los dos ZIPs de API (Task 2 y Task 5) + la migración v15 (Task 1) al servidor.
2. Verifica con phpMyAdmin que `routes.assignment_state` existe.
3. Instala el APK 127.
4. Importa el XLS de `RutasApp_IMPORT_telco_2026-05.xlsx` como `demo_owner` SIN seleccionar destinatario.
5. Verifica que las 8 rutas aparecen en bandeja **"Sin asignar"**.
6. Abre cada una, pulsa **Delegar** → elige `demo_manager` → confirma.
7. Cambia de usuario a `demo_manager`. Las rutas aparecen en su bandeja **"Delegadas"** (con `assignment_state = delegated_to_manager`).
8. `demo_manager` abre una, pulsa **Delegar** → elige `demo_agent` → confirma.
9. Cambia a `demo_agent`. La ruta aparece en su bandeja **"En ejecución"** (estado `assigned_to_agent`).
10. `demo_agent` ejecuta la ruta normalmente (visitas, KPIs...).

---

## Lo que NO entra en este plan (registrado para futuro)

- **Editar rutas (mover stops, cambiar fechas) según jerarquía**: ya existen los métodos en `StopRepository` y `RouteRepository`. Lo que falta es ajustar la lógica de permisos en `api.php` para que un manager pueda editar stops de sus agentes. Es Task aparte, ~1 día.
- **Dividir/fusionar rutas**: descartado en estas decisiones.
- **Devolución de rutas (rechazo)**: descartado. Se gestiona con una nueva delegación del nivel superior al destino correcto.
- **Catálogo dinámico de KPIs en el importador**: pendiente, mencionado en la auditoría. Es lo que permitiría importar los KPIs telco completos (Llamaya, Másmovil, PLV, etc.). Plan aparte.

---

## Riesgos identificados

1. **Migración v15 en producción mientras la app vieja sigue activa**: el `api.php` nuevo enviará `assignment_state` que la app vieja ignorará (Moshi tolera campos extra). Compatibilidad hacia atrás OK. Riesgo: bajo.

2. **Rutas creadas en la app vieja que ya tienen `user_id` de agente** recibirán `assignment_state = 'assigned_to_agent'` por el DEFAULT de la migración. Eso es correcto.

3. **Si Pablo despliega `api.php` ANTES que la migración SQL**, el SELECT de `assignment_state` dará error 500. Por eso Task 1 (migración) va antes que Task 2 (api.php). En el ZIP final se entregarán por separado con instrucciones explícitas de orden.

4. **Bug Room v15 conocido**: si Pablo instala el APK 127 y luego un APK más viejo (con Room v14), la app crasheará al abrir. Lo mismo que pasó con el rollback de la barra de progreso. Aviso explícito.
