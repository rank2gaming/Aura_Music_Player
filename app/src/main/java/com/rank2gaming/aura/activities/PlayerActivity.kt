package com.rank2gaming.aura.activities

import android.animation.ObjectAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rank2gaming.aura.R
import com.rank2gaming.aura.audio.EqualizerSettingsActivity
import com.rank2gaming.aura.audio.HighDefAudioSettingsActivity
import com.rank2gaming.aura.audio.SoundBoosterActivity
import com.rank2gaming.aura.playbackbuttons.PlaybackControlView
import com.rank2gaming.aura.service.MusicService
import com.rank2gaming.aura.ui.AuraVisualizerView
import com.rank2gaming.aura.ui.TemplateSheetFragment
import com.rank2gaming.aura.utils.AdMobManager
import com.rank2gaming.aura.utils.FileUtils
import com.rank2gaming.aura.utils.TemplateManager
import kotlin.math.abs

class PlayerActivity : AppCompatActivity(), MusicService.ServiceCallbacks {

    private var playbackControls: PlaybackControlView? = null

    private var btnFavorite: ImageView? = null
    private var btnQueue: ImageView? = null
    private var btnTimer: ImageView? = null
    private var btnMore: ImageView? = null
    private var btnBack: ImageView? = null

    private var btnVisualEffect: ImageView? = null
    private var btnEqualizer: ImageView? = null
    private var btnHdAudio: ImageView? = null

    private var btnThemeSwitch: View? = null

    private var txtTitle: TextView? = null
    private var txtArtist: TextView? = null
    private var txtCurrentTime: TextView? = null
    private var txtTotalDuration: TextView? = null

    private var seekBar: SeekBar? = null
    private var imgAlbumArt: ImageView? = null
    private var cardAlbumArt: CardView? = null

    private var visFull: AuraVisualizerView? = null
    private var visBtn: AuraVisualizerView? = null
    private var adView: AdView? = null

    private var musicService: MusicService? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var rotateAnimator: ObjectAnimator
    private var isBound = false

    private val hideHandler = Handler(Looper.getMainLooper())
    private var volumeFab: FloatingActionButton? = null
    private var volumeIndicator: View? = null
    private val hideRunnable = Runnable { hideVolumeButton() }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getMusicService()
            isBound = true
            musicService?.setCallback(this@PlayerActivity)

            updateUI()
            startProgressUpdater()
            setupVisualizer()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layoutId = TemplateManager.getLayoutResId(this)
        setContentView(layoutId)

        initViews()
        adjustToolbarPosition()

        if (imgAlbumArt != null) {
            rotateAnimator = ObjectAnimator.ofFloat(imgAlbumArt, "rotation", 0f, 360f).apply {
                duration = 10000
                repeatCount = ObjectAnimator.INFINITE
                interpolator = LinearInterpolator()
            }
        }

        if (adView != null) {
            AdMobManager.loadBanner(adView)
        }

        visBtn?.setIsMiniMode(true)
        val savedVis = getSharedPreferences("AuraAudioPrefs", Context.MODE_PRIVATE).getInt("vis_template", 0)
        visFull?.setTemplate(savedVis)
        visBtn?.setTemplate(savedVis)

        val intent = Intent(this, MusicService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        setupListeners()
        setupSwipeableVolumeUI()
        setupSmartBackNavigation()
    }

