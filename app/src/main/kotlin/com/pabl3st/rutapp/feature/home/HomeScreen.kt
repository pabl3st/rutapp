@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.home

import com.pabl3st.rutapp.core.ui.theme.RouteStatusTokens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.ui.theme.Spacing
import com.pabl3st.rutapp.data.local.entity.RouteEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.pabl3st.rutapp.core.UserRole
import com.pabl3st.rutapp.data.network.AccountUserDto

// ────────────────────────────────────────────────────────────
// HomeScreen — Lista de rutas de hoy con progreso
// ────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(
    onRouteClick:      (String) -> Unit   = {},
    onStopClick:       (String) -> Unit   = {},
    onNavigateToMapa:  () -> Unit          = {},
    onNavigateToTeam:  (() -> Unit)?       = null,  // solo manager/admin/owner/god
    vm: HomeViewModel = hiltViewModel(),
) {
    val ui    by vm.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val today  = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("es")))
        .replaceFirstChar { it.uppercase() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },


        modifier = Modifier.semantics { testTag = "home-screen" },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Hola, ${ui.userName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            today,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    // Botón "Mi equipo" para manager/admin/owner
                    if (onNavigateToTeam != null) {
                        IconButton(onClick = onNavigateToTeam) {
                            Icon(Icons.Default.Group, contentDescription = "Mi equipo")
                        }
                    }
                    // Mapa global — solo roles con rutas asignadas
                    if (!UserRole.from(ui.userRole).isViewer && !UserRole.from(ui.userRole).isGod) {
                        IconButton(onClick = onNavigateToMapa) {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = "Mapa del día",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (ui.isSyncing) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                    } else {
                        IconButton(onClick = vm::syncNow) {
                            Icon(Icons.Default.Sync, contentDescription = "Sincronizar")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            ui.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            ui.routes.isEmpty() -> EmptyRoutesMessage(
                Modifier.fillMaxSize().padding(padding)
            )

            else -> RouteListContent(
                ui           = ui,
                modifier     = Modifier.padding(padding),
                onRouteClick = onRouteClick,
                onStopClick  = onStopClick,
            )
        }
    }
}

// ── Contenido principal: stats + jornada(s) + lista de rutas ─
@Composable
private fun RouteListContent(
    ui: HomeUiState,
    modifier: Modifier = Modifier,
    onRouteClick: (String) -> Unit,
    onStopClick:  (String) -> Unit = {},
) {
    LazyColumn(
        modifier            = modifier.fillMaxSize(),
        contentPadding      = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // ── Stats del día ─────────────────────────────────────
        item {
            DayStatsRow(
                totalRoutes   = ui.routes.size,
                doneStops     = ui.doneStops,
                pendingStops  = ui.pendingStops,
            )
        }

        // ── Panel de equipo para manager ──────────────────────
        if (ui.isManager && ui.routes.isNotEmpty()) {
            // Owner y admin ven los totales de su ámbito (cuenta entera para owner,
            // subárbol descendente para admin). El dato ya viene filtrado de
            // RouteRepository según rol, así que solo decidimos si mostrar la card.
            if (UserRole.from(ui.userRole).level >= UserRole.ADMIN.level) {
                val isOwner = UserRole.from(ui.userRole).level >= UserRole.OWNER.level
                item { GlobalTotalsCard(routes = ui.routes, isOwner = isOwner) }
            }
            item {
                ManagerTeamSummary(
                    routes      = ui.routes,
                    teamMembers = ui.teamMembers,
                    callerRole  = UserRole.from(ui.userRole),
                )
            }
        }

        // ── JornadaBar — SOLO para agent (ejecuta rutas en campo) ──
        // manager/admin/owner/god supervisan pero no tienen jornada propia.
        // La jornada se gestiona via DaySessionEntity (idle/running/paused/done)
        // independientemente del route.status — no filtrar por "active".
        val isAgent = UserRole.from(ui.userRole) == UserRole.AGENT
        val jornadaRoutes = if (!isAgent) emptyList() else ui.routes.filter {
            it.route.status !in listOf("cancelled", "done")
        }
        if (jornadaRoutes.isNotEmpty()) {
            items(jornadaRoutes, key = { "jornada_${it.route.uid}" }) { rwp ->
                JornadaBar(routeUid = rwp.route.uid)
            }
        }

        // ── Sección rutas ─────────────────────────────────────
        item {
            Text(
                text  = "RUTAS DE HOY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.xs),
            )
        }

        items(ui.routes, key = { it.route.uid }) { rwp ->
            RouteProgressCard(
                rwp         = rwp,
                onClick     = { onRouteClick(rwp.route.uid) },
                onStopClick = onStopClick,
            )
        }

        item { Spacer(Modifier.height(Spacing.lg)) }
    }
}

// ── GlobalTotalsCard — totales del ámbito visible (owner: cuenta; admin: subárbol)
@Composable
private fun GlobalTotalsCard(routes: List<RouteWithProgress>, isOwner: Boolean) {
    val totalRoutes = routes.size
    val totalStops  = routes.sumOf { it.totalStops }
    val doneStops   = routes.sumOf { it.doneStops }
    val agents      = routes.map { it.route.userId }.distinct().size
    val progress    = if (totalStops > 0) doneStops.toFloat() / totalStops else 0f

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                if (isOwner) "Totales de la empresa — hoy"
                else         "Totales de tu equipo — hoy",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TotalMetric("$agents", "Agentes")
                TotalMetric("$totalRoutes", "Rutas")
                TotalMetric("$doneStops/$totalStops", "Visitadas")
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "${(progress * 100).toInt()}% completado",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TotalMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── ManagerTeamSummary ───────────────────────────────────────
/**
 * Bloque "Equipo hoy" agrupado por jerarquía:
 * - Para un manager (Ana): aparecen sus agentes como filas planas.
 * - Para un admin (Carlos): aparecen sus managers como cabecera bold,
 *   y bajo cada uno los agentes que cuelgan con flecha indentada.
 * - Para owner: ve admins → managers → agentes (3 niveles).
 *
 * Si no hay teamMembers cargados (fallo de red o agent), cae a la lista
 * plana antigua por agentId.
 */
@Composable
private fun ManagerTeamSummary(
    routes:      List<RouteWithProgress>,
    teamMembers: List<AccountUserDto>,
    callerRole:  UserRole,
) {
    if (routes.isEmpty()) return

    // Index por userId para resolver rápido
    val usersById = teamMembers.associateBy { it.userId }
    // Rutas agrupadas por su agente (route.userId)
    val routesByAgent: Map<Int, List<RouteWithProgress>> = routes.groupBy { it.route.userId }
    // Conteo de agentes únicos
    val agentsCount = routesByAgent.size

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                "Equipo hoy — $agentsCount agente${if (agentsCount != 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Construir el árbol de presentación según rol del caller:
            // - manager (Ana): un nivel, agentes directos.
            // - admin (Carlos): dos niveles, managers → sus agentes.
            // - owner (Laura): tres niveles, admins → managers → agentes.
            val tree = buildTeamTree(
                routesByAgent = routesByAgent,
                usersById     = usersById,
                callerRole    = callerRole,
            )

            if (tree.isEmpty()) {
                // Fallback: lista plana antigua (sin info de jerarquía)
                routesByAgent.forEach { (agentId, agentRoutes) ->
                    TeamRouteRow(
                        label  = usersById[agentId]?.displayName
                            ?: agentRoutes.firstOrNull()?.route?.name
                            ?: "Agente",
                        routes = agentRoutes,
                        indent = 0,
                    )
                }
            } else {
                tree.forEach { node -> RenderTeamNode(node) }
            }
        }
    }
}

