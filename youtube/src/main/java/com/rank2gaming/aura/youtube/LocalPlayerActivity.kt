package com.rank2gaming.aura.youtube

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.rank2gaming.aura.audio.AudioEnhancerManager
import com.rank2gaming.aura.audio.EqualizerSettingsActivity
import com.rank2gaming.aura.audio.HDAudioManager
import com.rank2gaming.aura.audio.SoundBoosterActivity
import com.rank2gaming.aura.youtube.databinding.ActivityLocalPlayerBinding
import kotlin.math.abs

@UnstableApi
class LocalPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocalPlayerBinding
    private var exoPlayer: ExoPlayer? = null
    private var isPlaying = true
    private var isLandscape = false
    private var resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

    // Audio Effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val playlist = ArrayList<Uri>()
    private var currentVideoIndex = -1

    // Gesture Logic
    private var startX = 0f
    private var isSeeking = false
    private var startPosMs = 0L

    private val updateHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            updateHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackLocal.setOnClickListener { finish() }

        loadBannerAd()
        loadPlaylistData()

        val initialUri = intent.data
        if (initialUri != null) {
            currentVideoIndex = playlist.indexOf(initialUri)
            if (currentVideoIndex == -1) {
                playlist.add(initialUri)
                currentVideoIndex = 0
            }
            initializePlayer(playlist[currentVideoIndex])
        }

        setupControls()
        setupGestures()
    }

    override fun onResume() {
        super.onResume()
        if (exoPlayer != null) {
            refreshAudioEffects()
        }
    }

    private fun loadBannerAd() {
        try {
            val adView = AdView(this)
            adView.setAdSize(AdSize.BANNER)
            adView.adUnitId = "ca-app-pub-3940256099942544/6300978111"
            binding.adContainer.addView(adView)
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun loadPlaylistData() {
        try {
            val projection = arrayOf(MediaStore.Video.Media._ID)
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
            val cursor = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder)
            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    playlist.add(uri)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun initializePlayer(uri: Uri) {
        releasePlayer()
        exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer

        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.playWhenReady = true

        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    binding.seekBar.max = exoPlayer?.duration?.toInt() ?: 0
                    binding.txtTotalTime.text = formatTime(exoPlayer?.duration ?: 0)
                    updateHandler.post(updateRunnable)
                } else if (state == Player.STATE_ENDED) {
                    playNextVideo()
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                this@LocalPlayerActivity.isPlaying = isPlaying
                binding.btnPlayPause.setImageResource(
                    if (isPlaying) com.rank2gaming.aura.audio.R.drawable.ic_pause
                    else com.rank2gaming.aura.audio.R.drawable.ic_play
                )
            }
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    initAudioEffects(audioSessionId)
                }
            }
        })
    }

    private fun initAudioEffects(sessionId: Int) {
        try {
            equalizer?.release()
            loudnessEnhancer?.release()
            bassBoost?.release()
            virtualizer?.release()

            equalizer = Equalizer(0, sessionId)
            loudnessEnhancer = LoudnessEnhancer(sessionId)
            bassBoost = BassBoost(0, sessionId)
            virtualizer = Virtualizer(0, sessionId)

            refreshAudioEffects()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun refreshAudioEffects() {
        try {
            if (equalizer != null) {
                equalizer?.enabled = true
                AudioEnhancerManager.applyEffects(equalizer!!, this)
            }

            if (loudnessEnhancer != null) {
                val sysBoost = if (HDAudioManager.isHdEnabled(this)) HDAudioManager.getVolumeBoost(this) else 0
                val knobBoost = AudioEnhancerManager.getValue(this, AudioEnhancerManager.KEY_GAIN_BOOST)
                val targetGain = (sysBoost + (knobBoost * 300)).toInt().coerceAtMost(2000)

                if (targetGain > 0) {
                    loudnessEnhancer?.setTargetGain(targetGain)
                    loudnessEnhancer?.enabled = true
                } else {
                    loudnessEnhancer?.enabled = false
                }
            }

            if (bassBoost != null && bassBoost!!.strengthSupported) {
                val bbStrength = (AudioEnhancerManager.getValue(this, AudioEnhancerManager.KEY_BASS_BOOST_API) * 100).toInt().coerceIn(0, 1000).toShort()
                bassBoost?.setStrength(bbStrength)
                bassBoost?.enabled = bbStrength > 0
            }

            if (virtualizer != null && virtualizer!!.strengthSupported) {
                val stereoVal = AudioEnhancerManager.getValue(this, AudioEnhancerManager.KEY_STEREO)
                val surroundVal = AudioEnhancerManager.getValue(this, AudioEnhancerManager.KEY_SURROUND)
                val combinedStrength = ((stereoVal * 200) + (surroundVal * 100)).toInt().coerceIn(0, 1000).toShort()
                virtualizer?.setStrength(combinedStrength)
                virtualizer?.enabled = combinedStrength > 0
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun releasePlayer() {
        try {
            equalizer?.release(); equalizer = null
            bassBoost?.release(); bassBoost = null
            virtualizer?.release(); virtualizer = null
            loudnessEnhancer?.release(); loudnessEnhancer = null
        } catch (e: Exception) {}

        updateHandler.removeCallbacks(updateRunnable)
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun playNextVideo() {
        if (playlist.isNotEmpty()) {
            currentVideoIndex = (currentVideoIndex + 1) % playlist.size
            initializePlayer(playlist[currentVideoIndex])
        }
    }

    private fun playPrevVideo() {
        if (playlist.isNotEmpty()) {
            currentVideoIndex = if (currentVideoIndex - 1 < 0) playlist.size - 1 else currentVideoIndex - 1
            initializePlayer(playlist[currentVideoIndex])
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        // Explicitly typed parameters to fix "Cannot infer type" error
        binding.touchOverlay.setOnTouchListener { _: View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startPosMs = exoPlayer?.currentPosition ?: 0
                    isSeeking = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.x - startX
                    if (abs(deltaX) > 50) {
                        val seekAmount = (deltaX * 50).toLong()
                        val newPos = (startPosMs + seekAmount).coerceIn(0, exoPlayer?.duration ?: 0)
                        binding.txtGestureIndicator.visibility = View.VISIBLE
                        val direction = if (deltaX > 0) ">>" else "<<"
                        binding.txtGestureIndicator.text = "$direction ${formatTime(newPos)}"
                        exoPlayer?.seekTo(newPos)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    binding.txtGestureIndicator.visibility = View.GONE
                    isSeeking = false
                    if (abs(event.x - startX) < 20) toggleControls()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupControls() {
        binding.btnPlayPause.setOnClickListener { if (isPlaying) exoPlayer?.pause() else exoPlayer?.play() }
        binding.btnNext.setOnClickListener { playNextVideo() }
        binding.btnPrev.setOnClickListener { playPrevVideo() }

        binding.btnBooster.setOnClickListener { startActivity(Intent(this, SoundBoosterActivity::class.java)) }
        binding.btnEqualizer.setOnClickListener { startActivity(Intent(this, EqualizerSettingsActivity::class.java)) }

        binding.btnRotate.setOnClickListener {
            isLandscape = !isLandscape
            requestedOrientation = if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        binding.btnFitMode.setOnClickListener {
            resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
            binding.playerView.resizeMode = resizeMode
            Toast.makeText(this, if(resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) "Fill" else "Fit", Toast.LENGTH_SHORT).show()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.txtCurrentTime.text = formatTime(progress.toLong())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                exoPlayer?.seekTo(seekBar?.progress?.toLong() ?: 0L)
            }
        })
    }

    private fun toggleControls() {
        val v = if (binding.controlsLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        binding.controlsLayout.visibility = v
        binding.bottomControlsBg.visibility = v
        binding.rightControls.visibility = v
        binding.btnBackLocal.visibility = v
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
        return String.format("%02d:%02d", m, s)
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
}