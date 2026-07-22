package com.example.equalizersoftware

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.equalizersoftware.ui.theme.EqualizerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: EqualizerViewModel = viewModel()
            val context = LocalContext.current
            val volume by viewModel.currentVolume
            val preset by viewModel.currentPreset

            EqualizerTheme(volume = volume, preset = preset) {
                DisposableEffect(Unit) {
                    // Auto-start the service when app launches
                    viewModel.bindService(context)
                    onDispose {
                        viewModel.unbindService(context)
                    }
                }

                EqualizerApp(viewModel)
            }
        }
    }
}

@Composable
fun EqualizerApp(viewModel: EqualizerViewModel) {
    val context = LocalContext.current
    val volume by viewModel.currentVolume
    val preset by viewModel.currentPreset
    val bandLevels by viewModel.bandLevels
    val bandFreqs by viewModel.bandFrequencies
    val bassBoost by viewModel.bassBoostStrength
    val virtualizer by viewModel.virtualizerStrength
    val loudness by viewModel.loudnessGain
    val balance by viewModel.balance
    val isRunning by viewModel.isServiceRunning

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0E27)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Flow Equalizer",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // EQ Graph Section with Presets
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xFF1A1F3A), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("▮▮▮", fontSize = 18.sp, color = Color(0xFF4DB8FF))
                            Text(
                                "Equalizer",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        // Equalizer Toggle
                        Switch(
                            checked = isRunning,
                            onCheckedChange = {
                                if (it) viewModel.bindService(context)
                                else viewModel.stopService(context)
                            },
                            modifier = Modifier.scale(0.8f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF4DB8FF),
                                checkedTrackColor = Color(0xFF6EC8FF)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // EQ Curve Graph
                    EQGraphVisualizer(bandLevels, bandFreqs)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Frequency labels below graph
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k").forEach { freq ->
                            Text(
                                freq,
                                color = Color(0xFF7A8BA8),
                                fontSize = 9.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Presets Dropdown
                    var expandPresets by remember { mutableStateOf(false) }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Button(
                            onClick = { expandPresets = !expandPresets },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2A3154)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Preset: $preset",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (expandPresets) "▲" else "▼",
                                    color = Color(0xFF4DB8FF),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Dropdown Menu
                        if (expandPresets) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .background(Color(0xFF1A1F3A), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF2A3154), RoundedCornerShape(8.dp))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp)
                                ) {
                                    listOf("Lo-Fi", "Ambient", "Rock", "Pop", "Metal", "Electric", "Custom").forEach { p ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (preset == p) Color(0xFF4DB8FF) else Color.Transparent,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable {
                                                    viewModel.applyPreset(p)
                                                    expandPresets = false
                                                }
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                p,
                                                color = if (preset == p) Color.Black else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = if (preset == p) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // All 10 Main Equalizer Bands (31Hz to 16kHz) - Compact Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(Color(0xFF1A1F3A), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        "Main EQ",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val mainBandLabels = listOf(
                        Pair(7, "31Hz"),
                        Pair(8, "62Hz"),
                        Pair(9, "125Hz"),
                        Pair(10, "250Hz"),
                        Pair(11, "500Hz"),
                        Pair(12, "1kHz"),
                        Pair(13, "2kHz"),
                        Pair(14, "4kHz"),
                        Pair(15, "8kHz"),
                        Pair(16, "16kHz")
                    )

                    mainBandLabels.forEach { (band, freqLabel) ->
                        val level = bandLevels[band.toShort()] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                freqLabel,
                                color = Color(0xFF7A8BA8),
                                fontSize = 10.sp,
                                modifier = Modifier.width(45.dp)
                            )
                            Slider(
                                value = level.toFloat(),
                                onValueChange = { viewModel.setBandLevel(band.toShort(), it.toInt().toShort()) },
                                valueRange = 0f..1500f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF4DB8FF),
                                    activeTrackColor = Color(0xFF4DB8FF),
                                    inactiveTrackColor = Color(0xFF3A4054)
                                )
                            )
                            Text(
                                "+${level / 100}dB",
                                color = Color.White,
                                fontSize = 9.sp,
                                modifier = Modifier.width(40.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Extended EQ Bands (20kHz to 3.5kHz) - Additional frequencies
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(Color(0xFF1A1F3A), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        "Extended EQ",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val extendedBandLabels = listOf(
                        Pair(17, "18kHz"),
                        Pair(18, "20kHz"),
                        Pair(19, "3.5kHz"),
                        Pair(5, "10kHz"),
                        Pair(6, "12kHz")
                    )

                    extendedBandLabels.forEach { (band, freqLabel) ->
                        val level = bandLevels[band.toShort()] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                freqLabel,
                                color = Color(0xFF7A8BA8),
                                fontSize = 10.sp,
                                modifier = Modifier.width(45.dp)
                            )
                            Slider(
                                value = level.toFloat(),
                                onValueChange = { viewModel.setBandLevel(band.toShort(), it.toInt().toShort()) },
                                valueRange = 0f..1500f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF4DB8FF),
                                    activeTrackColor = Color(0xFF4DB8FF),
                                    inactiveTrackColor = Color(0xFF3A4054)
                                )
                            )
                            Text(
                                "+${level / 100}dB",
                                color = Color.White,
                                fontSize = 9.sp,
                                modifier = Modifier.width(40.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(16.dp))            // Bass Boost - NO TOGGLE, Always ON with multiple frequency bands
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color(0xFF1A1F3A), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔊", fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            "Bass Boost",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bass Boost Frequencies: 10Hz, 50Hz, 80Hz, 100Hz, 120Hz, 150Hz, 200Hz
                    val bassFrequencies = listOf(
                        Pair("10Hz", 0),
                        Pair("50Hz", 1),
                        Pair("80Hz", 2),
                        Pair("100Hz", 3),
                        Pair("120Hz", 4),
                        Pair("150Hz", 5),
                        Pair("200Hz", 6)
                    )

                    bassFrequencies.forEach { (freq, bandIdx) ->
                        val bandLevel = bandLevels[bandIdx.toShort()] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                freq,
                                color = Color(0xFF7A8BA8),
                                fontSize = 11.sp,
                                modifier = Modifier.width(45.dp)
                            )
                            Slider(
                                value = bandLevel.toFloat(),
                                onValueChange = { viewModel.setBandLevel(bandIdx.toShort(), it.toInt().toShort()) },
                                valueRange = 0f..1500f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF4DB8FF),
                                    activeTrackColor = Color(0xFF4DB8FF),
                                    inactiveTrackColor = Color(0xFF3A4054)
                                )
                            )
                            Text(
                                "+${bandLevel / 100}dB",
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.width(45.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bass Boost Master Gain slider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚡", fontSize = 16.sp)
                        Text(
                            "Master",
                            color = Color(0xFF7A8BA8),
                            fontSize = 12.sp,
                            modifier = Modifier.width(50.dp)
                        )
                        Slider(
                            value = bassBoost,
                            onValueChange = { viewModel.setBassBoost(it) },
                            valueRange = 0f..100f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4DB8FF),
                                activeTrackColor = Color(0xFF4DB8FF),
                                inactiveTrackColor = Color(0xFF3A4054)
                            )
                        )
                        Text(
                            "+${bassBoost.toInt()}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.width(45.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Loudness - NO TOGGLE, Always ON with slider visible
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color(0xFF1A1F3A), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔔", fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            "Loudness",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Loudness Gain slider - always visible
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚡", fontSize = 16.sp)
                        Text(
                            "Gain",
                            color = Color(0xFF7A8BA8),
                            fontSize = 12.sp,
                            modifier = Modifier.width(40.dp)
                        )
                        Slider(
                            value = loudness,
                            onValueChange = { viewModel.setLoudness(it) },
                            valueRange = 0f..100f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4DB8FF),
                                activeTrackColor = Color(0xFF4DB8FF),
                                inactiveTrackColor = Color(0xFF3A4054)
                            )
                        )
                        Text(
                            "+${loudness.toInt()}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.width(50.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Virtualizer - NO TOGGLE, Always ON with slider visible
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color(0xFF1A1F3A), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⭕", fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            "Virtualizer",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Virtualizer Gain slider - always visible
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚡", fontSize = 16.sp)
                        Text(
                            "Gain",
                            color = Color(0xFF7A8BA8),
                            fontSize = 12.sp,
                            modifier = Modifier.width(40.dp)
                        )
                        Slider(
                            value = virtualizer,
                            onValueChange = { viewModel.setVirtualizer(it) },
                            valueRange = 0f..100f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4DB8FF),
                                activeTrackColor = Color(0xFF4DB8FF),
                                inactiveTrackColor = Color(0xFF3A4054)
                            )
                        )
                        Text(
                            "+${virtualizer.toInt()}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.width(50.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Audio Balance - NO TOGGLE, Always ON with slider visible
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color(0xFF1A1F3A), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⬌", fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            "Audio Balance",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Balance slider - always visible, ranges from -100 (Left) to +100 (Right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("L", color = Color(0xFF7A8BA8), fontSize = 12.sp, modifier = Modifier.width(15.dp))
                        Slider(
                            value = balance,
                            onValueChange = { viewModel.setBalance(it) },
                            valueRange = -100f..100f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4DB8FF),
                                activeTrackColor = Color(0xFF4DB8FF),
                                inactiveTrackColor = Color(0xFF3A4054)
                            )
                        )
                        Text("R", color = Color(0xFF7A8BA8), fontSize = 12.sp, modifier = Modifier.width(15.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Left", color = Color(0xFF7A8BA8), fontSize = 10.sp)
                        Text("Center", color = Color(0xFF7A8BA8), fontSize = 10.sp)
                        Text("Right", color = Color(0xFF7A8BA8), fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun EQGraphVisualizer(bandLevels: Map<Short, Short>, bandFreqs: Map<Short, Float>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color(0xFF0F1428), RoundedCornerShape(8.dp))
            .drawBehind {
                val width = size.width
                val height = size.height
                val centerY = height / 2

                // Draw grid
                for (i in 0..10) {
                    val x = (i / 10f) * width
                    drawLine(
                        color = Color(0xFF2A3154),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f
                    )
                }

                // Draw horizontal center line
                drawLine(
                    color = Color(0xFF2A3154),
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 1f
                )

                // Draw EQ curve
                val points = mutableListOf<Offset>()
                for (i in 0 until 10) {
                    val band = i.toShort()
                    val level = (bandLevels[band]?.toFloat() ?: 0f)
                    val normalized = (level / 1500f).coerceIn(-1f, 1f)
                    val x = (i / 9f) * width
                    val y = centerY - (normalized * (height / 2.5f))
                    points.add(Offset(x, y))
                }

                // Draw curve line
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = Color(0xFF4DB8FF),
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 2.5f
                    )
                }

                // Draw points
                points.forEach { point ->
                    drawCircle(
                        color = Color(0xFF4DB8FF),
                        radius = 4f,
                        center = point
                    )
                }

                // Draw filled area under curve
                val path = Path()
                path.moveTo(points[0].x, centerY)
                points.forEach { path.lineTo(it.x, it.y) }
                path.lineTo(points.last().x, centerY)
                path.close()
                drawPath(
                    path = path,
                    color = Color(0xFF4DB8FF),
                    alpha = 0.15f
                )
            }
    )
}

@Composable
fun EffectCard(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    icon: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color(0xFF1A1F3A), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.scale(0.8f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF4DB8FF),
                        checkedTrackColor = Color(0xFF6EC8FF),
                        uncheckedThumbColor = Color(0xFF6A7588),
                        uncheckedTrackColor = Color(0xFF3A4054)
                    )
                )
            }

            if (isEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚡", fontSize = 16.sp)
                    Slider(
                        value = value,
                        onValueChange = onValueChange,
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF4DB8FF),
                            activeTrackColor = Color(0xFF4DB8FF),
                            inactiveTrackColor = Color(0xFF3A4054)
                        )
                    )
                    Text(
                        "${value.toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.width(30.dp)
                    )
                }
            }
        }
    }
}
