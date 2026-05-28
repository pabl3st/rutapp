package com.pabl3st.rutapp.data.repository

import com.pabl3st.rutapp.core.UserRole
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.pabl3st.rutapp.sync.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext

import com.pabl3st.rutapp.data.local.dao.DaySessionDao
import com.pabl3st.rutapp.data.local.dao.KpiValueDao
import com.pabl3st.rutapp.data.local.dao.RouteDao
import com.pabl3st.rutapp.data.local.dao.RouteStopCount
import com.pabl3st.rutapp.data.local.dao.StopDao
import com.pabl3st.rutapp.data.local.dao.StopVisitDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity
import com.pabl3st.rutapp.data.network.RouteAssignmentDto
import com.pabl3st.rutapp.data.network.RouteDto
import com.pabl3st.rutapp.data.network.RutasApiService
import com.pabl3st.rutapp.data.session.SessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val routeDao:      RouteDao,
    private val stopDao:       StopDao,
    private val stopVisitDao:  StopVisitDao,
    private val daySessionDao: DaySessionDao,
    private val kpiValueDao:   KpiValueDao,
    private val syncQueueDao:  SyncQueueDao,
    private val api:           RutasApiService,
    private val session:       SessionManager,
    private val syncGateway:   com.pabl3st.rutapp.sync.SyncGateway,
    private val moshi:         Moshi,
) {
    private val mapType    = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter by lazy { moshi.adapter<Map<String, Any?>>(mapType) }

    // ── Roles con visibilidad ampliada ────────────────────────
    /**
     * True solo para owner/god — únicos que ven TODA la cuenta sin filtro.
     * Admin/manager/agent quedan acotados a su subárbol descendente, que
     * llega del servidor en managed_agent_ids y se aplica en isManagedView.
     */
    private val isFullAccountView: Boolean
        get() = UserRole.from(session.userRole) in setOf(UserRole.OWNER, UserRole.GOD)

    /**
     * True para admin/manager — ven su subárbol descendente (hijos, nietos,
     * etc) + a sí mismos. La lista de user_ids está en session.managedAgentIds,
     * rellenada por delta_sync.
     */
    private val isManagedView: Boolean
        get() = UserRole.from(session.userRole) in setOf(UserRole.ADMIN, UserRole.MANAGER)

    fun observeToday(): Flow<List<RouteEntity>> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return when {
            isFullAccountView -> routeDao.observeByDateForAccount(session.accountId, today)
            isManagedView     -> {
                // managedAgentIds ya incluye al propio usuario (lo añade el servidor).
                // Si está vacío (primer arranque sin sync) usamos al menos al propio
                // userId para no mostrar nada de otros.
                val ids = session.managedAgentIds.takeIf { it.isNotEmpty() } ?: listOf(session.userId)
                routeDao.observeByDateForUserIds(ids, today)
            }
            else -> routeDao.observeByDate(session.userId, today)
        }
    }

    fun observeAll(): Flow<List<RouteEntity>> = when {
        isFullAccountView -> routeDao.observeByAccount(session.accountId)
        isManagedView     -> {
            val ids = session.managedAgentIds.takeIf { it.isNotEmpty() } ?: listOf(session.userId)
            routeDao.observeByUserIds(ids)
        }
        else -> routeDao.observeByUser(session.userId)
    }

    suspend fun getByUid(uid: String): RouteEntity? =
        routeDao.getByUid(uid)

    /** Conteo de paradas (total/completadas) por ruta, indexado por routeUid. */
    fun observeStopCounts(): Flow<Map<String, RouteStopCount>> =
        routeDao.observeStopCounts().map { list -> list.associateBy { it.routeUid } }

    suspend fun getByNameAndUser(name: String, userId: Int): RouteEntity? =
        routeDao.getByNameAndUser(name, userId)

    /** Borra TODAS las rutas, paradas, visitas, KPIs y la cola local de sync de la cuenta.
     *  Primero llama al servidor (también borra ahí), luego limpia Room en orden FK seguro.
     *  Tras esto el usuario puede re-importar sin colisiones con datos huérfanos.
     *  Solo owner/god — el servidor también lo valida.
     *
     *  Devuelve un string null si todo OK, o un mensaje con el motivo del fallo.
     *  Antes devolvía Boolean pero eso ocultaba excepciones reales (sin token,
     *  http 4xx/5xx, timeouts, IOException) que el usuario solo veía como
     *  'Error de conexión' aunque la causa fuese otra. */
    suspend fun clearAllRoutes(): String? {
        val token = session.token ?: return "Sin sesión activa"
        return try {
            val response = api.clearRoutes(token = token)
            if (!response.isSuccessful) {
                "HTTP ${response.code()}: ${response.message().ifBlank { "sin detalle" }}"
            } else {
                // Orden importante por dependencias FK locales:
                //   kpi_values → stop_visits → stops → routes → sync_queue (purga total)
                // sync_queue se vacía entera: cualquier op pendiente sobre entidades
                // recién borradas dejaría de tener sentido y bloquearía pushes futuros.
                kpiValueDao.deleteAllByAccount(session.accountId)
                stopVisitDao.deleteAllByAccount(session.accountId)
                stopDao.deleteAllByAccount(session.accountId)
                routeDao.deleteAllByAccount(session.accountId)
                syncQueueDao.purgeAll()
                null
            }
        } catch (e: Exception) {
            "${e.javaClass.simpleName}: ${e.message ?: "sin mensaje"}"
        }
    }

    /** Marca la ruta como completada y encola para sync */
    suspend fun markDone(uid: String) {
        val now = java.time.Instant.now().toString()
        routeDao.updateStatus(uid, "done", now)
        triggerSync()
    }

    /** Elimina una fecha concreta del array scheduledDates de la ruta.
     *  Si era la fecha principal (dateAssigned), promueve la siguiente del array.
     *  Si no quedan fechas, deja dateAssigned = "1970-01-01". */
    suspend fun unassignDate(uid: String, dateStr: String? = null) {
        val route = routeDao.getByUid(uid) ?: return
        val now   = java.time.Instant.now().toString()

        val currentDates = (route.scheduledDates ?: emptyList()).toMutableList()
        // Incluir dateAssigned si no está ya en el array
        if (route.dateAssigned.isNotBlank() && route.dateAssigned != "1970-01-01"
            && !currentDates.contains(route.dateAssigned)) {
            currentDates.add(0, route.dateAssigned)
        }

        // Quitar la fecha indicada (o todas si dateStr == null)
        val remaining = if (dateStr != null) currentDates.filter { it != dateStr } else emptyList()

        val newDateAssigned = remaining.minOrNull() ?: "1970-01-01"
        val otherDates      = remaining.filter { it != newDateAssigned }
        val newScheduled    = if (otherDates.isNotEmpty()) otherDates else null

        val updated = route.copy(
            dateAssigned   = newDateAssigned,
            scheduledDates = newScheduled,
            status         = if (remaining.isEmpty()) "pending" else route.status,
            updatedAt      = now,
            syncStatus     = "pending",
        )
        routeDao.upsert(updated)
        enqueue("route", uid, "update", routeToMap(updated))
    }

    /** Añade una fecha al array scheduledDates sin eliminar las existentes.
     *  Si la ruta no tiene fecha principal válida, la convierte en dateAssigned. */
    suspend fun assignDate(uid: String, dateStr: String) {
        // Validar formato ISO antes de procesar — rechazar strings inválidos
        val validDate = runCatching { java.time.LocalDate.parse(dateStr) }.getOrNull()
            ?: return  // fecha inválida → ignorar silenciosamente
        val route = routeDao.getByUid(uid) ?: return
        val now   = java.time.Instant.now().toString()

        val dates = (route.scheduledDates ?: emptyList()).toMutableList()
        // Incluir dateAssigned actual si es válida
        val currentMain = route.dateAssigned
        if (currentMain.isNotBlank() && currentMain != "1970-01-01" && !dates.contains(currentMain)) {
            dates.add(0, currentMain)
        }
        // Añadir la nueva fecha si no está ya
        if (!dates.contains(dateStr)) dates.add(dateStr)
        dates.sort()

        val newMain      = dates.first()
        val otherDates   = dates.drop(1)
        val newScheduled = if (otherDates.isNotEmpty()) otherDates else null

        val updated = route.copy(
            dateAssigned   = newMain,
            scheduledDates = newScheduled,
            updatedAt      = now,
            syncStatus     = "pending",
        )
        routeDao.upsert(updated)
        enqueue("route", uid, "update", routeToMap(updated))
    }

    // ── Disparar sync ─────────────────────────────────────────
    // Delegado a SyncGateway: durante operaciones masivas (importer) el
    // gateway suprime estos triggers para evitar el bombardeo histórico
    // de ~4500 enqueueUniqueWork(REPLACE) por importación que cancelaban
    // al worker en bucle y nunca llegaba a hacer batch_sync.
    private fun triggerSync() = syncGateway.trigger()

    /** Devuelve SyncQueueEntity para cada ruta local pendiente de subir al servidor.
     *  Usado por SyncRepository para re-encolar huérfanas sin duplicar lógica. */
    suspend fun getPendingOperations(): List<SyncQueueEntity> =
        routeDao.getPendingSync().map { route ->
            SyncQueueEntity(
                entity    = "route",
                entityUid = route.uid,
                operation = "create",
                payload   = mapAdapter.toJson(routeToMap(route)),
            )
        }

    suspend fun createRoute(
        name: String,
        dateAssigned: String,
        notes: String? = null,
        scheduledDates: List<String>? = null,
        forUserId: Int? = null,     // si lo pasa el manager, la ruta se crea para ese agente
    ): RouteEntity {
        require(name.isNotBlank()) { "Nombre de ruta vacío" }
        val now   = Instant.now().atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val route = RouteEntity(
            uid             = UUID.randomUUID().toString(),
            accountId       = session.accountId,
            userId          = forUserId ?: session.userId,
            name            = name,
            dateAssigned    = dateAssigned,
            scheduledDates  = scheduledDates,
            notes           = notes,
            createdAt       = now,
            updatedAt       = now,
            syncStatus      = "pending",
        )
        routeDao.upsert(route)
        enqueue("route", route.uid, "create", routeToMap(route))
        return route
    }



    /** Actualiza solo dateAssigned y scheduledDates de una ruta existente (reimportación). */
    suspend fun updateSchedule(uid: String, dateAssigned: String, scheduledDates: List<String>?) {
        val route = routeDao.getByUid(uid) ?: return
        val now   = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val updated = route.copy(
            dateAssigned   = dateAssigned,
            scheduledDates = scheduledDates,
            updatedAt      = now,
            syncStatus     = "pending",
        )
        routeDao.upsert(updated)
        enqueue("route", uid, "update", routeToMap(updated))
    }

    /** Reasigna una ruta a un usuario diferente.
     *  Solo permitido para owner/admin/manager (verificado en la capa ViewModel).
     *  El cambio se sincroniza con el servidor vía delta sync. */
    suspend fun reassignRoute(
        routeUid: String,
        newUserId: Int,
        reason: String? = null,
    ): Result<Unit> = runCatching {
        val token = session.token ?: error("Sin sesión activa")
        val route = routeDao.getByUid(routeUid) ?: error("Ruta no encontrada: $routeUid")

        // 1. Llamar al servidor — valida jerarquía, registra historial, envía FCM push
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

        // 2. Actualizar Room local
        val now = Instant.now().atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val updated = route.copy(
            userId     = newUserId,
            updatedAt  = now,
            syncStatus = "synced",  // ya está en el servidor
        )
        routeDao.upsert(updated)

        // 3. Forzar delta_sync para traer el estado actualizado
        triggerSync()
    }

    /** Historial de reasignaciones de una ruta. Solo lectura — viene del servidor. */
    suspend fun fetchRouteHistory(routeUid: String): Result<List<RouteAssignmentDto>> = runCatching {
        val token = session.token ?: error("Sin sesión activa")
        val resp  = api.routeHistory(routeUid = routeUid, token = token)
        if (!resp.isSuccessful || resp.body()?.ok != true) {
            error(resp.body()?.error ?: "No se pudo cargar el historial")
        }
        resp.body()?.history ?: emptyList()
    }

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

    // ── Delta sync desde servidor ──────────────────────────────
    private val lastFetchMs = AtomicLong(0L)

    /**
     * @param forceFull si true, ignora el timestamp incremental y descarga
     *   todo desde la época. Útil en pull-to-refresh para recuperar cambios
     *   hechos directamente en BD que el sync incremental se saltaría.
     */
    suspend fun fetchDelta(forceFull: Boolean = false): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        // El throttle de 10s no aplica a un full-sync explícito del usuario
        if (!forceFull && now - lastFetchMs.get() < 10_000L) return@runCatching
        lastFetchMs.set(now)
        val token = session.token ?: return@runCatching
        val since = if (forceFull) "2000-01-01T00:00:00Z"
                    else session.lastSyncTimestamp.ifEmpty { "2000-01-01T00:00:00Z" }
        val resp  = api.deltaSync(token = token, since = since)
        if (!resp.isSuccessful || resp.body()?.ok != true) return@runCatching

        val body = resp.body()!!
        body.routes?.map { it.toEntity(session.userId, session.accountId) }
            ?.let { routeDao.upsertAll(it) }
        body.stops?.mapNotNull { it.toEntity(session.accountId) }
            ?.let { if (it.isNotEmpty()) stopDao.upsertAll(it) }
        body.daySessions?.mapNotNull { toEntity(it) }
            ?.let { if (it.isNotEmpty()) it.forEach { s -> daySessionDao.upsert(s) } }
        body.kpiValues?.mapNotNull { toKpiEntity(it) }
            ?.let { if (it.isNotEmpty()) kpiValueDao.upsertAll(it) }
        body.serverTime?.let { session.lastSyncTimestamp = it }
    }

    private fun toEntity(dto: com.pabl3st.rutapp.data.network.DaySessionDto) =
        com.pabl3st.rutapp.data.local.entity.DaySessionEntity(
            routeUid   = dto.routeUid,
            dateStr    = dto.dateStr,
            state      = dto.state,
            startedAt  = dto.startedAt,
            elapsedMs  = dto.elapsedMs,
            distanceKm = dto.distanceKm,
            lastLat    = dto.lastLat,
            lastLng    = dto.lastLng,
            updatedAt  = dto.updatedAt,
        )

    private fun toKpiEntity(dto: com.pabl3st.rutapp.data.network.KpiValueDto): com.pabl3st.rutapp.data.local.entity.KpiValueEntity? {
        if (dto.stopUid.isBlank() || dto.kpiId.isBlank()) return null
        // Modelo C: visit_uid viene del servidor. Si el servidor aún no lo
        // envía (compat retroatrás), generamos el patrón v1 igual que el
        // back-fill de las migraciones.
        val visitUid = dto.visitUid?.takeIf { it.isNotBlank() } ?: "${dto.stopUid}-v1"
        return com.pabl3st.rutapp.data.local.entity.KpiValueEntity(
            visitUid   = visitUid,
            stopUid    = dto.stopUid,
            kpiId      = dto.kpiId,
            valueText  = dto.valueText ?: "",
            syncStatus = "synced",
        )
    }

    // ── Helpers ───────────────────────────────────────────────
    private fun routeToMap(r: RouteEntity): Map<String, Any?> = mapOf(
        "name"             to r.name,
        "date_assigned"    to r.dateAssigned,
        "scheduled_dates"  to r.scheduledDates?.joinToString(","),
        "status"           to r.status,
        "notes"            to r.notes,
        "created_at"       to r.createdAt,
        "user_id"          to r.userId,   // necesario para asignación a agente y reasignación
    )

    private suspend fun enqueue(entity: String, uid: String, op: String, data: Map<String, Any?>) {
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entity    = entity,
                entityUid = uid,
                operation = op,
                payload   = mapAdapter.toJson(data),
            )
        )
        // Sync inmediato: todo encolado intenta subir ya si hay red.
        // Centralizado aquí para que ninguna escritura futura lo olvide.
        triggerSync()
    }

    /**
     * Encola un batch de KPI values asociado a una visita concreta para sync.
     * El servidor espera UN op por (stopUid, visitUid) con todos los KPIs como
     * objeto `values: {kpi_id → value_text}`. El uid del op es el visitUid
     * (clave natural en el modelo C, único por (stop, fecha)).
     *
     * Usado por el importer cuando carga reports históricos: en vez de insertar
     * en kpi_values con syncStatus="synced" (que NO sube al server), se encola
     * para que el siguiente runSync lo envíe.
     */
    suspend fun enqueueKpiValuesBatch(
        stopUid:  String,
        visitUid: String,
        values:   Map<String, String>,
    ) {
        if (values.isEmpty()) return
        enqueue(
            entity = "kpi_values",
            uid    = visitUid,
            op     = "upsert",
            data   = mapOf(
                "stopUid"  to stopUid,
                "visitUid" to visitUid,
                "values"   to values,
            ),
        )
    }
}
