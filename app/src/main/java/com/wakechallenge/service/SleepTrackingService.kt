package com.wakechallenge.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.wakechallenge.R
import com.wakechallenge.WakeUpChallengeApp
import com.wakechallenge.data.repository.AlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Service that tracks sleep patterns using accelerometer data
 * to determine the optimal time to wake the user within their alarm window.
 *
 * Sleep phases are detected based on movement patterns:
 * - Deep Sleep: Very little movement
 * - Light Sleep: Moderate movement (best time to wake)
 * - REM Sleep: Occasional movement bursts
 * - Awake: Frequent movement
 */
@AndroidEntryPoint
class SleepTrackingService : Service(), SensorEventListener {

    companion object {
        const val ACTION_START_TRACKING = "com.wakechallenge.START_SLEEP_TRACKING"
        const val ACTION_STOP_TRACKING = "com.wakechallenge.STOP_SLEEP_TRACKING"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_TIME_MILLIS = "alarm_time_millis"
        const val EXTRA_WINDOW_MINUTES = "window_minutes"
        const val NOTIFICATION_ID = 2001

        // Movement thresholds for sleep phase detection
        // TODO: Make these configurable in settings
        const val MOVEMENT_THRESHOLD_LOW = 0.5f    // Deep sleep
        const val MOVEMENT_THRESHOLD_MED = 1.5f    // Light sleep
        const val MOVEMENT_THRESHOLD_HIGH = 3.0f   // REM/Awake

        // Sampling settings
        private const val SAMPLE_WINDOW_MS = 30000L // 30 seconds
        private const val CHECK_INTERVAL_MS = 60000L // Check every minute
    }

    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var currentAlarmId: Long = -1
    private var targetAlarmTimeMillis: Long = 0
    private var windowMinutes: Int = 30
    private var windowStartMillis: Long = 0

    private val movementSamples = mutableListOf<Float>()
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var isFirstReading = true

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var monitoringJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Acquire wake lock to keep tracking while screen is off
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WakeUpChallenge::SleepTracking"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                currentAlarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
                targetAlarmTimeMillis = intent.getLongExtra(EXTRA_ALARM_TIME_MILLIS, 0)
                windowMinutes = intent.getIntExtra(EXTRA_WINDOW_MINUTES, 30)

                if (currentAlarmId != -1L && targetAlarmTimeMillis > 0) {
                    windowStartMillis = targetAlarmTimeMillis - (windowMinutes * 60 * 1000)
                    startForeground(NOTIFICATION_ID, createNotification())
                    startTracking()
                }
            }
            ACTION_STOP_TRACKING -> {
                stopTracking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startTracking() {
        wakeLock?.acquire(windowMinutes * 60 * 1000L + 60000L)

        // Register accelerometer listener
        accelerometer?.let { sensor ->
            sensorManager?.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        // Start monitoring job
        monitoringJob = serviceScope.launch {
            while (isActive) {
                delay(CHECK_INTERVAL_MS)
                checkSleepPhase()
            }
        }
    }

    private fun stopTracking() {
        monitoringJob?.cancel()
        sensorManager?.unregisterListener(this)
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        movementSamples.clear()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            if (sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = sensorEvent.values[0]
                val y = sensorEvent.values[1]
                val z = sensorEvent.values[2]

                if (!isFirstReading) {
                    // Calculate movement magnitude
                    val deltaX = x - lastX
                    val deltaY = y - lastY
                    val deltaZ = z - lastZ
                    val movement = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

                    synchronized(movementSamples) {
                        movementSamples.add(movement)
                        // Keep only samples from last sample window
                        if (movementSamples.size > 100) {
                            movementSamples.removeAt(0)
                        }
                    }
                }

                lastX = x
                lastY = y
                lastZ = z
                isFirstReading = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for our purposes
    }

    private fun checkSleepPhase() {
        val currentTimeMillis = System.currentTimeMillis()

        // Check if we're within the smart alarm window
        if (currentTimeMillis < windowStartMillis) {
            return // Not yet in the window
        }

        if (currentTimeMillis >= targetAlarmTimeMillis) {
            // Past the target time, trigger alarm immediately
            triggerAlarm()
            return
        }

        // Calculate average movement in the sample window
        val avgMovement = synchronized(movementSamples) {
            if (movementSamples.isEmpty()) 0f
            else movementSamples.average().toFloat()
        }

        // Determine sleep phase
        val sleepPhase = when {
            avgMovement < MOVEMENT_THRESHOLD_LOW -> SleepPhase.DEEP_SLEEP
            avgMovement < MOVEMENT_THRESHOLD_MED -> SleepPhase.LIGHT_SLEEP
            avgMovement < MOVEMENT_THRESHOLD_HIGH -> SleepPhase.REM
            else -> SleepPhase.AWAKE
        }

        // Trigger alarm if user is in light sleep (optimal wake time)
        // or if they appear to be awake already
        if (sleepPhase == SleepPhase.LIGHT_SLEEP || sleepPhase == SleepPhase.AWAKE) {
            // Give a slight delay to confirm the phase
            serviceScope.launch {
                delay(10000) // Wait 10 seconds to confirm

                // Recalculate
                val confirmAvg = synchronized(movementSamples) {
                    if (movementSamples.isEmpty()) 0f
                    else movementSamples.average().toFloat()
                }

                if (confirmAvg >= MOVEMENT_THRESHOLD_LOW) {
                    triggerAlarm()
                }
            }
        }

        // Update notification with current sleep phase
        updateNotification(sleepPhase, avgMovement)
    }

    private fun triggerAlarm() {
        stopTracking()

        // Trigger the alarm
        val alarmIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, currentAlarmId)
            action = AlarmReceiver.ACTION_ALARM_TRIGGERED
        }
        sendBroadcast(alarmIntent)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val cancelIntent = Intent(this, SleepTrackingService::class.java).apply {
            action = ACTION_STOP_TRACKING
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, WakeUpChallengeApp.TRACKING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Smart Alarm Active")
            .setContentText("Monitoring your sleep to wake you at the optimal time")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(R.drawable.ic_dismiss, "Cancel", cancelPendingIntent)
            .build()
    }

    private fun updateNotification(phase: SleepPhase, avgMovement: Float) {
        val phaseText = when (phase) {
            SleepPhase.DEEP_SLEEP -> "Deep Sleep"
            SleepPhase.LIGHT_SLEEP -> "Light Sleep (Optimal)"
            SleepPhase.REM -> "REM Sleep"
            SleepPhase.AWAKE -> "Awake"
        }

        val notification = NotificationCompat.Builder(this, WakeUpChallengeApp.TRACKING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Smart Alarm Active")
            .setContentText("Current phase: $phaseText")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
        serviceScope.cancel()
    }

    enum class SleepPhase {
        DEEP_SLEEP,
        LIGHT_SLEEP,
        REM,
        AWAKE
    }
}
