package com.wakechallenge.presentation.games

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakechallenge.R
import com.wakechallenge.domain.model.GameDifficulty
import com.wakechallenge.presentation.components.ConfettiAnimation
import com.wakechallenge.presentation.components.GameBackground
import com.wakechallenge.presentation.components.GameBackgroundStyle
import com.wakechallenge.presentation.components.GiveUpButton
import com.wakechallenge.ui.theme.TypeGameAccent

@Composable
fun TypePhraseScreen(
    difficulty: GameDifficulty,
    onGameComplete: () -> Unit,
    onGiveUp: ((GameDifficulty) -> Unit)? = null
) {
    val phrases = when (difficulty) {
        GameDifficulty.EASY -> listOf(
            "Wake up now",
            "Good morning",
            "Rise and shine",
            "Time to wake up",
            "New day starts"
        )
        GameDifficulty.MEDIUM -> listOf(
            "Today is a great day!",
            "I am ready to start my day",
            "Good morning sunshine",
            "Time to be productive",
            "Rise and shine superstar"
        )
        GameDifficulty.HARD -> listOf(
            "Every morning brings new potential opportunities!",
            "Wake up with determination, go to bed with satisfaction",
            "The early bird catches the worm, so wake up now!",
            "Success is not final, failure is not fatal: it is the courage to continue",
            "Today I choose to be awake, alert, and ready for anything"
        )
    }

    val targetPhrase = remember { phrases.random() }
    var userInput by remember { mutableStateOf("") }
    var isCorrect by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Calculate match progress
    val matchLength = userInput.zip(targetPhrase).takeWhile { (a, b) ->
        a.equals(b, ignoreCase = true)
    }.size
    val progress = matchLength.toFloat() / targetPhrase.length

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

    // Progress color animation
    val progressColor by animateColorAsState(
        targetValue = when {
            progress >= 1f -> Color.Green
            progress >= 0.5f -> TypeGameAccent
            else -> TypeGameAccent.copy(alpha = 0.7f)
        },
        animationSpec = tween(300),
        label = "progressColor"
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(isCorrect) {
        if (isCorrect) {
            showConfetti = true
        }
    }

    GameBackground(style = GameBackgroundStyle.TYPE_PHRASE) {
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
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                imageVector = Icons.Default.Keyboard,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .scale(iconScale),
                tint = TypeGameAccent
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.game_phrase_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.game_phrase_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = progressColor,
                    trackColor = Color.White.copy(alpha = 0.2f),
                )
                Text(
                    text = stringResource(R.string.game_phrase_matched, (animatedProgress * 100).toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = progressColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Target phrase with better styling
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TypeGameAccent.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.game_phrase_target),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = targetPhrase,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Character comparison
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                targetPhrase.forEachIndexed { index, char ->
                    val userChar = userInput.getOrNull(index)
                    val isMatch = userChar?.equals(char, ignoreCase = true) == true
                    val isTyped = userChar != null

                    val charColor by animateColorAsState(
                        targetValue = when {
                            !isTyped -> Color.White.copy(alpha = 0.3f)
                            isMatch -> Color.Green
                            else -> Color.Red
                        },
                        animationSpec = tween(200),
                        label = "charColor$index"
                    )

                    val charScale by animateFloatAsState(
                        targetValue = if (isMatch) 1.2f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "charScale$index"
                    )

                    Text(
                        text = char.toString(),
                        color = charColor,
                        fontSize = 14.sp,
                        fontWeight = if (isMatch) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .scale(charScale)
                            .padding(horizontal = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // User input with better styling
            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                label = { Text(stringResource(R.string.game_phrase_type_here), color = Color.White.copy(alpha = 0.7f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (userInput.equals(targetPhrase, ignoreCase = true)) {
                            isCorrect = true
                        }
                    }
                ),
                isError = userInput.isNotEmpty() && !targetPhrase.lowercase().startsWith(userInput.lowercase()),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = TypeGameAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                    cursorColor = TypeGameAccent
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit button
            Button(
                onClick = {
                    if (userInput.equals(targetPhrase, ignoreCase = true)) {
                        isCorrect = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = userInput.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TypeGameAccent,
                    disabledContainerColor = TypeGameAccent.copy(alpha = 0.3f)
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.game_submit), fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
