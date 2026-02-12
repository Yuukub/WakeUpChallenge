package com.wakechallenge.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [AlarmEntity::class, StatisticsEntity::class, AchievementEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(AlarmConverters::class)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun statisticsDao(): StatisticsDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        const val DATABASE_NAME = "wake_up_challenge_db"

        @Volatile
        private var INSTANCE: AlarmDatabase? = null

        fun getInstance(context: Context): AlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
