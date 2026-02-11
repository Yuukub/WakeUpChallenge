package com.wakechallenge.presentation.games

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
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
import com.wakechallenge.R
import kotlinx.coroutines.delay
import com.wakechallenge.domain.model.GameDifficulty
import com.wakechallenge.presentation.components.ConfettiAnimation
import com.wakechallenge.presentation.components.GameBackground
import com.wakechallenge.presentation.components.GameBackgroundStyle
import com.wakechallenge.presentation.components.GiveUpButton
import com.wakechallenge.ui.theme.ColorMatchAccent
import kotlinx.coroutines.delay

data class ColorOption(
    val color: Color,
    val name: String
)

@Composable
fun ColorMatchScreen(
    difficulty: GameDifficulty,
    onGameComplete: () -> Unit,
    onGiveUp: ((GameDifficulty) -> Unit)? = null
) {
    val requiredMatches = when (difficulty) {
        GameDifficulty.EASY -> 5
        GameDifficulty.MEDIUM -> 8
        GameDifficulty.HARD -> 12
    }

    val allColors = listOf(
        ColorOption(Color(0xFFE53935), "Red"),
        ColorOption(Color(0xFF1E88E5), "Blue"),
        ColorOption(Color(0xFF43A047), "Green"),
        ColorOption(Color(0xFFFFB300), "Yellow"),
        ColorOption(Color(0xFF8E24AA), "Purple"),
        ColorOption(Color(0xFF00ACC1), "Cyan"),
        ColorOption(Color(0xFFFF7043), "Orange"),
        ColorOption(Color(0xFFEC407A), "Pink")
    )

    var targetColor by remember { mutableStateOf(allColors.random()) }
    var options by remember { mutableStateOf(generateOptions(targetColor, allColors)) }
    var matchCount by remember { mutableIntStateOf(0) }
    var showConfetti by remember { mutableStateOf(false) }

    // Track which button was clicked and whether it was correct
    var clickedIndex by remember { mutableStateOf<Int?>(null) }
    var wasCorrectClick by remember { mutableStateOf<Boolean?>(null) }
    var lastWasCorrect by remember { mutableStateOf(true) } // To prevent flickering on exit

    LaunchedEffect(matchCount) {
        if (matchCount >= requiredMatches) {
            delay(500)
            showConfetti = true
        }
    }

    // Shake animation for wrong answer
    val shakeOffset = remember { Animatable(0f) }

    // Handle feedback and reset after delay
    LaunchedEffect(wasCorrectClick) {
        if (wasCorrectClick != null) {
            lastWasCorrect = wasCorrectClick == true
        }

        if (wasCorrectClick == false) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 300
                    0f at 0
                    -15f at 50
                    15f at 100
                    -10f at 150
                    10f at 200
                    0f at 300
                }
            )
        }
        if (wasCorrectClick != null) {
            delay(500)
            // Reset state and generate new question if correct AND game not finished
            if (wasCorrectClick == true) {
                if (matchCount < requiredMatches) {
                    targetColor = allColors.random()
                    options = generateOptions(targetColor, allColors)
                }
            }
            clickedIndex = null
            wasCorrectClick = null
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

    // Target color pulse
    val targetPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "targetPulse"
    )

    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = matchCount.toFloat() / requiredMatches,
        animationSpec = tween(500, easing = EaseOutCubic),
        label = "progress"
    )

    GameBackground(style = GameBackgroundStyle.COLOR_MATCH) {
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
                .padding(24.dp)
                .graphicsLayer { translationX = shakeOffset.value },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Game icon
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .scale(iconScale),
                tint = ColorMatchAccent
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.game_color_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.game_color_instruction),
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
                color = ColorMatchAccent,
                trackColor = Color.White.copy(alpha = 0.2f)
            )

            // Animated counter dots
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(requiredMatches) { index ->
                    val isCompleted = index < matchCount
                    val scale by animateFloatAsState(
                        targetValue = if (isCompleted) 1f else 0.6f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "dot"
                    )
                    val color by animateColorAsState(
                        targetValue = if (isCompleted) ColorMatchAccent else Color.White.copy(alpha = 0.3f),
                        animationSpec = tween(300),
                        label = "dotColor"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(if (requiredMatches > 10) 8.dp else 10.dp)
                            .scale(scale)
                            .clip(RoundedCornerShape(50))
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Target color display with pulse animation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.game_color_find),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(targetPulse)
                        .shadow(12.dp, CircleShape, spotColor = targetColor.color)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    targetColor.color,
                                    targetColor.color.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .border(4.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = targetColor.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

            Spacer(modifier = Modifier.height(28.dp))

            // Color options with better layout
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                options.chunked(2).forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEachIndexed { colIndex, colorOption ->
                            val optionIndex = rowIndex * 2 + colIndex
                            AnimatedColorButton(
                                color = colorOption.color,
                                isCorrect = if (clickedIndex == optionIndex) wasCorrectClick else null,
                                onClick = {
                                    if (clickedIndex == null) { // Prevent multiple clicks
                                        clickedIndex = optionIndex
                                        if (colorOption == targetColor) {
                                            matchCount++
                                            wasCorrectClick = true
                                        } else {
                                            wasCorrectClick = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Animated Feedback
            AnimatedVisibility(
                visible = wasCorrectClick != null,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut()
            ) {
                Card(
                    modifier = Modifier.padding(top = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (lastWasCorrect)
                            Color.Green.copy(alpha = 0.2f)
                        else
                            Color.Red.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (lastWasCorrect) stringResource(R.string.game_correct) else stringResource(R.string.game_try_again),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (lastWasCorrect) Color.Green else Color.Red,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
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

@Composable
private fun AnimatedColorButton(
    color: Color,
    isCorrect: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = when (isCorrect) {
            true -> 1.15f
            false -> 0.95f
            null -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val borderColor by animateColorAsState(
        targetValue = when (isCorrect) {
            true -> Color.Green
            false -> Color.Red
            null -> Color.White.copy(alpha = 0.2f)
        },
        animationSpec = tween(300),
        label = "borderColor"
    )

    val borderWidth by animateFloatAsState(
        targetValue = when (isCorrect) {
            true -> 4f
            false -> 4f
            null -> 2f
        },
        animationSpec = tween(200),
        label = "borderWidth"
    )

    Box(
        modifier = modifier
            .aspectRatio(2f)
            .scale(scale)
            .then(
                if (isCorrect == true) {
                    Modifier.shadow(10.dp, RoundedCornerShape(16.dp), spotColor = Color.Green)
                } else {
                    Modifier.shadow(6.dp, RoundedCornerShape(16.dp))
                }
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(color, color.copy(alpha = 0.85f))
                )
            )
            .border(borderWidth.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Show check or X icon when feedback is given
        AnimatedVisibility(
            visible = isCorrect != null,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCorrect == true) Color.Green.copy(alpha = 0.8f)
                        else Color.Red.copy(alpha = 0.8f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCorrect == true) "✓" else "✗",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun generateOptions(target: ColorOption, allColors: List<ColorOption>): List<ColorOption> {
    val others = allColors.filter { it != target }.shuffled().take(3)
    return (others + target).shuffled()
}
