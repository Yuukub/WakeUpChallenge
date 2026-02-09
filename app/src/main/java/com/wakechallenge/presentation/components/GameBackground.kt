package com.wakechallenge.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.wakechallenge.ui.theme.*

enum class GameBackgroundStyle {
    MATH,
    MEMORY,
    TIC_TAC_TOE,
    TYPE_PHRASE,
    PUZZLE_SLIDE,
    COLOR_MATCH,
    DEFAULT
}

@Composable
fun GameBackground(
    style: GameBackgroundStyle,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val gradientColors = when (style) {
        GameBackgroundStyle.MATH -> listOf(
            Color(0xFF1A1A2E),
            Color(0xFF16213E),
            MathGameAccent.copy(alpha = 0.3f)
        )
        GameBackgroundStyle.MEMORY -> listOf(
            Color(0xFF1A1A2E),
            Color(0xFF2D1B4E),
            MemoryGameAccent.copy(alpha = 0.2f)
        )
        GameBackgroundStyle.TIC_TAC_TOE -> listOf(
            Color(0xFF0D1B2A),
            Color(0xFF1B3A4B),
            TicTacToeAccent.copy(alpha = 0.2f)
        )
        GameBackgroundStyle.TYPE_PHRASE -> listOf(
            Color(0xFF1A1A2E),
            Color(0xFF2D1B4E),
            TypeGameAccent.copy(alpha = 0.2f)
        )
        GameBackgroundStyle.PUZZLE_SLIDE -> listOf(
            Color(0xFF1A1A2E),
            Color(0xFF2E1B3D),
            PuzzleGameAccent.copy(alpha = 0.2f)
        )
        GameBackgroundStyle.COLOR_MATCH -> listOf(
            Color(0xFF1A1A2E),
            Color(0xFF2E1F0F),
            ColorMatchAccent.copy(alpha = 0.2f)
        )
        GameBackgroundStyle.DEFAULT -> GradientDawn
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(colors = gradientColors)
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        content = content
    )
}

@Composable
fun AnimatedGameBackground(
    style: GameBackgroundStyle,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // For now, use the same as GameBackground
    // Can be extended with shimmer or pulse animations
    GameBackground(
        style = style,
        modifier = modifier,
        content = content
    )
}
