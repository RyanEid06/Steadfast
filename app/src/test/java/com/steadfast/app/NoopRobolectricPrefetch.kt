package com.steadfast.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.PrefetchRequest
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

// Compose's later scheduler also disables prefetch on Robolectric because its frame start time is wrong.
// https://android.googlesource.com/platform/frameworks/support/+/88e1988b6bf1969184b60bb87c92237a13fdf0b5/compose/foundation/foundation/src/androidMain/kotlin/androidx/compose/foundation/lazy/layout/PrefetchScheduler.android.kt
@OptIn(ExperimentalFoundationApi::class)
@Implements(className = "androidx.compose.foundation.lazy.layout.AndroidPrefetchScheduler", isInAndroidSdk = false)
class NoopRobolectricPrefetch {
    @Implementation fun schedulePrefetch(request: PrefetchRequest) = Unit
}
