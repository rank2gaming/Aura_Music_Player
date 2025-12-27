package com.rank2gaming.aura.youtube.utils

// Wrapper
data class YouTubeResponse(
    val items: List<YouTubeVideoItem>?
)

// Item
data class YouTubeVideoItem(
    val id: String, // Correct for 'videos' endpoint. (If using 'search', this would need to be an Object)
    val snippet: Snippet
)

data class Snippet(
    val title: String,
    val channelTitle: String,
    val thumbnails: Thumbnails
)

data class Thumbnails(
    val medium: ThumbnailUrl,
    val high: ThumbnailUrl
)

data class ThumbnailUrl(
    val url: String
)