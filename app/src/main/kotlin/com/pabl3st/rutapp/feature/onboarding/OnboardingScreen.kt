@file:OptIn(ExperimentalAnimationApi::class)
package com.pabl3st.rutapp.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pabl3st.rutapp.core.permission.AppPermission

// Paleta Field Pro Dark — igual que AuthScreens
private val CyanGlow   = Color(0xFF22D3EE)
private val DarkBg     = Color(0xFF09090B)
private val DarkCard   = Color(0xFF18181B)
private val DarkBorder = Color(0xFF27272A)
private val TextPrim   = Color(0xFFFAFAFA)
private val TextMuted  = Color(0xFF71717A)
private val TextSub    = Color(0xFFD4D4D8)

@Composable
fun OnboardingScreen(
    onComplete: (isLoggedIn: Boolean) -> Unit,
    vm: OnboardingViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    // Lanzador de permisos del sistema — se recrea cuando cambia el permiso actual
    val currentPerm = ui.currentPermission
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        currentPerm?.let { vm.onPermissionResult(it, granted) }
    }

    LaunchedEffect(ui.step) {
        if (ui.step == OnboardingStep.COMPLETE) {
            onComplete(ui.isAlreadyLoggedIn)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .drawBehind {
                val spacing = 28.dp.toPx()
                val r = 1.2.dp.toPx()
                val dotColor = CyanGlow.copy(alpha = 0.06f)
                var x = spacing
                while (x < size.width) {
                    var y = spacing
                    while (y < size.height) {
                        drawCircle(dotColor, r, Offset(x, y))
                        y += spacing
                    }
                    x += spacing
                }
            }
            .systemBarsPadding(),
    ) {
        AnimatedContent(
            targetState    = ui.step,
            transitionSpec = {
                fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 10 } togetherWith
                fadeOut(tween(250))
            },
            label = "onboarding_step",
        ) { step ->
            when (step) {
                OnboardingStep.SPLASH -> SplashContent()
                OnboardingStep.PERMISSION_PROMPT -> {
                    ui.currentPermission?.let { perm ->
                        PermissionPromptContent(
                            permission   = perm,
                            grantedCount = ui.grantedCount,
                            totalCount   = ui.totalCount,
                            onAllow      = {
                                permLauncher.launch(perm.manifestPermissions.toTypedArray())
                            },
                            onSkip       = if (!perm.isCritical) vm::skipCurrentPermission else null,
                        )
                    }
                }
                OnboardingStep.COMPLETE -> {
                    // Loading mientras navega
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CyanGlow, strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// SPLASH
// ══════════════════════════════════════════════════════════════
@Composable
private fun SplashContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logo_scale",
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue  = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    Column(
        modifier            = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Logo con glow animado
        Box(contentAlignment = Alignment.Center) {
            // Halo exterior
            Box(
                modifier = Modifier
                    .size((72 * scale).dp)
                    .clip(RoundedCornerShape((72 * 0.28f * scale).dp))
                    .background(CyanGlow.copy(alpha = glowAlpha)),
            )
            // Icono principal
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CyanGlow.copy(alpha = 0.12f))
                    .border(1.dp, CyanGlow.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Route, null,
                    Modifier.size(32.dp), tint = CyanGlow)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("RutasApp", color = TextPrim, fontSize = 30.sp,
            fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
        Spacer(Modifier.height(4.dp))
        Text("Gestión de rutas de campo", color = TextMuted, fontSize = 13.sp)

        Spacer(Modifier.height(48.dp))

        CircularProgressIndicator(
            color       = CyanGlow.copy(alpha = 0.6f),
            strokeWidth = 1.5.dp,
            modifier    = Modifier.size(18.dp),
        )
    }
}

// ══════════════════════════════════════════════════════════════
// PERMISSION PROMPT — uno a uno
// ══════════════════════════════════════════════════════════════
@Composable
private fun PermissionPromptContent(
    permission:   AppPermission,
    grantedCount: Int,
    totalCount:   Int,
    onAllow:      () -> Unit,
    onSkip:       (() -> Unit)?,
) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Indicador de progreso (ej: 1 de 2)
        if (totalCount > 1) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(totalCount) { i ->
                    val active = i <= grantedCount
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (active) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) CyanGlow else DarkBorder
                            ),
                    )
                }
            }
            Spacer(Modifier.height(40.dp))
        }

        // Icono del permiso
        val icon = permissionIcon(permission)
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    if (permission.isCritical) CyanGlow.copy(0.12f)
                    else DarkCard
                )
                .border(
                    1.dp,
                    if (permission.isCritical) CyanGlow.copy(0.4f) else DarkBorder,
                    RoundedCornerShape(22.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(38.dp),
                tint = if (permission.isCritical) CyanGlow else TextSub)
        }

        Spacer(Modifier.height(28.dp))

        // Título y explicación
        Text(
            permission.title,
            color      = TextPrim,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            letterSpacing = (-0.3).sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            permission.rationale,
            color     = TextMuted,
            fontSize  = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
        )

        if (permission.isCritical) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CyanGlow.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Info, null, Modifier.size(14.dp),
                        tint = CyanGlow.copy(0.7f))
                    Spacer(Modifier.width(8.dp))
                    Text("Necesario para el funcionamiento principal de la app",
                        color = CyanGlow.copy(0.7f), fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        // Botón principal
        Button(
            onClick  = onAllow,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = CyanGlow,
                contentColor   = Color(0xFF09090B),
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp),
        ) {
            Text("Permitir", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        // Botón omitir — solo para permisos no críticos
        if (onSkip != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick  = onSkip,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Ahora no", color = TextMuted, fontSize = 13.sp)
            }
        } else {
            Spacer(Modifier.height(12.dp))
            Text(
                "Este permiso es necesario para continuar",
                color    = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Iconos por permiso ────────────────────────────────────────
@Composable
private fun permissionIcon(permission: AppPermission): ImageVector = when (permission) {
    is AppPermission.Notifications      -> Icons.Outlined.Notifications
    is AppPermission.LocationFine       -> Icons.Outlined.LocationOn
    is AppPermission.LocationCoarse     -> Icons.Outlined.MyLocation
    is AppPermission.LocationBackground -> Icons.Outlined.Route
    is AppPermission.Camera             -> Icons.Outlined.Camera
    is AppPermission.Storage            -> Icons.Outlined.FolderOpen
    is AppPermission.MediaImages        -> Icons.Outlined.PermMedia
}

private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
