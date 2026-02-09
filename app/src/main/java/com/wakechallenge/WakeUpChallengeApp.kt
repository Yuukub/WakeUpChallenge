package com.wakechallenge

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WakeUpChallengeApp : Application() {

    companion object {
        const val ALARM_CHANNEL_ID = "alarm_channel"
        const val ALARM_CHANNEL_NAME = "Alarm Notifications"
        const val TRACKING_CHANNEL_ID = "tracking_channel"
        const val TRACKING_CHANNEL_NAME = "Sleep Tracking"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Alarm channel - high priority
            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                ALARM_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for alarm notifications"
                enableVibration(true)
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(alarmChannel)

            // Sleep tracking channel - low priority
            val trackingChannel = NotificationChannel(
                TRACKING_CHANNEL_ID,
                TRACKING_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for sleep tracking service"
                enableVibration(false)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(trackingChannel)
        }
    }
}
