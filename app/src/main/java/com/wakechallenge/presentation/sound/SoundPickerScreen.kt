package com.wakechallenge.presentation.sound

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.core.content.ContextCompat
import com.wakechallenge.R
import com.wakechallenge.domain.model.AlarmSound
import com.wakechallenge.domain.model.SoundType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundPickerScreen(
    currentSoundUri: String?,
    onSoundSelected: (AlarmSound?) -> Unit,
    onNavigateToRecorder: () -> Unit,
    onNavigateToTTS: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedSound by remember { mutableStateOf(currentSoundUri) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingUri by remember { mutableStateOf<String?>(null) }

    // Default alarm sounds from system
    val defaultSounds = remember {
        val sounds = mutableListOf<AlarmSound>()
        val ringtoneManager = RingtoneManager(context)
        ringtoneManager.setType(RingtoneManager.TYPE_ALARM)
        val cursor = ringtoneManager.cursor

        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = ringtoneManager.getRingtoneUri(cursor.position).toString()
            sounds.add(
                AlarmSound(
                    id = uri,
                    name = title,
                    uri = uri,
                    type = SoundType.DEFAULT
                )
            )
        }
        sounds.toList()
    }

    // NEW: Observe results from recorder/TTS screens
    // We need to access the NavController's backstack entry for this
    // Since we don't have it here, we'll assume it's passed via parameter or handled in NavHost
    // Actually, let's fix the NavHost to pass these back.
    // However, we can fix the default sound selection here.


    // Music picker launcher
    val musicPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val sound = AlarmSound(
                id = it.toString(),
                name = "Custom Music",
                uri = it.toString(),
                type = SoundType.MUSIC
            )
            onSoundSelected(sound)
        }
    }

    // Permission launcher for audio files
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            musicPickerLauncher.launch("audio/*")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    fun playSound(uri: String) {
        mediaPlayer?.release()
        if (playingUri == uri) {
            playingUri = null
            return
        }
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(uri))
                prepare()
                start()
                setOnCompletionListener {
                    playingUri = null
                }
            }
            playingUri = uri
        } catch (e: Exception) {
            playingUri = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sound_alarm_sound)) },
                navigationIcon = {
                    IconButton(onClick = {
                        mediaPlayer?.release()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    TextButton(onClick = {
                        mediaPlayer?.release()
                        selectedSound?.let { uri ->
                            val soundName = defaultSounds.find { it.uri == uri }?.name 
                                ?: if (uri.startsWith("content://")) "Custom Music" else "Selected Sound"
                            
                            onSoundSelected(
                                AlarmSound(
                                    id = uri,
                                    name = soundName,
                                    uri = uri,
                                    type = SoundType.DEFAULT
                                )
                            )
                        } ?: onNavigateBack()
                    }) {
                        Text(stringResource(R.string.save))
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
            // Custom options section
            item {
                Text(
                    text = stringResource(R.string.sound_custom_sounds),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Pick from music
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.sound_choose_music)) },
                            supportingContent = { Text(stringResource(R.string.sound_choose_music_desc)) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.READ_MEDIA_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    musicPickerLauncher.launch("audio/*")
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                                }
                            }
                        )

                        HorizontalDivider()

                        // Record custom sound
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.sound_record)) },
                            supportingContent = { Text(stringResource(R.string.sound_record_desc)) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable { onNavigateToRecorder() }
                        )

                        HorizontalDivider()

                        // Text-to-Speech
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.sound_tts)) },
                            supportingContent = { Text(stringResource(R.string.sound_tts_desc)) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable { onNavigateToTTS() }
                        )
                    }
                }
            }

            // Default sounds section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.sound_default_sounds),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(defaultSounds) { sound ->
                SoundItem(
                    sound = sound,
                    isSelected = selectedSound == sound.uri,
                    isPlaying = playingUri == sound.uri,
                    onSelect = { selectedSound = sound.uri },
                    onPlay = { playSound(sound.uri) }
                )
            }
        }
    }
}

@Composable
private fun SoundItem(
    sound: AlarmSound,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = sound.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Stop" else "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
