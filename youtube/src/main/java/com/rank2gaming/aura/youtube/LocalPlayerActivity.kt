package com.rank2gaming.aura.youtube

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.rank2gaming.aura.youtube.databinding.ActivityLocalPlayerBinding
import java.util.Locale

@androidx.annotation.OptIn(UnstableApi::class)
class LocalPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocalPlayerBinding
    private var exoPlayer: ExoPlayer? = null
    private var videoUri: Uri? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isSeeking = false

    private val updateProgressAction = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uriString = intent.getStringExtra("VIDEO_URI")
        if (uriString != null) {
            videoUri = Uri.parse(uriString)
            initializePlayer()
        } else {
            Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show()
            finish()
        }

        setupControls()
        loadBannerAd()
    }

    private fun loadBannerAd() {
        val adView = AdView(this)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test ID
        binding.adContainer.addView(adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer

        val mediaItem = MediaItem.fromUri(videoUri!!)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.playWhenReady = true

        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    binding.seekBar.max = exoPlayer?.duration?.toInt() ?: 0
                    binding.txtTotalTime.text = formatTime(exoPlayer?.duration ?: 0)
                    handler.post(updateProgressAction)
                } else if (playbackState == Player.STATE_ENDED) {
                    handler.removeCallbacks(updateProgressAction)
                    // Use local resource R.drawable.ic_play
                    binding.btnPlayPause.setImageResource(R.drawable.ic_play)
                }
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupControls() {
        binding.playerView.setOnClickListener { toggleControls() }

        binding.btnPlayPause.setOnClickListener {
            if (exoPlayer?.isPlaying == true) {
                exoPlayer?.pause()
                binding.btnPlayPause.setImageResource(R.drawable.ic_play)
            } else {
                exoPlayer?.play()
                binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
            }
        }

        binding.btnBackLocal.setOnClickListener { finish() }

        binding.btnNext.setOnClickListener {
            exoPlayer?.seekTo(exoPlayer!!.currentPosition + 10000)
        }

        binding.btnPrev.setOnClickListener {
            exoPlayer?.seekTo(exoPlayer!!.currentPosition - 10000)
        }

        // --- FIX: Use launchAudioActivity to prevent "Unresolved Reference" ---
        binding.btnEqualizer.setOnClickListener {
            launchAudioActivity("com.rank2gaming.aura.audio.EqualizerSettingsActivity")
        }
        binding.btnHd.setOnClickListener {
            launchAudioActivity("com.rank2gaming.aura.audio.HighDefAudioSettingsActivity")
        }
        binding.btnBooster.setOnClickListener {
            launchAudioActivity("com.rank2gaming.aura.audio.SoundBoosterActivity")
        }

        binding.btnResize.setOnClickListener {
            if (binding.playerView.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                Toast.makeText(this, "Zoom", Toast.LENGTH_SHORT).show()
            } else {
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                Toast.makeText(this, "Fit", Toast.LENGTH_SHORT).show()
            }
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.txtCurrentTime.text = formatTime(progress.toLong())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { isSeeking = true }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isSeeking = false
                exoPlayer?.seekTo(seekBar?.progress?.toLong() ?: 0L)
            }
        })
    }

    private fun launchAudioActivity(className: String) {
        try {
            val intent = Intent().setClassName(this, className)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Audio settings not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleControls() {
        val v = if (binding.topControls.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        binding.topControls.visibility = v
        binding.bottomControlsBg.visibility = v
        // binding.rightControls.visibility = v // Removed as it's inside bottomControlsBg now
    }

    private fun updateProgress() {
        if (!isSeeking) {
            exoPlayer?.let {
                val current = it.currentPosition
                binding.seekBar.progress = current.toInt()
                binding.txtCurrentTime.text = formatTime(current)
            }
        }
    }

    private fun formatTime(ms: Long): String {
        val seconds = ms / 1000
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        exoPlayer?.release()
    }
}