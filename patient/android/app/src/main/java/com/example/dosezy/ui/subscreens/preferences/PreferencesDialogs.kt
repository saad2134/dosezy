/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.subscreens.preferences

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dosezy.R
import com.example.dosezy.data.model.AlarmSound
import com.example.dosezy.data.model.Language
import com.example.dosezy.data.model.Theme
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.ui.components.SelectionDialog
import com.example.dosezy.utils.SoundUtils
import java.io.File

// Dialog Composable for Alarm Duration Selection
@Composable
fun AlarmDurationSelectionDialog(
    currentDuration: Int,
    onDurationSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        0 to stringResource(R.string.alarm_duration_continuous),
        -1 to stringResource(R.string.alarm_duration_once),
        15 to stringResource(R.string.alarm_duration_15s),
        30 to stringResource(R.string.alarm_duration_30s),
        60 to stringResource(R.string.alarm_duration_1m),
        120 to stringResource(R.string.alarm_duration_2m),
        300 to stringResource(R.string.alarm_duration_5m)
    )

    SelectionDialog(
        title = stringResource(R.string.pref_alarm_duration_title),
        options = options.map { it.second to it.first },
        currentSelection = currentDuration,
        onOptionSelected = onDurationSelected,
        onDismiss = onDismiss
    )
}

