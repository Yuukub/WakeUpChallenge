package com.wakechallenge.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.wakechallenge.AlarmActivity
import com.wakechallenge.R
import com.wakechallenge.WakeUpChallengeApp
import com.wakechallenge.data.repository.AlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@AndroidEntryPoint
class AlarmService : Service() {

    companion object {
        private const val TAG = "AlarmService"
        const val ACTION_START_ALARM = "com.wakechallenge.START_ALARM"
        const val ACTION_STOP_ALARM = "com.wakechallenge.STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.wakechallenge.SNOOZE_ALARM"
        const val ACTION_RESCHEDULE_ALARMS = "com.wakechallenge.RESCHEDULE_ALARMS"
        const val NOTIFICATION_ID = 1001
    }

    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var currentAlarmId: Long = -1
    private var volumeJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val alarmMutex = Mutex()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ALARM -> {
                val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)
                if (alarmId != -1L) {
                    currentAlarmId = alarmId
                    startForeground(NOTIFICATION_ID, createNotification(alarmId))
                    startAlarm(alarmId)
                }
            }
            ACTION_STOP_ALARM -> {
                stopAlarm()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_SNOOZE_ALARM -> {
                snoozeAlarm()
            }
            ACTION_RESCHEDULE_ALARMS -> {
                rescheduleAllAlarms()
            }
        }
        return START_STICKY
    }

    private fun startAlarm(alarmId: Long) {
        serviceScope.launch {
            val alarm = alarmRepository.getAlarmById(alarmId) ?: return@launch

            // Start vibration
            if (alarm.isVibrationEnabled) {
                startVibration()
            }

            // Start sound
            startAlarmSound(alarm.soundUri, alarm.gradualVolumeEnabled, alarm.gradualVolumeDurationSeconds)

            // Launch alarm activity
            val alarmActivityIntent = Intent(this@AlarmService, AlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            }
            startActivity(alarmActivityIntent)
        }
    }

    private fun startAlarmSound(soundUri: String?, gradualVolume: Boolean, gradualDurationSeconds: Int) {
        val uri = if (soundUri.isNullOrBlank()) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        } else {
            android.net.Uri.parse(soundUri)
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                prepare()

                if (gradualVolume) {
                    setVolume(0.1f, 0.1f)
                    startGradualVolume(gradualDurationSeconds)
                } else {
                    setVolume(1f, 1f)
                }

                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm sound: ${e.message}", e)
            // Fallback to default alarm sound
            try {
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@AlarmService, defaultUri)
                    isLooping = true
                    prepare()
                    setVolume(1f, 1f)
                    start()
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to play default alarm sound: ${e2.message}", e2)
            }
        }
    }

    private fun startGradualVolume(durationSeconds: Int) {
        volumeJob?.cancel()
        volumeJob = serviceScope.launch {
            val steps = 20
            val delayPerStep = (durationSeconds * 1000L) / steps

            for (i in 1..steps) {
                delay(delayPerStep)
                val volume = i.toFloat() / steps
                mediaPlayer?.setVolume(volume, volume)
            }
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 500, 200, 500, 200, 500, 1000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAlarm() {
        serviceScope.launch {
            alarmMutex.withLock {
                volumeJob?.cancel()
                try {
                    mediaPlayer?.apply {
                        if (isPlaying) stop()
                        release()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping media player: ${e.message}", e)
                }
                mediaPlayer = null
                vibrator?.cancel()
            }
        }
    }

    private fun snoozeAlarm() {
        serviceScope.launch {
            val alarm = alarmRepository.getAlarmById(currentAlarmId) ?: return@launch

            if (alarm.snoozeEnabled && alarm.currentSnoozeCount < alarm.maxSnoozeCount) {
                // Update snooze count
                alarmRepository.updateSnoozeCount(alarm.id, alarm.currentSnoozeCount + 1)

                // Schedule snooze
                alarmScheduler.scheduleSnooze(alarm)

                // Stop current alarm
                stopAlarm()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun rescheduleAllAlarms() {
        serviceScope.launch {
            alarmRepository.getEnabledAlarms().collect { alarms ->
                alarms.forEach { alarm ->
                    alarmScheduler.scheduleAlarm(alarm)
                }
                stopSelf()
            }
        }
    }

    private fun createNotification(alarmId: Long): Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_SNOOZE_ALARM
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, WakeUpChallengeApp.ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Wake Up Challenge")
            .setContentText("Time to wake up! Complete a game to dismiss.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.drawable.ic_snooze, "Snooze", snoozePendingIntent)
            .addAction(R.drawable.ic_dismiss, "Dismiss", dismissPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        serviceScope.cancel()
    }
}
