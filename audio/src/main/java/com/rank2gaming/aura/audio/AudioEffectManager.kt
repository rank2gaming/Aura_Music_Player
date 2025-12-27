@file:Suppress("DEPRECATION") // Fixes warnings for Virtualizer

package com.rank2gaming.aura.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log

object AudioEffectManager {
    private const val TAG = "AudioEffectManager"
    private const val PREFS_NAME = "AuraAudioPrefs"

    const val KEY_EQ_ENABLED = "eq_enabled"
    const val KEY_BASS_BOOST = "bass_boost"
    const val KEY_VIRTUALIZER = "virtualizer_strength"
    const val KEY_EQ_PRESET = "eq_preset"
    const val KEY_HEADPHONE_TYPE = "headphone_type"

    private fun getPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEqEnabled(context: Context): Boolean = getPrefs(context).getBoolean(KEY_EQ_ENABLED, false)
    fun setEqEnabled(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean(KEY_EQ_ENABLED, enabled).apply()

    fun getBassStrength(context: Context): Int = getPrefs(context).getInt(KEY_BASS_BOOST, 0)
    fun saveBassStrength(context: Context, strength: Int) = getPrefs(context).edit().putInt(KEY_BASS_BOOST, strength).apply()

    fun getVirtualizerStrength(context: Context): Int = getPrefs(context).getInt(KEY_VIRTUALIZER, 0)
    fun saveVirtualizerStrength(context: Context, strength: Int) = getPrefs(context).edit().putInt(KEY_VIRTUALIZER, strength).apply()

    fun getBandLevel(context: Context, band: Short): Int = getPrefs(context).getInt("eq_band_$band", 0)
    fun saveBandLevel(context: Context, band: Short, level: Int) = getPrefs(context).edit().putInt("eq_band_$band", level).apply()

    fun getPreset(context: Context): Int = getPrefs(context).getInt(KEY_EQ_PRESET, 0)
    fun savePreset(context: Context, preset: Int) = getPrefs(context).edit().putInt(KEY_EQ_PRESET, preset).apply()

    fun getHeadphoneType(context: Context): Int = getPrefs(context).getInt(KEY_HEADPHONE_TYPE, 0)
    fun setHeadphoneType(context: Context, type: Int) = getPrefs(context).edit().putInt(KEY_HEADPHONE_TYPE, type).apply()

    // Hardware Init
    fun createEqualizer(sessionId: Int): Equalizer? = try { Equalizer(0, sessionId).apply { enabled = true } } catch (e: Exception) { null }

    fun createBassBoost(sessionId: Int): BassBoost? = try { BassBoost(0, sessionId).apply { enabled = true } } catch (e: Exception) { null }

    fun createVirtualizer(sessionId: Int): Virtualizer? = try { Virtualizer(0, sessionId).apply { enabled = true } } catch (e: Exception) { null }
}