package com.wakechallenge.presentation.addalarm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wakechallenge.R
import com.wakechallenge.domain.model.GameDifficulty
import com.wakechallenge.domain.model.GameType
import com.wakechallenge.domain.model.localizedDisplayName
import com.wakechallenge.domain.model.localizedDescription
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmScreen(
    alarmId: Long?,
    viewModel: AddAlarmViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onPracticeGame: (GameType) -> Unit,
    onSelectSound: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(alarmId) {
        if (alarmId != null) {
            viewModel.loadAlarm(alarmId)
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (alarmId == null) stringResource(R.string.add_alarm_new) else stringResource(R.string.add_alarm_edit))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.saveAlarm() }) {
                        Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Time Picker
            TimePickerSection(
                time = uiState.time,
                onTimeChange = { viewModel.updateTime(it) }
            )

            // Label
            OutlinedTextField(
                value = uiState.label,
                onValueChange = { viewModel.updateLabel(it) },
                label = { Text(stringResource(R.string.add_alarm_label)) },
                placeholder = { Text(stringResource(R.string.add_alarm_label_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Repeat Days
            RepeatDaysSection(
                selectedDays = uiState.repeatDays,
                onDayToggle = { viewModel.toggleRepeatDay(it) }
            )

            // Games Selection
            GamesSection(
                selectedGames = uiState.selectedGames,
                onGameToggle = { viewModel.toggleGame(it) },
                onPracticeGame = onPracticeGame
            )

            // Game Difficulty
            DifficultySection(
                difficulty = uiState.gameDifficulty,
                onDifficultyChange = { viewModel.updateDifficulty(it) }
            )

            // Snooze Settings
            SnoozeSection(
                snoozeEnabled = uiState.snoozeEnabled,
                snoozeDuration = uiState.snoozeDurationMinutes,
                maxSnoozeCount = uiState.maxSnoozeCount,
                onSnoozeEnabledChange = { viewModel.updateSnoozeEnabled(it) },
                onSnoozeDurationChange = { viewModel.updateSnoozeDuration(it) },
                onMaxSnoozeCountChange = { viewModel.updateMaxSnoozeCount(it) }
            )

            // Gradual Volume
            GradualVolumeSection(
                enabled = uiState.gradualVolumeEnabled,
                duration = uiState.gradualVolumeDurationSeconds,
                onEnabledChange = { viewModel.updateGradualVolumeEnabled(it) },
                onDurationChange = { viewModel.updateGradualVolumeDuration(it) }
            )

            // Vibration
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.add_alarm_vibration), style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = uiState.isVibrationEnabled,
                    onCheckedChange = { viewModel.updateVibrationEnabled(it) }
                )
            }

            // Sound Selection
            SoundSection(
                soundUri = uiState.soundUri,
                onSelectSound = onSelectSound
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimePickerSection(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit
) {
    val itemHeight = 56.dp
    val visibleItems = 5
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }

    val hoursCount = (Int.MAX_VALUE / 24) * 24
    val minutesCount = (Int.MAX_VALUE / 60) * 60

    // Ensure center is exactly 00:00 or 00 minutes
    val hourCenter = (hoursCount / 2 / 24) * 24
    val minuteCenter = (minutesCount / 2 / 60) * 60

    val initialHourIndex = remember { hourCenter + time.hour }
    val initialMinuteIndex = remember { minuteCenter + time.minute }

    val hourListState = rememberLazyListState(initialFirstVisibleItemIndex = initialHourIndex)
    val minuteListState = rememberLazyListState(initialFirstVisibleItemIndex = initialMinuteIndex)
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // Scroll to correct position when time changes (e.g., when loading existing alarm)
    LaunchedEffect(time) {
        val currentHour = hourListState.firstVisibleItemIndex % 24
        if (currentHour != time.hour) {
            hourListState.scrollToItem(hourCenter + time.hour)
        }
        
        val currentMinute = minuteListState.firstVisibleItemIndex % 60
        if (currentMinute != time.minute) {
            minuteListState.scrollToItem(minuteCenter + time.minute)
        }
    }

    // Update time and provide haptics when scroll stops
    LaunchedEffect(hourListState.firstVisibleItemIndex) {
        val newHour = hourListState.firstVisibleItemIndex % 24
        if (newHour != time.hour) {
            onTimeChange(LocalTime.of(newHour, time.minute))
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    LaunchedEffect(minuteListState.firstVisibleItemIndex) {
        val newMinute = minuteListState.firstVisibleItemIndex % 60
        if (newMinute != time.minute) {
            onTimeChange(LocalTime.of(time.hour, newMinute))
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.add_alarm_set_time),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight * visibleItems),
                contentAlignment = Alignment.Center
            ) {
                // Selection highlight
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(itemHeight)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours wheel
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        LazyColumn(
                            state = hourListState,
                            modifier = Modifier
                                .height(itemHeight * visibleItems)
                                .width(80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            flingBehavior = rememberSnapFlingBehavior(lazyListState = hourListState),
                            contentPadding = PaddingValues(vertical = itemHeight * 2)
                        ) {
                            items(hoursCount) { index ->
                                val hour = index % 24
                                
                                // Calculate precise distance from center using scroll offset for real-time animation
                                val scrollOffset = hourListState.firstVisibleItemScrollOffset / itemHeightPx
                                val preciseDistance = index - hourListState.firstVisibleItemIndex - scrollOffset
                                val absDistance = abs(preciseDistance)
                                
                                // Dynamic visuals based on precise distance
                                val isSelected = absDistance < 0.5f
                                val opacity = (1f - (absDistance * 0.3f)).coerceIn(0.2f, 1f)
                                val scale = (1f - (absDistance * 0.12f)).coerceIn(0.65f, 1f)
                                val rotationX = preciseDistance * 18f // Simulated cylinder rotation
                                
                                WheelPickerItem(
                                    text = String.format("%02d", hour),
                                    isSelected = isSelected,
                                    height = itemHeight,
                                    opacity = opacity,
                                    scale = scale,
                                    rotationX = rotationX,
                                    onClick = {
                                        coroutineScope.launch {
                                            hourListState.animateScrollToItem(index)
                                        }
                                    }
                                )
                            }
                        }

                        // Fade gradient overlay
                        FadeOverlay(height = itemHeight * visibleItems)
                    }

                    // Separator
                    Text(
                        text = ":",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Minutes wheel
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        LazyColumn(
                            state = minuteListState,
                            modifier = Modifier
                                .height(itemHeight * visibleItems)
                                .width(80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            flingBehavior = rememberSnapFlingBehavior(lazyListState = minuteListState),
                            contentPadding = PaddingValues(vertical = itemHeight * 2)
                        ) {
                            items(minutesCount) { index ->
                                val minute = index % 60
                                
                                // Calculate precise distance from center using scroll offset
                                val scrollOffset = minuteListState.firstVisibleItemScrollOffset / itemHeightPx
                                val preciseDistance = index - minuteListState.firstVisibleItemIndex - scrollOffset
                                val absDistance = abs(preciseDistance)
                                
                                val isSelected = absDistance < 0.5f
                                val opacity = (1f - (absDistance * 0.3f)).coerceIn(0.2f, 1f)
                                val scale = (1f - (absDistance * 0.12f)).coerceIn(0.65f, 1f)
                                val rotationX = preciseDistance * 18f
                                
                                WheelPickerItem(
                                    text = String.format("%02d", minute),
                                    isSelected = isSelected,
                                    height = itemHeight,
                                    opacity = opacity,
                                    scale = scale,
                                    rotationX = rotationX,
                                    onClick = {
                                        coroutineScope.launch {
                                            minuteListState.animateScrollToItem(index)
                                        }
                                    }
                                )
                            }
                        }

                        // Fade gradient overlay
                        FadeOverlay(height = itemHeight * visibleItems)
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelPickerItem(
    text: String,
    isSelected: Boolean,
    height: androidx.compose.ui.unit.Dp,
    opacity: Float = 1f,
    scale: Float = 1f,
    rotationX: Float = 0f,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(height)
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = opacity
                this.scaleX = scale
                this.scaleY = scale
                this.rotationX = rotationX
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 32.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FadeOverlay(height: androidx.compose.ui.unit.Dp) {
    val containerColor = MaterialTheme.colorScheme.primaryContainer

    Column(
        modifier = Modifier
            .height(height)
            .fillMaxWidth()
    ) {
        // Top fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            containerColor,
                            containerColor.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )

        Spacer(modifier = Modifier.weight(1f))

        // Bottom fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            containerColor.copy(alpha = 0.7f),
                            containerColor
                        )
                    )
                )
        )
    }
}

@Composable
private fun RepeatDaysSection(
    selectedDays: Set<DayOfWeek>,
    onDayToggle: (DayOfWeek) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.add_alarm_repeat),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DayOfWeek.entries.forEach { day ->
                val isSelected = selectedDays.contains(day)
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    onClick = { onDayToggle(day) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GamesSection(
    selectedGames: Set<GameType>,
    onGameToggle: (GameType) -> Unit,
    onPracticeGame: (GameType) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_games),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.add_alarm_select_games),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.height(320.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(GameType.entries) { game ->
                val isSelected = selectedGames.contains(game)
                GameCard(
                    game = game,
                    isSelected = isSelected,
                    onToggle = { onGameToggle(game) },
                    onPractice = { onPracticeGame(game) }
                )
            }
        }
    }
}

@Composable
private fun GameCard(
    game: GameType,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onPractice: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = game.localizedDisplayName(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() }
                )
            }
            Text(
                text = game.localizedDescription(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onPractice,
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.play), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun DifficultySection(
    difficulty: GameDifficulty,
    onDifficultyChange: (GameDifficulty) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.add_alarm_difficulty),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GameDifficulty.entries.forEach { diff ->
                FilterChip(
                    selected = difficulty == diff,
                    onClick = { onDifficultyChange(diff) },
                    label = { Text(diff.localizedDisplayName()) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SnoozeSection(
    snoozeEnabled: Boolean,
    snoozeDuration: Int,
    maxSnoozeCount: Int,
    onSnoozeEnabledChange: (Boolean) -> Unit,
    onSnoozeDurationChange: (Int) -> Unit,
    onMaxSnoozeCountChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.add_alarm_snooze), style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = snoozeEnabled,
                onCheckedChange = onSnoozeEnabledChange
            )
        }

        if (snoozeEnabled) {
            Spacer(modifier = Modifier.height(8.dp))

            // Snooze duration
            Text(
                text = stringResource(R.string.add_alarm_snooze_duration) + ": " + stringResource(R.string.duration_minutes, snoozeDuration),
                style = MaterialTheme.typography.bodyMedium
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf(5, 10, 15, 20)) { duration ->
                    FilterChip(
                        selected = snoozeDuration == duration,
                        onClick = { onSnoozeDurationChange(duration) },
                        label = { Text("$duration min") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Max snooze count
            Text(
                text = stringResource(R.string.add_alarm_max_snooze, maxSnoozeCount),
                style = MaterialTheme.typography.bodyMedium
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf(1, 2, 3)) { count ->
                    FilterChip(
                        selected = maxSnoozeCount == count,
                        onClick = { onMaxSnoozeCountChange(count) },
                        label = { Text("$count") }
                    )
                }
            }
        }
    }
}

@Composable
private fun GradualVolumeSection(
    enabled: Boolean,
    duration: Int,
    onEnabledChange: (Boolean) -> Unit,
    onDurationChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(stringResource(R.string.add_alarm_gradual_volume), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.add_alarm_gradual_volume_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }

        if (enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.duration_minutes, duration / 60),
                style = MaterialTheme.typography.bodyMedium
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf(30, 60, 120, 300)) { seconds ->
                    val label = if (seconds < 60) "${seconds}s" else "${seconds / 60}min"
                    FilterChip(
                        selected = duration == seconds,
                        onClick = { onDurationChange(seconds) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SoundSection(
    soundUri: String?,
    onSelectSound: () -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.add_alarm_sound),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelectSound),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (soundUri.isNullOrBlank())
                            stringResource(R.string.sound_default)
                        else
                            stringResource(R.string.add_alarm_select_sound),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
