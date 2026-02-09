package com.wakechallenge.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.wakechallenge.MainActivity
import com.wakechallenge.R
import com.wakechallenge.data.database.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AlarmWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_ALARM = "com.wakechallenge.widget.TOGGLE_ALARM"
        const val EXTRA_ALARM_ID = "alarm_id"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, AlarmWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, AlarmWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_TOGGLE_ALARM -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
                if (alarmId != -1L) {
                    toggleAlarm(context, alarmId)
                }
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        scope.launch {
            val database = AlarmDatabase.getInstance(context)
            val alarms = database.alarmDao().getEnabledAlarms().first()

            val views = RemoteViews(context.packageName, R.layout.widget_alarm)

            // Open app when clicking on widget
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, openAppPendingIntent)

            if (alarms.isEmpty()) {
                views.setTextViewText(R.id.widget_time, "--:--")
                views.setTextViewText(R.id.widget_label, "No alarms set")
                views.setTextViewText(R.id.widget_next_alarm, "Tap to add alarm")
            } else {
                // Find next alarm
                val now = LocalDateTime.now()
                val nextAlarm = alarms.minByOrNull { alarm ->
                    calculateTimeUntilAlarm(
                        LocalTime.of(alarm.hour, alarm.minute),
                        alarm.repeatDays,
                        now
                    )
                }

                if (nextAlarm != null) {
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                    val alarmTime = LocalTime.of(nextAlarm.hour, nextAlarm.minute)

                    views.setTextViewText(R.id.widget_time, alarmTime.format(timeFormatter))
                    views.setTextViewText(
                        R.id.widget_label,
                        nextAlarm.label.ifBlank { "Alarm" }
                    )

                    val timeUntil = calculateTimeUntilAlarm(alarmTime, nextAlarm.repeatDays, now)
                    val hours = timeUntil / 60
                    val minutes = timeUntil % 60

                    val timeUntilText = when {
                        hours > 0 -> "In ${hours}h ${minutes}m"
                        minutes > 0 -> "In ${minutes}m"
                        else -> "Now"
                    }
                    views.setTextViewText(R.id.widget_next_alarm, timeUntilText)

                    // Toggle button
                    val toggleIntent = Intent(context, AlarmWidgetProvider::class.java).apply {
                        action = ACTION_TOGGLE_ALARM
                        putExtra(EXTRA_ALARM_ID, nextAlarm.id)
                    }
                    val togglePendingIntent = PendingIntent.getBroadcast(
                        context,
                        nextAlarm.id.toInt(),
                        toggleIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_toggle_button, togglePendingIntent)
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun toggleAlarm(context: Context, alarmId: Long) {
        scope.launch {
            val database = AlarmDatabase.getInstance(context)
            val alarm = database.alarmDao().getAlarmById(alarmId)
            if (alarm != null) {
                database.alarmDao().updateAlarmEnabled(alarmId, !alarm.isEnabled)
                updateAllWidgets(context)
            }
        }
    }

    private fun calculateTimeUntilAlarm(
        alarmTime: LocalTime,
        repeatDays: String,
        now: LocalDateTime
    ): Long {
        val today = now.toLocalDate()
        val currentTime = now.toLocalTime()

        // For one-time alarms
        if (repeatDays.isBlank()) {
            val todayAlarm = today.atTime(alarmTime)
            return if (alarmTime.isAfter(currentTime)) {
                java.time.Duration.between(now, todayAlarm).toMinutes()
            } else {
                java.time.Duration.between(now, todayAlarm.plusDays(1)).toMinutes()
            }
        }

        // For repeating alarms
        val days = repeatDays.split(",").map { java.time.DayOfWeek.valueOf(it) }.toSet()
        val currentDayOfWeek = today.dayOfWeek

        // Check if alarm should ring today
        if (days.contains(currentDayOfWeek) && alarmTime.isAfter(currentTime)) {
            return java.time.Duration.between(now, today.atTime(alarmTime)).toMinutes()
        }

        // Find next day
        for (daysToAdd in 1..7) {
            val futureDate = today.plusDays(daysToAdd.toLong())
            if (days.contains(futureDate.dayOfWeek)) {
                return java.time.Duration.between(now, futureDate.atTime(alarmTime)).toMinutes()
            }
        }

        return Long.MAX_VALUE
    }
}
