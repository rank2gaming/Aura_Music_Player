package com.rank2gaming.aura.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.util.Locale

object FormatUtils {

    // Custom map for formats Android sometimes misses
    private val extensionMap = mapOf(
        "flac" to "audio/flac",
        "opus" to "audio/opus",
        "ogg" to "audio/ogg",
        "oga" to "audio/ogg",
        "m4a" to "audio/mp4",
        "wma" to "audio/x-ms-wma",
        "ape" to "audio/x-ape",
        "alac" to "audio/alac",
        "aiff" to "audio/x-aiff",
        "wav" to "audio/wav"
    )

    fun getMimeType(context: Context, uri: Uri): String {
        var mimeType: String? = null

        // 1. Try ContentResolver
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            mimeType = context.contentResolver.getType(uri)
        }

        // 2. Try File Extension
        if (mimeType == null) {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())?.lowercase(Locale.ROOT)
                ?: uri.path?.substringAfterLast('.', "")?.lowercase(Locale.ROOT)

            if (!extension.isNullOrEmpty()) {
                mimeType = extensionMap[extension]
                    ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            }
        }

        // 3. Default fallback
        return mimeType ?: "audio/*"
    }
}