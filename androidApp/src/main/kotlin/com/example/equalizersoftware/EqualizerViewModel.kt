package com.example.equalizersoftware

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.equalizersoftware.data.SettingsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EqualizerViewModel : ViewModel() {

    private var equalizerService: EqualizerService? = null
    private var isBound = false
    private var settingsManager: SettingsManager? = null

    private val _isServiceRunning = mutableStateOf(false)
    val isServiceRunning: State<Boolean> = _isServiceRunning

    private val _bassBoostStrength = mutableStateOf(0f)
    val bassBoostStrength: State<Float> = _bassBoostStrength

    private val _virtualizerStrength = mutableStateOf(0f)
    val virtualizerStrength: State<Float> = _virtualizerStrength

    private val _loudnessGain = mutableStateOf(0f)
    val loudnessGain: State<Float> = _loudnessGain

    private val _bandLevels = mutableStateOf(mapOf<Short, Short>())
    val bandLevels: State<Map<Short, Short>> = _bandLevels

    private val _bandFrequencies = mutableStateOf(mapOf<Short, Float>())
    val bandFrequencies: State<Map<Short, Float>> = _bandFrequencies

    private val _currentVolume = mutableStateOf(0)
    val currentVolume: State<Int> = _currentVolume

    private val _currentPreset = mutableStateOf("Custom")
    val currentPreset: State<String> = _currentPreset

    private val _balance = mutableStateOf(0f)
    val balance: State<Float> = _balance

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("EqualizerViewModel", "Service Connected")
            val binder = service as EqualizerService.LocalBinder
            equalizerService = binder.getService()
            isBound = true
            _isServiceRunning.value = true
            updateFromService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("EqualizerViewModel", "Service Disconnected")
            isBound = false
            _isServiceRunning.value = false
            equalizerService = null
        }
    }

    fun initSettings(context: Context) {
        if (settingsManager == null) {
            settingsManager = SettingsManager(context)
            _currentPreset.value = settingsManager?.getPreset() ?: "Flat"
            _bassBoostStrength.value = (settingsManager?.getBassBoost()?.toFloat() ?: 0f) / 10f
            _virtualizerStrength.value = (settingsManager?.getVirtualizer()?.toFloat() ?: 0f) / 10f
            _loudnessGain.value = (settingsManager?.getLoudness()?.toFloat() ?: 0f) / 100f
            
            val levels = mutableMapOf<Short, Short>()
            for (i in 0 until 20) {
                levels[i.toShort()] = settingsManager?.getBandLevel(i.toShort()) ?: 0
            }
            _bandLevels.value = levels
        }
    }

    fun bindService(context: Context) {
        initSettings(context)
        val intent = Intent(context, EqualizerService::class.java)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            _isServiceRunning.value = true
        } catch (e: Exception) {
            Log.e("EqualizerViewModel", "bindService failed", e)
        }
        monitorVolume(context)
    }

    private fun monitorVolume(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        viewModelScope.launch {
            try {
                while (true) {
                    try {
                        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        if (maxVolume > 0) {
                            _currentVolume.value = (volume.toFloat() / maxVolume * 100).toInt()
                        }
                    } catch (e: Exception) {
                        // Ignore volume monitoring errors
                    }
                    delay(500)
                }
            } catch (e: Exception) {
                Log.d("EqualizerViewModel", "Volume monitoring stopped")
            }
        }
    }

    private fun updateFromService() {
        equalizerService?.let { service ->
            val bands = service.getNumberOfBands()
            val levels = mutableMapOf<Short, Short>()
            val freqs = mutableMapOf<Short, Float>()
            for (i in 0 until bands) {
                val band = i.toShort()
                levels[band] = settingsManager?.getBandLevel(band) ?: 0
                freqs[band] = service.getBandFrequency(band)
            }
            _bandLevels.value = levels
            _bandFrequencies.value = freqs
            _bassBoostStrength.value = (service.bassBoost?.roundedStrength?.toFloat() ?: 0f) / 10f
            _virtualizerStrength.value = (service.virtualizer?.roundedStrength?.toFloat() ?: 0f) / 10f
            _loudnessGain.value = (service.loudnessEnhancer?.targetGain?.toFloat() ?: 0f) / 100f
        }
    }

    fun unbindService(context: Context) {
        if (isBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: Exception) {
                Log.e("EqualizerViewModel", "unbindService failed", e)
            }
            isBound = false
        }
    }

    fun stopService(context: Context) {
        _isServiceRunning.value = false
        unbindService(context)
        val intent = Intent(context, EqualizerService::class.java).apply {
            action = "STOP_SERVICE"
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            Log.e("EqualizerViewModel", "stopService failed", e)
        }
        equalizerService = null
    }

    fun setBandLevel(band: Short, level: Short) {
        _bandLevels.value = _bandLevels.value.toMutableMap().apply { put(band, level) }
        if (equalizerService != null) {
            equalizerService?.setBandLevel(band, level)
            Log.d("EqualizerViewModel", "Band $band set to $level")
        } else {
            Log.w("EqualizerViewModel", "Service not available, only updating UI state. Band $band set to $level")
        }
        settingsManager?.saveBandLevel(band, level)
        
        // Auto-save as custom preset
        if (_currentPreset.value == "Custom") {
            settingsManager?.saveCustomBandLevel(band, level)
        } else {
            // If changing bands from a preset, switch to Custom
            _currentPreset.value = "Custom"
            settingsManager?.savePreset("Custom")
            settingsManager?.saveCustomBandLevel(band, level)
        }
    }

    fun setBassBoost(strength: Float) {
        _bassBoostStrength.value = strength
        val s = (strength * 1000).toInt().toShort()  // Map 0-1 to 0-1000
        if (equalizerService != null) {
            equalizerService?.setBassBoost(s)
            Log.d("EqualizerViewModel", "Bass boost set to $s")
        } else {
            Log.w("EqualizerViewModel", "Service not available, only updating UI state. Bass boost set to $s")
        }
        settingsManager?.saveBassBoost(s)
    }

    fun setVirtualizer(strength: Float) {
        _virtualizerStrength.value = strength
        val s = (strength * 1000).toInt().toShort()  // Map 0-1 to 0-1000
        if (equalizerService != null) {
            equalizerService?.setVirtualizer(s)
            Log.d("EqualizerViewModel", "Virtualizer set to $s")
        } else {
            Log.w("EqualizerViewModel", "Service not available, only updating UI state. Virtualizer set to $s")
        }
        settingsManager?.saveVirtualizer(s)
    }

    fun setLoudness(gain: Float) {
        _loudnessGain.value = gain
        // Gain is 0-100 from UI. 
        // LoudnessEnhancer targetGain is in mB. 
        // Flow Equalizer has very strong loudness. Let's map 0-100 to 0-3000 mB (30dB)
        val g = (gain * 30).toInt()
        if (equalizerService != null) {
            equalizerService?.setLoudness(g)
            Log.d("EqualizerViewModel", "Loudness set to $g mB")
        } else {
            Log.w("EqualizerViewModel", "Service not available, only updating UI state. Loudness set to $g mB")
        }
        settingsManager?.saveLoudness(g)
    }

    fun setBalance(balance: Float) {
        _balance.value = balance
    }

    fun applyPreset(presetName: String) {
        _currentPreset.value = presetName
        settingsManager?.savePreset(presetName)
        
        when (presetName) {
            "Flat" -> {
                for (i in 0 until 20) {
                    setBandLevel(i.toShort(), 0)
                }
            }
            "Rock" -> {
                // Rock: Bass and treble boost, mid scoop
                setBandLevel(0, 8)      // 31Hz - bass
                setBandLevel(1, 8)      // 40Hz
                setBandLevel(2, 6)      // 50Hz
                setBandLevel(3, 4)      // 63Hz
                setBandLevel(4, 2)      // 80Hz
                setBandLevel(5, 1)      // 100Hz
                setBandLevel(6, 0)      // 125Hz - mid scoop
                setBandLevel(7, 0)      // 160Hz
                setBandLevel(8, 1)      // 200Hz
                setBandLevel(9, 2)      // 250Hz
                setBandLevel(10, 1)     // 400Hz
                setBandLevel(11, 0)     // 630Hz
                setBandLevel(12, 0)     // 1kHz
                setBandLevel(13, 2)     // 1.6kHz
                setBandLevel(14, 6)     // 2.5kHz - treble start
                setBandLevel(15, 8)     // 4kHz
                setBandLevel(16, 9)     // 6.3kHz
                setBandLevel(17, 8)     // 8kHz
                setBandLevel(18, 6)     // 12.5kHz
                setBandLevel(19, 5)     // 16kHz
            }
            "Pop" -> {
                // Pop: Bright and punchy, boosted mids
                setBandLevel(0, 4)      // 31Hz
                setBandLevel(1, 5)      // 40Hz
                setBandLevel(2, 5)      // 50Hz
                setBandLevel(3, 4)      // 63Hz
                setBandLevel(4, 3)      // 80Hz
                setBandLevel(5, 2)      // 100Hz
                setBandLevel(6, 3)      // 125Hz
                setBandLevel(7, 4)      // 160Hz
                setBandLevel(8, 5)      // 200Hz - mids boost
                setBandLevel(9, 6)      // 250Hz
                setBandLevel(10, 6)     // 400Hz
                setBandLevel(11, 5)     // 630Hz
                setBandLevel(12, 4)     // 1kHz
                setBandLevel(13, 4)     // 1.6kHz
                setBandLevel(14, 5)     // 2.5kHz
                setBandLevel(15, 6)     // 4kHz
                setBandLevel(16, 5)     // 6.3kHz
                setBandLevel(17, 4)     // 8kHz
                setBandLevel(18, 2)     // 12.5kHz
                setBandLevel(19, 1)     // 16kHz
            }
            "Metal" -> {
                // Metal: Heavy bass and treble, scooped mids
                setBandLevel(0, 10)     // 31Hz - heavy bass
                setBandLevel(1, 10)     // 40Hz
                setBandLevel(2, 9)      // 50Hz
                setBandLevel(3, 8)      // 63Hz
                setBandLevel(4, 6)      // 80Hz
                setBandLevel(5, 3)      // 100Hz
                setBandLevel(6, 1)      // 125Hz - scoop
                setBandLevel(7, 0)      // 160Hz
                setBandLevel(8, 0)      // 200Hz
                setBandLevel(9, 1)      // 250Hz
                setBandLevel(10, 0)     // 400Hz
                setBandLevel(11, 1)     // 630Hz
                setBandLevel(12, 2)     // 1kHz
                setBandLevel(13, 5)     // 1.6kHz
                setBandLevel(14, 8)     // 2.5kHz - treble
                setBandLevel(15, 10)    // 4kHz
                setBandLevel(16, 11)    // 6.3kHz
                setBandLevel(17, 10)    // 8kHz
                setBandLevel(18, 8)     // 12.5kHz
                setBandLevel(19, 7)     // 16kHz
            }
            "Electric" -> {
                // Electric: Focused on vocals and presence
                setBandLevel(0, 3)      // 31Hz
                setBandLevel(1, 4)      // 40Hz
                setBandLevel(2, 5)      // 50Hz
                setBandLevel(3, 5)      // 63Hz
                setBandLevel(4, 4)      // 80Hz
                setBandLevel(5, 3)      // 100Hz
                setBandLevel(6, 2)      // 125Hz
                setBandLevel(7, 2)      // 160Hz
                setBandLevel(8, 3)      // 200Hz
                setBandLevel(9, 4)      // 250Hz
                setBandLevel(10, 5)     // 400Hz - presence
                setBandLevel(11, 6)     // 630Hz
                setBandLevel(12, 6)     // 1kHz
                setBandLevel(13, 5)     // 1.6kHz
                setBandLevel(14, 4)     // 2.5kHz
                setBandLevel(15, 5)     // 4kHz
                setBandLevel(16, 4)     // 6.3kHz
                setBandLevel(17, 3)     // 8kHz
                setBandLevel(18, 2)     // 12.5kHz
                setBandLevel(19, 1)     // 16kHz
            }
            "Lo-Fi" -> {
                // Lo-Fi: Warm and muffled, reduced highs
                setBandLevel(0, 6)      // 31Hz
                setBandLevel(1, 6)      // 40Hz
                setBandLevel(2, 5)      // 50Hz
                setBandLevel(3, 4)      // 63Hz
                setBandLevel(4, 3)      // 80Hz
                setBandLevel(5, 2)      // 100Hz
                setBandLevel(6, 1)      // 125Hz
                setBandLevel(7, 1)      // 160Hz
                setBandLevel(8, 1)      // 200Hz
                setBandLevel(9, 1)      // 250Hz
                setBandLevel(10, 0)     // 400Hz
                setBandLevel(11, 0)     // 630Hz
                setBandLevel(12, 0)     // 1kHz
                setBandLevel(13, 0)     // 1.6kHz
                setBandLevel(14, 0)     // 2.5kHz
                setBandLevel(15, 0)     // 4kHz
                setBandLevel(16, 0)     // 6.3kHz
                setBandLevel(17, 0)     // 8kHz
                setBandLevel(18, 0)     // 12.5kHz
                setBandLevel(19, 0)     // 16kHz
            }
            "Ambient" -> {
                // Ambient: Smooth and spacious, enhanced mids and highs
                setBandLevel(0, 2)      // 31Hz
                setBandLevel(1, 2)      // 40Hz
                setBandLevel(2, 2)      // 50Hz
                setBandLevel(3, 2)      // 63Hz
                setBandLevel(4, 2)      // 80Hz
                setBandLevel(5, 1)      // 100Hz
                setBandLevel(6, 0)      // 125Hz
                setBandLevel(7, 1)      // 160Hz
                setBandLevel(8, 2)      // 200Hz
                setBandLevel(9, 3)      // 250Hz
                setBandLevel(10, 4)     // 400Hz
                setBandLevel(11, 4)     // 630Hz
                setBandLevel(12, 4)     // 1kHz
                setBandLevel(13, 5)     // 1.6kHz
                setBandLevel(14, 5)     // 2.5kHz
                setBandLevel(15, 5)     // 4kHz
                setBandLevel(16, 4)     // 6.3kHz
                setBandLevel(17, 4)     // 8kHz
                setBandLevel(18, 3)     // 12.5kHz
                setBandLevel(19, 2)     // 16kHz
            }
            "Custom" -> {
                // Load custom preset from settings
                loadCustomPreset()
            }
        }
        _currentPreset.value = presetName
    }

    private fun loadCustomPreset() {
        // Load all 20 band levels for custom preset
        val customBands = mutableMapOf<Short, Short>()
        for (i in 0 until 20) {
            val bandKey = "custom_band_$i"
            val level = settingsManager?.getCustomBandLevel(i.toShort()) ?: 0
            customBands[i.toShort()] = level
            equalizerService?.setBandLevel(i.toShort(), level)
        }
        _bandLevels.value = customBands
    }

    fun saveCustomPreset() {
        // Save all current band levels as custom preset
        for (i in 0 until 20) {
            val level = _bandLevels.value[i.toShort()] ?: 0
            settingsManager?.saveCustomBandLevel(i.toShort(), level)
        }
        settingsManager?.savePreset("Custom")
    }
}
