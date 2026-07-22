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
        val s = (strength * 10).toInt().toShort()
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
        val s = (strength * 10).toInt().toShort()
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
                setBandLevel(0, 300)    // 10Hz
                setBandLevel(1, 200)    // 50Hz
                setBandLevel(2, 100)    // 80Hz
                setBandLevel(3, 150)    // 100Hz
                setBandLevel(4, 200)    // 120Hz
                setBandLevel(5, 250)    // 150Hz
                setBandLevel(6, 200)    // 200Hz
                setBandLevel(7, 50)     // 31Hz
                setBandLevel(8, 100)    // 62Hz
                setBandLevel(9, 150)    // 125Hz
                setBandLevel(10, 200)   // 250Hz
                setBandLevel(11, 150)   // 500Hz
                setBandLevel(12, 100)   // 1kHz
                setBandLevel(13, 50)    // 2kHz
                setBandLevel(14, 0)     // 4kHz
                setBandLevel(15, 200)   // 8kHz
                setBandLevel(16, 250)   // 16kHz
                setBandLevel(17, 0)     // 18kHz
                setBandLevel(18, 0)     // 20kHz
                setBandLevel(19, 50)    // 3.5kHz
            }
            "Pop" -> {
                setBandLevel(0, 0)      // 10Hz
                setBandLevel(1, 100)    // 50Hz
                setBandLevel(2, 150)    // 80Hz
                setBandLevel(3, 200)    // 100Hz
                setBandLevel(4, 150)    // 120Hz
                setBandLevel(5, 100)    // 150Hz
                setBandLevel(6, 50)     // 200Hz
                setBandLevel(7, 0)      // 31Hz
                setBandLevel(8, 50)     // 62Hz
                setBandLevel(9, 100)    // 125Hz
                setBandLevel(10, 150)   // 250Hz
                setBandLevel(11, 200)   // 500Hz
                setBandLevel(12, 250)   // 1kHz
                setBandLevel(13, 200)   // 2kHz
                setBandLevel(14, 100)   // 4kHz
                setBandLevel(15, 50)    // 8kHz
                setBandLevel(16, 0)     // 16kHz
                setBandLevel(17, 0)     // 18kHz
                setBandLevel(18, 0)     // 20kHz
                setBandLevel(19, 150)   // 3.5kHz
            }
            "Metal" -> {
                setBandLevel(0, 400)    // 10Hz - Heavy bass
                setBandLevel(1, 350)    // 50Hz
                setBandLevel(2, 300)    // 80Hz
                setBandLevel(3, 250)    // 100Hz
                setBandLevel(4, 200)    // 120Hz
                setBandLevel(5, 150)    // 150Hz
                setBandLevel(6, 100)    // 200Hz
                setBandLevel(7, 150)    // 31Hz
                setBandLevel(8, 200)    // 62Hz
                setBandLevel(9, 250)    // 125Hz
                setBandLevel(10, 200)   // 250Hz
                setBandLevel(11, 100)   // 500Hz
                setBandLevel(12, 50)    // 1kHz
                setBandLevel(13, 100)   // 2kHz
                setBandLevel(14, 150)   // 4kHz
                setBandLevel(15, 300)   // 8kHz - Heavy treble
                setBandLevel(16, 350)   // 16kHz
                setBandLevel(17, 300)   // 18kHz
                setBandLevel(18, 250)   // 20kHz
                setBandLevel(19, 200)   // 3.5kHz
            }
            "Electric" -> {
                setBandLevel(0, 250)    // 10Hz
                setBandLevel(1, 200)    // 50Hz
                setBandLevel(2, 150)    // 80Hz
                setBandLevel(3, 100)    // 100Hz
                setBandLevel(4, 50)     // 120Hz
                setBandLevel(5, 0)      // 150Hz
                setBandLevel(6, 0)      // 200Hz
                setBandLevel(7, 0)      // 31Hz
                setBandLevel(8, 50)     // 62Hz
                setBandLevel(9, 100)    // 125Hz
                setBandLevel(10, 50)    // 250Hz
                setBandLevel(11, 0)     // 500Hz
                setBandLevel(12, 50)    // 1kHz
                setBandLevel(13, 200)   // 2kHz
                setBandLevel(14, 300)   // 4kHz
                setBandLevel(15, 250)   // 8kHz
                setBandLevel(16, 150)   // 16kHz
                setBandLevel(17, 100)   // 18kHz
                setBandLevel(18, 50)    // 20kHz
                setBandLevel(19, 180)   // 3.5kHz
            }
            "Lo-Fi" -> {
                setBandLevel(0, 100)    // 10Hz
                setBandLevel(1, 80)     // 50Hz
                setBandLevel(2, 60)     // 80Hz
                setBandLevel(3, 40)     // 100Hz
                setBandLevel(4, 20)     // 120Hz
                setBandLevel(5, 0)      // 150Hz
                setBandLevel(6, 0)      // 200Hz
                setBandLevel(7, 0)      // 31Hz
                setBandLevel(8, 20)     // 62Hz
                setBandLevel(9, 40)     // 125Hz
                setBandLevel(10, 0)     // 250Hz
                setBandLevel(11, 0)     // 500Hz
                setBandLevel(12, 0)     // 1kHz
                setBandLevel(13, 0)     // 2kHz
                setBandLevel(14, 0)     // 4kHz
                setBandLevel(15, 0)     // 8kHz
                setBandLevel(16, 0)     // 16kHz
                setBandLevel(17, 0)     // 18kHz
                setBandLevel(18, 0)     // 20kHz
                setBandLevel(19, 0)     // 3.5kHz
            }
            "Ambient" -> {
                setBandLevel(0, 50)     // 10Hz
                setBandLevel(1, 40)     // 50Hz
                setBandLevel(2, 30)     // 80Hz
                setBandLevel(3, 20)     // 100Hz
                setBandLevel(4, 10)     // 120Hz
                setBandLevel(5, 0)      // 150Hz
                setBandLevel(6, 0)      // 200Hz
                setBandLevel(7, 0)      // 31Hz
                setBandLevel(8, 10)     // 62Hz
                setBandLevel(9, 20)     // 125Hz
                setBandLevel(10, 30)    // 250Hz
                setBandLevel(11, 50)    // 500Hz
                setBandLevel(12, 100)   // 1kHz
                setBandLevel(13, 120)   // 2kHz
                setBandLevel(14, 100)   // 4kHz
                setBandLevel(15, 80)    // 8kHz
                setBandLevel(16, 100)   // 16kHz
                setBandLevel(17, 120)   // 18kHz
                setBandLevel(18, 100)   // 20kHz
                setBandLevel(19, 80)    // 3.5kHz
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
