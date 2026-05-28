# Modelo C — stop_visits (informes por visita) Implementation Plan

> **Para quien ejecute:** las tareas usan checkbox `- [ ]` para seguimiento. Cada tarea produce un commit autocontenido. Ejecutar **estrictamente en orden** — la BD debe estar lista antes que el cliente, el cliente antes que la UI.

**Goal:** Resolver el problema de visitas multi-fecha **sin duplicar stops**. Una parada PDV es una entidad única (149 paradas siguen siendo 149). Las visitas a esa parada son entidades separadas (`stop_visits`), una por fecha. KPIs y fotos se anclan a la visita concreta, no al stop.

**Architecture:** Nueva tabla `stop_visits` (servidor + Room) con PK compuesta `(stop_uid, visit_date)`. El stop conserva campos de "estado actual" (última visita), pero el histórico completo vive en `stop_visits`. La UI lee fechas disponibles de `route.scheduledDates`, no de `stop.dateAssigned`. El selector de fecha muestra TODAS las paradas de la ruta + el estado de la visita en esa fecha.

**Tech Stack:** Kotlin + Jetpack Compose + Room + Hilt + Retrofit + Moshi (cliente). PHP 8.1 + MariaDB 10.6 (servidor cPanel).

**Decisiones de diseño:**

- `stop_visits` es **append-only en sentido lógico**: cada visita es una fila inmutable salvo edición explícita. Se sincroniza bidireccional como cualquier entidad.
- El stop conserva `status`, `visitedAt`, `visitResult` etc. como **espejo de la última visita** — mantiene compatibilidad con vistas existentes (Biblioteca, lista de stops) sin tener que hacer JOIN cada vez.
- `kpi_values` se modifica: ahora apunta a `(visit_uid, kpi_id)` en lugar de `(stop_uid, kpi_id)`. Eso requiere migración de datos existentes (62 filas en producción).
- `visit_photos` se modifica: ahora apunta a `(visit_uid)` en lugar de `(stop_uid)`. Migración de datos existentes (0 filas en producción ahora mismo según el dump).
- Para stops existentes sin visitas, se crea una `stop_visit` "inicial" desde sus campos actuales — no se pierde histórico.
- Idempotencia del importer: dedup por `(stop_uid, visit_date)` con UNIQUE KEY.

---

## Task 1: Migración SQL servidor — tabla `stop_visits`

**Files:**
- Create: `rutasproapk/migration_v16_stop_visits.sql`

- [ ] **Step 1: Crear el fichero de migración**

```sql
-- migration_v16_stop_visits.sql
-- Modelo de informes diarios independientes:
-- Una parada (stop) puede visitarse N veces (una por fecha programada).
-- Cada visita es una fila en stop_visits — append-only en sentido lógico,
-- sync bidireccional como cualquier entidad.

CREATE TABLE IF NOT EXISTS `stop_visits` (
  `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `uid` varchar(36) NOT NULL,
  `stop_uid` varchar(36) NOT NULL,
  `route_uid` varchar(36) NOT NULL,       -- desnormalizado para queries por ruta
  `account_id` int(10) UNSIGNED NOT NULL,
  `visit_date` date NOT NULL,             -- YYYY-MM-DD — fecha programada de la visita
  `status` enum('pending','visiting','done','skipped') NOT NULL DEFAULT 'pending',
  `visited_at` datetime DEFAULT NULL,
  `visit_result` varchar(20) DEFAULT NULL,
  `next_action` text DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `check_in_ts` bigint(20) DEFAULT NULL,
  `check_out_ts` bigint(20) DEFAULT NULL,
  `gps_lat_visit` double DEFAULT NULL,
  `gps_lng_visit` double DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_visit_uid` (`uid`),
  UNIQUE KEY `uq_stop_date` (`stop_uid`, `visit_date`),
  KEY `idx_route_date` (`route_uid`, `visit_date`),
  KEY `idx_account_updated` (`account_id`, `updated_at`),
  CONSTRAINT `fk_visit_stop` FOREIGN KEY (`stop_uid`) REFERENCES `stops` (`uid`) ON DELETE CASCADE,
  CONSTRAINT `fk_visit_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Back-fill: para cada stop existente con visitedAt o status != 'pending',
-- crear una visita inicial con sus datos actuales.
-- Esto preserva el histórico de los 0 stops que hay ahora en producción
-- (vacía: confirmado por dump del 28/05) y será idempotente.
INSERT INTO stop_visits (uid, stop_uid, route_uid, account_id, visit_date,
                         status, visited_at, visit_result, next_action, notes,
                         check_in_ts, check_out_ts, gps_lat_visit, gps_lng_visit,
                         created_at, updated_at)
SELECT
    CONCAT(s.uid, '-v1') AS uid,           -- UID derivado para idempotencia
    s.uid AS stop_uid,
    r.uid AS route_uid,
    s.account_id,
    COALESCE(s.date_assigned, DATE(s.created_at)) AS visit_date,
    s.status,
    s.visited_at,
    s.visit_result,
    s.next_action,
    s.notes,
    s.check_in_ts,
    s.check_out_ts,
    s.gps_lat_visit,
    s.gps_lng_visit,
    s.created_at,
    s.updated_at
FROM stops s
JOIN routes r ON r.id = s.route_id
WHERE s.deleted_at IS NULL
  AND (s.visited_at IS NOT NULL OR s.status != 'pending' OR s.date_assigned IS NOT NULL)
ON DUPLICATE KEY UPDATE updated_at = stop_visits.updated_at;

-- Registrar la migración
INSERT INTO schema_migrations (version, applied_at)
VALUES ('v16_stop_visits', NOW())
ON DUPLICATE KEY UPDATE applied_at = applied_at;
```

- [ ] **Step 2: Verificación manual en phpMyAdmin tras subirla**

Ejecutar:
```sql
SHOW COLUMNS FROM stop_visits;
SELECT COUNT(*) FROM stop_visits;
```

Esperado: la tabla existe con todas las columnas. El COUNT es 0 (BD producción sin stops actualmente) o > 0 si había stops con visitas.

- [ ] **Step 3: Commit**

```bash
git add rutasproapk/migration_v16_stop_visits.sql
git commit -m "feat(db): migración v16 — tabla stop_visits (informes diarios)"
```

---

## Task 2: Migración SQL — mover kpi_values a (visit_uid, kpi_id)

**Files:**
- Create: `rutasproapk/migration_v17_kpi_to_visit.sql`

- [ ] **Step 1: Crear el fichero de migración**

```sql
-- migration_v17_kpi_to_visit.sql
-- Los KPIs ya no se anclan al stop sino a la visita concreta.
-- Esto permite que un PDV con 4 visitas mensuales tenga 4 sets de KPIs.

