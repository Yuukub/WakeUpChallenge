package com.wakechallenge.presentation.games

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.wakechallenge.ui.theme.MemoryGameAccent
import kotlinx.coroutines.delay

data class MemoryCard(
    val id: Int,
    val symbol: String,
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false
)

@Composable
fun MemoryMatchScreen(
    difficulty: GameDifficulty,
    onGameComplete: () -> Unit,
    onGiveUp: ((GameDifficulty) -> Unit)? = null
) {
    // Separate columns and numPairs for proper difficulty scaling
    val (columns, numPairs) = when (difficulty) {
        GameDifficulty.EASY -> 3 to 3      // 6 cards, 3 pairs (very easy)
        GameDifficulty.MEDIUM -> 4 to 8    // 16 cards, 8 pairs (medium)
        GameDifficulty.HARD -> 6 to 18     // 36 cards, 18 pairs (hard)
    }

    val symbols = listOf(
        "🌟", "🎯", "🎨", "🎭", "🎪", "🎬",
        "🎸", "🎹", "🎺", "🎻", "🎼", "🎵",
        "🌈", "🌸", "🌺", "🍀", "🍁", "🍄"
    )

    val cards = remember(numPairs) {
        val selectedSymbols = symbols.shuffled().take(numPairs)
        (selectedSymbols + selectedSymbols)
            .shuffled()
            .mapIndexed { index, symbol ->
                MemoryCard(id = index, symbol = symbol)
            }
            .toMutableStateList()
    }

    var firstSelected by remember { mutableStateOf<Int?>(null) }
    var secondSelected by remember { mutableStateOf<Int?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var matchedPairs by remember { mutableIntStateOf(0) }
    var showConfetti by remember { mutableStateOf(false) }
    val totalPairs = numPairs

    // Check for match
    LaunchedEffect(secondSelected) {
        if (firstSelected != null && secondSelected != null) {
            isChecking = true
            delay(800)

            val first = firstSelected ?: return@LaunchedEffect
            val second = secondSelected ?: return@LaunchedEffect

            if (cards[first].symbol == cards[second].symbol) {
                cards[first] = cards[first].copy(isMatched = true)
                cards[second] = cards[second].copy(isMatched = true)
                matchedPairs++
            } else {
                cards[first] = cards[first].copy(isFlipped = false)
                cards[second] = cards[second].copy(isFlipped = false)
            }

            firstSelected = null
            secondSelected = null
            isChecking = false
        }
    }

    // Check for game complete
    LaunchedEffect(matchedPairs) {
        if (matchedPairs == totalPairs) {
            delay(500)
            showConfetti = true
        }
    }

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
        targetValue = matchedPairs.toFloat() / totalPairs,
        animationSpec = tween(500, easing = EaseOutCubic),
        label = "progress"
    )

    GameBackground(style = GameBackgroundStyle.MEMORY) {
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Game icon
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .scale(iconScale),
                tint = MemoryGameAccent
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.game_memory_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.game_memory_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = MemoryGameAccent,
                trackColor = Color.White.copy(alpha = 0.2f),
            )

            // Animated pair counter
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalPairs) { index ->
                    val isMatched = index < matchedPairs
                    val scale by animateFloatAsState(
                        targetValue = if (isMatched) 1f else 0.6f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "pairDot"
                    )
                    val color by animateColorAsState(
                        targetValue = if (isMatched) MemoryGameAccent else Color.White.copy(alpha = 0.3f),
                        animationSpec = tween(300),
                        label = "dotColor"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(if (totalPairs > 12) 8.dp else 10.dp)
                            .scale(scale)
                            .clip(RoundedCornerShape(50))
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(cards, key = { it.id }) { card ->
                    FlipCard(
                        card = card,
                        isClickable = !isChecking && !card.isMatched && !card.isFlipped,
                        onClick = {
                            if (!isChecking && !card.isMatched && !card.isFlipped) {
                                cards[card.id] = card.copy(isFlipped = true)
                                when {
                                    firstSelected == null -> firstSelected = card.id
                                    secondSelected == null -> secondSelected = card.id
                                }
                            }
                        }
                    )
                }
            }

            // Confetti animation on win
            if (showConfetti) {
                ConfettiAnimation(
                    modifier = Modifier.fillMaxSize(),
                    onAnimationEnd = {}
                )
                LaunchedEffect(Unit) {
                    delay(2000)
                    onGameComplete()
                }
            }
        }
    }
}
}

@Composable
private fun FlipCard(
    card: MemoryCard,
    isClickable: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFlipped || card.isMatched) 180f else 0f,
        animationSpec = tween(400, easing = EaseInOutCubic),
        label = "cardRotation"
    )

    // Match celebration animation
    val matchScale by animateFloatAsState(
        targetValue = if (card.isMatched) 1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "matchScale"
    )

    // Glow animation for matched cards
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(matchScale)
            .then(
                if (card.isMatched) Modifier.shadow(6.dp, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clip(RoundedCornerShape(12.dp))
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(enabled = isClickable, onClick = onClick)
            .background(
                when {
                    card.isMatched -> Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF4CAF50).copy(alpha = glowAlpha),
                            Color(0xFF81C784).copy(alpha = glowAlpha)
                        )
                    )
                    rotation > 90f -> Brush.linearGradient(
                        colors = listOf(
                            MemoryGameAccent.copy(alpha = 0.3f),
                            MemoryGameAccent.copy(alpha = 0.4f)
                        )
                    )
                    else -> Brush.linearGradient(
                        colors = listOf(
                            MemoryGameAccent,
                            MemoryGameAccent.copy(alpha = 0.8f)
                        )
                    )
                }
            )
            .then(
                if (card.isMatched) Modifier.border(
                    2.dp,
                    Color(0xFF4CAF50),
                    RoundedCornerShape(12.dp)
                )
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (rotation > 90f) {
            Text(
                text = card.symbol,
                fontSize = 32.sp,
                modifier = Modifier.graphicsLayer { rotationY = 180f }
            )
        } else {
            // Card back design
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
