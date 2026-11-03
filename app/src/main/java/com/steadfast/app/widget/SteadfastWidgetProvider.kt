package com.steadfast.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.widget.RemoteViews
import com.steadfast.app.MainActivity
import com.steadfast.app.R
import com.steadfast.app.core.security.BackupManager
import com.steadfast.app.core.time.StreakDurationCalculator
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.data.repository.MilestoneRepository
import com.steadfast.app.data.seed.AssetSeedLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal data class WidgetContent(val streak: String, val focus: String, val milestone: String, val currentDays: Long? = null)

internal fun createWidgetViews(context: Context, content: WidgetContent, pendingIntent: PendingIntent): RemoteViews =
    RemoteViews(context.packageName, R.layout.widget_steadfast).apply {
        setImageViewBitmap(R.id.widget_scene, renderWidgetScene(content.currentDays))
        setTextViewText(R.id.widget_streak_days, content.streak)
        setTextViewText(R.id.widget_focus_label, content.focus)
        setTextViewText(R.id.widget_milestone_label, content.milestone)
        setOnClickPendingIntent(R.id.widget_root, pendingIntent)
    }

fun requestWidgetRefresh(context: Context) {
    try {
        val appContext = context.applicationContext
        val ids = AppWidgetManager.getInstance(appContext).getAppWidgetIds(ComponentName(appContext, SteadfastWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            appContext.sendBroadcast(Intent(appContext, SteadfastWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            })
        }
    } catch (_: RuntimeException) { /* Optional refresh must not fail a committed mutation; periodic updates remain. */ }
}

internal suspend fun loadWidgetContent(context: Context): WidgetContent {
    if (BackupManager.hasPendingRestore(context)) {
        return WidgetContent("Open Steadfast", "Restore in progress", "Open the app to finish restoring")
    }
    val active = SteadfastDatabase.getInstance(context).streakDao().getActiveSessionSync()
        ?: return WidgetContent("Begin Walk", "Steadfast Recovery", "Tap to Start")
    val duration = StreakDurationCalculator.calculate(active.startEpochMillis, active.startZoneId)
    val next = MilestoneRepository(AssetSeedLoader(context), SteadfastPreferences(context)).getNextMilestone(duration)
    // A restore may begin after the query; never publish its partial state.
    if (BackupManager.hasPendingRestore(context)) {
        return WidgetContent("Open Steadfast", "Restore in progress", "Open the app to finish restoring")
    }
    return WidgetContent(
        "${duration.totalDays} days",
        "A stronger you. A closer walk.",
        next?.let { "Next: ${it.title}" } ?: "All Milestones Achieved",
        duration.totalDays
    )
}

class SteadfastWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        val pendingIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val publish: (WidgetContent) -> Unit = { content ->
                    val views = createWidgetViews(context, content, pendingIntent)
                    for (id in appWidgetIds) {
                        appWidgetManager.updateAppWidget(id, views)
                    }
                }
                try {
                    val published = BackupManager.withStableUserData(context) {
                        publish(loadWidgetContent(context))
                        true
                    }
                    if (published != true) {
                        publish(WidgetContent("Open Steadfast", "Restore in progress", "Open the app to finish restoring"))
                    }
                } catch (_: Exception) {
                    publish(WidgetContent("Open Steadfast", "Progress unavailable", "Open the app to refresh"))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
