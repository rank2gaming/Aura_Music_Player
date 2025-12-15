package com.rank2gaming.aura.playbackbuttons

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.rank2gaming.aura.playbackbuttons.databinding.ViewPlaybackControlsBinding

class PlaybackControlView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewPlaybackControlsBinding =
        ViewPlaybackControlsBinding.inflate(LayoutInflater.from(context), this, true)

    var onPlayPauseClick: (() -> Unit)? = null
    var onNextClick: (() -> Unit)? = null
    var onPrevClick: (() -> Unit)? = null
    var onShuffleClick: (() -> Unit)? = null
    var onRepeatClick: (() -> Unit)? = null

    init {
        binding.btnPlayPause.setOnClickListener { onPlayPauseClick?.invoke() }
        binding.btnNext.setOnClickListener { onNextClick?.invoke() }
        binding.btnPrev.setOnClickListener { onPrevClick?.invoke() }
        binding.btnShuffle.setOnClickListener { onShuffleClick?.invoke() }
        binding.btnRepeat.setOnClickListener { onRepeatClick?.invoke() }
    }

    fun setPlayingState(isPlaying: Boolean) {
        val iconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        binding.btnPlayPause.setImageResource(iconRes)
    }

    fun setShuffleState(isEnabled: Boolean) {
        val color = if (isEnabled) R.color.teal_200 else android.R.color.white
        binding.btnShuffle.setColorFilter(ContextCompat.getColor(context, color))
    }

    fun setRepeatState(isEnabled: Boolean) {
        val color = if (isEnabled) R.color.teal_200 else android.R.color.white
        binding.btnRepeat.setColorFilter(ContextCompat.getColor(context, color))
    }

    fun setButtonTint(color: Int) {
        binding.btnNext.setColorFilter(color)
        binding.btnPrev.setColorFilter(color)
        binding.btnPlayPause.setColorFilter(color)
        // Shuffle/Repeat are usually managed by state, but default can be set if disabled
        if (binding.btnShuffle.colorFilter == null) binding.btnShuffle.setColorFilter(color)
        if (binding.btnRepeat.colorFilter == null) binding.btnRepeat.setColorFilter(color)
    }
}