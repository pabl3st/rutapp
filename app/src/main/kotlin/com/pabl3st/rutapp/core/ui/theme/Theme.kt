package com.pabl3st.rutapp.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.graphics.vector.ImageVector
import com.pabl3st.rutapp.R

// ── Field Pro Dark — Paleta ───────────────────────────────────
// Inspiración: Linear, Raycast, RepMove
// Dark-first: fondos casi negro con acentos cian eléctrico
object RutasColors {

    // ── Acento principal: Cian eléctrico ─────────────────────
    val Cyan400        = Color(0xFF22D3EE)   // acento principal dark
    val Cyan500        = Color(0xFF06B6D4)   // acento hover dark
    val Cyan900        = Color(0xFF083344)   // container dark
    val CyanLight      = Color(0xFF0E7490)   // acento principal light
    val CyanLightBg    = Color(0xFFECFEFF)   // container light

    // ── Éxito: Verde esmeralda ────────────────────────────────
    val Emerald400     = Color(0xFF34D399)
    val Emerald500     = Color(0xFF10B981)
    val Emerald900     = Color(0xFF064E3B)
    val EmeraldLight   = Color(0xFF059669)
    val EmeraldLightBg = Color(0xFFECFDF5)

    // ── Advertencia: Ámbar ────────────────────────────────────
    val Amber400       = Color(0xFFFBBF24)
    val Amber900       = Color(0xFF451A03)
    val AmberLight     = Color(0xFFD97706)
    val AmberLightBg   = Color(0xFFFFFBEB)

    // ── Error: Rojo ───────────────────────────────────────────
    val Red400         = Color(0xFFF87171)
    val Red900         = Color(0xFF450A0A)
    val RedLight       = Color(0xFFDC2626)
    val RedLightBg     = Color(0xFFFEF2F2)

    // ── Fondos Dark (escala de profundidad) ───────────────────
    val Dark950        = Color(0xFF09090B)   // background — casi negro puro
    val Dark900        = Color(0xFF0D0D10)   // surface level 0
    val Dark800        = Color(0xFF18181B)   // surface level 1 (cards)
    val Dark700        = Color(0xFF27272A)   // surface level 2 (elevated)
    val Dark600        = Color(0xFF3F3F46)   // border / divider
    val Dark500        = Color(0xFF52525B)   // outline subtle

    // ── Texto Dark ────────────────────────────────────────────
    val TextDark100    = Color(0xFFFAFAFA)   // primary — casi blanco puro
    val TextDark200    = Color(0xFFD4D4D8)   // secondary
    val TextDark400    = Color(0xFF71717A)   // muted / placeholder

    // ── Fondos Light (slate frío) ─────────────────────────────
    val Light50        = Color(0xFFF8FAFC)   // background
    val Light100       = Color(0xFFF1F5F9)   // surface variant
    val Light200       = Color(0xFFE2E8F0)   // border
    val LightCard      = Color(0xFFFFFFFF)

    // ── Texto Light ───────────────────────────────────────────
    val TextLight900   = Color(0xFF0F172A)   // primary
    val TextLight600   = Color(0xFF475569)   // secondary
    val TextLight400   = Color(0xFF94A3B8)   // muted

    // ── Compatibilidad con código existente ───────────────────
    val Primary        = CyanLight
    val PrimaryLight   = CyanLightBg
    val PrimaryDark    = Color(0xFF0E7490)
    val Success        = Emerald500
    val SuccessLight   = EmeraldLightBg
    val Danger         = RedLight
    val DangerLight    = RedLightBg
    val Warning        = AmberLight
    val WarningLight   = AmberLightBg
    val Info           = Cyan500
    val BgPage         = Light50
    val BgSurface      = Light100
    val BgCard         = LightCard
    val DarkBg         = Dark950
    val DarkSurface    = Dark800
    val DarkCard       = Dark800
    val DarkHover      = Dark700
    val TextPrimary       = TextLight900
    val TextSecondary     = TextLight600
    val TextSubtle        = TextLight400
    val TextMuted         = TextLight400
    val TextPrimaryDark   = TextDark100
    val TextSecondaryDark = TextDark200
    val Border         = Light200
    val BorderInput    = Light200
}

object RutasShapes {
    val xs = 4.dp
    val sm = 6.dp
    val md = 10.dp
    val lg = 14.dp
}

