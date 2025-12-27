package com.rank2gaming.aura.activities

import android.Manifest
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.ComponentName
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.rank2gaming.aura.R
import com.rank2gaming.aura.adapters.*
import com.rank2gaming.aura.audio.EqualizerSettingsActivity
import com.rank2gaming.aura.audio.HighDefAudioSettingsActivity
import com.rank2gaming.aura.databinding.ActivityMainBinding
import com.rank2gaming.aura.model.Song
import com.rank2gaming.aura.service.MusicService
import com.rank2gaming.aura.utils.AdMobManager
import com.rank2gaming.aura.utils.CoverArtManager
import com.rank2gaming.aura.utils.FileUtils
import com.rank2gaming.aura.utils.PlaylistBackupManager
import com.rank2gaming.aura.utils.PlaylistManager
import com.rank2gaming.aura.utils.SearchManager
import com.rank2gaming.aura.utils.ThemeManager
import com.rank2gaming.aura.youtube.LocalVideoListActivity
import com.rank2gaming.aura.youtube.YouTubeDashboardActivity
import kotlin.math.abs

class MainActivity : AppCompatActivity(), MusicService.ServiceCallbacks {

    private lateinit var binding: ActivityMainBinding
    private var musicService: MusicService? = null
    private val allSongs = ArrayList<Song>()
    private var currentTab = 0
    private var isBound = false
    private var songAdapter: SongAdapter? = null
    private val PERMISSION_REQ_CODE = 100

    private var videoHomeView: View? = null

    private var selectedSongForEdit: Song? = null
    private var pendingActionSong: Song? = null
    private var pendingActionName: String? = null
    private var pendingActionType = ""

    private val hideHandler = Handler(Looper.getMainLooper())
    private var volumeFab: FloatingActionButton? = null
    private var volumeIndicator: View? = null
    private val hideRunnable = Runnable { hideVolumeButton() }

    // Mini Player Expansion & Animation
    private var isMiniPlayerExpanded = false
    private lateinit var miniRotateAnimator: ObjectAnimator
    private val updateHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateMiniPlayerProgress()
            updateHandler.postDelayed(this, 1000)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getMusicService()
            isBound = true
            musicService?.setCallback(this@MainActivity)

            if (musicService?.getCurrentSong() == null && allSongs.isNotEmpty()) {
                musicService?.setList(allSongs)
            }

