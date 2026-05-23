package com.pabl3st.rutapp.data.repository
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.pabl3st.rutapp.sync.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext

import com.pabl3st.rutapp.data.local.dao.DaySessionDao
import com.pabl3st.rutapp.data.local.dao.KpiValueDao
import com.pabl3st.rutapp.data.local.dao.RouteDao
import com.pabl3st.rutapp.data.local.dao.StopDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity
import com.pabl3st.rutapp.data.network.RouteDto
import com.pabl3st.rutapp.data.network.RutasApiService
import com.pabl3st.rutapp.data.session.SessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
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
    private val daySessionDao: DaySessionDao,
    private val kpiValueDao:   KpiValueDao,
    private val syncQueueDao:  SyncQueueDao,
    private val api:           RutasApiService,
    private val session:       SessionManager,
    private val moshi:         Moshi,
) {
    private val mapType    = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter by lazy { moshi.adapter<Map<String, Any?>>(mapType) }

    // ── Roles con visibilidad ampliada ────────────────────────
    /** True solo para roles con visión completa de la cuenta */
    private val isFullAccountView: Boolean
        get() = session.userRole in listOf("owner", "admin", "god")

    /** Manager ve solo sus agentes directos */
    private val isManagedView: Boolean
        get() = session.userRole == "manager"

    fun observeToday(): Flow<List<RouteEntity>> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return when {
            isFullAccountView -> routeDao.observeByDateForAccount(session.accountId, today)
            isManagedView     -> {
                val agentIds = session.managedAgentIds
                if (agentIds.isEmpty()) routeDao.observeByDate(session.userId, today)
                else routeDao.observeByDateForUserIds(agentIds + session.userId, today)
            }
            else -> routeDao.observeByDate(session.userId, today)
        }
    }

    fun observeAll(): Flow<List<RouteEntity>> = when {
        isFullAccountView -> routeDao.observeByAccount(session.accountId)
        isManagedView     -> {
            val agentIds = session.managedAgentIds
            if (agentIds.isEmpty()) routeDao.observeByUser(session.userId)
            else routeDao.observeByUserIds(agentIds + session.userId)
        }
        else -> routeDao.observeByUser(session.userId)
    }

    suspend fun getByUid(uid: String): RouteEntity? =
        routeDao.getByUid(uid)

    suspend fun getByNameAndUser(name: String, userId: Int): RouteEntity? =
        routeDao.getByNameAndUser(name, userId)

    /** Borra TODAS las rutas y paradas de la cuenta (solo owner/god).
     *  Primero llama al servidor, luego limpia Room local. */
    suspend fun clearAllRoutes(): Boolean {
        return try {
            val token = session.token ?: return false
            val response = api.clearRoutes(token = token)
            if (response.isSuccessful) {
                routeDao.deleteAllByAccount(session.accountId)
                stopDao.deleteAllByAccount(session.accountId)
                true
            } else false
        } catch (e: Exception) { false }
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

    // ── Crear ruta localmente + encolar sync ──────────────────
    private fun triggerSync() {
        runCatching {
            WorkManager.getInstance(appContext)
                .enqueueUniqueWork(
                    SyncWorker.WORK_NAME_ONDEMAND,
                    ExistingWorkPolicy.REPLACE,
                    SyncWorker.onDemandRequest(),
                )
        }
    }

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
        triggerSync()
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
        triggerSync()
    }

    /** Reasigna una ruta a un usuario diferente.
     *  Solo permitido para owner/admin/manager (verificado en la capa ViewModel).
     *  El cambio se sincroniza con el servidor vía delta sync. */
    suspend fun reassignRoute(routeUid: String, newUserId: Int): Result<Unit> = runCatching {
        val token = session.token ?: error("Sin sesión activa")
        val route = routeDao.getByUid(routeUid) ?: error("Ruta no encontrada: $routeUid")

        // 1. Llamar al servidor — valida jerarquía y envía FCM push al nuevo agente
        val response = api.assignRoute(
            token = token,
            body  = mapOf("route_uid" to routeUid, "new_user_id" to newUserId),
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

    // ── Delta sync desde servidor ──────────────────────────────
    private val lastFetchMs = AtomicLong(0L)

    suspend fun fetchDelta(): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        if (now - lastFetchMs.get() < 10_000L) return@runCatching
        lastFetchMs.set(now)
        val token = session.token ?: return@runCatching
        val since = session.lastSyncTimestamp.ifEmpty { "2000-01-01T00:00:00Z" }
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
        return com.pabl3st.rutapp.data.local.entity.KpiValueEntity(
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
    }
}
