package com.wakechallenge.data.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val progress: Int = 0,
    val maxProgress: Int = 1
)

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, name ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1")
    fun getUnlockedAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievementById(id: String): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("UPDATE achievements SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int)

    @Query("UPDATE achievements SET isUnlocked = 1, unlockedAt = :timestamp WHERE id = :id")
    suspend fun unlockAchievement(id: String, timestamp: Long)
}

// Predefined achievements
object Achievements {
    val ALL = listOf(
        AchievementEntity(
            id = "early_bird",
            name = "Early Bird",
            description = "Wake up on time for 7 consecutive days",
            iconName = "wb_sunny",
            maxProgress = 7
        ),
        AchievementEntity(
            id = "speed_demon",
            name = "Speed Demon",
            description = "Complete any game in under 10 seconds",
            iconName = "speed",
            maxProgress = 1
        ),
        AchievementEntity(
            id = "math_master",
            name = "Math Master",
            description = "Complete 50 math challenges",
            iconName = "calculate",
            maxProgress = 50
        ),
        AchievementEntity(
            id = "puzzle_pro",
            name = "Puzzle Pro",
            description = "Complete 20 puzzle games",
            iconName = "extension",
            maxProgress = 20
        ),
        AchievementEntity(
            id = "shake_champion",
            name = "Shake Champion",
            description = "Complete shake challenge 30 times",
            iconName = "vibration",
            maxProgress = 30
        ),
        AchievementEntity(
            id = "no_snooze",
            name = "No Snooze Zone",
            description = "Dismiss alarm without snoozing for 5 days",
            iconName = "alarm_off",
            maxProgress = 5
        ),
        AchievementEntity(
            id = "streak_week",
            name = "Weekly Warrior",
            description = "Maintain a 7-day wake-up streak",
            iconName = "local_fire_department",
            maxProgress = 7
        ),
        AchievementEntity(
            id = "streak_month",
            name = "Monthly Master",
            description = "Maintain a 30-day wake-up streak",
            iconName = "emoji_events",
            maxProgress = 30
        ),
        AchievementEntity(
            id = "variety_gamer",
            name = "Variety Gamer",
            description = "Play all 8 different games",
            iconName = "casino",
            maxProgress = 8
        ),
        AchievementEntity(
            id = "perfect_week",
            name = "Perfect Week",
            description = "Wake up at exact alarm time for a week",
            iconName = "stars",
            maxProgress = 7
        )
    )
}
