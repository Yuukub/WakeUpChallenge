package com.wakechallenge.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.wakechallenge.domain.model.Alarm
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: Alarm) {
        val nextAlarmTime = calculateNextAlarmTime(alarm)
        val triggerTimeMillis = nextAlarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // For smart alarms, schedule sleep tracking to start before the alarm window
        if (alarm.isSmartAlarm) {
            scheduleSmartAlarmTracking(alarm, triggerTimeMillis)
        } else {
            // Regular alarm scheduling
            scheduleRegularAlarm(alarm, triggerTimeMillis)
        }
    }

    private fun scheduleRegularAlarm(alarm: Alarm, triggerTimeMillis: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            action = AlarmReceiver.ACTION_ALARM_TRIGGERED
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
                    pendingIntent
                )
            }
        } else {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
                pendingIntent
            )
        }
    }

    private fun scheduleSmartAlarmTracking(alarm: Alarm, alarmTimeMillis: Long) {
        // Start sleep tracking at the beginning of the smart alarm window
        val windowStartMillis = alarmTimeMillis - (alarm.smartAlarmWindowMinutes * 60 * 1000)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_IS_SMART_ALARM, true)
            putExtra(AlarmReceiver.EXTRA_ALARM_TIME_MILLIS, alarmTimeMillis)
            putExtra(AlarmReceiver.EXTRA_SMART_WINDOW_MINUTES, alarm.smartAlarmWindowMinutes)
            action = AlarmReceiver.ACTION_START_SMART_TRACKING
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (alarm.id + 20000).toInt(), // Different request code for smart alarm tracking
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule the sleep tracking to start
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    windowStartMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                windowStartMillis,
                pendingIntent
            )
        }

        // Also schedule a backup regular alarm at the target time
        // in case sleep tracking doesn't trigger earlier
        scheduleRegularAlarm(alarm, alarmTimeMillis)
    }

    fun cancelAlarm(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGERED
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    fun scheduleSnooze(alarm: Alarm) {
        val snoozeTime = LocalDateTime.now().plusMinutes(alarm.snoozeDurationMinutes.toLong())
        val triggerTimeMillis = snoozeTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_IS_SNOOZE, true)
            action = AlarmReceiver.ACTION_ALARM_TRIGGERED
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (alarm.id + 10000).toInt(), // Different request code for snooze
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
                    pendingIntent
                )
            }
        } else {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
                pendingIntent
            )
        }
    }

    private fun calculateNextAlarmTime(alarm: Alarm): LocalDateTime {
        val now = LocalDateTime.now()
        val alarmTime = alarm.time

        // For one-time alarms
        if (alarm.isOneTime) {
            val todayAlarm = now.toLocalDate().atTime(alarmTime)
            return if (todayAlarm.isAfter(now)) {
                todayAlarm
            } else {
                todayAlarm.plusDays(1)
            }
        }

        // For repeating alarms
        val today = now.toLocalDate()
        val currentDayOfWeek = today.dayOfWeek

        // Check if alarm should ring today
        if (alarm.repeatDays.contains(currentDayOfWeek)) {
            val todayAlarm = today.atTime(alarmTime)
            if (todayAlarm.isAfter(now)) {
                return todayAlarm
            }
        }

        // Find the next day the alarm should ring
        for (daysToAdd in 1..7) {
            val futureDate = today.plusDays(daysToAdd.toLong())
            if (alarm.repeatDays.contains(futureDate.dayOfWeek)) {
                return futureDate.atTime(alarmTime)
            }
        }

        // Fallback: tomorrow at alarm time
        return today.plusDays(1).atTime(alarmTime)
    }
}
