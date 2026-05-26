package com.pabl3st.rutapp.core

/**
 * Roles de usuario en RutasApp.
 * Fuente de verdad única — sustituye los strings "owner","admin","manager","agent","viewer","god"
 * dispersos por el código.
 */
enum class UserRole(val key: String, val level: Int) {
    VIEWER ("viewer",  1),
    AGENT  ("agent",   2),
    MANAGER("manager", 3),
    ADMIN  ("admin",   4),
    OWNER  ("owner",   5),
    GOD    ("god",     6),
    ;

    // ── Permisos funcionales ──────────────────────────────────

    /** Puede crear/gestionar rutas */
    val canCreateRoutes: Boolean get() = level >= AGENT.level

    /** Puede eliminar rutas (borrado masivo) */
    val canDeleteRoutes: Boolean get() = level >= ADMIN.level

    /** Puede ver el panel Admin */
    val canAccessAdmin: Boolean get() = level >= ADMIN.level && this != GOD

    /** Puede ver el equipo (Mi equipo / KPIs equipo) */
    val canViewTeam: Boolean get() = level >= MANAGER.level && this != GOD

    /** Puede invitar usuarios */
    val canInviteUsers: Boolean get() = level >= ADMIN.level

    /** Puede reasignar rutas a otros usuarios */
    val canReassignRoutes: Boolean get() = level >= MANAGER.level

    /** Puede editar stops (añadir/eliminar en RouteDetail) */
    val canEditStops: Boolean get() = level >= MANAGER.level || this == OWNER

    /** Puede configurar perfil de negocio (sector, KPIs) */
    val canEditBusinessProfile: Boolean get() = level >= ADMIN.level || this == GOD

    /** Puede ver el God Dashboard */
    val isGod: Boolean get() = this == GOD

    /** Puede ver como mínimo la pantalla de Perfil (todos excepto sin sesión) */
    val isViewer: Boolean get() = this == VIEWER

    /** Puede acceder al wizard de importación */
    val canImport: Boolean get() = level >= AGENT.level

    /** Usuarios de nivel inferior al que puede asignar como supervisados */
    val assignableRoles: Set<UserRole> get() = when (this) {
        GOD     -> setOf(OWNER, ADMIN, MANAGER, AGENT)
        OWNER   -> setOf(ADMIN, MANAGER, AGENT)
        ADMIN   -> setOf(MANAGER, AGENT)
        MANAGER -> setOf(AGENT)
        else    -> emptySet()
    }

    /** Roles válidos como supervisor de este rol */
    val validSupervisorRoles: Set<UserRole> get() = when (this) {
        ADMIN   -> setOf(OWNER)
        MANAGER -> setOf(ADMIN, OWNER)
        AGENT,
        VIEWER  -> setOf(MANAGER, ADMIN, OWNER)
        else    -> emptySet()
    }

    companion object {
        /** Parsea un string del servidor al enum. Devuelve AGENT si desconocido. */
        fun from(key: String): UserRole =
            entries.firstOrNull { it.key == key } ?: AGENT

        /** Lista de todos los roles para UI (excepto GOD) */
        val selectableRoles: List<UserRole> = listOf(VIEWER, AGENT, MANAGER, ADMIN, OWNER)
    }
}
