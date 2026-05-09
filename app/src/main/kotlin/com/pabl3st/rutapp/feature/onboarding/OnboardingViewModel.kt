package com.pabl3st.rutapp.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pabl3st.rutapp.core.permission.AppPermission
import com.pabl3st.rutapp.core.permission.PermissionManager
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep {
    SPLASH,              // logo + animación inicial (1.5s)
    PERMISSION_PROMPT,   // pide el permiso actual uno a uno
    COMPLETE,            // todos los permisos procesados → continuar
}

data class OnboardingUiState(
    val step: OnboardingStep                    = OnboardingStep.SPLASH,
    val currentPermission: AppPermission?       = null,
    val pendingPermissions: List<AppPermission> = emptyList(),
    val grantedCount: Int                       = 0,
    val totalCount: Int                         = 0,
    val isAlreadyLoggedIn: Boolean              = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val permissionManager: PermissionManager,
    private val session: SessionManager,
) : ViewModel() {

    private val _ui = MutableStateFlow(OnboardingUiState())
    val ui: StateFlow<OnboardingUiState> = _ui.asStateFlow()

    init {
        startSplash()
    }

    private fun startSplash() {
        viewModelScope.launch {
            delay(1_400L)  // splash mínimo 1.4s
            evaluateNext()
        }
    }

    /**
     * Evalúa qué hay que hacer después del splash:
     * - Si el onboarding ya se completó antes → COMPLETE directo
     * - Si hay permisos pendientes → mostrar el primero
     * - Si todo está ok → COMPLETE
     */
    fun evaluateNext() {
        val pending = permissionManager.onboardingPermissions
            .filter { perm ->
                perm.manifestPermissions.isNotEmpty() &&
                !permissionManager.isGranted(perm) &&
                !permissionManager.wasPermanentlyDenied(perm)
            }

        if (permissionManager.wasOnboardingShown() && pending.isEmpty()) {
            _ui.update { it.copy(
                step              = OnboardingStep.COMPLETE,
                isAlreadyLoggedIn = session.isLoggedIn,
            )}
            return
        }

        if (pending.isEmpty()) {
            permissionManager.markOnboardingShown()
            _ui.update { it.copy(
                step              = OnboardingStep.COMPLETE,
                isAlreadyLoggedIn = session.isLoggedIn,
            )}
            return
        }

        _ui.update { it.copy(
            step               = OnboardingStep.PERMISSION_PROMPT,
            currentPermission  = pending.first(),
            pendingPermissions = pending,
            totalCount         = pending.size + permissionManager.onboardingPermissions
                .filter { permissionManager.isGranted(it) }.size,
            grantedCount       = permissionManager.onboardingPermissions
                .filter { permissionManager.isGranted(it) }.size,
        )}
    }

    /** Llamado cuando el sistema devuelve el resultado del permiso */
    fun onPermissionResult(permission: AppPermission, granted: Boolean) {
        if (!granted) {
            permissionManager.markPermanentlyDenied(permission)
        }
        // Avanzar al siguiente permiso pendiente
        val remaining = _ui.value.pendingPermissions.drop(1)
            .filter { p ->
                p.manifestPermissions.isNotEmpty() &&
                !permissionManager.isGranted(p) &&
                !permissionManager.wasPermanentlyDenied(p)
            }

        if (remaining.isEmpty()) {
            permissionManager.markOnboardingShown()
            _ui.update { it.copy(
                step              = OnboardingStep.COMPLETE,
                isAlreadyLoggedIn = session.isLoggedIn,
            )}
        } else {
            _ui.update { it.copy(
                currentPermission  = remaining.first(),
                pendingPermissions = remaining,
                grantedCount       = permissionManager.onboardingPermissions
                    .filter { permissionManager.isGranted(it) }.size,
            )}
        }
    }

    /** El usuario pulsa "Omitir" en un permiso no crítico */
    fun skipCurrentPermission() {
        val current = _ui.value.currentPermission ?: return
        onPermissionResult(current, granted = false)
    }
}
