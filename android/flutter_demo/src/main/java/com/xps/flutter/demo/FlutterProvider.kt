package com.xps.flutter.demo

import android.content.Context
import io.flutter.embedding.android.FlutterActivity

class FlutterProvider {
    companion object {
        @JvmStatic
        fun provideFlutter(activity: Context) {
            activity.startActivity(FlutterActivity.createDefaultIntent(activity))
        }
    }
}
