package com.example.myorangefit.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Aggiorna subito il widget!
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, OrangeFitWidget::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        for (id in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id)
        }
    }
}
