package com.trixxexe.trixxwave.ui.components.glass

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.trixxexe.trixxwave.data.preferences.ThemeConfig

fun getThemeAccentColor(themeConfig: ThemeConfig): Color {
    return try {
        Color(android.graphics.Color.parseColor(themeConfig.accentColorHex))
    } catch (e: Exception) {
        Color(0xFFF27D26)
    }
}

fun getThemePrimaryColor(themeConfig: ThemeConfig): Color {
    return try {
        Color(android.graphics.Color.parseColor(themeConfig.primaryColorHex))
    } catch (e: Exception) {
        if (themeConfig.mode == "Light") Color(0xFFF1F5F9) else Color(0xFF050505)
    }
}

fun getThemeSecondaryColor(themeConfig: ThemeConfig): Color {
    return when (themeConfig.preset) {
        "AMOLED Dark" -> Color(0xFF00E5FF)
        "AMOLED Light" -> Color(0xFF4F46E5)
        "Liquid Obsidian" -> Color(0xFF7B2CBF)
        "Cyber Pink" -> Color(0xFF00BBFF)
        "Emerald Wave" -> Color(0xFF06B6D4)
        "Sunset Gold" -> Color(0xFFEF4444)
        "Aether White" -> Color(0xFFEC4899)
        else -> Color(0xFF8B5CF6)
    }
}

fun getThemeCanvasColor(themeConfig: ThemeConfig): Color {
    if (themeConfig.preset == "AMOLED Dark") return Color(0xFF000000)
    if (themeConfig.preset == "AMOLED Light") return Color(0xFFFFFFFF)
    if (themeConfig.preset == "Custom Aesthetics") return getThemePrimaryColor(themeConfig)

    if (themeConfig.mode == "Light") return Color(0xFFF1F5F9)
    return when (themeConfig.preset) {
        "Liquid Obsidian" -> Color(0xFF07090E)
        "Cyber Pink" -> Color(0xFF07030A)
        "Emerald Wave" -> Color(0xFF030A07)
        "Sunset Gold" -> Color(0xFF0A0503)
        else -> Color(0xFF050505)
    }
}

fun Modifier.liquidGlass(
    themeConfig: ThemeConfig,
    cornerRadius: Dp = 20.dp,
    tintColor: Color = Color.Unspecified
): Modifier {
    val accent = getThemeAccentColor(themeConfig)
    val isLight = themeConfig.mode == "Light"

    return this.then(
        Modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (themeConfig.contrastSafeMode) {
                        if (isLight) listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0))
                        else listOf(Color(0xF0141414), Color(0xF0080808))
                    } else {
                        val opacity = themeConfig.glassOpacity.coerceIn(0.08f, 0.85f)
                        val baseTint = if (tintColor != Color.Unspecified) tintColor
                        else if (isLight) Color.White else Color.White

                        listOf(
                            baseTint.copy(alpha = opacity),
                            baseTint.copy(alpha = opacity * 0.7f)
                        )
                    }
                )
            )
            .border(
                width = if (themeConfig.contrastSafeMode) 1.5.dp else 1.dp,
                brush = Brush.linearGradient(
                    colors = if (themeConfig.contrastSafeMode) {
                        listOf(if (isLight) Color.Black else Color.White, accent)
                    } else {
                        listOf(
                            (if (isLight) Color.Black else Color.White).copy(alpha = 0.22f),
                            (if (isLight) Color.Black else Color.White).copy(alpha = 0.08f),
                            accent.copy(alpha = 0.35f)
                        )
                    }
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    )
}

@Composable
fun AmbientGlassBackground(
    modifier: Modifier = Modifier,
    themeConfig: ThemeConfig
) {
    val canvasBg = getThemeCanvasColor(themeConfig)
    val accent = getThemeAccentColor(themeConfig)
    val secondary = getThemeSecondaryColor(themeConfig)
    val isLight = themeConfig.mode == "Light"

    if (!themeConfig.customBgUri.isNullOrBlank()) {
        Box(modifier = modifier.fillMaxSize().background(canvasBg)) {
            AsyncImage(
                model = themeConfig.customBgUri,
                contentDescription = "Custom Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(themeConfig.blurIntensity.dp)
            )
            // Overlay glass tint to maintain legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isLight) Color.White.copy(alpha = 0.55f)
                        else Color.Black.copy(alpha = 0.65f)
                    )
            )
        }
        return
    }

    if (!themeConfig.animatedOrbsEnabled) {
        Canvas(modifier = modifier.background(canvasBg)) {}
        return
    }

    if (themeConfig.staticBlurMode) {
        Canvas(modifier = modifier.background(canvasBg)) {
            val width = size.width
            val height = size.height

            // Static Performance Blur mode - zero continuous GPU redraws
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(width * 0.3f, height * 0.3f),
                    radius = width * 0.9f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(width * 0.7f, height * 0.7f),
                    radius = width * 1.0f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(width * 0.5f, height * 0.85f),
                    radius = width * 0.8f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                    center = Offset(width * 0.5f, height * 0.5f),
                    radius = maxOf(width, height) * 0.85f
                )
            )
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    val animOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aurora1"
    )

    val rad = Math.toRadians(animOffset1.toDouble())

    Canvas(modifier = modifier.background(canvasBg)) {
        val width = size.width
        val height = size.height

        // Deep Soothing Atmosphere Layer 1: Indigo Wave
        val orb1X = width * 0.3f + (Math.sin(rad) * (width * 0.25f)).toFloat()
        val orb1Y = height * 0.25f + (Math.cos(rad) * (height * 0.15f)).toFloat()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.18f), Color(0xFF1E1B4B).copy(alpha = 0.05f), Color.Transparent),
                center = Offset(orb1X, orb1Y),
                radius = width * 1.1f
            )
        )

        // Deep Soothing Atmosphere Layer 2: Violet Aurora
        val orb2X = width * 0.7f - (Math.cos(rad * 0.8) * (width * 0.3f)).toFloat()
        val orb2Y = height * 0.65f + (Math.sin(rad * 0.8) * (height * 0.2f)).toFloat()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.16f), Color(0xFF4C1D95).copy(alpha = 0.04f), Color.Transparent),
                center = Offset(orb2X, orb2Y),
                radius = width * 1.25f
            )
        )

        // Deep Soothing Atmosphere Layer 3: Warm Emerald / Cyan Tint
        val orb3X = width * 0.5f + (Math.sin(rad * 1.3) * (width * 0.2f)).toFloat()
        val orb3Y = height * 0.85f - (Math.cos(rad * 1.1) * (height * 0.15f)).toFloat()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF06B6D4).copy(alpha = 0.12f), Color.Transparent),
                center = Offset(orb3X, orb3Y),
                radius = width * 0.95f
            )
        )

        // Deep Soothing Atmosphere Layer 4: Subtle Soft Warm Amber Reflection
        val orb4X = width * 0.2f - (Math.sin(rad * 0.6) * (width * 0.15f)).toFloat()
        val orb4Y = height * 0.75f + (Math.cos(rad * 0.6) * (height * 0.18f)).toFloat()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(orb4X, orb4Y),
                radius = width * 0.8f
            )
        )

        // Soothing Dark Edge Vignette overlay for maximum contrast and glass depth
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                center = Offset(width * 0.5f, height * 0.5f),
                radius = maxOf(width, height) * 0.85f
            )
        )
    }
}

