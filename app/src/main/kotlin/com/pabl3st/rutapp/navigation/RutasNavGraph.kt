package com.pabl3st.rutapp.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pabl3st.rutapp.feature.auth.AuthRoot
import com.pabl3st.rutapp.feature.home.HomeScreen
import com.pabl3st.rutapp.feature.auth.ExitAppDialog

@Composable
fun RutasNavGraph(
    navController: NavHostController = rememberNavController(),
    onExitApp: () -> Unit,           // viene de MainActivity.finish()
) {
    NavHost(navController = navController, startDestination = Screen.Auth.route) {

        // ── Auth ───────────────────────────────────────────────
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

        // ── Home ───────────────────────────────────────────────
        // Home es la pantalla raíz post-login. Atrás desde aquí = salir.
        composable(Screen.Home.route) {
            var showExitDialog by remember { mutableStateOf(false) }
            BackHandler { showExitDialog = true }

            if (showExitDialog) {
                ExitAppDialog(
                    onConfirm = onExitApp,
                    onDismiss = { showExitDialog = false },
                )
            }
            HomeScreen()
        }

        // ── Resto de pantallas (S02+) ──────────────────────────
        // Estas pantallas tienen navegación normal: atrás = popBackStack
        composable(Screen.Rutas.route)     { PlaceholderScreen("Rutas · S03") }
        composable(Screen.Mapa.route)      { PlaceholderScreen("Mapa · S04") }
        composable(Screen.Kpis.route)      { PlaceholderScreen("KPIs · S06") }
        composable(Screen.Calendario.route){ PlaceholderScreen("Calendario · S07") }
        composable(Screen.Perfil.route)    { PlaceholderScreen("Perfil · S01") }
        composable(Screen.Admin.route)     { PlaceholderScreen("Admin · S09") }
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
