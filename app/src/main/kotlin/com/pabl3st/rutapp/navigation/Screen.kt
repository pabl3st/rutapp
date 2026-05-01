package com.pabl3st.rutapp.navigation

sealed class Screen(val route: String) {
    object Home      : Screen("home")
    object Rutas     : Screen("rutas")
    object Mapa      : Screen("mapa")
    object Kpis      : Screen("kpis")
    object Calendario: Screen("calendario")
    object Perfil    : Screen("perfil")
    object Admin     : Screen("admin")
    object Login     : Screen("login")
    object Register  : Screen("register")
}
