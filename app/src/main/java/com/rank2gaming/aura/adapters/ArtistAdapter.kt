package com.rank2gaming.aura.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rank2gaming.aura.databinding.ItemArtistBinding

// Simple data class for Adapter usage
data class ArtistData(val name: String, val songCount: Int)

class ArtistAdapter(
    private var artists: List<ArtistData>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder>() {

    // Palette matching the screenshot (Slate, Lime, Purple, Pink, Green, Teal, etc.)
    private val avatarColors = listOf(
        0xFF546E7A.toInt(), // Blue Grey
        0xFFAFB42B.toInt(), // Lime/Olive
        0xFF9575CD.toInt(), // Light Purple
        0xFF5E35B1.toInt(), // Deep Purple
        0xFFD81B60.toInt(), // Pinkish Red
        0xFF43A047.toInt(), // Green
        0xFF00897B.toInt(), // Teal
        0xFFEC407A.toInt(), // Rose
        0xFF3949AB.toInt(), // Indigo
        0xFF6D4C41.toInt()  // Brown
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val binding = ItemArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArtistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        holder.bind(artists[position], position)
    }

    override fun getItemCount() = artists.size

    inner class ArtistViewHolder(private val binding: ItemArtistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(artist: ArtistData, position: Int) {
            binding.txtArtistName.text = artist.name

            // Handle singular/plural text
            val songText = if (artist.songCount == 1) "1 Song" else "${artist.songCount} Songs"
            binding.txtArtistCount.text = songText // You can add Album count here if available in data

            // Set Avatar Letter
            val firstLetter = if (artist.name.isNotEmpty()) artist.name.first().uppercase() else "?"
            binding.imgArtistAvatar.text = firstLetter

            // Set Avatar Background Color (Cycling through palette)
            val colorIndex = position % avatarColors.size
            binding.imgArtistAvatar.backgroundTintList = ColorStateList.valueOf(avatarColors[colorIndex])

            binding.root.setOnClickListener { onClick(artist.name) }
        }
    }
}