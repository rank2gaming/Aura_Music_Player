package com.rank2gaming.aura.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val path: String,
    val duration: Long,
    val albumId: Long,
    val folderPath: String = ""
) : Parcelable