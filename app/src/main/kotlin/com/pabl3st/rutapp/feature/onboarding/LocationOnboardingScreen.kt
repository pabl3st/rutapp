@file:OptIn(ExperimentalMaterial3Api::class)
package com.pabl3st.rutapp.feature.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import com.pabl3st.rutapp.core.location.LocationPermissionState
import com.pabl3st.rutapp.core.location.locationPermissionState
import com.pabl3st.rutapp.core.ui.theme.Spacing

private enum class LocOnbStage { LOCATION, EXTRAS }

/**
 * Onboarding de permisos de ubicacion POST-LOGIN.
 *
 * Etapa 1 (LOCATION): permiso de ubicacion en primer plano (fine/coarse), con
 *   estados de denegado / bloqueado y acceso a Ajustes.
 * Etapa 2 (EXTRAS): solo para agent tras conceder foreground - ofrece ubicacion
 *   en segundo plano y exencion de optimizacion de bateria, ambos necesarios
 *   para que el ForegroundService de GPS sobreviva a la jornada. Son opcionales:
 *   el boton Continuar siempre deja pasar.
 */
@Composable
fun LocationOnboardingScreen(
    onPermissionGranted: () -> Unit,
    onSkip: () -> Unit,                 // para roles no-agent que no necesitan GPS
    isAgentRole: Boolean = true,        // agents no pueden saltar - necesitan GPS para km/visitas
    vm: LocationOnboardingViewModel = hiltViewModel(),
) {
    val context       = LocalContext.current
    var stage         by remember { mutableStateOf(LocOnbStage.LOCATION) }
    var permState     by remember { mutableStateOf(context.locationPermissionState()) }
    var showDenied    by remember { mutableStateOf(false) }
    var showPermanent by remember { mutableStateOf(false) }
    var visible       by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    // Al conceder foreground: encadenar EXTRAS (agent) o salir directamente.
    LaunchedEffect(permState, stage) {
        if (permState == LocationPermissionState.Granted && stage == LocOnbStage.LOCATION) {
            if (isAgentRole && vm.hasPendingExtras()) {
                stage = LocOnbStage.EXTRAS
            } else {
                onPermissionGranted()
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        permState = context.locationPermissionState()
        when {
            // permState actualizado; el LaunchedEffect encadena EXTRAS o sale.
            granted -> Unit
            (context as? androidx.activity.ComponentActivity)
                ?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) == false -> {
                showPermanent = true
            }
            else -> showDenied = true
        }
    }

    Scaffold { padding ->
        when (stage) {
            LocOnbStage.LOCATION -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                Spacer(Modifier.height(Spacing.xl))

                // Icono principal animado
                AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically { -40 }) {
                    Surface(
                        shape  = MaterialTheme.shapes.extraLarge,
                        color  = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(96.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(52.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = visible, enter = fadeIn()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(
                            "Activa la ubicacion",
                            style     = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "Para registrar tus visitas correctamente necesitamos saber donde estas.",
                            style     = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Tarjetas explicativas de beneficios
                AnimatedVisibility(visible = visible, enter = fadeIn()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OnboardingBenefit(
                            icon  = Icons.Default.Route,
                            title = "Km recorridos",
                            desc  = "Calculamos la distancia real de tu jornada usando tu posicion GPS.",
                        )
                        OnboardingBenefit(
                            icon  = Icons.Default.CheckCircle,
                            title = "Registro de visitas",
                            desc  = "Cada visita queda geolocalizada para que el supervisor vea tu actividad.",
                        )
                        OnboardingBenefit(
                            icon  = Icons.Default.Navigation,
                            title = "Navegacion a PDVs",
                            desc  = "Te guiamos desde tu posicion actual hasta el siguiente punto de venta.",
                        )
                        OnboardingBenefit(
                            icon  = Icons.Default.Security,
                            title = "Tu privacidad",
                            desc  = "La ubicacion solo se usa durante la jornada activa. Nunca fuera de la app.",
                        )
                    }
                }

                // Aviso si se ha denegado
                if (showDenied) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Row(Modifier.padding(Spacing.md), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Has denegado el permiso. Pulsa 'Permitir ubicacion' para intentarlo de nuevo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                // Aviso si esta permanentemente denegado
                if (showPermanent) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Text("El permiso esta bloqueado. Ve a Ajustes del sistema para activarlo manualmente.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                    )
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.Settings, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Abrir Ajustes del sistema")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.sm))

                // Boton principal
                Button(
                    onClick = {
                        if (showPermanent) {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            )
                        } else {
                            launcher.launch(arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Default.LocationOn, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (showPermanent) "Ir a Ajustes" else "Permitir ubicacion",
                        style = MaterialTheme.typography.labelLarge)
                }

                // Boton saltar - solo para roles no-agent
                if (!isAgentRole) {
                    TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                        Text("Continuar sin ubicacion",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text(
                        "La ubicacion es necesaria para registrar km y visitas geolocalizadas.",
                        style     = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(Spacing.xl))
            }

            LocOnbStage.EXTRAS -> {
                var bgAsked  by remember { mutableStateOf(false) }
                var batAsked by remember { mutableStateOf(false) }
                val showBg   = vm.backgroundApplies() && !vm.isBackgroundGranted()
                val showBat  = !vm.isBatteryIgnoring()

                val bgLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { _ ->
                    vm.markBackgroundShown()
                    bgAsked = true
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    Spacer(Modifier.height(Spacing.xl))

                    Surface(
                        shape    = MaterialTheme.shapes.extraLarge,
                        color    = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(96.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                modifier = Modifier.size(52.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(
                            "Que no se corte tu jornada",
                            style     = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "Dos ajustes rapidos para que el GPS siga registrando aunque minimices la app.",
                            style     = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (showBg) {
                        ExtraPermissionCard(
                            icon        = Icons.Default.Route,
                            title       = "Ubicacion en segundo plano",
                            desc        = "Permite registrar tu recorrido con la app minimizada durante la jornada.",
                            actionLabel = if (bgAsked) "Solicitado" else "Permitir",
                            enabled     = !bgAsked,
                            onAction    = { bgLauncher.launch(vm.backgroundManifestPermissions) },
                        )
                    }

                    if (showBat) {
                        ExtraPermissionCard(
                            icon        = Icons.Default.Settings,
                            title       = "Sin optimizacion de bateria",
                            desc        = "Evita que el sistema detenga el seguimiento GPS para ahorrar energia.",
                            actionLabel = if (batAsked) "Abierto" else "Desactivar",
                            enabled     = true,
                            onAction    = {
                                context.startActivity(vm.batteryRequestIntent())
                                vm.markBatteryShown()
                                batAsked = true
                            },
                        )
                    }

                    Spacer(Modifier.height(Spacing.sm))

                    Button(
                        onClick  = { onPermissionGranted() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Continuar", style = MaterialTheme.typography.labelLarge)
                    }

                    Text(
                        "Puedes cambiar estos ajustes despues desde Ajustes del sistema.",
                        style     = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(Spacing.xl))
                }
            }
        }
    }
}

@Composable
private fun ExtraPermissionCard(
    icon:        ImageVector,
    title:       String,
    desc:        String,
    actionLabel: String,
    enabled:     Boolean,
    onAction:    () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier            = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Surface(
                    shape    = MaterialTheme.shapes.small,
                    color    = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.labelLarge)
                    Text(desc, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedButton(
                onClick  = onAction,
                enabled  = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun OnboardingBenefit(
    icon:  ImageVector,
    title: String,
    desc:  String,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier            = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment   = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(desc, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
