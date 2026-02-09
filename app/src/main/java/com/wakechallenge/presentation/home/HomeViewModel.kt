package com.wakechallenge.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakechallenge.data.repository.AlarmRepository
import com.wakechallenge.domain.model.Alarm
import com.wakechallenge.service.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class HomeUiState(
    val alarms: List<Alarm> = emptyList(),
    val nextAlarm: Alarm? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadAlarms()
    }

    private fun loadAlarms() {
        viewModelScope.launch {
            alarmRepository.getAllAlarms()
                .collect { alarms ->
                    val nextAlarm = findNextAlarm(alarms)
                    _uiState.update {
                        it.copy(
                            alarms = alarms,
                            nextAlarm = nextAlarm,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun findNextAlarm(alarms: List<Alarm>): Alarm? {
        val now = LocalTime.now()
        val enabledAlarms = alarms.filter { it.isEnabled }

        if (enabledAlarms.isEmpty()) return null

        // Find the next alarm that will ring
        val todayAlarms = enabledAlarms.filter { it.time.isAfter(now) }
        val tomorrowAlarms = enabledAlarms.filter { it.time.isBefore(now) || it.time == now }

        return todayAlarms.minByOrNull { it.time }
            ?: tomorrowAlarms.minByOrNull { it.time }
    }

    fun toggleAlarm(alarmId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            alarmRepository.updateAlarmEnabled(alarmId, isEnabled)

            // Schedule or cancel the alarm
            val alarm = alarmRepository.getAlarmById(alarmId)
            if (alarm != null) {
                if (isEnabled) {
                    alarmScheduler.scheduleAlarm(alarm)
                } else {
                    alarmScheduler.cancelAlarm(alarm)
                }
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmScheduler.cancelAlarm(alarm)
            alarmRepository.deleteAlarm(alarm)
        }
    }
}
