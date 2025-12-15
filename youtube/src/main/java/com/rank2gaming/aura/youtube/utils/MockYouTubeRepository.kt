package com.rank2gaming.aura.youtube.utils

class MockYouTubeRepository {

    fun getMockVideos(): List<YouTubeVideoItem> {
        val mockList = ArrayList<YouTubeVideoItem>()

        // Add 5 fake videos for testing
        mockList.add(createMockVideo("vid01", "Test Song 1", "Artist A", "https://img.youtube.com/vi/dQw4w9WgXcQ/mqdefault.jpg"))
        mockList.add(createMockVideo("vid02", "Test Song 2", "Artist B", "https://img.youtube.com/vi/kJQP7kiw5Fk/mqdefault.jpg"))
        mockList.add(createMockVideo("vid03", "Test Song 3", "Artist C", "https://img.youtube.com/vi/9bZkp7q19f0/mqdefault.jpg"))
        mockList.add(createMockVideo("vid04", "Test Song 4", "Artist D", "https://img.youtube.com/vi/JGwWNGJdvx8/mqdefault.jpg"))
        mockList.add(createMockVideo("vid05", "Test Song 5", "Artist E", "https://img.youtube.com/vi/fJ9rUzIMcZQ/mqdefault.jpg"))

        return mockList
    }

    private fun createMockVideo(id: String, title: String, channel: String, thumbUrl: String): YouTubeVideoItem {
        val thumbnail = ThumbnailUrl(thumbUrl)
        val thumbs = Thumbnails(thumbnail, thumbnail) // Use same for med/high
        val snippet = Snippet(title, channel, thumbs)
        return YouTubeVideoItem(id, snippet)
    }
}