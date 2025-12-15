package com.rank2gaming.aura.youtube

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.rank2gaming.aura.youtube.databinding.ActivityYoutubeDashboardBinding
import com.rank2gaming.aura.youtube.utils.VideoAdapter
// import com.rank2gaming.aura.youtube.utils.YouTubeRepository // Commented out for testing
// import com.rank2gaming.aura.youtube.utils.MockYouTubeRepository // Ensure you created this file
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class YouTubeDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityYoutubeDashboardBinding

    // --- REAL REPOSITORY (Commented Out) ---
    // private val repository = YouTubeRepository()

    // --- MOCK REPOSITORY (Active) ---
    // NOTE: You must have the MockYouTubeRepository.kt file created as per previous instructions
    private val mockRepository = com.rank2gaming.aura.youtube.utils.MockYouTubeRepository()

    private val adapter = VideoAdapter { videoId ->
        val intent = Intent(this, YouTubePlayerActivity::class.java)
        intent.putExtra("VIDEO_ID", videoId)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYoutubeDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        loadVideos()
    }

    private fun loadVideos() {
        binding.progressBar.visibility = View.VISIBLE
        binding.txtError.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // --- TEST MODE: LOAD FAKE DATA ---
                // Ensure your MockRepository returns the correct 'VideoItem' type
                val videos = mockRepository.getMockVideos()

                // --- REAL MODE: UNCOMMENT TO USE API ---
                // val videos = repository.getMusicVideos()

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    if (videos.isNotEmpty()) {
                        binding.recyclerView.visibility = View.VISIBLE
                        // Make sure VideoAdapter expects the list type returned by MockRepo
                        adapter.setList(videos)
                    } else {
                        showError("No videos found.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    showError("Error: ${e.message}")
                }
            }
        }
    }

    private fun showError(msg: String) {
        binding.txtError.text = msg
        binding.txtError.visibility = View.VISIBLE
        // Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}