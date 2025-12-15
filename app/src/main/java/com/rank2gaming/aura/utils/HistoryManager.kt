package com.rank2gaming.aura.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object HistoryManager {
    private const val PREFS_NAME = "AuraHistory"
    private const val KEY_RECENT_IDS = "recent_played_ids"
    private const val MAX_HISTORY = 50 // Limit history to last 50 songs

    // Save a song ID to history (pushes to top, removes duplicates)
    fun addSongToHistory(context: Context, songId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_RECENT_IDS, "[]")
        val type = object : TypeToken<ArrayList<Long>>() {}.type
        val list: ArrayList<Long> = Gson().fromJson(json, type) ?: ArrayList()

        // Remove if existing (so it moves to top)
        list.remove(songId)
        // Add to beginning
        list.add(0, songId)

        // Trim size
        if (list.size > MAX_HISTORY) {
            list.subList(MAX_HISTORY, list.size).clear()
        }

        // Save back
        prefs.edit().putString(KEY_RECENT_IDS, Gson().toJson(list)).apply()
    }

    // Get the list of IDs
    fun getRecentSongIds(context: Context): List<Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_RECENT_IDS, "[]")
        val type = object : TypeToken<ArrayList<Long>>() {}.type
        return Gson().fromJson(json, type) ?: ArrayList()
    }
}