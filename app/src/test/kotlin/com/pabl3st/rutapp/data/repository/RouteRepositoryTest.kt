package com.pabl3st.rutapp.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.pabl3st.rutapp.data.local.dao.RouteDao
import com.pabl3st.rutapp.data.local.dao.StopDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.network.DeltaSyncResponse
import com.pabl3st.rutapp.data.network.RutasApiService
import com.pabl3st.rutapp.util.FakeSessionManager
import com.pabl3st.rutapp.util.TestFixtures
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import retrofit2.Response

/**
 * Tests unitarios para RouteRepository.
 * EXTENSIÓN: añadir test cuando se añada nueva lógica a RouteRepository.
 */
@RunWith(JUnit4::class)
class RouteRepositoryTest {

    private lateinit var routeDao: RouteDao
    private lateinit var stopDao: StopDao
    private lateinit var syncQueueDao: SyncQueueDao
    private lateinit var api: RutasApiService
    private lateinit var session: FakeSessionManager
    private lateinit var repo: RouteRepository

    @Before
    fun setup() {
        routeDao     = mockk(relaxed = true)
        stopDao      = mockk(relaxed = true)
        syncQueueDao = mockk(relaxed = true)
        api          = mockk(relaxed = true)
        session      = FakeSessionManager()
        repo = RouteRepository(routeDao, stopDao, syncQueueDao, api, session,
            Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build())
    }

    @Test
    fun `observeToday delega al DAO con userId correcto`() = runTest {
        every { routeDao.observeByDate(any(), any()) } returns flowOf(listOf(TestFixtures.routeEntity()))

        repo.observeToday().test {
            assertThat(awaitItem()).hasSize(1)
            awaitComplete()
        }

        verify { routeDao.observeByDate(TestFixtures.USER_ID, any()) }
    }

    @Test
    fun `observeAll delega al DAO con userId correcto`() = runTest {
        every { routeDao.observeByUser(any()) } returns flowOf(listOf(
            TestFixtures.routeEntity(uid = "r1"), TestFixtures.routeEntity(uid = "r2")))

        repo.observeAll().test {
            assertThat(awaitItem()).hasSize(2)
            awaitComplete()
        }

        verify { routeDao.observeByUser(TestFixtures.USER_ID) }
    }

    @Test
    fun `createRoute llama upsert en routeDao`() = runTest {
        repo.createRoute(name = "Nueva Ruta", dateAssigned = "2026-05-01")
        coVerify { routeDao.upsert(any()) }
    }

    @Test
    fun `createRoute encola operacion create en SyncQueue`() = runTest {
        repo.createRoute(name = "Nueva Ruta", dateAssigned = "2026-05-01")
        coVerify { syncQueueDao.enqueue(match { it.entity == "route" && it.operation == "create" }) }
    }

    @Test
    fun `createRoute retorna entidad con syncStatus pending y datos correctos`() = runTest {
        val route = repo.createRoute(name = "Test", dateAssigned = "2026-05-01", notes = "Nota")
        assertThat(route.syncStatus).isEqualTo("pending")
        assertThat(route.name).isEqualTo("Test")
        assertThat(route.notes).isEqualTo("Nota")
        assertThat(route.userId).isEqualTo(TestFixtures.USER_ID)
        assertThat(route.accountId).isEqualTo(TestFixtures.ACCOUNT_ID)
    }

    @Test
    fun `createRoute genera uid unico cada vez`() = runTest {
        val r1 = repo.createRoute("R1", "2026-05-01")
        val r2 = repo.createRoute("R2", "2026-05-01")
        assertThat(r1.uid).isNotEqualTo(r2.uid)
    }

    @Test
    fun `fetchDelta sin token no llama a la API`() = runTest {
        session.setNoAuth()
        repo.fetchDelta()
        coVerify(exactly = 0) { api.deltaSync(any(), any(), any()) }
    }

    @Test
    fun `fetchDelta guarda routes en Room`() = runTest {
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse(stops = emptyList()))

        repo.fetchDelta()

