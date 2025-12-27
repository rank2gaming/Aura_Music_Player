@file:Suppress("DEPRECATION")
@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.rank2gaming.aura.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import com.rank2gaming.aura.R
import com.rank2gaming.aura.activities.PlayerActivity
import com.rank2gaming.aura.audio.AudioBinder
import com.rank2gaming.aura.audio.AudioEffectManager
import com.rank2gaming.aura.audio.AudioEnhancerManager
import com.rank2gaming.aura.audio.AudioFeatureListener
import com.rank2gaming.aura.audio.AudioSettingsWatcher
import com.rank2gaming.aura.audio.EqualizerSettingsActivity
import com.rank2gaming.aura.audio.HDAudioManager
import com.rank2gaming.aura.audio.IAudioService
import com.rank2gaming.aura.audio.SoundBoosterActivity
import com.rank2gaming.aura.model.Song
import com.rank2gaming.aura.utils.BalanceAudioProcessor
import com.rank2gaming.aura.utils.FormatUtils
import com.rank2gaming.aura.utils.HistoryManager
import com.rank2gaming.aura.utils.PlaylistManager
import com.rank2gaming.aura.utils.SettingsManager
import java.util.ArrayList

class MusicService : Service(), IAudioService, AudioFeatureListener {

    inner class MusicBinder : Binder(), AudioBinder {
        fun getMusicService(): MusicService = this@MusicService
        override fun getService(): IAudioService = this@MusicService
    }

    private val binder = MusicBinder()
    private var exoPlayer: ExoPlayer? = null
    private var audioManager: AudioManager? = null
    private var mediaSession: MediaSessionCompat? = null

    // Audio Effects
    private val EFFECT_PRIORITY = 1000
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var bassBoost: BassBoost? = null
    private var equalizer: Equalizer? = null
    private var virtualizer: Virtualizer? = null

    private val balanceProcessor = BalanceAudioProcessor()
    private var settingsWatcher: AudioSettingsWatcher? = null

    private var songList: ArrayList<Song> = ArrayList()
    private var songPos: Int = 0
    private var currentAlbumArtBitmap: Bitmap? = null
    private var lastLoadedAlbumId: Long = -1

    var isShuffle = false
    var isRepeat = false
    var isFavorite = false

    private var serviceCallbacks: ServiceCallbacks? = null
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = Runnable { if (isPlaying()) pause() }

    interface ServiceCallbacks { fun onUpdateUI() }

