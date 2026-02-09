package com.wakechallenge.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.wakechallenge.domain.model.Alarm
import com.wakechallenge.domain.model.GameDifficulty
import com.wakechallenge.domain.model.GameType
import java.time.DayOfWeek
import java.time.LocalTime

@Entity(tableName = "alarms")
@TypeConverters(AlarmConverters::class)
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    val isEnabled: Boolean,
    val repeatDays: String, // Stored as comma-separated values
    val selectedGames: String, // Stored as comma-separated values
    val gameDifficulty: String,
    val soundUri: String?,
    val isVibrationEnabled: Boolean,
    val snoozeEnabled: Boolean,
    val snoozeDurationMinutes: Int,
    val maxSnoozeCount: Int,
    val currentSnoozeCount: Int,
    val gradualVolumeEnabled: Boolean,
    val gradualVolumeDurationSeconds: Int,
    val isSmartAlarm: Boolean,
    val smartAlarmWindowMinutes: Int
) {
    fun toDomain(): Alarm = Alarm(
        id = id,
        time = LocalTime.of(hour, minute),
        label = label,
        isEnabled = isEnabled,
        repeatDays = if (repeatDays.isBlank()) emptySet() else repeatDays.split(",").map { DayOfWeek.valueOf(it) }.toSet(),
        selectedGames = if (selectedGames.isBlank()) setOf(GameType.MATH) else selectedGames.split(",").map { GameType.valueOf(it) }.toSet(),
        gameDifficulty = GameDifficulty.valueOf(gameDifficulty),
        soundUri = soundUri,
        isVibrationEnabled = isVibrationEnabled,
        snoozeEnabled = snoozeEnabled,
        snoozeDurationMinutes = snoozeDurationMinutes,
        maxSnoozeCount = maxSnoozeCount,
        currentSnoozeCount = currentSnoozeCount,
        gradualVolumeEnabled = gradualVolumeEnabled,
        gradualVolumeDurationSeconds = gradualVolumeDurationSeconds,
        isSmartAlarm = isSmartAlarm,
        smartAlarmWindowMinutes = smartAlarmWindowMinutes
    )

    companion object {
        fun fromDomain(alarm: Alarm): AlarmEntity = AlarmEntity(
            id = alarm.id,
            hour = alarm.time.hour,
            minute = alarm.time.minute,
            label = alarm.label,
            isEnabled = alarm.isEnabled,
            repeatDays = alarm.repeatDays.joinToString(",") { it.name },
            selectedGames = alarm.selectedGames.joinToString(",") { it.name },
            gameDifficulty = alarm.gameDifficulty.name,
            soundUri = alarm.soundUri,
            isVibrationEnabled = alarm.isVibrationEnabled,
            snoozeEnabled = alarm.snoozeEnabled,
            snoozeDurationMinutes = alarm.snoozeDurationMinutes,
            maxSnoozeCount = alarm.maxSnoozeCount,
            currentSnoozeCount = alarm.currentSnoozeCount,
            gradualVolumeEnabled = alarm.gradualVolumeEnabled,
            gradualVolumeDurationSeconds = alarm.gradualVolumeDurationSeconds,
            isSmartAlarm = alarm.isSmartAlarm,
            smartAlarmWindowMinutes = alarm.smartAlarmWindowMinutes
        )
    }
}

class AlarmConverters {
    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? = time?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }
}
