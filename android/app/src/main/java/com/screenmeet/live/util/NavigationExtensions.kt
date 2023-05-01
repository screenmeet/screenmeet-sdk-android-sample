package com.screenmeet.live.util

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.fragment.findNavController

const val NAVIGATION_DESTINATION = "destination"

fun <T> Fragment.getNavigationResult(
    @IdRes id: Int,
    key: String,
    onAbsent: (() -> Unit)? = null,
    onResult: (result: T) -> Unit
) {
    val navBackStackEntry = tryOrNull { findNavController().getBackStackEntry(id) }
    if (navBackStackEntry == null) {
        onAbsent?.invoke()
        return
    }

    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME &&
            navBackStackEntry.savedStateHandle.contains(key)
        ) {
            val result = navBackStackEntry.savedStateHandle.get<T>(key)
            result?.let(onResult) ?: onAbsent?.invoke()
            navBackStackEntry.savedStateHandle.remove<T>(key)
        }
    }
    navBackStackEntry.getLifecycle().addObserver(observer)

    viewLifecycleOwner.lifecycle.addObserver(
        LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                navBackStackEntry.getLifecycle().removeObserver(observer)
            }
        }
    )
}
