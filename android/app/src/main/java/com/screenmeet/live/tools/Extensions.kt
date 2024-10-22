package com.screenmeet.live.tools

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.screenmeet.live.R

fun <T : Any> tryOrNull(body: () -> T?): T? = try {
    body()
} catch (e: Exception) {
    e.printStackTrace()
    null
}
fun Context.showAlert(dialogTittle: String, message: String, confirmed: () -> Unit) {
    AlertDialog.Builder(this, R.style.RoundedDialog)
        .setTitle(dialogTittle)
        .setMessage(message)
        .setCancelable(false)
        .setPositiveButton("OK") { _, _ -> confirmed() }
        .setNegativeButton("CANCEL", null)
        .show()
}

fun ImageButton.setButtonBackgroundColor(colorRes: Int) {
    val background = background as GradientDrawable
    background.setColor(ContextCompat.getColor(context, colorRes))
}
