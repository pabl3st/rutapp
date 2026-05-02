package com.pabl3st.rutapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

private data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

private val NAV_ITEMS = listOf(
    NavItem(Screen.Home,      "Hoy",     Icons.Default.Home,          Icons.Default.Home),
    NavItem(Screen.Rutas,     "Rutas",   Icons.Default.Route,         Icons.Default.Route),
    NavItem(Screen.Mapa,      "Mapa",    Icons.Default.Map,           Icons.Default.Map),
    NavItem(Screen.Kpis,      "KPIs",    Icons.Default.BarChart,      Icons.Default.BarChart),
    NavItem(Screen.Perfil,    "Perfil",  Icons.Default.AccountCircle, Icons.Default.AccountCircle),
)

// Rutas donde se muestra la bottom bar
val BOTTOM_BAR_ROUTES = NAV_ITEMS.map { it.screen.route }.toSet()

private val MANAGER_ROLES = setOf("owner", "admin", "manager")

@Composable
fun RutasBottomBar(navController: NavHostController, userRole: String = "agent") {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // agents no ven el tab Rutas — su flujo es Home → paradas → visita
    val visibleItems = if (userRole in MANAGER_ROLES) NAV_ITEMS
                       else NAV_ITEMS.filter { it.screen != Screen.Rutas }

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
