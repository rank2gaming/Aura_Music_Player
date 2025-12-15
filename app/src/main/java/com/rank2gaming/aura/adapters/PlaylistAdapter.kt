package com.rank2gaming.aura.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rank2gaming.aura.R
import com.rank2gaming.aura.databinding.ItemPlaylistBinding
import com.rank2gaming.aura.databinding.ItemPlaylistHeaderBinding // Auto-generated from xml above

data class PlaylistData(
    val name: String,
    val songCount: Int,
    val type: Int
)

class PlaylistAdapter(
    private var playlists: List<PlaylistData>,
    private val onPlaylistClick: (String) -> Unit,
    private val onHeaderAction: (Int, View) -> Unit, // 0 for Add, 1 for Menu
    private val onUserPlaylistMenu: (String, View) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER_TOP = 0
        const val TYPE_SMART = 1
        const val TYPE_HEADER_SECTION = 2
        const val TYPE_USER = 3
    }

    override fun getItemViewType(position: Int): Int {
        return playlists[position].type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER_TOP -> {
                val binding = ItemPlaylistHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HeaderTopViewHolder(binding)
            }
            TYPE_HEADER_SECTION -> {
                val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
                view.setPadding(48, 16, 16, 0) // Padding left 48px ~ 16dp
                val text = view.findViewById<TextView>(android.R.id.text1)
                text.setTextColor(Color.parseColor("#BBBBBB"))
                text.textSize = 14f
                SectionViewHolder(view)
            }
            else -> { // SMART or USER
                val binding = ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PlaylistViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = playlists[position]
        when (holder) {
            is HeaderTopViewHolder -> holder.bind(item)
            is SectionViewHolder -> (holder.itemView as TextView).text = item.name
            is PlaylistViewHolder -> holder.bind(item)
        }
    }

    override fun getItemCount() = playlists.size

    // --- View Holders ---

    inner class HeaderTopViewHolder(private val binding: ItemPlaylistHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PlaylistData) {
            binding.txtHeaderCount.text = item.name // "X Playlists"
            binding.btnAddPlaylist.setOnClickListener { onHeaderAction(0, it) }
            binding.btnHeaderMenu.setOnClickListener { onHeaderAction(1, it) }
        }
    }

    inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class PlaylistViewHolder(private val binding: ItemPlaylistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PlaylistData) {
            binding.txtPlaylistName.text = item.name
            binding.txtSongCount.text = if (item.songCount == 1) "1 song" else "${item.songCount} songs"

            // Hide/Show Menu based on type
            if (item.type == TYPE_SMART) {
                binding.btnMenu.visibility = View.VISIBLE // Design shows dots even for smart playlists
            } else {
                binding.btnMenu.visibility = View.VISIBLE
            }

            // Design & Colors based on Name (to match screenshot)
            when (item.name) {
                "My favourite" -> {
                    binding.imgIcon.setImageResource(R.drawable.ic_favorite)
                    binding.cardIcon.setCardBackgroundColor(Color.parseColor("#4285F4")) // Blue
                }
                "Recently added" -> {
                    binding.imgIcon.setImageResource(R.drawable.ic_music_note)
                    binding.cardIcon.setCardBackgroundColor(Color.parseColor("#00E676")) // Green
                }
                "Recently played" -> {
                    binding.imgIcon.setImageResource(R.drawable.ic_history) // Need ic_pie_chart if possible
                    binding.cardIcon.setCardBackgroundColor(Color.parseColor("#AA00FF")) // Purple
                }
                "My top tracks" -> {
                    binding.imgIcon.setImageResource(R.drawable.ic_star) // Need ic_fire
                    binding.cardIcon.setCardBackgroundColor(Color.parseColor("#FF6D00")) // Orange
                }
                else -> {
                    // User Playlists - Uses Cover Art logic usually, or default icon
                    binding.imgIcon.setImageResource(R.drawable.ic_music_note)
                    binding.cardIcon.setCardBackgroundColor(Color.parseColor("#333333")) // Dark
                    // If you have album art for playlist, load it here using Glide
                }
            }

            binding.root.setOnClickListener { onPlaylistClick(item.name) }
            binding.btnMenu.setOnClickListener {
                if (item.type == TYPE_USER) onUserPlaylistMenu(item.name, it)
                else { /* Smart playlist menu actions if any */ }
            }
        }
    }
}