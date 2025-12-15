package com.rank2gaming.aura.model

import androidx.annotation.LayoutRes

data class PlayerTheme(
    val id: Int,
    val name: String,
    @LayoutRes val layoutResId: Int,
    val isLight: Boolean = false
)