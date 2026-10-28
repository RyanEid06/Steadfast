package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.core.time.StreakDurationCalculator
import com.example.data.datastore.SteadfastPreferences
import com.example.data.db.SteadfastDatabase
import com.example.data.repository.MilestoneRepository
import com.example.data.seed.AssetSeedLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SteadfastWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = SteadfastDatabase.getInstance(context)
                val active = db.streakDao().getActiveSessionSync()
                val seedLoader = AssetSeedLoader(context)
                val preferences = SteadfastPreferences(context)
                val milestoneRepo = MilestoneRepository(seedLoader, preferences)

                val (streakText, focusText, nextMilestoneText) = if (active != null) {
                    val duration = StreakDurationCalculator.calculate(
                        startEpochMillis = active.startEpochMillis,
                        startZoneId = active.startZoneId
                    )
                    val days = duration.totalDays + 1
                    val focus = active.recoveryFocusId.replace("_", " ").replaceFirstChar { it.uppercase() }
                    val nextMilestone = milestoneRepo.getNextMilestone(duration)
                    val milestoneStr = if (nextMilestone != null) {
                        "Next: ${nextMilestone.title}"
                    } else {
                        "All Milestones Achieved"
                    }
                    Triple("Day $days", focus, milestoneStr)
                } else {
                    Triple("Begin Walk", "Steadfast Recovery", "Tap to Start")
                }

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_steadfast).apply {
                        setTextViewText(R.id.widget_streak_days, streakText)
                        setTextViewText(R.id.widget_focus_label, focusText)
                        setTextViewText(R.id.widget_milestone_label, nextMilestoneText)
                        setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
