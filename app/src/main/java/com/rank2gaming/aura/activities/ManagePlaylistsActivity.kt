package com.rank2gaming.aura.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.rank2gaming.aura.adapters.ManageData
import com.rank2gaming.aura.adapters.ManagePlaylistAdapter
import com.rank2gaming.aura.databinding.ActivityManagePlaylistsBinding
import com.rank2gaming.aura.utils.PlaylistBackupManager
import com.rank2gaming.aura.utils.PlaylistCoverManager
import com.rank2gaming.aura.utils.PlaylistManager
import com.rank2gaming.aura.utils.ThemeManager

class ManagePlaylistsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManagePlaylistsBinding
    private var selectedPlaylistForCover: String? = null

    // Image Picker
    private val imagePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && selectedPlaylistForCover != null) {
            val uri = result.data?.data
            if (uri != null) {
                PlaylistCoverManager.saveCover(this, selectedPlaylistForCover!!, uri.toString())
                loadPlaylists() // Refresh UI
                Toast.makeText(this, "Profile Picture Updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Restore File Picker
    private val restorePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                PlaylistBackupManager.restorePlaylists(this, uri)
                loadPlaylists()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagePlaylistsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyTheme(this, binding.root)

        setupUI()
        loadPlaylists()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnBackup.setOnClickListener { PlaylistBackupManager.backupPlaylists(this) }

        binding.btnRestore.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "application/json"
            restorePicker.launch(intent)
        }
    }

    private fun loadPlaylists() {
        val playlists = PlaylistManager.getPlaylists(this)

        if (playlists.isEmpty()) {
            binding.recyclerManage.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.recyclerManage.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE

            val dataList = playlists.map { ManageData(it.key, it.value.size) }

            binding.recyclerManage.layoutManager = LinearLayoutManager(this)
            binding.recyclerManage.adapter = ManagePlaylistAdapter(dataList,
                onCoverClick = { name ->
                    selectedPlaylistForCover = name
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    imagePicker.launch(intent)
                },
                onRenameClick = { name -> showRenameDialog(name) },
                onDeleteClick = { name -> showDeleteDialog(name) }
            )
        }
    }

    private fun showRenameDialog(oldName: String) {
        val input = EditText(this)
        input.setText(oldName)
        AlertDialog.Builder(this)
            .setTitle("Rename Playlist")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != oldName) {
                    // Logic to rename in PlaylistManager (Need to implement rename in Manager ideally)
                    // For now: Create new, Copy songs, Delete old
                    val songs = PlaylistManager.getPlaylists(this)[oldName] ?: emptyList()
                    PlaylistManager.createPlaylist(this, newName)
                    songs.forEach { PlaylistManager.addSongToPlaylist(this, newName, it) }
                    PlaylistManager.deletePlaylist(this, oldName)

                    // Move Cover Art
                    val oldCover = PlaylistCoverManager.getCover(this, oldName)
                    if (oldCover != null) {
                        PlaylistCoverManager.saveCover(this, newName, oldCover)
                        PlaylistCoverManager.removeCover(this, oldName)
                    }

                    loadPlaylists()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(name: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Playlist")
            .setMessage("Are you sure you want to delete '$name'?")
            .setPositiveButton("Delete") { _, _ ->
                PlaylistManager.deletePlaylist(this, name)
                PlaylistCoverManager.removeCover(this, name)
                loadPlaylists()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}