-- Añadir columna visit_uid manteniendo stop_uid para compatibilidad atrás.
ALTER TABLE kpi_values
  ADD COLUMN IF NOT EXISTS `visit_uid` varchar(36) DEFAULT NULL AFTER `stop_uid`,
  ADD KEY IF NOT EXISTS `idx_visit_uid` (`visit_uid`);

-- Back-fill: cada KPI existente apunta a la visita inicial del stop (creada en v16).
UPDATE kpi_values kv
JOIN stop_visits sv ON sv.uid = CONCAT(kv.stop_uid, '-v1')
SET kv.visit_uid = sv.uid
WHERE kv.visit_uid IS NULL;

-- Cambio de UNIQUE KEY: ahora por (visit_uid, kpi_id).
-- La key antigua (stop_uid, kpi_id) se conserva como índice pero ya no es UNIQUE.
ALTER TABLE kpi_values
  DROP INDEX IF EXISTS `uq_stop_kpi`,
  ADD UNIQUE KEY IF NOT EXISTS `uq_visit_kpi` (`visit_uid`, `kpi_id`),
  ADD KEY IF NOT EXISTS `idx_stop_kpi` (`stop_uid`, `kpi_id`);

INSERT INTO schema_migrations (version, applied_at)
VALUES ('v17_kpi_to_visit', NOW())
ON DUPLICATE KEY UPDATE applied_at = applied_at;
```

- [ ] **Step 2: Verificación**

```sql
SHOW COLUMNS FROM kpi_values LIKE 'visit_uid';
SELECT COUNT(*) FROM kpi_values WHERE visit_uid IS NULL;  -- esperado 0
SHOW INDEX FROM kpi_values WHERE Key_name = 'uq_visit_kpi';  -- esperado 1 fila
```

- [ ] **Step 3: Commit**

```bash
git add rutasproapk/migration_v17_kpi_to_visit.sql
git commit -m "feat(db): migración v17 — kpi_values ancladas a visit_uid"
```

---

## Task 3: Migración SQL — visit_photos a visit_uid

**Files:**
- Create: `rutasproapk/migration_v18_photos_to_visit.sql`

- [ ] **Step 1: Crear el fichero**

```sql
-- migration_v18_photos_to_visit.sql
-- Las fotos pasan a estar ancladas a la visita concreta.

ALTER TABLE visit_photos
  ADD COLUMN IF NOT EXISTS `visit_uid` varchar(36) DEFAULT NULL AFTER `stop_uid`,
  ADD KEY IF NOT EXISTS `idx_visit_uid` (`visit_uid`);

-- Back-fill desde la visita inicial del stop.
UPDATE visit_photos vp
JOIN stop_visits sv ON sv.uid = CONCAT(vp.stop_uid, '-v1')
SET vp.visit_uid = sv.uid
WHERE vp.visit_uid IS NULL;

