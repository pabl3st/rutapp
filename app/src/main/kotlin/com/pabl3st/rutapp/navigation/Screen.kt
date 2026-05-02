package com.pabl3st.rutapp.navigation

sealed class Screen(val route: String) {
    object Auth      : Screen("auth")
    object Home      : Screen("home")
    object Rutas     : Screen("rutas")
    object Mapa      : Screen("mapa")
    object Kpis      : Screen("kpis")
    object Calendario: Screen("calendario")
    object Perfil    : Screen("perfil")
    object Admin     : Screen("admin")
    object Visita : Screen("visita/{stopUid}") {
        fun createRoute(uid: String) = "visita/$uid"
    }
    object RouteDetail : Screen("route/{routeUid}") {
        fun createRoute(uid: String) = "route/$uid"
    }
    object RouteMap : Screen("map/{routeUid}") {
        fun createRoute(uid: String) = "map/$uid"
    }
    object CrearParada : Screen("crear-parada/{routeUid}") {
        fun createRoute(routeUid: String) = "crear-parada/$routeUid"
    }
}