    companion object {
        const val CHANNEL_ID = "AuraMusicChannel"
        const val ACTION_PLAY = "action_play"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_NEXT = "action_next"
        const val ACTION_PREVIOUS = "action_previous"
        const val ACTION_TOGGLE_FAVORITE = "com.rank2gaming.aura.ACTION_TOGGLE_FAVORITE"
        const val ACTION_OPEN_EQ = "com.rank2gaming.aura.ACTION_OPEN_EQ"
        const val ACTION_OPEN_BOOSTER = "com.rank2gaming.aura.ACTION_OPEN_BOOSTER"
        const val ACTION_TOGGLE_HD = "com.rank2gaming.aura.ACTION_TOGGLE_HD"
        const val PLAYLIST_FAVORITES = "My favourite"
        const val TAG = "MusicService"
    }

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && isPlaying()) pause()
        }
    }

    fun setCallback(callbacks: ServiceCallbacks) { this.serviceCallbacks = callbacks }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        settingsWatcher = AudioSettingsWatcher(this, this)
        settingsWatcher?.startWatching()

        initMediaSession()
        initExoPlayer()

        try { registerReceiver(becomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) } catch (e: Exception) {}

        loadLastPlayedSong()
    }

    // --- Audio Feature Listener Implementation ---

    override fun onHdAudioChanged(enabled: Boolean, volumeBoost: Int) {
        try {
            loudnessEnhancer?.enabled = enabled
            if (enabled) {
                onVolumeBoostChanged(volumeBoost)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onVolumeBoostChanged(boost: Int) {
        try {
            if (loudnessEnhancer != null) {
                if (boost > 0 && loudnessEnhancer?.enabled == false) {
                    loudnessEnhancer?.enabled = true
                }
                val digitalGain = (boost * 50).coerceAtMost(1000)
                loudnessEnhancer?.setTargetGain(digitalGain)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onBassBoostChanged(strength: Int) {
        try {
            if (bassBoost == null && exoPlayer != null) {
                rebuildAudioEffects(exoPlayer!!.audioSessionId)
            }
            if (bassBoost?.strengthSupported == true) {
                if (!bassBoost!!.enabled) bassBoost!!.enabled = true
                bassBoost?.setStrength(strength.toShort())
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onVirtualizerChanged(strength: Int, isMono: Boolean) {
        try {
            val shouldEnable = (strength > 0) && !isMono
            virtualizer?.let { virt ->
                virt.enabled = shouldEnable
                if (shouldEnable && virt.strengthSupported) {
                    virt.setStrength(strength.toShort())
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onEqualizerEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onEqualizerBandChanged(band: Short, level: Int) {
        try {
            equalizer?.let { eq ->
                val range = eq.bandLevelRange
                if (range != null) {
                    val safeLevel = level.toShort().coerceIn(range[0], range[1])
                    eq.setBandLevel(band, safeLevel)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onMonoAudioChanged(enabled: Boolean) { }

    override fun onBoosterEffectChanged(effectType: Int, valueDb: Float) {
        try {
            if (equalizer == null && exoPlayer != null) {
                val id = exoPlayer!!.audioSessionId
                if (id != 0) rebuildAudioEffects(id)
            }

            if (equalizer != null) {
                if (!equalizer!!.enabled) {
                    equalizer!!.enabled = true
                    AudioEffectManager.setEqEnabled(this, true)
                }
                AudioEnhancerManager.applyEffects(equalizer!!, this)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // --- Initialization & Rebuild Logic ---

    private fun initExoPlayer() {
        val customAudioSink = DefaultAudioSink.Builder()
            .setAudioProcessors(arrayOf(balanceProcessor))
            .setEnableFloatOutput(true)
            .setEnableAudioTrackPlaybackParams(true)
            .build()

        val renderersFactory = RenderersFactory { handler, videoListener, audioListener, textOutput, metadataOutput ->
            arrayOf(
                MediaCodecAudioRenderer(
                    this@MusicService,
                    MediaCodecSelector.DEFAULT,
                    handler,
                    audioListener,
                    customAudioSink
                )
            )
        }

        exoPlayer = ExoPlayer.Builder(this, renderersFactory)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        exoPlayer?.setAudioAttributes(audioAttributes, true)

        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) onCompletion()
                updateMediaSessionState(playbackState)
                notifyUI()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateMediaSessionState(if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED)
                showNotification()
                notifyUI()
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                rebuildAudioEffects(audioSessionId)
            }
        })
    }

    // --- FAVORITES LOGIC ---
    private fun checkIsFavorite() {
        val song = songList.getOrNull(songPos) ?: return
        val favorites = PlaylistManager.getPlaylists(this)[PLAYLIST_FAVORITES] ?: emptyList()
        isFavorite = favorites.contains(song.id)
    }

    fun toggleFavorite() {
        val song = songList.getOrNull(songPos) ?: return
        isFavorite = !isFavorite

        if (isFavorite) {
            PlaylistManager.createPlaylist(this, PLAYLIST_FAVORITES)
            PlaylistManager.addSongToPlaylist(this, PLAYLIST_FAVORITES, song.id)
        } else {
            PlaylistManager.removeSongFromPlaylist(this, PLAYLIST_FAVORITES, song.id)
        }
        notifyUI()
    }

    private fun releaseAudioEffects() {
        try {
            loudnessEnhancer?.release(); loudnessEnhancer = null
            bassBoost?.release(); bassBoost = null
            equalizer?.release(); equalizer = null
            virtualizer?.release(); virtualizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing effects: ${e.message}")
        }
    }

    private fun rebuildAudioEffects(sessionId: Int) {
        if (sessionId == 0) return
        releaseAudioEffects()

        try {
            try {
                loudnessEnhancer = LoudnessEnhancer(sessionId)
                onVolumeBoostChanged(HDAudioManager.getVolumeBoost(this))
            } catch (e: Exception) {}

            try {
                bassBoost = BassBoost(EFFECT_PRIORITY, sessionId)
                if (bassBoost?.strengthSupported == true) {
                    onBassBoostChanged(AudioEffectManager.getBassStrength(this))
                    bassBoost?.enabled = AudioEffectManager.isEqEnabled(this)
                }
            } catch (e: Exception) {}

            try {
                virtualizer = Virtualizer(EFFECT_PRIORITY, sessionId)
                val str = AudioEffectManager.getVirtualizerStrength(this)
                val isMono = HDAudioManager.isMonoAudio(this)
                onVirtualizerChanged(str, isMono)
            } catch (e: Exception) {}

            try {
                equalizer = Equalizer(EFFECT_PRIORITY, sessionId)
                restoreEQSettings()
                onEqualizerEnabled(AudioEffectManager.isEqEnabled(this))
                AudioEnhancerManager.applyEffects(equalizer!!, this)
            } catch (e: Exception) {}

        } catch (e: Exception) { e.printStackTrace() }
    }

    // New Helper for Visualizer Linkage (Crucial for PlayerActivity)
    fun getAudioSessionId(): Int {
        return exoPlayer?.audioSessionId ?: 0
    }

    private fun restoreEQSettings() {
        try {
            equalizer?.let { eq ->
                val bands = eq.numberOfBands
                val range = eq.bandLevelRange ?: return
                for (i in 0 until bands) {
                    val level = AudioEffectManager.getBandLevel(this, i.toShort())
                    val safeLevel = level.toShort().coerceIn(range[0], range[1])
                    eq.setBandLevel(i.toShort(), safeLevel)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // --- IAudioService Implementation ---
    override fun getEqualizer(): Equalizer? {
        if (equalizer == null && exoPlayer != null) {
            val id = exoPlayer!!.audioSessionId
            if (id != 0) rebuildAudioEffects(id)
        }
        return equalizer
    }

    override fun setBassStrength(strength: Short) = onBassBoostChanged(strength.toInt())
    override fun setEQBandLevel(band: Short, level: Short) = onEqualizerBandChanged(band, level.toInt())

    // --- Standard Service Logic ---

    private fun initMediaSession() {
        val mediaButtonReceiverComponent = ComponentName(applicationContext, MediaButtonReceiver::class.java)
        mediaSession = MediaSessionCompat(this, "MusicService", mediaButtonReceiverComponent, null)

        mediaSession?.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() { play() }
            override fun onPause() { pause() }
            override fun onSkipToNext() { playNext() }
            override fun onSkipToPrevious() { playPrev() }
            override fun onStop() { pause() }

            // Handle Seek from Notification
            override fun onSeekTo(pos: Long) {
                super.onSeekTo(pos)
                exoPlayer?.seekTo(pos)
                updateMediaSessionState(if (isPlaying()) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED)
            }

            override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    mediaButtonEvent?.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                } else {
                    mediaButtonEvent?.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                }
                if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_NEXT -> playNext()
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> playPrev()
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> if(isPlaying()) pause() else play()
                        KeyEvent.KEYCODE_MEDIA_PLAY -> play()
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> pause()
                    }
                    return true
                }
                return super.onMediaButtonEvent(mediaButtonEvent)
            }

            // Handle Custom Actions from Notification
            override fun onCustomAction(action: String?, extras: Bundle?) {
                when (action) {
                    ACTION_OPEN_BOOSTER -> {
                        val i = Intent(applicationContext, SoundBoosterActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(i)
                    }
                    ACTION_OPEN_EQ -> {
                        val i = Intent(applicationContext, EqualizerSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(i)
                    }
                    ACTION_TOGGLE_HD -> {
                        val current = HDAudioManager.isHdEnabled(this@MusicService)
                        HDAudioManager.setHdEnabled(this@MusicService, !current)
                    }
                }
            }
        })

        mediaSession?.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession?.isActive = true
    }

    private fun loadLastPlayedSong() {
        val lastId = SettingsManager.getLastSongId(this)
        if (lastId == -1L) return

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        try {
            contentResolver.query(uri, projection, "${MediaStore.Audio.Media._ID} = ?", arrayOf(lastId.toString()), null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val duration = cursor.getLong(4)
                    val song = Song(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), duration, cursor.getLong(5), "")
                    songList.clear()
                    songList.add(song)
                    songPos = 0

                    // FIX: Use Content URI for direct playback from internal logic
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
                    val mimeType = FormatUtils.getMimeType(this, contentUri)

                    val item = MediaItem.Builder()
                        .setUri(contentUri)
                        .setMimeType(mimeType)
                        .build()

                    exoPlayer?.setMediaItem(item)
                    exoPlayer?.prepare()

                    val lastPos = SettingsManager.getLastSongPos(this)
                    if (lastPos > 0) exoPlayer?.seekTo(lastPos.toLong())

                    currentAlbumArtBitmap = getAlbumArtBitmap(song.albumId)
                    checkIsFavorite() // Check favorite state

                    if ((exoPlayer?.audioSessionId ?: 0) != 0) {
                        rebuildAudioEffects(exoPlayer!!.audioSessionId)
                    }

                    updateMediaMetadata()
                    updateMediaSessionState(PlaybackStateCompat.STATE_PAUSED)
                    showNotification()
                    notifyUI()
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun setList(songs: ArrayList<Song>) {
        songList = songs
        songPos = 0
    }

    fun setSong(index: Int) {
        songPos = index
        playSong()
    }

    private fun playSong() {
        if (songList.isEmpty()) return
        val song = songList[songPos]

        SettingsManager.setLastSongId(this, song.id)
        HistoryManager.addSongToHistory(this, song.id)
        checkIsFavorite() // Update favorite state

        try {
            // FIX: Use Content URI to bypass file path restrictions on Android 11+ (M4A Support)
            val uri = if (song.id != -1L) {
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
            } else {
                Uri.parse(song.path)
            }

            val mimeType = FormatUtils.getMimeType(this, uri)

            val item = MediaItem.Builder()
                .setUri(uri)
                .setMimeType(mimeType)
                .build()

            exoPlayer?.setMediaItem(item)
            exoPlayer?.prepare()
            exoPlayer?.play()

            if (lastLoadedAlbumId != song.albumId) {
                currentAlbumArtBitmap = getAlbumArtBitmap(song.albumId)
                lastLoadedAlbumId = song.albumId
            }
            updateMediaMetadata()
        } catch (e: Exception) {
            playNext()
        }
    }

    fun play() { if (songList.isNotEmpty()) exoPlayer?.play() }

    fun pause() {
        if (isPlaying()) {
            exoPlayer?.pause()
            SettingsManager.setLastSongPos(this, getCurrentPosition())
        }
    }

    fun playNext() {
        if (isShuffle) {
            songPos = (0 until songList.size).random()
        } else {
            songPos++
            if (songPos >= songList.size) songPos = 0
        }
        playSong()
    }

    fun playPrev() {
        songPos--
        if (songPos < 0) songPos = songList.size - 1
        playSong()
    }

    fun isPlaying() = exoPlayer?.isPlaying == true
    fun getCurrentPosition() = exoPlayer?.currentPosition?.toInt() ?: 0
    fun getDuration() = if(exoPlayer?.duration != null && exoPlayer!!.duration > 0) exoPlayer!!.duration.toInt() else songList.getOrNull(songPos)?.duration?.toInt() ?: 0
    fun seekTo(pos: Int) { exoPlayer?.seekTo(pos.toLong()) }
    fun getCurrentSong() = if (songList.isNotEmpty()) songList[songPos] else null

    private fun onCompletion() {
        if (isRepeat) {
            exoPlayer?.seekTo(0)
            exoPlayer?.play()
        } else {
            playNext()
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        if (intent?.action != null) {
            when (intent.action) {
                ACTION_PLAY -> play()
                ACTION_PAUSE -> pause()
                ACTION_NEXT -> playNext()
                ACTION_PREVIOUS -> playPrev()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try{ unregisterReceiver(becomingNoisyReceiver) }catch(e:Exception){};
        settingsWatcher?.stopWatching()
        mediaSession?.release()
        exoPlayer?.release()
        releaseAudioEffects()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if(exoPlayer != null) SettingsManager.setLastSongPos(this, getCurrentPosition())
        stopSelf()
    }

    fun setSleepTimer(min: Int) {
        timerHandler.removeCallbacks(timerRunnable)
        if(min>0) timerHandler.postDelayed(timerRunnable, min*60*1000L)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_LOW)
        channel.description = "Media Playback Controls"
        channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun updateMediaSessionState(state: Int) {
        val playbackState = if (isPlaying()) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val speed = if (isPlaying()) 1.0f else 0f

        val builder = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    or PlaybackStateCompat.ACTION_SEEK_TO) // Enable Seek
            .setState(state, getCurrentPosition().toLong(), speed, SystemClock.elapsedRealtime())

        // CUSTOM ACTIONS FOR LOCK SCREEN (Android 13+)
        builder.addCustomAction(ACTION_OPEN_BOOSTER, "Booster", R.drawable.ic_tune)
        builder.addCustomAction(ACTION_OPEN_EQ, "Equalizer", R.drawable.ic_equalizer)

        mediaSession?.setPlaybackState(builder.build())
    }

    // Add Duration Metadata
    private fun updateMediaMetadata() {
        if(songList.isEmpty()) return
        val song = songList[songPos]

        // Prefer player duration if known, else file metadata duration
        val duration = if (exoPlayer != null && exoPlayer!!.duration > 0) exoPlayer!!.duration else song.duration

        mediaSession?.setMetadata(MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentAlbumArtBitmap).build())
    }

    private fun getAlbumArtBitmap(id: Long): Bitmap? {
        return try {
            val pfd = contentResolver.openFileDescriptor(android.content.ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id), "r")
            if(pfd!=null) BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor).also { pfd.close() } else null
        } catch(e: Exception) { null }
    }

    private fun notifyUI() = serviceCallbacks?.onUpdateUI()

    private fun showNotification() {
        val song = songList.getOrNull(songPos) ?: return

        // Main Intent
        val notifIntent = Intent(this, PlayerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pIntent = PendingIntent.getActivity(this, 0, notifIntent, PendingIntent.FLAG_IMMUTABLE)

        // Actions
        val prevPending = PendingIntent.getService(this, 0, Intent(this, MusicService::class.java).setAction(ACTION_PREVIOUS), PendingIntent.FLAG_IMMUTABLE)
        val playAction = if (isPlaying()) ACTION_PAUSE else ACTION_PLAY
        val playPending = PendingIntent.getService(this, 1, Intent(this, MusicService::class.java).setAction(playAction), PendingIntent.FLAG_IMMUTABLE)
        val nextPending = PendingIntent.getService(this, 2, Intent(this, MusicService::class.java).setAction(ACTION_NEXT), PendingIntent.FLAG_IMMUTABLE)

        // Booster & EQ Intents (Pop-up style)
        val boosterIntent = Intent(this, SoundBoosterActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val boosterPending = PendingIntent.getActivity(this, 10, boosterIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val eqIntent = Intent(this, EqualizerSettingsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val eqPending = PendingIntent.getActivity(this, 11, eqIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setLargeIcon(currentAlbumArtBitmap)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setContentIntent(pIntent)
            .setOngoing(isPlaying())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)

            // 5 Actions: Booster | Prev | Play | Next | EQ
            .addAction(R.drawable.ic_tune, "Boost", boosterPending)
            .addAction(R.drawable.ic_previous, "Prev", prevPending)
            .addAction(if (isPlaying()) R.drawable.ic_pause else R.drawable.ic_play, "Play", playPending)
            .addAction(R.drawable.ic_next, "Next", nextPending)
            .addAction(R.drawable.ic_equalizer, "EQ", eqPending)

            // Compact view
            .setStyle(MediaStyle()
                .setShowActionsInCompactView(1, 2, 3)
                .setMediaSession(mediaSession?.sessionToken))
            .build()

        startForeground(R.id.music_notification_id, notification)
    }
}