package com.steadfast.app.core.accessibility

import android.content.Context
import com.steadfast.app.core.design.ReducedMotionPolicy

object ReducedMotionHelper {
    var testReducedMotionOverride: Boolean?
        get() = ReducedMotionPolicy.testReducedMotionOverride
        set(value) {
            ReducedMotionPolicy.testReducedMotionOverride = value
        }

    fun isReducedMotionEnabled(context: Context): Boolean {
        return ReducedMotionPolicy.isReducedMotionEnabled(context)
    }
}
