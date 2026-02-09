package com.wakechallenge.data.repository

import com.wakechallenge.data.database.AlarmDao
import com.wakechallenge.data.database.AlarmEntity
import com.wakechallenge.domain.model.Alarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
    private val alarmDao: AlarmDao
) {
    fun getAllAlarms(): Flow<List<Alarm>> = alarmDao.getAllAlarms().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getEnabledAlarms(): Flow<List<Alarm>> = alarmDao.getEnabledAlarms().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getAlarmById(id: Long): Alarm? = alarmDao.getAlarmById(id)?.toDomain()

    suspend fun insertAlarm(alarm: Alarm): Long {
        return alarmDao.insertAlarm(AlarmEntity.fromDomain(alarm))
    }

    suspend fun updateAlarm(alarm: Alarm) {
        alarmDao.updateAlarm(AlarmEntity.fromDomain(alarm))
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        alarmDao.deleteAlarm(AlarmEntity.fromDomain(alarm))
    }

    suspend fun deleteAlarmById(id: Long) {
        alarmDao.deleteAlarmById(id)
    }

    suspend fun updateAlarmEnabled(id: Long, isEnabled: Boolean) {
        alarmDao.updateAlarmEnabled(id, isEnabled)
    }

    suspend fun updateSnoozeCount(id: Long, count: Int) {
        alarmDao.updateSnoozeCount(id, count)
    }

    suspend fun getAlarmByTime(hour: Int, minute: Int): Alarm? {
        return alarmDao.getAlarmByTime(hour, minute)?.toDomain()
    }
}
