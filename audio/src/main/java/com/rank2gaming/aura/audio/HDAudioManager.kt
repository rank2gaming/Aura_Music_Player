package com.rank2gaming.aura.audio

import android.content.Context
import android.media.audiofx.LoudnessEnhancer

object HDAudioManager {
    private const val PREFS_NAME = "AuraAudioPrefs"

    const val KEY_HD_ENABLED = "hd_audio_enabled"
    const val KEY_VOLUME_BOOST = "volume_boost"
    const val KEY_TREBLE = "treble_level"
    const val KEY_STEREO_TREBLE = "stereo_treble"
    const val KEY_MONO_AUDIO = "mono_audio"

    private fun getPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isHdEnabled(context: Context): Boolean = getPrefs(context).getBoolean(KEY_HD_ENABLED, false)
    fun setHdEnabled(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean(KEY_HD_ENABLED, enabled).apply()

    fun getVolumeBoost(context: Context): Int = getPrefs(context).getInt(KEY_VOLUME_BOOST, 0)
    fun setVolumeBoost(context: Context, boost: Int) = getPrefs(context).edit().putInt(KEY_VOLUME_BOOST, boost).apply()

    fun getTrebleLevel(context: Context): Int = getPrefs(context).getInt(KEY_TREBLE, 0)
    fun setTrebleLevel(context: Context, level: Int) = getPrefs(context).edit().putInt(KEY_TREBLE, level).apply()

    fun isMonoAudio(context: Context): Boolean = getPrefs(context).getBoolean(KEY_MONO_AUDIO, false)
    fun setMonoAudio(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean(KEY_MONO_AUDIO, enabled).apply()

    fun isStereoTreble(context: Context): Boolean = getPrefs(context).getBoolean(KEY_STEREO_TREBLE, false)
    fun setStereoTreble(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean(KEY_STEREO_TREBLE, enabled).apply()

    fun resetEffects(context: Context) {
        getPrefs(context).edit().putInt(KEY_VOLUME_BOOST, 0).putInt(KEY_TREBLE, 0).putBoolean(KEY_MONO_AUDIO, false).apply()
    }

    fun createLoudnessEnhancer(sessionId: Int): LoudnessEnhancer? = try { LoudnessEnhancer(sessionId).apply { enabled = true } } catch (e: Exception) { null }
}