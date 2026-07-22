package com.example.equalizersoftware.data

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.locks.ReentrantReadWriteLock

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("equalizer_settings", Context.MODE_PRIVATE)
    private val lock = ReentrantReadWriteLock()

    fun saveBandLevel(band: Short, level: Short) {
        lock.writeLock().lock()
        try {
            prefs.edit().putInt("band_$band", level.toInt()).apply()
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun getBandLevel(band: Short): Short {
        lock.readLock().lock()
        try {
            return prefs.getInt("band_$band", 0).toShort()
        } finally {
            lock.readLock().unlock()
        }
    }

    fun saveBassBoost(strength: Short) {
        lock.writeLock().lock()
        try {
            prefs.edit().putInt("bass_boost", strength.toInt()).apply()
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun getBassBoost(): Short {
        lock.readLock().lock()
        try {
            return prefs.getInt("bass_boost", 0).toShort()
        } finally {
            lock.readLock().unlock()
        }
    }

    fun saveVirtualizer(strength: Short) {
        lock.writeLock().lock()
        try {
            prefs.edit().putInt("virtualizer", strength.toInt()).apply()
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun getVirtualizer(): Short {
        lock.readLock().lock()
        try {
            return prefs.getInt("virtualizer", 0).toShort()
        } finally {
            lock.readLock().unlock()
        }
    }

    fun saveLoudness(gain: Int) {
        lock.writeLock().lock()
        try {
            prefs.edit().putInt("loudness", gain).apply()
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun getLoudness(): Int {
        lock.readLock().lock()
        try {
            return prefs.getInt("loudness", 0)
        } finally {
            lock.readLock().unlock()
        }
    }

    fun savePreset(preset: String) {
        lock.writeLock().lock()
        try {
            prefs.edit().putString("current_preset", preset).apply()
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun getPreset(): String {
        lock.readLock().lock()
        try {
            return prefs.getString("current_preset", "Flat") ?: "Flat"
        } finally {
            lock.readLock().unlock()
        }
    }

    fun saveCustomBandLevel(band: Short, level: Short) {
        lock.writeLock().lock()
        try {
            prefs.edit().putInt("custom_band_${band}", level.toInt()).apply()
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun getCustomBandLevel(band: Short): Short {
        lock.readLock().lock()
        try {
            return prefs.getInt("custom_band_${band}", 0).toShort()
        } finally {
            lock.readLock().unlock()
        }
    }
}
