package com.rank2gaming.aura.activities

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.rank2gaming.aura.R
import com.rank2gaming.aura.adapters.SongAdapter
import com.rank2gaming.aura.databinding.ActivityArtistDetailsBinding
import com.rank2gaming.aura.model.Song
import com.rank2gaming.aura.service.MusicService
import com.rank2gaming.aura.utils.FileUtils
import com.rank2gaming.aura.utils.ThemeManager

class ArtistDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArtistDetailsBinding
    private val artistSongs = ArrayList<Song>()
    private var artistName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtistDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply Theme
        ThemeManager.applyTheme(this, binding.root)

        // Get Data
        artistName = intent.getStringExtra("artist_name") ?: "Unknown Artist"

        setupUI()
        loadArtistSongs()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "" // Hide default title, we use the custom TextView

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.txtArtistNameDetail.text = artistName
    }

    private fun loadArtistSongs() {
        artistSongs.clear()

        // Query MediaStore for songs by this artist
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.ARTIST} = ?"
        val selectionArgs = arrayOf(artistName)
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
            cursor?.use {
                while (it.moveToNext()) {
                    val path = it.getString(3)
                    artistSongs.add(Song(
                        it.getLong(0),
                        it.getString(1),
                        it.getString(2),
                        path,
                        it.getLong(4),
                        it.getLong(5),
                        "" // Folder path not needed here
                    ))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        // Update UI
        binding.txtArtistStats.text = "${artistSongs.size} Songs"

        // Set Header Image (Use album art from the first song found)
        if (artistSongs.isNotEmpty()) {
            val artUri = FileUtils.getAlbumArtUri(artistSongs[0].albumId)
            Glide.with(this)
                .load(artUri)
                .placeholder(R.drawable.ic_music_note)
                .centerCrop()
                .into(binding.imgArtistHeader)
        }

        // Setup RecyclerView
        binding.recyclerArtistSongs.layoutManager = LinearLayoutManager(this)
        val adapter = SongAdapter(artistSongs, this,
            onClick = { song ->
                // Play logic: Use Intent to start service or notify main activity
                val intent = Intent(this, MusicService::class.java)
                intent.action = MusicService.ACTION_PLAY
                // Note: In a real app, passing the whole list to service is better.
                // For now, we simulate playing by sending a broadcast or handling in PlayerActivity
                startService(intent)
                // You might need to update the Service's list here
            },
            onMenuClick = { _, view ->
                // Simple menu placeholder
                val popup = PopupMenu(this, view)
                popup.menu.add("Play")
                popup.show()
            }
        )
        binding.recyclerArtistSongs.adapter = adapter
    }
}