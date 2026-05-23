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
    val visibleItems = when (userRole) {
        -> if (UserRole.from(it).isViewer) NAV_ITEMS.filter { n -> n.screen == Screen.Perfil }
        else     -> NAV_ITEMS
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

