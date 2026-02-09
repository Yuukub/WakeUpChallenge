package com.wakechallenge.presentation.sound

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.wakechallenge.R
import com.wakechallenge.domain.model.AlarmSound
import com.wakechallenge.domain.model.SoundType
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioRecorderScreen(
    onSoundRecorded: (AlarmSound) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var hasRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }

    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Timer for recording duration
    LaunchedEffect(isRecording, isPaused) {
        while (isRecording && !isPaused) {
            delay(1000)
            recordingDuration++
        }
    }

    // Pulsing animation for recording indicator
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.release()
            mediaPlayer?.release()
        }
    }

    fun startRecording() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(context.cacheDir, "alarm_recording_$timestamp.m4a")
        recordingFile = file

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        isRecording = true
        recordingDuration = 0
    }

    fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false
        isPaused = false
        hasRecording = true
    }

    fun playRecording() {
        recordingFile?.let { file ->
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    isPlaying = false
                }
            }
            isPlaying = true
        }
    }

    fun stopPlaying() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        isPlaying = false
    }

    fun deleteRecording() {
        recordingFile?.delete()
        recordingFile = null
        hasRecording = false
        recordingDuration = 0
    }

    fun saveRecording() {
        recordingFile?.let { file ->
            // Copy to permanent location
            val permanentDir = File(context.filesDir, "recordings")
            if (!permanentDir.exists()) permanentDir.mkdirs()

            val permanentFile = File(permanentDir, file.name)
            file.copyTo(permanentFile, overwrite = true)

            val sound = AlarmSound(
                id = permanentFile.absolutePath,
                name = "Recorded Sound",
                uri = permanentFile.absolutePath,
                type = SoundType.RECORDED
            )
            onSoundRecorded(sound)
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sound_record_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isRecording) stopRecording()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!hasPermission) {
                Icon(
                    Icons.Default.MicOff,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.sound_mic_permission),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Text(stringResource(R.string.sound_grant_permission))
                }
            } else {
                // Recording indicator
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .then(
                            if (isRecording && !isPaused) {
                                Modifier.scale(scale)
                            } else {
                                Modifier
                            }
                        )
                        .background(
                            color = if (isRecording) Color.Red.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Duration display
                Text(
                    text = formatDuration(recordingDuration),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when {
                        isRecording && !isPaused -> stringResource(R.string.sound_recording)
                        isRecording && isPaused -> stringResource(R.string.sound_paused)
                        hasRecording -> stringResource(R.string.sound_recording_complete)
                        else -> stringResource(R.string.sound_tap_to_record)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Control buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasRecording && !isRecording) {
                        // Delete button
                        IconButton(
                            onClick = { deleteRecording() },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        // Play/Stop button
                        FilledIconButton(
                            onClick = {
                                if (isPlaying) stopPlaying() else playRecording()
                            },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) stringResource(R.string.cd_stop) else stringResource(R.string.cd_play),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Save button
                        FilledIconButton(
                            onClick = { saveRecording() },
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Green.copy(alpha = 0.8f)
                            )
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = stringResource(R.string.save),
                                modifier = Modifier.size(32.dp),
                                tint = Color.White
                            )
                        }
                    } else {
                        // Main record button
                        FilledIconButton(
                            onClick = {
                                if (isRecording) {
                                    stopRecording()
                                } else {
                                    startRecording()
                                }
                            },
                            modifier = Modifier.size(80.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                contentDescription = if (isRecording) stringResource(R.string.cd_stop) else stringResource(R.string.sound_record),
                                modifier = Modifier.size(40.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                if (hasRecording && !isRecording) {
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(onClick = { startRecording() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.sound_record_again))
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
