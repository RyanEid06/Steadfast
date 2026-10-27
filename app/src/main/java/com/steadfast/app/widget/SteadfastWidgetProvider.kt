package com.steadfast.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.steadfast.app.MainActivity
import com.steadfast.app.R
import com.steadfast.app.core.time.StreakDurationCalculator
import com.steadfast.app.data.db.SteadfastDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SteadfastWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        CoroutineScope(Dispatchers.IO).launch {
            val db = SteadfastDatabase.getInstance(context)
            val active = db.streakDao().getActiveSessionSync()
            val streakDays = if (active != null) {
                val duration = StreakDurationCalculator.calculate(
                    startEpochMillis = active.startEpochMillis,
                    startZoneId = active.startZoneId
                )
                duration.totalDays + 1
            } else {
                1
            }

            val focusText = active?.recoveryFocusId?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Recovery"

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_steadfast).apply {
                    setTextViewText(R.id.widget_streak_days, "Day $streakDays")
                    setTextViewText(R.id.widget_focus_label, focusText)
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
