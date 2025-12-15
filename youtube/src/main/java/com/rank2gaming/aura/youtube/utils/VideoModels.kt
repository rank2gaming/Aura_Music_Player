package com.rank2gaming.aura.youtube.utils

// Response wrapper
data class YouTubeResponse(
    val items: List<YouTubeVideoItem>?
)

// Renamed to avoid conflict with LocalVideoAdapter's VideoItem
data class YouTubeVideoItem(
    val id: String,
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