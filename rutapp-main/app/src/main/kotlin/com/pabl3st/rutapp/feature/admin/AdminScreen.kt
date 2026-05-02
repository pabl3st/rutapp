@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.ui.theme.Spacing

@Composable
fun AdminScreen(
    vm: AdminViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Admin") })
        }
    ) { padding ->
        if (ui.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {

            // ── Perfil de sesión ──────────────────────────────
            item {
                Text("Sesión activa", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Spacing.sm))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        AdminInfoRow(Icons.Default.Person, "Nombre", ui.userName)
                        AdminInfoRow(Icons.Default.Email, "Email", ui.userEmail)
                        AdminInfoRow(Icons.Default.Badge, "Rol", ui.userRole.uppercase())
                        AdminInfoRow(Icons.Default.Business, "Cuenta", ui.accountName)
                    }
                }
            }

            // ── Estadísticas globales ─────────────────────────
            item {
                Text("Estadísticas", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    AdminStatCard(
                        modifier = Modifier.weight(1f),
                        icon  = Icons.Default.Route,
                        value = "${ui.totalRoutes}",
                        label = "Rutas totales",
                    )
                    AdminStatCard(
                        modifier = Modifier.weight(1f),
                        icon  = Icons.Default.Place,
                        value = "${ui.totalStops}",
                        label = "Stops totales",
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = if (ui.pendingSync > 0)
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                    else CardDefaults.cardColors(),
                ) {
                    Row(
                        modifier          = Modifier.padding(Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (ui.pendingSync > 0) Icons.Default.CloudOff else Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = if (ui.pendingSync > 0) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(Spacing.md))
                        Text(
                            if (ui.pendingSync > 0) "${ui.pendingSync} operaciones pendientes de sync"
                            else "Sincronización al día",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // ── Info del build ────────────────────────────────
            item {
                Text("Aplicación", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Spacing.sm))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        AdminInfoRow(Icons.Default.Android, "Versión", "1.0.0-s10")
                        AdminInfoRow(Icons.Default.Storage, "BD Local", "Room v6")
                        AdminInfoRow(Icons.Default.Cloud, "API", "v1.1.0")
                        AdminInfoRow(Icons.Default.Map, "Mapa", "MapLibre 11.5.1")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.sm))
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AdminStatCard(
    modifier: Modifier = Modifier,
    icon:     ImageVector,
    value:    String,
    label:    String,
) {
    Card(modifier = modifier) {
        Column(
            modifier            = Modifier.padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(Spacing.xs))
            Text(value, style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
