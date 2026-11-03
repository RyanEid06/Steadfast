package com.steadfast.app

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.steadfast.app.widget.WidgetContent
import com.steadfast.app.widget.createWidgetViews
import com.steadfast.app.widget.renderWidgetScene
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetSceneTest {
    @Test fun currentStreakBrightensTheSceneAndResetReturnsToDim() {
        val days = listOf(0L, 1L, 7L, 30L, 90L, 365L)
        val scenes = days.map(::renderWidgetScene)
        try {
            val brightness = scenes.map(::averageBrightness)
            brightness.zipWithNext().forEach { (before, after) -> assertTrue("Scene must brighten as the current streak grows", after > before) }
            val reset = renderWidgetScene(0)
            val unavailable = renderWidgetScene(null)
            val capped = renderWidgetScene(Long.MAX_VALUE)
            try {
                assertTrue(reset.sameAs(scenes.first()))
                assertTrue(unavailable.sameAs(reset))
                assertTrue(capped.sameAs(scenes.last()))
                assertTrue("Scene bitmap must remain below the Binder transaction budget", capped.byteCount < 1_000_000)
                assertEquals("Rounded corners must stay transparent", 0, Color.alpha(capped.getPixel(0, 0)))
            } finally {
                reset.recycle(); unavailable.recycle(); capped.recycle()
            }
        } finally { scenes.forEach(Bitmap::recycle) }
    }

    @Test fun brightestSceneKeepsCreamTextReadableOverItsWholeTextArea() {
        val scene = renderWidgetScene(365)
        try {
            val cream = androidx.core.graphics.ColorUtils.calculateLuminance(Color.rgb(246, 244, 237))
            for (y in scene.height / 3 until scene.height - 20 step 8) {
                for (x in 40 until scene.width - 40 step 8) {
                    val background = androidx.core.graphics.ColorUtils.calculateLuminance(scene.getPixel(x, y))
                    assertTrue("Cream text must retain at least 4.5:1 contrast", (cream + .05) / (background + .05) >= 4.5)
                }
            }
        } finally { scene.recycle() }
    }

    @Test fun remoteViewsRenderProgressAndTapOpensExistingApp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val views = createWidgetViews(context, WidgetContent("30 days", "A stronger you. A closer walk.", "Next: One year", 30), intent)
        val root = views.apply(context, FrameLayout(context))
        assertEquals("30 days", root.findViewById<TextView>(R.id.widget_streak_days).text.toString())
        assertEquals("A stronger you. A closer walk.", root.findViewById<TextView>(R.id.widget_focus_label).text.toString())
        assertTrue(root.performClick())
        assertEquals(MainActivity::class.java.name, shadowOf(context.applicationContext as Application).nextStartedActivity.component!!.className)
        val repository = generateSequence(java.io.File(".").absoluteFile) { it.parentFile }
            .first { java.io.File(it, "settings.gradle.kts").isFile }
        val previews = java.io.File(repository, ".build-outputs/v1.1/widget-preview").apply { mkdirs() }
        val previewContext = context.createConfigurationContext(android.content.res.Configuration(context.resources.configuration).apply { densityDpi = 320 })
        for (days in listOf(0L, 30L, 365L)) {
            val preview = createWidgetViews(previewContext, WidgetContent("$days days", "A stronger you. A closer walk.", "", days), intent)
                .apply(previewContext, FrameLayout(previewContext))
            preview.measure(android.view.View.MeasureSpec.makeMeasureSpec(360, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(220, android.view.View.MeasureSpec.EXACTLY))
            preview.layout(0, 0, 360, 220)
            val bitmap = Bitmap.createBitmap(360, 220, Bitmap.Config.ARGB_8888)
            preview.draw(Canvas(bitmap))
            java.io.File(previews, "streak-$days.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    private fun averageBrightness(bitmap: Bitmap): Double {
        var sum = 0.0
        for (y in 0 until bitmap.height step 4) for (x in 0 until bitmap.width step 4) {
            val pixel = bitmap.getPixel(x, y)
            sum += Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)
        }
        return sum
    }
}
