package com.wakechallenge.presentation.games

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakechallenge.R
import kotlinx.coroutines.delay
import com.wakechallenge.domain.model.GameDifficulty
import com.wakechallenge.presentation.components.ConfettiAnimation
import com.wakechallenge.presentation.components.GameBackground
import com.wakechallenge.presentation.components.GameBackgroundStyle
import com.wakechallenge.presentation.components.GiveUpButton
import com.wakechallenge.ui.theme.PuzzleGameAccent

@Composable
fun PuzzleSlideScreen(
    difficulty: GameDifficulty,
    onGameComplete: () -> Unit,
    onGiveUp: ((GameDifficulty) -> Unit)? = null
) {
    val gridSize = when (difficulty) {
        GameDifficulty.EASY -> 2    // 2x2 = 3 tiles (easy)
        GameDifficulty.MEDIUM -> 3  // 3x3 = 8 tiles (medium)
        GameDifficulty.HARD -> 4    // 4x4 = 15 tiles (hard)
    }

    val totalTiles = gridSize * gridSize
    val tiles = remember {
        val initial = (1 until totalTiles).toMutableList<Int?>()
        initial.add(null) // Empty tile
        // Shuffle (ensure solvable)
        do {
            initial.shuffle()
        } while (!isSolvable(initial, gridSize))
        initial.toMutableStateList()
    }

    var moveCount by remember { mutableIntStateOf(0) }
    var isSolved by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }

    // Check if solved
    LaunchedEffect(tiles.toList()) {
        val target = (1 until totalTiles).toList() + listOf(null)
        if (tiles.toList() == target) {
            isSolved = true
            showConfetti = true
        }
    }

    // Calculate correct tiles count
    val correctTilesCount = tiles.mapIndexed { index, tile ->
        if (tile == index + 1) 1 else 0
    }.sum()
    val progress = correctTilesCount.toFloat() / (totalTiles - 1)

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

    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300, easing = EaseOutCubic),
        label = "progress"
    )

    GameBackground(style = GameBackgroundStyle.PUZZLE_SLIDE) {
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
                imageVector = Icons.Default.Extension,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .scale(iconScale),
                tint = PuzzleGameAccent
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.game_puzzle_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.game_puzzle_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress and moves
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Moves counter
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = PuzzleGameAccent.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.game_puzzle_moves),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        AnimatedContent(
                            targetState = moveCount,
                            transitionSpec = {
                                slideInVertically { -it } + fadeIn() togetherWith
                                        slideOutVertically { it } + fadeOut()
                            },
                            label = "moveCount"
                        ) { count ->
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = PuzzleGameAccent
                            )
                        }
                    }
                }

                // Progress
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.game_puzzle_correct),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "$correctTilesCount/${totalTiles - 1}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF4CAF50),
                trackColor = Color.White.copy(alpha = 0.2f),
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Puzzle grid
            Card(
                modifier = Modifier
                    .aspectRatio(1f)
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = PuzzleGameAccent.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (row in 0 until gridSize) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0 until gridSize) {
                                val index = row * gridSize + col
                                val tile = tiles[index]
                                val canMoveThis = tile != null && canMove(index, tiles.indexOf(null), gridSize)

                                AnimatedPuzzleTile(
                                    number = tile,
                                    isCorrectPosition = tile == index + 1,
                                    canMove = canMoveThis,
                                    onClick = {
                                        if (canMoveThis) {
                                            val emptyIndex = tiles.indexOf(null)
                                            tiles[emptyIndex] = tile
                                            tiles[index] = null
                                            moveCount++
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Success message
            AnimatedVisibility(
                visible = isSolved,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.padding(top = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Green.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.game_puzzle_solved, moveCount),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Green,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
        }

            // Confetti animation on win
            if (showConfetti) {
                ConfettiAnimation(
                    modifier = Modifier.fillMaxSize(),
                    onAnimationEnd = {} // We handle completion via LaunchedEffect below
                )

                // Use LaunchedEffect to handle the delay and completion
                LaunchedEffect(Unit) {
                    delay(2000) // Wait for confetti
                    onGameComplete()
                }
            }
        }
    }
}

@Composable
private fun AnimatedPuzzleTile(
    number: Int?,
    isCorrectPosition: Boolean,
    canMove: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Scale animation for clickable tiles
    val scale by animateFloatAsState(
        targetValue = if (canMove) 1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tileScale"
    )

    // Glow animation for correct position
    val infiniteTransition = rememberInfiniteTransition(label = "correctGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            number == null -> Color.Transparent
            isCorrectPosition -> Color(0xFF4CAF50)
            canMove -> PuzzleGameAccent.copy(alpha = 0.9f)
            else -> PuzzleGameAccent.copy(alpha = 0.7f)
        },
        animationSpec = tween(300),
        label = "bgColor"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .scale(scale)
            .then(
                if (number != null && isCorrectPosition) {
                    Modifier.shadow(6.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFF4CAF50))
                } else if (number != null) {
                    Modifier.shadow(4.dp, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (number != null) {
                    Brush.linearGradient(
                        colors = if (isCorrectPosition) {
                            listOf(
                                Color(0xFF4CAF50).copy(alpha = glowAlpha),
                                Color(0xFF45B649).copy(alpha = glowAlpha)
                            )
                        } else {
                            listOf(backgroundColor, backgroundColor.copy(alpha = 0.8f))
                        }
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                }
            )
            .then(
                if (isCorrectPosition && number != null) {
                    Modifier.border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(12.dp))
                } else if (canMove && number != null) {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .clickable(enabled = canMove, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = number != null,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Text(
                text = number?.toString() ?: "",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

private fun canMove(tileIndex: Int, emptyIndex: Int, gridSize: Int): Boolean {
    val tileRow = tileIndex / gridSize
    val tileCol = tileIndex % gridSize
    val emptyRow = emptyIndex / gridSize
    val emptyCol = emptyIndex % gridSize

    return (tileRow == emptyRow && kotlin.math.abs(tileCol - emptyCol) == 1) ||
           (tileCol == emptyCol && kotlin.math.abs(tileRow - emptyRow) == 1)
}

private fun isSolvable(tiles: List<Int?>, gridSize: Int): Boolean {
    var inversions = 0
    val flatTiles = tiles.filterNotNull()

    for (i in flatTiles.indices) {
        for (j in i + 1 until flatTiles.size) {
            if (flatTiles[i] > flatTiles[j]) {
                inversions++
            }
        }
    }

    val emptyRow = tiles.indexOf(null) / gridSize

    return if (gridSize % 2 == 1) {
        inversions % 2 == 0
    } else {
        (inversions + emptyRow) % 2 == 1
    }
}
