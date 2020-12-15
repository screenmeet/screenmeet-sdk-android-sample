package com.screenmeet.sdkkotlindemo

import android.app.Application
import android.util.Log
import com.screenmeet.sdk.ScreenMeet

class SupportApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        //TODO Provide your API token below
        val configuration = ScreenMeet.Configuration(null)
        val defaultConfig = "API token" + configuration.apiToken() +
                " httpTimeout" + configuration.httpTimeout() +
                " httpNumRetry" + configuration.httpNumRetry() +
                " socketConnectionNumRetries" + configuration.socketConnectionNumRetries() +
                " socketConnectionTimeout" + configuration.socketConnectionTimeout() +
                " socketReconnectNum" + configuration.socketReconnectNum() +
                " socketReconnectDelay" + configuration.socketReconnectDelay() +
                " webRtcNumRetries" + configuration.webRtcNumRetries() +
                " webRtcTimeout" + configuration.webRtcTimeout() +
                " verboseLogging" + configuration.verboseLogging()
        Log.i(this.javaClass.simpleName, "ScreenMeet SDK defaultConfig: $defaultConfig")

        configuration.httpTimeout(30000)
                .httpNumRetry(5)
                .socketConnectionTimeout(20000)
                .socketConnectionNumRetries(5)
                .socketReconnectDelay(0)
                .socketReconnectNum(-1) //-1 - try to reconnect until session times out
                .webRtcTimeout(60000)
                .webRtcNumRetries(5)
                .verboseLogging(true)
        ScreenMeet.init(this, configuration)
        registerActivityLifecycleCallbacks(ScreenMeet.activityLifecycleCallback())
    }
}