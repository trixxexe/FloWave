package com.trixxexe.trixxwave.ui.components.glass

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.trixxexe.trixxwave.data.preferences.ThemeConfig

data class AudioOutputDeviceItem(
    val id: Int,
    val name: String,
    val typeName: String,
    val icon: ImageVector,
    val isExternal: Boolean,
    val audioDeviceInfo: AudioDeviceInfo?
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
    var connectedDevices by remember { mutableStateOf(getConnectedOutputDevices(context)) }
    var selectedDeviceId by remember { mutableIntStateOf(-1) }

    // Register Audio Device Listener for real-time Aux / Bluetooth / USB connection detection
    DisposableEffect(context) {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                connectedDevices = getConnectedOutputDevices(context)
            }
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                connectedDevices = getConnectedOutputDevices(context)
            }
        }
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        onDispose {
            audioManager.unregisterAudioDeviceCallback(callback)
        }
    }

    val externalDevices = remember(connectedDevices) {
        connectedDevices.filter { it.isExternal }
    }

    val activeDevice = remember(connectedDevices, selectedDeviceId) {
        connectedDevices.find { it.id == selectedDeviceId }
            ?: externalDevices.firstOrNull()
            ?: connectedDevices.find { !it.isExternal }
    }

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
                    text = "$volumePercent%",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Roundish Glass Output Device Switcher Button - Appears when external audio devices (Bluetooth / Aux / USB / Cast) are connected
        AnimatedVisibility(
            visible = externalDevices.isNotEmpty(),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
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
                        .background(accentColor.copy(alpha = 0.25f))
                        .border(1.dp, accentColor.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = activeDevice?.icon ?: Icons.Default.BluetoothAudio,
                        contentDescription = "Audio Device Output Switcher",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showDeviceDialog) {
        AudioOutputDeviceDialog(
            themeConfig = themeConfig,
            devices = connectedDevices,
            activeDeviceId = activeDevice?.id ?: -1,
            onSelectDevice = { deviceItem ->
                selectedDeviceId = deviceItem.id
                switchAudioDevice(context, deviceItem)
                showDeviceDialog = false
            },
            onDismiss = { showDeviceDialog = false }
        )
    }
}

fun getConnectedOutputDevices(context: Context): List<AudioOutputDeviceItem> {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    val result = mutableListOf<AudioOutputDeviceItem>()

    for (device in devices) {
        val type = device.type
        val rawName = device.productName?.toString()?.trim()

        val (icon, typeName, defaultName) = when (type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> Triple(Icons.Default.Bluetooth, "Bluetooth Audio", "Bluetooth Audio Device")

            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_AUX_LINE -> Triple(Icons.Default.Headphones, "Aux / Wired Headset", "3.5mm Aux / Headphones")

            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> Triple(Icons.Default.SpeakerGroup, "USB DAC Audio", "USB Audio DAC")

            AudioDeviceInfo.TYPE_IP,
            AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> Triple(Icons.Default.Cast, "Cast / Network", "Wireless Audio Cast")

            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> Triple(Icons.Default.Speaker, "Phone Speaker", "Internal Phone Speaker")

            else -> continue
        }

        val displayName = if (!rawName.isNullOrBlank() && rawName != "0") rawName else defaultName
        val isExternal = type != AudioDeviceInfo.TYPE_BUILTIN_SPEAKER && type != AudioDeviceInfo.TYPE_BUILTIN_EARPIECE

        result.add(
            AudioOutputDeviceItem(
                id = device.id,
                name = displayName,
                typeName = typeName,
                icon = icon,
                isExternal = isExternal,
                audioDeviceInfo = device
            )
        )
    }
    return result
}

fun switchAudioDevice(context: Context, deviceItem: AudioOutputDeviceItem) {
    try {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (deviceItem.isExternal && deviceItem.audioDeviceInfo != null) {
                audioManager.setCommunicationDevice(deviceItem.audioDeviceInfo)
            } else {
                audioManager.clearCommunicationDevice()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun AudioOutputDeviceDialog(
    themeConfig: ThemeConfig,
    devices: List<AudioOutputDeviceItem>,
    activeDeviceId: Int,
    onSelectDevice: (AudioOutputDeviceItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = getThemeAccentColor(themeConfig)

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
                            text = "Audio Output Panel",
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

                if (devices.isEmpty()) {
                    Text(
                        text = "No active audio devices found.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    devices.forEach { device ->
                        val isSelected = device.id == activeDeviceId

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) accentColor.copy(alpha = 0.22f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectDevice(device) }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) accentColor else Color.White.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = device.icon,
                                            contentDescription = device.name,
                                            tint = if (isSelected) Color.Black else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = device.name,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${device.typeName} • Connected",
                                            color = if (isSelected) accentColor else Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active Route",
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Launch System Native Media Output Control Sheet Button
                Button(
                    onClick = {
                        try {
                            val intent = Intent("com.android.settings.panel.action.MEDIA_OUTPUT").apply {
                                putExtra("com.android.settings.panel.extra.PACKAGE_NAME", context.packageName)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback to system sound settings if media panel is unavailable
                            try {
                                context.startActivity(Intent(android.provider.Settings.ACTION_SOUND_SETTINGS))
                            } catch (ex: Exception) { ex.printStackTrace() }
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Cast, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("System Output Settings", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

