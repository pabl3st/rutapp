package com.pabl3st.rutapp.feature

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.pabl3st.rutapp.data.repository.AuthRepository
import com.pabl3st.rutapp.data.repository.AuthResult
import com.pabl3st.rutapp.data.repository.AuthSuccess
import com.pabl3st.rutapp.data.repository.RouteRepository
import com.pabl3st.rutapp.data.repository.StopRepository
import com.pabl3st.rutapp.feature.auth.AuthScreen
import com.pabl3st.rutapp.feature.auth.AuthViewModel
import com.pabl3st.rutapp.feature.rutas.CrearParadaViewModel
import com.pabl3st.rutapp.feature.rutas.RouteDetailViewModel
import com.pabl3st.rutapp.feature.rutas.RutasViewModel
import com.pabl3st.rutapp.util.FakeSessionManager
import com.pabl3st.rutapp.util.TestFixtures
import androidx.lifecycle.SavedStateHandle
import com.pabl3st.rutapp.core.map.MapProvider
import io.mockk.coEvery
import io.mockk.coVerify
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repo:    AuthRepository
    private lateinit var session: FakeSessionManager
    private lateinit var vm:      AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo    = mockk(relaxed = true)
        session = FakeSessionManager()
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun createVm() = AuthViewModel(repo, session)

    @Test
    fun `sin sesion navega a CHOOSE_TYPE`() = runTest {
        session.setNoAuth()
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.ui.value.screen).isEqualTo(AuthScreen.CHOOSE_TYPE)
    }

    @Test
    fun `con sesion valida marca isAuthenticated`() = runTest {
        session.setLoggedIn()
        coEvery { repo.verifySession() } returns AuthResult.Success(fakeAuthSuccess())
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.ui.value.isAuthenticated).isTrue()
    }

    @Test
    fun `con sesion invalida navega a LOGIN`() = runTest {
        session.setLoggedIn()
        coEvery { repo.verifySession() } returns AuthResult.Error("Expirado", 401)
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.ui.value.screen).isEqualTo(AuthScreen.LOGIN)
    }

    @Test
    fun `login vacio muestra error de validacion`() = runTest {
        session.setNoAuth()
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.login()
        assertThat(vm.ui.value.error).isNotNull()
        coVerify(exactly = 0) { repo.login(any(), any()) }
    }

    @Test
    fun `login con solo username muestra error de password`() = runTest {
        session.setNoAuth()
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onUsernameChange("god")
        vm.login()
        assertThat(vm.ui.value.error).contains("contraseña")
    }

    @Test
    fun `login exitoso marca isAuthenticated`() = runTest {
        session.setNoAuth()
        coEvery { repo.login(any(), any()) } returns AuthResult.Success(fakeAuthSuccess())
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onUsernameChange("god")
        vm.onPasswordChange("God2026!")
        vm.login()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.ui.value.isAuthenticated).isTrue()
        assertThat(vm.ui.value.isLoading).isFalse()
    }

    @Test
    fun `login fallido muestra error`() = runTest {
        session.setNoAuth()
        coEvery { repo.login(any(), any()) } returns AuthResult.Error("Credenciales incorrectas")
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onUsernameChange("god")
        vm.onPasswordChange("wrong")
        vm.login()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.ui.value.error).isEqualTo("Credenciales incorrectas")
        assertThat(vm.ui.value.isAuthenticated).isFalse()
    }

    @Test
    fun `handleBack en CHOOSE_TYPE muestra ExitDialog`() = runTest {
        session.setNoAuth()
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        val handled = vm.handleBack()
        assertThat(handled).isTrue()
        assertThat(vm.ui.value.showExitDialog).isTrue()
    }

    @Test
    fun `handleBack en LOGIN sin datos vuelve a CHOOSE_TYPE`() = runTest {
        session.setNoAuth()
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onGoToLogin()
        val handled = vm.handleBack()
        assertThat(handled).isTrue()
        assertThat(vm.ui.value.screen).isEqualTo(AuthScreen.CHOOSE_TYPE)
        assertThat(vm.ui.value.showDiscardDialog).isFalse()
    }

    @Test
    fun `handleBack en LOGIN con datos muestra DiscardDialog`() = runTest {
        session.setNoAuth()
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onGoToLogin()
        vm.onUsernameChange("god")
        vm.handleBack()
        assertThat(vm.ui.value.showDiscardDialog).isTrue()
    }

    @Test
    fun `registerIndividual con nombre vacio muestra error`() = runTest {
        session.setNoAuth()
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onChooseIndividual()
        vm.onUsernameChange("god")
        vm.onEmailChange("god@test.com")
        vm.onPasswordChange("12345678")
        vm.registerIndividual()
        assertThat(vm.ui.value.error).contains("nombre")
    }

    @Test
    fun `registerIndividual con password corta muestra error`() = runTest {
        session.setNoAuth()
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onNameChange("God Admin")
        vm.onUsernameChange("god")
        vm.onEmailChange("god@test.com")
        vm.onPasswordChange("123")
        vm.registerIndividual()
        assertThat(vm.ui.value.error).contains("8")
    }

    @Test
    fun `logout llama al repo y resetea estado`() = runTest {
        session.setLoggedIn()
        coEvery { repo.verifySession() } returns AuthResult.Success(fakeAuthSuccess())
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.logout()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repo.logout() }
        assertThat(vm.ui.value.screen).isEqualTo(AuthScreen.CHOOSE_TYPE)
        assertThat(vm.ui.value.isAuthenticated).isFalse()
    }

    private fun fakeAuthSuccess() = AuthSuccess(
        token = TestFixtures.TOKEN, userId = TestFixtures.USER_ID,
        userName = "god", userEmail = "god@rutasapp.dev",
        userRole = "owner", userDisplayName = "God Admin",
        accountId = TestFixtures.ACCOUNT_ID, accountType = "individual",
        accountName = "God Admin", isCompany = false,
    )
}

