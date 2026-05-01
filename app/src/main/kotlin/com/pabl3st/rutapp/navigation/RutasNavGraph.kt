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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import com.pabl3st.rutapp.feature.auth.AuthRoot
import com.pabl3st.rutapp.feature.auth.ExitAppDialog
import com.pabl3st.rutapp.feature.home.HomeScreen
import com.pabl3st.rutapp.feature.perfil.PerfilScreen
import com.pabl3st.rutapp.feature.rutas.RouteDetailScreen
import com.pabl3st.rutapp.feature.rutas.RouteMapScreen
import com.pabl3st.rutapp.feature.rutas.RutasScreen
import com.pabl3st.rutapp.feature.visita.VisitaScreen

// ── Transiciones de navegación ───────────────────────────
private val enterPush   = slideInHorizontally(tween(280)) { it / 4 } + fadeIn(tween(280))
private val exitPush    = slideOutHorizontally(tween(280)) { -it / 4 } + fadeOut(tween(200))
private val enterPop    = slideInHorizontally(tween(280)) { -it / 4 } + fadeIn(tween(280))
private val exitPop     = slideOutHorizontally(tween(280)) { it / 4 } + fadeOut(tween(200))
private val enterFade   = fadeIn(tween(220))
private val exitFade    = fadeOut(tween(180))

@Composable
fun RutasNavGraph(
    navController: NavHostController = rememberNavController(),
    onExitApp: () -> Unit,
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in BOTTOM_BAR_ROUTES

    Scaffold(
        bottomBar = {
            if (showBottomBar) RutasBottomBar(navController)
        }
    ) { scaffoldPadding ->
        NavHost(
            navController       = navController,
            startDestination    = Screen.Auth.route,
            modifier            = Modifier.padding(scaffoldPadding),
            enterTransition     = { enterFade },
            exitTransition      = { exitFade },
            popEnterTransition  = { enterFade },
            popExitTransition   = { exitFade },
        ) {

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
                    onRouteClick = { uid -> navController.navigate(Screen.RouteDetail.createRoute(uid)) },
                )
            }

            composable(Screen.Rutas.route) {
                RutasScreen(
                    onRouteClick = { uid -> navController.navigate(Screen.RouteDetail.createRoute(uid)) },
                    onBack       = { navController.popBackStack() },
                )
            }

            composable(
                route               = Screen.RouteDetail.route,
                arguments           = listOf(navArgument("routeUid") { type = NavType.StringType }),
                enterTransition     = { enterPush },
                exitTransition      = { exitPush },
                popEnterTransition  = { enterPop },
                popExitTransition   = { exitPop },
            ) { backStackEntry ->
                val routeUid = backStackEntry.arguments?.getString("routeUid") ?: return@composable
                RouteDetailScreen(
                    routeUid        = routeUid,
                    onBack          = { navController.popBackStack() },
                    onNavigateToMap = { uid -> navController.navigate(Screen.RouteMap.createRoute(uid)) },
                    onStopClick     = { uid -> navController.navigate(Screen.Visita.createRoute(uid)) },
                )
            }

            composable(
                route               = Screen.RouteMap.route,
                arguments           = listOf(navArgument("routeUid") { type = NavType.StringType }),
                enterTransition     = { enterPush },
                exitTransition      = { exitPush },
                popEnterTransition  = { enterPop },
                popExitTransition   = { exitPop },
            ) { backStackEntry ->
                val routeUid = backStackEntry.arguments?.getString("routeUid") ?: return@composable
                RouteMapScreen(
                    routeUid = routeUid,
                    onBack   = { navController.popBackStack() },
                )
            }

            composable(
                route               = Screen.Visita.route,
                arguments           = listOf(navArgument("stopUid") { type = NavType.StringType }),
                enterTransition     = { enterPush },
                exitTransition      = { exitPush },
                popEnterTransition  = { enterPop },
                popExitTransition   = { exitPop },
            ) { backStackEntry ->
                val stopUid = backStackEntry.arguments?.getString("stopUid") ?: return@composable
                VisitaScreen(
                    stopUid = stopUid,
                    onBack  = { navController.popBackStack() },
                )
            }

            composable(Screen.Mapa.route)       { PlaceholderScreen("Mapa", "S07") }
            composable(Screen.Kpis.route)       { PlaceholderScreen("KPIs", "S07") }
            composable(Screen.Calendario.route) { PlaceholderScreen("Calendario", "S08") }
            composable(Screen.Admin.route)      { PlaceholderScreen("Admin", "S09") }

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
}

@Composable
fun PlaceholderScreen(label: String, sprint: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Disponible en $sprint",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Build OK",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}
