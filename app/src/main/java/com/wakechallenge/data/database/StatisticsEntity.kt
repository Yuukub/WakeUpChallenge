package com.wakechallenge.data.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "statistics")
data class StatisticsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alarmId: Long,
    val scheduledTime: Long, // Timestamp
    val wakeUpTime: Long, // Timestamp when alarm was dismissed
    val gameType: String,
    val gameDuration: Long, // Milliseconds to complete the game
    val snoozedCount: Int,
    val date: String // YYYY-MM-DD format
)

@Dao
interface StatisticsDao {
    @Query("SELECT * FROM statistics ORDER BY wakeUpTime DESC")
    fun getAllStatistics(): Flow<List<StatisticsEntity>>

    @Query("SELECT * FROM statistics WHERE date BETWEEN :startDate AND :endDate ORDER BY wakeUpTime DESC")
    fun getStatisticsByDateRange(startDate: String, endDate: String): Flow<List<StatisticsEntity>>

    @Query("SELECT * FROM statistics WHERE date = :date")
    fun getStatisticsByDate(date: String): Flow<List<StatisticsEntity>>

    @Query("SELECT AVG(gameDuration) FROM statistics WHERE gameType = :gameType")
    suspend fun getAverageGameDuration(gameType: String): Long?

    @Query("SELECT COUNT(*) FROM statistics WHERE date = :date")
    suspend fun getWakeUpCountByDate(date: String): Int

    @Query("SELECT gameType, COUNT(*) as count FROM statistics GROUP BY gameType ORDER BY count DESC")
    fun getMostPlayedGames(): Flow<List<GamePlayCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatistics(statistics: StatisticsEntity): Long

    @Query("DELETE FROM statistics")
    suspend fun clearAllStatistics()
}

data class GamePlayCount(
    val gameType: String,
    val count: Int
)