INSERT INTO schema_migrations (version, applied_at)
VALUES ('v18_photos_to_visit', NOW())
ON DUPLICATE KEY UPDATE applied_at = applied_at;
```

- [ ] **Step 2: Verificación**

```sql
SHOW COLUMNS FROM visit_photos LIKE 'visit_uid';
SELECT COUNT(*) FROM visit_photos WHERE visit_uid IS NULL;
-- Esperado 0 si ya había fotos; OK si no había
```

- [ ] **Step 3: Commit**

```bash
git add rutasproapk/migration_v18_photos_to_visit.sql
git commit -m "feat(db): migración v18 — visit_photos ancladas a visit_uid"
```

---

## Task 4: Endpoints `api.php` — CRUD de stop_visits

**Files:**
- Modify: `rutasproapk/api.php` (router de acciones)

- [ ] **Step 1: Localizar el bloque router**

Abrir `rutasproapk/api.php`. Localizar el final del bloque `assign_route` (línea ~2090). Los endpoints nuevos van **después** de `assign_route`.

- [ ] **Step 2: Añadir endpoint `visits_by_route`**

Insertar tras el cierre de `assign_route`:

```php
// ════════════════════════════════════════════════════════════════════
// visits_by_route — todas las visitas de una ruta agrupadas por fecha
// Roles: agent ve solo sus rutas, manager ve las de su equipo, admin/owner todas
// ════════════════════════════════════════════════════════════════════
if ($action === 'visits_by_route') {
    [$uid, $rl, $aid] = requireAuth();
    $routeUid = san($_GET['route_uid'] ?? '', 36);
    if (!$routeUid) jsonError('route_uid requerido', 400);

    // Validar que el usuario puede ver esta ruta
    $st = db()->prepare(
        'SELECT r.uid, r.user_id FROM routes r WHERE r.uid=? AND r.account_id=?'
    );
    $st->execute([$routeUid, $aid]);
    $row = $st->fetch();
    if (!$row) jsonError('Ruta no encontrada', 404);

    // Agent solo sus rutas
    if ($rl <= 2 && (int)$row['user_id'] !== $uid) {
        jsonError('Sin permiso', 403);
    }

    $st = db()->prepare(
        'SELECT uid, stop_uid, route_uid, visit_date, status, visited_at,
                visit_result, next_action, notes,
                check_in_ts, check_out_ts, gps_lat_visit, gps_lng_visit,
                created_at, updated_at
         FROM stop_visits
         WHERE route_uid=? AND deleted_at IS NULL
         ORDER BY visit_date ASC, stop_uid ASC'
    );
    $st->execute([$routeUid]);
    jsonOk(['visits' => $st->fetchAll()]);
}
```

- [ ] **Step 3: Añadir endpoint `visit_save` (crear o actualizar visita)**

A continuación:

```php
// ════════════════════════════════════════════════════════════════════
// visit_save — crear o actualizar una visita (upsert por uid)
// El cliente envía el uid de la visita. Si existe → UPDATE, si no → INSERT.
// ════════════════════════════════════════════════════════════════════
if ($action === 'visit_save') {
    [$uid, $rl, $aid] = requireAuth();
    $data = $body;
    $visitUid = san($data['uid'] ?? '', 36);
    $stopUid  = san($data['stop_uid'] ?? '', 36);
    $routeUid = san($data['route_uid'] ?? '', 36);
    $visitDate = san($data['visit_date'] ?? '', 10);  // YYYY-MM-DD

    if (!$visitUid || !$stopUid || !$routeUid || !$visitDate) {
        jsonError('uid, stop_uid, route_uid, visit_date requeridos', 400);
    }
    if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $visitDate)) {
        jsonError('visit_date formato YYYY-MM-DD', 400);
    }

    // Validar que el stop pertenece a la cuenta del usuario
    $st = db()->prepare('SELECT uid FROM stops WHERE uid=? AND account_id=?');
    $st->execute([$stopUid, $aid]);
    if (!$st->fetch()) jsonError('Stop no encontrado', 404);

    $existing = db()->prepare('SELECT id FROM stop_visits WHERE uid=? AND account_id=?');
    $existing->execute([$visitUid, $aid]);

    if ($existing->fetch()) {
        // UPDATE
        db()->prepare(
            'UPDATE stop_visits SET
                status=?, visited_at=?, visit_result=?, next_action=?, notes=?,
                check_in_ts=COALESCE(?,check_in_ts),
                check_out_ts=COALESCE(?,check_out_ts),
                gps_lat_visit=COALESCE(?,gps_lat_visit),
                gps_lng_visit=COALESCE(?,gps_lng_visit),
                updated_at=NOW()
             WHERE uid=? AND account_id=?'
        )->execute([
            san($data['status'] ?? 'pending', 20),
            $data['visited_at'] ?? null,
            san($data['visit_result'] ?? '', 20) ?: null,
            san($data['next_action'] ?? '', 5000) ?: null,
            san($data['notes'] ?? '', 5000) ?: null,
            $data['check_in_ts']  ?? null,
            $data['check_out_ts'] ?? null,
            isset($data['gps_lat_visit']) ? (float)$data['gps_lat_visit'] : null,
            isset($data['gps_lng_visit']) ? (float)$data['gps_lng_visit'] : null,
            $visitUid, $aid,
        ]);
        jsonOk(['uid' => $visitUid, 'updated' => true]);
    } else {
        // INSERT
        db()->prepare(
            'INSERT INTO stop_visits
                (uid, stop_uid, route_uid, account_id, visit_date,
                 status, visited_at, visit_result, next_action, notes,
                 check_in_ts, check_out_ts, gps_lat_visit, gps_lng_visit,
                 created_at, updated_at)
             VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())'
        )->execute([
            $visitUid, $stopUid, $routeUid, $aid, $visitDate,
            san($data['status'] ?? 'pending', 20),
            $data['visited_at'] ?? null,
            san($data['visit_result'] ?? '', 20) ?: null,
            san($data['next_action'] ?? '', 5000) ?: null,
            san($data['notes'] ?? '', 5000) ?: null,
            $data['check_in_ts']  ?? null,
            $data['check_out_ts'] ?? null,
            isset($data['gps_lat_visit']) ? (float)$data['gps_lat_visit'] : null,
            isset($data['gps_lng_visit']) ? (float)$data['gps_lng_visit'] : null,
        ]);
        jsonOk(['uid' => $visitUid, 'created' => true]);
    }
}
```

- [ ] **Step 4: Modificar `delta_sync` para incluir visitas**

Localizar `if ($action === 'delta_sync')` (línea ~628). Buscar el bloque que devuelve `stops` (línea ~684). Añadir tras él, antes de devolver el JSON final, un bloque similar para `stop_visits`:

```php
    // stop_visits modificadas desde $since
    $vQuery = "SELECT sv.uid, sv.stop_uid, sv.route_uid, sv.visit_date, sv.status,
                       sv.visited_at, sv.visit_result, sv.next_action, sv.notes,
                       sv.check_in_ts, sv.check_out_ts, sv.gps_lat_visit, sv.gps_lng_visit,
                       sv.created_at, sv.updated_at, sv.deleted_at
                FROM stop_visits sv
                JOIN routes r ON r.uid = sv.route_uid
                WHERE {$stopsWhere} AND sv.updated_at > ?
                ORDER BY sv.updated_at ASC LIMIT 500";
    $vS = db()->prepare($vQuery);
    if ($stopsParam === null) {
        $vS->execute([...$agentIds, $since]);
    } else {
        $vS->execute([$stopsParam, $since]);
    }
    $visits = $vS->fetchAll();
```

Y añadir `'visits' => $visits` al array de respuesta `jsonOk([...])` del `delta_sync`.

- [ ] **Step 5: Modificar `batch_sync` para procesar visitas**

Localizar `if ($action === 'batch_sync')` (línea ~773). Localizar el bloque `if ($entity === 'stop')`. Añadir **después** de su cierre `}`:

```php
                } elseif ($entity === 'stop_visit') {
                    if ($operation === 'create' || $operation === 'update') {
                        $existing = db()->prepare('SELECT id FROM stop_visits WHERE uid=? AND account_id=?');
                        $existing->execute([$clientUid, $aid]);
                        if ($existing->fetch()) {
                            db()->prepare(
                                'UPDATE stop_visits SET
                                    status=?, visited_at=?, visit_result=?, next_action=?, notes=?,
                                    check_in_ts=COALESCE(?,check_in_ts),
                                    check_out_ts=COALESCE(?,check_out_ts),
                                    gps_lat_visit=COALESCE(?,gps_lat_visit),
                                    gps_lng_visit=COALESCE(?,gps_lng_visit),
                                    updated_at=NOW()
                                 WHERE uid=? AND account_id=?'
                            )->execute([
                                san($data['status'] ?? 'pending', 20),
                                $data['visited_at'] ?? null,
                                san($data['visit_result'] ?? '', 20) ?: null,
                                san($data['next_action'] ?? '', 5000) ?: null,
                                san($data['notes'] ?? '', 5000) ?: null,
                                $data['check_in_ts']  ?? null,
                                $data['check_out_ts'] ?? null,
                                isset($data['gps_lat_visit']) ? (float)$data['gps_lat_visit'] : null,
                                isset($data['gps_lng_visit']) ? (float)$data['gps_lng_visit'] : null,
                                $clientUid, $aid,
                            ]);
                        } else {
                            db()->prepare(
                                'INSERT INTO stop_visits
                                    (uid, stop_uid, route_uid, account_id, visit_date,
                                     status, visited_at, visit_result, next_action, notes,
                                     check_in_ts, check_out_ts, gps_lat_visit, gps_lng_visit,
                                     created_at, updated_at)
                                 VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())'
                            )->execute([
                                $clientUid,
                                san($data['stop_uid'] ?? '', 36),
                                san($data['route_uid'] ?? '', 36),
                                $aid,
                                san($data['visit_date'] ?? '', 10),
                                san($data['status'] ?? 'pending', 20),
                                $data['visited_at'] ?? null,
                                san($data['visit_result'] ?? '', 20) ?: null,
                                san($data['next_action'] ?? '', 5000) ?: null,
                                san($data['notes'] ?? '', 5000) ?: null,
                                $data['check_in_ts']  ?? null,
                                $data['check_out_ts'] ?? null,
                                isset($data['gps_lat_visit']) ? (float)$data['gps_lat_visit'] : null,
                                isset($data['gps_lng_visit']) ? (float)$data['gps_lng_visit'] : null,
                            ]);
                        }
                        $synced[] = ['uid' => $clientUid, 'entity' => 'stop_visit'];
                    } elseif ($operation === 'delete') {
                        db()->prepare(
                            'UPDATE stop_visits SET deleted_at=NOW(), updated_at=NOW() WHERE uid=? AND account_id=?'
                        )->execute([$clientUid, $aid]);
                        $synced[] = ['uid' => $clientUid, 'entity' => 'stop_visit', 'deleted' => true];
                    }
