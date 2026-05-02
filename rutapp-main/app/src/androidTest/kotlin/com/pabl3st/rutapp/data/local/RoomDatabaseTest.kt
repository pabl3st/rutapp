package com.pabl3st.rutapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.pabl3st.rutapp.util.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests de integración con Room en memoria.
 * Estos tests se ejecutan en el emulador/dispositivo (androidTest).
 *
 * COBERTURA:
 * - RouteDao: upsert, observeByUser, observeByDate, getPendingSync,
 *             updateSyncStatus, deleteByUid, getByUid
 * - StopDao: upsert, observeByRoute, getPendingSync, updateSyncStatus, updateStatus
 * - SyncQueueDao: enqueue, getNext50, delete, markFailed, count
 *
 * EXTENSIÓN: añadir tests cuando se añadan nuevas tablas o DAOs
 */
@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {

    private lateinit var db:      RutasDatabase
    private val routeDao   get() = db.routeDao()
    private val stopDao    get() = db.stopDao()
    private val syncDao    get() = db.syncQueueDao()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RutasDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() { db.close() }

    // ── RouteDao ──────────────────────────────────────────────

    @Test
    fun `routeDao upsert y getByUid`() = runTest {
        val route = TestFixtures.routeEntity()
        routeDao.upsert(route)
        val result = routeDao.getByUid("route-uid-001")
        assertThat(result).isNotNull()
        assertThat(result!!.name).isEqualTo("Ruta Centro Valencia")
    }

    @Test
    fun `routeDao upsert actualiza entidad existente`() = runTest {
        val route = TestFixtures.routeEntity()
        routeDao.upsert(route)
        routeDao.upsert(route.copy(name = "Nombre Actualizado"))
        val result = routeDao.getByUid("route-uid-001")
        assertThat(result!!.name).isEqualTo("Nombre Actualizado")
    }

    @Test
    fun `routeDao upsertAll inserta multiples rutas`() = runTest {
        val routes = listOf(
            TestFixtures.routeEntity(uid = "uid-1"),
            TestFixtures.routeEntity(uid = "uid-2"),
            TestFixtures.routeEntity(uid = "uid-3"),
        )
        routeDao.upsertAll(routes)
        assertThat(routeDao.getByUid("uid-1")).isNotNull()
        assertThat(routeDao.getByUid("uid-2")).isNotNull()
        assertThat(routeDao.getByUid("uid-3")).isNotNull()
    }

    @Test
    fun `routeDao observeByUser emite rutas del usuario`() = runTest {
        routeDao.upsert(TestFixtures.routeEntity(uid = "u1", userId = 2))
        routeDao.upsert(TestFixtures.routeEntity(uid = "u2", userId = 99)) // otro usuario

        routeDao.observeByUser(2).test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items[0].uid).isEqualTo("u1")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `routeDao observeByDate filtra por fecha`() = runTest {
        routeDao.upsert(TestFixtures.routeEntity(uid = "hoy",   dateAssigned = "2026-05-01"))
        routeDao.upsert(TestFixtures.routeEntity(uid = "mañana", dateAssigned = "2026-05-02"))

        routeDao.observeByDate(TestFixtures.USER_ID, "2026-05-01").test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items[0].uid).isEqualTo("hoy")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `routeDao observeByDate no incluye rutas con deletedAt`() = runTest {
        routeDao.upsert(TestFixtures.routeEntity(uid = "activa",  deletedAt = null))
        routeDao.upsert(TestFixtures.routeEntity(uid = "borrada", deletedAt = "2026-05-01T10:00:00Z"))

        routeDao.observeByDate(TestFixtures.USER_ID, "2026-05-01").test {
            val items = awaitItem()
            assertThat(items.map { it.uid }).doesNotContain("borrada")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `routeDao getPendingSync retorna rutas con syncStatus pending o error`() = runTest {
        routeDao.upsert(TestFixtures.routeEntity(uid = "p1", syncStatus = "pending"))
        routeDao.upsert(TestFixtures.routeEntity(uid = "p2", syncStatus = "error"))
        routeDao.upsert(TestFixtures.routeEntity(uid = "p3", syncStatus = "synced"))

        val pending = routeDao.getPendingSync()
        assertThat(pending.map { it.uid }).containsExactly("p1", "p2")
    }

    @Test
    fun `routeDao updateSyncStatus cambia el estado correctamente`() = runTest {
        routeDao.upsert(TestFixtures.routeEntity(syncStatus = "pending"))
        routeDao.updateSyncStatus("route-uid-001", "synced", "2026-05-01T10:00:00Z")

        val updated = routeDao.getByUid("route-uid-001")
        assertThat(updated!!.syncStatus).isEqualTo("synced")
        assertThat(updated.syncedAt).isEqualTo("2026-05-01T10:00:00Z")
    }

    @Test
    fun `routeDao deleteByUid elimina la ruta`() = runTest {
        routeDao.upsert(TestFixtures.routeEntity())
        routeDao.deleteByUid("route-uid-001")
        assertThat(routeDao.getByUid("route-uid-001")).isNull()
    }

    // ── StopDao ───────────────────────────────────────────────

    @Test
    fun `stopDao upsert y getByUid`() = runTest {
        routeDao.upsert(TestFixtures.routeEntity())  // FK necesaria
        stopDao.upsert(TestFixtures.stopEntity())
        val result = stopDao.getByUid("stop-uid-001")
        assertThat(result).isNotNull()
        assertThat(result!!.name).isEqualTo("Distribuciones Martínez")
    }

    @Test
    fun `stopDao observeByRoute filtra por ruta`() = runTest {
        routeDao.upsert(TestFixtures.routeEntity(uid = "r1"))
        routeDao.upsert(TestFixtures.routeEntity(uid = "r2"))
        stopDao.upsert(TestFixtures.stopEntity(uid = "s1", routeUid = "r1"))
        stopDao.upsert(TestFixtures.stopEntity(uid = "s2", routeUid = "r2"))

        stopDao.observeByRoute("r1").test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items[0].uid).isEqualTo("s1")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stopDao observeByRoute ordena por orderIndex`() = runTest {
        routeDao.upsert(TestFixtures.routeEntity())
        stopDao.upsert(TestFixtures.stopEntity(uid = "s3", orderIndex = 2))
        stopDao.upsert(TestFixtures.stopEntity(uid = "s1", orderIndex = 0))
        stopDao.upsert(TestFixtures.stopEntity(uid = "s2", orderIndex = 1))

        stopDao.observeByRoute("route-uid-001").test {
            val items = awaitItem()
            assertThat(items.map { it.uid }).containsExactly("s1", "s2", "s3").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stopDao observeByRoute no incluye stops con deletedAt`() = runTest {
        routeDao.upsert(TestFixtures.routeEntity())
        stopDao.upsert(TestFixtures.stopEntity(uid = "activo"))
        stopDao.upsert(TestFixtures.stopEntity(uid = "borrado").copy(deletedAt = "2026-05-01T10:00:00Z"))

        stopDao.observeByRoute("route-uid-001").test {
            val items = awaitItem()
            assertThat(items.map { it.uid }).doesNotContain("borrado")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stopDao updateStatus cambia a done con visitedAt`() = runTest {
        routeDao.upsert(TestFixtures.routeEntity())
        stopDao.upsert(TestFixtures.stopEntity(status = "pending"))

        stopDao.updateStatus("stop-uid-001", "done", "2026-05-01T10:30:00Z", "2026-05-01T10:30:00Z")

        val updated = stopDao.getByUid("stop-uid-001")
        assertThat(updated!!.status).isEqualTo("done")
        assertThat(updated.visitedAt).isEqualTo("2026-05-01T10:30:00Z")
        assertThat(updated.syncStatus).isEqualTo("pending")  // marcado para sync
    }

    @Test
    fun `stopDao getPendingSync retorna stops pendientes`() = runTest {
        routeDao.upsert(TestFixtures.routeEntity())
        stopDao.upsert(TestFixtures.stopEntity(uid = "p", syncStatus = "pending"))
        stopDao.upsert(TestFixtures.stopEntity(uid = "e", syncStatus = "error"))
        stopDao.upsert(TestFixtures.stopEntity(uid = "s", syncStatus = "synced"))

        val pending = stopDao.getPendingSync()
        assertThat(pending.map { it.uid }).containsExactly("p", "e")
    }

    // ── SyncQueueDao ──────────────────────────────────────────

    @Test
    fun `syncQueueDao enqueue y count`() = runTest {
        assertThat(syncDao.count()).isEqualTo(0)
        syncDao.enqueue(TestFixtures.syncQueueEntity())
        assertThat(syncDao.count()).isEqualTo(1)
    }

    @Test
    fun `syncQueueDao getNext50 retorna en orden FIFO`() = runTest {
        syncDao.enqueue(TestFixtures.syncQueueEntity(entityUid = "uid-1"))
        syncDao.enqueue(TestFixtures.syncQueueEntity(entityUid = "uid-2"))
        syncDao.enqueue(TestFixtures.syncQueueEntity(entityUid = "uid-3"))

        val items = syncDao.getNext50()
        assertThat(items.map { it.entityUid }).containsExactly("uid-1", "uid-2", "uid-3").inOrder()
    }

    @Test
    fun `syncQueueDao delete elimina el item`() = runTest {
        syncDao.enqueue(TestFixtures.syncQueueEntity())
        val id = syncDao.getNext50().first().id
        syncDao.delete(id)
        assertThat(syncDao.count()).isEqualTo(0)
    }

    @Test
    fun `syncQueueDao markFailed incrementa attempts y guarda error`() = runTest {
        syncDao.enqueue(TestFixtures.syncQueueEntity(attempts = 0))
        val id = syncDao.getNext50().first().id

        syncDao.markFailed(id, "Network timeout")

        val updated = syncDao.getNext50().first()
        assertThat(updated.attempts).isEqualTo(1)
        assertThat(updated.lastError).isEqualTo("Network timeout")
    }

    @Test
    fun `syncQueueDao getNext50 limita a 50 items`() = runTest {
        repeat(60) { i ->
            syncDao.enqueue(TestFixtures.syncQueueEntity(entityUid = "uid-$i"))
        }
        assertThat(syncDao.getNext50()).hasSize(50)
    }
}
