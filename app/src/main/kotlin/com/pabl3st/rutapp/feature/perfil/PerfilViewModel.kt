package com.pabl3st.rutapp.feature.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.BuildConfig
import com.pabl3st.rutapp.data.repository.AuthRepository
import com.pabl3st.rutapp.data.repository.BusinessProfileRepository
import com.pabl3st.rutapp.data.repository.UserPrefs
import com.pabl3st.rutapp.data.repository.UserPrefsRepository
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PerfilUiState(
    val displayName:      String  = "",
    val username:         String  = "",
    val email:            String  = "",
    val role:             String  = "",
    val accountName:      String  = "",
    val accountType:      String  = "",
    val plan:             String  = "",
    val appVersion:       String  = "",
    val sectorLabel:      String  = "",
    val showLogoutDialog: Boolean = false,
    val isLoggingOut:     Boolean = false,
)

@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val session:     SessionManager,
    private val authRepo:    AuthRepository,
    private val profileRepo: BusinessProfileRepository,
    private val prefsRepo:   UserPrefsRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(
        PerfilUiState(
            displayName = session.userDisplayName,
            username    = session.userName,
            email       = session.userEmail,
            role        = session.userRole,
            accountName = session.accountName,
            accountType = session.accountType,
            plan        = "free",
            appVersion  = BuildConfig.VERSION_NAME,
        )
    )
    val ui: StateFlow<PerfilUiState> = _ui.asStateFlow()

    // ── Preferencias — flujo reactivo directo desde DataStore ─
    val userPrefs: StateFlow<UserPrefs> = prefsRepo.prefs
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPrefs())

    init { loadSectorLabel() }

    private fun loadSectorLabel() {
        viewModelScope.launch {
            profileRepo.observeProfile().collect { profile ->
                val label = if (profile != null)
                    profileRepo.sectorLabel(profile.sector)
                else
                    profileRepo.sectorLabel("custom")
                _ui.update { it.copy(sectorLabel = label) }
            }
        }
    }

    // ── Setters de preferencias ───────────────────────────────
    fun setLanguage(lang: String)           = pref { copy(language = lang) }
    fun setShowVisitDuration(v: Boolean)    = pref { copy(showVisitDuration = v) }
    fun setShowNextAction(v: Boolean)       = pref { copy(showNextAction = v) }
    fun setShowPhotos(v: Boolean)           = pref { copy(showPhotos = v) }
    fun setRequireResult(v: Boolean)        = pref { copy(requireResult = v) }
    fun setPushEnabled(v: Boolean)          = pref { copy(pushEnabled = v) }
    fun setAutoSync(v: Boolean)             = pref { copy(autoSync = v) }
    fun setJornadaReminder(v: Boolean)      = pref { copy(jornadaReminder = v) }
    fun setJornadaReminderHour(h: Int)      = pref { copy(jornadaReminderHour = h.coerceIn(5, 22)) }

    private fun pref(transform: UserPrefs.() -> UserPrefs) {
        viewModelScope.launch { prefsRepo.update(transform) }
    }

    // ── Logout ────────────────────────────────────────────────
    fun onLogoutClick()   = _ui.update { it.copy(showLogoutDialog = true) }
    fun onLogoutDismiss() = _ui.update { it.copy(showLogoutDialog = false) }

    fun onLogoutConfirm(onDone: () -> Unit) {
        _ui.update { it.copy(isLoggingOut = true, showLogoutDialog = false) }
        viewModelScope.launch {
            authRepo.logout()
            onDone()
        }
    }
}
