package com.rank2gaming.aura.utils

import com.rank2gaming.aura.model.Song
import java.util.Locale

object SearchManager {
    fun filterSongs(originalList: List<Song>, query: String?): List<Song> {
        if (query.isNullOrEmpty()) return originalList
        val lowerCaseQuery = query.lowercase(Locale.getDefault())
        return originalList.filter { song ->
            song.title.lowercase().contains(lowerCaseQuery) ||
                    song.artist.lowercase().contains(lowerCaseQuery)
        }
    }
}