// ── Tipografía — DM Sans ──────────────────────────────────────
val DmSans = FontFamily(
    Font(R.font.dm_sans,          FontWeight.Normal),
    Font(R.font.dm_sans_medium,   FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
    Font(R.font.dm_sans_bold,     FontWeight.Bold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
)

// ── Dark color scheme — Field Pro Dark ───────────────────────
private val DarkColors = darkColorScheme(
    // Cian como primary
    primary             = RutasColors.Cyan400,
    onPrimary           = RutasColors.Dark950,
    primaryContainer    = RutasColors.Cyan900,
    onPrimaryContainer  = RutasColors.Cyan400,

    // Esmeralda como secondary (éxito, done, synced)
    secondary           = RutasColors.Emerald400,
    onSecondary         = RutasColors.Dark950,
    secondaryContainer  = RutasColors.Emerald900,
    onSecondaryContainer= RutasColors.Emerald400,

    // Ámbar como tertiary (pending, warning)
    tertiary            = RutasColors.Amber400,
    onTertiary          = RutasColors.Dark950,
    tertiaryContainer   = RutasColors.Amber900,
    onTertiaryContainer = RutasColors.Amber400,

    // Error
    error               = RutasColors.Red400,
    onError             = RutasColors.Dark950,
    errorContainer      = RutasColors.Red900,
    onErrorContainer    = RutasColors.Red400,

    // Fondos en escala de profundidad
    background          = RutasColors.Dark950,
    onBackground        = RutasColors.TextDark100,
    surface             = RutasColors.Dark800,
    onSurface           = RutasColors.TextDark100,
    surfaceVariant      = RutasColors.Dark700,
    onSurfaceVariant    = RutasColors.TextDark200,

    // Bordes y overlays
    outline             = RutasColors.Dark600,
    outlineVariant      = RutasColors.Dark500,
    scrim               = Color(0xCC000000),

    // Surface niveles adicionales
    surfaceBright       = RutasColors.Dark700,
    surfaceDim          = RutasColors.Dark900,
    surfaceContainer    = RutasColors.Dark800,
    surfaceContainerHigh= RutasColors.Dark700,
    surfaceContainerHighest = RutasColors.Dark600,
    surfaceContainerLow = RutasColors.Dark900,
    surfaceContainerLowest = RutasColors.Dark950,
)

// ── Light color scheme — Sales Clarity ───────────────────────
private val LightColors = lightColorScheme(
    primary             = RutasColors.CyanLight,
    onPrimary           = Color.White,
    primaryContainer    = RutasColors.CyanLightBg,
    onPrimaryContainer  = RutasColors.CyanLight,

    secondary           = RutasColors.EmeraldLight,
    onSecondary         = Color.White,
    secondaryContainer  = RutasColors.EmeraldLightBg,
    onSecondaryContainer= RutasColors.EmeraldLight,

    tertiary            = RutasColors.AmberLight,
    onTertiary          = Color.White,
    tertiaryContainer   = RutasColors.AmberLightBg,
    onTertiaryContainer = RutasColors.AmberLight,

    error               = RutasColors.RedLight,
    onError             = Color.White,
    errorContainer      = RutasColors.RedLightBg,
    onErrorContainer    = RutasColors.RedLight,

    background          = RutasColors.Light50,
    onBackground        = RutasColors.TextLight900,
    surface             = RutasColors.LightCard,
    onSurface           = RutasColors.TextLight900,
    surfaceVariant      = RutasColors.Light100,
    onSurfaceVariant    = RutasColors.TextLight600,

    outline             = RutasColors.Light200,
    outlineVariant      = RutasColors.Light200,
    scrim               = Color(0x80000000),
)

// ── Tipografía refinada ───────────────────────────────────────
// Letter-spacing negativo en headlines = más impacto y modernidad
private val AppTypography = Typography(
    headlineLarge  = TextStyle(
        fontSize = 24.sp, fontWeight = FontWeight.Bold,
        lineHeight = 30.sp, letterSpacing = (-0.5).sp, fontFamily = DmSans
    ),
    headlineMedium = TextStyle(
        fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
        lineHeight = 26.sp, letterSpacing = (-0.3).sp, fontFamily = DmSans
    ),
    headlineSmall  = TextStyle(
        fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp, letterSpacing = (-0.2).sp, fontFamily = DmSans
    ),
    titleLarge     = TextStyle(
        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp, letterSpacing = (-0.1).sp, fontFamily = DmSans
    ),
    titleMedium    = TextStyle(
        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        lineHeight = 18.sp, fontFamily = DmSans
    ),
    titleSmall     = TextStyle(
        fontSize = 13.sp, fontWeight = FontWeight.Medium,
        lineHeight = 18.sp, fontFamily = DmSans
    ),
    bodyLarge      = TextStyle(
        fontSize = 14.sp, fontWeight = FontWeight.Normal,
        lineHeight = 22.sp, fontFamily = DmSans
    ),
    bodyMedium     = TextStyle(
        fontSize = 13.sp, fontWeight = FontWeight.Normal,
        lineHeight = 20.sp, fontFamily = DmSans
    ),
    bodySmall      = TextStyle(
        fontSize = 12.sp, fontWeight = FontWeight.Normal,
        lineHeight = 17.sp, fontFamily = DmSans
    ),
    labelLarge     = TextStyle(
        fontSize = 12.sp, fontWeight = FontWeight.Medium,
        lineHeight = 16.sp, letterSpacing = 0.1.sp, fontFamily = DmSans
    ),
    labelMedium    = TextStyle(
        fontSize = 11.sp, fontWeight = FontWeight.Medium,
        lineHeight = 14.sp, letterSpacing = 0.1.sp, fontFamily = DmSans
    ),
    labelSmall     = TextStyle(
        fontSize = 10.sp, fontWeight = FontWeight.Medium,
        lineHeight = 13.sp, letterSpacing = 0.3.sp, fontFamily = DmSans
    ),
)

// ── Shapes modernas — más redondas que el tema anterior ──────
// Inspiradas en Linear/Raycast: pequeños elementos sharp, cards suaves
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),   // chips, badges, inputs
    small      = RoundedCornerShape(8.dp),   // botones pequeños
    medium     = RoundedCornerShape(12.dp),  // cards, dialogs
    large      = RoundedCornerShape(16.dp),  // bottom sheets, modales
    extraLarge = RoundedCornerShape(20.dp),  // FAB, surfaces grandes
)

