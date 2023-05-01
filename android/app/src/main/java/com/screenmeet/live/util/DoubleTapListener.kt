package com.screenmeet.live.util

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

class DoubleTapListener(val context: Context, onDoubleTap: () -> Unit) : View.OnTouchListener {
    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                onDoubleTap()
                return super.onDoubleTap(e)
            }
        }
    )

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }
}
