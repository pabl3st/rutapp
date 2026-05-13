package com.pabl3st.rutapp.feature.home

import com.google.common.truth.Truth.assertThat
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import com.pabl3st.rutapp.data.repository.SyncRepository
import com.pabl3st.rutapp.util.FakeSessionManager
import com.pabl3st.rutapp.util.TestFixtures
import androidx.work.WorkManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var routeRepo:   RouteRepository
    private lateinit var stopRepo:    StopRepository
    private lateinit var syncRepo:    SyncRepository
    private lateinit var workManager: WorkManager

    private val route = TestFixtures.routeEntity()
    private val stop  = TestFixtures.stopEntity(routeUid = route.uid)

    private fun buildVm() = HomeViewModel(
        routeRepo   = routeRepo,
        stopRepo    = stopRepo,
        syncRepo    = syncRepo,
        session     = FakeSessionManager(),
        workManager = workManager,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        routeRepo   = mockk(relaxed = true)
        stopRepo    = mockk(relaxed = true)
        syncRepo    = mockk(relaxed = true)
        workManager = mockk(relaxed = true)

        every { routeRepo.observeToday() } returns flowOf(listOf(route))
        every { stopRepo.observeByRouteUids(any()) } returns flowOf(listOf(stop))
    }

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `estado inicial isLoading true`() = runTest {
        val vm = buildVm()
        assertThat(vm.ui.value.isLoading).isTrue()
    }

    @Test
    fun `tras cargar rutas isLoading false`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        assertThat(vm.ui.value.isLoading).isFalse()
    }

    @Test
    fun `rutas del DAO se reflejan en routes`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        assertThat(vm.ui.value.routes.map { it.route }).contains(route)
    }

    @Test
    fun `totalStops se calcula correctamente`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        assertThat(vm.ui.value.totalStops).isEqualTo(1)
    }

    @Test
    fun `doneStops es 0 con stop pendiente`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        assertThat(vm.ui.value.doneStops).isEqualTo(0)
    }

    @Test
    fun `doneStops es 1 con stop done`() = runTest {
        val doneStop = TestFixtures.stopEntity(routeUid = route.uid, status = "done")
        every { stopRepo.observeByRouteUids(any()) } returns flowOf(listOf(doneStop))
        val vm = buildVm(); advanceUntilIdle()
        assertThat(vm.ui.value.doneStops).isEqualTo(1)
    }

    @Test
    fun `pendingStops es totalStops menos doneStops`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        with(vm.ui.value) {
            assertThat(pendingStops).isEqualTo(totalStops - doneStops)
        }
    }

    @Test
    fun `sin rutas hoy routes lista vacia`() = runTest {
        every { routeRepo.observeToday() } returns flowOf(emptyList())
        val vm = buildVm(); advanceUntilIdle()
        assertThat(vm.ui.value.routes).isEmpty()
        assertThat(vm.ui.value.totalStops).isEqualTo(0)
    }

    @Test
    fun `syncNow llama a routeRepo fetchDelta`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        vm.syncNow()
        advanceUntilIdle()
        coVerify { routeRepo.fetchDelta() }
    }

    @Test
    fun `syncNow no llama dos veces si ya esta sincronizando`() = runTest {
        coEvery { routeRepo.fetchDelta() } coAnswers {
            kotlinx.coroutines.delay(1000)
        }
        val vm = buildVm(); advanceUntilIdle()
        vm.syncNow()
        vm.syncNow() // segunda llamada ignorada
        advanceUntilIdle()
        coVerify(exactly = 1) { routeRepo.fetchDelta() }
    }

    @Test
    fun `clearError limpia el error`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        vm.clearError()
        assertThat(vm.ui.value.error).isNull()
    }

    @Test
    fun `progress es 0f con stop pendiente`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        assertThat(vm.ui.value.routes.first().progress).isEqualTo(0f)
    }

    @Test
    fun `progress es 1f con todos los stops done`() = runTest {
        val doneStop = TestFixtures.stopEntity(routeUid = route.uid, status = "done")
        every { stopRepo.observeByRouteUids(any()) } returns flowOf(listOf(doneStop))
        val vm = buildVm(); advanceUntilIdle()
        assertThat(vm.ui.value.routes.first().progress).isEqualTo(1f)
    }
}
