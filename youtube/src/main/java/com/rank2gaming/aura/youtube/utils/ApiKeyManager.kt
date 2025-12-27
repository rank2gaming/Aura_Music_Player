package com.rank2gaming.aura.youtube.utils

object ApiKeyManager {
    // REPLACE THIS with your new key from Google Cloud Console
    private const val API_KEY = "AIzaSyBKFhF8T6c3RBzWCSo0kwxaX0Q0JcsV1tQ"

    fun getKey(): String {
        return API_KEY
    }
}