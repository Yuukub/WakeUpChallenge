package com.wakechallenge

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import com.wakechallenge.util.LocaleHelper
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.wakechallenge.domain.model.GameType
import com.wakechallenge.presentation.games.GameScreen
import com.wakechallenge.service.AlarmReceiver
import com.wakechallenge.service.AlarmService
import com.wakechallenge.ui.theme.WakeUpChallengeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameActivity : ComponentActivity() {

    private var alarmId: Long = -1

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)
        val gameType = intent.getStringExtra("game_type") ?: GameType.MATH.name

        // Keep screen on and show over lock screen
        setupWakeUpScreen()

        enableEdgeToEdge()
        setContent {
            WakeUpChallengeTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen(
                        alarmId = alarmId,
                        gameType = gameType,
                        isPracticeMode = false,
                        onGameComplete = {
                            // Stop the alarm service
                            stopAlarmService()
                            // Close this activity
                            finish()
                        }
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

        // Keep screen on during game
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun stopAlarmService() {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        startService(stopIntent)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Disable back button - user must complete the game
        // Do nothing
    }
}
