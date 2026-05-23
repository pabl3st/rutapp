package com.pabl3st.rutapp.navigation

import com.pabl3st.rutapp.core.UserRole

import androidx.lifecycle.ViewModel
import com.pabl3st.rutapp.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel mínimo para el NavGraph.
 * Solo expone el rol del usuario actual desde SessionManager
 * para que el NavGraph pueda tomar decisiones de navegación
 * (ej: god → GodDashboard, resto → AdminScreen) sin depender
 * de HomeViewModel ni flows complejos.
 */
@HiltViewModel
class NavStateViewModel @Inject constructor(
    private val session: SessionManager,
) : ViewModel() {
    val userRole: String get() = session.userRole
    val isGod:    Boolean get() = UserRole.from(session.userRole).isGod
}
