package com.wakechallenge.presentation.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wakechallenge.R
import com.wakechallenge.domain.model.GameDifficulty
import com.wakechallenge.domain.model.GameType
import com.wakechallenge.domain.model.localizedDisplayName
import com.wakechallenge.presentation.components.GameBackground
import com.wakechallenge.presentation.components.GameBackgroundStyle

@Composable
fun GameScreen(
    alarmId: Long,
    gameType: String,
    isPracticeMode: Boolean,
    viewModel: GameViewModel = hiltViewModel(),
    onGameComplete: () -> Unit
) {
    val alarm by viewModel.alarm.collectAsState()
    val gameTypeEnum = GameType.valueOf(gameType)

    // Mutable state for difficulty - can be changed via Give Up
    var currentDifficulty by remember { mutableStateOf(alarm?.gameDifficulty ?: GameDifficulty.MEDIUM) }

    LaunchedEffect(alarmId) {
        if (!isPracticeMode) {
            viewModel.loadAlarm(alarmId)
        }
    }

    // Update difficulty when alarm loads
    LaunchedEffect(alarm?.gameDifficulty) {
        alarm?.gameDifficulty?.let { currentDifficulty = it }
    }

    val onComplete: () -> Unit = {
        if (!isPracticeMode) {
            viewModel.onGameComplete()
        }
        onGameComplete()
    }

    val onGiveUp: (GameDifficulty) -> Unit = { newDifficulty ->
        currentDifficulty = newDifficulty
    }

    // Key resets the game when difficulty changes
    Box(modifier = Modifier.fillMaxSize()) {
        key(currentDifficulty) {
            when (gameTypeEnum) {
                GameType.MATH -> MathGameScreen(
                    difficulty = currentDifficulty,
                    onGameComplete = onComplete,
                    onGiveUp = onGiveUp
                )
                GameType.TIC_TAC_TOE -> TicTacToeScreen(
                    difficulty = currentDifficulty,
                    onGameComplete = onComplete,
                    onGiveUp = onGiveUp
                )
                GameType.MEMORY_MATCH -> MemoryMatchScreen(
                    difficulty = currentDifficulty,
                    onGameComplete = onComplete,
                    onGiveUp = onGiveUp
                )
                GameType.TYPE_PHRASE -> TypePhraseScreen(
                    difficulty = currentDifficulty,
                    onGameComplete = onComplete,
                    onGiveUp = onGiveUp
                )
                GameType.PUZZLE_SLIDE -> PuzzleSlideScreen(
                    difficulty = currentDifficulty,
                    onGameComplete = onComplete,
                    onGiveUp = onGiveUp
                )
                GameType.COLOR_MATCH -> ColorMatchScreen(
                    difficulty = currentDifficulty,
                    onGameComplete = onComplete,
                    onGiveUp = onGiveUp
                )
            }
        }

        // Exit button for Practice Mode
        if (isPracticeMode) {
            IconButton(
                onClick = onGameComplete, // Reusing onGameComplete to exit/back
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .offset(y = 8.dp) // Adjust for status bar if needed, or rely on internal padding
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun GamePracticeScreen(
    gameType: String,
    onNavigateBack: () -> Unit
) {
    val gameTypeEnum = GameType.valueOf(gameType)
    var selectedDifficulty by remember { mutableStateOf<GameDifficulty?>(null) }

    val difficulty = selectedDifficulty

    if (difficulty == null) {
        DifficultySelectionScreen(
            gameType = gameTypeEnum,
            onDifficultySelected = { selectedDifficulty = it },
            onNavigateBack = onNavigateBack
        )
    } else {
        val onGiveUp: (GameDifficulty) -> Unit = { newDifficulty ->
            selectedDifficulty = newDifficulty
        }

        // Key resets the game when difficulty changes
        key(difficulty) {
            when (gameTypeEnum) {
                GameType.MATH -> MathGameScreen(
                    difficulty = difficulty,
                    onGameComplete = onNavigateBack,
                    onGiveUp = onGiveUp
                )
                GameType.TIC_TAC_TOE -> TicTacToeScreen(
                    difficulty = difficulty,
                    onGameComplete = onNavigateBack,
                    onGiveUp = onGiveUp
                )
                GameType.MEMORY_MATCH -> MemoryMatchScreen(
                    difficulty = difficulty,
                    onGameComplete = onNavigateBack,
                    onGiveUp = onGiveUp
                )
                GameType.TYPE_PHRASE -> TypePhraseScreen(
                    difficulty = difficulty,
                    onGameComplete = onNavigateBack,
                    onGiveUp = onGiveUp
                )
                GameType.PUZZLE_SLIDE -> PuzzleSlideScreen(
                    difficulty = difficulty,
                    onGameComplete = onNavigateBack,
                    onGiveUp = onGiveUp
                )
                GameType.COLOR_MATCH -> ColorMatchScreen(
                    difficulty = difficulty,
                    onGameComplete = onNavigateBack,
                    onGiveUp = onGiveUp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DifficultySelectionScreen(
    gameType: GameType,
    onDifficultySelected: (GameDifficulty) -> Unit,
    onNavigateBack: () -> Unit
) {
    GameBackground(style = GameBackgroundStyle.DEFAULT) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(gameType.localizedDisplayName(), color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.difficulty_select),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.difficulty_select_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                DifficultyCard(
                    difficulty = GameDifficulty.EASY,
                    description = getDifficultyDescription(gameType, GameDifficulty.EASY),
                    color = Color(0xFF4CAF50),
                    onClick = { onDifficultySelected(GameDifficulty.EASY) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                DifficultyCard(
                    difficulty = GameDifficulty.MEDIUM,
                    description = getDifficultyDescription(gameType, GameDifficulty.MEDIUM),
                    color = Color(0xFFFF9800),
                    onClick = { onDifficultySelected(GameDifficulty.MEDIUM) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                DifficultyCard(
                    difficulty = GameDifficulty.HARD,
                    description = getDifficultyDescription(gameType, GameDifficulty.HARD),
                    color = Color(0xFFF44336),
                    onClick = { onDifficultySelected(GameDifficulty.HARD) }
                )
            }
        }
    }
}

@Composable
private fun DifficultyCard(
    difficulty: GameDifficulty,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.2f)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = difficulty.localizedDisplayName(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.cd_play),
                tint = color,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun getDifficultyDescription(gameType: GameType, difficulty: GameDifficulty): String {
    return when (gameType) {
        GameType.MATH -> when (difficulty) {
            GameDifficulty.EASY -> stringResource(R.string.diff_math_easy)
            GameDifficulty.MEDIUM -> stringResource(R.string.diff_math_medium)
            GameDifficulty.HARD -> stringResource(R.string.diff_math_hard)
        }
        GameType.TIC_TAC_TOE -> when (difficulty) {
            GameDifficulty.EASY -> stringResource(R.string.diff_tictactoe_easy)
            GameDifficulty.MEDIUM -> stringResource(R.string.diff_tictactoe_medium)
            GameDifficulty.HARD -> stringResource(R.string.diff_tictactoe_hard)
        }
        GameType.MEMORY_MATCH -> when (difficulty) {
            GameDifficulty.EASY -> stringResource(R.string.diff_memory_easy)
            GameDifficulty.MEDIUM -> stringResource(R.string.diff_memory_medium)
            GameDifficulty.HARD -> stringResource(R.string.diff_memory_hard)
        }
        GameType.TYPE_PHRASE -> when (difficulty) {
            GameDifficulty.EASY -> stringResource(R.string.diff_phrase_easy)
            GameDifficulty.MEDIUM -> stringResource(R.string.diff_phrase_medium)
            GameDifficulty.HARD -> stringResource(R.string.diff_phrase_hard)
        }
        GameType.PUZZLE_SLIDE -> when (difficulty) {
            GameDifficulty.EASY -> stringResource(R.string.diff_puzzle_easy)
            GameDifficulty.MEDIUM -> stringResource(R.string.diff_puzzle_medium)
            GameDifficulty.HARD -> stringResource(R.string.diff_puzzle_hard)
        }
        GameType.COLOR_MATCH -> when (difficulty) {
            GameDifficulty.EASY -> stringResource(R.string.diff_color_easy)
            GameDifficulty.MEDIUM -> stringResource(R.string.diff_color_medium)
            GameDifficulty.HARD -> stringResource(R.string.diff_color_hard)
        }
    }
}
