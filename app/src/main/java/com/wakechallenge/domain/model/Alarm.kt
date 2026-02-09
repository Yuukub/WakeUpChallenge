package com.wakechallenge.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

data class Alarm(
    val id: Long = 0,
    val time: LocalTime,
    val label: String = "",
    val isEnabled: Boolean = true,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val selectedGames: Set<GameType> = setOf(GameType.MATH),
    val gameDifficulty: GameDifficulty = GameDifficulty.MEDIUM,
    val soundUri: String? = null,
    val isVibrationEnabled: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 5,
    val maxSnoozeCount: Int = 3,
    val currentSnoozeCount: Int = 0,
    val gradualVolumeEnabled: Boolean = false,
    val gradualVolumeDurationSeconds: Int = 60,
    val isSmartAlarm: Boolean = false,
    val smartAlarmWindowMinutes: Int = 30
) {
    val isOneTime: Boolean get() = repeatDays.isEmpty()

    val repeatDaysText: String get() = when {
        repeatDays.isEmpty() -> "Once"
        repeatDays.size == 7 -> "Every day"
        repeatDays == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekends"
        repeatDays == setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        ) -> "Weekdays"
        else -> repeatDays.joinToString(", ") { it.name.take(3) }
    }

    val timeText: String get() = String.format("%02d:%02d", time.hour, time.minute)
}

data class AlarmSound(
    val id: String,
    val name: String,
    val uri: String,
    val type: SoundType
)

enum class SoundType {
    DEFAULT,
    MUSIC,
    RECORDED,
    TTS
}
