package com.rank2gaming.aura.adapters

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rank2gaming.aura.R
import com.rank2gaming.aura.databinding.ItemSongBinding
import com.rank2gaming.aura.model.Song
import com.rank2gaming.aura.utils.CoverArtManager
import com.rank2gaming.aura.utils.FileUtils
import com.rank2gaming.aura.utils.ItemAnimation
import com.rank2gaming.aura.utils.ThemeManager

class SongAdapter(
    private var songs: List<Song>,
    private val context: Context,
    private val onClick: (Song) -> Unit,
    private val onMenuClick: (Song, android.view.View) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private var currentSongId: Long = -1
    private var isPlaying: Boolean = false
    private var lastPosition = -1

    // 10 Fixed Colors for Cover Art Background
    private val coverColors = listOf(
        0xFFE57373.toInt(), 0xFFBA68C8.toInt(), 0xFF64B5F6.toInt(), 0xFF4DB6AC.toInt(),
        0xFFFFF176.toInt(), 0xFFFFB74D.toInt(), 0xFFA1887F.toInt(), 0xFF90A4AE.toInt(),
        0xFF9575CD.toInt(), 0xFF4DD0E1.toInt()
    )

    // 5 Animation Colors
    private val animColors = listOf(
        0xFF00E676.toInt(), 0xFFFF1744.toInt(), 0xFF2979FF.toInt(),
        0xFFFFEA00.toInt(), 0xFFD500F9.toInt()
    )

    fun updateList(newSongs: List<Song>) {
        songs = newSongs
        notifyDataSetChanged()
    }

    // Efficiently update only the rows that changed state
    fun updatePlayingStatus(songId: Long, playing: Boolean) {
        val prevId = currentSongId
        currentSongId = songId
        isPlaying = playing

        val prevIndex = songs.indexOfFirst { it.id == prevId }
        val currIndex = songs.indexOfFirst { it.id == currentSongId }

        if (prevIndex != -1) notifyItemChanged(prevIndex, "payload_status")
        if (currIndex != -1) notifyItemChanged(currIndex, "payload_status")

        // Fallback if indices not found (e.g., first load)
        if (prevIndex == -1 && currIndex == -1) notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.bind(song, position)
        // Scroll Animation
        lastPosition = ItemAnimation.animate(holder.itemView, position, lastPosition)
    }

    // Partial update bind (avoids reloading image)
    override fun onBindViewHolder(holder: SongViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            holder.updateStatus(songs[position], position)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onViewDetachedFromWindow(holder: SongViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.stopAnimation()
        holder.itemView.clearAnimation()
    }

    override fun getItemCount() = songs.size

    inner class SongViewHolder(private val binding: ItemSongBinding) : RecyclerView.ViewHolder(binding.root) {

        private var spillAnimator: ObjectAnimator? = null

        fun bind(song: Song, position: Int) {
            binding.textTitle.text = song.title
            binding.textArtist.text = song.artist
            binding.textDuration.text = FileUtils.formatDuration(song.duration)

            // 1. Set Background Color (One of 10)
            val colorIndex = position % 10
            binding.imgAlbumArt.setBackgroundColor(coverColors[colorIndex])

            // 2. Load Image (Check Custom Art -> Default Art -> Fallback Icon)
            val customArt = CoverArtManager.getCustomArt(context, song.id)
            val loadObj = if (customArt != null) Uri.parse(customArt) else FileUtils.getAlbumArtUri(song.albumId)

            Glide.with(binding.root)
                .load(loadObj)
                .circleCrop()
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .fallback(R.drawable.ic_music_note)
                .into(binding.imgAlbumArt)

            // 3. Highlight Selected Song Logic
            updateStatus(song, position)

            binding.root.setOnClickListener { onClick(song) }
            binding.btnMenu.setOnClickListener { onMenuClick(song, it) }
        }

        fun updateStatus(song: Song, position: Int) {
            if (song.id == currentSongId) {
                val highlightColor = getHighlightColorForTheme()
                binding.textTitle.setTextColor(highlightColor)
                binding.textArtist.setTextColor(Color.WHITE)
                binding.viewAnimation.visibility = View.VISIBLE

                val animColorIndex = position % 5
                binding.viewAnimation.backgroundTintList = ColorStateList.valueOf(animColors[animColorIndex])

                if (isPlaying) startSpillAnimation(binding.viewAnimation)
                else stopSpillAnimation(binding.viewAnimation)

            } else {
                binding.textTitle.setTextColor(Color.WHITE)
                binding.textArtist.setTextColor(Color.parseColor("#BBBBBB"))
                binding.viewAnimation.visibility = View.GONE
                stopSpillAnimation(binding.viewAnimation)
            }
        }

        private fun startSpillAnimation(view: View) {
            if (spillAnimator == null || !spillAnimator!!.isRunning) {
                val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.5f)
                val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.5f)
                val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.5f)

                spillAnimator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, alpha).apply {
                    duration = 600
                    repeatCount = ObjectAnimator.INFINITE
                    repeatMode = ObjectAnimator.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
            }
        }

        private fun stopSpillAnimation(view: View) {
            spillAnimator?.cancel()
            view.scaleX = 1f
            view.scaleY = 1f
            view.alpha = 1f
        }

        fun stopAnimation() {
            spillAnimator?.cancel()
        }

        private fun getHighlightColorForTheme(): Int {
            val themeIndex = ThemeManager.getThemeIndex(context)
            // Provide contrasting colors for themes
            return when (themeIndex) {
                0 -> 0xFF00E676.toInt() // Green
                1 -> 0xFF40C4FF.toInt() // Blue
                2 -> 0xFFFFEA00.toInt() // Yellow
                else -> 0xFF00E676.toInt()
            }
        }
    }
}