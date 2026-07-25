package com.trixxexe.trixxwave.ui.components.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trixxexe.trixxwave.data.preferences.ThemeConfig

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    themeConfig: ThemeConfig = ThemeConfig(),
    cornerRadius: Dp = 20.dp,
    tintColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    testTag: String = "liquid_glass_card",
    content: @Composable BoxScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .liquidGlass(
                themeConfig = themeConfig,
                cornerRadius = cornerRadius,
                tintColor = tintColor
            )
            .then(clickableModifier)
            .padding(12.dp)
    ) {
        content()
    }
}
