package com.wakechallenge.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wakechallenge.R
import com.wakechallenge.domain.model.GameDifficulty
import com.wakechallenge.domain.model.localizedDisplayName

@Composable
fun GiveUpButton(
    currentDifficulty: GameDifficulty,
    onGiveUp: (GameDifficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    // Only show for MEDIUM and HARD
    if (currentDifficulty == GameDifficulty.EASY) return

    var showDialog by remember { mutableStateOf(false) }

    val nextDifficulty = when (currentDifficulty) {
        GameDifficulty.HARD -> GameDifficulty.MEDIUM
        GameDifficulty.MEDIUM -> GameDifficulty.EASY
        GameDifficulty.EASY -> return
    }

    // Give Up button
    TextButton(
        onClick = { showDialog = true },
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(
            contentColor = Color.White.copy(alpha = 0.7f)
        )
    ) {
        Icon(
            imageVector = Icons.Default.Flag,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = stringResource(R.string.game_give_up))
    }

    // Confirmation dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(text = stringResource(R.string.game_give_up))
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.game_give_up_confirm,
                        nextDifficulty.localizedDisplayName()
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onGiveUp(nextDifficulty)
                    }
                ) {
                    Text(text = stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = stringResource(R.string.no))
                }
            }
        )
    }
}
