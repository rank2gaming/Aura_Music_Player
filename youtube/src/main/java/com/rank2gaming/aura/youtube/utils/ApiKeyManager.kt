package com.rank2gaming.aura.youtube.utils

object ApiKeyManager {
    // Verified working for fetching list
    private const val API_KEY = "AIzaSyBKFhF8T6c3RBzWCSo0kwxaX0Q0JcsV1tQ"

    fun getKey(): String {
        return API_KEY
    }
}