```

- [ ] **Step 6: Commit**

```bash
git add rutasproapk/api.php
git commit -m "feat(api): endpoints stop_visits (visits_by_route, visit_save, batch_sync, delta_sync)"
```

---

## Task 5: Room — `StopVisitEntity` y migración 15→16

**Files:**
- Create: `app/src/main/kotlin/com/pabl3st/rutapp/data/local/entity/StopVisitEntity.kt`
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/data/local/RutasDatabase.kt` (subir versión + migración)
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/di/DatabaseModule.kt` (registrar migración)

- [ ] **Step 1: Crear `StopVisitEntity.kt`**

```kotlin
package com.pabl3st.rutapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Una visita programada o realizada a un stop en una fecha concreta.
 * PK: uid (UUID generado en cliente).
 * UNIQUE: (stopUid, visitDate) — evita duplicados al sincronizar.
 *
 * Cada fecha programada de una ruta crea una stop_visit "pending".
 * El agente la pasa a "visiting" al check-in y a "done"/"skipped" al guardar.
 * Los KPIs y fotos de esa visita se enlazan vía visitUid.
 */
@Entity(
    tableName = "stop_visits",
    indices = [
        Index(value = ["stopUid", "visitDate"], unique = true),
        Index("routeUid"),
        Index("syncStatus"),
    ],
)
data class StopVisitEntity(
    @PrimaryKey val uid: String,
    val stopUid: String,
    val routeUid: String,
    val accountId: Int,
    val visitDate: String,                 // YYYY-MM-DD
    val status: String     = "pending",    // pending|visiting|done|skipped
    val visitedAt: String? = null,
    val visitResult: String? = null,
    val nextAction: String?  = null,
    val notes: String?       = null,
    val checkInTs:  Long?    = null,
    val checkOutTs: Long?    = null,
    val gpsLatVisit: Double? = null,
    val gpsLngVisit: Double? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?   = null,
    val syncStatus: String   = "pending",  // pending|synced|error
    val syncedAt: String?    = null,
)
```

- [ ] **Step 2: Modificar `RutasDatabase.kt` — subir versión a 16**

Localizar el `@Database(...)` (línea ~28). Cambiar `version = 15` a `version = 16`. Añadir `StopVisitEntity::class` al array de entities.

Añadir la migración después de `MIGRATION_14_15`:

```kotlin
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS stop_visits (
                uid TEXT NOT NULL PRIMARY KEY,
                stopUid TEXT NOT NULL,
                routeUid TEXT NOT NULL,
                accountId INTEGER NOT NULL,
                visitDate TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'pending',
                visitedAt TEXT,
                visitResult TEXT,
                nextAction TEXT,
                notes TEXT,
                checkInTs INTEGER,
                checkOutTs INTEGER,
                gpsLatVisit REAL,
                gpsLngVisit REAL,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                deletedAt TEXT,
                syncStatus TEXT NOT NULL DEFAULT 'pending',
                syncedAt TEXT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_stop_visits_stop_date ON stop_visits (stopUid, visitDate)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_stop_visits_route ON stop_visits (routeUid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_stop_visits_sync ON stop_visits (syncStatus)")

        // Back-fill: por cada stop con datos de visita, crear una stop_visit inicial.
        db.execSQL("""
            INSERT OR IGNORE INTO stop_visits
                (uid, stopUid, routeUid, accountId, visitDate, status,
                 visitedAt, visitResult, nextAction, notes,
                 checkInTs, checkOutTs, gpsLatVisit, gpsLngVisit,
                 createdAt, updatedAt, syncStatus)
            SELECT
                uid || '-v1',
                uid,
                routeUid,
                accountId,
                COALESCE(dateAssigned, substr(createdAt, 1, 10)),
                status,
                visitedAt,
                visitResult,
                nextAction,
                notes,
                checkInTs,
                checkOutTs,
                gpsLatVisit,
                gpsLngVisit,
                createdAt,
                updatedAt,
                'synced'
            FROM stops
            WHERE deletedAt IS NULL
              AND (visitedAt IS NOT NULL OR status != 'pending' OR dateAssigned IS NOT NULL)
        """.trimIndent())
    }
}
```

- [ ] **Step 3: Modificar `DatabaseModule.kt`**

Añadir `RutasDatabase.MIGRATION_15_16` a la lista de `addMigrations(...)`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/data/local/entity/StopVisitEntity.kt \
        app/src/main/kotlin/com/pabl3st/rutapp/data/local/RutasDatabase.kt \
        app/src/main/kotlin/com/pabl3st/rutapp/di/DatabaseModule.kt
git commit -m "feat(room): StopVisitEntity + migration 15→16"
```

---

## Task 6: DAO — `StopVisitDao`

