package com.rank2gaming.aura.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PlaylistManager {
    private const val PREF_NAME = "AuraPlaylists"
    private const val KEY_USER_PLAYLISTS = "user_playlists_map" // Map<String, List<Long>>

    // Create a new empty playlist
    fun createPlaylist(context: Context, name: String) {
        val playlists = getPlaylists(context).toMutableMap()
        if (!playlists.containsKey(name)) {
            playlists[name] = emptyList()
            savePlaylists(context, playlists)
        }
    }

    // Delete a playlist
    fun deletePlaylist(context: Context, name: String) {
        val playlists = getPlaylists(context).toMutableMap()
        playlists.remove(name)
        savePlaylists(context, playlists)
    }

    // Add a song to a specific playlist
    fun addSongToPlaylist(context: Context, playlistName: String, songId: Long) {
        val playlists = getPlaylists(context).toMutableMap()
        val currentList = playlists[playlistName]?.toMutableList() ?: mutableListOf()
        if (!currentList.contains(songId)) {
            currentList.add(songId)
            playlists[playlistName] = currentList
            savePlaylists(context, playlists)
        }
    }

    // Remove a song from a playlist
    fun removeSongFromPlaylist(context: Context, playlistName: String, songId: Long) {
        val playlists = getPlaylists(context).toMutableMap()
        val currentList = playlists[playlistName]?.toMutableList() ?: return
        currentList.remove(songId)
        playlists[playlistName] = currentList
        savePlaylists(context, playlists)
    }

    // Get all playlists
    fun getPlaylists(context: Context): Map<String, List<Long>> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_USER_PLAYLISTS, "{}")
        val type = object : TypeToken<Map<String, List<Long>>>() {}.type
        return Gson().fromJson(json, type)
    }

    // Save playlists to SharedPreferences
    private fun savePlaylists(context: Context, playlists: Map<String, List<Long>>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(playlists)
        prefs.edit().putString(KEY_USER_PLAYLISTS, json).apply()
    }
}