/** Nodo del árbol jerárquico que se renderiza. */
private data class TeamNode(
    val displayName: String,
    val role:        String,           // "admin" | "manager" | "agent"
    val routes:      List<RouteWithProgress>, // rutas propias + recursivas de hijos
    val children:    List<TeamNode>    = emptyList(),
    val indent:      Int               = 0,    // 0/1/2 — nivel visual
)

/**
 * Construye el árbol según el rol del caller. Si no hay teamMembers o no se
 * detecta jerarquía, devuelve lista vacía y el caller usa el fallback plano.
 */
private fun buildTeamTree(
    routesByAgent: Map<Int, List<RouteWithProgress>>,
    usersById:     Map<Int, AccountUserDto>,
    callerRole:    UserRole,
): List<TeamNode> {
    if (usersById.isEmpty()) return emptyList()
    // userIds que tienen rutas hoy
    val agentsWithRoutes = routesByAgent.keys

    /** Helper recursivo: devuelve los nodos hoja (agentes con rutas) bajo userId. */
    fun gatherDescendantAgents(rootId: Int, depth: Int = 0): List<TeamNode> {
        if (depth > 5) return emptyList()  // anti-ciclos
        val direct = usersById.values.filter { it.managerId == rootId }
        return direct.flatMap { u ->
            when (u.role) {
                "agent" -> if (u.userId in agentsWithRoutes) {
                    val agentRoutes = routesByAgent[u.userId] ?: emptyList()
                    listOf(TeamNode(
                        displayName = u.displayName,
                        role        = "agent",
                        routes      = agentRoutes,
                        indent      = depth,
                    ))
                } else emptyList()
                else -> {
                    // manager o admin intermedio
                    val children = gatherDescendantAgents(u.userId, depth + 1)
                    if (children.isEmpty()) emptyList()
                    else listOf(TeamNode(
                        displayName = u.displayName,
                        role        = u.role,
                        routes      = children.flatMap { it.routes },
                        children    = children,
                        indent      = depth,
                    ))
                }
            }
        }
    }

    return when (callerRole) {
        UserRole.MANAGER -> {
            // El manager ve solo a sus agentes directos, sin niveles intermedios.
            val myId = usersById.values.firstOrNull { it.role == "manager" && it.userId in usersById.keys }
            // No es robusto identificar el caller sin más info; usar agentsWithRoutes directos.
            agentsWithRoutes.mapNotNull { id ->
                val u = usersById[id] ?: return@mapNotNull null
                TeamNode(
                    displayName = u.displayName,
                    role        = u.role,
                    routes      = routesByAgent[id] ?: emptyList(),
                    indent      = 0,
                )
            }
        }
        UserRole.ADMIN -> {
            // Admin: cabecera por manager, agentes indentados bajo cada manager.
            val managers = usersById.values.filter { it.role == "manager" }
            managers.mapNotNull { mgr ->
                val agents = gatherDescendantAgents(mgr.userId, 1)
                if (agents.isEmpty()) null
                else TeamNode(
                    displayName = mgr.displayName,
                    role        = "manager",
                    routes      = agents.flatMap { it.routes },
                    children    = agents,
                    indent      = 0,
                )
            }
        }
        UserRole.OWNER, UserRole.GOD -> {
            // Owner: árbol completo Admin → Manager → Agente
            val admins = usersById.values.filter { it.role == "admin" }
            admins.mapNotNull { adm ->
                val managersUnder = gatherDescendantAgents(adm.userId, 1)
                if (managersUnder.isEmpty()) null
                else TeamNode(
                    displayName = adm.displayName,
                    role        = "admin",
                    routes      = managersUnder.flatMap { it.routes },
                    children    = managersUnder,
                    indent      = 0,
                )
            }
        }
        else -> emptyList()
    }
}