            if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
                handleIncomingIntent(intent)
            } else {
                updateMiniPlayer()
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) { musicService = null; isBound = false }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            if (isBound && musicService != null) {
                handleIncomingIntent(intent)
            }
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && selectedSongForEdit != null) {
            val uri = result.data?.data
            if (uri != null) {
                CoverArtManager.saveCustomArt(this, selectedSongForEdit!!.id, uri.toString())
                songAdapter?.notifyDataSetChanged()
                updateMiniPlayer()
                Toast.makeText(this, "Cover Art Saved", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val restoreFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                PlaylistBackupManager.restorePlaylists(this, uri)
                if (currentTab == 5) loadTabContent(5)
            }
        }
    }

    private val recoverLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (pendingActionType == "RENAME" && pendingActionSong != null && pendingActionName != null) performRename(pendingActionSong!!, pendingActionName!!)
            else if (pendingActionType == "DELETE" && pendingActionSong != null) deleteSong(pendingActionSong!!)
        } else { Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyTheme(this, binding.mainContent)
        checkAndRequestPermissions()

        AdMobManager.loadBanner(binding.adView)

        // Setup Mini Player Visuals
        binding.miniPlayer.visualizerMiniBtn.setIsMiniMode(true)
        binding.miniPlayer.visualizerMiniArt.setIsMiniMode(true)

        val savedTemplate = getSharedPreferences("AuraAudioPrefs", Context.MODE_PRIVATE).getInt("vis_template", 0)
        binding.miniPlayer.visualizerMiniBtn.setTemplate(savedTemplate)
        binding.miniPlayer.visualizerMiniArt.setTemplate(savedTemplate)

        // Setup Rotation Animation for Mini Player Art
        miniRotateAnimator = ObjectAnimator.ofFloat(binding.miniPlayer.imgMiniArt, "rotation", 0f, 360f).apply {
            duration = 10000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }

        binding.toolbar.imgMenu.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> binding.drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_equalizer -> startActivity(Intent(this, EqualizerSettingsActivity::class.java))
                R.id.nav_hd_audio -> startActivity(Intent(this, HighDefAudioSettingsActivity::class.java))
                R.id.nav_themes -> startActivity(Intent(this, ThemeActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_about -> startActivity(Intent(this, AboutActivity::class.java))
                R.id.nav_privacy -> startActivity(Intent(this, PrivacyActivity::class.java))
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        setupTabs()
        setupListeners()
        setupSwipeableVolumeUI()
        setupMiniPlayerGestures()
    }

    private fun handleIncomingIntent(intent: Intent) {
        val uri = intent.data ?: return
        try {
            var title = "Unknown Song"
            var artist = "Unknown Artist"
            val path = uri.toString()
            var duration = 0L

            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (nameIndex != -1) title = it.getString(nameIndex)
                    if (title.contains(".")) title = title.substringBeforeLast(".")
                    val artistIndex = it.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)
                    if (artistIndex != -1) artist = it.getString(artistIndex) ?: "Unknown Artist"
                    val durIndex = it.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)
                    if (durIndex != -1) duration = it.getLong(durIndex)
                }
            }

            val tempSong = Song(-1L, title, artist, path, duration, -1L, "")
            val singleList = ArrayList<Song>()
            singleList.add(tempSong)

            musicService?.setList(singleList)
            musicService?.setSong(0)
            startActivity(Intent(this, PlayerActivity::class.java))
            setIntent(Intent())

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Could not play file", Toast.LENGTH_SHORT).show()
        }
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

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 != null) {
                    val diffX = e2.x - e1.x
                    if (diffX < -20 && abs(velocityX) > 100) { showVolumeButton(); return true }
                }
                return false
            }
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

    // --- SWIPE GESTURES & MINI PLAYER EXPANSION ---
    private fun setupMiniPlayerGestures() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 != null) {
                    val diffY = e2.y - e1.y
                    if (abs(diffY) > 100 && abs(velocityY) > 100) {
                        if (diffY < 0) { // Swipe Up to Expand
                            toggleMiniPlayerExpansion(true)
                        } else { // Swipe Down to Collapse
                            toggleMiniPlayerExpansion(false)
                        }
                        return true
                    }
                }
                return false
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                startActivity(Intent(this@MainActivity, PlayerActivity::class.java))
                return true
            }
        })

        binding.miniPlayer.root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        // Initialize Controls
        try {
            val btnPrev = binding.miniPlayer.root.findViewById<View>(R.id.btnMiniPrev)
            val btnNext = binding.miniPlayer.root.findViewById<View>(R.id.btnMiniNext)
            val seekBar = binding.miniPlayer.root.findViewById<SeekBar>(R.id.miniSeekBar)

            btnPrev?.setOnClickListener { musicService?.playPrev() }
            btnNext?.setOnClickListener { musicService?.playNext() }
            seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) musicService?.seekTo(progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleMiniPlayerExpansion(expand: Boolean) {
        isMiniPlayerExpanded = expand
        try {
            val visibility = if (expand) View.VISIBLE else View.GONE
            val btnPrev = binding.miniPlayer.root.findViewById<View>(R.id.btnMiniPrev)
            val btnNext = binding.miniPlayer.root.findViewById<View>(R.id.btnMiniNext)
            val seekBar = binding.miniPlayer.root.findViewById<SeekBar>(R.id.miniSeekBar)

            btnPrev?.visibility = visibility
            btnNext?.visibility = visibility
            seekBar?.visibility = visibility

            if (expand) updateMiniPlayerProgress()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun updateMiniPlayerProgress() {
        if (isMiniPlayerExpanded && musicService != null && musicService!!.isPlaying()) {
            val seekBar = binding.miniPlayer.root.findViewById<SeekBar>(R.id.miniSeekBar)
            seekBar?.max = musicService!!.getDuration()
            seekBar?.progress = musicService!!.getCurrentPosition()
        }
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyTheme(this, binding.mainContent)
        if (currentTab == 5) loadTabContent(5)
        if (isBound && musicService != null) { musicService?.setCallback(this); updateMiniPlayer() }
        if (isMiniPlayerExpanded) updateHandler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        updateHandler.removeCallbacks(updateRunnable)
        if (::miniRotateAnimator.isInitialized && miniRotateAnimator.isRunning) {
            miniRotateAnimator.pause()
        }
    }

    override fun onUpdateUI() {
        if (!isDestroyed && !isFinishing) runOnUiThread {
            updateMiniPlayer()
            if (isMiniPlayerExpanded) updateMiniPlayerProgress()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= 29 && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (permissions.isNotEmpty()) ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQ_CODE) else loadSongs()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_CODE) {
            val storagePerm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, storagePerm) == PackageManager.PERMISSION_GRANTED) loadSongs()
            else showPermissionSettingsDialog()
        }
    }

    private fun showPermissionSettingsDialog() {
        AlertDialog.Builder(this).setTitle("Permissions Required").setMessage("Storage access is needed.").setPositiveButton("Settings") { _, _ ->
            try { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", packageName, null) }) } catch (e: Exception) {}
        }.setNegativeButton("Cancel", null).show()
    }

    private fun loadSongs() {
        allSongs.clear()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        // M4A/WAV Fix: Filter by Duration > 10s instead of IS_MUSIC
        val selection = "${MediaStore.Audio.Media.DURATION} >= 10000"

        try {
            val cursor = contentResolver.query(uri, projection, selection, null, "${MediaStore.Audio.Media.TITLE} ASC")
            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val title = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)) ?: "Unknown"
                    val artist = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)) ?: "Unknown Artist"
                    val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)) ?: ""
                    val dur = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                    val albumId = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))

                    if (path.isNotEmpty() && !path.contains("/AuraMusic/", ignoreCase = true)) {
                        val folderPath = try { path.substringBeforeLast("/") } catch (e: Exception) { "" }
                        allSongs.add(Song(id, title, artist, path, dur, albumId, folderPath))
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        if (songAdapter == null && currentTab == 0) loadTabContent(0) else songAdapter?.updateList(allSongs)
        val intent = Intent(this, MusicService::class.java); startService(intent); bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setupTabs() {
        val tabs = listOf("Songs", "Video", "Albums", "Artists", "Folders", "Playlists")
        tabs.forEach { binding.tabLayout.addTab(binding.tabLayout.newTab().setText(it)) }
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { currentTab = tab?.position ?: 0; loadTabContent(currentTab) }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadTabContent(index: Int) {
        binding.recyclerView.visibility = View.VISIBLE
        videoHomeView?.visibility = View.GONE
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        when (index) {
            0 -> {
                if (songAdapter == null) songAdapter = SongAdapter(allSongs, this, onClick = { playSong(it) }, onMenuClick = { s, v -> showSongMenu(s, v) })
                else songAdapter?.updateList(allSongs)
                binding.recyclerView.adapter = songAdapter
            }
            1 -> {
                binding.recyclerView.visibility = View.GONE
                showVideoHome()
            }
            2 -> {
                val albums = allSongs.distinctBy { it.albumId }
                binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
                binding.recyclerView.adapter = AlbumAdapter(albums) { id -> startActivity(Intent(this, AlbumDetailsActivity::class.java).putExtra("album_id", id)) }
            }
            3 -> {
                val artists = allSongs.groupBy { it.artist }.map { ArtistData(it.key, it.value.size) }
                binding.recyclerView.adapter = ArtistAdapter(artists) { name -> startActivity(Intent(this, ArtistDetailsActivity::class.java).putExtra("artist_name", name)) }
            }
            4 -> {
                val folders = allSongs.groupBy { it.folderPath }.map { FolderData(it.key, it.value.size) }
                binding.recyclerView.adapter = FolderAdapter(folders) { path -> startActivity(Intent(this, FolderDetailsActivity::class.java).putExtra("folder_path", path)) }
            }
            5 -> {
                val data = ArrayList<PlaylistData>()
                val userLists = PlaylistManager.getPlaylists(this)
                val totalPlaylists = 4 + userLists.size
                data.add(PlaylistData("$totalPlaylists Playlists", 0, PlaylistAdapter.TYPE_HEADER_TOP))
                data.add(PlaylistData("My favourite", 0, PlaylistAdapter.TYPE_SMART))
                data.add(PlaylistData("Recently added", 0, PlaylistAdapter.TYPE_SMART))
                data.add(PlaylistData("Recently played", 0, PlaylistAdapter.TYPE_SMART))
                data.add(PlaylistData("My top tracks", 0, PlaylistAdapter.TYPE_SMART))
                data.add(PlaylistData("My playlists (${userLists.size})", 0, PlaylistAdapter.TYPE_HEADER_SECTION))
                for ((name, songs) in userLists) { data.add(PlaylistData(name, songs.size, PlaylistAdapter.TYPE_USER)) }
                binding.recyclerView.adapter = PlaylistAdapter(data,
                    onPlaylistClick = { name -> startActivity(Intent(this, PlaylistDetailsActivity::class.java).putExtra("playlist_name", name)) },
                    onHeaderAction = { actionType, view -> if (actionType == 0) showCreatePlaylistDialog() else showHeaderMenu(view) },
                    onUserPlaylistMenu = { name, view ->
                        val popup = PopupMenu(this, view)
                        popup.menu.add("Delete")
                        popup.setOnMenuItemClickListener { PlaylistManager.deletePlaylist(this, name); loadTabContent(5); true }
                        popup.show()
                    }
                )
            }
        }
    }

    private fun showVideoHome() {
        if (videoHomeView == null) {
            try {
                val layoutId = com.rank2gaming.aura.youtube.R.layout.activity_video_home
                val parent = binding.recyclerView.parent as ViewGroup
                videoHomeView = LayoutInflater.from(this).inflate(layoutId, parent, false)
                parent.addView(videoHomeView)

                val cardLocal = videoHomeView?.findViewById<View>(com.rank2gaming.aura.youtube.R.id.card_local)
                val cardOnline = videoHomeView?.findViewById<View>(com.rank2gaming.aura.youtube.R.id.card_online)
                val btnBack = videoHomeView?.findViewById<View>(com.rank2gaming.aura.youtube.R.id.btn_back)

                btnBack?.visibility = View.VISIBLE
                btnBack?.setOnClickListener {
                    binding.tabLayout.getTabAt(0)?.select()
                }

                cardLocal?.setOnClickListener { startActivity(Intent(this, LocalVideoListActivity::class.java)) }
                cardOnline?.setOnClickListener { startActivity(Intent(this, YouTubeDashboardActivity::class.java)) }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error loading Video Tab", Toast.LENGTH_SHORT).show()
            }
        }
        videoHomeView?.visibility = View.VISIBLE
    }

    private fun showHeaderMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add("Back up playlist")
        popup.menu.add("Restore playlist")
        popup.menu.add("Import playlist")
        popup.menu.add("Manage")
        popup.setOnMenuItemClickListener {
            when(it.title) {
                "Back up playlist" -> PlaylistBackupManager.backupPlaylists(this)
                "Restore playlist" -> {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }
                    restoreFileLauncher.launch(intent)
                }
                "Import playlist" -> Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show()
                "Manage" -> startActivity(Intent(this, ManagePlaylistsActivity::class.java))
            }
            true
        }
        popup.show()
    }

    private fun showCreatePlaylistDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this).setTitle("New Playlist").setView(input).setPositiveButton("Create") { _, _ ->
            val name = input.text.toString()
            if (name.isNotEmpty()) { PlaylistManager.createPlaylist(this, name); loadTabContent(5) }
        }.setNegativeButton("Cancel", null).show()
    }

    private fun playSong(song: Song) {
        musicService?.setList(allSongs)
        musicService?.setSong(allSongs.indexOf(song))
    }

    private fun setupListeners() {
        binding.toolbar.imgSearch.setOnClickListener { binding.toolbar.root.visibility = View.GONE; binding.searchBar.visibility = View.VISIBLE }
        binding.imgBackSearch.setOnClickListener { binding.searchBar.visibility = View.GONE; binding.toolbar.root.visibility = View.VISIBLE; loadTabContent(currentTab) }
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { if (currentTab == 0 && songAdapter != null) songAdapter!!.updateList(SearchManager.filterSongs(allSongs, s.toString())) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.toolbar.imgMore.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add(0, 1, 0, "About Us")
            popup.menu.add(0, 2, 0, "Privacy Policy")
            popup.menu.add(0, 3, 0, "Share App")
            popup.setOnMenuItemClickListener {
                when(it.itemId) {
                    1 -> startActivity(Intent(this, AboutActivity::class.java))
                    2 -> startActivity(Intent(this, PrivacyActivity::class.java))
                    3 -> startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_TEXT, "Check out Aura Music Player!") }, "Share"))
                }
                true
            }
            popup.show()
        }

        binding.miniPlayer.root.setOnClickListener { startActivity(Intent(this, PlayerActivity::class.java)) }
        binding.miniPlayer.btnMiniPlay.setOnClickListener { if (musicService?.isPlaying() == true) musicService?.pause() else musicService?.play() }
    }

    private fun updateMiniPlayer() {
        val song = musicService?.getCurrentSong()
        if (song != null) {
            binding.miniPlayer.root.visibility = View.VISIBLE
            binding.miniPlayer.txtMiniTitle.text = song.title
            binding.miniPlayer.txtMiniArtist.text = song.artist
            try {
                val customArt = CoverArtManager.getCustomArt(this, song.id)
                val artUri = if (customArt != null) Uri.parse(customArt) else FileUtils.getAlbumArtUri(song.albumId)
                Glide.with(this).load(artUri).placeholder(R.drawable.ic_music_note).into(binding.miniPlayer.imgMiniArt)
            } catch (e: Exception) { e.printStackTrace() }
            val isPlaying = musicService!!.isPlaying()
            binding.miniPlayer.btnMiniPlay.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)

            binding.miniPlayer.visualizerMiniBtn.setPlaying(isPlaying)
            binding.miniPlayer.visualizerMiniArt.setPlaying(isPlaying)
            binding.miniPlayer.visualizerMiniBtn.visibility = if (isPlaying) View.VISIBLE else View.INVISIBLE
            binding.miniPlayer.visualizerMiniArt.visibility = if (isPlaying) View.VISIBLE else View.INVISIBLE

            if (songAdapter != null) songAdapter?.updatePlayingStatus(song.id, isPlaying)

            // Handle Rotation
            if (isPlaying) {
                if (!miniRotateAnimator.isStarted) miniRotateAnimator.start() else miniRotateAnimator.resume()
            } else {
                miniRotateAnimator.pause()
            }

            // Sync expanded view visibility
            if (isMiniPlayerExpanded) {
                updateMiniPlayerProgress()
                val btnPrev = binding.miniPlayer.root.findViewById<View>(R.id.btnMiniPrev)
                val btnNext = binding.miniPlayer.root.findViewById<View>(R.id.btnMiniNext)
                val seekBar = binding.miniPlayer.root.findViewById<SeekBar>(R.id.miniSeekBar)
                btnPrev?.visibility = View.VISIBLE
                btnNext?.visibility = View.VISIBLE
                seekBar?.visibility = View.VISIBLE
            } else {
                val btnPrev = binding.miniPlayer.root.findViewById<View>(R.id.btnMiniPrev)
                val btnNext = binding.miniPlayer.root.findViewById<View>(R.id.btnMiniNext)
                val seekBar = binding.miniPlayer.root.findViewById<SeekBar>(R.id.miniSeekBar)
                btnPrev?.visibility = View.GONE
                btnNext?.visibility = View.GONE
                seekBar?.visibility = View.GONE
            }

        } else {
            binding.miniPlayer.root.visibility = View.GONE
            if (::miniRotateAnimator.isInitialized) miniRotateAnimator.cancel()
        }
    }

    private fun showSongMenu(song: Song, view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add(0, 1, 0, "Add to Playlist"); popup.menu.add(0, 2, 0, "Rename"); popup.menu.add(0, 3, 0, "Set as Ringtone")
        popup.menu.add(0, 4, 0, "Edit Cover Art"); popup.menu.add(0, 5, 0, "Remove Cover Art"); popup.menu.add(0, 6, 0, "Delete")
        popup.setOnMenuItemClickListener {
            when(it.itemId) {
                1 -> showAddToPlaylistDialog(song)
                2 -> showRenameDialog(song)
                3 -> confirmSetRingtone(song)
                4 -> { selectedSongForEdit = song; imagePickerLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) }
                5 -> { CoverArtManager.removeCustomArt(this, song.id); songAdapter?.notifyDataSetChanged(); updateMiniPlayer() }
                6 -> confirmDelete(song)
            }
            true
        }
        popup.show()
    }

    private fun confirmDelete(song: Song) { AlertDialog.Builder(this).setTitle("Delete").setMessage("Delete '${song.title}'?").setPositiveButton("Yes") {_,_-> deleteSong(song)}.setNegativeButton("No", null).show() }
    private fun confirmSetRingtone(song: Song) { AlertDialog.Builder(this).setTitle("Set Ringtone").setMessage("Set as ringtone?").setPositiveButton("Yes") {_,_-> setRingtone(song)}.setNegativeButton("No", null).show() }
    private fun showAddToPlaylistDialog(song: Song) {
        val lists = PlaylistManager.getPlaylists(this).keys.toTypedArray()
        AlertDialog.Builder(this).setTitle("Add to").setItems(lists + "Create New") { _, which ->
            if (which < lists.size) PlaylistManager.addSongToPlaylist(this, lists[which], song.id)
            else showCreatePlaylistDialog()
        }.show()
    }
    private fun showRenameDialog(song: Song) {
        val input = EditText(this); input.setText(song.title)
        AlertDialog.Builder(this).setTitle("Rename").setView(input).setPositiveButton("Save") { _, _ -> performRename(song, input.text.toString()) }.show()
    }

    private fun performRename(song: Song, name: String) {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        val values = ContentValues().apply { put(MediaStore.Audio.Media.TITLE, name) }
        try { contentResolver.update(uri, values, null, null); loadSongs() } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = MediaStore.createWriteRequest(contentResolver, listOf(uri)); pendingActionSong = song; pendingActionName = name; pendingActionType = "RENAME"; recoverLauncher.launch(IntentSenderRequest.Builder(intent.intentSender).build())
            }
        }
    }
    private fun deleteSong(song: Song) {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        try { contentResolver.delete(uri, null, null); allSongs.remove(song); songAdapter?.updateList(allSongs) } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = MediaStore.createDeleteRequest(contentResolver, listOf(uri)); pendingActionSong = song; pendingActionType = "DELETE"; recoverLauncher.launch(IntentSenderRequest.Builder(intent.intentSender).build())
            }
        }
    }
    private fun setRingtone(song: Song) {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(this)) startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).setData(Uri.parse("package:$packageName")))
        else try { RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)) } catch (e: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) { unbindService(serviceConnection); isBound = false }
        binding.miniPlayer.visualizerMiniBtn.release()
        binding.miniPlayer.visualizerMiniArt.release()
    }
}