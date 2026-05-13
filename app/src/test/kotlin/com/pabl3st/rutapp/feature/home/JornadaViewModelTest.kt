package com.pabl3st.rutapp.feature.home

import com.google.common.truth.Truth.assertThat
import com.pabl3st.rutapp.data.local.entity.DaySessionEntity
import com.pabl3st.rutapp.data.repository.JornadaRepository
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
import android.content.Context
import io.mockk.mockkStatic

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class JornadaViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var jornadaRepo: JornadaRepository
    private lateinit var appContext:  Context

    private val routeUid = "route-uid-001"
    private val dateStr  = "2026-05-14"
    private val idleSession = DaySessionEntity(
        routeUid  = routeUid,
        dateStr   = dateStr,
        state     = "idle",
        elapsedMs = 0L,
    )
    private val runningSession = DaySessionEntity(
        routeUid  = routeUid,
        dateStr   = dateStr,
        state     = "running",
        startedAt = System.currentTimeMillis() - 60_000L,
        elapsedMs = 0L,
    )

    private fun buildVm(): JornadaViewModel {
        return JornadaViewModel(
            jornadaRepo = jornadaRepo,
            appContext  = appContext,
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        jornadaRepo = mockk(relaxed = true)
        appContext   = mockk(relaxed = true)

        every  { jornadaRepo.observe(routeUid, any()) } returns flowOf(idleSession)
        coEvery { jornadaRepo.todayStr() } returns dateStr
        coEvery { jornadaRepo.elapsedMs(any()) } returns 0L
    }

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `estado inicial session null`() = runTest {
        val vm = buildVm()
        assertThat(vm.ui.value.session).isNull()
    }

    @Test
    fun `init con routeUid carga la sesion`() = runTest {
        val vm = buildVm()
        vm.init(routeUid)
        advanceUntilIdle()
        assertThat(vm.ui.value.session).isEqualTo(idleSession)
    }

    @Test
    fun `start llama al repositorio`() = runTest {
        val vm = buildVm()
        vm.init(routeUid)
        advanceUntilIdle()
        vm.start()
        advanceUntilIdle()
        coVerify { jornadaRepo.start(routeUid, any()) }
    }

    @Test
    fun `pause llama al repositorio`() = runTest {
        every { jornadaRepo.observe(routeUid, any()) } returns flowOf(runningSession)
        val vm = buildVm()
        vm.init(routeUid)
        advanceUntilIdle()
        vm.pause()
        advanceUntilIdle()
        coVerify { jornadaRepo.pause(routeUid, any()) }
    }

    @Test
    fun `finish llama al repositorio`() = runTest {
        val vm = buildVm()
        vm.init(routeUid)
        advanceUntilIdle()
        vm.finish()
        advanceUntilIdle()
        coVerify { jornadaRepo.finish(routeUid, any()) }
    }

    @Test
    fun `formatElapsed segundos menos de un minuto`() = runTest {
        val vm = buildVm()
        assertThat(vm.formatElapsed(45_000L)).isEqualTo("00:45")
    }

    @Test
    fun `formatElapsed un minuto exacto`() = runTest {
        val vm = buildVm()
        assertThat(vm.formatElapsed(60_000L)).isEqualTo("01:00")
    }

    @Test
    fun `formatElapsed horas correctamente`() = runTest {
        val vm = buildVm()
        // 1h 23m 45s = 5025 segundos = 5025000 ms
        assertThat(vm.formatElapsed(5_025_000L)).isEqualTo("01:23:45")
    }

    @Test
    fun `formatElapsed cero`() = runTest {
        val vm = buildVm()
        assertThat(vm.formatElapsed(0L)).isEqualTo("00:00")
    }

    @Test
    fun `formatElapsed valores negativos devuelven 00 00`() = runTest {
        val vm = buildVm()
        assertThat(vm.formatElapsed(-1000L)).isEqualTo("00:00")
    }
}
