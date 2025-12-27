package com.rank2gaming.aura.youtube

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Bundle
import android.view.*
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.rank2gaming.aura.youtube.databinding.ActivityYoutubePlayerBinding
import java.util.Locale
import kotlin.math.abs

class YouTubePlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityYoutubePlayerBinding
    private var youTubePlayer: YouTubePlayer? = null
    private var videoId: String = ""
    private var isFullscreen = false
    private var currentDuration = 0f
    private var isPlaying = false // Track playback state

    // Gestures
    private var deviceWidth = 0
    private var audioManager: AudioManager? = null
    private var startY = 0f
    private var startX = 0f
    private var isVolumeGesture = false
    private var isBrightnessGesture = false
    private var isSeeking = false
    private var initialTouchVolume = 0
    private var initialTouchBrightness = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYoutubePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoId = intent.getStringExtra("VIDEO_ID") ?: ""
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        deviceWidth = resources.displayMetrics.widthPixels

        setupYouTubePlayer()
        setupControls()
        setupGestures()
        loadBannerAd()
    }

    private fun loadBannerAd() {
        val adView = AdView(this)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test ID
        binding.adContainer.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    private fun setupYouTubePlayer() {
        lifecycle.addObserver(binding.youtubePlayerView)

        val options = IFramePlayerOptions.Builder().controls(0).build()
        binding.youtubePlayerView.initialize(object : AbstractYouTubePlayerListener() {
            override fun onReady(player: YouTubePlayer) {
                youTubePlayer = player
                if (videoId.isNotEmpty()) player.loadVideo(videoId, 0f)
            }

            override fun onStateChange(player: YouTubePlayer, state: PlayerConstants.PlayerState) {
                if (state == PlayerConstants.PlayerState.PLAYING) {
                    isPlaying = true
                    binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
                } else if (state == PlayerConstants.PlayerState.PAUSED || state == PlayerConstants.PlayerState.ENDED) {
                    isPlaying = false
                    binding.btnPlayPause.setImageResource(R.drawable.ic_play)
                }
            }

            override fun onCurrentSecond(player: YouTubePlayer, second: Float) {
                if (!isSeeking) {
                    binding.seekBar.progress = second.toInt()
                    binding.txtCurrentTime.text = formatTime(second)
                }
            }

            override fun onVideoDuration(player: YouTubePlayer, duration: Float) {
                currentDuration = duration
                binding.seekBar.max = duration.toInt()
                binding.txtTotalTime.text = formatTime(duration)
            }
        }, options)
    }

    private fun setupControls() {
        binding.btnBackPlayer.setOnClickListener { finish() }

        binding.btnPlayPause.setOnClickListener {
            if (isPlaying) {
                youTubePlayer?.pause()
            } else {
                youTubePlayer?.play()
            }
        }

        // Use setClassName to avoid circular dependency with :audio module
        binding.btnEqualizer.setOnClickListener { launchAudioActivity("com.rank2gaming.aura.audio.EqualizerSettingsActivity") }
        binding.btnHd.setOnClickListener { launchAudioActivity("com.rank2gaming.aura.audio.HighDefAudioSettingsActivity") }
        binding.btnBooster.setOnClickListener { launchAudioActivity("com.rank2gaming.aura.audio.SoundBoosterActivity") }

        binding.btnFullscreen.setOnClickListener { toggleFullscreen() }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.txtCurrentTime.text = formatTime(progress.toFloat())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { isSeeking = true }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isSeeking = false
                youTubePlayer?.seekTo(seekBar?.progress?.toFloat() ?: 0f)
            }
        })
    }

    private fun launchAudioActivity(className: String) {
        try {
            val intent = Intent().setClassName(this, className)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace() // Log error if module is missing
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        binding.touchOverlay.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.y
                    startX = event.x
                    initialTouchVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                    initialTouchBrightness = window.attributes.screenBrightness
                    if (initialTouchBrightness < 0) initialTouchBrightness = 0.5f
                    isVolumeGesture = false
                    isBrightnessGesture = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = startY - event.y
                    val deltaX = event.x - startX

                    if (abs(deltaY) > abs(deltaX) && abs(deltaY) > 50) {
                        if (startX > deviceWidth / 2) {
                            // Right side: Volume
                            isVolumeGesture = true
                            adjustVolume(deltaY)
                        } else {
                            // Left side: Brightness
                            isBrightnessGesture = true
                            adjustBrightness(deltaY)
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    binding.txtGestureIndicator.visibility = View.GONE
                    if (!isVolumeGesture && !isBrightnessGesture && abs(event.x - startX) < 20 && abs(event.y - startY) < 20) {
                        toggleControlsVisibility()
                    }
                }
            }
            true
        }
    }

    private fun adjustVolume(deltaY: Float) {
        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val change = (deltaY / 500f * maxVol).toInt()
        val newVol = (initialTouchVolume + change).coerceIn(0, maxVol)
        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
        showGestureIndicator("Vol: ${(newVol * 100 / maxVol)}%")
    }

    private fun adjustBrightness(deltaY: Float) {
        val change = deltaY / 1000f
        val newBright = (initialTouchBrightness + change).coerceIn(0.01f, 1.0f)
        val lp = window.attributes
        lp.screenBrightness = newBright
        window.attributes = lp
        showGestureIndicator("Bright: ${(newBright * 100).toInt()}%")
    }

    private fun showGestureIndicator(text: String) {
        binding.txtGestureIndicator.text = text
        binding.txtGestureIndicator.visibility = View.VISIBLE
    }

    private fun toggleControlsVisibility() {
        val isVisible = binding.rightControlsPanel.isVisible
        binding.rightControlsPanel.isVisible = !isVisible
        binding.seekBarContainer.isVisible = !isVisible
        binding.btnBackPlayer.isVisible = !isVisible
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            WindowCompat.getInsetsController(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
        }
        deviceWidth = resources.displayMetrics.widthPixels
    }

    private fun formatTime(seconds: Float): String {
        val s = seconds.toInt()
        val m = s / 60
        val sec = s % 60
        return String.format(Locale.US, "%02d:%02d", m, sec)
    }
}