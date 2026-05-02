package com.pabl3st.rutapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.pabl3st.rutapp.util.TestFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests de integración para Room — DAOs contra BD in-memory real.
 * EXTENSIÓN: añadir test aquí cuando se añadan nuevas entidades o DAOs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomDatabaseTest {

    private lateinit var db: RutasDatabase

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, RutasDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() = db.close()

    // ── RouteDao ──────────────────────────────────────────────

    @Test
    fun `RouteDao upsert y observeByDate devuelve ruta del dia`() = runTest {
        db.routeDao().upsert(TestFixtures.routeEntity(dateAssigned = "2026-05-01"))

        val result = db.routeDao().observeByDate(TestFixtures.USER_ID, "2026-05-01").first()

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Ruta Centro Valencia")
    }

    @Test
    fun `RouteDao observeByDate no devuelve rutas de otro dia`() = runTest {
        db.routeDao().upsert(TestFixtures.routeEntity(dateAssigned = "2026-05-02"))

        val result = db.routeDao().observeByDate(TestFixtures.USER_ID, "2026-05-01").first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `RouteDao observeByUser devuelve todas las rutas del usuario`() = runTest {
        db.routeDao().upsertAll(listOf(
            TestFixtures.routeEntity(uid = "r1", dateAssigned = "2026-05-01"),
            TestFixtures.routeEntity(uid = "r2", dateAssigned = "2026-05-02"),
            TestFixtures.routeEntity(uid = "r3", dateAssigned = "2026-05-03"),
        ))

        val result = db.routeDao().observeByUser(TestFixtures.USER_ID).first()

        assertThat(result).hasSize(3)
    }

    @Test
    fun `RouteDao observeByUser no devuelve rutas de otro usuario`() = runTest {
        db.routeDao().upsert(TestFixtures.routeEntity(userId = 999))

        val result = db.routeDao().observeByUser(TestFixtures.USER_ID).first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `RouteDao getByUid retorna la ruta correcta`() = runTest {
        db.routeDao().upsert(TestFixtures.routeEntity(uid = "target-uid"))

        val found = db.routeDao().getByUid("target-uid")

        assertThat(found).isNotNull()
        assertThat(found!!.uid).isEqualTo("target-uid")
    }

    @Test
    fun `RouteDao getByUid retorna null si no existe`() = runTest {
        val found = db.routeDao().getByUid("nonexistent")
        assertThat(found).isNull()
    }

    @Test
    fun `RouteDao upsert actualiza ruta existente sin duplicar`() = runTest {
        db.routeDao().upsert(TestFixtures.routeEntity(uid = "r1", name = "Original"))
        db.routeDao().upsert(TestFixtures.routeEntity(uid = "r1", name = "Actualizada"))

        val result = db.routeDao().observeByUser(TestFixtures.USER_ID).first()

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Actualizada")
    }

    @Test
    fun `RouteDao updateSyncStatus cambia estado y syncedAt`() = runTest {
        db.routeDao().upsert(TestFixtures.routeEntity(uid = "r1", syncStatus = "pending"))

        db.routeDao().updateSyncStatus("r1", "synced", "2026-05-01T10:00:00Z")

        val found = db.routeDao().getByUid("r1")
        assertThat(found!!.syncStatus).isEqualTo("synced")
        assertThat(found.syncedAt).isEqualTo("2026-05-01T10:00:00Z")
    }

    @Test
    fun `RouteDao getPendingSync devuelve solo pending y error`() = runTest {
        db.routeDao().upsertAll(listOf(
            TestFixtures.routeEntity(uid = "r1", syncStatus = "pending"),
            TestFixtures.routeEntity(uid = "r2", syncStatus = "synced"),
            TestFixtures.routeEntity(uid = "r3", syncStatus = "error"),
        ))

        val pending = db.routeDao().getPendingSync()

        assertThat(pending.map { it.uid }).containsExactly("r1", "r3")
    }

    @Test
    fun `RouteDao deleteByUid elimina la ruta`() = runTest {
        db.routeDao().upsert(TestFixtures.routeEntity(uid = "r1"))
        db.routeDao().deleteByUid("r1")

        assertThat(db.routeDao().getByUid("r1")).isNull()
    }

    @Test
    fun `RouteDao observeByDate excluye rutas con deletedAt`() = runTest {
        db.routeDao().upsert(
            TestFixtures.routeEntity(uid = "r1", dateAssigned = "2026-05-01")
                .copy(deletedAt = "2026-05-01T12:00:00Z")
        )

        val result = db.routeDao().observeByDate(TestFixtures.USER_ID, "2026-05-01").first()

        assertThat(result).isEmpty()
    }

    // ── StopDao ───────────────────────────────────────────────

    @Test
    fun `StopDao upsert y observeByRoute devuelve stops ordenados por orderIndex`() = runTest {
        db.stopDao().upsertAll(listOf(
            TestFixtures.stopEntity(uid = "s3", orderIndex = 2),
            TestFixtures.stopEntity(uid = "s1", orderIndex = 0),
            TestFixtures.stopEntity(uid = "s2", orderIndex = 1),
        ))

        val result = db.stopDao().observeByRoute("route-uid-001").first()

        assertThat(result).hasSize(3)
        assertThat(result.map { it.uid }).containsExactly("s1", "s2", "s3").inOrder()
    }

    @Test
    fun `StopDao observeByRoute no mezcla stops de rutas distintas`() = runTest {
        db.stopDao().upsert(TestFixtures.stopEntity(uid = "sA", routeUid = "route-a"))
        db.stopDao().upsert(TestFixtures.stopEntity(uid = "sB", routeUid = "route-b"))

        val result = db.stopDao().observeByRoute("route-a").first()

        assertThat(result).hasSize(1)
        assertThat(result[0].uid).isEqualTo("sA")
    }

    @Test
    fun `StopDao getByUid retorna el stop correcto`() = runTest {
        db.stopDao().upsert(TestFixtures.stopEntity(uid = "target-stop"))

        val found = db.stopDao().getByUid("target-stop")

        assertThat(found).isNotNull()
        assertThat(found!!.uid).isEqualTo("target-stop")
    }

    @Test
    fun `StopDao updateStatus marca done y guarda visitedAt y syncStatus pending`() = runTest {
        db.stopDao().upsert(TestFixtures.stopEntity(uid = "s1", status = "pending"))

        db.stopDao().updateStatus("s1", "done", "2026-05-01T10:30:00Z", "2026-05-01T10:30:00Z")

        val found = db.stopDao().getByUid("s1")
        assertThat(found!!.status).isEqualTo("done")
        assertThat(found.visitedAt).isEqualTo("2026-05-01T10:30:00Z")
        assertThat(found.syncStatus).isEqualTo("pending")
    }

    @Test
    fun `StopDao getPendingSync devuelve solo pending y error`() = runTest {
        db.stopDao().upsertAll(listOf(
            TestFixtures.stopEntity(uid = "s1", syncStatus = "pending"),
            TestFixtures.stopEntity(uid = "s2", syncStatus = "synced"),
            TestFixtures.stopEntity(uid = "s3", syncStatus = "error"),
        ))

        val pending = db.stopDao().getPendingSync()

        assertThat(pending.map { it.uid }).containsExactly("s1", "s3")
    }

    @Test
    fun `StopDao observeByRoute excluye stops con deletedAt`() = runTest {
        db.stopDao().upsert(
            TestFixtures.stopEntity(uid = "s1").copy(deletedAt = "2026-05-01T12:00:00Z")
        )

        val result = db.stopDao().observeByRoute("route-uid-001").first()

        assertThat(result).isEmpty()
    }

    // ── SyncQueueDao ──────────────────────────────────────────

    @Test
    fun `SyncQueueDao enqueue y getNext50 retorna en orden FIFO`() = runTest {
        listOf("uid-1", "uid-2", "uid-3").forEach { uid ->
            db.syncQueueDao().enqueue(TestFixtures.syncQueueEntity(entityUid = uid))
        }

        val result = db.syncQueueDao().getNext50()

        assertThat(result.map { it.entityUid }).containsExactly("uid-1", "uid-2", "uid-3").inOrder()
    }

    @Test
    fun `SyncQueueDao count es correcto tras enqueue y delete`() = runTest {
        assertThat(db.syncQueueDao().count()).isEqualTo(0)

        db.syncQueueDao().enqueue(TestFixtures.syncQueueEntity(entityUid = "uid-1"))
        db.syncQueueDao().enqueue(TestFixtures.syncQueueEntity(entityUid = "uid-2"))
        assertThat(db.syncQueueDao().count()).isEqualTo(2)

        val items = db.syncQueueDao().getNext50()
        db.syncQueueDao().delete(items[0].id)
        assertThat(db.syncQueueDao().count()).isEqualTo(1)
    }

    @Test
    fun `SyncQueueDao markFailed incrementa attempts y guarda lastError`() = runTest {
        db.syncQueueDao().enqueue(TestFixtures.syncQueueEntity(entityUid = "uid-1", attempts = 0))

        val item = db.syncQueueDao().getNext50()[0]
        db.syncQueueDao().markFailed(item.id, "Network error")

        val updated = db.syncQueueDao().getNext50()[0]
        assertThat(updated.attempts).isEqualTo(1)
        assertThat(updated.lastError).isEqualTo("Network error")
    }

    @Test
    fun `SyncQueueDao getNext50 limita a 50 items`() = runTest {
        repeat(60) { i ->
            db.syncQueueDao().enqueue(TestFixtures.syncQueueEntity(entityUid = "uid-$i"))
        }

        val result = db.syncQueueDao().getNext50()

        assertThat(result).hasSize(50)
    }
}
