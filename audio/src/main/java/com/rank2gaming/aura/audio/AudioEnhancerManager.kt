package com.rank2gaming.aura.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.Equalizer

object AudioEnhancerManager {
    private const val PREFS_NAME = "AuraAudioPrefs"

    // Keys
    const val KEY_DEFINITION = "definition"
    const val KEY_CLARITY = "clarity"
    const val KEY_BRIGHTNESS = "brightness"
    const val KEY_EQUAKE = "e_quake"
    const val KEY_BASS = "bass"
    const val KEY_BOXY = "boxy"

    const val KEY_BASS_BOOST_API = "bass_boost_api"
    const val KEY_TREBLE = "treble"
    const val KEY_SURROUND = "surround"

    const val KEY_GAIN = "gain"
    const val KEY_GAIN_BOOST = "gain_boost"
    const val KEY_STEREO = "stereo"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveValue(context: Context, key: String, value: Float) {
        getPrefs(context).edit().putFloat(key, value).apply()
    }

    fun getValue(context: Context, key: String): Float {
        return getPrefs(context).getFloat(key, 0f)
    }

    // --- Effect Application Logic (EQ Bands) ---
    fun applyEffects(eq: Equalizer, context: Context) {
        if (!eq.hasControl()) return

        val bands = eq.numberOfBands

        fun dbToMb(db: Float): Short {
            return (db * 100).toInt().toShort()
        }

        for (i in 0 until bands) {
            val centerFreq = eq.getCenterFreq(i.toShort()) / 1000 // Hz

            // 1. Get Base Level (Standard EQ)
            val baseLevel = AudioEffectManager.getBandLevel(context, i.toShort())

            // 2. Calculate Independent Booster Offset
            var boosterBoostDb = 0f

            // e-Quake: Deep Sub (< 90Hz)
            if (centerFreq < 90) {
                boosterBoostDb += getValue(context, KEY_EQUAKE)
            }

            // Bass: (60Hz - 280Hz)
            if (centerFreq in 60..280) {
                boosterBoostDb += getValue(context, KEY_BASS)
            }

            // Anti-Boxy: (FIXED RANGE: 200Hz - 1000Hz)
            // Widened to include standard 250Hz, 500Hz, and 1kHz bands
            if (centerFreq in 200..1000) {
                boosterBoostDb -= getValue(context, KEY_BOXY)
            }

            // Clarity: (1kHz - 4kHz)
            if (centerFreq in 1000..4000) {
                boosterBoostDb += getValue(context, KEY_CLARITY)
            }

            // Definition: (3kHz - 12kHz)
            if (centerFreq in 3000..12000) {
                boosterBoostDb += getValue(context, KEY_DEFINITION)
            }

            // Treble: (> 4kHz)
            if (centerFreq > 4000) {
                boosterBoostDb += getValue(context, KEY_TREBLE)
            }

            // Brightness: (> 10kHz)
            if (centerFreq > 10000) {
                boosterBoostDb += getValue(context, KEY_BRIGHTNESS)
            }

            // 3. Combine
            val boostMb = dbToMb(boosterBoostDb)
            val totalLevel = (baseLevel + boostMb).toShort()

            try {
                eq.setBandLevel(i.toShort(), totalLevel)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}