package com.rank2gaming.aura.youtube.utils

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rank2gaming.aura.youtube.databinding.ItemYoutubeVideoBinding

// Explicit class for Local Videos
data class LocalVideoItem(
    val uri: Uri,
    val name: String,
    val duration: Long
)

class LocalVideoAdapter(
    private val videos: List<LocalVideoItem>,
    private val onClick: (Uri) -> Unit
) : RecyclerView.Adapter<LocalVideoAdapter.LocalViewHolder>() {

    // Reusing the YouTube card layout for consistency
    class LocalViewHolder(val binding: ItemYoutubeVideoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocalViewHolder {
        val binding = ItemYoutubeVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LocalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LocalViewHolder, position: Int) {
        val video = videos[position]

        holder.binding.txtTitle.text = video.name
        holder.binding.txtChannel.text = formatDuration(video.duration)

        // Load thumbnail using Glide
        Glide.with(holder.itemView.context)
            .load(video.uri)
            .into(holder.binding.imgThumbnail) // XML handles scaleType="centerCrop"

        holder.itemView.setOnClickListener {
            onClick(video.uri)
        }
    }

    override fun getItemCount() = videos.size

    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}