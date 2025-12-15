package com.rank2gaming.aura.activities

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import com.rank2gaming.aura.adapters.SongAdapter
import com.rank2gaming.aura.databinding.ActivityFolderDetailsBinding
import com.rank2gaming.aura.model.Song
import com.rank2gaming.aura.service.MusicService
import com.rank2gaming.aura.utils.ThemeManager
import java.io.File

class FolderDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFolderDetailsBinding
    private val folderSongs = ArrayList<Song>()
    private var folderPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFolderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ThemeManager.applyTheme(this, binding.root)

        folderPath = intent.getStringExtra("folder_path") ?: ""
        val folderName = try { File(folderPath).name } catch (e: Exception) { "Folder" }

        setupUI(folderName)
        loadFolderSongs()
    }

    private fun setupUI(title: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = title
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadFolderSongs() {
        folderSongs.clear()

        // Query ALL songs, then filter manually by path (safest for folders)
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = contentResolver.query(uri, projection, selection, null, sortOrder)
            cursor?.use {
                while (it.moveToNext()) {
                    val path = it.getString(3)
                    // Check if this song belongs to the passed folder path
                    if (path.startsWith(folderPath)) {
                        // Extract parent folder to ensure exact match (not sub-sub folders if unwanted)
                        val parent = try { path.substringBeforeLast("/") } catch (e: Exception) { "" }
                        if (parent == folderPath) {
                            folderSongs.add(Song(
                                it.getLong(0),
                                it.getString(1),
                                it.getString(2),
                                path,
                                it.getLong(4),
                                it.getLong(5),
                                parent
                            ))
                        }
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        // Setup Recycler
        binding.recyclerFolderSongs.layoutManager = LinearLayoutManager(this)
        val adapter = SongAdapter(folderSongs, this,
            onClick = { song ->
                val intent = Intent(this, MusicService::class.java)
                intent.action = MusicService.ACTION_PLAY
                startService(intent)
            },
            onMenuClick = { _, view ->
                val popup = PopupMenu(this, view)
                popup.menu.add("Play")
                popup.show()
            }
        )
        binding.recyclerFolderSongs.adapter = adapter
    }
}