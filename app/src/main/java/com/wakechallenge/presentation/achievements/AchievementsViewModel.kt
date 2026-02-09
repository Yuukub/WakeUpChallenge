package com.wakechallenge.presentation.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakechallenge.data.database.AchievementDao
import com.wakechallenge.data.database.AchievementEntity
import com.wakechallenge.data.database.Achievements
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementDao: AchievementDao
) : ViewModel() {

    private val _achievements = MutableStateFlow<List<AchievementEntity>>(emptyList())
    val achievements: StateFlow<List<AchievementEntity>> = _achievements.asStateFlow()

    init {
        initializeAchievements()
        loadAchievements()
    }

    private fun initializeAchievements() {
        viewModelScope.launch {
            // Insert default achievements if they don't exist
            val existingIds = achievementDao.getAllAchievements()
            achievementDao.insertAchievements(Achievements.ALL)
        }
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            achievementDao.getAllAchievements().collect { list ->
                _achievements.value = list.ifEmpty { Achievements.ALL }
            }
        }
    }
}
