package com.wakechallenge.domain.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wakechallenge.R

enum class GameType(
    @param:StringRes val displayNameRes: Int,
    @param:StringRes val descriptionRes: Int,
    val iconName: String,
    // Keep for backwards compatibility - used in non-Composable contexts
    val displayName: String,
    val description: String
) {
    MATH(
        displayNameRes = R.string.game_math_title,
        descriptionRes = R.string.game_math_desc,
        iconName = "calculate",
        displayName = "Math Challenge",
        description = "Solve math problems to dismiss"
    ),
    TIC_TAC_TOE(
        displayNameRes = R.string.game_tictactoe_title,
        descriptionRes = R.string.game_tictactoe_desc,
        iconName = "grid_3x3",
        displayName = "Tic-Tac-Toe",
        description = "Win or draw against AI"
    ),
    MEMORY_MATCH(
        displayNameRes = R.string.game_memory_title,
        descriptionRes = R.string.game_memory_desc,
        iconName = "flip",
        displayName = "Memory Match",
        description = "Find all matching pairs"
    ),
    TYPE_PHRASE(
        displayNameRes = R.string.game_phrase_title,
        descriptionRes = R.string.game_phrase_desc,
        iconName = "keyboard",
        displayName = "Type the Phrase",
        description = "Type the displayed phrase correctly"
    ),
    PUZZLE_SLIDE(
        displayNameRes = R.string.game_puzzle_title,
        descriptionRes = R.string.game_puzzle_desc,
        iconName = "extension",
        displayName = "Puzzle Slide",
        description = "Solve the sliding puzzle"
    ),
    COLOR_MATCH(
        displayNameRes = R.string.game_color_title,
        descriptionRes = R.string.game_color_desc,
        iconName = "palette",
        displayName = "Color Match",
        description = "Match the colors correctly"
    )
}

// Extension functions for Composable context
@Composable
fun GameType.localizedDisplayName(): String = stringResource(displayNameRes)

@Composable
fun GameType.localizedDescription(): String = stringResource(descriptionRes)

enum class GameDifficulty(
    @param:StringRes val displayNameRes: Int,
    // Keep for backwards compatibility
    val displayName: String
) {
    EASY(R.string.difficulty_easy, "Easy"),
    MEDIUM(R.string.difficulty_medium, "Medium"),
    HARD(R.string.difficulty_hard, "Hard")
}

// Extension function for Composable context
@Composable
fun GameDifficulty.localizedDisplayName(): String = stringResource(displayNameRes)
