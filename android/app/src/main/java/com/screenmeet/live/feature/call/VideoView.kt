package com.screenmeet.live.feature.call

import android.content.Context
import android.util.AttributeSet
import com.screenmeet.sdk.accessSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import org.webrtc.VideoTrack

typealias onFramesStuck = (Boolean) -> Unit

class VideoView : SurfaceViewRenderer {

    private val stuckThresholdNs = 5000000000L // 5sec

    private lateinit var egl: EglBase.Context
    private var rendererEvents: RendererCommon.RendererEvents? = null

    private var videoTrack: VideoTrack? = null
    private var lastFrameNs: Long = 0
    private var frameStuck: Boolean = false
    private var frameTrackingJob: Job? = null

    val trackId: String?
        get() = videoTrack?.id()

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun init(eglContext: EglBase.Context, events: RendererCommon.RendererEvents?) {
        egl = eglContext
        rendererEvents = events
        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
    }

    fun render(track: VideoTrack?) {
        val currentTrack = videoTrack
        val hasTrack = currentTrack != null

        if (currentTrack == track) {
            return
        }

        if (track != null) {
            if (hasTrack) {
                currentTrack.accessSafe {
                    it.removeSink(frameTrackingSink)
                }
            } else {
                super.init(egl, rendererEvents)
            }
            videoTrack = track
            track.accessSafe {
                it.addSink(frameTrackingSink)
            }
        } else {
            clear()
        }
    }

    fun clear() {
        videoTrack.accessSafe {
            it.removeSink(frameTrackingSink)
        }
        clearImage()
        release()
        videoTrack = null
        frameTrackingJob?.cancel()
        frameTrackingJob = null
    }

    fun listenFramesStuck(scope: CoroutineScope, stuck: onFramesStuck) {
        frameTrackingJob = scope.launch {
            stuck(frameStuck)
            while (true) {
                if (videoTrack != null) {
                    if (System.nanoTime() - lastFrameNs > stuckThresholdNs) {
                        if (!frameStuck) {
                            stuck(true)
                            frameStuck = true
                        }
                    } else {
                        if (frameStuck) {
                            stuck(false)
                            frameStuck = false
                        }
                    }
                } else {
                    if (frameStuck) {
                        stuck(false)
                        frameStuck = false
                    }
                }
                delay(500)
            }
        }
    }

    private val frameTrackingSink = VideoSink { videoFrame: VideoFrame ->
        lastFrameNs = videoFrame.timestampNs
        this@VideoView.onFrame(videoFrame)
    }
}
