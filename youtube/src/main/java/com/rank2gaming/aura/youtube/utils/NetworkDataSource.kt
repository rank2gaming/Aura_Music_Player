package com.rank2gaming.aura.youtube.utils

import android.util.Log
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class NetworkDataSource {

    private val client = OkHttpClient()
    private val gson = Gson()

    fun fetchTrendingMusicVideos(): List<YouTubeVideoItem> {
        val apiKey = ApiKeyManager.getKey()

        // URL configured for: Music Category (10), Most Popular, India Region (IN)
        val url = "https://www.googleapis.com/youtube/v3/videos?" +
                "part=snippet" +
                "&chart=mostPopular" +
                "&maxResults=20" +
                "&regionCode=IN" +
                "&videoCategoryId=10" +
                "&key=$apiKey"

        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    // --- IMPORTANT DEBUG LOG ---
                    Log.e("NetworkDataSource", "API FAILURE: Code=${response.code}, Body=$responseBody")

                    if (response.code == 403) {
                        throw IOException("API Key Error: Quota exceeded or invalid key.")
                    }
                    throw IOException("Network Error: ${response.code}")
                }

                // Log success to verify data is arriving
                Log.d("NetworkDataSource", "API Success. Body length: ${responseBody.length}")

                val data = gson.fromJson(responseBody, YouTubeResponse::class.java)
                return data.items ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("NetworkDataSource", "Request Failed", e)
            throw e
        }
    }
}