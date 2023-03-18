package com.screenmeet.live.overlay

import android.content.Context
import androidx.activity.ComponentActivity
import com.screenmeet.sdk.VideoElement

class WidgetManager(val context: Context) {

    private var videoOverlay: VideoOverlay? = null
    private var overlayPermissionDenied: Boolean = false
    private val configurationWatcher = ConfigurationWatcher(context)

    init {
        configurationWatcher.subscribeChanges {
            updateScreenConfig(it)
        }
    }

    fun showFloatingWidget(activity: ComponentActivity, videoElement: VideoElement) {
        if (PermissionProvider.canDrawOverlay(context)) {
            doShowFloatingWidget(configurationWatcher.screenConfig, videoElement)
        } else {
            if (overlayPermissionDenied) return
            PermissionProvider.requestOverlay(context, activity.activityResultRegistry) { granted ->
                if (granted) {
                    doShowFloatingWidget(configurationWatcher.screenConfig, videoElement)
                } else {
                    overlayPermissionDenied = true
                }
            }
        }
    }

    private fun doShowFloatingWidget(screenConfig: ScreenConfig, videoElement: VideoElement) {
        val overlayAttached: Boolean
        if (videoOverlay == null) {
            val overlay = VideoOverlay(context)
            overlayAttached = overlay.showOverlay(screenConfig)
            if (overlayAttached) {
                videoOverlay = overlay
            }
        } else overlayAttached = true

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

    private fun updateScreenConfig(screenConfig: ScreenConfig) {
        videoOverlay?.updateScreenConfig(screenConfig)
    }
}
