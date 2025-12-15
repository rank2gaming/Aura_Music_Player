package com.rank2gaming.aura.activities

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.rank2gaming.aura.R
import com.rank2gaming.aura.adapters.SongAdapter
import com.rank2gaming.aura.databinding.ActivityAlbumDetailsBinding
import com.rank2gaming.aura.model.Song
import com.rank2gaming.aura.service.MusicService
import com.rank2gaming.aura.utils.FileUtils
import com.rank2gaming.aura.utils.ThemeManager

class AlbumDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlbumDetailsBinding
    private val albumSongs = ArrayList<Song>()
    private var albumId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlbumDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ThemeManager.applyTheme(this, binding.root)

        albumId = intent.getLongExtra("album_id", -1)

        setupUI()
        loadAlbumSongs()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadAlbumSongs() {
        albumSongs.clear()

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        // Filter by Album ID
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.ALBUM_ID} = ?"
        val selectionArgs = arrayOf(albumId.toString())
        val sortOrder = "${MediaStore.Audio.Media.TRACK} ASC" // Sort by Track Number usually preferred for albums

        try {
            val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
            cursor?.use {
                while (it.moveToNext()) {
                    val path = it.getString(3)
                    albumSongs.add(Song(
                        it.getLong(0),
                        it.getString(1),
                        it.getString(2),
                        path,
                        it.getLong(4),
                        it.getLong(5),
                        ""
                    ))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        // Update UI info based on the first song found
        if (albumSongs.isNotEmpty()) {
            val firstSong = albumSongs[0]
            // We don't have exact Album Name in Song model, but usually Title/Artist helps context
            // Ideally, you'd pass Album Name via Intent or query MediaStore.Audio.Albums
            // For now, we rely on the header or fetch album info separately if needed.
            // Let's assume the passed intent might have had the name, or we just display Artist
            binding.txtAlbumArtistDetail.text = firstSong.artist

            // Set Header Image
            val artUri = FileUtils.getAlbumArtUri(albumId)
            Glide.with(this)
                .load(artUri)
                .placeholder(R.drawable.ic_music_note)
                .centerCrop()
                .into(binding.imgAlbumHeader)
        }

        // Get Album Name passed via Intent if possible, else generic
        val namePassed = intent.getStringExtra("album_name") ?: "Unknown Album"
        binding.txtAlbumNameDetail.text = namePassed

        binding.txtAlbumStats.text = "${albumSongs.size} Songs"

        // Setup Adapter
        binding.recyclerAlbumSongs.layoutManager = LinearLayoutManager(this)
        val adapter = SongAdapter(albumSongs, this,
            onClick = { song ->
                val intent = Intent(this, MusicService::class.java)
                intent.action = MusicService.ACTION_PLAY
                // In a full implementation, you'd pass the specific song to play
                startService(intent)
            },
            onMenuClick = { _, view ->
                val popup = PopupMenu(this, view)
                popup.menu.add("Play")
                popup.show()
            }
        )
        binding.recyclerAlbumSongs.adapter = adapter
    }
}