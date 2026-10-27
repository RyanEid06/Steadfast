package com.steadfast.app.core.accessibility

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object ReducedMotionHelper {
    /**
     * Test hook to verify reduced motion behavior in unit / Robolectric tests.
     */
    var testReducedMotionOverride: Boolean? = null

    fun isReducedMotionEnabled(context: Context): Boolean {
        testReducedMotionOverride?.let { return it }
        return try {
            val resolver = context.contentResolver
            val durationScale = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
            val transitionScale = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1.0f)
            durationScale == 0f || transitionScale == 0f
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) { ReducedMotionHelper.isReducedMotionEnabled(context) }
}
