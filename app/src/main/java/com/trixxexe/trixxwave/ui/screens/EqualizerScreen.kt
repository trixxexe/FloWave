package com.trixxexe.trixxwave.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trixxexe.trixxwave.data.preferences.ThemeConfig
import com.trixxexe.trixxwave.media.AudioEqualizerManager
import com.trixxexe.trixxwave.media.EqualizerBand
import com.trixxexe.trixxwave.ui.components.glass.LiquidGlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    equalizerManager: AudioEqualizerManager,
    themeConfig: ThemeConfig,
    onBack: () -> Unit
) {
    var bassBoostVal by remember { mutableStateOf(0.5f) }
    var virtualizerVal by remember { mutableStateOf(0.3f) }
    var selectedPreset by remember { mutableStateOf("Flat") }
    val presets = listOf("Flat", "Bass Boost", "Rock", "Pop", "Electronic")

    val bands = remember { equalizerManager.getBands() }

    Column(
        modifier = Modifier
            .testTag("equalizer_screen")
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "10-Band Graphic Equalizer",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Presets Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presets) { preset ->
                FilterChip(
                    selected = selectedPreset == preset,
                    onClick = {
                        selectedPreset = preset
                        equalizerManager.applyPreset(preset)
                    },
                    label = { Text(preset) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFF27D26),
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bass Boost & Virtualizer
        LiquidGlassCard(
            themeConfig = themeConfig,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column {
                Text("Bass Boost", color = Color.White, fontWeight = FontWeight.Bold)
                Slider(
                    value = bassBoostVal,
                    onValueChange = {
                        bassBoostVal = it
                        equalizerManager.setBassBoost((it * 1000).toInt().toShort())
                    },
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFF27D26), activeTrackColor = Color(0xFFF27D26))
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("3D Virtualizer", color = Color.White, fontWeight = FontWeight.Bold)
                Slider(
                    value = virtualizerVal,
                    onValueChange = {
                        virtualizerVal = it
                        equalizerManager.setVirtualizer((it * 1000).toInt().toShort())
                    },
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF8B5CF6), activeTrackColor = Color(0xFF8B5CF6))
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Frequency Bands (Hz)",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Equalizer Sliders
        if (bands.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Text("Equalizer active on audio playback.", color = Color(0xFF94A3B8))
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                bands.forEach { band ->
                    var sliderVal by remember { mutableStateOf(band.currentLevelMb.toFloat()) }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(
                            text = "${band.centerFreqHz}Hz",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = sliderVal,
                            onValueChange = {
                                sliderVal = it
                                equalizerManager.setBandLevel(band.index, it.toInt().toShort())
                            },
                            valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat(),
                            colors = SliderDefaults.colors(thumbColor = Color(0xFFF27D26), activeTrackColor = Color(0xFFF27D26)),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
