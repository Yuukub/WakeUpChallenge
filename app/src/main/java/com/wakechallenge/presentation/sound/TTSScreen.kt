package com.wakechallenge.presentation.sound

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wakechallenge.R
import com.wakechallenge.domain.model.AlarmSound
import com.wakechallenge.domain.model.SoundType
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TTSScreen(
    onSoundCreated: (AlarmSound) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("Good morning! Time to wake up and start your day!") }
    var isTTSReady by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var selectedLocale by remember { mutableStateOf(Locale.US) }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var pitch by remember { mutableFloatStateOf(1.0f) }

    val availableLocales = listOf(
        Locale.US to "English (US)",
        Locale.UK to "English (UK)",
        Locale("th", "TH") to "Thai",
        Locale.JAPAN to "Japanese",
        Locale.KOREA to "Korean",
        Locale.CHINA to "Chinese"
    )

    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTTSReady = true
            }
        }
        textToSpeech = tts

        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    fun speak() {
        textToSpeech?.apply {
            language = selectedLocale
            setSpeechRate(speechRate)
            setPitch(pitch)
            speak(text, TextToSpeech.QUEUE_FLUSH, null, "preview")
            isSpeaking = true

            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                }
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                }
            })
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        isSpeaking = false
    }

    fun saveAsTTSSound() {
        if (text.isBlank()) return

        isSaving = true

        // Create a unique identifier for this TTS sound
        val soundId = "tts_${System.currentTimeMillis()}"

        // Store TTS settings in SharedPreferences or DataStore
        val prefs = context.getSharedPreferences("tts_sounds", android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("${soundId}_text", text)
            putString("${soundId}_locale", selectedLocale.toString())
            putFloat("${soundId}_rate", speechRate)
            putFloat("${soundId}_pitch", pitch)
            apply()
        }

        val sound = AlarmSound(
            id = soundId,
            name = if (text.length > 30) text.take(30) + "..." else text,
            uri = "tts://$soundId",
            type = SoundType.TTS
        )

        isSaving = false
        onSoundCreated(sound)
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sound_tts_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        stopSpeaking()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { saveAsTTSSound() },
                        enabled = text.isNotBlank() && isTTSReady && !isSaving
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Text input
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.tts_message_label)) },
                placeholder = { Text(stringResource(R.string.tts_message_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Suggested messages
            Text(
                text = stringResource(R.string.tts_suggestions),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            val suggestions = listOf(
                "Good morning! Time to wake up!",
                "Rise and shine! Today is going to be a great day!",
                "Wake up! You have important things to do today!",
                "Time to get up! Don't be late!",
                "Good morning sunshine! Let's start the day!"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.take(2).forEach { suggestion ->
                    SuggestionChip(
                        onClick = { text = suggestion },
                        label = { Text(suggestion.take(20) + "...") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider()

            // Language selection
            Text(
                text = stringResource(R.string.tts_voice_settings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Language dropdown
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = availableLocales.find { it.first == selectedLocale }?.second ?: "English (US)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.tts_language)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableLocales.forEach { (locale, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedLocale = locale
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Speech rate slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.tts_speech_rate))
                    Text("${String.format("%.1f", speechRate)}x")
                }
                Slider(
                    value = speechRate,
                    onValueChange = { speechRate = it },
                    valueRange = 0.5f..2.0f,
                    steps = 5
                )
            }

            // Pitch slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.tts_pitch))
                    Text(String.format("%.1f", pitch))
                }
                Slider(
                    value = pitch,
                    onValueChange = { pitch = it },
                    valueRange = 0.5f..2.0f,
                    steps = 5
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Preview button
            Button(
                onClick = {
                    if (isSpeaking) stopSpeaking() else speak()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isTTSReady && text.isNotBlank()
            ) {
                Icon(
                    if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isSpeaking) stringResource(R.string.tts_stop_preview) else stringResource(R.string.tts_preview))
            }

            if (!isTTSReady) {
                Text(
                    text = stringResource(R.string.tts_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
