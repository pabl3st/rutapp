package com.pabl3st.rutapp.data.repository

import com.google.common.truth.Truth.assertThat
import com.pabl3st.rutapp.data.local.dao.RouteDao
import com.pabl3st.rutapp.data.local.dao.StopDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.network.BatchSyncResponse
import com.pabl3st.rutapp.data.network.DeltaSyncResponse
import com.pabl3st.rutapp.data.network.RutasApiService
import com.pabl3st.rutapp.util.FakeSessionManager
import com.pabl3st.rutapp.util.TestFixtures
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import retrofit2.Response

/**
 * Tests para SyncRepository
 *
 * COBERTURA:
 * - runSync() sin auth → SyncResult.NoAuth
 * - runSync() cola vacía + delta OK → SyncResult.Success
 * - runSync() upload falla → SyncResult.UploadError
 * - runSync() download falla → SyncResult.DownloadError
 * - uploadPending(): envía operaciones, elimina synced de queue, marca errores
 * - downloadDelta(): guarda routes Y stops, actualiza timestamp
 * - pendingCount(): delega al DAO
 *
 * EXTENSIÓN: añadir tests cuando se añadan nuevas entidades al sync
 */
@RunWith(JUnit4::class)
class SyncRepositoryTest {

    private lateinit var syncQueueDao: SyncQueueDao
    private lateinit var routeDao:     RouteDao
    private lateinit var stopDao:      StopDao
    private lateinit var api:          RutasApiService
    private lateinit var session:      FakeSessionManager
    private lateinit var moshi:        Moshi
    private lateinit var repo:         SyncRepository

    @Before
    fun setUp() {
        syncQueueDao = mockk(relaxed = true)
        routeDao     = mockk(relaxed = true)
        stopDao      = mockk(relaxed = true)
        api          = mockk(relaxed = true)
        session      = FakeSessionManager()
        moshi        = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        repo         = SyncRepository(syncQueueDao, routeDao, stopDao, api, session, moshi)
    }

    // ── runSync ───────────────────────────────────────────────

    @Test
    fun `runSync sin token retorna NoAuth`() = runTest {
        session.setNoAuth()
        val result = repo.runSync()
        assertThat(result).isInstanceOf(SyncResult.NoAuth::class.java)
        coVerify(exactly = 0) { api.batchSync(any(), any(), any()) }
        coVerify(exactly = 0) { api.deltaSync(any(), any(), any()) }
    }

    @Test
    fun `runSync con cola vacia y delta OK retorna Success`() = runTest {
        coEvery { syncQueueDao.getNext50() } returns emptyList()
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse())

