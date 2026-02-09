package com.wakechallenge.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wakechallenge.presentation.addalarm.AddAlarmViewModel
import com.wakechallenge.presentation.addalarm.AddAlarmScreen
import com.wakechallenge.presentation.alarm.AlarmScreen
import com.wakechallenge.presentation.games.GameScreen
import com.wakechallenge.presentation.home.HomeScreen
import com.wakechallenge.presentation.settings.SettingsScreen
import com.wakechallenge.presentation.statistics.StatisticsScreen
import com.wakechallenge.presentation.achievements.AchievementsScreen
import com.wakechallenge.presentation.games.GamePracticeScreen
import com.wakechallenge.presentation.sound.SoundPickerScreen
import com.wakechallenge.presentation.sound.AudioRecorderScreen
import com.wakechallenge.presentation.sound.TTSScreen
import com.wakechallenge.domain.model.AlarmSound

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AddAlarm : Screen("add_alarm?alarmId={alarmId}") {
        fun createRoute(alarmId: Long? = null) = if (alarmId != null) {
            "add_alarm?alarmId=$alarmId"
        } else {
            "add_alarm"
        }
    }
    data object Alarm : Screen("alarm/{alarmId}") {
        fun createRoute(alarmId: Long) = "alarm/$alarmId"
    }
    data object Game : Screen("game/{alarmId}/{gameType}") {
        fun createRoute(alarmId: Long, gameType: String) = "game/$alarmId/$gameType"
    }
    data object GamePractice : Screen("game_practice/{gameType}") {
        fun createRoute(gameType: String) = "game_practice/$gameType"
    }
    data object Settings : Screen("settings")
    data object Statistics : Screen("statistics")
    data object Achievements : Screen("achievements")
    data object SoundPicker : Screen("sound_picker?currentUri={currentUri}") {
        fun createRoute(currentUri: String? = null) = if (currentUri != null) {
            "sound_picker?currentUri=$currentUri"
        } else {
            "sound_picker"
        }
    }
    data object AudioRecorder : Screen("audio_recorder")
    data object TTS : Screen("tts")
}

@Composable
fun WakeUpChallengeNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onAddAlarm = { navController.navigate(Screen.AddAlarm.createRoute()) },
                onEditAlarm = { alarmId -> navController.navigate(Screen.AddAlarm.createRoute(alarmId)) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenStatistics = { navController.navigate(Screen.Statistics.route) },
                onOpenAchievements = { navController.navigate(Screen.Achievements.route) }
            )
        }

        composable(
            route = Screen.AddAlarm.route,
            arguments = listOf(
                navArgument("alarmId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: -1L
            val viewModel: AddAlarmViewModel = hiltViewModel()

            // Get selected sound from SoundPicker
            val selectedSoundUri = backStackEntry.savedStateHandle.get<String>("selectedSound")
            LaunchedEffect(selectedSoundUri) {
                selectedSoundUri?.let {
                    viewModel.updateSoundUri(it)
                    backStackEntry.savedStateHandle.remove<String>("selectedSound")
                }
            }

            AddAlarmScreen(
                alarmId = if (alarmId == -1L) null else alarmId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onPracticeGame = { gameType ->
                    navController.navigate(Screen.GamePractice.createRoute(gameType.name))
                },
                onSelectSound = {
                    val currentUri = viewModel.uiState.value.soundUri ?: ""
                    navController.navigate(Screen.SoundPicker.createRoute(currentUri))
                }
            )
        }

        composable(
            route = Screen.Alarm.route,
            arguments = listOf(
                navArgument("alarmId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: return@composable
            AlarmScreen(
                alarmId = alarmId,
                onDismiss = { navController.popBackStack(Screen.Home.route, false) },
                onStartGame = { gameType ->
                    navController.navigate(Screen.Game.createRoute(alarmId, gameType.name))
                }
            )
        }

        composable(
            route = Screen.Game.route,
            arguments = listOf(
                navArgument("alarmId") { type = NavType.LongType },
                navArgument("gameType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: return@composable
            val gameType = backStackEntry.arguments?.getString("gameType") ?: return@composable
            GameScreen(
                alarmId = alarmId,
                gameType = gameType,
                isPracticeMode = false,
                onGameComplete = {
                    navController.popBackStack(Screen.Home.route, false)
                }
            )
        }

        composable(
            route = Screen.GamePractice.route,
            arguments = listOf(
                navArgument("gameType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val gameType = backStackEntry.arguments?.getString("gameType") ?: return@composable
            GamePracticeScreen(
                gameType = gameType,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGamePractice = { gameType ->
                    navController.navigate(Screen.GamePractice.createRoute(gameType.name))
                }
            )
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Achievements.route) {
            AchievementsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SoundPicker.route,
            arguments = listOf(
                navArgument("currentUri") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val currentUri = backStackEntry.arguments?.getString("currentUri")
            SoundPickerScreen(
                currentSoundUri = currentUri,
                onSoundSelected = { sound ->
                    // Handle sound selection - save to previous screen's saved state
                    navController.previousBackStackEntry?.savedStateHandle?.set("selectedSound", sound?.uri)
                    navController.popBackStack()
                },
                onNavigateToRecorder = {
                    navController.navigate(Screen.AudioRecorder.route)
                },
                onNavigateToTTS = {
                    navController.navigate(Screen.TTS.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AudioRecorder.route) {
            AudioRecorderScreen(
                onSoundRecorded = { sound ->
                    // Pass back to sound picker, then to add alarm
                    navController.previousBackStackEntry?.savedStateHandle?.set("recordedSound", sound.uri)
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TTS.route) {
            TTSScreen(
                onSoundCreated = { sound ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("ttsSound", sound.uri)
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
