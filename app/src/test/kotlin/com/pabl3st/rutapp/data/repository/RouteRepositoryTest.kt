package com.pabl3st.rutapp.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.pabl3st.rutapp.data.local.dao.RouteDao
import com.pabl3st.rutapp.data.local.dao.StopDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import com.pabl3st.rutapp.data.network.DeltaSyncResponse
import com.pabl3st.rutapp.data.network.RutasApiService
import com.pabl3st.rutapp.util.FakeSessionManager
import com.pabl3st.rutapp.util.TestFixtures
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import retrofit2.Response

/**
 * Tests para RouteRepository
 *
 * COBERTURA:
 * - observeToday(): delega correctamente al DAO con userId y fecha
 * - observeAll(): delega al DAO con userId
 * - getByUid(): delega al DAO
 * - createRoute(): persiste en Room + encola en SyncQueue con payload correcto
 * - fetchDelta(): guarda routes Y stops en Room, actualiza lastSyncTimestamp
 * - fetchDelta() sin token: no hace llamada API
 * - fetchDelta() con respuesta de error: no persiste nada
 * - fetchDelta() con stops sin route_uid: los descarta (toEntity retorna null)
 *
 * EXTENSIÓN: añadir tests aquí cuando se añadan métodos a RouteRepository
 */
@RunWith(JUnit4::class)
class RouteRepositoryTest {

    private lateinit var routeDao:     RouteDao
    private lateinit var stopDao:      StopDao
    private lateinit var syncQueueDao: SyncQueueDao
    private lateinit var api:          RutasApiService
    private lateinit var session:      FakeSessionManager
    private lateinit var moshi:        Moshi
    private lateinit var repo:         RouteRepository

    @Before
    fun setUp() {
        routeDao     = mockk(relaxed = true)
        stopDao      = mockk(relaxed = true)
        syncQueueDao = mockk(relaxed = true)
        api          = mockk(relaxed = true)
        session      = FakeSessionManager()
        moshi        = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        repo         = RouteRepository(routeDao, stopDao, syncQueueDao, api, session, moshi)
    }

    // ── observeToday ─────────────────────────────────────────

    @Test
    fun `observeToday llama al DAO con userId correcto`() = runTest {
        val routes = listOf(TestFixtures.routeEntity())
        coEvery { routeDao.observeByDate(any(), any()) } returns flowOf(routes)

        repo.observeToday().test {
            assertThat(awaitItem()).isEqualTo(routes)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { routeDao.observeByDate(TestFixtures.USER_ID, any()) }
    }

    @Test
    fun `observeToday emite lista vacia cuando no hay rutas`() = runTest {
        coEvery { routeDao.observeByDate(any(), any()) } returns flowOf(emptyList())

        repo.observeToday().test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── observeAll ────────────────────────────────────────────

    @Test
    fun `observeAll llama al DAO con userId correcto`() = runTest {
        coEvery { routeDao.observeByUser(any()) } returns flowOf(emptyList())

        repo.observeAll().test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { routeDao.observeByUser(TestFixtures.USER_ID) }
    }

    // ── getByUid ──────────────────────────────────────────────

    @Test
    fun `getByUid retorna entidad del DAO`() = runTest {
        val entity = TestFixtures.routeEntity()
        coEvery { routeDao.getByUid("route-uid-001") } returns entity

        val result = repo.getByUid("route-uid-001")
        assertThat(result).isEqualTo(entity)
    }

    @Test
    fun `getByUid retorna null cuando no existe`() = runTest {
        coEvery { routeDao.getByUid(any()) } returns null

        val result = repo.getByUid("no-existe")
        assertThat(result).isNull()
    }

    // ── createRoute ───────────────────────────────────────────

    @Test
    fun `createRoute persiste en Room con datos correctos`() = runTest {
        val entitySlot = slot<RouteEntity>()
        coEvery { routeDao.upsert(capture(entitySlot)) } returns Unit

        val result = repo.createRoute(
            name         = "Nueva Ruta",
            dateAssigned = "2026-05-02",
            notes        = "Notas",
        )

        assertThat(result.name).isEqualTo("Nueva Ruta")
        assertThat(result.dateAssigned).isEqualTo("2026-05-02")
        assertThat(result.notes).isEqualTo("Notas")
        assertThat(result.userId).isEqualTo(TestFixtures.USER_ID)
        assertThat(result.accountId).isEqualTo(TestFixtures.ACCOUNT_ID)
        assertThat(result.syncStatus).isEqualTo("pending")
        assertThat(result.uid).isNotEmpty()

        coVerify { routeDao.upsert(any()) }
    }

    @Test
    fun `createRoute encola operacion en SyncQueue`() = runTest {
        repo.createRoute(name = "Test", dateAssigned = "2026-05-01")

        coVerify {
            syncQueueDao.enqueue(match { item ->
                item.entity == "route" &&
                item.operation == "create" &&
                item.payload.contains("Test")
            })
        }
    }

    @Test
    fun `createRoute genera uid unico cada vez`() = runTest {
        val uid1 = repo.createRoute("Ruta 1", "2026-05-01").uid
        val uid2 = repo.createRoute("Ruta 2", "2026-05-01").uid
        assertThat(uid1).isNotEqualTo(uid2)
    }

    @Test
    fun `createRoute sin notas persiste null`() = runTest {
        val result = repo.createRoute(name = "Sin notas", dateAssigned = "2026-05-01")
        assertThat(result.notes).isNull()
    }

    // ── fetchDelta ────────────────────────────────────────────

    @Test
    fun `fetchDelta guarda routes en Room`() = runTest {
        val routes = listOf(TestFixtures.routeDto(), TestFixtures.routeDto(uid = "uid-2", id = 2))
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse(routes = routes, stops = emptyList()))

        repo.fetchDelta()

        coVerify { routeDao.upsertAll(match { it.size == 2 }) }
    }

    @Test
    fun `fetchDelta guarda stops en Room — bug critico prevenido`() = runTest {
        // Este test previene la regresión del bug donde fetchDelta guardaba
        // routes pero ignoraba completamente los stops
        val stops = listOf(
            TestFixtures.stopDto(uid = "stop-1"),
            TestFixtures.stopDto(uid = "stop-2", routeUid = "route-uid-001"),
        )
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse(stops = stops))

        repo.fetchDelta()

        coVerify { stopDao.upsertAll(match { it.size == 2 }) }
    }

