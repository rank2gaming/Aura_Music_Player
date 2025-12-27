package com.rank2gaming.aura.youtube

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.rank2gaming.aura.youtube.databinding.ActivityLocalVideoListBinding
import com.rank2gaming.aura.youtube.utils.LocalVideoAdapter
import com.rank2gaming.aura.youtube.utils.LocalVideoItem

// FIX: Added OptIn for Media3 Unstable API usage
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class LocalVideoListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocalVideoListBinding
    private val PERMISSION_REQUEST_CODE = 101
    private val videoList = ArrayList<LocalVideoItem>()
    private lateinit var adapter: LocalVideoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalVideoListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        checkPermissions()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = LocalVideoAdapter(videoList) { videoUri ->
            val intent = Intent(this, LocalPlayerActivity::class.java)
            intent.putExtra("VIDEO_URI", videoUri.toString())
            startActivity(intent)
        }
        binding.recyclerView.adapter = adapter
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_VIDEO), PERMISSION_REQUEST_CODE)
            } else {
                loadLocalVideos()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
            } else {
                loadLocalVideos()
            }
        }
    }

    private fun loadLocalVideos() {
        videoList.clear()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )

        try {
            val cursor = contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn)
                    val duration = it.getLong(durationColumn)
                    val contentUri: Uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    videoList.add(LocalVideoItem(contentUri, name, duration))
                }
            }
            if (videoList.isEmpty()) Toast.makeText(this, "No videos found", Toast.LENGTH_SHORT).show()
            adapter.notifyDataSetChanged()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading videos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) loadLocalVideos()
            else Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }
}