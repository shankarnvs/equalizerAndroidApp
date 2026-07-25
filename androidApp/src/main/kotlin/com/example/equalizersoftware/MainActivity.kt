package com.example.equalizersoftware

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.equalizersoftware.ui.Widgets
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private var equalizerService: EqualizerService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? EqualizerService.LocalBinder
            equalizerService = binder?.getService()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            equalizerService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val intent = Intent(this, EqualizerService::class.java)
        startForegroundService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            val viewModel: EqualizerViewModel = viewModel()
            LaunchedEffect(Unit) {
                viewModel.bindService(this@MainActivity)
            }
            
            val currentVolume by viewModel.currentVolume
            val currentPreset by viewModel.currentPreset
            
            EqualizerApp(viewModel, equalizerService, currentVolume, currentPreset)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }
}

@Composable
fun EqualizerApp(viewModel: EqualizerViewModel, service: EqualizerService?, volume: Int, preset: String) {
    val isServiceRunning by viewModel.isServiceRunning
    val currentVolume by viewModel.currentVolume
    val currentPreset by viewModel.currentPreset
    val bandLevels by viewModel.bandLevels
    val bassBoostStrength by viewModel.bassBoostStrength
    val loudnessGain by viewModel.loudnessGain
    val virtualizerStrength by viewModel.virtualizerStrength
    val balance by viewModel.balance

    var selectedTab by remember { mutableStateOf(0) }
    var isEQEnabled by remember { mutableStateOf(true) }
    
    val tabs = listOf("Main EQ", "Extended", "Bass", "Loudness", "Virtualizer", "Balance")
    val presets = listOf("Flat", "Rock", "Pop", "Metal", "Electric", "Lo-Fi", "Ambient", "Custom")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1A3A),
                        Color(0xFF1A2D4D),
                        Color(0xFF0A2A3A)
                    )
                )
            )
            .padding(top = 24.dp, bottom = 60.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0F2E))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Sound Flow Controller", fontSize = 24.sp, color = Color(0xFFFFFF00), fontWeight = FontWeight.Bold)
                Text("$currentPreset | Vol: $currentVolume%", fontSize = 11.sp, color = Color(0xFFFFFF00))
            }
            Button(
                onClick = { 
                    isEQEnabled = !isEQEnabled
                    service?.setEqualizerEnabled(isEQEnabled)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEQEnabled) Color(0xFFFFFF00) else Color.Gray
                )
            ) {
                Text(if (isEQEnabled) "ON" else "OFF", color = Color.Black, fontSize = 10.sp)
            }
        }

        HorizontalDivider(color = Color(0xFF2A4A6A), thickness = 1.dp)

        // Graph Section (40% of remaining space)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .background(Color(0xFF0F1F3F))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val freqBands = listOf(
                    "31Hz" to (bandLevels[0.toShort()] ?: 0),
                    "62Hz" to (bandLevels[1.toShort()] ?: 0),
                    "125Hz" to (bandLevels[2.toShort()] ?: 0),
                    "250Hz" to (bandLevels[3.toShort()] ?: 0),
                    "500Hz" to (bandLevels[4.toShort()] ?: 0),
                    "1kHz" to (bandLevels[5.toShort()] ?: 0),
                    "2kHz" to (bandLevels[6.toShort()] ?: 0),
                    "4kHz" to (bandLevels[7.toShort()] ?: 0),
                    "8kHz" to (bandLevels[8.toShort()] ?: 0),
                    "16kHz" to (bandLevels[9.toShort()] ?: 0)
                )
                
                freqBands.forEach { (label, level) ->
                    val heightFraction = ((level + 15f) / 30f).coerceIn(0.1f, 1f)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .fillMaxHeight(heightFraction)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF0078FF),
                                            Color(0xFFFFFF00),
                                            Color(0xFFFF0000)
                                        )
                                    ),
                                    shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                )
                        )
                        Text(label, fontSize = 7.sp, color = Color(0xFF888888), maxLines = 1)
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFF2A4A6A), thickness = 1.dp)

        // Presets Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(Color(0xFF0A1F3F))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            presets.forEach { presetName ->
                Button(
                    onClick = { viewModel.applyPreset(presetName) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentPreset == presetName) Color(0xFFFFFF00) else Color(0xFF1A3F5F)
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(presetName, fontSize = 10.sp, color = if (currentPreset == presetName) Color.Black else Color(0xFFFFFF00))
                }
            }
        }

        HorizontalDivider(color = Color(0xFF2A4A6A), thickness = 1.dp)

        // Tabs Section (15% of remaining space)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.176f)
                .horizontalScroll(rememberScrollState())
                .background(Color(0xFF0A1F3F))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { idx, tab ->
                Button(
                    onClick = { selectedTab = idx },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == idx) Color(0xFFFFFF00) else Color(0xFF1A3F5F)
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(tab, fontSize = 11.sp, color = if (selectedTab == idx) Color.Black else Color(0xFFFFFF00))
                }
            }
        }

        HorizontalDivider(color = Color(0xFF2A4A6A), thickness = 1.dp)

        // Content Section (remaining 45%)
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTab) {
                    0 -> MainEQTab(viewModel, bandLevels)
                    1 -> ExtendedEQTab(viewModel, bandLevels)
                    2 -> BassTab(viewModel, bandLevels, bassBoostStrength)
                    3 -> LoudnessTab(viewModel, loudnessGain)
                    4 -> VirtualizerTab(viewModel, virtualizerStrength)
                    5 -> BalanceTab(viewModel, balance)
                }
            }
        }
    }
}