// Dialog Composable for Alarm Sound Selection
@Composable
fun AlarmSoundSelectionDialog(
    currentSound: AlarmSound,
    customSoundTitle: String?,
    customSoundPath: String?,
    onSoundSelected: (AlarmSound, String?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedSound by remember { mutableStateOf(currentSound) }
    var currentCustomPath by remember { mutableStateOf(customSoundPath) }
    var currentCustomTitle by remember { mutableStateOf(customSoundTitle) }

    var previewingSound by remember { mutableStateOf<AlarmSound?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var ringtone by remember { mutableStateOf<Ringtone?>(null) }

    fun stopAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        try {
            ringtone?.stop()
        } catch (_: Exception) {}
        ringtone = null
        previewingSound = null
    }

    fun playPreview(sound: AlarmSound, customPathToPlay: String? = null) {
        stopAudio()
        previewingSound = sound
        try {
            if (sound == AlarmSound.CUSTOM) {
                val path = customPathToPlay ?: currentCustomPath
                if (!path.isNullOrEmpty() && File(path).exists()) {
                    val mp = MediaPlayer().apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            setAudioStreamType(android.media.AudioManager.STREAM_ALARM)
                        }
                        setDataSource(path)
                        setOnCompletionListener {
                            previewingSound = null
                        }
                        prepare()
                        start()
                    }
                    mediaPlayer = mp
                } else {
                    previewingSound = null
                }
            } else if (sound.rawResId != null) {
                val afd = context.resources.openRawResourceFd(sound.rawResId)
                if (afd != null) {
                    val mp = MediaPlayer().apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            setAudioStreamType(android.media.AudioManager.STREAM_ALARM)
                        }
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        setOnCompletionListener {
                            previewingSound = null
                        }
                        prepare()
                        start()
                    }
                    // Guard: Close AssetFileDescriptor only AFTER prepare() to prevent Bad file descriptor status=0x1 failure in MediaPlayer native layer
                    try { afd.close() } catch (_: Exception) {}
                    mediaPlayer = mp
                }
            } else {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val r = RingtoneManager.getRingtone(context, alarmUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    r?.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    r?.streamType = android.media.AudioManager.STREAM_ALARM
                }
                ringtone = r
                r?.play()
            }
        } catch (_: Exception) {
            previewingSound = null
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val (savedFile, fileName) = SoundUtils.saveCustomAudio(context, uri)
            if (savedFile != null) {
                selectedSound = AlarmSound.CUSTOM
                currentCustomPath = savedFile.absolutePath
                currentCustomTitle = fileName
                onSoundSelected(AlarmSound.CUSTOM, savedFile.absolutePath, fileName)
                playPreview(AlarmSound.CUSTOM, savedFile.absolutePath)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopAudio()
        }
    }

    val sounds = listOf(
        AlarmSound.SYSTEM_DEFAULT,
        AlarmSound.GENTLE_CHIME,
        AlarmSound.MEDICAL_MARIMBA,
        AlarmSound.BRISK_PULSE,
        AlarmSound.CALM_BELL,
        AlarmSound.CUSTOM
    )

    AlertDialog(
        onDismissRequest = {
            stopAudio()
            onDismiss()
        },
        tonalElevation = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.pref_alarm_sound),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                sounds.forEach { sound ->
                    val isPlaying = previewingSound == sound
                    val isSelected = selectedSound == sound
                    val isCustom = sound == AlarmSound.CUSTOM

                    ListItem(
                        headlineContent = {
                            if (isCustom) {
                                Text(
                                    text = if (!currentCustomTitle.isNullOrBlank()) currentCustomTitle!! else stringResource(R.string.alarm_sound_custom),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    text = stringResource(sound.getTitleRes()),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        supportingContent = if (isCustom) {
                            {
                                Text(
                                    text = if (currentCustomPath != null) stringResource(R.string.alarm_sound_select_file) else stringResource(R.string.alarm_sound_no_file_selected),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else null,
                        leadingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    if (isCustom && currentCustomPath == null) {
                                        audioPickerLauncher.launch("audio/*")
                                    } else {
                                        selectedSound = sound
                                        playPreview(sound)
                                        val pathToSend = if (sound == AlarmSound.CUSTOM) currentCustomPath else null
                                        val titleToSend = if (sound == AlarmSound.CUSTOM) currentCustomTitle else null
                                        onSoundSelected(sound, pathToSend, titleToSend)
                                    }
                                }
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isCustom) {
                                    IconButton(
                                        onClick = {
                                            audioPickerLauncher.launch("audio/*")
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = stringResource(R.string.alarm_sound_select_file),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (!isCustom || (isCustom && currentCustomPath != null)) {
                                    IconButton(
                                        onClick = {
                                            if (isPlaying) {
                                                stopAudio()
                                            } else {
                                                playPreview(sound)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                                            contentDescription = stringResource(R.string.alarm_sound_preview),
                                            tint = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (isCustom && currentCustomPath == null) {
                                    audioPickerLauncher.launch("audio/*")
                                } else {
                                    selectedSound = sound
                                    playPreview(sound)
                                    onSoundSelected(sound, currentCustomPath, currentCustomTitle)
                                }
                            }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    stopAudio()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

// Dialog Composable for Theme Selection
@Composable
fun ThemeSelectionDialog(
    currentTheme: Theme,
    onThemeSelected: (Theme) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = stringResource(R.string.pref_select_theme),
        options = listOf(
            stringResource(R.string.theme_system) to Theme.SYSTEM,
            stringResource(R.string.theme_light) to Theme.LIGHT,
            stringResource(R.string.theme_dark) to Theme.DARK
        ),
        currentSelection = currentTheme,
        onOptionSelected = onThemeSelected,
        onDismiss = onDismiss
    )
}

// Dialog Composable for Time Format Selection
@Composable
fun TimeFormatSelectionDialog(
    currentTimeFormat: TimeFormat,
    onTimeFormatSelected: (TimeFormat) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = stringResource(R.string.pref_select_time_format),
        options = listOf(
            stringResource(R.string.time_format_12) to TimeFormat.HOUR_12,
            stringResource(R.string.time_format_24) to TimeFormat.HOUR_24
        ),
        currentSelection = currentTimeFormat,
        onOptionSelected = onTimeFormatSelected,
        onDismiss = onDismiss
    )
}

// Dialog Composable for Late After Selection
@Composable
fun LateAfterSelectionDialog(
    currentHours: Int,
    currentMissedHours: Int = 6,
    onHoursSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val maxLate = (currentMissedHours - 1).coerceIn(1, 8)
    val hourOptionStrings = (1..maxLate).map { hours ->
        stringResource(R.string.hours_format, hours) to hours
    }
    SelectionDialog(
        title = stringResource(R.string.pref_late_after),
        options = hourOptionStrings,
        currentSelection = currentHours.coerceIn(1, maxLate),
        onOptionSelected = onHoursSelected,
        onDismiss = onDismiss
    )
}

// Dialog Composable for Missed After Selection
@Composable
fun MissedAfterSelectionDialog(
    currentHours: Int,
    currentLateHours: Int = 3,
    onHoursSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val minMissed = (currentLateHours + 1).coerceIn(2, 9)
    val hourOptionStrings = (minMissed..9).map { hours ->
        stringResource(R.string.hours_format, hours) to hours
    }
    SelectionDialog(
        title = stringResource(R.string.pref_mark_missed_after),
        options = hourOptionStrings,
        currentSelection = currentHours.coerceIn(minMissed, 9),
        onOptionSelected = onHoursSelected,
        onDismiss = onDismiss
    )
}

// Dialog Composable for Language Selection
@Composable
fun LanguageSelectionDialog(
    currentLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    val sysDefaultStr = stringResource(R.string.system_default)
    SelectionDialog(
        title = stringResource(R.string.select_language),
        options = listOf(
            sysDefaultStr to Language.SYSTEM,
            "English" to Language.ENGLISH,
            "Español (Spanish)" to Language.SPANISH,
            "हिन्दी (Hindi)" to Language.HINDI,
            "中文 (Chinese)" to Language.CHINESE,
            "Português (Portuguese)" to Language.PORTUGUESE,
            "العربية (Arabic)" to Language.ARABIC,
            "Français (French)" to Language.FRENCH,
            "Deutsch (German)" to Language.GERMAN,
            "日本語 (Japanese)" to Language.JAPANESE,
            "Русский (Russian)" to Language.RUSSIAN,
            "Italiano (Italian)" to Language.ITALIAN,
            "বাংলা (Bengali)" to Language.BENGALI
        ),
        currentSelection = currentLanguage,
        onOptionSelected = onLanguageSelected,
        onDismiss = onDismiss
    )
}

// Dialog Composable for Snooze Duration Selection
@Composable
fun SnoozeSelectionDialog(
    currentMinutes: Int,
    onMinutesSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(5, 10, 15, 20, 30).map { mins ->
        stringResource(R.string.minutes_format, mins) to mins
    }
    SelectionDialog(
        title = stringResource(R.string.pref_select_snooze_duration),
        options = options,
        currentSelection = currentMinutes,
        onOptionSelected = onMinutesSelected,
        onDismiss = onDismiss
    )
}

// Guard: ADHD support intake safeguard duration picker dialog (options: 1, 2, 3, 5 minutes) (#104)
@Composable
fun IntakeDurationSelectionDialog(
    currentSeconds: Int,
    onDurationSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        60 to stringResource(R.string.pref_intake_duration_format, 1),
        120 to stringResource(R.string.pref_intake_duration_format, 2),
        180 to stringResource(R.string.pref_intake_duration_format, 3),
        300 to stringResource(R.string.pref_intake_duration_format, 5)
    )

    SelectionDialog(
        title = stringResource(R.string.dialog_intake_duration_title),
        options = options.map { it.second to it.first },
        currentSelection = currentSeconds,
        onOptionSelected = onDurationSelected,
        onDismiss = onDismiss
    )
}
