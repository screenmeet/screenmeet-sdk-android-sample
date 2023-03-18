package com.screenmeet.live.service

import android.app.*
import android.content.Intent
import android.content.res.Configuration
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_HIGH
import com.screenmeet.live.MainActivity
import com.screenmeet.live.R

class ForegroundService : Service() {

    private val binder: IBinder = LocalBinder()

    private var serviceHandler: Handler? = null
    private var configurationChanged = false

    override fun onCreate() {
        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        serviceHandler = Handler(handlerThread.looper)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.app_name)
            val channel = NotificationChannel(
                ANDROID_CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false)
        if (startedFromNotification) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChanged = true
    }

    @Suppress("DEPRECATION")
    override fun onBind(intent: Intent): IBinder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else stopForeground(true)
        configurationChanged = false
        return binder
    }

    @Suppress("DEPRECATION")
    override fun onRebind(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else stopForeground(true)
        configurationChanged = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        if (!configurationChanged) {
            startForeground(NOTIFICATION_ID, notification)
        }
        return true
    }

    override fun onDestroy() {
        serviceHandler!!.removeCallbacksAndMessages(null)
    }

    private val notification: Notification
        get() {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)
            val activityPendingIntent = PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
            val resources = applicationContext.resources
            val builder = NotificationCompat.Builder(this, ANDROID_CHANNEL_ID)
                .setContentIntent(activityPendingIntent)
                .setContentText(resources.getString(R.string.foreground_title))
                .setContentTitle(resources.getString(R.string.app_name))
                .setOngoing(true)
                .setPriority(IMPORTANCE_HIGH)
                .setSmallIcon(R.drawable.ic_support_agent)
                .setWhen(System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(ANDROID_CHANNEL_ID)
            }
            return builder.build()
        }

    @Suppress("unused")
    inner class LocalBinder : Binder() {
        val service: ForegroundService
            get() = this@ForegroundService
    }

    companion object {
        private const val ANDROID_CHANNEL_ID = "Live Main"
        private const val NOTIFICATION_ID = 101
        private val TAG = ForegroundService::class.java.simpleName
        private const val EXTRA_STARTED_FROM_NOTIFICATION = "started_from_notification"
    }
}
