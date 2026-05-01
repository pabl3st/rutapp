package com.pabl3st.rutapp.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun RutasNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route)      { PlaceholderScreen("Home · S02") }
        composable(Screen.Login.route)     { PlaceholderScreen("Login · S01") }
        composable(Screen.Register.route)  { PlaceholderScreen("Registro · S01") }
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
            Text("RutasApp", style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text("Build OK", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary)
        }
    }
}
