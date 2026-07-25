package com.trixxexe.trixxwave.ui.components.glass

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.trixxexe.trixxwave.data.preferences.ThemeConfig

data class AudioOutputDevice(
    val id: String,
    val name: String,
    val type: String,
    val icon: ImageVector,
    val isConnected: Boolean,
    val isSelected: Boolean
)

@Composable
fun LiquidGlassVolumeControlRow(
    themeConfig: ThemeConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    
    var currentVolumeInt by remember { 
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) 
    }
    
    var showDeviceDialog by remember { mutableStateOf(false) }
    var selectedDeviceName by remember { mutableStateOf("Bluetooth AirPods Pro") }

    val accentColor = getThemeAccentColor(themeConfig)

    val volumeFraction = (currentVolumeInt.toFloat() / maxVolume).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(targetValue = volumeFraction, label = "volumeFraction")
    val volumePercent = (animatedFraction * 100).toInt()

    var containerWidthPx by remember { mutableFloatStateOf(1f) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("liquid_glass_volume_row"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Capsule Liquid Glass Volume Slider
        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("liquid_glass_volume_slider")
                .liquidGlass(
                    themeConfig = themeConfig,
                    cornerRadius = 26.dp
                )
                .onSizeChanged { containerWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newFraction = (offset.x / containerWidthPx).coerceIn(0f, 1f)
                        val newVol = (newFraction * maxVolume).toInt()
                        currentVolumeInt = newVol
                        try {
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val newFraction = (change.position.x / containerWidthPx).coerceIn(0f, 1f)
                        val newVol = (newFraction * maxVolume).toInt()
                        currentVolumeInt = newVol
                        try {
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Fill background inside glass capsule
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp, topEnd = if (animatedFraction > 0.92f) 26.dp else 12.dp, bottomEnd = if (animatedFraction > 0.92f) 26.dp else 12.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                accentColor.copy(alpha = 0.38f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.4f), accentColor.copy(alpha = 0.5f))
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
            )

            // Inner Content: Icon & Volume Number
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val volIcon = when {
                    volumePercent == 0 -> Icons.Default.VolumeMute
                    volumePercent < 40 -> Icons.Default.VolumeDown
                    else -> Icons.Default.VolumeUp
                }

                Icon(
                    imageVector = volIcon,
                    contentDescription = "Volume",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "$volumePercent",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Roundish Glass Output Device Shower Button
        Box(
            modifier = Modifier
                .size(52.dp)
                .testTag("bluetooth_device_shower_button")
                .liquidGlass(
                    themeConfig = themeConfig,
                    cornerRadius = 26.dp
                )
                .clickable { showDeviceDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BluetoothAudio,
                    contentDescription = "Bluetooth Device Output",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showDeviceDialog) {
        AudioOutputDeviceDialog(
            themeConfig = themeConfig,
            selectedDeviceName = selectedDeviceName,
            onSelectDevice = { name ->
                selectedDeviceName = name
                showDeviceDialog = false
            },
            onDismiss = { showDeviceDialog = false }
        )
    }
}

@Composable
fun AudioOutputDeviceDialog(
    themeConfig: ThemeConfig,
    selectedDeviceName: String,
    onSelectDevice: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = getThemeAccentColor(themeConfig)

    val devices = listOf(
        AudioOutputDevice("1", "Bluetooth AirPods Pro", "Bluetooth Audio", Icons.Default.Headphones, isConnected = true, isSelected = selectedDeviceName == "Bluetooth AirPods Pro"),
        AudioOutputDevice("2", "Phone Internal Speaker", "System Speaker", Icons.Default.Speaker, isConnected = true, isSelected = selectedDeviceName == "Phone Internal Speaker"),
        AudioOutputDevice("3", "FloWave AirPlay Dock", "AirPlay / Cast", Icons.Default.Cast, isConnected = true, isSelected = selectedDeviceName == "FloWave AirPlay Dock"),
        AudioOutputDevice("4", "Type-C High-Res DAC", "Wired Audio", Icons.Default.SpeakerGroup, isConnected = false, isSelected = selectedDeviceName == "Type-C High-Res DAC")
    )

    Dialog(onDismissRequest = onDismiss) {
        LiquidGlassCard(
            themeConfig = themeConfig,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("audio_output_device_dialog"),
            cornerRadius = 28.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BluetoothAudio,
                            contentDescription = "Audio Output",
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Audio Output Devices",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                devices.forEach { device ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (device.isSelected) accentColor.copy(alpha = 0.22f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (device.isSelected) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectDevice(device.name) }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (device.isSelected) accentColor else Color.White.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = device.icon,
                                        contentDescription = device.name,
                                        tint = if (device.isSelected) Color.Black else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = device.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${device.type} • ${if (device.isConnected) "Connected" else "Disconnected"}",
                                        color = if (device.isConnected) accentColor else Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (device.isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
