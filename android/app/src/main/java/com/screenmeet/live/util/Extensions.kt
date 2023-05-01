package com.screenmeet.live.util

fun <T : Any> tryOrNull(body: () -> T?): T? {
    return try {
        body()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
