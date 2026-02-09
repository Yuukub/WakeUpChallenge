package com.wakechallenge.presentation.settings

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import com.wakechallenge.R
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.wakechallenge.domain.model.GameType
import com.wakechallenge.util.LocaleHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val DEFAULT_SNOOZE_MINUTES = intPreferencesKey("default_snooze_minutes")
    val DEFAULT_VIBRATION = booleanPreferencesKey("default_vibration")
    val DEFAULT_GRADUAL_VOLUME = booleanPreferencesKey("default_gradual_volume")
    val GRADUAL_VOLUME_DURATION = intPreferencesKey("gradual_volume_duration")
    val DEFAULT_SMART_ALARM = booleanPreferencesKey("default_smart_alarm")
    val SMART_ALARM_WINDOW = intPreferencesKey("smart_alarm_window")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGamePractice: ((GameType) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Settings state
    var darkMode by remember { mutableStateOf(true) }
    var appLanguage by remember { mutableStateOf(LocaleHelper.LANGUAGE_SYSTEM) }
    var defaultSnoozeDuration by remember { mutableIntStateOf(5) }
    var defaultVibration by remember { mutableStateOf(true) }
    var defaultGradualVolume by remember { mutableStateOf(false) }
    var gradualVolumeDuration by remember { mutableIntStateOf(60) }
    var defaultSmartAlarm by remember { mutableStateOf(false) }
    var smartAlarmWindow by remember { mutableIntStateOf(30) }

    // Dialogs
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showSnoozePicker by remember { mutableStateOf(false) }
    var showGradualDurationPicker by remember { mutableStateOf(false) }
    var showSmartWindowPicker by remember { mutableStateOf(false) }
    var showGamePractice by remember { mutableStateOf(false) }

    // Load settings
    LaunchedEffect(Unit) {
        val prefs = context.settingsDataStore.data.first()
        darkMode = prefs[SettingsKeys.DARK_MODE] ?: true
        appLanguage = prefs[LocaleHelper.LANGUAGE_KEY] ?: LocaleHelper.LANGUAGE_SYSTEM
        defaultSnoozeDuration = prefs[SettingsKeys.DEFAULT_SNOOZE_MINUTES] ?: 5
        defaultVibration = prefs[SettingsKeys.DEFAULT_VIBRATION] ?: true
        defaultGradualVolume = prefs[SettingsKeys.DEFAULT_GRADUAL_VOLUME] ?: false
        gradualVolumeDuration = prefs[SettingsKeys.GRADUAL_VOLUME_DURATION] ?: 60
        defaultSmartAlarm = prefs[SettingsKeys.DEFAULT_SMART_ALARM] ?: false
        smartAlarmWindow = prefs[SettingsKeys.SMART_ALARM_WINDOW] ?: 30
    }

    // Save functions
    fun saveBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        scope.launch {
            context.settingsDataStore.edit { prefs ->
                prefs[key] = value
            }
        }
    }

    fun saveInt(key: Preferences.Key<Int>, value: Int) {
        scope.launch {
            context.settingsDataStore.edit { prefs ->
                prefs[key] = value
            }
        }
    }

    fun saveString(key: Preferences.Key<String>, value: String) {
        scope.launch {
            context.settingsDataStore.edit { prefs ->
                prefs[key] = value
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // General Section
            item {
                SettingsSection(title = stringResource(R.string.settings_general))
            }

            item {
                SettingsSwitch(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.settings_dark_mode),
                    description = stringResource(R.string.settings_dark_mode_desc),
                    checked = darkMode,
                    onCheckedChange = {
                        darkMode = it
                        saveBoolean(SettingsKeys.DARK_MODE, it)
                    }
                )
            }

            item {
                SettingsClickable(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_language),
                    description = when (appLanguage) {
                        LocaleHelper.LANGUAGE_ENGLISH -> stringResource(R.string.language_english)
                        LocaleHelper.LANGUAGE_THAI -> stringResource(R.string.language_thai)
                        else -> stringResource(R.string.language_system)
                    },
                    onClick = { showLanguagePicker = true }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Alarm Defaults Section
            item {
                SettingsSection(title = stringResource(R.string.settings_alarm_defaults))
            }

            item {
                SettingsClickable(
                    icon = Icons.Default.Snooze,
                    title = stringResource(R.string.settings_snooze_duration),
                    description = stringResource(R.string.settings_snooze_duration_value, defaultSnoozeDuration),
                    onClick = { showSnoozePicker = true }
                )
            }

            item {
                SettingsSwitch(
                    icon = Icons.Default.Vibration,
                    title = stringResource(R.string.settings_vibration),
                    description = stringResource(R.string.settings_vibration_desc),
                    checked = defaultVibration,
                    onCheckedChange = {
                        defaultVibration = it
                        saveBoolean(SettingsKeys.DEFAULT_VIBRATION, it)
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Smart Features Section
            item {
                SettingsSection(title = stringResource(R.string.settings_smart_features))
            }

            item {
                SettingsSwitch(
                    icon = Icons.Default.VolumeUp,
                    title = stringResource(R.string.settings_gradual_volume),
                    description = stringResource(R.string.settings_gradual_volume_desc),
                    checked = defaultGradualVolume,
                    onCheckedChange = {
                        defaultGradualVolume = it
                        saveBoolean(SettingsKeys.DEFAULT_GRADUAL_VOLUME, it)
                    }
                )
            }

            if (defaultGradualVolume) {
                item {
                    SettingsClickable(
                        icon = Icons.Default.Timer,
                        title = stringResource(R.string.settings_gradual_duration),
                        description = stringResource(R.string.settings_gradual_duration_value, gradualVolumeDuration),
                        onClick = { showGradualDurationPicker = true },
                        indent = true
                    )
                }
            }

            item {
                SettingsSwitch(
                    icon = Icons.Default.Bedtime,
                    title = stringResource(R.string.settings_smart_alarm),
                    description = stringResource(R.string.settings_smart_alarm_desc),
                    checked = defaultSmartAlarm,
                    onCheckedChange = {
                        defaultSmartAlarm = it
                        saveBoolean(SettingsKeys.DEFAULT_SMART_ALARM, it)
                    }
                )
            }

            if (defaultSmartAlarm) {
                item {
                    SettingsClickable(
                        icon = Icons.Default.Schedule,
                        title = stringResource(R.string.settings_smart_window),
                        description = stringResource(R.string.settings_smart_window_value, smartAlarmWindow),
                        onClick = { showSmartWindowPicker = true },
                        indent = true
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Games Section
            item {
                SettingsSection(title = stringResource(R.string.settings_games))
            }

            item {
                SettingsClickable(
                    icon = Icons.Default.SportsEsports,
                    title = stringResource(R.string.settings_practice),
                    description = stringResource(R.string.settings_practice_desc),
                    onClick = { showGamePractice = true }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // About Section
            item {
                SettingsSection(title = stringResource(R.string.settings_about))
            }

            item {
                SettingsInfo(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.settings_version),
                    description = "1.0.0"
                )
            }

            item {
                SettingsInfo(
                    icon = Icons.Default.Code,
                    title = stringResource(R.string.settings_developer),
                    description = stringResource(R.string.settings_developer_name)
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Language Picker Dialog
    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column {
                    val options = listOf(
                        LocaleHelper.LANGUAGE_SYSTEM to R.string.language_system,
                        LocaleHelper.LANGUAGE_ENGLISH to R.string.language_english,
                        LocaleHelper.LANGUAGE_THAI to R.string.language_thai
                    )
                    options.forEach { (code, labelRes) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    appLanguage = code
                                    saveString(LocaleHelper.LANGUAGE_KEY, code)
                                    showLanguagePicker = false
                                    (context as? Activity)?.recreate()
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appLanguage == code,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(labelRes))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguagePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Snooze Duration Picker Dialog
    if (showSnoozePicker) {
        val options = listOf(1, 3, 5, 10, 15, 20)
        AlertDialog(
            onDismissRequest = { showSnoozePicker = false },
            title = { Text(stringResource(R.string.settings_snooze_duration)) },
            text = {
                Column {
                    options.forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    defaultSnoozeDuration = minutes
                                    saveInt(SettingsKeys.DEFAULT_SNOOZE_MINUTES, minutes)
                                    showSnoozePicker = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultSnoozeDuration == minutes,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(R.string.duration_minutes, minutes))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSnoozePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Gradual Volume Duration Picker
    if (showGradualDurationPicker) {
        val options = listOf(30, 60, 120, 180, 300)
        AlertDialog(
            onDismissRequest = { showGradualDurationPicker = false },
            title = { Text(stringResource(R.string.settings_gradual_duration)) },
            text = {
                Column {
                    options.forEach { seconds ->
                        val label = when (seconds) {
                            30 -> stringResource(R.string.duration_seconds, 30)
                            60 -> stringResource(R.string.duration_1_minute)
                            120 -> stringResource(R.string.duration_minutes, 2)
                            180 -> stringResource(R.string.duration_minutes, 3)
                            300 -> stringResource(R.string.duration_minutes, 5)
                            else -> stringResource(R.string.duration_seconds, seconds)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    gradualVolumeDuration = seconds
                                    saveInt(SettingsKeys.GRADUAL_VOLUME_DURATION, seconds)
                                    showGradualDurationPicker = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = gradualVolumeDuration == seconds,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGradualDurationPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Smart Alarm Window Picker
    if (showSmartWindowPicker) {
        val options = listOf(15, 30, 45, 60)
        AlertDialog(
            onDismissRequest = { showSmartWindowPicker = false },
            title = { Text(stringResource(R.string.settings_smart_window)) },
            text = {
                Column {
                    options.forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    smartAlarmWindow = minutes
                                    saveInt(SettingsKeys.SMART_ALARM_WINDOW, minutes)
                                    showSmartWindowPicker = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = smartAlarmWindow == minutes,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(R.string.settings_smart_window_value, minutes))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSmartWindowPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Game Practice Dialog
    if (showGamePractice) {
        AlertDialog(
            onDismissRequest = { showGamePractice = false },
            title = { Text(stringResource(R.string.settings_practice)) },
            text = {
                Column {
                    GameType.entries.forEach { game ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showGamePractice = false
                                    onNavigateToGamePractice?.invoke(game)
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (game) {
                                    GameType.MATH -> Icons.Default.Calculate
                                    GameType.TIC_TAC_TOE -> Icons.Default.Grid3x3
                                    GameType.MEMORY_MATCH -> Icons.Default.GridView
                                    GameType.TYPE_PHRASE -> Icons.Default.Keyboard
                                    GameType.PUZZLE_SLIDE -> Icons.Default.Extension
                                    GameType.COLOR_MATCH -> Icons.Default.Palette
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = game.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = game.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGamePractice = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsSwitch(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsClickable(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    indent: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .then(if (indent) Modifier.padding(start = 40.dp) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SettingsInfo(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