@Composable
fun MainEQTab(viewModel: EqualizerViewModel, levels: Map<Short, Short>) {
    val freqs = listOf("31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            freqs.forEachIndexed { i, freq ->
                val level = levels[i.toShort()] ?: 0
                Widgets.VerticalThermometerBar(
                    value = level / 15f,
                    onValueChange = { viewModel.setBandLevel(i.toShort(), (it * 15).toInt().toShort()) },
                    label = freq
                )
            }
        }
    }
}

@Composable
fun ExtendedEQTab(viewModel: EqualizerViewModel, levels: Map<Short, Short>) {
    val freqs = listOf("20kHz", "25kHz", "30kHz", "35kHz", "40kHz")
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            freqs.forEachIndexed { i, freq ->
                val level = levels[(i + 10).toShort()] ?: 0
                Widgets.VerticalThermometerBar(
                    value = level / 15f,
                    onValueChange = { viewModel.setBandLevel((i + 10).toShort(), (it * 15).toInt().toShort()) },
                    label = freq
                )
            }
        }
    }
}

@Composable
fun BassTab(viewModel: EqualizerViewModel, levels: Map<Short, Short>, boost: Float) {
    val freqs = listOf("10Hz", "20Hz", "40Hz", "80Hz", "100Hz", "150Hz", "200Hz")
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            freqs.forEachIndexed { i, freq ->
                val level = levels[i.toShort()] ?: 0
                Widgets.VerticalThermometerBar(
                    value = level / 15f,
                    onValueChange = { viewModel.setBandLevel(i.toShort(), (it * 15).toInt().toShort()) },
                    label = freq
                )
            }
        }
        
        HorizontalDivider(color = Color(0xFF2A4A6A), thickness = 1.dp)
        
        Widgets.HorizontalThermometerBar(
            value = boost,
            onValueChange = { viewModel.setBassBoost(it) },
            label = "Bass Boost"
        )
    }
}

@Composable
fun LoudnessTab(viewModel: EqualizerViewModel, level: Float) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)) {
        Widgets.HorizontalThermometerBar(
            value = level,
            onValueChange = { viewModel.setLoudness(it) },
            label = "Loudness Enhancement"
        )
    }
}

@Composable
fun VirtualizerTab(viewModel: EqualizerViewModel, level: Float) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)) {
        Widgets.HorizontalThermometerBar(
            value = level,
            onValueChange = { viewModel.setVirtualizer(it) },
            label = "Spatial Audio"
        )
    }
}

@Composable
fun BalanceTab(viewModel: EqualizerViewModel, balance: Float) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)) {
        val leftVal = (1 - balance).coerceIn(0f, 1f)
        val rightVal = balance.coerceIn(0f, 1f)
        
        Widgets.StereoPairControl(
            leftValue = leftVal,
            onLeftChange = { viewModel.setBalance(1 - it) },
            rightValue = rightVal,
            onRightChange = { viewModel.setBalance(it) }
        )
    }
}
