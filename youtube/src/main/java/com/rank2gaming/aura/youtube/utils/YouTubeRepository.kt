package com.rank2gaming.aura.youtube.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Acts as a mediator between the data source and the UI.
 * In the future, this class can handle caching (switching between Network and Local DB).
 */
class YouTubeRepository(
    private val networkDataSource: NetworkDataSource = NetworkDataSource()
) {

    // We make this a suspend function to ensure safety and allow switching contexts
    suspend fun getMusicVideos(): List<YouTubeVideoItem> {
        return withContext(Dispatchers.IO) {
            // Simply delegate the fetching to the data source
            networkDataSource.fetchTrendingMusicVideos()
        }
    }
}