package com.rank2gaming.aura.activities

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import com.rank2gaming.aura.R
import com.rank2gaming.aura.adapters.SongAdapter
import com.rank2gaming.aura.databinding.ActivityPlaylistDetailsBinding
import com.rank2gaming.aura.model.Song
import com.rank2gaming.aura.service.MusicService
import com.rank2gaming.aura.utils.HistoryManager
import com.rank2gaming.aura.utils.PlaylistManager
import com.rank2gaming.aura.utils.ThemeManager

class PlaylistDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistDetailsBinding
    private val playlistSongs = ArrayList<Song>()
    private var playlistName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyTheme(this, binding.root)

        playlistName = intent.getStringExtra("playlist_name") ?: "Playlist"
        setupUI()
        loadPlaylistData()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.txtPlaylistNameDetail.text = playlistName

        when (playlistName) {
            "My favourite" -> binding.imgPlaylistHeader.setImageResource(R.drawable.ic_favorite)
            "Recently added" -> binding.imgPlaylistHeader.setImageResource(R.drawable.ic_music_note)
            "Recently played" -> binding.imgPlaylistHeader.setImageResource(R.drawable.ic_history)
            "My top tracks" -> binding.imgPlaylistHeader.setImageResource(R.drawable.ic_star)
            else -> binding.imgPlaylistHeader.setImageResource(R.drawable.ic_music_note)
        }
    }

    private fun loadPlaylistData() {
        playlistSongs.clear()
        when (playlistName) {
            "Recently added" -> loadRecentlyAdded()
            "Recently played" -> loadRecentlyPlayed()
            "My top tracks" -> loadRecentlyPlayed() // Reusing history for now
            else -> loadUserPlaylist(playlistName)
        }
        updateRecycler()
    }

    // Common Helper to process Cursor and apply "Unknown Artist" fix
    private fun processCursor(cursor: android.database.Cursor) {
        while (cursor.moveToNext()) {
            val rawArtist = cursor.getString(2)
            // FIX: REPLACE UNKNOWN ARTIST WITH APP NAME
            val finalArtist = if (rawArtist == null || rawArtist == "<unknown>") "Aura Music Player" else rawArtist

            val path = cursor.getString(3)
            playlistSongs.add(Song(cursor.getLong(0), cursor.getString(1), finalArtist, path, cursor.getLong(4), cursor.getLong(5), ""))
        }
    }

    private fun loadRecentlyAdded() {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM_ID)
        try {
            val cursor = contentResolver.query(uri, projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, "${MediaStore.Audio.Media.DATE_ADDED} DESC LIMIT 50")
            cursor?.use { processCursor(it) }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun loadRecentlyPlayed() {
        val recentIds = HistoryManager.getRecentSongIds(this)
        if (recentIds.isEmpty()) return
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM_ID)
        val selection = "${MediaStore.Audio.Media._ID} IN (${recentIds.joinToString(",")})"

        val tempMap = HashMap<Long, Song>()
        try {
            val cursor = contentResolver.query(uri, projection, selection, null, null)
            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val rawArtist = it.getString(2)
                    val finalArtist = if (rawArtist == null || rawArtist == "<unknown>") "Aura Music Player" else rawArtist
                    val song = Song(id, it.getString(1), finalArtist, it.getString(3), it.getLong(4), it.getLong(5), "")
                    tempMap[id] = song
                }
            }
        } catch (e: Exception) {}

        // Preserve history order
        for (id in recentIds) { if (tempMap.containsKey(id)) playlistSongs.add(tempMap[id]!!) }
    }

    private fun loadUserPlaylist(name: String) {
        val songIds = PlaylistManager.getPlaylists(this)[name] ?: return
        if (songIds.isEmpty()) return
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM_ID)
        val selection = "${MediaStore.Audio.Media._ID} IN (${songIds.joinToString(",")})"
        try {
            val cursor = contentResolver.query(uri, projection, selection, null, null)
            cursor?.use { processCursor(it) }
        } catch (e: Exception) {}
    }

    private fun updateRecycler() {
        binding.txtPlaylistStats.text = "${playlistSongs.size} Songs"
        binding.recyclerPlaylistSongs.layoutManager = LinearLayoutManager(this)
        val adapter = SongAdapter(playlistSongs, this,
            onClick = { song -> val intent = Intent(this, MusicService::class.java); intent.action = MusicService.ACTION_PLAY; startService(intent) },
            onMenuClick = { _, view -> val popup = PopupMenu(this, view); popup.menu.add("Play"); popup.show() }
        )
        binding.recyclerPlaylistSongs.adapter = adapter
    }
}