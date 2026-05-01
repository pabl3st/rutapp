package com.pabl3st.rutapp.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.pabl3st.rutapp.data.local.dao.StopDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.local.entity.StopEntity
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

@RunWith(JUnit4::class)
class StopRepositoryTest {

    private lateinit var stopDao:      StopDao
    private lateinit var syncQueueDao: SyncQueueDao
    private lateinit var session:      FakeSessionManager
    private lateinit var moshi:        Moshi
    private lateinit var repo:         StopRepository

    @Before
    fun setUp() {
        stopDao      = mockk(relaxed = true)
        syncQueueDao = mockk(relaxed = true)
        session      = FakeSessionManager()
        moshi        = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        repo         = StopRepository(stopDao, syncQueueDao, session, moshi)
    }

    // ── observeByRoute ────────────────────────────────────────

    @Test
    fun `observeByRoute delega al DAO con routeUid correcto`() = runTest {
        val stops = listOf(TestFixtures.stopEntity())
        coEvery { stopDao.observeByRoute("route-uid-001") } returns flowOf(stops)

        repo.observeByRoute("route-uid-001").test {
            assertThat(awaitItem()).isEqualTo(stops)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { stopDao.observeByRoute("route-uid-001") }
    }

    @Test
    fun `observeByRoute emite lista vacia cuando no hay stops`() = runTest {
        coEvery { stopDao.observeByRoute(any()) } returns flowOf(emptyList())

        repo.observeByRoute("uid-vacio").test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── createStop ────────────────────────────────────────────

    @Test
    fun `createStop persiste en Room con datos correctos`() = runTest {
        val entitySlot = slot<StopEntity>()
        coEvery { stopDao.upsert(capture(entitySlot)) } returns Unit

        val result = repo.createStop(
            routeUid   = "route-uid-001",
            name       = "Cliente Test",
            address    = "Calle Mayor 1",
            lat        = 39.47,
            lng        = -0.37,
            orderIndex = 2,
            notes      = "Llamar antes",
        )

        assertThat(result.routeUid).isEqualTo("route-uid-001")
        assertThat(result.name).isEqualTo("Cliente Test")
        assertThat(result.address).isEqualTo("Calle Mayor 1")
        assertThat(result.lat).isWithin(0.001).of(39.47)
        assertThat(result.lng).isWithin(0.001).of(-0.37)
        assertThat(result.orderIndex).isEqualTo(2)
        assertThat(result.notes).isEqualTo("Llamar antes")
        assertThat(result.status).isEqualTo("pending")
        assertThat(result.syncStatus).isEqualTo("pending")
        assertThat(result.accountId).isEqualTo(TestFixtures.ACCOUNT_ID)
        assertThat(result.uid).isNotEmpty()
    }

    @Test
    fun `createStop encola operacion create en SyncQueue`() = runTest {
        repo.createStop(routeUid = "route-uid-001", name = "Test Stop")

        coVerify {
            syncQueueDao.enqueue(match { item ->
                item.entity    == "stop" &&
                item.operation == "create" &&
                item.payload.contains("route-uid-001")
            })
        }
    }

    @Test
    fun `createStop genera uid unico cada vez`() = runTest {
        val uid1 = repo.createStop("route-1", "Stop A").uid
        val uid2 = repo.createStop("route-1", "Stop B").uid
        assertThat(uid1).isNotEqualTo(uid2)
    }

    @Test
    fun `createStop sin coordenadas guarda null`() = runTest {
        val result = repo.createStop(routeUid = "r", name = "Sin GPS")
        assertThat(result.lat).isNull()
        assertThat(result.lng).isNull()
    }

    @Test
    fun `createStop orderIndex por defecto es 0`() = runTest {
        val result = repo.createStop(routeUid = "r", name = "Primer stop")
        assertThat(result.orderIndex).isEqualTo(0)
    }

    // ── markVisited ───────────────────────────────────────────

    @Test
    fun `markVisited actualiza status a done en el DAO`() = runTest {
        val stop = TestFixtures.stopEntity(uid = "stop-uid-001", status = "pending")
        coEvery { stopDao.getByUid("stop-uid-001") } returns stop

        repo.markVisited("stop-uid-001")

        coVerify {
            stopDao.updateStatus(
                uid       = "stop-uid-001",
                status    = "done",
                at        = any(),
                updatedAt = any(),
            )
        }
    }

    @Test
    fun `markVisited encola operacion update en SyncQueue`() = runTest {
        val stop = TestFixtures.stopEntity(uid = "stop-uid-001")
        coEvery { stopDao.getByUid("stop-uid-001") } returns stop

        repo.markVisited("stop-uid-001")

        coVerify {
            syncQueueDao.enqueue(match { item ->
                item.entity    == "stop" &&
                item.entityUid == "stop-uid-001" &&
                item.operation == "update"
            })
        }
    }

    @Test
    fun `markVisited con uid inexistente no encola nada`() = runTest {
        coEvery { stopDao.getByUid(any()) } returns null

        repo.markVisited("uid-no-existe")

        // updateStatus sí se llama (Room update por UID es idempotente)
        // pero NO debe encolarse si no existe el stop
        coVerify(exactly = 0) {
            syncQueueDao.enqueue(match { it.entityUid == "uid-no-existe" && it.operation == "update" })
        }
    }
}
