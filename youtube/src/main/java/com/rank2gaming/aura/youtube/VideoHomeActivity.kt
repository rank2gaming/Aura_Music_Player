package com.rank2gaming.aura.youtube

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rank2gaming.aura.youtube.databinding.ActivityVideoHomeBinding

class VideoHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = Color.parseColor("#FF0000") // YouTube Branding

        binding.cardLocal.setOnClickListener {
            startActivity(Intent(this, LocalVideoListActivity::class.java))
        }

        binding.cardOnline.setOnClickListener {
            startActivity(Intent(this, YouTubeDashboardActivity::class.java))
        }

        binding.btnBack.setOnClickListener { finish() }
    }
}