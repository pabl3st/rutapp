package com.pabl3st.rutapp.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

object Spacing {
    val xs = 4.dp; val sm = 8.dp; val md = 16.dp
    val lg = 24.dp; val xl = 32.dp; val xxl = 48.dp
    val touchMin = 48.dp
}

private val LightColors = lightColorScheme(
    primary          = Color(0xFF1565C0),
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E4FF),
    secondary        = Color(0xFF00695C),
    tertiary         = Color(0xFFBA7517),
    error            = Color(0xFFB71C1C),
    surface          = Color(0xFFF8F9FA),
    background       = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF49454F),
    outline          = Color(0xFFCFD8DC),
    surfaceVariant   = Color(0xFFECEFF1),
)

private val DarkColors = darkColorScheme(
    primary          = Color(0xFF90CAF9),
    onPrimary        = Color(0xFF003065),
    secondary        = Color(0xFF80CBC4),
    tertiary         = Color(0xFFFFCC80),
    error            = Color(0xFFEF9A9A),
    surface          = Color(0xFF1A1C1E),
    background       = Color(0xFF111318),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline          = Color(0xFF455A64),
    surfaceVariant   = Color(0xFF2A2D31),
)

private val AppTypography = Typography(
    headlineLarge  = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleLarge     = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium,   lineHeight = 24.sp),
    titleMedium    = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium,   lineHeight = 22.sp),
    bodyLarge      = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal,   lineHeight = 24.sp),
    bodyMedium     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal,   lineHeight = 20.sp),
    labelLarge     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium,   lineHeight = 20.sp),
    labelSmall     = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium,   lineHeight = 16.sp),
)

@Composable
fun RutasAppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
