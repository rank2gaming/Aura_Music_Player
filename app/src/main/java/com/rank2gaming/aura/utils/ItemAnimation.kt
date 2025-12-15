package com.rank2gaming.aura.utils

import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import com.rank2gaming.aura.R

object ItemAnimation {

    fun animate(view: View, position: Int, lastPosition: Int): Int {
        // Only animate if the item hasn't been displayed yet or direction changes
        // However, to ensure animation happens in ANY direction as requested:

        val animationId = if (position > lastPosition) {
            // User is scrolling DOWN (Loading items 5, 6, 7...)
            R.anim.anim_scroll_up // Items slide up from bottom
        } else {
            // User is scrolling UP (Loading items 3, 2, 1...)
            R.anim.anim_scroll_down // Items scale in from center
        }

        val animation = AnimationUtils.loadAnimation(view.context, animationId)
        view.startAnimation(animation)

        return position
    }
}