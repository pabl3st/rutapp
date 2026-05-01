package com.pabl3st.rutapp.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
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

// ── Paleta extraída de la PWA (app.css / app1.css) ───────────
// Primary: #2563eb  — azul eléctrico
// Success: #10b981  — verde esmeralda
// Danger:  #ef4444  — rojo
// Warning: #f59e0b  — ámbar
// Header:  #0f172a  — azul marino oscuro
// Mono:    JetBrains Mono para códigos/números

object RutasColors {
    // Primarios
    val Primary      = Color(0xFF2563EB)
    val PrimaryLight = Color(0xFFEFF6FF)
    val PrimaryDark  = Color(0xFF1D4ED8)

    // Semánticos
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

    // Fondos light
    val BgPage       = Color(0xFFF1F5F9)
    val BgSurface    = Color(0xFFF8FAFC)
    val BgCard       = Color(0xFFFFFFFF)

    // Fondos dark
    val DarkBg       = Color(0xFF0F172A)   // --app-header, bg-page dark
    val DarkSurface  = Color(0xFF1E293B)   // --bg-surface dark
    val DarkCard     = Color(0xFF1E293B)   // --bg-card dark
    val DarkHover    = Color(0xFF334155)   // --bg-hover dark

    // Texto light
    val TextPrimary   = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF475569)
    val TextSubtle    = Color(0xFF64748B)
    val TextMuted     = Color(0xFF94A3B8)

    // Texto dark
    val TextPrimaryDark   = Color(0xFFF1F5F9)
    val TextSecondaryDark = Color(0xFF94A3B8)

    // Bordes
    val Border       = Color(0xFFE2E8F0)
    val BorderInput  = Color(0xFFCBD5E1)
}

// ── Espaciado fiel al CSS ─────────────────────────────────────
object Spacing {
    val xs  = 4.dp   // --space-1
    val sm  = 8.dp   // --space-2
    val md  = 12.dp  // --space-3 / --px
    val lg  = 16.dp  // --space-4
    val xl  = 24.dp  // --space-6
    val xxl = 32.dp  // --space-8
    val touchMin = 48.dp
}

// ── Esquinas (border-radius del CSS) ─────────────────────────
object RutasShapes {
    val xs = 4.dp   // badges mini
    val sm = 6.dp   // chips, pills — --r-sm
    val md = 10.dp  // cards, inputs — --r-md
    val lg = 14.dp  // modals, bottom sheets — --r-lg
}

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

// ── Tipografía — DM Sans (system fallback hasta importar la font) ─
// DM Sans se añadirá como font asset en S04. Por ahora usa system-ui.
private val AppTypography = Typography(
    // Títulos de pantalla
    headlineLarge  = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    headlineMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp, letterSpacing = (-0.2).sp),
    // Títulos de sección — --t-lg 15px
    titleLarge     = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    titleMedium    = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp),
    titleSmall     = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium,   lineHeight = 18.sp),
    // Cuerpo — --t-md 13.5px, --t-base 12.5px
    bodyLarge      = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal,   lineHeight = 20.sp),
    bodyMedium     = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal,   lineHeight = 18.sp),
    bodySmall      = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal,   lineHeight = 16.sp),
    // Labels — --t-sm 11px, --t-xs 10px
    labelLarge     = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium,   lineHeight = 16.sp),
    labelMedium    = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium,   lineHeight = 14.sp),
    labelSmall     = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium,   lineHeight = 14.sp, letterSpacing = 0.2.sp),
)

// ── Shapes fiel al CSS --r-sm/md/lg ──────────────────────────
private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(RutasShapes.xs),
    small      = androidx.compose.foundation.shape.RoundedCornerShape(RutasShapes.sm),
    medium     = androidx.compose.foundation.shape.RoundedCornerShape(RutasShapes.md),
    large      = androidx.compose.foundation.shape.RoundedCornerShape(RutasShapes.lg),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
)

@Composable
fun RutasAppTheme(
    darkTheme: Boolean = true,   // Dark por defecto — igual que la PWA
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar del color del header — #0f172a dark / #f1f5f9 light
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
