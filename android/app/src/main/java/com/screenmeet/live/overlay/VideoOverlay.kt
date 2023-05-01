package com.screenmeet.live.overlay

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.screenmeet.live.MainActivity
import com.screenmeet.live.R
import com.screenmeet.live.SupportApplication
import com.screenmeet.live.databinding.OverlayWidgetBinding
import com.screenmeet.live.util.DoubleTapListener
import com.screenmeet.sdk.ScreenMeet.Companion.eglContext
import com.screenmeet.sdk.VideoElement
import org.webrtc.RendererCommon
import kotlin.math.abs
import kotlin.math.min

class VideoOverlay(context: Context) : BaseOverlay(context) {

    private val widgetCornerMargin = convertDpToPixel(20, context)
    private val widgetInnerPadding = convertDpToPixel(5, context)
    private val widgetMaxSize = convertDpToPixel(200, context)

    private var inBounded = true
    private var initialXCoordinate = 0
    private var startX = 0
    private var initialYCoordinate = 0
    private var startY = 0

    private var binding: OverlayWidgetBinding? = null

    override fun buildOverlay(context: Context): View {
        val ctx = ContextThemeWrapper(context, R.style.AppTheme)
        val inflater = LayoutInflater.from(ctx)
        binding = OverlayWidgetBinding.inflate(inflater)
        binding?.root?.isVisible = false

        overlayWidth = WindowManager.LayoutParams.WRAP_CONTENT
        overlayHeight = WindowManager.LayoutParams.WRAP_CONTENT

        binding?.renderer?.apply {
            isClickable = false
            isFocusable = false
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            setZOrderMediaOverlay(false)

            val eventsListener = object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() {}

                override fun onFrameResolutionChanged(width: Int, height: Int, rotation: Int) {
                    val screenRatio = if (screen.height > screen.width) {
                        if (width > height) {
                            2f
                        } else {
                            3f
                        }
                    } else {
                        if (width > height) {
                            3f
                        } else {
                            2f
                        }
                    }

                    val downScaleBy = min(
                        screen.width / (screenRatio * width),
                        screen.height / (screenRatio * height)
                    )

                    val (widgetWidth, widgetHeight) = Pair(
                        width * downScaleBy,
                        height * downScaleBy
                    )

                    val overlay = this@VideoOverlay.overlay
                    overlay?.post {
                        val layoutParams = overlay.layoutParams
                        if (layoutParams != null) {
                            layoutParams.width = widgetWidth.toInt()
                            layoutParams.height = widgetHeight.toInt()
                            windowManager.updateViewLayout(overlay, layoutParams)
                            binding?.root?.isVisible = true
                        }
                        touchMoveEvent(widgetCornerMargin, widgetCornerMargin)
                        touchUpEvent(widgetCornerMargin, widgetCornerMargin)
                    }
                }
            }
            init(eglContext!!, eventsListener)
        }

        return binding!!.root.apply {
            setPadding(widgetInnerPadding)
            val doubleTapListener = DoubleTapListener(context) {
                if (!SupportApplication.inBackground) return@DoubleTapListener
                val contextNew = this.context
                val intent = Intent(contextNew, MainActivity::class.java)
                intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP)
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                contextNew.startActivity(intent)
            }
            setOnTouchListener { v, event ->
                doubleTapListener.onTouch(v, event)
                v.performClick()
                processTouch(event)
                return@setOnTouchListener true
            }
        }
    }

    fun attachVideoTrack(videoElement: VideoElement) {
        binding?.apply {
            val trackId = renderer.trackId
            if (trackId != null && trackId == videoElement.track?.id()) {
                return
            }

            val layoutParams = root.layoutParams
            if (layoutParams != null) {
                layoutParams.width = widgetMaxSize
                layoutParams.height = widgetMaxSize
                windowManager.updateViewLayout(overlay, layoutParams)
            }

            nameTv.text = videoElement.userName
            if (videoElement.isAudioSharing) {
                microButton.setImageResource(R.drawable.mic)
                microButton.backgroundTintList = null
                microButton.colorFilter = null
            } else {
                val context = root.context
                microButton.setImageResource(R.drawable.mic_off)
                val color = ContextCompat.getColor(context, R.color.bright_red)
                microButton.setColorFilter(color)
            }

            val videoTrack = videoElement.track
            val hasTrack = videoTrack != null
            renderer.render(videoTrack)
            renderer.isVisible = hasTrack
            logo.isVisible = !hasTrack
        }
    }

    override fun hideOverlay() {
        super.hideOverlay()
        binding?.renderer?.clear()
    }

    override fun onConfigChanged() {
        overlay?.let {
            applyDiff(it, 0, 0, true)
        }
    }

    private fun processTouch(event: MotionEvent) {
        val x = event.rawX.toInt()
        val y = event.rawY.toInt()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchDownEvent(x, y)
            MotionEvent.ACTION_MOVE -> touchMoveEvent(x, y)
            MotionEvent.ACTION_UP -> touchUpEvent(x, y)
        }
    }

    private fun touchDownEvent(x: Int, y: Int) {
        startX = x
        startY = y
        initialXCoordinate = x
        initialYCoordinate = y
    }

    private fun touchMoveEvent(x: Int, y: Int) {
        overlay?.let {
            inBounded = false

            val yDiffMove = y - initialYCoordinate
            val xDiffMove = x - initialXCoordinate
            initialYCoordinate = y
            initialXCoordinate = x
            applyDiff(it, xDiffMove, yDiffMove)
        }
    }

    private fun touchUpEvent(x: Int, y: Int): Boolean {
        val vicinity = 48
        overlay?.let {
            if (inBounded) {
                inBounded = false
                return false
            }
            inBounded = false

            val yDiff = y - initialYCoordinate
            val xDiff = x - initialXCoordinate
            applyDiff(it, xDiff, yDiff, true)
        }

        return abs(y - startY) < vicinity || abs(x - startX) < vicinity
    }

    private fun applyDiff(view: View, xDiff: Int, yDiff: Int, stickToCorner: Boolean = false) {
        val layoutParamsWidget = view.layoutParams as WindowManager.LayoutParams

        val widgetX = layoutParamsWidget.x
        val widgetY = layoutParamsWidget.y
        val widgetHeight = view.measuredHeight
        val widgetWidth = view.measuredWidth
        val screenHeight = screen.height
        val screenWidth = screen.width

        val widgetMaxY = screenHeight - widgetHeight - screen.topInset - screen.bottomInset
        val widgetMaxX = screenWidth - widgetWidth - screen.leftInset - screen.rightInset

        var widgetNewY = widgetY + yDiff
        var widgetNewX = widgetX + xDiff

        if (stickToCorner) {
            widgetNewX = if (widgetNewX > widgetMaxX / 2) {
                widgetMaxX - widgetCornerMargin
            } else {
                0 + widgetCornerMargin
            }

            widgetNewY = if (widgetNewY > widgetMaxY / 2) {
                widgetMaxY - widgetCornerMargin
            } else {
                0 + widgetCornerMargin
            }
        } else {
            if (widgetNewY < 0) {
                widgetNewY = 0
            } else if (widgetNewY > widgetMaxY) {
                widgetNewY = widgetMaxY
            }
            if (widgetNewX < 0) {
                widgetNewX = 0
            } else if (widgetNewX > widgetMaxX) {
                widgetNewX = widgetMaxX
            }
        }

        layoutParamsWidget.x = widgetNewX
        layoutParamsWidget.y = widgetNewY
        windowManager.updateViewLayout(view, layoutParamsWidget)
    }
}