// ── Spacing ampliado — grid 8pt completo ─────────────────────
object Spacing {
    val x2  = 2.dp    // hair-line
    val xs  = 4.dp
    val s3  = 6.dp    // entre xs y sm
    val sm  = 8.dp
    val md  = 12.dp
    val lg  = 16.dp
    val xl  = 24.dp
    val xxl = 32.dp
    val x3l = 40.dp
    val x4l = 48.dp
    val touchMin = 48.dp
    val navHeight = 64.dp
    val topBarHeight = 56.dp
}

// ── Constantes de status — única fuente de verdad ───────────
// Evita string literals dispersos: "done", "active", "pending", "cancelled"
object RouteStatus {
    const val PENDING   = "pending"
    const val ACTIVE    = "active"
    const val DONE      = "done"
    const val CANCELLED = "cancelled"
}

object StopStatus {
    const val PENDING  = "pending"
    const val VISITING = "visiting"
    const val DONE     = "done"
    const val SKIPPED  = "skipped"
}

// ── Tokens semánticos de estado — centralizados ───────────────
// Uso: val c = RouteStatus.color(route.status)
// Evita duplicar when(status) en 6+ pantallas

data class StatusTokens(
    val color:     Color,
    val container: Color,
    val icon:      ImageVector,
    val label:     String,
)

object RouteStatusTokens {
    @Composable
    fun of(status: String): StatusTokens {
        val cs = MaterialTheme.colorScheme
        return when (status) {
            "active"    -> StatusTokens(cs.primary,          cs.primaryContainer,   Icons.Default.PlayCircle,  "Activa")
            "done"      -> StatusTokens(cs.secondary,        cs.secondaryContainer, Icons.Default.CheckCircle, "Completada")
            "cancelled" -> StatusTokens(cs.error,            cs.errorContainer,     Icons.Default.Cancel,      "Cancelada")
            else        -> StatusTokens(cs.onSurfaceVariant, cs.surfaceVariant,     Icons.Default.Schedule,    "Pendiente")
        }
    }
}

object StopStatusTokens {
    @Composable
    fun of(status: String): StatusTokens {
        val cs = MaterialTheme.colorScheme
        return when (status) {
            "done"     -> StatusTokens(cs.secondary,        cs.secondaryContainer, Icons.Default.CheckCircle, "Visitado")
            "visiting" -> StatusTokens(cs.primary,          cs.primaryContainer,   Icons.Default.PlayCircle,  "En visita")
            "skipped"  -> StatusTokens(cs.error,            cs.errorContainer,     Icons.Default.Cancel,      "Saltado")
            else       -> StatusTokens(cs.onSurfaceVariant, cs.surfaceVariant,     Icons.Default.Schedule,    "Pendiente")
        }
    }
}

// ── Theme composable ──────────────────────────────────────────
@Composable
fun RutasAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar transparente — deja que el background del tema se vea
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content,
    )
}