**Files:**
- Create: `app/src/main/kotlin/com/pabl3st/rutapp/data/local/dao/StopVisitDao.kt`
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/data/local/RutasDatabase.kt` (declarar el DAO)

- [ ] **Step 1: Crear `StopVisitDao.kt`**

```kotlin
package com.pabl3st.rutapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pabl3st.rutapp.data.local.entity.StopVisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StopVisitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(visit: StopVisitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(visits: List<StopVisitEntity>)

    /** Visitas de una ruta en una fecha concreta */
    @Query("""
        SELECT * FROM stop_visits
        WHERE routeUid = :routeUid AND visitDate = :date AND deletedAt IS NULL
    """)
    fun observeByRouteAndDate(routeUid: String, date: String): Flow<List<StopVisitEntity>>

    /** Una visita específica por (stopUid, visitDate) */
    @Query("""
        SELECT * FROM stop_visits
        WHERE stopUid = :stopUid AND visitDate = :date AND deletedAt IS NULL
        LIMIT 1
    """)
    suspend fun getByStopAndDate(stopUid: String, date: String): StopVisitEntity?

    /** Todas las visitas de un stop (histórico completo) */
    @Query("""
        SELECT * FROM stop_visits
        WHERE stopUid = :stopUid AND deletedAt IS NULL
        ORDER BY visitDate DESC
    """)
    fun observeByStop(stopUid: String): Flow<List<StopVisitEntity>>

    /** Todas las visitas pendientes de sync */
    @Query("SELECT * FROM stop_visits WHERE syncStatus = 'pending' AND deletedAt IS NULL")
    suspend fun getPendingSync(): List<StopVisitEntity>

    @Query("UPDATE stop_visits SET syncStatus = :status, syncedAt = :syncedAt WHERE uid = :uid")
    suspend fun markSynced(uid: String, status: String, syncedAt: String)

    /** Fechas únicas con visitas para una ruta */
    @Query("""
        SELECT DISTINCT visitDate FROM stop_visits
        WHERE routeUid = :routeUid AND deletedAt IS NULL
        ORDER BY visitDate ASC
    """)
    suspend fun getDistinctDatesByRoute(routeUid: String): List<String>
}
```

- [ ] **Step 2: Declarar el DAO en `RutasDatabase.kt`**

Dentro del `abstract class RutasDatabase`, añadir:
```kotlin
abstract fun stopVisitDao(): StopVisitDao
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/data/local/dao/StopVisitDao.kt \
        app/src/main/kotlin/com/pabl3st/rutapp/data/local/RutasDatabase.kt