@Composable
private fun RenderTeamNode(node: TeamNode) {
    // Cabecera del nodo: nombre + progreso agregado
    val total = node.routes.sumOf { it.totalStops }
    val done  = node.routes.sumOf { it.doneStops }
    val isLeafAgent = node.role == "agent"
    Row(
        Modifier.fillMaxWidth().padding(start = (node.indent * 16).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (node.indent > 0) {
            Text("↳ ", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f)) {
            Text(
                node.displayName,
                style    = if (isLeafAgent) MaterialTheme.typography.bodySmall
                           else            MaterialTheme.typography.titleSmall,
                fontWeight = if (isLeafAgent) androidx.compose.ui.text.font.FontWeight.Normal
                             else            androidx.compose.ui.text.font.FontWeight.SemiBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (isLeafAgent) {
                val routeNames = node.routes.joinToString(", ") { it.route.name }
                if (routeNames.isNotBlank()) {
                    Text(
                        routeNames,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                val progress = if (total > 0) done.toFloat() / total else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                )
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            "$done/$total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    // Renderizar hijos recursivamente
    node.children.forEach { child -> RenderTeamNode(child) }
}

@Composable
private fun TeamRouteRow(
    label:  String,
    routes: List<RouteWithProgress>,
    indent: Int,
) {
    val total = routes.sumOf { it.totalStops }
    val done  = routes.sumOf { it.doneStops }
    val progress = if (total > 0) done.toFloat() / total else 0f
    Row(
        Modifier.fillMaxWidth().padding(start = (indent * 16).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style    = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            "$done/$total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// ── Stats del día: 3 cajas ────────────────────────────────────
@Composable
private fun DayStatsRow(
    totalRoutes:  Int,
    doneStops:    Int,
    pendingStops: Int,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        StatBox(
            value    = totalRoutes.toString(),
            label    = "Rutas hoy",
            modifier = Modifier.weight(1f),
        )
        StatBox(
            value    = doneStops.toString(),
            label    = "Visitadas",
            modifier = Modifier.weight(1f),
            color    = MaterialTheme.colorScheme.primary,
        )
        StatBox(
            value    = pendingStops.toString(),
            label    = "Pendientes",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatBox(
    value:    String,
    label:    String,
    modifier: Modifier = Modifier,
    color:    androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = modifier,
        shape    = MaterialTheme.shapes.small,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier              = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text  = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Tarjeta de ruta con barra de progreso ─────────────────────
@Composable
private fun RouteProgressCard(
    rwp:     RouteWithProgress,
    onClick: () -> Unit,
    onStopClick: ((String) -> Unit)? = null,
) {
    val route       = rwp.route
    var expanded by remember(route.uid) { mutableStateOf(false) }
    val progressAnim by animateFloatAsState(
        targetValue  = rwp.progress,
        animationSpec = tween(durationMillis = 600),
        label        = "progress_${route.uid}",
    )

    // Color según estado — tokens centralizados
    val _st = RouteStatusTokens.of(route.status)
    val statusLabel = _st.label
    val statusColor = _st.color

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {

            // ── Cabecera: icono + nombre + chip estado ────────
            Row(
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Icono con fondo según estado
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (route.status == "done")
                                MaterialTheme.colorScheme.tertiaryContainer
                            else
                                MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = Icons.Default.Route,
                        contentDescription = null,
                        modifier           = Modifier.size(20.dp),
                        tint               = if (route.status == "done")
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                // Nombre y fecha
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text      = route.name,
                        style     = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                    )
                    // Calcular label de fecha:
                    // si hoy está en scheduledDates → "Hoy"
                    // si dateAssigned == hoy → "Hoy"
                    // si dateAssigned == "1970-01-01" o vacío → "Sin fecha"
                    // si hoy está en scheduledDates pero dateAssigned es pasada → "Hoy (+ fechas)"
                    val todayStr = java.time.LocalDate.now().toString()
                    val dateLabel = when {
                        route.dateAssigned == todayStr -> "Hoy"
                        route.scheduledDates?.contains(todayStr) == true -> "Hoy"
                        route.dateAssigned.isBlank() || route.dateAssigned == "1970-01-01" -> "Sin fecha"
                        else -> {
                            runCatching {
                                val fmt = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
                                val d = java.time.LocalDate.parse(route.dateAssigned, fmt)
                                val tomorrow = java.time.LocalDate.now().plusDays(1)
                                val yesterday = java.time.LocalDate.now().minusDays(1)
                                when (d) {
                                    java.time.LocalDate.now() -> "Hoy"
                                    tomorrow  -> "Mañana"
                                    yesterday -> "Ayer"
                                    else -> d.format(java.time.format.DateTimeFormatter
                                        .ofPattern("d MMM yyyy", java.util.Locale("es")))
                                }
                            }.getOrDefault(route.dateAssigned)
                        }
                    }
                    Text(
                        text  = dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Chip de estado
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = statusColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        text  = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 3.dp),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // ── Siguiente parada pendiente ───────────────────
            rwp.nextPendingStop?.let { stop ->
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier              = Modifier.padding(bottom = 4.dp),
                ) {
                    Icon(Icons.Default.NavigateNext, null,
                        Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(stop.name,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.primary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false))
                    stop.address?.takeIf { it.isNotBlank() }?.let { addr ->
                        Text("· $addr",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f))
                    }
                }
            }

            // ── Progreso ──────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Barra lineal de progreso
                LinearProgressIndicator(
                    progress       = { progressAnim },
                    modifier       = Modifier.weight(1f).height(6.dp).clip(MaterialTheme.shapes.extraSmall),
                    strokeCap      = StrokeCap.Round,
                    color          = if (route.status == "done")
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.primary,
                    trackColor     = MaterialTheme.colorScheme.surfaceVariant,
                )

                // Texto paradas
                Text(
                    text  = "${rwp.doneStops}/${rwp.totalStops}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Chevron de expansión (solo si hay PDVs que mostrar)
                if (rwp.stops.isNotEmpty()) {
                    IconButton(
                        onClick  = { expanded = !expanded },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector        = if (expanded) Icons.Default.KeyboardArrowUp
                                                 else           Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Colapsar PDVs" else "Ver PDVs",
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ── Desglose de PDVs (colapsable) ─────────────────
            AnimatedVisibility(visible = expanded) {
                Column(
                    Modifier.fillMaxWidth().padding(top = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 4.dp),
                        color    = MaterialTheme.colorScheme.outlineVariant,
                    )
                    rwp.stops.forEach { stop ->
                        StopRowMini(
                            stop    = stop,
                            onClick = onStopClick?.let { { it(stop.uid) } },
                        )
                    }
                }
            }

            // Notas opcionales
            route.notes?.let { notes ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = notes,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── Estado vacío ──────────────────────────────────────────────
@Composable
private fun EmptyRoutesMessage(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier         = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Route,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                "Sin rutas para hoy",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Las rutas asignadas aparecerán aquí",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── StopRowMini ───────────────────────────────────────────────
// Fila compacta dentro del desglose colapsable de la card de ruta.
// Muestra: chip de status + orderIndex + nombre + externalId + dirección truncada.
@Composable
private fun StopRowMini(
    stop:    com.pabl3st.rutapp.data.local.entity.StopEntity,
    onClick: (() -> Unit)? = null,
) {
    val statusColor = when (stop.status) {
        "done"     -> MaterialTheme.colorScheme.tertiary
        "visiting" -> MaterialTheme.colorScheme.primary
        "skipped"  -> MaterialTheme.colorScheme.error
        else       -> MaterialTheme.colorScheme.outline
    }
    val rowModifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp)
    } else {
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
    }
    Row(
        modifier            = rowModifier,
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Punto de status
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor),
        )
        // Índice
        Text(
            "${stop.orderIndex + 1}.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 18.dp),
        )
        // Nombre + externalId
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stop.name,
                    style    = MaterialTheme.typography.bodySmall,
                    fontWeight = if (stop.status == "done") FontWeight.Normal else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                stop.externalId?.takeIf { it.isNotBlank() }?.let { ext ->
                    Text(
                        ext,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            stop.address?.takeIf { it.isNotBlank() }?.let { addr ->
                Text(
                    addr,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