    private fun setupSmartBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackAction()
            }
        })
        btnBack?.setOnClickListener { handleBackAction() }
    }

    private fun handleBackAction() {
        if (isTaskRoot) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
        finish()
    }

    private fun setupVisualizer() {
        val isPlaying = musicService?.isPlaying() == true
        visFull?.setPlaying(isPlaying)
        visBtn?.setPlaying(isPlaying)

        if (isPlaying) {
            visFull?.visibility = View.VISIBLE
            visBtn?.visibility = View.VISIBLE
        } else {
            visFull?.visibility = View.INVISIBLE
            visBtn?.visibility = View.INVISIBLE
        }
    }

    private fun initViews() {
        playbackControls = findViewById(R.id.playback_controls)

        btnFavorite = findViewById(R.id.btn_favorite)
        btnQueue = findViewById(R.id.btn_queue)
        btnTimer = findViewById(R.id.btn_timer)
        btnMore = findViewById(R.id.btn_more)
        btnBack = findViewById(R.id.btn_back)

        btnVisualEffect = findViewById(R.id.btn_visual_effect)
        btnEqualizer = findViewById(R.id.btn_equalizer)
        btnHdAudio = findViewById(R.id.btn_hd_audio)

        btnThemeSwitch = findViewById(R.id.themeswitchable)

        txtTitle = findViewById(R.id.txt_title_big)
        txtArtist = findViewById(R.id.txt_artist_big)
        txtCurrentTime = findViewById(R.id.txt_current_time)
        txtTotalDuration = findViewById(R.id.txt_total_duration)
        seekBar = findViewById(R.id.seekBar)

        imgAlbumArt = findViewById(R.id.img_album_art_big)
        cardAlbumArt = findViewById(R.id.card_album_art)

        visFull = findViewById(R.id.visualizer_full)
        visBtn = findViewById(R.id.visualizer_play_btn)

        adView = findViewById(R.id.adViewPlayer)
        if (adView == null) adView = findViewById(R.id.adViewFooter)
    }

    private fun updateUI() {
        if (isDestroyed || isFinishing) return
        val song = musicService?.getCurrentSong() ?: return

        txtTitle?.text = song.title
        txtArtist?.text = song.artist
        txtTotalDuration?.text = FileUtils.formatDuration(song.duration)

        if (imgAlbumArt != null) {
            try {
                Glide.with(this).load(FileUtils.getAlbumArtUri(song.albumId))
                    .placeholder(R.drawable.ic_music_note)
                    .circleCrop()
                    .into(imgAlbumArt!!)
            } catch (e: Exception) {}
        }

        seekBar?.max = musicService!!.getDuration()
        updateButtons()

        setupVisualizer()

        if (musicService!!.isPlaying()) {
            if (::rotateAnimator.isInitialized && (cardAlbumArt?.radius ?: 0f) > 50 && !rotateAnimator.isStarted) {
                rotateAnimator.start()
            } else if (::rotateAnimator.isInitialized) {
                rotateAnimator.resume()
            }
        } else {
            if (::rotateAnimator.isInitialized) rotateAnimator.pause()
        }

        if (visFull?.visibility == View.VISIBLE) {
            btnVisualEffect?.setColorFilter(ContextCompat.getColor(this, R.color.teal_200))
        } else {
            btnVisualEffect?.setColorFilter(ContextCompat.getColor(this, R.color.white))
        }
    }

    private fun adjustToolbarPosition() {
        val density = resources.displayMetrics.density
        val topMarginPx = (16 * density).toInt()

        fun addMargin(view: View?) {
            if (view == null) return
            val params = view.layoutParams as? ViewGroup.MarginLayoutParams
            if (params != null) {
                params.topMargin += topMarginPx
                view.layoutParams = params
            }
        }

        addMargin(btnBack)
        addMargin(btnMore)
        addMargin(btnThemeSwitch)
    }

    private fun setupListeners() {

        btnThemeSwitch?.setOnClickListener {
            val sheet = TemplateSheetFragment { recreate() }
            sheet.show(supportFragmentManager, "TemplateSheet")
        }

        btnEqualizer?.setOnClickListener { view ->
            showAudioMenu(view)
        }

        btnHdAudio?.setOnClickListener { startActivity(Intent(this, HighDefAudioSettingsActivity::class.java)) }

        btnQueue?.setOnClickListener { Toast.makeText(this, "Queue feature coming soon", Toast.LENGTH_SHORT).show() }

        playbackControls?.onPlayPauseClick = {
            togglePlayPause()
        }

        // Added Listener for the Visualizer Play Button itself
        visBtn?.setOnClickListener {
            togglePlayPause()
        }

        playbackControls?.onNextClick = { musicService?.playNext() }
        playbackControls?.onPrevClick = { musicService?.playPrev() }

        playbackControls?.onShuffleClick = {
            musicService?.let {
                it.isShuffle = !it.isShuffle
                playbackControls?.setShuffleState(it.isShuffle)
                Toast.makeText(this, if (it.isShuffle) "Shuffle On" else "Shuffle Off", Toast.LENGTH_SHORT).show()
            }
        }

        playbackControls?.onRepeatClick = {
            musicService?.let {
                it.isRepeat = !it.isRepeat
                playbackControls?.setRepeatState(it.isRepeat)
                Toast.makeText(this, if (it.isRepeat) "Repeat On" else "Repeat Off", Toast.LENGTH_SHORT).show()
            }
        }

        btnVisualEffect?.setOnClickListener {
            visFull?.nextTemplate()
            visBtn?.nextTemplate()
            val newIndex = visFull?.getCurrentTemplate() ?: 0
            getSharedPreferences("AuraAudioPrefs", Context.MODE_PRIVATE).edit().putInt("vis_template", newIndex).apply()
            visFull?.invalidate()
        }

        btnFavorite?.setOnClickListener {
            musicService?.isFavorite = !(musicService?.isFavorite ?: false)
            updateButtons()
        }

        btnTimer?.setOnClickListener { showTimerDialog() }

        btnMore?.setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.menu.add("Share")
            popup.show()
        }

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService?.seekTo(progress)
                    updateTimeLabels(progress.toLong(), musicService?.getDuration()?.toLong() ?: 0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun togglePlayPause() {
        if (musicService == null) return
        if (musicService!!.isPlaying()) {
            musicService?.pause()
        } else {
            musicService?.play()
        }
    }

    private fun showAudioMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add(0, 1, 0, "Equalizer")
        popup.menu.add(0, 2, 0, "High Definition Audio")
        popup.menu.add(0, 3, 0, "Sound Booster")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> startActivity(Intent(this, EqualizerSettingsActivity::class.java))
                2 -> startActivity(Intent(this, HighDefAudioSettingsActivity::class.java))
                3 -> startActivity(Intent(this, SoundBoosterActivity::class.java))
            }
            true
        }
        popup.show()
    }

    private fun updateButtons() {
        val isPlaying = musicService?.isPlaying() == true
        playbackControls?.setPlayingState(isPlaying)

        musicService?.let {
            playbackControls?.setShuffleState(it.isShuffle)
            playbackControls?.setRepeatState(it.isRepeat)
        }

        val isFav = musicService?.isFavorite == true
        btnFavorite?.setColorFilter(ContextCompat.getColor(this, if(isFav) R.color.teal_200 else R.color.white))
    }

    private fun showTimerDialog() {
        val options = arrayOf("10 min", "20 min", "30 min", "60 min", "Stop Timer")
        val values = intArrayOf(10, 20, 30, 60, 0)
        AlertDialog.Builder(this)
            .setTitle("Sleep Timer")
            .setItems(options) { _, which ->
                musicService?.setSleepTimer(values[which])
                Toast.makeText(this, "Timer Updated", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun updateTimeLabels(current: Long, total: Long) {
        txtCurrentTime?.text = FileUtils.formatDuration(current)
        txtTotalDuration?.text = FileUtils.formatDuration(total)
    }

    private fun startProgressUpdater() {
        runOnUiThread(object : Runnable {
            override fun run() {
                if (isDestroyed || isFinishing) return
                if (musicService != null && musicService!!.isPlaying()) {
                    val current = musicService!!.getCurrentPosition()
                    val total = musicService!!.getDuration().toLong()

                    seekBar?.progress = current
                    updateTimeLabels(current.toLong(), total)
                }
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun setupSwipeableVolumeUI() {
        val density = resources.displayMetrics.density
        volumeFab = FloatingActionButton(this)
        volumeFab?.setImageResource(R.drawable.ic_volume_control)
        volumeFab?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00E676"))
        volumeFab?.imageTintList = ColorStateList.valueOf(Color.BLACK)
        volumeFab?.size = FloatingActionButton.SIZE_MINI
        val fabParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        fabParams.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        fabParams.marginEnd = (16 * density).toInt()
        volumeFab?.layoutParams = fabParams
        volumeFab?.translationX = 300f
        volumeFab?.setOnClickListener {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
            showVolumeButton()
        }
        addContentView(volumeFab, fabParams)

        volumeIndicator = View(this)
        val indicatorDrawable = GradientDrawable()
        indicatorDrawable.shape = GradientDrawable.RECTANGLE
        indicatorDrawable.setColor(Color.WHITE)
        indicatorDrawable.setStroke(2, Color.BLACK)
        indicatorDrawable.cornerRadii = floatArrayOf(20f, 20f, 0f, 0f, 0f, 0f, 20f, 20f)
        volumeIndicator?.background = indicatorDrawable
        volumeIndicator?.elevation = 10f
        val indicatorParams = FrameLayout.LayoutParams((6 * density).toInt(), (50 * density).toInt())
        indicatorParams.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        volumeIndicator?.layoutParams = indicatorParams
        volumeIndicator?.alpha = 1.0f

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (distanceX > 5) { showVolumeButton(); return true }
                return false
            }
        })

        volumeIndicator?.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) volumeIndicator?.performClick()
            true
        }
        addContentView(volumeIndicator, indicatorParams)
    }

    private fun showVolumeButton() {
        volumeFab?.animate()?.translationX(0f)?.setDuration(300)?.start()
        volumeIndicator?.animate()?.alpha(0f)?.setDuration(300)?.start()
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, 3000)
    }

    private fun hideVolumeButton() {
        volumeFab?.animate()?.translationX(300f)?.setDuration(300)?.start()
        volumeIndicator?.animate()?.alpha(1f)?.setDuration(300)?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        visFull?.release()
        visBtn?.release()
    }

    override fun onUpdateUI() {
        if (!isFinishing && !isDestroyed) {
            runOnUiThread { updateUI() }
        }
    }
}