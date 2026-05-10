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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.navArgument
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import com.pabl3st.rutapp.feature.auth.AuthRoot
import com.pabl3st.rutapp.feature.onboarding.OnboardingScreen
import com.pabl3st.rutapp.feature.auth.ExitAppDialog
import com.pabl3st.rutapp.feature.home.HomeScreen
import com.pabl3st.rutapp.feature.perfil.BusinessProfileScreen
import com.pabl3st.rutapp.feature.perfil.PerfilScreen
import com.pabl3st.rutapp.feature.biblioteca.BibliotecaScreen
import com.pabl3st.rutapp.feature.rutas.CrearParadaScreen
import com.pabl3st.rutapp.feature.rutas.RouteDetailScreen
import com.pabl3st.rutapp.feature.rutas.RouteMapScreen
import com.pabl3st.rutapp.feature.rutas.RutasScreen
import com.pabl3st.rutapp.feature.admin.AdminScreen
import com.pabl3st.rutapp.feature.admin.GodDashboardScreen
import com.pabl3st.rutapp.feature.calendario.CalendarioScreen
import com.pabl3st.rutapp.feature.kpis.KpisScreen
import com.pabl3st.rutapp.feature.mapa.GlobalMapScreen
import com.pabl3st.rutapp.feature.importar.ImportarScreen
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
    val backStack    by navController.currentBackStackEntryAsState()
    val currentRoute  = backStack?.destination?.route
    val showBottomBar = currentRoute in BOTTOM_BAR_ROUTES

    // Leer rol desde SessionManager via un ViewModel ligero
    val navVm: NavStateViewModel = hiltViewModel()
    val userRole = navVm.userRole

    Scaffold(
        bottomBar = {
            if (showBottomBar) RutasBottomBar(
                navController = navController,
                userRole      = userRole,
            )
        }
    ) { scaffoldPadding ->
        NavHost(
            navController       = navController,
            startDestination    = Screen.Onboarding.route,
            modifier            = Modifier.padding(scaffoldPadding),
            enterTransition     = { enterFade },
            exitTransition      = { exitFade },
            popEnterTransition  = { enterFade },
            popExitTransition   = { exitFade },
        ) {

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = { isLoggedIn ->
                        if (isLoggedIn) {
                            // Ya tiene sesión — ir directo a home
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        } else {
                            // Sin sesión — ir a login
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

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
                    onRouteClick     = { uid -> navController.navigate(Screen.RouteDetail.createRoute(uid)) },
                    onStopClick      = { uid -> navController.navigate(Screen.Visita.createRoute(uid)) },
                    onNavigateToMapa = { navController.navigate(Screen.Mapa.route) },
                )
            }

            composable(Screen.Rutas.route) {
                RutasScreen(
                    onRouteClick = { uid -> navController.navigate(Screen.RouteDetail.createRoute(uid)) },
                    onBack       = { navController.popBackStack() },
                    onImport     = { navController.navigate(Screen.Importar.route) },
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
                    onAddStop       = { uid -> navController.navigate(Screen.CrearParada.createRoute(uid)) },
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
                route               = Screen.CrearParada.route,
                arguments           = listOf(navArgument("routeUid") { type = NavType.StringType }),
                enterTransition     = { enterPush },
                exitTransition      = { exitPush },
                popEnterTransition  = { enterPop },
                popExitTransition   = { exitPop },
            ) {
                CrearParadaScreen(
                    onBack  = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
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

            composable(Screen.Mapa.route) {
                GlobalMapScreen(
                    onNavigateToStop  = { uid -> navController.navigate(Screen.Visita.createRoute(uid)) },
                    onNavigateToRoute = { uid -> navController.navigate(Screen.RouteDetail.createRoute(uid)) },
                )
            }
            composable(Screen.Kpis.route) {
                KpisScreen(
                    onNavigateToBiblioteca = { navController.navigate(Screen.Biblioteca.route) },
                )
            }
            composable(
                route = Screen.Calendario.route,
                // Tab del BottomNav → fade como el resto de tabs
            ) {
                val calBackEntry by navController.currentBackStackEntryAsState()
                val calShowBack = calBackEntry?.let {
                    navController.previousBackStackEntry?.destination?.route
                        ?.let { r -> r !in BOTTOM_BAR_ROUTES } ?: false
                } ?: false
                CalendarioScreen(
                    onBack          = { navController.popBackStack() },
                    showBackButton  = calShowBack,
                    onRouteClick    = { uid -> navController.navigate(Screen.RouteDetail.createRoute(uid)) },
                )
            }
            composable(
                route              = Screen.Admin.route,
                enterTransition    = { enterPush },
                exitTransition     = { exitPush },
                popEnterTransition = { enterPop },
                popExitTransition  = { exitPop },
            ) {
                AdminScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.GodDashboard.route) {
                GodDashboardScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Perfil.route) {
                PerfilScreen(
                    onLoggedOut = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onNavigateToBusinessProfile = {
                        navController.navigate(Screen.BusinessProfile.route)
                    },
                    onNavigateToCalendario = {
                        navController.navigate(Screen.Calendario.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAdmin = {
                        if (userRole == "god")
                            navController.navigate(Screen.GodDashboard.route)
                        else
                            navController.navigate(Screen.Admin.route)
                    },
                )
            }

            composable(
                route              = Screen.BusinessProfile.route,
                enterTransition    = { enterPush },
                exitTransition     = { exitPush },
                popEnterTransition = { enterPop },
                popExitTransition  = { exitPop },
            ) {
                BusinessProfileScreen(onBack = { navController.popBackStack() })
            }

            composable(route = Screen.Biblioteca.route) {
                BibliotecaScreen(
                    onBack      = { navController.popBackStack() },
                    onStopClick = { stopUid ->
                        navController.navigate(Screen.Visita.createRoute(stopUid))
                    },
                )
            }
            composable(route = Screen.Importar.route) {
                ImportarScreen(
                    onBack = { navController.popBackStack() },
                    onDone = {
                        navController.navigate(Screen.Rutas.route) {
                            popUpTo(Screen.Importar.route) { inclusive = true }
                        }
                    },
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