// ════════════════════════════════════════════════════════════
// RutasViewModelTest
// ════════════════════════════════════════════════════════════

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class RutasViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var routeRepo: RouteRepository
    private lateinit var session:   FakeSessionManager
    private lateinit var vm:        RutasViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        routeRepo = mockk(relaxed = true)
        session   = FakeSessionManager()
        coEvery { routeRepo.observeAll() } returns flowOf(emptyList())
        vm = RutasViewModel(routeRepo, session)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `estado inicial isLoading true`() {
        assertThat(vm.ui.value.isLoading).isTrue()
    }

    @Test
    fun `rutas del DAO se reflejan en el estado`() = runTest {
        val routes = listOf(TestFixtures.routeEntity(), TestFixtures.routeEntity(uid = "uid-2"))
        coEvery { routeRepo.observeAll() } returns flowOf(routes)
        val newVm = RutasViewModel(routeRepo, session)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(newVm.ui.value.routes).hasSize(2)
        assertThat(newVm.ui.value.isLoading).isFalse()
    }

    @Test
    fun `createRoute con nombre vacio muestra error`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onShowCreateDialog()
        vm.onNewRouteNameChange("")
        vm.createRoute()
        assertThat(vm.ui.value.error).isNotNull()
        coVerify(exactly = 0) { routeRepo.createRoute(any(), any(), any()) }
    }

    @Test
    fun `createRoute valido llama al repositorio`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onShowCreateDialog()
        vm.onNewRouteNameChange("Nueva Ruta")
        vm.createRoute()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { routeRepo.createRoute("Nueva Ruta", any(), any()) }
        assertThat(vm.ui.value.showCreateDialog).isFalse()
    }

    @Test
    fun `onDismissCreateDialog limpia campos y cierra dialogo`() = runTest {
        vm.onShowCreateDialog()
        vm.onNewRouteNameChange("Borrar esto")
        vm.onDismissCreateDialog()
        assertThat(vm.ui.value.showCreateDialog).isFalse()
        assertThat(vm.ui.value.newRouteName).isEmpty()
    }
}

