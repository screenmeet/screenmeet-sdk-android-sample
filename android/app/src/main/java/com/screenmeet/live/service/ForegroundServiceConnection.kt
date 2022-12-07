package com.screenmeet.live.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

class ForegroundServiceConnection : ServiceConnection {

    private var bound = false

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        bound = true
    }

    override fun onServiceDisconnected(name: ComponentName) {
        bound = false
    }

    fun bind(context: Context) {
        context.startService(Intent(context, ForegroundService::class.java))
        context.bindService(
            Intent(context, ForegroundService::class.java),
            this,
            Context.BIND_AUTO_CREATE
        )
    }

    fun unbind(context: Context) {
        if (bound) {
            context.unbindService(this)
            bound = false
        }
    }
}
