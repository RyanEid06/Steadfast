package com.steadfast.app

import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetLayoutTest {
    @Test fun minimumWidgetKeepsProgressReadableAtTwiceFontScale() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(base.resources.configuration).apply { fontScale = 2f }
        val context = base.createConfigurationContext(config)
        val view = LayoutInflater.from(context).inflate(R.layout.widget_steadfast, null)
        view.findViewById<TextView>(R.id.widget_streak_days).text = "12345 days"
        view.findViewById<TextView>(R.id.widget_focus_label).text = "A stronger you. A closer walk."
        view.findViewById<TextView>(R.id.widget_milestone_label).text = "Next: One year of steady progress"
        val width = (180 * context.resources.displayMetrics.density).toInt()
        val height = (110 * context.resources.displayMetrics.density).toInt()
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
        for (id in listOf(R.id.widget_streak_days, R.id.widget_focus_label)) {
            val child = view.findViewById<TextView>(id)
            println("Widget font2 line=$id bounds=${child.top}..${child.bottom} height=${child.height} textHeight=${child.layout?.height}")
            assertTrue("Required widget line extends outside 180x110dp at font2: ${child.top}..${child.bottom} / $height", child.height > 0 && child.top >= 0 && child.bottom <= height)
            assertTrue("Widget line was vertically clipped", child.layout != null && child.layout.height <= child.height - child.compoundPaddingTop - child.compoundPaddingBottom)
        }
        val streak = view.findViewById<TextView>(R.id.widget_streak_days)
        assertTrue("Current streak must fit at the minimum tested width", streak.layout.getEllipsisCount(0) == 0)
    }
}
