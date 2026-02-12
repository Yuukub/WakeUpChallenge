package com.wakechallenge.presentation.games

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakechallenge.data.database.StatisticsDao
import com.wakechallenge.data.database.StatisticsEntity
import com.wakechallenge.data.repository.AlarmRepository
import com.wakechallenge.domain.model.Alarm
import com.wakechallenge.service.AlarmScheduler
import com.wakechallenge.service.AlarmService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler,
    private val statisticsDao: StatisticsDao,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _alarm = MutableStateFlow<Alarm?>(null)
    val alarm: StateFlow<Alarm?> = _alarm.asStateFlow()

    private var gameStartTime: Long = System.currentTimeMillis()

    fun loadAlarm(alarmId: Long) {
        viewModelScope.launch {
            _alarm.value = alarmRepository.getAlarmById(alarmId)
            gameStartTime = System.currentTimeMillis()
        }
    }

    fun onGameComplete() {
        viewModelScope.launch {
            val currentAlarm = _alarm.value ?: return@launch

            // Stop alarm service
            val stopIntent = Intent(context, AlarmService::class.java).apply {
                action = AlarmService.ACTION_STOP_ALARM
            }
            context.startService(stopIntent)

            // Save statistics
            val gameDuration = System.currentTimeMillis() - gameStartTime
            val statistics = StatisticsEntity(
                alarmId = currentAlarm.id,
                scheduledTime = System.currentTimeMillis(),
                wakeUpTime = System.currentTimeMillis(),
                gameType = currentAlarm.selectedGames.firstOrNull()?.name ?: "MATH",
                gameDuration = gameDuration,
                snoozedCount = currentAlarm.currentSnoozeCount,
                date = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            )
            statisticsDao.insertStatistics(statistics)

            // Reset snooze count
            alarmRepository.updateSnoozeCount(currentAlarm.id, 0)

            // If one-time alarm, disable it
            if (currentAlarm.isOneTime) {
                alarmRepository.updateAlarmEnabled(currentAlarm.id, false)
            } else {
                // Schedule next occurrence
                alarmScheduler.scheduleAlarm(currentAlarm)
            }
        }
    }
}
