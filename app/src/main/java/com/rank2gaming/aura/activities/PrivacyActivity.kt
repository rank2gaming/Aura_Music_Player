package com.rank2gaming.aura.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rank2gaming.aura.databinding.ActivityPrivacyBinding
import com.rank2gaming.aura.utils.ThemeManager

class PrivacyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrivacyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ThemeManager.applyTheme(this, binding.root)

        binding.btnBack.setOnClickListener { finish() }
    }
}