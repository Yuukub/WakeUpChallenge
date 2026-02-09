package com.wakechallenge.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ALARM_TRIGGERED = "com.wakechallenge.ALARM_TRIGGERED"
        const val ACTION_START_SMART_TRACKING = "com.wakechallenge.START_SMART_TRACKING"
        const val ACTION_SNOOZE = "com.wakechallenge.SNOOZE"
        const val ACTION_DISMISS = "com.wakechallenge.DISMISS"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_IS_SNOOZE = "is_snooze"
        const val EXTRA_IS_SMART_ALARM = "is_smart_alarm"
        const val EXTRA_ALARM_TIME_MILLIS = "alarm_time_millis"
        const val EXTRA_SMART_WINDOW_MINUTES = "smart_window_minutes"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ALARM_TRIGGERED -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
                val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)

                if (alarmId != -1L) {
                    startAlarmService(context, alarmId, isSnooze)
                }
            }
            ACTION_START_SMART_TRACKING -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
                val alarmTimeMillis = intent.getLongExtra(EXTRA_ALARM_TIME_MILLIS, 0)
                val windowMinutes = intent.getIntExtra(EXTRA_SMART_WINDOW_MINUTES, 30)

                if (alarmId != -1L && alarmTimeMillis > 0) {
                    startSleepTrackingService(context, alarmId, alarmTimeMillis, windowMinutes)
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Reschedule all alarms after device reboot
                rescheduleAlarms(context)
            }
        }
    }

    private fun startAlarmService(context: Context, alarmId: Long, isSnooze: Boolean) {
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun rescheduleAlarms(context: Context) {
        // This would be called after device reboot to reschedule all alarms
        // Implementation would load alarms from database and reschedule them
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_RESCHEDULE_ALARMS
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun startSleepTrackingService(
        context: Context,
        alarmId: Long,
        alarmTimeMillis: Long,
        windowMinutes: Int
    ) {
        val serviceIntent = Intent(context, SleepTrackingService::class.java).apply {
            action = SleepTrackingService.ACTION_START_TRACKING
            putExtra(SleepTrackingService.EXTRA_ALARM_ID, alarmId)
            putExtra(SleepTrackingService.EXTRA_ALARM_TIME_MILLIS, alarmTimeMillis)
            putExtra(SleepTrackingService.EXTRA_WINDOW_MINUTES, windowMinutes)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
