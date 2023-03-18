package com.screenmeet.live.feature

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import com.screenmeet.live.R
import com.screenmeet.live.databinding.FragmentUiConfidentialityBinding
import com.screenmeet.live.util.viewBinding
import com.screenmeet.sdk.ScreenMeet
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import java.util.Random

@AndroidEntryPoint
class ConfidentialityFragment : Fragment(R.layout.fragment_ui_confidentiality) {

    private val viewsToObfuscate = ArrayList<View>()

    private val binding by viewBinding(FragmentUiConfidentialityBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.obfuscateNew.setOnClickListener {
            val obfuscatedView = constructConfidentialView(
                binding.root.context,
                viewsToObfuscate.size
            )

            binding.obfuscateContainer.addView(
                obfuscatedView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            viewsToObfuscate.add(obfuscatedView)
            ScreenMeet.setConfidential(obfuscatedView.findViewWithTag("tv"))
        }
        binding.deobfuscate.setOnClickListener {
            viewsToObfuscate.lastOrNull()?.let { obfuscatedView ->
                ScreenMeet.unsetConfidential(obfuscatedView.findViewWithTag("tv"))
                viewsToObfuscate.remove(obfuscatedView)
                binding.obfuscateContainer.removeView(obfuscatedView)
            }
        }

        applyInsets()
    }

    private fun applyInsets() {
        binding.obfuscateContainer.applyInsetter {
            type(statusBars = true, navigationBars = true){
                padding()
            }
        }
        binding.controlContainer.applyInsetter {
            type(navigationBars = true){
                margin()
            }
        }
    }

    private fun constructConfidentialView(context: Context, id: Int): View {
        return TextView(context).apply {
            tag = "tv"
            text = "Secret text $id"
            setTextColor(resources.getColor(R.color.colorAccent, null))
            val maxSize = 25
            val minSize = 11
            textSize = (Random().nextInt(maxSize) + minSize).toFloat()
            measure(0, 0)
            val displaySize = Point()
            context.getSystemService<DisplayManager>()
                ?.getDisplay(Display.DEFAULT_DISPLAY)
                ?.getRealSize(displaySize)
            setOnTouchListener(DragListener(displaySize.x, this))
        }
    }

    private class DragListener(windowWidth: Int, val draggable: View) : View.OnTouchListener {
        var leftMargin = windowWidth * 0.01
        var rightMargin = windowWidth - draggable.measuredWidth - windowWidth * 0.01
        var deltaX = 0f
        var deltaY = 0f

        var mode = DragState.NONE

        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            when (motionEvent.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    deltaX = motionEvent.x
                    deltaY = motionEvent.y
                    mode = DragState.DRAG
                }
                MotionEvent.ACTION_POINTER_DOWN -> mode = DragState.ZOOM
                MotionEvent.ACTION_MOVE -> if (mode == DragState.DRAG) {
                    when {
                        draggable.x + motionEvent.x - deltaX > rightMargin -> {
                            draggable.x = rightMargin.toFloat()
                            draggable.y = draggable.y
                        }
                        draggable.x + motionEvent.x - deltaX < leftMargin -> {
                            draggable.x = leftMargin.toFloat()
                            draggable.y = draggable.y
                        }
                        else -> {
                            draggable.x = draggable.x + motionEvent.x - deltaX
                            draggable.y = draggable.y
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> mode = DragState.NONE
            }
            view.performClick()
            return true
        }

        enum class DragState {
            NONE,
            DRAG,
            ZOOM
        }
    }
}
