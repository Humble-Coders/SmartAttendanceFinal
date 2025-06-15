package com.humblecoders.smartattendance.utils

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

/**
 * Composable that observes app lifecycle events
 */
@Composable
fun AppLifecycleObserver(
    onAppResumed: () -> Unit,
    onAppPaused: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Timber.d("🔄 App resumed - checking permissions")
                    onAppResumed()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Timber.d("⏸️ App paused")
                    onAppPaused()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}