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
import com.pabl3st.rutapp.R

// ── Paleta extraída de la PWA (app.css) ──────────────────────
object RutasColors {
    val Primary      = Color(0xFF2563EB)
    val PrimaryLight = Color(0xFFEFF6FF)
    val PrimaryDark  = Color(0xFF1D4ED8)
    val Success      = Color(0xFF10B981)
    val SuccessLight = Color(0xFFF0FDF4)
    val Danger       = Color(0xFFEF4444)
    val DangerLight  = Color(0xFFFEF2F2)
    val Warning      = Color(0xFFF59E0B)
    val WarningLight = Color(0xFFFFFBEB)
    val Info         = Color(0xFF06B6D4)
    val Purple       = Color(0xFF7C3AED)
    val PurpleLight  = Color(0xFFFAF5FF)
    val Pink         = Color(0xFFF472B6)
    val BgPage       = Color(0xFFF1F5F9)
    val BgSurface    = Color(0xFFF8FAFC)
    val BgCard       = Color(0xFFFFFFFF)
    val DarkBg       = Color(0xFF0F172A)
    val DarkSurface  = Color(0xFF1E293B)
    val DarkCard     = Color(0xFF1E293B)
    val DarkHover    = Color(0xFF334155)
    val TextPrimary       = Color(0xFF0F172A)
    val TextSecondary     = Color(0xFF475569)
    val TextSubtle        = Color(0xFF64748B)
    val TextMuted         = Color(0xFF94A3B8)
    val TextPrimaryDark   = Color(0xFFF1F5F9)
    val TextSecondaryDark = Color(0xFF94A3B8)
    val Border       = Color(0xFFE2E8F0)
    val BorderInput  = Color(0xFFCBD5E1)
}

// ── Espaciado fiel al CSS ─────────────────────────────────────
object Spacing {
    val xs  = 4.dp
    val sm  = 8.dp
    val md  = 12.dp
    val lg  = 16.dp
    val xl  = 24.dp
    val xxl = 32.dp
    val touchMin = 48.dp
}

// ── Shapes ────────────────────────────────────────────────────
object RutasShapes {
    val xs = 4.dp
    val sm = 6.dp
    val md = 10.dp
    val lg = 14.dp
}

// ── Tipografía — DM Sans + JetBrains Mono ────────────────────
val DmSans = FontFamily(
    Font(R.font.dm_sans,          FontWeight.Normal),
    Font(R.font.dm_sans_medium,   FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
    Font(R.font.dm_sans_bold,     FontWeight.Bold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
)

// ── Color schemes ─────────────────────────────────────────────
private val LightColors = lightColorScheme(
    primary             = RutasColors.Primary,
    onPrimary           = Color.White,
    primaryContainer    = RutasColors.PrimaryLight,
    onPrimaryContainer  = RutasColors.PrimaryDark,
    secondary           = RutasColors.Success,
    onSecondary         = Color.White,
    secondaryContainer  = RutasColors.SuccessLight,
    onSecondaryContainer= RutasColors.Success,
    tertiary            = RutasColors.Warning,
    onTertiary          = Color.White,
    tertiaryContainer   = RutasColors.WarningLight,
    onTertiaryContainer = RutasColors.Warning,
    error               = RutasColors.Danger,
    onError             = Color.White,
    errorContainer      = RutasColors.DangerLight,
    background          = RutasColors.BgPage,
    onBackground        = RutasColors.TextPrimary,
    surface             = RutasColors.BgCard,
    onSurface           = RutasColors.TextPrimary,
    surfaceVariant      = RutasColors.BgSurface,
    onSurfaceVariant    = RutasColors.TextSubtle,
    outline             = RutasColors.Border,
    outlineVariant      = RutasColors.BorderInput,
    scrim               = Color(0x80000000),
)

private val DarkColors = darkColorScheme(
    primary             = RutasColors.Primary,
    onPrimary           = Color.White,
    primaryContainer    = RutasColors.PrimaryDark,
    onPrimaryContainer  = RutasColors.PrimaryLight,
    secondary           = RutasColors.Success,
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFF065F46),
    onSecondaryContainer= RutasColors.Success,
    tertiary            = RutasColors.Warning,
    onTertiary          = RutasColors.DarkBg,
    tertiaryContainer   = Color(0xFFB45309),
    onTertiaryContainer = RutasColors.WarningLight,
    error               = RutasColors.Danger,
    onError             = Color.White,
    errorContainer      = Color(0xFF7F1D1D),
    background          = RutasColors.DarkBg,
    onBackground        = RutasColors.TextPrimaryDark,
    surface             = RutasColors.DarkCard,
    onSurface           = RutasColors.TextPrimaryDark,
    surfaceVariant      = RutasColors.DarkSurface,
    onSurfaceVariant    = RutasColors.TextSecondaryDark,
    outline             = Color(0xFF334155),
    outlineVariant      = Color(0xFF1E293B),
    scrim               = Color(0x99000000),
)

// ── Typography con DM Sans ────────────────────────────────────
private val AppTypography = Typography(
    headlineLarge  = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp, letterSpacing = (-0.3).sp, fontFamily = DmSans),
    headlineMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp, letterSpacing = (-0.2).sp, fontFamily = DmSans),
    titleLarge     = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, fontFamily = DmSans),
    titleMedium    = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp, fontFamily = DmSans),
    titleSmall     = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium,   lineHeight = 18.sp, fontFamily = DmSans),
    bodyLarge      = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal,   lineHeight = 20.sp, fontFamily = DmSans),
    bodyMedium     = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal,   lineHeight = 18.sp, fontFamily = DmSans),
    bodySmall      = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal,   lineHeight = 16.sp, fontFamily = DmSans),
    labelLarge     = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium,   lineHeight = 16.sp, fontFamily = DmSans),
    labelMedium    = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium,   lineHeight = 14.sp, fontFamily = DmSans),
    labelSmall     = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium,   lineHeight = 14.sp, letterSpacing = 0.2.sp, fontFamily = DmSans),
)

// ── Shapes ────────────────────────────────────────────────────
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(RutasShapes.xs),
    small      = RoundedCornerShape(RutasShapes.sm),
    medium     = RoundedCornerShape(RutasShapes.md),
    large      = RoundedCornerShape(RutasShapes.lg),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun RutasAppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = (if (darkTheme) RutasColors.DarkBg else RutasColors.BgPage).toArgb()
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
