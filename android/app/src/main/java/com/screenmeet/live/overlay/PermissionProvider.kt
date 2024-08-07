package com.screenmeet.live.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts

typealias onPermissionResult = (Boolean) -> Unit

object PermissionProvider {

    private const val REQUEST_KEY = "overlay_request"

    fun canDrawOverlay(context: Context) = Settings.canDrawOverlays(context)

    fun requestOverlay(
        context: Context,
        activityResultRegistry: ActivityResultRegistry,
        result: onPermissionResult
    ) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )

        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        activityResultRegistry.register(
            REQUEST_KEY,
            ActivityResultContracts.StartActivityForResult()
        ) {
            result.invoke(canDrawOverlay(context))
        }.launch(intent)
    }
}
