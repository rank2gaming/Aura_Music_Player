package com.rank2gaming.aura.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.rank2gaming.aura.databinding.ActivitySplashBinding
import com.rank2gaming.aura.utils.AdMobManager
import com.rank2gaming.aura.utils.ThemeManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyTheme(this, binding.root)

        // 1. Initialize AdMob Manager
        AdMobManager.initialize(this)

        // 2. Start Loading Interstitial
        AdMobManager.loadInterstitial(this)

        // 3. Wait 2 seconds, then attempt to show ad or move on
        Handler(Looper.getMainLooper()).postDelayed({
            checkAdAndNavigate()
        }, 2000)
    }

    private fun checkAdAndNavigate() {
        if (isNavigating) return
        isNavigating = true

        // 4. Delegate Show Logic to Manager
        AdMobManager.showInterstitial(this) {
            // Callback runs when ad is closed or failed
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}