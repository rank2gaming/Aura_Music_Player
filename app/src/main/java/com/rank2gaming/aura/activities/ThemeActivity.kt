package com.rank2gaming.aura.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.rank2gaming.aura.databinding.ActivityThemeBinding
import com.rank2gaming.aura.utils.ThemeManager

class ThemeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityThemeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ThemeManager.applyTheme(this, binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val themes = arrayOf(
            "Aura Purple", "Crimson", "Deep Blue", "Forest", "Orange",
            "Violet", "Midnight", "Pink", "Cyan", "Chocolate"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, themes)
        binding.listThemes.adapter = adapter

        binding.listThemes.setOnItemClickListener { _, _, position, _ ->
            ThemeManager.saveTheme(this, position)

            // Restart App to apply theme globally
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}