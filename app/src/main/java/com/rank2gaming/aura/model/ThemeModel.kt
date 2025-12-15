package com.rank2gaming.aura.model

import androidx.annotation.DrawableRes

enum class LayoutType {
    BOTTOM_HORIZONTAL, // Standard player at bottom
    VERTICAL_RIGHT,    // Buttons stacked on the right
    VERTICAL_LEFT,     // Buttons stacked on the left
    SPLIT_CORNERS,     // Play center, Prev/Next in corners
    CENTER_MINIMAL     // All buttons floating in center over art
}

data class ThemeModel(
    val id: Int,
    val name: String,
    @DrawableRes val backgroundResId: Int, // e.g., R.drawable.bg_gradient_1
    val layoutType: LayoutType,
    val buttonTintColor: String // Hex code e.g., "#FFFFFF"
)