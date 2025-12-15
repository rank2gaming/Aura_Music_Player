package com.rank2gaming.aura.utils

import android.content.Context
import com.rank2gaming.aura.R

object TemplateManager {

    private const val PREF_NAME = "AuraLayoutPrefs"
    private const val KEY_LAYOUT_INDEX = "current_layout_index"

    data class LayoutOption(
        val index: Int,
        val layoutRes: Int // The actual XML Layout ID
    )

    // === LAYOUT MAPPING ===
    // Maps Index 0-10 to the specific Layout Files.
    // Note: Ensure all these XML files (activity_player1 to activity_player10) exist in your project.
    val layouts = listOf(
        LayoutOption(0, R.layout.activity_player),      // Default
        LayoutOption(1, R.layout.activity_player1),     // Vip Style
        LayoutOption(2, R.layout.activity_player2),     // Warm Vibes
        LayoutOption(3, R.layout.activity_player3),     // Cool Breeze
        LayoutOption(4, R.layout.activity_player4),     // Vibrant Pop
        LayoutOption(5, R.layout.activity_player5),     // Rainy Mood
        LayoutOption(6, R.layout.activity_player6),     // Sunset Circle
        LayoutOption(7, R.layout.activity_player7),     // Galaxy Night
        LayoutOption(8, R.layout.activity_player8),     // Clean Minimal
        LayoutOption(9, R.layout.activity_player9),     // Ocean Waves
        LayoutOption(10, R.layout.activity_player10)    // Nature Green
    )

    fun getLayoutResId(context: Context): Int {
        val index = getLayoutIndex(context)
        return layouts.find { it.index == index }?.layoutRes ?: R.layout.activity_player
    }

    fun getLayoutIndex(context: Context): Int {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_LAYOUT_INDEX, 0)
    }

    fun saveLayoutIndex(context: Context, index: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_LAYOUT_INDEX, index)
            .apply()
    }
}