        val result = repo.runSync()
        assertThat(result).isInstanceOf(SyncResult.Success::class.java)
    }

    @Test
    fun `runSync cuando batchSync falla retorna UploadError`() = runTest {
        coEvery { syncQueueDao.getNext50() } returns
            listOf(TestFixtures.syncQueueEntity())
        coEvery { api.batchSync(any(), any(), any()) } throws Exception("Network error")

        val result = repo.runSync()
        assertThat(result).isInstanceOf(SyncResult.UploadError::class.java)
    }

    @Test
    fun `runSync cuando deltaSync falla retorna DownloadError`() = runTest {
        coEvery { syncQueueDao.getNext50() } returns emptyList()
        coEvery { api.deltaSync(any(), any(), any()) } throws Exception("Network error")

        val result = repo.runSync()
        assertThat(result).isInstanceOf(SyncResult.DownloadError::class.java)
    }

    // ── uploadPending ─────────────────────────────────────────

    @Test
    fun `uploadPending cola vacia no llama a batchSync`() = runTest {
        coEvery { syncQueueDao.getNext50() } returns emptyList()
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse())

        repo.runSync()

        coVerify(exactly = 0) { api.batchSync(any(), any(), any()) }
    }

    @Test
    fun `uploadPending elimina items synced de la queue`() = runTest {
        val item = TestFixtures.syncQueueEntity(id = 99, entityUid = "route-uid-001")
        coEvery { syncQueueDao.getNext50() } returns listOf(item)
        coEvery { api.batchSync(any(), any(), any()) } returns
            Response.success(TestFixtures.batchSyncResponse(syncedUids = listOf("route-uid-001")))
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse())

        repo.runSync()

        coVerify { syncQueueDao.delete(99) }
    }

    @Test
    fun `uploadPending marca error en items fallidos`() = runTest {
        val item = TestFixtures.syncQueueEntity(id = 55, entityUid = "route-uid-fail")
        coEvery { syncQueueDao.getNext50() } returns listOf(item)
        coEvery { api.batchSync(any(), any(), any()) } returns
            Response.success(TestFixtures.batchSyncResponse(
                syncedUids = emptyList(),
                errorUids  = listOf("route-uid-fail"),
            ))
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse())

        repo.runSync()

        coVerify { syncQueueDao.markFailed(55, any()) }
        coVerify(exactly = 0) { syncQueueDao.delete(55) }
    }

    @Test
    fun `uploadPending actualiza syncStatus en Room para routes`() = runTest {
        val item = TestFixtures.syncQueueEntity(entity = "route", entityUid = "route-uid-001")
        coEvery { syncQueueDao.getNext50() } returns listOf(item)
        coEvery { api.batchSync(any(), any(), any()) } returns
            Response.success(TestFixtures.batchSyncResponse(
                syncedUids = listOf("route-uid-001"), entity = "route"
            ))
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse())

        repo.runSync()

        coVerify { routeDao.updateSyncStatus("route-uid-001", "synced", any()) }
    }

    @Test
    fun `uploadPending actualiza syncStatus en Room para stops`() = runTest {
        val item = TestFixtures.syncQueueEntity(entity = "stop", entityUid = "stop-uid-001")
        coEvery { syncQueueDao.getNext50() } returns listOf(item)
        coEvery { api.batchSync(any(), any(), any()) } returns
            Response.success(TestFixtures.batchSyncResponse(
                syncedUids = listOf("stop-uid-001"), entity = "stop"
            ))
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse())

        repo.runSync()

        coVerify { stopDao.updateSyncStatus("stop-uid-001", "synced", any()) }
    }

    // ── downloadDelta ─────────────────────────────────────────

    @Test
    fun `downloadDelta guarda routes en Room`() = runTest {
        coEvery { syncQueueDao.getNext50() } returns emptyList()
        val routes = listOf(TestFixtures.routeDto(), TestFixtures.routeDto(uid = "uid-2", id = 2))
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse(routes = routes, stops = emptyList()))

        repo.runSync()

        coVerify { routeDao.upsertAll(match { it.size == 2 }) }
    }

    @Test
    fun `downloadDelta guarda stops en Room — bug critico prevenido`() = runTest {
        // Previene regresión: stops deben persistirse, no solo routes
        coEvery { syncQueueDao.getNext50() } returns emptyList()
        val stops = listOf(
            TestFixtures.stopDto(uid = "s1"),
            TestFixtures.stopDto(uid = "s2"),
        )
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse(stops = stops))

        repo.runSync()

        coVerify { stopDao.upsertAll(match { it.size == 2 }) }
    }

    @Test
    fun `downloadDelta no llama upsertAll si stops es lista vacia`() = runTest {
        coEvery { syncQueueDao.getNext50() } returns emptyList()
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse(stops = emptyList()))

        repo.runSync()

        coVerify(exactly = 0) { stopDao.upsertAll(any()) }
    }

    @Test
    fun `downloadDelta actualiza lastSyncTimestamp`() = runTest {
        coEvery { syncQueueDao.getNext50() } returns emptyList()
        coEvery { api.deltaSync(any(), any(), any()) } returns
            Response.success(TestFixtures.deltaSyncResponse())

        repo.runSync()

        assertThat(session.lastSyncTimestamp).isEqualTo(TestFixtures.SERVER_TIME)
    }

    // ── pendingCount ──────────────────────────────────────────

    @Test
    fun `pendingCount delega al DAO`() = runTest {
        coEvery { syncQueueDao.count() } returns 5
        assertThat(repo.pendingCount()).isEqualTo(5)
    }

    @Test
    fun `pendingCount retorna 0 cuando cola vacia`() = runTest {
        coEvery { syncQueueDao.count() } returns 0
        assertThat(repo.pendingCount()).isEqualTo(0)
    }
}
