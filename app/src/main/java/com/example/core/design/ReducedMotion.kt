package com.example.core.design

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

val LocalReducedMotion = compositionLocalOf { false }

object ReducedMotionPolicy {

    var testReducedMotionOverride: Boolean? = null

    fun isReducedMotionEnabled(context: Context): Boolean {
        testReducedMotionOverride?.let { return it }
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            val transitionScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            scale == 0f || transitionScale == 0f
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    var isReduced by remember {
        mutableStateOf(ReducedMotionPolicy.isReducedMotionEnabled(context))
    }

    DisposableEffect(context) {
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                isReduced = ReducedMotionPolicy.isReducedMotionEnabled(context)
            }
        }
        val uri1 = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        val uri2 = Settings.Global.getUriFor(Settings.Global.TRANSITION_ANIMATION_SCALE)
        try {
            context.contentResolver.registerContentObserver(uri1, false, observer)
            context.contentResolver.registerContentObserver(uri2, false, observer)
        } catch (_: Exception) {}

        onDispose {
            try {
                context.contentResolver.unregisterContentObserver(observer)
            } catch (_: Exception) {}
        }
    }

    return isReduced
}
