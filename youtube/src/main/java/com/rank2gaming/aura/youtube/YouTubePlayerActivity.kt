package com.rank2gaming.aura.youtube

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Bundle
import android.view.*
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.rank2gaming.aura.audio.EqualizerSettingsActivity
import com.rank2gaming.aura.audio.HighDefAudioSettingsActivity
import com.rank2gaming.aura.audio.SoundBoosterActivity
import com.rank2gaming.aura.youtube.databinding.ActivityYoutubePlayerBinding
import kotlin.math.abs

class YouTubePlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityYoutubePlayerBinding
    private var youTubePlayer: YouTubePlayer? = null
    private var isFullscreen = false
    private var isPlaying = true
    private var duration = 0f

    private var audioManager: AudioManager? = null
    private var deviceWidth = 0
    private var maxVolume = 0
    private var currentVolume = 0
    private var startY = 0f
    private var startX = 0f
    private var isVolumeGesture = false
    private var isBrightnessGesture = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYoutubePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val videoId = intent.getStringExtra("VIDEO_ID") ?: ""
        if (videoId.isEmpty()) {
            Toast.makeText(this, "Video Unavailable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        deviceWidth = resources.displayMetrics.widthPixels

        // BACK BUTTON
        binding.btnBackPlayer.setOnClickListener { finish() }

        initPlayer(videoId)
        loadAds()
        setupControls()
        setupGestures()
    }

    private fun initPlayer(videoId: String) {
        lifecycle.addObserver(binding.youtubePlayerView)
        val options = IFramePlayerOptions.Builder().controls(0).build()

        binding.youtubePlayerView.initialize(object : AbstractYouTubePlayerListener() {
            override fun onReady(player: YouTubePlayer) {
                youTubePlayer = player
                player.loadVideo(videoId, 0f)
            }
            override fun onStateChange(player: YouTubePlayer, state: PlayerConstants.PlayerState) {
                isPlaying = state == PlayerConstants.PlayerState.PLAYING
                binding.btnPlayPause.setImageResource(if (isPlaying) com.rank2gaming.aura.audio.R.drawable.ic_pause else com.rank2gaming.aura.audio.R.drawable.ic_play)
            }
            override fun onVideoDuration(player: YouTubePlayer, dur: Float) {
                duration = dur
                binding.seekBar.max = dur.toInt()
                binding.txtTotalTime.text = formatTime(dur)
            }
            override fun onCurrentSecond(player: YouTubePlayer, second: Float) {
                if (!isSeeking) {
                    binding.seekBar.progress = second.toInt()
                    binding.txtCurrentTime.text = formatTime(second)
                }
            }
        }, options)
    }

    private fun loadAds() {
        try {
            val adView = AdView(this)
            adView.setAdSize(AdSize.BANNER)
            adView.adUnitId = "ca-app-pub-3940256099942544/6300978111"
            binding.adContainer.addView(adView)
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private var isSeeking = false
    private fun setupControls() {
        binding.btnPlayPause.setOnClickListener {
            if (isPlaying) youTubePlayer?.pause() else youTubePlayer?.play()
        }
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
        binding.btnFullscreen.setOnClickListener { toggleFullscreen() }
        binding.btnEqualizer.setOnClickListener { startActivity(Intent(this, EqualizerSettingsActivity::class.java)) }
        binding.btnHd.setOnClickListener { startActivity(Intent(this, HighDefAudioSettingsActivity::class.java)) }
        binding.btnBooster.setOnClickListener { startActivity(Intent(this, SoundBoosterActivity::class.java)) }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        binding.touchOverlay.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.y
                    startX = event.x
                    currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                    isVolumeGesture = startX > deviceWidth * 0.66
                    isBrightnessGesture = !isVolumeGesture
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = startY - event.y
                    if (abs(deltaY) > 50) {
                        if (isVolumeGesture) adjustVolume(deltaY) else adjustBrightness(deltaY)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    binding.txtGestureIndicator.visibility = View.GONE
                    if (abs(startY - event.y) < 20 && abs(startX - event.x) < 20) toggleControlsVisibility()
                    true
                }
                else -> false
            }
        }
    }

    private fun adjustVolume(deltaY: Float) {
        val deltaVol = (deltaY / 300).toInt()
        val newVol = (currentVolume + deltaVol).coerceIn(0, maxVolume)
        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
        showGestureIndicator("Vol: $newVol / $maxVolume")
    }

    private fun adjustBrightness(deltaY: Float) {
        val lp = window.attributes
        var currentBright = lp.screenBrightness
        if (currentBright < 0) currentBright = 0.5f
        val deltaBright = deltaY / 5000
        val newBright = (currentBright + deltaBright).coerceIn(0.01f, 1.0f)
        lp.screenBrightness = newBright
        window.attributes = lp
        showGestureIndicator("Bright: ${(newBright * 100).toInt()}%")
    }

    private fun showGestureIndicator(text: String) {
        binding.txtGestureIndicator.text = text
        binding.txtGestureIndicator.visibility = View.VISIBLE
    }

    private fun toggleControlsVisibility() {
        val v = if (binding.rightControlsPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        binding.rightControlsPanel.visibility = v
        binding.seekBarContainer.visibility = v
        binding.btnBackPlayer.visibility = v
    }

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
        return String.format("%02d:%02d", m, sec)
    }
}