    @Test
    fun `fetchDelta descarta stops sin route_uid`() = runTest {
        val stops = listOf(
            TestFixtures.stopDto(uid = "stop-valido", routeUid = "uid-001"),
            TestFixtures.stopDto(uid = "stop-invalido", routeUid = null),
        )
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse(stops = stops))

        repo.fetchDelta()

        // Solo 1 stop válido debe persistirse
        coVerify { stopDao.upsertAll(match { it.size == 1 }) }
    }

    @Test
    fun `fetchDelta actualiza lastSyncTimestamp`() = runTest {
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse())

        repo.fetchDelta()

        assertThat(session.lastSyncTimestamp).isEqualTo(TestFixtures.SERVER_TIME)
    }

    @Test
    fun `fetchDelta sin token no llama a la API`() = runTest {
        session.setNoAuth()

        repo.fetchDelta()

        coVerify(exactly = 0) { api.deltaSync(any(), any(), any()) }
    }

    @Test
    fun `fetchDelta con respuesta de error no persiste nada`() = runTest {
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(DeltaSyncResponse(
                ok = false, routes = null, stops = null,
                serverTime = null, error = "Unauthorized"
            ))

        repo.fetchDelta()

        coVerify(exactly = 0) { routeDao.upsertAll(any()) }
        coVerify(exactly = 0) { stopDao.upsertAll(any()) }
    }

    @Test
    fun `fetchDelta con excepcion de red no propaga el error`() = runTest {
        coEvery { api.deltaSync(any(), any(), any()) } throws Exception("Network error")

        // No debe lanzar excepción — fetchDelta usa runCatching
        val result = repo.fetchDelta()
        assertThat(result.isFailure).isFalse()
    }

    @Test
    fun `fetchDelta usa since de lastSyncTimestamp`() = runTest {
        session.lastSyncTimestamp = "2026-05-01T10:00:00Z"
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse())

        repo.fetchDelta()

        coVerify { api.deltaSync(any(), any(), "2026-05-01T10:00:00Z") }
    }

    @Test
    fun `fetchDelta con lastSyncTimestamp vacio usa fecha por defecto`() = runTest {
        session.lastSyncTimestamp = ""
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse())

        repo.fetchDelta()

        coVerify { api.deltaSync(any(), any(), "2000-01-01T00:00:00Z") }
    }
}
