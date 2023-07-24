package com.screenmeet.live.util

import android.graphics.drawable.GradientDrawable
import android.widget.ImageButton
import androidx.core.content.ContextCompat

fun <T : Any> tryOrNull(body: () -> T?): T? {
    return try {
        body()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun ImageButton.setButtonBackgroundColor(colorRes: Int) {
    val background = background as GradientDrawable
    background.setColor(ContextCompat.getColor(context, colorRes))
}
