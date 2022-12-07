package com.screenmeet.live

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.screenmeet.live.service.ForegroundServiceConnection
import com.screenmeet.sdk.ScreenMeet
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SupportApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // TODO Provide your API token below
        val configuration = ScreenMeet.Configuration(BuildConfig.SM_API_KEY)
        configuration.logLevel(ScreenMeet.Configuration.LogLevel.DEBUG)
        ScreenMeet.init(this, configuration)
        registerActivityLifecycleCallbacks(ScreenMeet.activityLifecycleCallback())
    }

    companion object {

        @JvmField
        var instance: SupportApplication? = null
        private val serviceConnection = ForegroundServiceConnection()

        private val observer: DefaultLifecycleObserver = object : DefaultLifecycleObserver {

            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                instance?.let { serviceConnection.bind(it) }
            }

            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                instance?.let { serviceConnection.unbind(it) }
            }
        }

        @JvmStatic
        fun startListeningForeground() {
            ProcessLifecycleOwner
                .get().lifecycle
                .addObserver(observer)
        }

        @JvmStatic
        fun stopListeningForeground() {
            ProcessLifecycleOwner
                .get().lifecycle
                .removeObserver(observer)
        }
    }
}
