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
    object Visita : Screen("visita/{stopUid}?date={date}") {
        fun createRoute(uid: String, date: String? = null): String =
            if (date.isNullOrBlank()) "visita/$uid" else "visita/$uid?date=$date"
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
    object EditarParada : Screen("editar-parada/{stopUid}") {
        fun createRoute(stopUid: String) = "editar-parada/$stopUid"
    }
    object LocationOnboarding : Screen("location-onboarding")
    object Team : Screen("team")
    object AgentDetail : Screen("agent/{agentId}") {
        fun createRoute(agentId: Int) = "agent/$agentId"
    }
    object BusinessProfile : Screen("business-profile")
    object AddStops    : Screen("add-stops/{routeUid}") {
        fun createRoute(routeUid: String) = "add-stops/$routeUid"
    }
    object Biblioteca      : Screen("biblioteca")
    object Importar        : Screen("importar")
    object GodDashboard    : Screen("god-dashboard")
    object Onboarding      : Screen("onboarding")
}

