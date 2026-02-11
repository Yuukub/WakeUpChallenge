package com.wakechallenge.presentation.games

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakechallenge.R
import kotlinx.coroutines.delay
import com.wakechallenge.domain.model.GameDifficulty
import com.wakechallenge.presentation.components.ConfettiAnimation
import com.wakechallenge.presentation.components.GameBackground
import com.wakechallenge.presentation.components.GameBackgroundStyle
import com.wakechallenge.presentation.components.GiveUpButton
import com.wakechallenge.ui.theme.TicTacToeAccent
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class Player { X, O, NONE }
enum class GameResult { WIN, LOSE, DRAW, ONGOING }

@Composable
fun TicTacToeScreen(
    difficulty: GameDifficulty,
    onGameComplete: () -> Unit,
    onGiveUp: ((GameDifficulty) -> Unit)? = null
) {
    var board by remember { mutableStateOf(List(9) { Player.NONE }) }
    var currentPlayer by remember { mutableStateOf(Player.X) }
    var gameResult by remember { mutableStateOf(GameResult.ONGOING) }
    var showResult by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    var winningCells by remember { mutableStateOf<List<Int>>(emptyList()) }
    var moveCount by remember { mutableIntStateOf(0) }
    var showMustWinMessage by remember { mutableStateOf(false) }

    // Icon pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "icon")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    // Check for game end
    LaunchedEffect(board) {
        val (result, cells) = checkGameResultWithWinningCells(board, Player.X)
        if (result != GameResult.ONGOING) {
            // Draw means try again (must win to complete) for ALL difficulties
            if (result == GameResult.DRAW) {
                showMustWinMessage = true
                delay(1500)
                // Reset game
                board = List(9) { Player.NONE }
                currentPlayer = Player.X
                gameResult = GameResult.ONGOING
                winningCells = emptyList()
                moveCount = 0
                showMustWinMessage = false
            } else {
                gameResult = result
                winningCells = cells
                delay(500)
                showResult = true
                if (result == GameResult.WIN) {
                    showConfetti = true
                }
            }
        }
    }

    // AI move
    LaunchedEffect(currentPlayer, gameResult) {
        if (currentPlayer == Player.O && gameResult == GameResult.ONGOING) {
            delay(600)
            val aiMove = getAIMove(board, difficulty)
            if (aiMove != -1) {
                board = board.toMutableList().also { it[aiMove] = Player.O }
                moveCount++
                currentPlayer = Player.X
            }
        }
    }

    GameBackground(style = GameBackgroundStyle.TIC_TAC_TOE) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Give Up button (top right, only for MEDIUM and HARD)
            if (onGiveUp != null && difficulty != GameDifficulty.EASY) {
                GiveUpButton(
                    currentDifficulty = difficulty,
                    onGiveUp = onGiveUp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Game icon
            Icon(
                imageVector = Icons.Default.Grid3x3,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .scale(iconScale),
                tint = TicTacToeAccent
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.game_tictactoe_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.game_tictactoe_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            // Must win message (Easy mode draw)
            AnimatedVisibility(
                visible = showMustWinMessage,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Card(
                    modifier = Modifier.padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Yellow.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.game_tictactoe_must_win),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Yellow,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Move counter
            AnimatedContent(
                targetState = moveCount,
                transitionSpec = {
                    slideInVertically { -it } + fadeIn() togetherWith
                            slideOutVertically { it } + fadeOut()
                },
                label = "moveCount"
            ) { count ->
                Text(
                    text = stringResource(R.string.game_moves, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Game board
            Card(
                modifier = Modifier
                    .aspectRatio(1f)
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = TicTacToeAccent.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (row in 0..2) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0..2) {
                                val index = row * 3 + col
                                val isWinningCell = index in winningCells
                                TicTacToeCell(
                                    player = board[index],
                                    isWinningCell = isWinningCell,
                                    isClickable = board[index] == Player.NONE &&
                                            currentPlayer == Player.X &&
                                            gameResult == GameResult.ONGOING,
                                    onClick = {
                                        if (board[index] == Player.NONE &&
                                            currentPlayer == Player.X &&
                                            gameResult == GameResult.ONGOING
                                        ) {
                                            board = board.toMutableList().also { it[index] = Player.X }
                                            moveCount++
                                            currentPlayer = Player.O
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Status text with animation
            AnimatedContent(
                targetState = Triple(gameResult, currentPlayer, showResult),
                transitionSpec = {
                    fadeIn(tween(300)) + scaleIn(initialScale = 0.9f) togetherWith
                            fadeOut(tween(200)) + scaleOut(targetScale = 0.9f)
                },
                label = "status"
            ) { (result, player, _) ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (result) {
                            GameResult.WIN -> Color.Green.copy(alpha = 0.2f)
                            GameResult.DRAW -> Color.Yellow.copy(alpha = 0.2f)
                            GameResult.LOSE -> Color.Red.copy(alpha = 0.2f)
                            else -> Color.White.copy(alpha = 0.1f)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when {
                            result == GameResult.WIN -> stringResource(R.string.game_tictactoe_you_win)
                            result == GameResult.DRAW -> stringResource(R.string.game_tictactoe_draw)
                            result == GameResult.LOSE -> stringResource(R.string.game_tictactoe_you_lose)
                            player == Player.X -> stringResource(R.string.game_tictactoe_your_turn)
                            else -> stringResource(R.string.game_tictactoe_ai_thinking)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (result) {
                            GameResult.WIN -> Color.Green
                            GameResult.DRAW -> Color.Yellow
                            GameResult.LOSE -> Color.Red
                            else -> Color.White
                        },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons with animation
            AnimatedVisibility(
                visible = showResult,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut()
            ) {
                if (gameResult == GameResult.WIN) {
                    // Auto dismiss or manual dismiss with delay?
                    // Original code: Button to dismiss.
                    // Request: "Game some games after finish loop back to play a bit before close"
                    // Interpretation: After winning, show confetti, maybe wait a bit, then allow close or auto close?
                    // The user req 4: "Game some games after play finish have loop back to play again a bit before game close"
                    // It likely means "Show result/confetti for a while then close".
                    // But here we have a button "Dismiss Alarm".
                    // Let's keep the button but maybe add delay if it was auto-triggering.
                    // Wait, current design has a BUTTON to dismiss.
                    // If user wants "loop back to play a bit", maybe they mean the animation continues?
                    // Let's look at `ConfettiAnimation` in `PuzzleSlideScreen`: `onAnimationEnd = onGameComplete`
                    // In `TicTacToeScreen`, it just shows button.
                    // Let's make it consistent: Win -> Show Confetti -> Wait -> Show Button OR Auto Close?
                    // User said: "play finish loop back to play again a bit before game close".
                    // This is slightly ambiguous. "Loop back to play again"? Maybe they mean "Stay on screen"?
                    // "Game some games after play finish have loop back to play again a bit before game close"
                    // Re-reading Thai: "เกมบางเกมพอเล่นจบแล้วมีวนกลับมาให้เล่นอีกแปบนึงก่อนที่เกมจะปิดไป"
                    // "Some games when finished have loop back to play again a moment before game closes"
                    // Sounds like: The game restarts or lingers?
                    // "before the game closes" implies it closes automatically?
                    // Let's assume they mean: "After winning, stay on result screen for a bit so I can see I won, before it closes/I allow close".
                    // In TicTacToe, we have a manual "Dismiss Alarm" button.
                    // Let's add the Auto-Close logic with delay if that matches expectation, OR just ensure the confetti plays.
                    // The confetti is already there.
                    // Let me apply the "Win Only" logic change first fully.
                    
                     Button(
                        onClick = {
                             // Add delay here? No, this is manual click.
                             // Maybe the user wants AUTO CLOSE with delay?
                             onGameComplete()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TicTacToeAccent
                        )
                    ) {
                        Text(stringResource(R.string.game_dismiss_alarm), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            board = List(9) { Player.NONE }
                            currentPlayer = Player.X
                            gameResult = GameResult.ONGOING
                            showResult = false
                            winningCells = emptyList()
                            moveCount = 0
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TicTacToeAccent
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.game_play_again), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Confetti animation on win
            if (showConfetti) {
                ConfettiAnimation(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
}

@Composable
private fun TicTacToeCell(
    player: Player,
    isWinningCell: Boolean,
    isClickable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (player != Player.NONE) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cellScale"
    )

    // Winning cell glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isWinningCell && player == Player.X -> TicTacToeAccent.copy(alpha = glowAlpha)
            isWinningCell && player == Player.O -> Color(0xFFFF6B6B).copy(alpha = glowAlpha)
            player == Player.X -> TicTacToeAccent.copy(alpha = 0.3f)
            player == Player.O -> Color(0xFFFF6B6B).copy(alpha = 0.3f)
            isClickable -> Color.White.copy(alpha = 0.15f)
            else -> Color.White.copy(alpha = 0.1f)
        },
        animationSpec = tween(300),
        label = "bgColor"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(6.dp)
            .then(
                if (isWinningCell) Modifier.shadow(8.dp, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(
                if (isWinningCell) Modifier.border(
                    2.dp,
                    if (player == Player.X) TicTacToeAccent else Color(0xFFFF6B6B),
                    RoundedCornerShape(12.dp)
                )
                else Modifier
            )
            .clickable(enabled = isClickable, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = player != Player.NONE,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Text(
                text = when (player) {
                    Player.X -> "X"
                    Player.O -> "O"
                    Player.NONE -> ""
                },
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = when (player) {
                    Player.X -> TicTacToeAccent
                    Player.O -> Color(0xFFFF6B6B)
                    Player.NONE -> Color.Transparent
                },
                modifier = Modifier.scale(scale)
            )
        }
    }
}

private fun checkGameResultWithWinningCells(board: List<Player>, humanPlayer: Player): Pair<GameResult, List<Int>> {
    val winPatterns = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // rows
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // columns
        listOf(0, 4, 8), listOf(2, 4, 6) // diagonals
    )

    for (pattern in winPatterns) {
        val (a, b, c) = pattern
        if (board[a] != Player.NONE && board[a] == board[b] && board[b] == board[c]) {
            val result = if (board[a] == humanPlayer) GameResult.WIN else GameResult.LOSE
            return Pair(result, pattern)
        }
    }

    return if (board.none { it == Player.NONE }) {
        Pair(GameResult.DRAW, emptyList())
    } else {
        Pair(GameResult.ONGOING, emptyList())
    }
}

private fun checkGameResult(board: List<Player>, humanPlayer: Player): GameResult {
    return checkGameResultWithWinningCells(board, humanPlayer).first
}

private fun getAIMove(board: List<Player>, difficulty: GameDifficulty): Int {
    val emptyIndices = board.indices.filter { board[it] == Player.NONE }
    if (emptyIndices.isEmpty()) return -1

    return when (difficulty) {
        GameDifficulty.EASY -> emptyIndices.random()
        GameDifficulty.MEDIUM -> {
            // 50% chance to make optimal move
            if (Random.nextBoolean()) {
                findBestMove(board) ?: emptyIndices.random()
            } else {
                emptyIndices.random()
            }
        }
        GameDifficulty.HARD -> findBestMove(board) ?: emptyIndices.random()
    }
}

private fun findBestMove(board: List<Player>): Int? {
    // Try to win
    for (i in board.indices) {
        if (board[i] == Player.NONE) {
            val testBoard = board.toMutableList().also { it[i] = Player.O }
            if (checkGameResult(testBoard, Player.X) == GameResult.LOSE) {
                return i
            }
        }
    }

    // Block player from winning
    for (i in board.indices) {
        if (board[i] == Player.NONE) {
            val testBoard = board.toMutableList().also { it[i] = Player.X }
            if (checkGameResult(testBoard, Player.X) == GameResult.WIN) {
                return i
            }
        }
    }

    // Take center if available
    if (board[4] == Player.NONE) return 4

    // Take corners
    val corners = listOf(0, 2, 6, 8).filter { board[it] == Player.NONE }
    if (corners.isNotEmpty()) return corners.random()

    // Take any available
    return board.indices.firstOrNull { board[it] == Player.NONE }
}
