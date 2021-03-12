package com.screenmeet.sdkkotlindemo

import android.app.Activity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.screenmeet.sdk.ScreenMeet
import java.util.*

class ConfidentialityActivity : AppCompatActivity() {
    private val viewToObfuscate = ArrayList<View>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ui_confidentiality)

        findViewById<View>(R.id.obfuscateNew).setOnClickListener {
            val obfuscatedView = constructConfidentialView(this@ConfidentialityActivity, viewToObfuscate.size)
            (findViewById<View>(R.id.obfuscateContainer) as ViewGroup).addView(obfuscatedView,
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            viewToObfuscate.add(obfuscatedView)
            ScreenMeet.appStreamVideoSource().setConfidential(obfuscatedView.findViewWithTag("tv"))
        }

        findViewById<View>(R.id.deobfuscateNew).setOnClickListener {
            if (viewToObfuscate.isNotEmpty()) {
                val view = viewToObfuscate[viewToObfuscate.size - 1]
                ScreenMeet.appStreamVideoSource().unsetConfidential(view.findViewWithTag("tv"))
                viewToObfuscate.remove(view)
                (findViewById<View>(R.id.obfuscateContainer) as ViewGroup).removeView(view)
            }
        }
    }

    companion object {
        fun constructConfidentialView(context: Activity, id: Int): View {
            val displayMetrics = DisplayMetrics()
            context.windowManager.defaultDisplay.getMetrics(displayMetrics)
            val width = displayMetrics.widthPixels
            val view = TextView(context)

            view.tag = "tv"
            val text = "Secret text $id"
            view.text = text
            val textSize = Random().nextInt(25) + 11
            view.textSize = textSize.toFloat()
            val measuredSize = textSize * (view.text.length + 5)
            val spaceLeft = Space(context)
            val spaceRight = Space(context)

            val container = LinearLayout(context)
            container.addView(spaceLeft, ViewGroup.LayoutParams(width - measuredSize, ViewGroup.LayoutParams.MATCH_PARENT))
            container.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            container.addView(spaceRight, ViewGroup.LayoutParams(width - measuredSize, ViewGroup.LayoutParams.MATCH_PARENT))

            val scrollView = HorizontalScrollView(context)
            scrollView.isHorizontalScrollBarEnabled = false
            scrollView.addView(container, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            return scrollView
        }
    }
}