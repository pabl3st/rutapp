package com.pabl3st.rutapp.feature.onboarding

import android.content.Intent
import com.pabl3st.rutapp.core.BaseViewModel
import com.pabl3st.rutapp.core.permission.AppPermission
import com.pabl3st.rutapp.core.permission.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Estado y acciones del onboarding de ubicacion post-login.
 *
 * Foreground (fine/coarse) lo maneja la propia pantalla con un launcher.
 * Este VM cubre los EXTRAS que solo tienen sentido tras conceder foreground
 * y solo para el rol de campo (agent): localizacion en background y exencion
 * de optimizacion de bateria - ambos criticos para que el ForegroundService
 * de GPS sobreviva a la jornada.
 */
@HiltViewModel
class LocationOnboardingViewModel @Inject constructor(
    private val pm: PermissionManager,
) : BaseViewModel() {

    /** Permisos manifest a solicitar para background (vacio en < Android 10). */
    val backgroundManifestPermissions: Array<String> =
        AppPermission.LocationBackground.manifestPermissions.toTypedArray()

    fun backgroundApplies(): Boolean = pm.backgroundLocationApplies()

    fun isBackgroundGranted(): Boolean = pm.isBackgroundLocationGranted()

    fun isBatteryIgnoring(): Boolean = pm.isIgnoringBatteryOptimizations()

    fun batteryRequestIntent(): Intent = pm.batteryOptimizationRequestIntent()

    /** True si, tras conceder foreground, aun queda algun extra util para el agente. */
    fun hasPendingExtras(): Boolean =
        (backgroundApplies() && !isBackgroundGranted()) || !isBatteryIgnoring()

    fun markBackgroundShown() = pm.markBackgroundPromptShown()

    fun markBatteryShown() = pm.markBatteryPromptShown()
}
