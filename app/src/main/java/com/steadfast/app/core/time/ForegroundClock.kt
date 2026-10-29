package com.steadfast.app.core.time

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Foreground UI clock; callers keep Room data as independent Flow state. */
@Composable
fun rememberForegroundTimeMillis(enabled: Boolean = true): Long {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now by produceState(System.currentTimeMillis(), lifecycle, enabled) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            do {
                value = System.currentTimeMillis()
                if (!enabled) break
                delay(1_000L)
            } while (isActive)
        }
    }
    return now
}
