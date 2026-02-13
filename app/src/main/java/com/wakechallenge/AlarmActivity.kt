package com.wakechallenge

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import com.wakechallenge.util.LocaleHelper
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.wakechallenge.presentation.alarm.AlarmScreenContent
import com.wakechallenge.service.AlarmReceiver
import com.wakechallenge.service.AlarmService
import com.wakechallenge.ui.theme.WakeUpChallengeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    private var alarmId: Long = -1
    private var wakeLock: PowerManager.WakeLock? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)

        // Wake up and show over lock screen
        acquireWakeLock()
        setupWakeUpScreen()

        enableEdgeToEdge()
        setContent {
            WakeUpChallengeTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmScreenContent(
                        alarmId = alarmId,
                        onSnooze = { snoozeAlarm() },
                        onDismiss = { startGame() }
                    )
                }
            }
        }
    }

    private fun setupWakeUpScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        }

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "WakeUpChallenge::AlarmWakeLock"
            ).apply {
                acquire(10 * 60 * 1000L /* 10 minutes max */)
            }
        } catch (e: Exception) {
            // Log error if needed
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    private fun snoozeAlarm() {
        val serviceIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SNOOZE_ALARM
        }
        startService(serviceIntent)
        finish()
    }

    private fun startGame() {
        // Navigate to game activity
        val gameIntent = Intent(this, GameActivity::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        startActivity(gameIntent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Disable back button - user must complete the game
    }
}
