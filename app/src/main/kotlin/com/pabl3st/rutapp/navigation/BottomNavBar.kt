package com.pabl3st.rutapp.navigation

import com.pabl3st.rutapp.core.UserRole

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

private data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

private val NAV_ITEMS = listOf(
    NavItem(Screen.Home,       "Hoy",    Icons.Outlined.Home,           Icons.Filled.Home),
    NavItem(Screen.Rutas,      "Rutas",  Icons.AutoMirrored.Outlined.AltRoute,       Icons.AutoMirrored.Filled.AltRoute),
    NavItem(Screen.Calendario, "Agenda", Icons.Outlined.CalendarMonth,  Icons.Filled.CalendarMonth),
    NavItem(Screen.Kpis,       "KPIs",  Icons.Outlined.BarChart,       Icons.Filled.BarChart),
    NavItem(Screen.Perfil,     "Perfil", Icons.Outlined.AccountCircle,  Icons.Filled.AccountCircle),
)

// Rutas donde se muestra la bottom bar
val BOTTOM_BAR_ROUTES = NAV_ITEMS.map { it.screen.route }.toSet()

@Composable
fun RutasBottomBar(navController: NavHostController, userRole: String = "agent") {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // viewer: solo Perfil — sin acceso a rutas, mapa ni KPIs
    // god: solo Perfil (su dashboard está accesible desde Perfil)
    val role = UserRole.from(userRole)
    // Tabs visibles por rol — cada rol ve exactamente lo que necesita
    val visibleItems: List<NavItem> = when (role) {
        UserRole.VIEWER ->
            // Solo lectura — únicamente puede ver su perfil
            NAV_ITEMS.filter { it.screen == Screen.Perfil }

        UserRole.GOD ->
            // Dios — gestión cross-account desde Perfil, no ejecuta rutas
            NAV_ITEMS.filter { it.screen == Screen.Perfil }

        UserRole.AGENT ->
            // Comercial en campo — necesita todos los tabs operativos
            NAV_ITEMS

        UserRole.MANAGER ->
            // Supervisor — necesita todos los tabs (ve rutas de sus agentes en Rutas/Agenda)
            NAV_ITEMS

        UserRole.ADMIN ->
            // Jefe territorial — igual que manager + acceso a gestión
            NAV_ITEMS

        UserRole.OWNER ->
            // Empresa cliente — visión completa de la cuenta
            NAV_ITEMS
    }

    NavigationBar {
        visibleItems.forEach { item ->
            val selected = currentRoute == item.screen.route
            NavigationBarItem(
                selected     = selected,
                onClick      = {
                    if (!selected) {
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                },
                modifier = Modifier.semantics { testTag = "bottom-nav-${item.screen.route}" },
                icon  = {
                    Icon(
                        imageVector        = if (selected) item.selectedIcon else item.icon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label) },
            )
        }
    }
}

