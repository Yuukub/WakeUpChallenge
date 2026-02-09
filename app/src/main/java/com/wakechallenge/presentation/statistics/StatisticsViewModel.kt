package com.wakechallenge.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakechallenge.data.database.StatisticsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class StatisticsUiState(
    val totalWakeUps: Int = 0,
    val currentStreak: Int = 0,
    val avgGameTimeSeconds: Int = 0,
    val gamesPlayed: Int = 0,
    val weeklyData: List<Pair<String, Int>> = emptyList(),
    val mostPlayedGames: List<Pair<String, Int>> = emptyList()
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsDao: StatisticsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            statisticsDao.getAllStatistics().collect { stats ->
                val totalWakeUps = stats.size
                val gamesPlayed = stats.size
                val avgGameTime = if (stats.isNotEmpty()) {
                    (stats.sumOf { it.gameDuration } / stats.size / 1000).toInt()
                } else 0

                // Calculate current streak
                val streak = calculateStreak(stats.map { it.date }.distinct().sorted())

                // Weekly data
                val today = LocalDate.now()
                val weeklyData = (6 downTo 0).map { daysAgo ->
                    val date = today.minusDays(daysAgo.toLong())
                    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    val count = stats.count { it.date == date.format(DateTimeFormatter.ISO_DATE) }
                    dayName to count
                }

                // Most played games
                val gameCount = stats.groupBy { it.gameType }
                    .mapValues { it.value.size }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(5)
                    .map { (game, count) ->
                        game.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() } to count
                    }

                _uiState.update {
                    it.copy(
                        totalWakeUps = totalWakeUps,
                        currentStreak = streak,
                        avgGameTimeSeconds = avgGameTime,
                        gamesPlayed = gamesPlayed,
                        weeklyData = weeklyData,
                        mostPlayedGames = gameCount
                    )
                }
            }
        }
    }

    private fun calculateStreak(dates: List<String>): Int {
        if (dates.isEmpty()) return 0

        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_DATE)

        // Check if user woke up today or yesterday
        if (!dates.contains(today) && !dates.contains(yesterday)) {
            return 0
        }

        var streak = 0
        var checkDate = LocalDate.now()

        while (dates.contains(checkDate.format(DateTimeFormatter.ISO_DATE))) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        return streak
    }
}
