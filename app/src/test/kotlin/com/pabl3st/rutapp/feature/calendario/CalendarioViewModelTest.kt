package com.pabl3st.rutapp.feature.calendario

import com.google.common.truth.Truth.assertThat
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.UserPrefs
import com.pabl3st.rutapp.data.repository.UserPrefsRepository
import com.pabl3st.rutapp.util.TestFixtures
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
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class CalendarioViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var routeRepo:     RouteRepository
    private lateinit var userPrefsRepo: UserPrefsRepository

    private val route = TestFixtures.routeEntity()

    private fun buildVm() = CalendarioViewModel(
        routeRepo     = routeRepo,
        userPrefsRepo = userPrefsRepo,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        routeRepo     = mockk(relaxed = true)
        userPrefsRepo = mockk(relaxed = true)

        every { routeRepo.observeAll() } returns flowOf(listOf(route))
        coEvery { userPrefsRepo.prefs } returns flowOf(UserPrefs())
    }

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `estado inicial currentMonth es el mes actual`() = runTest {
        val vm = buildVm()
        assertThat(vm.ui.value.currentMonth).isEqualTo(YearMonth.now())
    }

    @Test
    fun `prevMonth retrocede un mes`() = runTest {
        val vm = buildVm()
        val initial = vm.ui.value.currentMonth
        vm.prevMonth()
        assertThat(vm.ui.value.currentMonth).isEqualTo(initial.minusMonths(1))
    }

    @Test
    fun `nextMonth avanza un mes`() = runTest {
        val vm = buildVm()
        val initial = vm.ui.value.currentMonth
        vm.nextMonth()
        assertThat(vm.ui.value.currentMonth).isEqualTo(initial.plusMonths(1))
    }

    @Test
    fun `prevMonth y nextMonth son inversos`() = runTest {
        val vm = buildVm()
        val initial = vm.ui.value.currentMonth
        vm.prevMonth()
        vm.nextMonth()
        assertThat(vm.ui.value.currentMonth).isEqualTo(initial)
    }

    @Test
    fun `selectDay actualiza selectedDay`() = runTest {
        val vm = buildVm()
        val day = LocalDate.of(2026, 5, 15)
        vm.selectDay(day)
        assertThat(vm.ui.value.selectedDay).isEqualTo(day)
    }

    @Test
    fun `selectDay distinto dia actualiza correctamente`() = runTest {
        val vm = buildVm()
        val day1 = LocalDate.of(2026, 5, 10)
        val day2 = LocalDate.of(2026, 5, 20)
        vm.selectDay(day1)
        vm.selectDay(day2)
        assertThat(vm.ui.value.selectedDay).isEqualTo(day2)
    }

    @Test
    fun `onDayLongPress activa showDayMenu y menuDay`() = runTest {
        val vm = buildVm()
        val day = LocalDate.of(2026, 5, 12)
        vm.onDayLongPress(day)
        assertThat(vm.ui.value.showDayMenu).isTrue()
        assertThat(vm.ui.value.menuDay).isEqualTo(day)
    }

    @Test
    fun `dismissDayMenu cierra el menu`() = runTest {
        val vm = buildVm()
        vm.onDayLongPress(LocalDate.now())
        vm.dismissDayMenu()
        assertThat(vm.ui.value.showDayMenu).isFalse()
    }

    @Test
    fun `onShowRouteSelector activa showRouteSelector`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        vm.onShowRouteSelector()
        assertThat(vm.ui.value.showRouteSelector).isTrue()
    }

    @Test
    fun `onDismissRouteSelector desactiva showRouteSelector`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        vm.onShowRouteSelector()
        vm.onDismissRouteSelector()
        assertThat(vm.ui.value.showRouteSelector).isFalse()
    }

    @Test
    fun `onAssignRouteToDay llama a routeRepo con fecha correcta`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        val day = LocalDate.of(2026, 5, 20)
        vm.selectDay(day)
        vm.onAssignRouteToDay(route)
        advanceUntilIdle()
        coVerify { routeRepo.assignDate(route.uid, "2026-05-20") }
    }

    @Test
    fun `onMarkVacation llama a userPrefsRepo toggleVacationDay`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        val day = LocalDate.of(2026, 5, 1)
        vm.onDayLongPress(day)
        vm.onMarkVacation()
        advanceUntilIdle()
        coVerify { userPrefsRepo.toggleVacationDay("2026-05-01") }
    }

    @Test
    fun `clearSnackbar elimina snackbar`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        vm.clearSnackbar()
        assertThat(vm.ui.value.snackbar).isNull()
    }

    @Test
    fun `rutas del DAO se reflejan en allRoutes`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        assertThat(vm.ui.value.allRoutes).contains(route)
    }
}
