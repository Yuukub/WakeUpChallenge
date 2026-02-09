package com.wakechallenge.presentation.addalarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakechallenge.data.repository.AlarmRepository
import com.wakechallenge.domain.model.Alarm
import com.wakechallenge.domain.model.GameDifficulty
import com.wakechallenge.domain.model.GameType
import com.wakechallenge.service.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

data class AddAlarmUiState(
    val alarmId: Long? = null,
    val time: LocalTime = LocalTime.of(7, 0),
    val label: String = "",
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val selectedGames: Set<GameType> = setOf(GameType.MATH),
    val gameDifficulty: GameDifficulty = GameDifficulty.MEDIUM,
    val soundUri: String? = null,
    val isVibrationEnabled: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 5,
    val maxSnoozeCount: Int = 3,
    val gradualVolumeEnabled: Boolean = false,
    val gradualVolumeDurationSeconds: Int = 60,
    val isSmartAlarm: Boolean = false,
    val smartAlarmWindowMinutes: Int = 30,
    val isSaved: Boolean = false
)

@HiltViewModel
class AddAlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAlarmUiState())
    val uiState: StateFlow<AddAlarmUiState> = _uiState.asStateFlow()

    fun loadAlarm(alarmId: Long) {
        viewModelScope.launch {
            val alarm = alarmRepository.getAlarmById(alarmId) ?: return@launch
            _uiState.update {
                it.copy(
                    alarmId = alarm.id,
                    time = alarm.time,
                    label = alarm.label,
                    repeatDays = alarm.repeatDays,
                    selectedGames = alarm.selectedGames,
                    gameDifficulty = alarm.gameDifficulty,
                    soundUri = alarm.soundUri,
                    isVibrationEnabled = alarm.isVibrationEnabled,
                    snoozeEnabled = alarm.snoozeEnabled,
                    snoozeDurationMinutes = alarm.snoozeDurationMinutes,
                    maxSnoozeCount = alarm.maxSnoozeCount,
                    gradualVolumeEnabled = alarm.gradualVolumeEnabled,
                    gradualVolumeDurationSeconds = alarm.gradualVolumeDurationSeconds,
                    isSmartAlarm = alarm.isSmartAlarm,
                    smartAlarmWindowMinutes = alarm.smartAlarmWindowMinutes
                )
            }
        }
    }

    fun updateTime(time: LocalTime) {
        _uiState.update { it.copy(time = time) }
    }

    fun updateLabel(label: String) {
        _uiState.update { it.copy(label = label) }
    }

    fun toggleRepeatDay(day: DayOfWeek) {
        _uiState.update { state ->
            val newDays = if (state.repeatDays.contains(day)) {
                state.repeatDays - day
            } else {
                state.repeatDays + day
            }
            state.copy(repeatDays = newDays)
        }
    }

    fun toggleGame(game: GameType) {
        _uiState.update { state ->
            val newGames = if (state.selectedGames.contains(game)) {
                if (state.selectedGames.size > 1) {
                    state.selectedGames - game
                } else {
                    state.selectedGames // Must have at least one game
                }
            } else {
                state.selectedGames + game
            }
            state.copy(selectedGames = newGames)
        }
    }

    fun updateDifficulty(difficulty: GameDifficulty) {
        _uiState.update { it.copy(gameDifficulty = difficulty) }
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isVibrationEnabled = enabled) }
    }

    fun updateSnoozeEnabled(enabled: Boolean) {
        _uiState.update { it.copy(snoozeEnabled = enabled) }
    }

    fun updateSnoozeDuration(minutes: Int) {
        _uiState.update { it.copy(snoozeDurationMinutes = minutes) }
    }

    fun updateMaxSnoozeCount(count: Int) {
        _uiState.update { it.copy(maxSnoozeCount = count) }
    }

    fun updateGradualVolumeEnabled(enabled: Boolean) {
        _uiState.update { it.copy(gradualVolumeEnabled = enabled) }
    }

    fun updateGradualVolumeDuration(seconds: Int) {
        _uiState.update { it.copy(gradualVolumeDurationSeconds = seconds) }
    }

    fun updateSoundUri(uri: String?) {
        _uiState.update { it.copy(soundUri = uri) }
    }

    fun saveAlarm() {
        viewModelScope.launch {
            val state = _uiState.value
            val alarm = Alarm(
                id = state.alarmId ?: 0,
                time = state.time,
                label = state.label,
                isEnabled = true,
                repeatDays = state.repeatDays,
                selectedGames = state.selectedGames,
                gameDifficulty = state.gameDifficulty,
                soundUri = state.soundUri,
                isVibrationEnabled = state.isVibrationEnabled,
                snoozeEnabled = state.snoozeEnabled,
                snoozeDurationMinutes = state.snoozeDurationMinutes,
                maxSnoozeCount = state.maxSnoozeCount,
                gradualVolumeEnabled = state.gradualVolumeEnabled,
                gradualVolumeDurationSeconds = state.gradualVolumeDurationSeconds,
                isSmartAlarm = state.isSmartAlarm,
                smartAlarmWindowMinutes = state.smartAlarmWindowMinutes
            )

            val savedId = alarmRepository.insertAlarm(alarm)
            val savedAlarm = alarm.copy(id = savedId)

            // Schedule the alarm
            alarmScheduler.scheduleAlarm(savedAlarm)

            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
