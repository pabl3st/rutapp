@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.ui.theme.Spacing

@Composable
fun PerfilScreen(
    onLoggedOut: () -> Unit,
    onBack: () -> Unit,
    vm: PerfilViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // ── Avatar + nombre ───────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape  = MaterialTheme.shapes.extraLarge,
                        color  = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text  = ui.displayName.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(Modifier.width(Spacing.lg))
                    Column {
                        Text(ui.displayName, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text("@${ui.username}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        RolChip(role = ui.role)
                    }
                }
            }

            // ── Información de cuenta ─────────────────────────
            SectionTitle("Cuenta")
            InfoCard {
                InfoRow(icon = Icons.Default.Email,  label = "Email",   value = ui.email)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow(icon = Icons.Default.Business, label = "Empresa", value = ui.accountName)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow(icon = Icons.Default.WorkspacePremium, label = "Plan",
                    value = ui.plan.replaceFirstChar { it.uppercase() })
            }

            // ── Ajustes ───────────────────────────────────────
            SectionTitle("Ajustes")
            InfoCard {
                InfoRowAction(
                    icon    = Icons.Default.Palette,
                    label   = "Apariencia",
                    detail  = "Dark mode",
                    onClick = { /* S08 */ },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRowAction(
                    icon    = Icons.Default.Notifications,
                    label   = "Notificaciones",
                    detail  = "Activadas",
                    onClick = { /* S08 */ },
                )
            }

            // ── Sesión ────────────────────────────────────────
            SectionTitle("Sesión")
            Button(
                onClick = vm::onLogoutClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor   = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(Icons.Default.Logout, contentDescription = null,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text("Cerrar sesión")
            }

            // ── Versión ───────────────────────────────────────
            Text(
                text  = "RutasApp S04",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = Spacing.sm),
            )
        }
    }

    // ── Diálogo confirmar logout ───────────────────────────────
    if (ui.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = vm::onLogoutDismiss,
            icon  = { Icon(Icons.Default.Logout, contentDescription = null) },
            title = { Text("Cerrar sesión") },
            text  = { Text("¿Seguro que quieres cerrar sesión? Tendrás que volver a iniciarla.") },
            confirmButton = {
                Button(
                    onClick = { vm.onLogoutConfirm(onLoggedOut) },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = vm::onLogoutDismiss) { Text("Cancelar") }
            },
        )
    }

    if (ui.isLoggingOut) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text  = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = Spacing.sm),
    )
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp), content = content)
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun InfoRowAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    detail: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(detail, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RolChip(role: String) {
    val (color, label) = when (role) {
        "owner"   -> MaterialTheme.colorScheme.primary to "Propietario"
        "admin"   -> MaterialTheme.colorScheme.secondary to "Admin"
        "manager" -> MaterialTheme.colorScheme.tertiary to "Manager"
        "agent"   -> MaterialTheme.colorScheme.onSurfaceVariant to "Agente"
        else      -> MaterialTheme.colorScheme.onSurfaceVariant to role
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(labelColor = color),
    )
}
