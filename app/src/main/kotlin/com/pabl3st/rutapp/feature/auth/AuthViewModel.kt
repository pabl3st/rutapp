package com.pabl3st.rutapp.feature.auth

import com.pabl3st.rutapp.core.BaseViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.data.repository.AuthRepository
import com.pabl3st.rutapp.data.repository.AuthResult
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthScreen { SPLASH, CHOOSE_TYPE, LOGIN, REGISTER_INDIVIDUAL, REGISTER_COMPANY }

data class AuthUiState(
    val screen: AuthScreen       = AuthScreen.SPLASH,
    val isLoading: Boolean       = false,
    val error: String?           = null,
    val isAuthenticated: Boolean = false,
    val isCompany: Boolean       = false,
    val showExitDialog: Boolean  = false,
    val showDiscardDialog: Boolean = false,
    // Campos del formulario
    val username: String         = "",
    val email: String            = "",
    val password: String         = "",
    val name: String             = "",
    val companyName: String      = "",
    val inviteCode: String       = "",
    val passwordVisible: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo:    AuthRepository,
    private val session: SessionManager,
    private val db:      com.pabl3st.rutapp.data.local.RutasDatabase,
) : BaseViewModel() {

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    init { checkSession() }

    private fun checkSession() {
        viewModelScope.launch {
            if (!session.isLoggedIn) {
                _ui.update { it.copy(screen = AuthScreen.CHOOSE_TYPE) }
                return@launch
            }
            when (val r = repo.verifySession()) {
                is AuthResult.Success -> _ui.update { it.copy(isAuthenticated = true) }
                is AuthResult.Error   -> {
                    // Si es error de red (sin conexión) y tenemos sesión cacheada → dejar pasar
                    // Solo forzar re-login si el servidor rechaza explícitamente el token (401/403)
                    if (r.code == 401 || r.code == 403) {
                        _ui.update { it.copy(screen = AuthScreen.CHOOSE_TYPE) }
                    } else {
                        // Error de red u otro — usar sesión cacheada si existe
                        if (session.isLoggedIn) {
                            _ui.update { it.copy(isAuthenticated = true) }
                        } else {
                            _ui.update { it.copy(screen = AuthScreen.CHOOSE_TYPE) }
                        }
                    }
                }
            }
        }
    }

    // ── Lógica de back — el único punto de verdad para el botón atrás ──
    // Retorna true si se manejó internamente, false si debe dejar pasar al sistema
    fun handleBack(): Boolean {
        val s = _ui.value
        return when (s.screen) {
            AuthScreen.SPLASH -> false  // dejar al sistema

            AuthScreen.CHOOSE_TYPE -> {
                // En ChooseType, atrás = pedir confirmación para salir de la app
                _ui.update { it.copy(showExitDialog = true) }
                true
            }

            AuthScreen.LOGIN -> {
                // Si tiene datos escritos, confirmar descarte. Si no, volver a ChooseType.
                if (s.username.isNotBlank() || s.password.isNotBlank()) {
                    _ui.update { it.copy(showDiscardDialog = true) }
                } else {
                    _ui.update { it.copy(screen = AuthScreen.CHOOSE_TYPE, error = null) }
                }
                true
            }

            AuthScreen.REGISTER_INDIVIDUAL, AuthScreen.REGISTER_COMPANY -> {
                val hasData = s.name.isNotBlank() || s.username.isNotBlank() ||
                              s.email.isNotBlank() || s.password.isNotBlank() ||
                              s.companyName.isNotBlank()
                if (hasData) {
                    _ui.update { it.copy(showDiscardDialog = true) }
                } else {
                    _ui.update { it.copy(screen = AuthScreen.CHOOSE_TYPE, error = null) }
                }
                true
            }
        }
    }

    // ── Diálogos ───────────────────────────────────────────────
    fun onExitConfirmed()          = _ui.update { it.copy(showExitDialog = false) }
    fun onExitDismissed()          = _ui.update { it.copy(showExitDialog = false) }
    fun onDiscardConfirmed() {
        _ui.update { it.copy(
            showDiscardDialog = false,
            screen = AuthScreen.CHOOSE_TYPE,
            // Limpiar campos
            username = "", email = "", password = "",
            name = "", companyName = "", inviteCode = "",
            error = null,
        )}
    }
    fun onDiscardDismissed()       = _ui.update { it.copy(showDiscardDialog = false) }

    // ── Navegación ─────────────────────────────────────────────
    fun onChooseIndividual() = _ui.update { it.copy(screen = AuthScreen.REGISTER_INDIVIDUAL, error = null) }
    fun onChooseCompany()    = _ui.update { it.copy(screen = AuthScreen.REGISTER_COMPANY,    error = null) }
    fun onGoToLogin()        = _ui.update { it.copy(screen = AuthScreen.LOGIN,               error = null) }
    fun onBackToChoose()     = _ui.update { it.copy(screen = AuthScreen.CHOOSE_TYPE,         error = null) }

    // ── Campos ─────────────────────────────────────────────────
    fun onUsernameChange(v: String)    = _ui.update { it.copy(username    = v, error = null) }
    fun onEmailChange(v: String)       = _ui.update { it.copy(email       = v, error = null) }
    fun onPasswordChange(v: String)    = _ui.update { it.copy(password    = v, error = null) }
    fun onNameChange(v: String)        = _ui.update { it.copy(name        = v, error = null) }
    fun onCompanyNameChange(v: String) = _ui.update { it.copy(companyName = v, error = null) }
    fun onInviteCodeChange(v: String)  = _ui.update { it.copy(inviteCode  = v, error = null) }
    fun onTogglePassword()             = _ui.update { it.copy(passwordVisible = !it.passwordVisible) }
    fun clearError()                   = _ui.update { it.copy(error = null) }

    // ── Acciones ───────────────────────────────────────────────
    fun registerIndividual() {
        val s = _ui.value
        if (!validateRegister(s.name, s.username, s.email, s.password)) return
        doLaunch {
            when (val r = repo.registerIndividual(s.name, s.username, s.email, s.password)) {
                is AuthResult.Success -> _ui.update { it.copy(isAuthenticated = true, isLoading = false) }
                is AuthResult.Error   -> _ui.update { it.copy(error = r.message, isLoading = false) }
            }
        }
    }

    fun registerCompany() {
        val s = _ui.value
        if (s.companyName.isBlank()) { _ui.update { it.copy(error = "El nombre de empresa es obligatorio") }; return }
        if (!validateRegister(s.name, s.username, s.email, s.password)) return
        doLaunch {
            when (val r = repo.registerCompany(s.companyName, s.name, s.username, s.email, s.password)) {
                is AuthResult.Success -> _ui.update { it.copy(isAuthenticated = true, isLoading = false, isCompany = true) }
                is AuthResult.Error   -> _ui.update { it.copy(error = r.message, isLoading = false) }
            }
        }
    }

    fun login() {
        val s = _ui.value
        if (s.username.isBlank()) { _ui.update { it.copy(error = "Introduce tu usuario o email") }; return }
        if (s.password.isBlank()) { _ui.update { it.copy(error = "Introduce tu contraseña") };      return }
        doLaunch {
            when (val r = repo.login(s.username.trim(), s.password)) {
                is AuthResult.Success -> _ui.update { it.copy(isAuthenticated = true, isLoading = false, isCompany = r.data.isCompany) }
                is AuthResult.Error   -> _ui.update { it.copy(error = r.message, isLoading = false) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            try { db.clearAllTables() } catch (_: Exception) {}
            _ui.update { AuthUiState(screen = AuthScreen.CHOOSE_TYPE) }
        }
    }

    private fun doLaunch(block: suspend () -> Unit) {
        _ui.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch { block() }
    }

    private fun validateRegister(name: String, username: String, email: String, password: String): Boolean =
        when {
            name.isBlank()     -> { _ui.update { it.copy(error = "El nombre es obligatorio") };                              false }
            username.isBlank() -> { _ui.update { it.copy(error = "El nombre de usuario es obligatorio") };                  false }
            username.length < 3-> { _ui.update { it.copy(error = "El usuario debe tener al menos 3 caracteres") };          false }
            !username.matches(Regex("[a-zA-Z0-9_]+")) ->
                                  { _ui.update { it.copy(error = "Solo letras, números y guión bajo") };                    false }
            email.isBlank()    -> { _ui.update { it.copy(error = "El email es obligatorio") };                              false }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                                  { _ui.update { it.copy(error = "El email no es válido") };                                false }
            password.length < 8-> { _ui.update { it.copy(error = "La contraseña debe tener al menos 8 caracteres") };      false }
            else               -> true
        }
    override fun onCoroutineError(t: Throwable) {
        _ui.update { it.copy(
            isLoading = false,
            error     = t.message ?: "Error inesperado",
        )}
    }

}

