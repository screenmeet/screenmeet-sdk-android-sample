package com.screenmeet.live

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.screenmeet.live.overlay.WidgetManager
import com.screenmeet.live.service.ForegroundServiceConnection
import com.screenmeet.live.tools.DataStoreManager
import com.screenmeet.sdk.ScreenMeet
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class SupportApplication : Application() {

    @Inject
    lateinit var dataStore: DataStoreManager

    override fun onCreate() {
        super.onCreate()
        instance = this
        Firebase.crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
        val (endpoint, tag, apiKey) = runBlocking { dataStore.getConnectionPrefs() }
        // TODO Provide your API token below
        val configuration = ScreenMeet.Configuration(apiKey ?: BuildConfig.SM_API_KEY)
        configuration.urlEndpoint(endpoint)
        configuration.serverTag(tag)
        ScreenMeet.init(this, configuration)
    }

    @Suppress("ktlint:standard:property-naming")
    companion object {

        @JvmField
        var instance: SupportApplication? = null

        var inBackground = false

        val widgetManager by lazy { WidgetManager(instance!!) }

        private val serviceConnection = ForegroundServiceConnection()

        private val observer = object : DefaultLifecycleObserver {

            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                instance?.let { serviceConnection.bind(it) }
            }

            override fun onPause(owner: LifecycleOwner) {
                super.onPause(owner)
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
            instance?.let { serviceConnection.bind(it) }
            ProcessLifecycleOwner
                .get().lifecycle
                .removeObserver(observer)
        }
    }
}
