package com.rank2gaming.aura.youtube

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.rank2gaming.aura.youtube.databinding.ActivityYoutubeDashboardBinding
import com.rank2gaming.aura.youtube.utils.VideoAdapter
import com.rank2gaming.aura.youtube.utils.YouTubeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class YouTubeDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityYoutubeDashboardBinding

    // --- REAL REPOSITORY (Active) ---
    // Matches the refactored structure using NetworkDataSource
    private val repository = YouTubeRepository()

    // --- MOCK REPOSITORY (Disabled) ---
    // private val mockRepository = com.rank2gaming.aura.youtube.utils.MockYouTubeRepository()

    private val adapter = VideoAdapter { videoId ->
        val intent = Intent(this, YouTubePlayerActivity::class.java)
        intent.putExtra("VIDEO_ID", videoId)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYoutubeDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Back Button
        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        loadVideos()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadVideos() {
        // Show Loading State
        binding.progressBar.visibility = View.VISIBLE
        binding.txtError.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE

        // Fetch Data on Background Thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // CALLING REAL API
                val videos = repository.getMusicVideos()

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE

                    if (videos.isNotEmpty()) {
                        binding.recyclerView.visibility = View.VISIBLE
                        adapter.setList(videos)
                    } else {
                        showError("No videos found. Check API Quota or Region.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    // Displays the specific error from NetworkDataSource (e.g., "403 Access Denied")
                    showError("Error: ${e.message}")
                }
            }
        }
    }

    private fun showError(msg: String) {
        binding.txtError.text = msg
        binding.txtError.visibility = View.VISIBLE
    }
}