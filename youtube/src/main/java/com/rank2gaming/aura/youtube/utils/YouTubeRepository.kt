package com.rank2gaming.aura.youtube.utils

import android.util.Log
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class YouTubeRepository {

    private val client = OkHttpClient()
    private val gson = Gson()

    fun getMusicVideos(): List<YouTubeVideoItem> {
        val apiKey = ApiKeyManager.getKey()

        val url = "https://www.googleapis.com/youtube/v3/videos?" +
                "part=snippet" +
                "&chart=mostPopular" +
                "&maxResults=20" +
                "&regionCode=IN" +
                "&videoCategoryId=10" +
                "&key=$apiKey"

        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("YouTubeRepo", "API Error: ${response.code} - $responseBody")
                if (response.code == 403) throw IOException("Access Denied (403). Check Key.")
                throw IOException("Network Error: ${response.code}")
            }

            try {
                val data = gson.fromJson(responseBody, YouTubeResponse::class.java)
                return data.items ?: emptyList()
            } catch (e: Exception) {
                Log.e("YouTubeRepo", "Parse Error: ${e.message}")
                return emptyList()
            }
        }
    }
}