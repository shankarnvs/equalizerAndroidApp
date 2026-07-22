package com.example.equalizersoftware

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.equalizersoftware.data.SettingsManager

class EqualizerService : Service() {

    private val binder = LocalBinder()
    private lateinit var settingsManager: SettingsManager

    var dynamicsProcessing: DynamicsProcessing? = null
    var legacyEqualizer: Equalizer? = null
    var bassBoost: BassBoost? = null
    var virtualizer: Virtualizer? = null
    var loudnessEnhancer: LoudnessEnhancer? = null

    private val bandFrequencies = floatArrayOf(
        31f, 40f, 50f, 63f, 80f, 100f, 125f, 160f, 200f, 250f,
        400f, 630f, 1000f, 1600f, 2500f, 4000f, 6300f, 8000f, 12500f, 16000f
    )

    inner class LocalBinder : Binder() {
        fun getService(): EqualizerService = this@EqualizerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, createNotification())
        }
        initializeAudioEffects()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            releaseAllEffects()
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun initializeAudioEffects() {
        // Init EQ (Try DynamicsProcessing first for 20 bands)
        try {
            // Some devices reject DynamicsProcessing on session 0.
            // We use a minimal config to increase compatibility.
            val builder = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                1, true, 20, false, 0, false, 0, false
            )
            dynamicsProcessing = DynamicsProcessing(0, 0, builder.build()).apply {
                for (i in 0 until 20) {
                    val userLevel = settingsManager.getBandLevel(i.toShort()).toFloat() / 100f
                    val band = DynamicsProcessing.EqBand(true, bandFrequencies[i], userLevel)
                    setPreEqBandAllChannelsTo(i, band)
                }
                enabled = true
            }
            Log.d("EqualizerService", "DynamicsProcessing initialized")
        } catch (e: Exception) {
            Log.e("EqualizerService", "DynamicsProcessing failed: ${e.message}")
            try {
                legacyEqualizer = Equalizer(0, 0).apply {
                    val bands = numberOfBands
                    for (i in 0 until bands) {
                        setBandLevel(i.toShort(), settingsManager.getBandLevel(i.toShort()))
                    }
                    enabled = true
                }
                Log.d("EqualizerService", "Legacy Equalizer initialized")
            } catch (e2: Exception) {
                Log.e("EqualizerService", "Legacy EQ failed: ${e2.message}")
            }
        }

        // Bass Boost
        try {
            bassBoost = BassBoost(0, 0).apply {
                if (strengthSupported) {
                    setStrength(settingsManager.getBassBoost())
                }
                enabled = true
            }
            Log.d("EqualizerService", "BassBoost initialized")
        } catch (e: Exception) {
            Log.e("EqualizerService", "BassBoost failed: ${e.message}")
        }

        // Virtualizer
        try {
            virtualizer = Virtualizer(0, 0).apply {
                if (strengthSupported) {
                    setStrength(settingsManager.getVirtualizer())
                }
                enabled = true
            }
            Log.d("EqualizerService", "Virtualizer initialized")
        } catch (e: Exception) {
            Log.e("EqualizerService", "Virtualizer failed: ${e.message}")
        }

        // Loudness Enhancer
        try {
            loudnessEnhancer = LoudnessEnhancer(0).apply {
                setTargetGain(settingsManager.getLoudness())
                enabled = true
            }
            Log.d("EqualizerService", "LoudnessEnhancer initialized")
        } catch (e: Exception) {
            Log.e("EqualizerService", "LoudnessEnhancer failed: ${e.message}")
        }
    }

    private fun applyBassEnhancement() {
        // Bass boost is now handled separately by BassBoost effect
        // No need to manually modify band levels
        // This prevents conflicts with user-set band levels
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "equalizer_channel",
                "Equalizer Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, EqualizerService::class.java).apply {
            action = "STOP_SERVICE"
        }
        val stopPendingIntent = android.app.PendingIntent.getService(
            this, 0, stopIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "equalizer_channel")
            .setContentTitle("Equalizer Active")
            .setContentText("Tuning your audio...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun releaseAllEffects() {
        try {
            dynamicsProcessing?.enabled = false
            dynamicsProcessing?.release()
            dynamicsProcessing = null

            legacyEqualizer?.enabled = false
            legacyEqualizer?.release()
            legacyEqualizer = null

            bassBoost?.enabled = false
            bassBoost?.release()
            bassBoost = null

            virtualizer?.enabled = false
            virtualizer?.release()
            virtualizer = null

            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            Log.d("EqualizerService", "All effects released")
        } catch (e: Exception) {
            Log.e("EqualizerService", "Error releasing effects", e)
        }
    }

    override fun onDestroy() {
        releaseAllEffects()
        super.onDestroy()
    }

    fun setBandLevel(band: Short, level: Short) {
        val db = level.toFloat() / 100f
        
        dynamicsProcessing?.let { dp ->
            try {
                if (band < 20) {
                    val eqBand = DynamicsProcessing.EqBand(true, bandFrequencies[band.toInt()], db)
                    dp.setPreEqBandAllChannelsTo(band.toInt(), eqBand)
                }
            } catch (e: Exception) {
                Log.e("EqualizerService", "dp.setBandLevel failed", e)
            }
        }
        
        legacyEqualizer?.let { eq ->
            try {
                if (band < eq.numberOfBands) {
                    eq.setBandLevel(band, level)
                }
            } catch (e: Exception) {
                Log.e("EqualizerService", "legacy.setBandLevel failed", e)
            }
        }
        
        settingsManager.saveBandLevel(band, level)
    }

    fun setBassBoost(strength: Short) {
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(strength)
            }
            settingsManager.saveBassBoost(strength)
        } catch (e: Exception) {
            Log.e("EqualizerService", "setBassBoost failed", e)
        }
    }

    fun getBandFrequency(band: Short): Float {
        dynamicsProcessing?.let { return bandFrequencies[band.toInt()] }
        legacyEqualizer?.let { return it.getCenterFreq(band) / 1000f }
        return 0f
    }
    
    fun getNumberOfBands(): Short {
        dynamicsProcessing?.let { return 20 }
        legacyEqualizer?.let { return it.numberOfBands }
        return 0
    }

    fun setVirtualizer(strength: Short) {
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(strength)
            }
            settingsManager.saveVirtualizer(strength)
        } catch (e: Exception) {
            Log.e("EqualizerService", "setVirtualizer failed", e)
        }
    }

    fun setLoudness(gainmB: Int) {
        try {
            loudnessEnhancer?.setTargetGain(gainmB)
            settingsManager.saveLoudness(gainmB)
        } catch (e: Exception) {
            Log.e("EqualizerService", "setLoudness failed", e)
        }
    }
}
