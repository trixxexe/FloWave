package com.trixxexe.trixxwave.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.trixxexe.trixxwave.data.preferences.ThemeConfig

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = SleekPurple,
    tertiary = AmberSunset,
    background = Color(0xFF000000),
    surface = Color(0xFF121212),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFFAFAFA),
    onSurface = Color(0xFFFAFAFA),
    onSurfaceVariant = Color(0xFFA0A0A0)
)

@Composable
fun TrixxWaveTheme(
    themeConfig: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(themeConfig.accentColorHex))
    } catch (e: Exception) {
        Color(0xFF00F5D4)
    }

    val isLight = themeConfig.mode == "Light"
    val bgColor = if (isLight) Color(0xFFF8FAFC) else if (themeConfig.mode == "Dark Charcoal") Color(0xFF0F0F12) else Color(0xFF000000)
    val surfaceColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF121212)

    val colorScheme = DarkColorScheme.copy(
        primary = accentColor,
        secondary = accentColor.copy(alpha = 0.8f),
        background = bgColor,
        surface = surfaceColor,
        onBackground = if (isLight) Color(0xFF0F172A) else Color(0xFFFAFAFA),
        onSurface = if (isLight) Color(0xFF0F172A) else Color(0xFFFAFAFA)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