// ════════════════════════════════════════════════════════════
// RouteDetailViewModelTest
// ════════════════════════════════════════════════════════════

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class RouteDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var routeRepo: RouteRepository
    private lateinit var stopRepo:  StopRepository
    private lateinit var vm:        RouteDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        routeRepo = mockk(relaxed = true)
        stopRepo  = mockk(relaxed = true)
        coEvery { routeRepo.getByUid(any()) } returns TestFixtures.routeEntity()
        coEvery { stopRepo.observeByRoute(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun createVm(uid: String = "route-uid-001") = RouteDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("routeUid" to uid)),
        routeRepo        = routeRepo,
        stopRepo         = stopRepo,
    )

    @Test
    fun `carga la ruta al iniciar`() = runTest {
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.ui.value.route).isNotNull()
        assertThat(vm.ui.value.route!!.uid).isEqualTo("route-uid-001")
    }

    @Test
    fun `los stops del DAO se reflejan en el estado`() = runTest {
        val stops = listOf(TestFixtures.stopEntity(), TestFixtures.stopEntity(uid = "s2"))
        coEvery { stopRepo.observeByRoute(any()) } returns flowOf(stops)
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.ui.value.stops).hasSize(2)
    }

    @Test
    fun `markStopVisited llama al repositorio`() = runTest {
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.markStopVisited("stop-uid-001")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { stopRepo.markVisited("stop-uid-001") }
    }

    @Test
    fun `isLoading false tras cargar ruta`() = runTest {
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.ui.value.isLoading).isFalse()
    }
}

// ════════════════════════════════════════════════════════════
// CrearParadaViewModelTest
// ════════════════════════════════════════════════════════════

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class CrearParadaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var stopRepo:    StopRepository
    private lateinit var mapProvider: MapProvider
    private lateinit var vm:          CrearParadaViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        stopRepo    = mockk(relaxed = true)
        mapProvider = mockk(relaxed = true)
        coEvery { stopRepo.createStop(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            TestFixtures.stopEntity()
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun createVm(routeUid: String = "route-uid-001") = CrearParadaViewModel(
        savedStateHandle = SavedStateHandle(mapOf("routeUid" to routeUid)),
        stopRepo         = stopRepo,
        mapProvider      = mapProvider,
    )

    @Test
    fun `estado inicial limpio`() {
        vm = createVm()
        assertThat(vm.ui.value.name).isEmpty()
        assertThat(vm.ui.value.isSaving).isFalse()
        assertThat(vm.ui.value.savedUid).isNull()
        assertThat(vm.ui.value.priority).isEqualTo(3)
    }

    @Test
    fun `save con nombre vacio muestra error`() = runTest {
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.save()
        assertThat(vm.ui.value.error).contains("nombre")
        coVerify(exactly = 0) { stopRepo.createStop(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `save valido llama al repositorio con routeUid correcto`() = runTest {
        vm = createVm(routeUid = "route-uid-001")
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onNameChange("Farmacia Central")
        vm.save()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { stopRepo.createStop("route-uid-001", "Farmacia Central", any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `save valido establece savedUid`() = runTest {
        vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onNameChange("Farmacia Central")
        vm.save()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.ui.value.savedUid).isNotNull()
        assertThat(vm.ui.value.isSaving).isFalse()
    }

    @Test
    fun `onNameChange limpia error previo`() {
        vm = createVm()
        vm.save()
        assertThat(vm.ui.value.error).isNotNull()
        vm.onNameChange("Algo")
        assertThat(vm.ui.value.error).isNull()
    }

    @Test
    fun `onPriorityChange se limita entre 1 y 5`() {
        vm = createVm()
        vm.onPriorityChange(0)
        assertThat(vm.ui.value.priority).isEqualTo(1)
        vm.onPriorityChange(6)
        assertThat(vm.ui.value.priority).isEqualTo(5)
        vm.onPriorityChange(2)
        assertThat(vm.ui.value.priority).isEqualTo(2)
    }

    @Test
    fun `geocodeAddress no hace nada con direccion vacia`() = runTest {
        vm = createVm()
        vm.geocodeAddress()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { mapProvider.geocode(any()) }
    }

    @Test
    fun `geocodeAddress con resultado rellena lat y lng`() = runTest {
        coEvery { mapProvider.geocode(any()) } returns
            com.pabl3st.rutapp.core.map.MapLatLng(39.4699, -0.3763)
        vm = createVm()
        vm.onAddressChange("Calle Colón 12, Valencia")
        vm.geocodeAddress()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.ui.value.lat).isNotEmpty()
        assertThat(vm.ui.value.lng).isNotEmpty()
        assertThat(vm.ui.value.isGeocoding).isFalse()
    }

    @Test
    fun `geocodeAddress sin resultado no modifica coords`() = runTest {
        coEvery { mapProvider.geocode(any()) } returns null
        vm = createVm()
        vm.onAddressChange("Dirección inventada xyz")
        vm.geocodeAddress()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.ui.value.lat).isEmpty()
        assertThat(vm.ui.value.isGeocoding).isFalse()
    }
}
