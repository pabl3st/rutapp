package com.pabl3st.rutapp.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pabl3st.rutapp.feature.auth.AuthRoot
import com.pabl3st.rutapp.feature.auth.ExitAppDialog
import com.pabl3st.rutapp.feature.home.HomeScreen
import com.pabl3st.rutapp.feature.perfil.PerfilScreen
import com.pabl3st.rutapp.feature.rutas.RouteDetailScreen
import com.pabl3st.rutapp.feature.rutas.RouteMapScreen
import com.pabl3st.rutapp.feature.rutas.RutasScreen

@Composable
fun RutasNavGraph(
    navController: NavHostController = rememberNavController(),
    onExitApp: () -> Unit,
) {
    NavHost(navController = navController, startDestination = Screen.Auth.route) {

        composable(Screen.Auth.route) {
            AuthRoot(
                onAuthenticated = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onExitApp = onExitApp,
            )
        }

        composable(Screen.Home.route) {
            var showExitDialog by remember { mutableStateOf(false) }
            BackHandler { showExitDialog = true }
            if (showExitDialog) {
                ExitAppDialog(onConfirm = onExitApp, onDismiss = { showExitDialog = false })
            }
            HomeScreen(
                onRouteClick       = { uid -> navController.navigate(Screen.RouteDetail.createRoute(uid)) },
                onNavigateToRutas  = { navController.navigate(Screen.Rutas.route) },
                onNavigateToPerfil = { navController.navigate(Screen.Perfil.route) },
            )
        }

        composable(Screen.Rutas.route) {
            RutasScreen(
                onRouteClick = { uid -> navController.navigate(Screen.RouteDetail.createRoute(uid)) },
                onBack       = { navController.popBackStack() },
            )
        }

        composable(
            route     = Screen.RouteDetail.route,
            arguments = listOf(navArgument("routeUid") { type = NavType.StringType }),
        ) { backStackEntry ->
            val routeUid = backStackEntry.arguments?.getString("routeUid") ?: return@composable
            RouteDetailScreen(
                routeUid        = routeUid,
                onBack          = { navController.popBackStack() },
                onNavigateToMap = { uid -> navController.navigate(Screen.RouteMap.createRoute(uid)) },
            )
        }

        composable(
            route     = Screen.RouteMap.route,
            arguments = listOf(navArgument("routeUid") { type = NavType.StringType }),
        ) { backStackEntry ->
            val routeUid = backStackEntry.arguments?.getString("routeUid") ?: return@composable
            RouteMapScreen(
                routeUid = routeUid,
                onBack   = { navController.popBackStack() },
            )
        }

        composable(Screen.Mapa.route)       { PlaceholderScreen("Mapa · S05") }
        composable(Screen.Kpis.route)       { PlaceholderScreen("KPIs · S08") }
        composable(Screen.Calendario.route) { PlaceholderScreen("Calendario · S08") }
        composable(Screen.Admin.route)      { PlaceholderScreen("Admin · S09") }

        composable(Screen.Perfil.route) {
            PerfilScreen(
                onLoggedOut = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
fun PlaceholderScreen(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("RutasApp",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text("Build OK",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary)
        }
    }
}
