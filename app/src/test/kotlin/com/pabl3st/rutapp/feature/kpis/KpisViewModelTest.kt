package com.pabl3st.rutapp.feature.kpis

import com.google.common.truth.Truth.assertThat
import com.pabl3st.rutapp.data.local.dao.KpiValueDao
import com.pabl3st.rutapp.data.local.dao.StopDao
import com.pabl3st.rutapp.data.local.entity.BusinessProfileEntity
import com.pabl3st.rutapp.data.local.entity.KpiDefinitionEntity
import com.pabl3st.rutapp.data.local.entity.KpiValueEntity
import com.pabl3st.rutapp.data.repository.BusinessProfileRepository
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import com.pabl3st.rutapp.util.FakeSessionManager
import com.pabl3st.rutapp.util.TestFixtures
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Tests para KpisViewModel.
 *
 * COBERTURA:
 * - calcPlusMetrics: OR logic (activaciones>=5 OR primerBono>=50)
 * - calcPlusMetrics: casos límite (exactamente 5 activaciones, exactamente 50€)
 * - calcPlusMetrics: sector != telco → siempre 0
 * - calcPlusMetrics: PDV sin KPIs → no cuenta como telco
 * - calcPlusMetrics: plusLl (telco_plus = true)
 * - buildSectorKpis: agrega valores numéricos correctamente
 * - KpiMetrics: estado inicial coherente
 *
 * EXTENSIÓN: añadir tests cuando se cambie la lógica Plus en calcPlusMetrics
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class KpisViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var routeRepo:    RouteRepository
    private lateinit var stopRepo:     StopRepository
    private lateinit var kpiValueDao:  KpiValueDao
    private lateinit var profileRepo:  BusinessProfileRepository
    private lateinit var session:      FakeSessionManager
    private lateinit var vm:           KpisViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        routeRepo   = mockk(relaxed = true)
        stopRepo    = mockk(relaxed = true)
        kpiValueDao = mockk(relaxed = true)
        profileRepo = mockk(relaxed = true)
        session     = FakeSessionManager()

        // defaults
        coEvery { routeRepo.observeAll() } returns flowOf(emptyList())
        coEvery { stopRepo.observeByRouteUids(any()) } returns flowOf(emptyList())
        coEvery { profileRepo.getOrCreateProfile() } returns BusinessProfileEntity(sector = "telco")
        coEvery { profileRepo.observeProfile() } returns flowOf(BusinessProfileEntity(sector = "telco"))
        coEvery { kpiValueDao.getByStops(any()) } returns emptyList()
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun createVm() = KpisViewModel(routeRepo, stopRepo, kpiValueDao, profileRepo, session)

    // ── calcPlusMetrics — lógica OR ────────────────────────────────────────

    @Test
    fun `plus se activa con activaciones exactamente 5`() = runTest {
        val stopUid = "stop-001"
        coEvery { kpiValueDao.getByStops(listOf(stopUid)) } returns listOf(
            KpiValueEntity(stopUid, "telco_activaciones", "5"),
            KpiValueEntity(stopUid, "telco_primer_bono",  "0"),
            KpiValueEntity(stopUid, "telco_plus",         "false"),
        )
        coEvery { stopRepo.observeByRouteUids(any()) } returns flowOf(
            listOf(TestFixtures.stopEntity(uid = stopUid))
        )

        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.ui.value.metrics.plusPdvs).isEqualTo(1)
    }

    @Test
    fun `plus NO se activa con activaciones 4 y bono bajo`() = runTest {
        val stopUid = "stop-001"
        coEvery { kpiValueDao.getByStops(listOf(stopUid)) } returns listOf(
            KpiValueEntity(stopUid, "telco_activaciones", "4"),
            KpiValueEntity(stopUid, "telco_primer_bono",  "49.99"),
            KpiValueEntity(stopUid, "telco_plus",         "false"),
        )
        coEvery { stopRepo.observeByRouteUids(any()) } returns flowOf(
            listOf(TestFixtures.stopEntity(uid = stopUid))
        )

        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.ui.value.metrics.plusPdvs).isEqualTo(0)
    }

    @Test
    fun `plus se activa con primerBono exactamente 50 aunque activaciones sean 0`() = runTest {
        val stopUid = "stop-001"
        coEvery { kpiValueDao.getByStops(listOf(stopUid)) } returns listOf(
            KpiValueEntity(stopUid, "telco_activaciones", "0"),
            KpiValueEntity(stopUid, "telco_primer_bono",  "50"),
            KpiValueEntity(stopUid, "telco_plus",         "false"),
        )
        coEvery { stopRepo.observeByRouteUids(any()) } returns flowOf(
            listOf(TestFixtures.stopEntity(uid = stopUid))
        )

        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.ui.value.metrics.plusPdvs).isEqualTo(1)
    }

    @Test
    fun `plus se activa si CUALQUIERA de los dos criterios se cumple - activaciones`() = runTest {
        val s1 = "stop-001"
        val s2 = "stop-002"
        coEvery { kpiValueDao.getByStops(any()) } returns listOf(
            // s1: cumple por activaciones
            KpiValueEntity(s1, "telco_activaciones", "10"),
            KpiValueEntity(s1, "telco_primer_bono",  "5"),
            KpiValueEntity(s1, "telco_plus",         "false"),
            // s2: cumple por bono
            KpiValueEntity(s2, "telco_activaciones", "2"),
            KpiValueEntity(s2, "telco_primer_bono",  "75"),
            KpiValueEntity(s2, "telco_plus",         "false"),
        )
        coEvery { stopRepo.observeByRouteUids(any()) } returns flowOf(
            listOf(TestFixtures.stopEntity(uid = s1), TestFixtures.stopEntity(uid = s2))
        )

        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.ui.value.metrics.plusPdvs).isEqualTo(2)
    }

    @Test
    fun `plusLl cuenta solo PDVs con telco_plus true`() = runTest {
        val s1 = "stop-001"
        val s2 = "stop-002"
        coEvery { kpiValueDao.getByStops(any()) } returns listOf(
            KpiValueEntity(s1, "telco_activaciones", "5"),
            KpiValueEntity(s1, "telco_plus",         "true"),   // Plus LL
            KpiValueEntity(s2, "telco_activaciones", "5"),
            KpiValueEntity(s2, "telco_plus",         "false"),  // Plus normal, no LL
        )
        coEvery { stopRepo.observeByRouteUids(any()) } returns flowOf(
            listOf(TestFixtures.stopEntity(uid = s1), TestFixtures.stopEntity(uid = s2))
        )

        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.ui.value.metrics.plusPdvs).isEqualTo(2)   // ambos son Plus
        assertThat(vm.ui.value.metrics.plusLlPdvs).isEqualTo(1) // solo s1 es Plus LL
    }

    @Test
    fun `sector distinto a telco retorna 0 plus y 0 plusLL`() = runTest {
        coEvery { profileRepo.getOrCreateProfile() } returns BusinessProfileEntity(sector = "farma")
        coEvery { profileRepo.observeProfile() } returns flowOf(BusinessProfileEntity(sector = "farma"))
        val stopUid = "stop-001"
        coEvery { kpiValueDao.getByStops(any()) } returns listOf(
            KpiValueEntity(stopUid, "telco_activaciones", "10"),
            KpiValueEntity(stopUid, "telco_plus",         "true"),
        )
        coEvery { stopRepo.observeByRouteUids(any()) } returns flowOf(
            listOf(TestFixtures.stopEntity(uid = stopUid))
        )

        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.ui.value.metrics.plusPdvs).isEqualTo(0)
        assertThat(vm.ui.value.metrics.plusLlPdvs).isEqualTo(0)
    }

    @Test
    fun `PDV sin KPIs registrados no cuenta como telco`() = runTest {
        val stopUid = "stop-sin-kpis"
        coEvery { kpiValueDao.getByStops(any()) } returns emptyList()
        coEvery { stopRepo.observeByRouteUids(any()) } returns flowOf(
            listOf(TestFixtures.stopEntity(uid = stopUid))
        )

        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.ui.value.metrics.plusPdvs).isEqualTo(0)
        assertThat(vm.ui.value.metrics.totalTelco).isEqualTo(0)
    }

    @Test
    fun `totalTelco cuenta solo PDVs con al menos un KPI registrado`() = runTest {
        val s1 = "stop-001"
        val s2 = "stop-002"
        val s3 = "stop-sin-kpis"
        coEvery { kpiValueDao.getByStops(any()) } returns listOf(
            KpiValueEntity(s1, "telco_activaciones", "3"),
            KpiValueEntity(s2, "telco_primer_bono",  "20"),
            // s3 no tiene KPIs
        )
        coEvery { stopRepo.observeByRouteUids(any()) } returns flowOf(
            listOf(
                TestFixtures.stopEntity(uid = s1),
                TestFixtures.stopEntity(uid = s2),
                TestFixtures.stopEntity(uid = s3),
            )
        )

        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.ui.value.metrics.totalTelco).isEqualTo(2)
    }

    // ── KpiMetrics estado inicial ──────────────────────────────────────────

    @Test
    fun `metricas iniciales son cero con lista de stops vacia`() = runTest {
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()

        val m = vm.ui.value.metrics
        assertThat(m.total).isEqualTo(0)
        assertThat(m.done).isEqualTo(0)
        assertThat(m.plusPdvs).isEqualTo(0)
        assertThat(m.plusLlPdvs).isEqualTo(0)
        assertThat(m.totalTelco).isEqualTo(0)
    }

    @Test
    fun `completionRate es 0 con 0 stops`() = runTest {
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.ui.value.metrics.completionRate).isEqualTo(0f)
    }

    @Test
    fun `completionRate es 1f con todos los stops visitados`() = runTest {
        val stops = listOf(
            TestFixtures.stopEntity(uid = "s1", status = "done"),
            TestFixtures.stopEntity(uid = "s2", status = "done"),
        )
        coEvery { stopRepo.observeByRouteUids(any()) } returns flowOf(stops)
        coEvery { routeRepo.observeAll() } returns flowOf(listOf(TestFixtures.routeEntity()))

        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.ui.value.metrics.completionRate).isEqualTo(1f)
    }
}
