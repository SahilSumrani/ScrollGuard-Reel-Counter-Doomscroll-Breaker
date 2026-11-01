package com.example.braindrain

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb

class BrainWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Iterate through all widgets
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, 0)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            count: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            
            // Get Brain State
            val brainState = BrainState.fromScrollCount(count)
            val colorInt = brainState.primaryColor.toArgb()

            // Update Views
            views.setTextViewText(R.id.widget_emoji, brainState.pillEmoji)
            views.setTextViewText(R.id.widget_state_text, brainState.pillLabel.uppercase())
            views.setTextColor(R.id.widget_state_text, colorInt)
            views.setTextViewText(R.id.widget_count_text, "$count Reels Today")
            
            // Intent to launch app on click
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        fun updateWidget(context: Context, count: Int) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, BrainWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, count)
            }
        }
    }
}
