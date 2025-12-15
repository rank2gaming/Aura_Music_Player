package com.rank2gaming.aura.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rank2gaming.aura.R
import com.rank2gaming.aura.databinding.ItemAlbumBinding
import com.rank2gaming.aura.model.Song
import com.rank2gaming.aura.utils.FileUtils

class AlbumAdapter(
    private var albums: List<Song>, // Using Song to represent the Album (first song of album)
    private val onClick: (Long) -> Unit // Returns Album ID
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val binding = ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlbumViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        holder.bind(albums[position])
    }

    override fun getItemCount() = albums.size

    inner class AlbumViewHolder(private val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song) {
            // Note: In a real app, you'd aggregate the song count.
            // Here we display basic info derived from the first song found for this album.
            binding.txtAlbumName.text = song.title // Or lookup Album Name via ID if available
            binding.txtAlbumArtist.text = song.artist

            Glide.with(binding.root)
                .load(FileUtils.getAlbumArtUri(song.albumId))
                .placeholder(R.drawable.ic_music_note)
                .into(binding.imgAlbumCover)

            binding.root.setOnClickListener { onClick(song.albumId) }
        }
    }
}