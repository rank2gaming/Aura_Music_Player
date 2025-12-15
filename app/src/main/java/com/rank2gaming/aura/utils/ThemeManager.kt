package com.rank2gaming.aura.utils

import android.app.Activity
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import com.rank2gaming.aura.R

object ThemeManager {

    private const val PREF_NAME = "AuraThemePrefs"
    private const val KEY_THEME_INDEX = "theme_index"

    // 10 Theme Gradients (Start Color, End Color)
    private val themeColors = listOf(
        Pair(0xFF2E00B8, 0xFF1A0068), // 0: Default Aura Purple
        Pair(0xFFB71C1C, 0xFF4A0000), // 1: Crimson
        Pair(0xFF0D47A1, 0xFF001064), // 2: Deep Blue
        Pair(0xFF1B5E20, 0xFF003300), // 3: Forest Green
        Pair(0xFFE65100, 0xFFBF360C), // 4: Burnt Orange
        Pair(0xFF4A148C, 0xFF2A003E), // 5: Deep Violet
        Pair(0xFF263238, 0xFF000000), // 6: Midnight Grey
        Pair(0xFF880E4F, 0xFF4A002C), // 7: Pink Velvet
        Pair(0xFF006064, 0xFF00363A), // 8: Cyan Dark
        Pair(0xFF3E2723, 0xFF1B0000)  // 9: Chocolate
    )

    fun saveTheme(context: Context, index: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_THEME_INDEX, index).apply()
    }

    fun getThemeIndex(context: Context): Int {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_THEME_INDEX, 0)
    }

    fun applyTheme(activity: Activity, rootView: View) {
        val index = getThemeIndex(activity)
        val colors = themeColors.getOrElse(index) { themeColors[0] }

        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(colors.first.toInt(), colors.second.toInt())
        )
        rootView.background = gradient
    }
}