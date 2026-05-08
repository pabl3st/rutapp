package com.pabl3st.rutapp.feature.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.BuildConfig
import com.pabl3st.rutapp.data.repository.AuthRepository
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PerfilUiState(
    val displayName: String  = "",
    val username: String     = "",
    val email: String        = "",
    val role: String         = "",
    val accountName: String  = "",
    val accountType: String  = "",
    val plan: String         = "",
    val appVersion: String        = "",
    val showLogoutDialog: Boolean = false,
    val isLoggingOut: Boolean    = false,
)

@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val session: SessionManager,
    private val authRepo: AuthRepository,
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

    fun onLogoutClick()    = _ui.update { it.copy(showLogoutDialog = true) }
    fun onLogoutDismiss()  = _ui.update { it.copy(showLogoutDialog = false) }

    fun onLogoutConfirm(onDone: () -> Unit) {
        _ui.update { it.copy(isLoggingOut = true, showLogoutDialog = false) }
        viewModelScope.launch {
            authRepo.logout()
            onDone()
        }
    }
}