git commit -m "feat(room): StopVisitDao"
```

---

## Task 7: DTO + Mapper — `StopVisitDto`

**Files:**
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/data/network/RutasApiService.kt` (añadir DTO)
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/data/repository/DtoMappers.kt` (mapper)

- [ ] **Step 1: Añadir `StopVisitDto` en `RutasApiService.kt`**

Tras `StopDto`:

```kotlin
@JsonClass(generateAdapter = true)
data class StopVisitDto(
    val uid: String,
    @Json(name = "stop_uid")     val stopUid: String,
    @Json(name = "route_uid")    val routeUid: String,
    @Json(name = "visit_date")   val visitDate: String,
    val status: String,
    @Json(name = "visited_at")   val visitedAt: String? = null,
    @Json(name = "visit_result") val visitResult: String? = null,
    @Json(name = "next_action")  val nextAction: String? = null,
    val notes: String? = null,
    @Json(name = "check_in_ts")  val checkInTs:  Long? = null,
    @Json(name = "check_out_ts") val checkOutTs: Long? = null,
    @Json(name = "gps_lat_visit") val gpsLatVisit: Double? = null,
    @Json(name = "gps_lng_visit") val gpsLngVisit: Double? = null,
    @Json(name = "created_at")   val createdAt: String,
    @Json(name = "updated_at")   val updatedAt: String,
    @Json(name = "deleted_at")   val deletedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class VisitsByRouteResponse(
    val ok: Boolean,
    val visits: List<StopVisitDto>?,
    val error: String?,
)
```

Añadir el campo `visits` al `DeltaSyncResponse` si existe (debe estar ya en `RutasApiService.kt` cerca de `delta_sync`):

```kotlin
@Json(name = "visits") val visits: List<StopVisitDto>? = null,
```

Y añadir los endpoints Retrofit. Localizar la interface `RutasApi` (suele estar en el mismo fichero). Añadir:

```kotlin
@GET("api.php")
suspend fun visitsByRoute(
    @Query("action") action: String = "visits_by_route",
    @Query("route_uid") routeUid: String,
): VisitsByRouteResponse

@POST("api.php")
suspend fun visitSave(
    @Query("action") action: String = "visit_save",
    @Body body: Map<String, @JvmSuppressWildcards Any?>,
): ApiResponse
```

- [ ] **Step 2: Añadir mapper en `DtoMappers.kt`**

```kotlin
// ── StopVisitDto → StopVisitEntity ──────────────────────────
fun StopVisitDto.toEntity(accountId: Int): StopVisitEntity = StopVisitEntity(
    uid          = uid,
    stopUid      = stopUid,
    routeUid     = routeUid,
    accountId    = accountId,
    visitDate    = visitDate,
    status       = status,
    visitedAt    = visitedAt,
    visitResult  = visitResult,
    nextAction   = nextAction,
    notes        = notes,
    checkInTs    = checkInTs,
    checkOutTs   = checkOutTs,
    gpsLatVisit  = gpsLatVisit,
    gpsLngVisit  = gpsLngVisit,
    createdAt    = createdAt,
    updatedAt    = updatedAt,
    deletedAt    = deletedAt,
    syncStatus   = "synced",   // viene del servidor → sincronizado
    syncedAt     = updatedAt,
)
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/data/network/RutasApiService.kt \
        app/src/main/kotlin/com/pabl3st/rutapp/data/repository/DtoMappers.kt
git commit -m "feat(net): StopVisitDto + mapper + endpoints visits_by_route/visit_save"
```

---

## Task 8: `StopVisitRepository`

**Files:**
- Create: `app/src/main/kotlin/com/pabl3st/rutapp/data/repository/StopVisitRepository.kt`

- [ ] **Step 1: Crear el repositorio**

```kotlin
package com.pabl3st.rutapp.data.repository

import com.pabl3st.rutapp.data.local.dao.StopVisitDao
import com.pabl3st.rutapp.data.local.entity.StopVisitEntity
import com.pabl3st.rutapp.data.session.SessionManager
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StopVisitRepository @Inject constructor(
    private val visitDao: StopVisitDao,
    private val session:  SessionManager,
) {
    fun observeByRouteAndDate(routeUid: String, date: String): Flow<List<StopVisitEntity>> =
        visitDao.observeByRouteAndDate(routeUid, date)

    fun observeByStop(stopUid: String): Flow<List<StopVisitEntity>> =
        visitDao.observeByStop(stopUid)

    suspend fun getByStopAndDate(stopUid: String, date: String): StopVisitEntity? =
        visitDao.getByStopAndDate(stopUid, date)

    /**
     * Crea una visita "pending" para un stop en una fecha concreta.
     * Idempotente: si ya existe (uniq (stopUid, visitDate)), no la duplica.
     */
    suspend fun ensureVisitExists(
        stopUid:  String,
        routeUid: String,
        date:     String,
    ): StopVisitEntity {
        visitDao.getByStopAndDate(stopUid, date)?.let { return it }
        val now = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val visit = StopVisitEntity(
            uid        = UUID.randomUUID().toString(),
            stopUid    = stopUid,
            routeUid   = routeUid,
            accountId  = session.accountId,
            visitDate  = date,
            createdAt  = now,
            updatedAt  = now,
            syncStatus = "pending",
        )
        visitDao.upsert(visit)
        return visit
    }

    suspend fun updateVisit(visit: StopVisitEntity) {
        val now = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        visitDao.upsert(visit.copy(updatedAt = now, syncStatus = "pending"))
    }

    suspend fun getDistinctDatesByRoute(routeUid: String): List<String> =
        visitDao.getDistinctDatesByRoute(routeUid)
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/data/repository/StopVisitRepository.kt
git commit -m "feat(repo): StopVisitRepository"
```

---

## Task 9: Refactor `RouteDetailViewModel` — fechas desde la ruta, visitas desde stop_visits

**Files:**
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RouteDetailViewModel.kt`

- [ ] **Step 1: Inyectar `StopVisitRepository`**

Buscar el constructor del ViewModel y añadir:

```kotlin
private val visitRepo: StopVisitRepository,
```

- [ ] **Step 2: Reemplazar `observeStops()` por nueva lógica**

Localizar la función `observeStops()` (línea ~128). Reemplazar el cuerpo entero por:

```kotlin
private fun observeStops() {
    viewModelScope.launch {
        // Las fechas disponibles son las de la RUTA (no de los stops).
        // route.dateAssigned + route.scheduledDates = todas las fechas programadas.
        val route = routeRepo.getByUid(routeUid) ?: return@launch
        val allDates = buildList {
            add(route.dateAssigned)
            route.scheduledDates?.let { addAll(it) }
        }.distinct().sorted()

        if (allDates.isNotEmpty()) {
            _ui.update { it.copy(availableDates = allDates) }
            val sel = _ui.value.selectedDate ?: allDates.firstOrNull { it >= java.time.LocalDate.now().toString() } ?: allDates.first()
            _ui.update { it.copy(selectedDate = sel) }
            // Asegurar visitas creadas para la fecha seleccionada
            ensureVisitsForDate(routeUid, sel)
        }

        // Los stops mostrados siempre son TODOS los de la ruta (149 paradas únicas).
        stopRepo.observeByRoute(routeUid)
            .catch { e -> _ui.update { it.copy(error = e.message) } }
            .collect { stops ->
                _baseStops.value = stops
                applySortMode(_ui.value.sortMode, stops)
            }
    }
}

private suspend fun ensureVisitsForDate(routeUid: String, date: String) {
    val stops = stopRepo.getByRoute(routeUid)
    stops.forEach { stop ->
        visitRepo.ensureVisitExists(stop.uid, routeUid, date)
    }
}
```

- [ ] **Step 3: Actualizar `onDateSelected()`**

Localizar `onDateSelected(date: String)` (línea ~316). Reemplazar:

```kotlin
fun onDateSelected(date: String) {
    _ui.update { it.copy(selectedDate = date) }
    viewModelScope.launch {
        ensureVisitsForDate(routeUid, date)
        // Los stops mostrados no cambian; lo que cambia es qué visita
        // se asocia a cada uno (vía visitRepo.observeByRouteAndDate).
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RouteDetailViewModel.kt
git commit -m "feat(ui): RouteDetailViewModel lee fechas de route.scheduledDates"
```

---

## Task 10: Sync — incluir stop_visits en `SyncRepository`

**Files:**
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/data/repository/SyncRepository.kt`

- [ ] **Step 1: Inyectar `StopVisitDao`**

Buscar el constructor de `SyncRepository`. Añadir:

```kotlin
private val visitDao: StopVisitDao,
```

- [ ] **Step 2: Localizar el método `runSync()` y añadir bloque de visitas**

Tras la lógica de sync de stops (subir + bajar), añadir:

```kotlin
// ── Subir stop_visits pendientes ──────────────────────────
val pendingVisits = visitDao.getPendingSync()
pendingVisits.forEach { visit ->
    val payload = mapOf(
        "uid"          to visit.uid,
        "stop_uid"     to visit.stopUid,
        "route_uid"    to visit.routeUid,
        "visit_date"   to visit.visitDate,
        "status"       to visit.status,
        "visited_at"   to visit.visitedAt,
        "visit_result" to visit.visitResult,
        "next_action"  to visit.nextAction,
        "notes"        to visit.notes,
        "check_in_ts"  to visit.checkInTs,
        "check_out_ts" to visit.checkOutTs,
        "gps_lat_visit" to visit.gpsLatVisit,
        "gps_lng_visit" to visit.gpsLngVisit,
    )
    runCatching { api.visitSave(body = payload) }
        .onSuccess { resp ->
            if (resp.ok) {
                visitDao.markSynced(visit.uid, "synced",
                    Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            }
        }
}
```

- [ ] **Step 3: Procesar visitas devueltas por `delta_sync`**

Localizar el bloque que procesa la respuesta de `delta_sync`. Tras procesar stops:

```kotlin
deltaResp.visits?.forEach { dto ->
    visitDao.upsert(dto.toEntity(session.accountId))
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/data/repository/SyncRepository.kt
git commit -m "feat(sync): subir y bajar stop_visits en runSync"
```

---

## Task 11: UI — `StopCard` lee status de la visita actual

**Files:**
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RouteDetailScreen.kt`

- [ ] **Step 1: Exponer las visitas de la fecha actual en `RouteDetailUiState`**

Modificar `RouteDetailUiState` para añadir:

```kotlin
val visitsByStop: Map<String, StopVisitEntity> = emptyMap(),  // stopUid → visita de hoy
```

- [ ] **Step 2: Cargar visitas reactivamente en ViewModel**

En `RouteDetailViewModel.observeStops()`, tras el `_ui.update { selectedDate }`, lanzar otro flow:

```kotlin
launch {
    _ui.map { it.selectedDate }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { date -> visitRepo.observeByRouteAndDate(routeUid, date) }
        .collect { visits ->
            _ui.update { it.copy(visitsByStop = visits.associateBy { v -> v.stopUid }) }
        }
}
```

- [ ] **Step 3: En `StopCard` mostrar el estado de la visita actual**

Localizar `StopCard` en `RouteDetailScreen.kt`. Aceptar parámetro `visit: StopVisitEntity?`. Sustituir `stop.status` por `visit?.status ?: "pending"` en el chip de estado.

Donde se invoca `StopCard`, pasar:
```kotlin
StopCard(
    stop  = stop,
    visit = ui.visitsByStop[stop.uid],
    ...
)
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RouteDetailScreen.kt \
        app/src/main/kotlin/com/pabl3st/rutapp/feature/rutas/RouteDetailViewModel.kt
git commit -m "feat(ui): StopCard refleja status de la visita activa, no del stop"
```

---

## Task 12: VisitaViewModel — guardar en stop_visit en lugar de stop

**Files:**
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/feature/visita/VisitaViewModel.kt`

- [ ] **Step 1: Inyectar `StopVisitRepository`**

```kotlin
private val visitRepo: StopVisitRepository,
```

- [ ] **Step 2: Localizar la función que guarda la visita (busca `saveVisit`, `completeVisit`, `markAsDone` o similar)**

```bash
grep -nE "fun (save|complete|finish|markAs).*[Vv]isit|fun guardar" app/src/main/kotlin/com/pabl3st/rutapp/feature/visita/VisitaViewModel.kt
```

- [ ] **Step 3: Cambiar destino de escritura**

Donde antes se hacía `stopRepo.updateXxx(stop.uid, status="done", visitedAt=..., visitResult=..., notes=..., kpis=...)`, ahora:

```kotlin
val visit = visitRepo.getByStopAndDate(stopUid, selectedDate)
    ?: visitRepo.ensureVisitExists(stopUid, routeUid, selectedDate)

visitRepo.updateVisit(visit.copy(
    status      = "done",
    visitedAt   = nowIso,
    visitResult = result,
    nextAction  = nextAction,
    notes       = notes,
    checkOutTs  = System.currentTimeMillis(),
))

// Actualizar también el espejo en el stop (para vistas que no usan stop_visits aún)
stopRepo.updateLastVisitMirror(stopUid, status="done", visitedAt=nowIso, visitResult=result)
```

- [ ] **Step 4: Añadir método `updateLastVisitMirror` en `StopRepository`**

En `StopRepository`, método nuevo:

```kotlin
suspend fun updateLastVisitMirror(
    stopUid:     String,
    status:      String,
    visitedAt:   String?,
    visitResult: String?,
) {
    val stop = stopDao.getByUid(stopUid) ?: return
    val now = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    stopDao.upsert(stop.copy(
        status      = status,
        visitedAt   = visitedAt,
        visitResult = visitResult,
        updatedAt   = now,
        syncStatus  = "pending",
    ))
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/feature/visita/VisitaViewModel.kt \
        app/src/main/kotlin/com/pabl3st/rutapp/data/repository/StopRepository.kt
git commit -m "feat(visita): guardar en stop_visit + mirror en stop"
```

---

## Task 13: Importer — generar stop_visits "pending" por cada fecha

**Files:**
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/feature/importar/ImportarViewModel.kt`

- [ ] **Step 1: Inyectar `StopVisitRepository`**

```kotlin
private val visitRepo: StopVisitRepository,
```

- [ ] **Step 2: Localizar el bucle `entriesToProcess.forEach { entry -> ... }` (línea ~752)**

- [ ] **Step 3: Tras el bloque que crea/actualiza stops (línea ~835), añadir generación de visitas:**

```kotlin
// Generar una stop_visit por cada (stop, fecha) — idempotente, no duplica.
val allDates = entry.scheduledDates.map { it.toString() }
val stopsOfRoute = stopRepo.getByRoute(route.uid)
allDates.forEach { date ->
    stopsOfRoute.forEach { stop ->
        visitRepo.ensureVisitExists(stop.uid, route.uid, date)
    }
}
```

- [ ] **Step 4: Mapear KPIs históricos a la visita correcta**

Localizar el bloque de import de KPIs (línea ~860). Donde antes se usaba `externalIdToStopUid[report.stopExternalId]`, ahora hay que resolver al visit_uid de la fecha del KPI:

```kotlin
val kpiEntities = kpiReports.mapNotNull { report ->
    val stopUid = externalIdToStopUid[report.stopExternalId] ?: return@mapNotNull null
    // El KPI tiene report.date — buscar la stop_visit de esa fecha
    val visit = visitRepo.getByStopAndDate(stopUid, report.date) ?: return@mapNotNull null
    buildList {
        if (report.kpiActivaciones.isNotBlank())
            add(KpiValueEntity(visit.uid, "telco_activaciones", report.kpiActivaciones, "synced"))
        // ... resto de KPIs igual, pero usando visit.uid en vez de stopUid
    }
}.flatten()
```

**Atención:** `KpiValueEntity.stopUid` tiene que pasar a llamarse `visitUid` (o usar el campo nuevo). Esto requiere otra migración Room — ver Task 14 antes.

- [ ] **Step 5: Commit (tras Task 14 para que compile)**

```bash
git add app/src/main/kotlin/com/pabl3st/rutapp/feature/importar/ImportarViewModel.kt
git commit -m "feat(import): generar stop_visits por (stop, fecha)"
```

---

## Task 14: Migrar `KpiValueEntity` y `VisitPhotoEntity` a `visitUid`

**Files:**
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/data/local/entity/KpiValueEntity.kt`
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/data/local/entity/VisitPhotoEntity.kt`
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/data/local/RutasDatabase.kt` (versión 17 + migración)
- Modify: `app/src/main/kotlin/com/pabl3st/rutapp/di/DatabaseModule.kt`
- Modify: todos los callers de `KpiValueEntity(stopUid=...)` y `VisitPhotoEntity(stopUid=...)`

- [ ] **Step 1: Modificar entidades — añadir `visitUid` manteniendo `stopUid` por compatibilidad atrás**

`KpiValueEntity`:
```kotlin
@Entity(
    tableName   = "kpi_values",
    primaryKeys = ["visitUid", "kpiId"],  // PK cambia a (visitUid, kpiId)
)
data class KpiValueEntity(
    val visitUid:  String,    // FK → stop_visits.uid (PK nueva)
    val stopUid:   String,    // FK → stops.uid (conservado para queries por PDV)
    val kpiId:     String,
    val valueText: String,
    val syncStatus: String = "pending",
)
```

`VisitPhotoEntity` — añadir `val visitUid: String? = null,` tras `stopUid`.

- [ ] **Step 2: Crear `MIGRATION_16_17`**

```kotlin
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Recrear kpi_values con nueva PK (visitUid, kpiId)
        db.execSQL("ALTER TABLE kpi_values RENAME TO kpi_values_old")
        db.execSQL("""
            CREATE TABLE kpi_values (
                visitUid TEXT NOT NULL,
                stopUid TEXT NOT NULL,
                kpiId TEXT NOT NULL,
                valueText TEXT NOT NULL,
                syncStatus TEXT NOT NULL DEFAULT 'pending',
                PRIMARY KEY (visitUid, kpiId)
            )
        """.trimIndent())
        // Back-fill desde la visita "v1" de cada stop
        db.execSQL("""
            INSERT INTO kpi_values (visitUid, stopUid, kpiId, valueText, syncStatus)
            SELECT
                stopUid || '-v1' AS visitUid,
                stopUid,
                kpiId,
                valueText,
                syncStatus
            FROM kpi_values_old
            WHERE stopUid IN (SELECT uid FROM stops WHERE deletedAt IS NULL)
        """.trimIndent())
        db.execSQL("DROP TABLE kpi_values_old")

        // visit_photos: añadir columna visitUid sin recrear (no es PK)
        db.execSQL("ALTER TABLE visit_photos ADD COLUMN visitUid TEXT")
        db.execSQL("""
            UPDATE visit_photos
            SET visitUid = stopUid || '-v1'
            WHERE visitUid IS NULL
        """.trimIndent())
    }
}
```

- [ ] **Step 3: Actualizar callers**

```bash
grep -rn "KpiValueEntity(" app/src/main/kotlin/
```

Cada constructor de `KpiValueEntity` debe pasar `visitUid` como primer parámetro. Buscar y actualizar.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(room): KpiValueEntity y VisitPhotoEntity ancladas a visitUid + migration 16→17"
```

---

## Task 15: Verificación end-to-end y despliegue

**Files:**
- Verificación, no modificación de código.

- [ ] **Step 1: Verificar compilación local**

Compilar el proyecto Android (build de GitHub Actions). Esperado: ✅ verde.

- [ ] **Step 2: Subir migraciones al servidor en orden**

Por phpMyAdmin en cPanel, ejecutar **en este orden**:

```
1. migration_v16_stop_visits.sql
2. migration_v17_kpi_to_visit.sql
3. migration_v18_photos_to_visit.sql
```

Verificar tras cada una:
```sql
SELECT version, applied_at FROM schema_migrations ORDER BY id DESC LIMIT 5;
```

- [ ] **Step 3: Subir `api.php` actualizado al hosting**

Sobreescribir `/api.php` con la versión del repo. Verificar:
```
curl 'https://mejoresiagratis.com/.../api.php?action=health'
→ debe responder 200 OK.
```

- [ ] **Step 4: Instalar APK del último commit**

Esperar build verde en GitHub Actions. Descargar APK. Instalar en móvil.

- [ ] **Step 5: Probar import del v7**

- Importar `RutasApp_IMPORT_v7_abril_mayo.xlsx`.
- **Esperado:** 8 rutas, 149 stops únicos (no duplicados), 70 fechas en `route.scheduledDates`, ~1300 `stop_visits` (149 × ~9 fechas en promedio).
- Abrir ruta PS01 → selector de fecha muestra las 4 fechas (mar/abr/may/jun).
- Seleccionar mayo → ven las 149 paradas con status "pending".
- Marcar una parada como visitada → solo afecta a esa visita, no a las demás fechas.

- [ ] **Step 6: Commit final con bump de versión**

```bash
# Editar app/build.gradle: versionCode 127, versionName "1.0.0-s30"
git add app/build.gradle
git commit -m "chore: bump versionCode 127 — modelo C completo"
git push origin main
```

---

## Self-review

**1. Cobertura del spec:**
- ✅ "No duplicar stops" — Task 13 genera visitas, no stops.
- ✅ "Resolver multi-fecha sin perder visitas" — `stop_visits` es independiente por fecha.
- ✅ "Mantener código existente" — `kpi_values` y `visit_photos` migrados en lugar de descartados.
- ✅ "Sync bidireccional" — Tasks 4 (servidor) y 10 (cliente).

**2. Riesgos detectados:**
- **PK change de `kpi_values`**: Task 14 recrea la tabla en cliente. Si la BD producción tiene 62 filas, hay que verificar back-fill antes de cambiar la PK también en servidor (Task 2 deja la antigua como índice no-único, es seguro).
- **`updateLastVisitMirror`**: cuando se marca una visita como "done", también actualiza el stop. Si el agente visita la del 1/5 antes que la del 15/5 (orden no cronológico), el "done" del stop refleja la última escritura, no la última cronológica. Esto es aceptable como "última visita registrada" — la vista de Biblioteca lo entenderá así.
- **Idempotencia del importer**: si se reimporta el mismo XLS, Task 13 hace `ensureVisitExists` que es no-op si existe. ✅ no duplica.

**3. Consistencia de tipos:**
- `StopVisitEntity.visitDate` es `String` (formato `YYYY-MM-DD`). Consistente con `RouteEntity.dateAssigned`.
- `KpiValueEntity.visitUid` String, primary key compuesta con `kpiId`. Consistente.
- DTOs usan `@Json` para snake_case (servidor) ↔ camelCase (Kotlin). Consistente.

**4. Despliegue:**
- Orden obligatorio: BD → api.php → APK. Si APK llega primero, llamará a endpoints que aún no existen → 500.
- Las migraciones son idempotentes (`IF NOT EXISTS`, `INSERT IGNORE`, `ON DUPLICATE KEY`). Re-ejecutarlas no rompe nada.

---

## Resumen de archivos tocados

**Servidor (3 archivos nuevos + 1 modificado):**
- `rutasproapk/migration_v16_stop_visits.sql` (nuevo)
- `rutasproapk/migration_v17_kpi_to_visit.sql` (nuevo)
- `rutasproapk/migration_v18_photos_to_visit.sql` (nuevo)
- `rutasproapk/api.php` (3 endpoints + 1 bloque batch_sync + 1 bloque delta_sync)

**Cliente (8 archivos nuevos + 9 modificados):**
- Nuevo: `StopVisitEntity.kt`, `StopVisitDao.kt`, `StopVisitRepository.kt`, `StopVisitDto`
- Modificado: `RutasDatabase.kt`, `DatabaseModule.kt`, `RutasApiService.kt`, `DtoMappers.kt`, `RouteDetailViewModel.kt`, `RouteDetailScreen.kt`, `VisitaViewModel.kt`, `StopRepository.kt`, `ImportarViewModel.kt`, `KpiValueEntity.kt`, `VisitPhotoEntity.kt`, `SyncRepository.kt`

**Total: ~15 commits autocontenidos, cada uno verificable.**
