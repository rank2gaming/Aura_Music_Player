package com.rank2gaming.aura.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.util.Locale

object FormatUtils {

    private val extensionMap = mapOf(
        "mp3" to "audio/mpeg",
        "m4a" to "audio/mp4",
        "flac" to "audio/flac",
        "wav" to "audio/wav",
        "ogg" to "audio/ogg",
        "opus" to "audio/opus",
        "aac" to "audio/aac",
        "wma" to "audio/x-ms-wma",
        "aiff" to "audio/x-aiff",
        "alac" to "audio/alac",
        "ape" to "audio/x-ape"
    )

    fun getMimeType(context: Context, uri: Uri): String {
        var mimeType: String? = null
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            mimeType = context.contentResolver.getType(uri)
        }
        if (mimeType == null) {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())?.lowercase(Locale.ROOT)
                ?: uri.path?.substringAfterLast('.', "")?.lowercase(Locale.ROOT)

            if (!extension.isNullOrEmpty()) {
                mimeType = extensionMap[extension] ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            }
        }
        return mimeType ?: "audio/*"
    }

    fun getAlbumArtUri(albumId: Long): Uri {
        return android.content.ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )
    }

    fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}