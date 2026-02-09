package com.wakechallenge.presentation.games

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakechallenge.R
import com.wakechallenge.domain.model.GameDifficulty
import com.wakechallenge.presentation.components.ConfettiAnimation
import com.wakechallenge.presentation.components.GameBackground
import com.wakechallenge.presentation.components.GameBackgroundStyle
import com.wakechallenge.presentation.components.GiveUpButton
import com.wakechallenge.ui.theme.MathGameAccent
import kotlin.random.Random

data class MathProblem(
    val num1: Int,
    val num2: Int,
    val operator: Char,
    val answer: Int
)

@Composable
fun MathGameScreen(
    difficulty: GameDifficulty,
    requiredCorrect: Int = 3,
    onGameComplete: () -> Unit,
    onGiveUp: ((GameDifficulty) -> Unit)? = null
) {
    var currentProblem by remember { mutableStateOf(generateProblem(difficulty)) }
    var answerChoices by remember { mutableStateOf(generateChoices(currentProblem.answer)) }
    var correctCount by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var isAnswerCorrect by remember { mutableStateOf<Boolean?>(null) }
    var showConfetti by remember { mutableStateOf(false) }
    var problemKey by remember { mutableIntStateOf(0) }

    // Shake animation for wrong answers
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(isAnswerCorrect) {
        if (isAnswerCorrect == false) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -20f at 50
                    20f at 100
                    -15f at 150
                    15f at 200
                    -10f at 250
                    10f at 300
                    0f at 400
                }
            )
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

    // Progress animation
    val animatedProgress by animateFloatAsState(
        targetValue = correctCount.toFloat() / requiredCorrect,
        animationSpec = tween(500, easing = EaseOutCubic),
        label = "progress"
    )

    // Handle answer feedback and generate new question
    LaunchedEffect(isAnswerCorrect) {
        if (isAnswerCorrect != null) {
            kotlinx.coroutines.delay(800)
            if (isAnswerCorrect == true) {
                val newProblem = generateProblem(difficulty)
                currentProblem = newProblem
                answerChoices = generateChoices(newProblem.answer)
                problemKey++
            }
            selectedAnswer = null
            isAnswerCorrect = null
        }
    }

    LaunchedEffect(correctCount) {
        if (correctCount >= requiredCorrect) {
            showConfetti = true
        }
    }

    GameBackground(style = GameBackgroundStyle.MATH) {
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
                // Math icon with pulse animation
            Icon(
                imageVector = Icons.Default.Calculate,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .scale(iconScale),
                tint = MathGameAccent
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress indicator
            Text(
                text = stringResource(R.string.game_math_instruction, requiredCorrect),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Animated Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = MathGameAccent,
                trackColor = Color.White.copy(alpha = 0.2f),
            )

            // Animated counter
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(requiredCorrect) { index ->
                    val isCompleted = index < correctCount
                    val scale by animateFloatAsState(
                        targetValue = if (isCompleted) 1f else 0.7f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "dot"
                    )
                    val color by animateColorAsState(
                        targetValue = if (isCompleted) MathGameAccent else Color.White.copy(alpha = 0.3f),
                        animationSpec = tween(300),
                        label = "dotColor"
                    )
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(12.dp)
                            .scale(scale),
                        shape = RoundedCornerShape(50),
                        color = color
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Math problem display with animation
            AnimatedContent(
                targetState = problemKey,
                transitionSpec = {
                    (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                            (slideOutHorizontally { width -> -width } + fadeOut())
                },
                label = "problem"
            ) { _ ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = shakeOffset.value },
                    colors = CardDefaults.cardColors(
                        containerColor = MathGameAccent.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${currentProblem.num1} ${currentProblem.operator} ${currentProblem.num2} = ?",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Answer choices - 2x2 grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shakeOffset.value },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(answerChoices) { choice ->
                    val isSelected = selectedAnswer == choice
                    val isCorrectChoice = choice == currentProblem.answer
                    val buttonColor by animateColorAsState(
                        targetValue = when {
                            isSelected && isAnswerCorrect == true -> Color.Green
                            isSelected && isAnswerCorrect == false -> Color.Red
                            else -> MathGameAccent
                        },
                        animationSpec = tween(300),
                        label = "buttonColor"
                    )
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isSelected) 0.95f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "buttonScale"
                    )

                    Button(
                        onClick = {
                            if (selectedAnswer == null) {
                                selectedAnswer = choice
                                if (choice == currentProblem.answer) {
                                    isAnswerCorrect = true
                                    correctCount++
                                } else {
                                    isAnswerCorrect = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .scale(buttonScale),
                        enabled = selectedAnswer == null,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor,
                            disabledContainerColor = buttonColor
                        )
                    ) {
                        Text(
                            text = "$choice",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Feedback message
            AnimatedVisibility(
                visible = isAnswerCorrect != null,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut()
            ) {
                Card(
                    modifier = Modifier.padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAnswerCorrect == true)
                            Color.Green.copy(alpha = 0.2f)
                        else
                            Color.Red.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isAnswerCorrect == true)
                            stringResource(R.string.game_correct)
                        else
                            stringResource(R.string.game_try_again),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isAnswerCorrect == true) Color.Green else Color.Red,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }

            // Confetti animation on win
            if (showConfetti) {
                ConfettiAnimation(
                    modifier = Modifier.fillMaxSize(),
                    onAnimationEnd = onGameComplete
                )
            }
        }
    }
}

private fun generateProblem(difficulty: GameDifficulty): MathProblem {
    val (minNum, maxNum) = when (difficulty) {
        GameDifficulty.EASY -> 1 to 10
        GameDifficulty.MEDIUM -> 10 to 50
        GameDifficulty.HARD -> 50 to 100
    }

    val operators = when (difficulty) {
        GameDifficulty.EASY -> listOf('+', '-')
        GameDifficulty.MEDIUM -> listOf('+', '-', '*')
        GameDifficulty.HARD -> listOf('+', '-', '*', '/')
    }

    val operator = operators.random()
    var num1 = Random.nextInt(minNum, maxNum + 1)
    var num2 = Random.nextInt(minNum, maxNum + 1)

    // Ensure division results in whole number
    if (operator == '/') {
        num2 = Random.nextInt(1, 10)
        num1 = num2 * Random.nextInt(1, 10)
    }

    // Ensure subtraction doesn't result in negative for easy mode
    if (operator == '-' && difficulty == GameDifficulty.EASY && num2 > num1) {
        val temp = num1
        num1 = num2
        num2 = temp
    }

    val answer = when (operator) {
        '+' -> num1 + num2
        '-' -> num1 - num2
        '*' -> num1 * num2
        '/' -> num1 / num2
        else -> 0
    }

    return MathProblem(num1, num2, operator, answer)
}

private fun generateChoices(correctAnswer: Int): List<Int> {
    val choices = mutableSetOf(correctAnswer)
    val range = when {
        correctAnswer == 0 -> 1..5
        correctAnswer > 0 -> maxOf(1, correctAnswer / 3)..maxOf(5, correctAnswer / 2)
        else -> maxOf(1, -correctAnswer / 3)..maxOf(5, -correctAnswer / 2)
    }

    while (choices.size < 4) {
        val offset = range.random() * (if (Random.nextBoolean()) 1 else -1)
        val wrongAnswer = correctAnswer + offset
        if (wrongAnswer != correctAnswer) {
            choices.add(wrongAnswer)
        }
    }

    return choices.shuffled()
}
