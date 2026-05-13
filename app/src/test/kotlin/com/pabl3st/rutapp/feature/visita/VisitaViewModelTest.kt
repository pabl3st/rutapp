package com.pabl3st.rutapp.feature.visita

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.pabl3st.rutapp.data.local.dao.KpiValueDao
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao
import com.pabl3st.rutapp.data.local.entity.BusinessProfileEntity
import com.pabl3st.rutapp.data.local.entity.KpiValueEntity
import com.pabl3st.rutapp.data.repository.BusinessProfileRepository
import com.pabl3st.rutapp.data.repository.PhotoRepository
import com.pabl3st.rutapp.data.repository.UserPrefs
import com.pabl3st.rutapp.data.repository.UserPrefsRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import com.pabl3st.rutapp.util.FakeSessionManager
import com.pabl3st.rutapp.util.TestFixtures
import androidx.lifecycle.SavedStateHandle
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
class VisitaViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var stopRepo:    StopRepository
    private lateinit var profileRepo: BusinessProfileRepository
    private lateinit var photoRepo:   PhotoRepository
    private lateinit var prefsRepo:   UserPrefsRepository
    private lateinit var kpiValueDao: KpiValueDao
    private lateinit var syncQueueDao: SyncQueueDao
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val stopUid = "stop-uid-001"
    private val stop    = TestFixtures.stopEntity(uid = stopUid)

    private fun buildVm(): VisitaViewModel {
        val ssh = SavedStateHandle(mapOf("stopUid" to stopUid))
        return VisitaViewModel(
            savedStateHandle = ssh,
            stopRepo         = stopRepo,
            profileRepo      = profileRepo,
            photoRepo        = photoRepo,
            kpiValueDao      = kpiValueDao,
            syncQueueDao     = syncQueueDao,
            prefsRepo        = prefsRepo,
            moshi            = moshi,
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        stopRepo     = mockk(relaxed = true)
        profileRepo  = mockk(relaxed = true)
        photoRepo    = mockk(relaxed = true)
        prefsRepo    = mockk(relaxed = true)
        kpiValueDao  = mockk(relaxed = true)
        syncQueueDao = mockk(relaxed = true)

        coEvery { stopRepo.getByUid(stopUid) } returns stop
        every   { photoRepo.observeByStop(stopUid) } returns flowOf(emptyList())
        coEvery { prefsRepo.prefs } returns flowOf(UserPrefs())
        coEvery { profileRepo.getOrCreateProfile() } returns
            BusinessProfileEntity(id = 1, accountId = TestFixtures.ACCOUNT_ID, sector = "telco")
        coEvery { profileRepo.getVisibleKpisForSector(any()) } returns emptyList()
        coEvery { kpiValueDao.getByStop(stopUid) } returns emptyList()
    }

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `estado inicial isLoading true`() = runTest {
        val vm = buildVm()
        vm.ui.test {
            assertThat(awaitItem().isLoading).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tras cargar stop isLoading false y stop correcto`() = runTest {
        val vm = buildVm()
        advanceUntilIdle()
        assertThat(vm.ui.value.isLoading).isFalse()
        assertThat(vm.ui.value.stop).isEqualTo(stop)
    }

    @Test
    fun `onResultChange actualiza selectedResult`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        vm.onResultChange("no_contactado")
        assertThat(vm.ui.value.selectedResult).isEqualTo("no_contactado")
    }

    @Test
    fun `onNotesChange actualiza notes`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        vm.onNotesChange("Llamar mañana")
        assertThat(vm.ui.value.notes).isEqualTo("Llamar mañana")
    }

    @Test
    fun `onNextActionChange actualiza nextAction`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        vm.onNextActionChange("Revisar stock")
        assertThat(vm.ui.value.nextAction).isEqualTo("Revisar stock")
    }

    @Test
    fun `onStoreOpenChange actualiza storeOpen`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        vm.onStoreOpenChange(false)
        assertThat(vm.ui.value.storeOpen).isFalse()
    }

    @Test
    fun `onPdvInactiveToggle invierte pdvInactive`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        assertThat(vm.ui.value.pdvInactive).isFalse()
        vm.onPdvInactiveToggle()
        assertThat(vm.ui.value.pdvInactive).isTrue()
        vm.onPdvInactiveToggle()
        assertThat(vm.ui.value.pdvInactive).isFalse()
    }

    @Test
    fun `onKpiValueChange guarda valor por id`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        vm.onKpiValueChange("kpi_acts", "12")
        assertThat(vm.ui.value.kpiValues["kpi_acts"]).isEqualTo("12")
    }

    @Test
    fun `onKpiValueChange multiples valores independientes`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        vm.onKpiValueChange("kpi_acts", "5")
        vm.onKpiValueChange("kpi_recargas", "99.5")
        assertThat(vm.ui.value.kpiValues["kpi_acts"]).isEqualTo("5")
        assertThat(vm.ui.value.kpiValues["kpi_recargas"]).isEqualTo("99.5")
    }

    @Test
    fun `saveVisit llama a stopRepo con resultado correcto`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        vm.onResultChange("contactado")
        vm.onNotesChange("Todo ok")
        vm.saveVisit()
        advanceUntilIdle()
        coVerify { stopRepo.saveVisitResult(stopUid, "contactado", "Todo ok", any()) }
    }

    @Test
    fun `saveVisit marca saved true tras exito`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        vm.saveVisit()
        advanceUntilIdle()
        assertThat(vm.ui.value.saved).isTrue()
    }

    @Test
    fun `clearError limpia el error`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        vm.clearError()
        assertThat(vm.ui.value.error).isNull()
    }

    @Test
    fun `onShowCamera y onHideCamera controlan showCamera`() = runTest {
        val vm = buildVm(); advanceUntilIdle()
        vm.onShowCamera()
        assertThat(vm.ui.value.showCamera).isTrue()
        vm.onHideCamera()
        assertThat(vm.ui.value.showCamera).isFalse()
    }
}
