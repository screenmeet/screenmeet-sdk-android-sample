package com.screenmeet.live.feature.call

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.*

typealias onFramesStuck = (Boolean) -> Unit

class VideoView: SurfaceViewRenderer {

    private val stuckThresholdNs = 5000000000L // 5sec

    private lateinit var egl: EglBase.Context
    private var rendererEvents: RendererCommon.RendererEvents? = null

    private var videoTrack: VideoTrack? = null
    private var lastFrameNs: Long = 0
    private var frameStuck: Boolean = false

    constructor(context: Context?): super(context)

    constructor(context: Context?, attrs: AttributeSet?): super(context, attrs)

    override fun init(
        eglContext: EglBase.Context,
        events: RendererCommon.RendererEvents?
    ){
        val track = videoTrack
        if (track != null){
            clean()
            egl = eglContext
            rendererEvents = events
            render(track)
        } else {
            egl = eglContext
            rendererEvents = events
        }
    }

    fun render(track: VideoTrack?){
        Log.d("VideoTrackRender", "render: $track")
        if (track == null){
            clear()
            return
        }

        val currentTrack = videoTrack
        videoTrack = if (currentTrack != null) {
            currentTrack.removeSink(frameTrackingSink)
            track.addSink(frameTrackingSink)
            track
        } else {
            super.init(egl, rendererEvents)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            track.addSink(frameTrackingSink)
            track
        }
    }

    fun clear(){
        videoTrack?.let {
            it.removeSink(frameTrackingSink)
            videoTrack = null
            clearImage()
            release()
        }
    }

    fun listenFramesStuck(scope: CoroutineScope, stuck: onFramesStuck){
        scope.launch {
            stuck(frameStuck)
            while (true) {
                if (videoTrack != null){
                    if (System.nanoTime() - lastFrameNs > stuckThresholdNs){
                        if (!frameStuck){
                            stuck(true)
                            frameStuck = true
                        }
                    } else {
                        if (frameStuck){
                            stuck(false)
                            frameStuck = false
                        }
                    }
                } else {
                    if (frameStuck){
                        stuck(false)
                        frameStuck = false
                    }
                }
                delay(500)
            }
        }
    }

    private fun clean(){
        videoTrack?.let {
            it.removeSink(frameTrackingSink)
            clearImage()
        }
    }

    private val frameTrackingSink = VideoSink { videoFrame: VideoFrame ->
        lastFrameNs = videoFrame.timestampNs
        this@VideoView.onFrame(videoFrame)
    }
}