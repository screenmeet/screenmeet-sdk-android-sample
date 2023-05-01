package com.screenmeet.live.overlay

import android.content.Context
import androidx.activity.ComponentActivity
import com.screenmeet.sdk.VideoElement
class WidgetManager(val context: Context) {

    private var videoOverlay: VideoOverlay? = null

    private var overlayPermissionDenied: Boolean = false

    fun showFloatingWidget(activity: ComponentActivity, videoElement: VideoElement) {
        if (PermissionProvider.canDrawOverlay(context)) {
            doShowFloatingWidget(videoElement)
        } else {
            if (overlayPermissionDenied) return
            PermissionProvider.requestOverlay(context, activity.activityResultRegistry) { granted ->
                if (granted) {
                    doShowFloatingWidget(videoElement)
                } else {
                    overlayPermissionDenied = true
                }
            }
        }
    }

    private fun doShowFloatingWidget(videoElement: VideoElement) {
        val overlayAttached: Boolean
        if (videoOverlay == null) {
            val overlay = VideoOverlay(context)
            overlayAttached = overlay.showOverlay()
            if (overlayAttached) {
                videoOverlay = overlay
            }
        } else {
            overlayAttached = true
        }

        if (overlayAttached) {
            videoOverlay?.attachVideoTrack(videoElement)
        }
    }

    fun hideFloatingWidget() {
        videoOverlay?.let {
            it.hideOverlay()
            videoOverlay = null
        }
    }
}
