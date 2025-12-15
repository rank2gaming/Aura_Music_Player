package com.rank2gaming.aura.utils

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {

    private const val PREFS_NAME = "AuraAudioPrefs"

    // --- WATER DETECTOR KEYS ---
    const val KEY_WATER_DETECT_ENABLED = "water_detect_enabled"
    const val KEY_WATER_NOTIF_ENABLED = "water_notif_enabled"

    // --- RESUME STATE KEYS ---
    const val KEY_LAST_SONG_ID = "last_song_id"
    const val KEY_LAST_SONG_POS = "last_song_pos"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getLastSongId(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_SONG_ID, -1L)
    }

    fun setLastSongId(context: Context, id: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_SONG_ID, id).apply()
    }

    fun getLastSongPos(context: Context): Int {
        return getPrefs(context).getInt(KEY_LAST_SONG_POS, 0)
    }

    fun setLastSongPos(context: Context, pos: Int) {
        getPrefs(context).edit().putInt(KEY_LAST_SONG_POS, pos).apply()
    }

    fun isWaterDetectEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WATER_DETECT_ENABLED, false)
    }

    fun setWaterDetectEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_WATER_DETECT_ENABLED, enabled).apply()
    }

    fun isWaterNotifEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WATER_NOTIF_ENABLED, false)
    }

    fun setWaterNotifEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_WATER_NOTIF_ENABLED, enabled).apply()
    }
}