        coVerify { routeDao.upsertAll(any()) }
    }

    @Test
    fun `fetchDelta guarda stops en Room - regression stops no aparecian`() = runTest {
        val response = TestFixtures.deltaSyncResponse(
            stops = listOf(
                TestFixtures.stopDto(uid = "s1", routeUid = "route-uid-001"),
                TestFixtures.stopDto(uid = "s2", routeUid = "route-uid-001"),
            )
        )
        coEvery { api.deltaSync(any(), any(), any()) } returns Response.success(response)

        repo.fetchDelta()

        coVerify { stopDao.upsertAll(match { it.size == 2 }) }
    }

    @Test
    fun `fetchDelta ignora stops con routeUid null`() = runTest {
        val response = TestFixtures.deltaSyncResponse(
            stops = listOf(
                TestFixtures.stopDto(uid = "s1", routeUid = "route-uid-001"),
                TestFixtures.stopDto(uid = "s2", routeUid = null),
            )
        )
        coEvery { api.deltaSync(any(), any(), any()) } returns Response.success(response)

        repo.fetchDelta()

        coVerify { stopDao.upsertAll(match { it.size == 1 }) }
    }

    @Test
    fun `fetchDelta actualiza lastSyncTimestamp`() = runTest {
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse(serverTime = "2026-05-01T20:00:00Z"))

        repo.fetchDelta()

        assertThat(session.lastSyncTimestamp).isEqualTo("2026-05-01T20:00:00Z")
    }

    @Test
    fun `fetchDelta no persiste nada con respuesta HTTP error`() = runTest {
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.error(401, okhttp3.ResponseBody.create(null, ""))

        repo.fetchDelta()

        coVerify(exactly = 0) { routeDao.upsertAll(any()) }
        coVerify(exactly = 0) { stopDao.upsertAll(any()) }
    }

    @Test
    fun `fetchDelta no persiste nada cuando ok es false`() = runTest {
        coEvery { api.deltaSync(any(), any(), any()) } returns Response.success(
            DeltaSyncResponse(ok = false, routes = null, stops = null, serverTime = null, error = "Unauthorized"))

        repo.fetchDelta()

        coVerify(exactly = 0) { routeDao.upsertAll(any()) }
    }

    @Test
    fun `fetchDelta usa 2000 como since cuando lastSyncTimestamp esta vacio`() = runTest {
        session.lastSyncTimestamp = ""
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse())

        repo.fetchDelta()

        coVerify { api.deltaSync(any(), any(), since = "2000-01-01T00:00:00Z") }
    }

    @Test
    fun `fetchDelta usa lastSyncTimestamp previo como since`() = runTest {
        session.lastSyncTimestamp = "2026-04-30T10:00:00Z"
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse())

        repo.fetchDelta()

        coVerify { api.deltaSync(any(), any(), since = "2026-04-30T10:00:00Z") }
    }
}

// ── Tests de cascada de visibilidad por rol ────────────────

    @Test
    fun `observeAll como owner usa observeByAccount (ve todo el account)`() = runTest {
        session.userRole = "owner"
        val routes = listOf(TestFixtures.routeEntity())
        every { routeDao.observeByAccount(any()) } returns flowOf(routes)

        repo.observeAll().test {
            assertThat(awaitItem()).isEqualTo(routes)
            cancelAndIgnoreRemainingEvents()
        }
        verify { routeDao.observeByAccount(session.accountId) }
    }

    @Test
    fun `observeAll como manager con agentes usa observeByUserIds`() = runTest {
        session.userRole       = "manager"
        session.managedAgentIds = listOf(10, 11)
        val agentRoute = TestFixtures.routeEntity(userId = 10)
        every { routeDao.observeByUserIds(any()) } returns flowOf(listOf(agentRoute))

        repo.observeAll().test {
            assertThat(awaitItem()).contains(agentRoute)
            cancelAndIgnoreRemainingEvents()
        }
        // Debe incluir los agentIds + el propio userId del manager
        verify { routeDao.observeByUserIds(match { it.containsAll(listOf(10, 11)) }) }
    }

    @Test
    fun `observeAll como manager sin agentes solo ve sus propias rutas`() = runTest {
        session.userRole        = "manager"
        session.managedAgentIds = emptyList()
        val ownRoute = TestFixtures.routeEntity(userId = session.userId)
        every { routeDao.observeByUser(session.userId) } returns flowOf(listOf(ownRoute))

        repo.observeAll().test {
            assertThat(awaitItem()).contains(ownRoute)
            cancelAndIgnoreRemainingEvents()
        }
        verify { routeDao.observeByUser(session.userId) }
    }

    @Test
    fun `observeAll como agent solo ve sus propias rutas`() = runTest {
        session.userRole = "agent"
        val ownRoute = TestFixtures.routeEntity(userId = session.userId)
        every { routeDao.observeByUser(session.userId) } returns flowOf(listOf(ownRoute))

        repo.observeAll().test {
            assertThat(awaitItem()).isEqualTo(listOf(ownRoute))
            cancelAndIgnoreRemainingEvents()
        }
        verify { routeDao.observeByUser(session.userId) }
        verify(exactly = 0) { routeDao.observeByAccount(any()) }
    }

    @Test
    fun `createRoute con forUserId asigna la ruta al usuario indicado`() = runTest {
        session.userRole = "manager"
        val targetUserId = 42

        repo.createRoute(name = "Ruta agente", dateAssigned = "2026-05-20", forUserId = targetUserId)

        coVerify {
            routeDao.upsert(match { it.userId == targetUserId })
        }
    }

    @Test
    fun `createRoute sin forUserId asigna la ruta al caller`() = runTest {
        session.userRole = "agent"

        repo.createRoute(name = "Mi ruta", dateAssigned = "2026-05-20", forUserId = null)

        coVerify {
            routeDao.upsert(match { it.userId == session.userId })
        }
    }
