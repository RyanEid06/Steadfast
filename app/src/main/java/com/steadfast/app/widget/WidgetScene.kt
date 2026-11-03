package com.steadfast.app.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import kotlin.math.ln

/** Static, bounded art: only the current streak brightens it; a restart immediately returns to dim. */
internal fun renderWidgetScene(currentDays: Long?): Bitmap {
    val light = (ln(1.0 + (currentDays ?: 0).coerceIn(0, 365)) / ln(366.0)).toFloat()
    val bitmap = Bitmap.createBitmap(600, 360, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    canvas.clipPath(Path().apply { addRoundRect(0f, 0f, 600f, 360f, 40f, 40f, Path.Direction.CW) })
    paint.shader = LinearGradient(0f, 0f, 0f, 360f, Color.rgb(12, 22, 39), Color.rgb(31, 47, 65), Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, 600f, 360f, paint)
    paint.shader = RadialGradient(445f, 143f, 230f,
        Color.argb((20 + 100 * light).toInt(), 238, 190, 96), Color.TRANSPARENT, Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, 600f, 360f, paint)
    paint.shader = null
    paint.color = Color.rgb(21, 34, 49)
    canvas.drawPath(Path().apply {
        moveTo(0f, 245f); lineTo(90f, 190f); lineTo(190f, 256f); lineTo(310f, 185f)
        lineTo(415f, 251f); lineTo(510f, 211f); lineTo(600f, 250f)
        lineTo(600f, 360f); lineTo(0f, 360f); close()
    }, paint)
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeWidth = 28f
    paint.color = Color.argb((15 + 50 * light).toInt(), 242, 194, 101)
    canvas.drawLine(445f, 67f, 445f, 243f, paint)
    canvas.drawLine(397f, 118f, 493f, 118f, paint)
    paint.strokeWidth = 8f
    paint.color = ColorUtils.blendARGB(Color.rgb(112, 101, 77), Color.rgb(250, 223, 164), light)
    canvas.drawLine(445f, 67f, 445f, 243f, paint)
    canvas.drawLine(397f, 118f, 493f, 118f, paint)
    paint.color = Color.rgb(12, 23, 37)
    canvas.drawPath(Path().apply {
        moveTo(0f, 295f); cubicTo(170f, 267f, 330f, 304f, 445f, 240f)
        cubicTo(510f, 236f, 565f, 274f, 600f, 267f)
        lineTo(600f, 360f); lineTo(0f, 360f); close()
    }, paint)
    // Keep the text field dark even at maximum glow and at the minimum widget size.
    paint.shader = LinearGradient(0f, 0f, 0f, 360f,
        intArrayOf(Color.TRANSPARENT, Color.argb(210, 10, 20, 34), Color.argb(230, 10, 20, 34)),
        floatArrayOf(0f, .33f, 1f), Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, 600f, 360f, paint)
    return bitmap
}
