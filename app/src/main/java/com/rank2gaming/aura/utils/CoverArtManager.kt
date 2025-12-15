package com.rank2gaming.aura.utils

import android.content.Context

object CoverArtManager {
    private const val PREFS_NAME = "AuraCoverArt"

    // Save a custom image URI for a specific song ID
    fun saveCustomArt(context: Context, songId: Long, uri: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("art_$songId", uri)
            .apply()
    }

    // Remove custom art (revert to original)
    fun removeCustomArt(context: Context, songId: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove("art_$songId")
            .apply()
    }

    // Get the custom URI if it exists
    fun getCustomArt(context: Context, songId: Long): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("art_$songId", null)
    }
}