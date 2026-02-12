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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
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
    private var textToSpeech: TextToSpeech? = null
    private var isTTSReady = false
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
        
        initializeTTS()
    }

    private fun initializeTTS() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTTSReady = true
            }
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
            if (alarm.soundUri?.startsWith("tts://") == true) {
                playTTSSound(alarm.soundUri)
            } else {
                startAlarmSound(alarm.soundUri, alarm.gradualVolumeEnabled, alarm.gradualVolumeDurationSeconds)
            }

            // Launch alarm activity
            val alarmActivityIntent = Intent(this@AlarmService, AlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            }
            
            try {
                startActivity(alarmActivityIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AlarmActivity: ${e.message}", e)
                // Activity might be blocked by background restrictions, 
                // but notification fullScreenIntent should handle it.
            }
        }
    }

    private fun startAlarmSound(soundUri: String?, gradualVolume: Boolean, gradualDurationSeconds: Int) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                
                if (soundUri.isNullOrBlank()) {
                    setDataSource(this@AlarmService, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                } else if (soundUri.startsWith("/")) {
                    // Absolute path for recorded sound
                    setDataSource(soundUri)
                } else {
                    setDataSource(this@AlarmService, android.net.Uri.parse(soundUri))
                }
                
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
            fallbackToDefaultSound()
        }
    }

    private fun fallbackToDefaultSound() {
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

    private fun playTTSSound(uriString: String) {
        val soundId = uriString.substringAfter("tts://")
        val prefs = getSharedPreferences("tts_sounds", Context.MODE_PRIVATE)
        val text = prefs.getString("${soundId}_text", "Good morning! Time to wake up!") ?: ""
        val localeStr = prefs.getString("${soundId}_locale", "en_US") ?: "en_US"
        val rate = prefs.getFloat("${soundId}_rate", 1.0f)
        val pitch = prefs.getFloat("${soundId}_pitch", 1.0f)

        serviceScope.launch {
            // Wait for TTS to be ready
            var retry = 0
            while (!isTTSReady && retry < 10) {
                delay(500)
                retry++
            }

            if (isTTSReady) {
                textToSpeech?.apply {
                    val locale = if (localeStr.contains("_")) {
                        val parts = localeStr.split("_")
                        Locale(parts[0], parts[1])
                    } else {
                        Locale(localeStr)
                    }
                    language = locale
                    setSpeechRate(rate)
                    setPitch(pitch)
                    
                    // Set audio attributes for alarm usage
                    @Suppress("DEPRECATION")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    }

                    // Use a unique ID to loop manually if needed, 
                    // or just play once. Standard alarms usually loop.
                    speak(text, TextToSpeech.QUEUE_FLUSH, null, "alarm_tts")
                    
                    setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {
                            // Loop for alarm
                            if (utteranceId == "alarm_tts") {
                                serviceScope.launch {
                                    delay(2000) // Small pause between repeats
                                    speak(text, TextToSpeech.QUEUE_FLUSH, null, "alarm_tts")
                                }
                            }
                        }
                        override fun onError(utteranceId: String?) {
                            fallbackToDefaultSound()
                        }
                    })
                }
            } else {
                fallbackToDefaultSound()
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
        // Use a non-cancellable scope if possible, or just GlobalScope for cleanup, 
        // but better to just use the existing scope if it's active.
        // If getting called from UI/Action, we use a coroutine.
        if (serviceScope.isActive) {
            serviceScope.launch {
                stopAlarmInternal()
            }
        } else {
            // Fallback if scope is already dead (unlikely here but good capability)
            runBlocking {
                stopAlarmInternal()
            }
        }
    }

    private suspend fun stopAlarmInternal() {
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
            textToSpeech?.stop()
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
            val alarms = alarmRepository.getEnabledAlarms().first()
            alarms.forEach { alarm ->
                alarmScheduler.scheduleAlarm(alarm)
            }
            stopSelf()
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
            .setContentText("ถึงเวลาตื่นแล้ว! แตะที่นี่เพื่อเล่นเกมและปิดเสียงปลุก")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent) // Ensure tapping notification opens activity
            .addAction(R.drawable.ic_snooze, "Snooze", snoozePendingIntent)
            .addAction(R.drawable.ic_dismiss, "Dismiss", dismissPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Run cleanup synchronously to ensure it happens before process death
        runBlocking {
            stopAlarmInternal()
        }
        textToSpeech?.shutdown()
        serviceScope.cancel()
    }
}
