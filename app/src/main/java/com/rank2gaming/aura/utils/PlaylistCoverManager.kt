package com.rank2gaming.aura.utils

import android.content.Context

object PlaylistCoverManager {
    private const val PREFS_NAME = "AuraPlaylistCovers"

    fun saveCover(context: Context, playlistName: String, uri: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("cover_$playlistName", uri)
            .apply()
    }

    fun getCover(context: Context, playlistName: String): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("cover_$playlistName", null)
    }

    fun removeCover(context: Context, playlistName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove("cover_$playlistName")
            .apply()
    }
}