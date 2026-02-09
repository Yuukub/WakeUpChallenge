package com.wakechallenge.di

import android.content.Context
import androidx.room.Room
import com.wakechallenge.data.database.AchievementDao
import com.wakechallenge.data.database.AlarmDao
import com.wakechallenge.data.database.AlarmDatabase
import com.wakechallenge.data.database.StatisticsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAlarmDatabase(
        @ApplicationContext context: Context
    ): AlarmDatabase {
        return Room.databaseBuilder(
            context,
            AlarmDatabase::class.java,
            AlarmDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideAlarmDao(database: AlarmDatabase): AlarmDao {
        return database.alarmDao()
    }

    @Provides
    @Singleton
    fun provideStatisticsDao(database: AlarmDatabase): StatisticsDao {
        return database.statisticsDao()
    }

    @Provides
    @Singleton
    fun provideAchievementDao(database: AlarmDatabase): AchievementDao {
        return database.achievementDao()
    }
}
