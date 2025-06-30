package com.myorangefit.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.myorangefit.app.R
import com.myorangefit.app.activity.MainActivity
import com.myorangefit.app.database.DatabaseHelper
import com.myorangefit.app.database.DatabaseHelperSingleton
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

/**
 * Implementation of App Widget functionality.
 */
private lateinit var databaseHelper: DatabaseHelper

class OrangeFitWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        databaseHelper = DatabaseHelperSingleton.getInstance(context)
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
        databaseHelper = DatabaseHelperSingleton.getInstance(context)
        scheduleMidnightUpdate(context)
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        cancelMidnightUpdate(context)
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    databaseHelper = DatabaseHelperSingleton.getInstance(context)
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.widget_orangefit)

    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    views.setOnClickPendingIntent(R.id.root, pendingIntent)

    // Ottieni la mappa {data: List<BodyPart>} per la settimana corrente
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    val dayKeys = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
    val bodyParts = listOf("chest", "back", "arms", "abs", "shoulders", "legs")

    val map = databaseHelper.getBodyPartsPerDayOfWeekDetailed(today)

    views.setTextViewText(R.id.frequency, map.size.toString())

    for (i in 0..6) {
        val dayKey = dayKeys[i]
        val dayViewId = context.resources.getIdentifier(dayKey, "id", context.packageName)

        var j = i + 1
        if (j == 7) j = 0
        val trainedParts = map[(j).toString()] ?: emptyList()
        Log.e("ss", trainedParts.toString())

        // Se ci sono allenamenti in questo giorno
        if (trainedParts.isNotEmpty()) {
            // Colora sfondo giorno (modifica con il tuo colore voluto)
            views.setInt(dayViewId, "setBackgroundResource", R.drawable.day_oval_bg_primary)
            views.setTextColor(dayViewId, ContextCompat.getColor(context, R.color.white))
        } else {
            // Sfondo normale
            views.setInt(dayViewId, "setBackgroundResource", R.drawable.day_oval_bg)
            views.setTextColor(dayViewId, ContextCompat.getColor(context, R.color.gray))
        }

        // Gestione dei dot (tutti nascosti, poi quelli allenati visibili)
        for (bp in bodyParts) {
            val dotId = context.resources.getIdentifier("dot_${bp}_$dayKey", "id", context.packageName)
            Log.e("ss", "dot_${bp}_$dayKey")
            if (trainedParts.map { it.lowercase() }.contains(bp)) {
                views.setViewVisibility(dotId, android.view.View.VISIBLE)
            } else {
                views.setViewVisibility(dotId, android.view.View.GONE)
            }
        }
    }

    val dayOfWeekFull = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).lowercase()
    val todayId = context.resources.getIdentifier(dayOfWeekFull, "id", context.packageName)
    views.setInt(todayId, "setBackgroundResource", R.drawable.day_oval_bg_today)
    views.setTextColor(todayId, ContextCompat.getColor(context, R.color.white))

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

/*
* setta un allarme che quando scatta fa partire il reciver che aggiorna il widget
 */
fun scheduleMidnightUpdate(context: Context) {
    val intent = Intent(context, MidnightReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 5) // 5 secondi dopo mezzanotte per sicurezza
        set(Calendar.MILLISECOND, 0)
        if (before(Calendar.getInstance())) {
            add(Calendar.DATE, 1)
        }
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    alarmManager.setInexactRepeating(
        android.app.AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        android.app.AlarmManager.INTERVAL_DAY,
        pendingIntent
    )
}

fun cancelMidnightUpdate(context: Context) {
    val intent = Intent(context, MidnightReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    alarmManager.cancel(pendingIntent)
}
