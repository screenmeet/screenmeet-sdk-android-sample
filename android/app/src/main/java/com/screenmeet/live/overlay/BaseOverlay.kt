package com.screenmeet.live.overlay

import android.annotation.SuppressLint
import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets.Type
import android.view.WindowManager
import androidx.annotation.UiThread
import kotlin.math.roundToInt

abstract class BaseOverlay(val context: Context) {

    protected var overlay: View? = null
    protected var screen = ScreenConfig()

    protected var overlayHeight = WindowManager.LayoutParams.WRAP_CONTENT
    protected var overlayWidth = WindowManager.LayoutParams.WRAP_CONTENT

    protected val windowManager by lazy { windowManager() }
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    fun showOverlay(): Boolean {
        removeIfAdded()
        val view = buildOverlay(context)
        applyOverlay(false, view, buildLayoutParams(overlayWidth, overlayHeight))
        loadScreenConfig(view)
        overlay = view
        context.registerComponentCallbacks(callback)
        return true
    }

    open fun hideOverlay() {
        removeIfAdded()
    }

    protected abstract fun buildOverlay(context: Context): View

    protected fun convertDpToPixel(dp: Int, context: Context): Int {
        val resources = context.resources
        val metrics = resources.displayMetrics
        return (metrics.density * dp).roundToInt()
    }

    protected open fun onConfigChanged() {
        Log.d("BaseOverlay", "Device Config changed")
    }

    @UiThread
    private fun applyOverlay(
        attached: Boolean,
        view: View,
        layoutParams: WindowManager.LayoutParams
    ) {
        mainHandler.post {
            if (attached) {
                windowManager.updateViewLayout(view, layoutParams)
            } else {
                windowManager.addView(view, layoutParams)
            }
        }
    }

    private fun buildLayoutParams(viewWidth: Int, viewHeight: Int): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams(
            viewWidth,
            viewHeight,
            getOverlayType(),
            getOverlayFlagsTouch(),
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.START or Gravity.TOP
        return params
    }

    private fun getOverlayFlagsTouch() = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

    @Suppress("Deprecation")
    private fun getOverlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    @UiThread
    private fun removeIfAdded() {
        mainHandler.post {
            if (isViewAttached(overlay)) {
                windowManager.removeView(overlay)
                overlay = null
                context.unregisterComponentCallbacks(callback)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun loadScreenConfig(view: View) {
        screen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val statusBars = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                Type.statusBars()
            ).top
            val navigationBars = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                Type.navigationBars()
            )
            val displayCutout = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                Type.displayCutout()
            )
            ScreenConfig(
                width = windowMetrics.bounds.width(),
                height = windowMetrics.bounds.height(),
                topInset = statusBars,
                bottomInset = navigationBars.bottom + displayCutout.bottom,
                leftInset = navigationBars.left + displayCutout.left,
                rightInset = navigationBars.right + displayCutout.right
            )
        } else {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            val displaySize = Point()
            display.getRealSize(displaySize)

            val windowInsets = view.rootWindowInsets
            if (windowInsets != null) {
                ScreenConfig(
                    width = displaySize.x,
                    height = displaySize.y,
                    topInset = windowInsets.stableInsetTop,
                    bottomInset = windowInsets.stableInsetBottom,
                    leftInset = windowInsets.stableInsetLeft,
                    rightInset = windowInsets.stableInsetRight
                )
            } else {
                // Edge case
                val statusBarHeight = getStatusBarHeightFromRes()
                ScreenConfig(
                    width = displaySize.x,
                    height = displaySize.y,
                    topInset = statusBarHeight,
                    bottomInset = statusBarHeight
                )
            }
        }
    }

    @SuppressLint("InternalInsetResource")
    private fun getStatusBarHeightFromRes(): Int {
        val hasMenuKey = ViewConfiguration.get(context).hasPermanentMenuKey()
        val resourceId: Int = context.resources.getIdentifier(
            "status_bar_height",
            "dimen",
            "android"
        )
        return if (resourceId > 0 && !hasMenuKey) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    private fun isViewAttached(view: View?): Boolean {
        return view != null && view.parent != null
    }

    private fun windowManager(): WindowManager {
        return context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val callback = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            overlay?.let {
                loadScreenConfig(it)
                onConfigChanged()
            }
        }

        override fun onLowMemory() {}
    }
}
