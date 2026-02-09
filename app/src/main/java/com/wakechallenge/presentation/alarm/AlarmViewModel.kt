package com.wakechallenge.presentation.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakechallenge.data.repository.AlarmRepository
import com.wakechallenge.domain.model.Alarm
import com.wakechallenge.service.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _alarm = MutableStateFlow<Alarm?>(null)
    val alarm: StateFlow<Alarm?> = _alarm.asStateFlow()

    fun loadAlarm(alarmId: Long) {
        viewModelScope.launch {
            _alarm.value = alarmRepository.getAlarmById(alarmId)
        }
    }

    fun snoozeAlarm() {
        viewModelScope.launch {
            val currentAlarm = _alarm.value ?: return@launch

            if (currentAlarm.snoozeEnabled && currentAlarm.currentSnoozeCount < currentAlarm.maxSnoozeCount) {
                // Update snooze count
                alarmRepository.updateSnoozeCount(
                    currentAlarm.id,
                    currentAlarm.currentSnoozeCount + 1
                )

                // Schedule snooze alarm
                alarmScheduler.scheduleSnooze(currentAlarm)
            }
        }
    }

    fun dismissAlarm() {
        viewModelScope.launch {
            val currentAlarm = _alarm.value ?: return@launch

            // Reset snooze count
            alarmRepository.updateSnoozeCount(currentAlarm.id, 0)

            // If it's a one-time alarm, disable it
            if (currentAlarm.isOneTime) {
                alarmRepository.updateAlarmEnabled(currentAlarm.id, false)
            } else {
                // Schedule next occurrence for repeating alarm
                alarmScheduler.scheduleAlarm(currentAlarm)
            }
        }
    }
}
