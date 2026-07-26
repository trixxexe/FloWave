package com.trixxexe.trixxwave.ui.components.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trixxexe.trixxwave.data.preferences.ThemeConfig

@Composable
fun HybridModeToggleBar(
    isOnlineMode: Boolean,
    themeConfig: ThemeConfig,
    onModeToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = getThemeAccentColor(themeConfig)

    Row(
        modifier = modifier
            .testTag("hybrid_mode_toggle_bar")
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Offline Mode Tab
        val offlineBg by animateColorAsState(
            targetValue = if (!isOnlineMode) accentColor.copy(alpha = 0.28f) else Color.Transparent,
            animationSpec = tween(300), label = "offlineBg"
        )
        val offlineText by animateColorAsState(
            targetValue = if (!isOnlineMode) Color.White else Color.White.copy(alpha = 0.65f),
            animationSpec = tween(300), label = "offlineText"
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(offlineBg)
                .border(
                    width = if (!isOnlineMode) 1.dp else 0.dp,
                    color = if (!isOnlineMode) accentColor.copy(alpha = 0.6f) else Color.Transparent,
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { onModeToggle(false) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Offline Mode",
                    tint = if (!isOnlineMode) accentColor else Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Offline Mode",
                    color = offlineText,
                    fontSize = 13.sp,
                    fontWeight = if (!isOnlineMode) FontWeight.Bold else FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Online Mode Tab
        val onlineBg by animateColorAsState(
            targetValue = if (isOnlineMode) accentColor.copy(alpha = 0.28f) else Color.Transparent,
            animationSpec = tween(300), label = "onlineBg"
        )
        val onlineText by animateColorAsState(
            targetValue = if (isOnlineMode) Color.White else Color.White.copy(alpha = 0.65f),
            animationSpec = tween(300), label = "onlineText"
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(onlineBg)
                .border(
                    width = if (isOnlineMode) 1.dp else 0.dp,
                    color = if (isOnlineMode) accentColor.copy(alpha = 0.6f) else Color.Transparent,
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { onModeToggle(true) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = "Online Mode",
                    tint = if (isOnlineMode) accentColor else Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Online Mode",
                    color = onlineText,
                    fontSize = 13.sp,
                    fontWeight = if (isOnlineMode) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
