package com.example.dosezy.ui.subscreens

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dosezy.R
import com.example.dosezy.data.model.AlarmSound
import com.example.dosezy.data.model.Language
import com.example.dosezy.data.model.Theme
import com.example.dosezy.data.model.TimeFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import com.example.dosezy.utils.SoundUtils
import java.io.File
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.dosezy.ui.components.PreferenceItem
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.viewmodels.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(navController: NavController) {
    val userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
    val currentUser by userViewModel.currentUser.collectAsState()

    // State for showing dialogs
    var showThemeDialog by remember { mutableStateOf(false) }
    var showTimeFormatDialog by remember { mutableStateOf(false) }
    var showLateAfterDialog by remember { mutableStateOf(false) }
    var showMissedAfterDialog by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showAlarmSoundDialog by remember { mutableStateOf(false) }
    var showAlarmDurationDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showNaggingIntervalDialog by remember { mutableStateOf(false) }
    var showNaggingRepeatsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar(
                navController = navController,
                currentUser = currentUser,
                title = androidx.compose.ui.res.stringResource(R.string.pref_title),
                showBackButton = true,
                actions = {}
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── General Section ──
            item {
                Text(
                    text = stringResource(R.string.pref_section_general),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                )
            }

            item {
                // Theme Preference
                PreferenceItem(
                    title = androidx.compose.ui.res.stringResource(R.string.pref_theme),
                    currentValue = currentUser?.theme?.let {
                        when (it) {
                            Theme.LIGHT -> androidx.compose.ui.res.stringResource(R.string.theme_light)
                            Theme.DARK -> androidx.compose.ui.res.stringResource(R.string.theme_dark)
                            Theme.SYSTEM -> androidx.compose.ui.res.stringResource(R.string.theme_system)
                        }
                    } ?: androidx.compose.ui.res.stringResource(R.string.theme_system),
                    iconName = "palette",
                    onClick = { showThemeDialog = true }
                )
            }

            item {
                // Language Preference
                val sysLangName = com.example.dosezy.utils.LocaleHelper.getSystemLanguageDisplayName()
                PreferenceItem(
                    title = androidx.compose.ui.res.stringResource(R.string.pref_language),
                    currentValue = currentUser?.language?.let {
                        when (it) {
                            Language.SYSTEM -> "${androidx.compose.ui.res.stringResource(R.string.system_default)} ($sysLangName)"
                            Language.ENGLISH -> "English"
                            Language.SPANISH -> "Español"
                            Language.HINDI -> "हिन्दी"
                            Language.CHINESE -> "中文"
                            Language.PORTUGUESE -> "Português"
                            Language.ARABIC -> "العربية"
                            Language.FRENCH -> "Français"
                            Language.GERMAN -> "Deutsch"
                            Language.JAPANESE -> "日本語"
                            Language.RUSSIAN -> "Русский"
                            Language.ITALIAN -> "Italiano"
                            Language.BENGALI -> "বাংলা"
                        }
                    } ?: "${androidx.compose.ui.res.stringResource(R.string.system_default)} ($sysLangName)",
                    iconName = "language",
                    onClick = { showLanguageDialog = true }
                )
            }

            // ── Notifications & Alarms Section ──
            item {
                Text(
                    text = stringResource(R.string.pref_section_notifications),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
                )
            }

            item {
                // Alarm Sound Preference
                val currentSound = currentUser?.alarmSound ?: AlarmSound.SYSTEM_DEFAULT
                val customTitle = currentUser?.customAlarmSoundTitle
                val soundDisplayValue = if (currentSound == AlarmSound.CUSTOM && !customTitle.isNullOrBlank()) {
                    customTitle
                } else {
                    androidx.compose.ui.res.stringResource(currentSound.getTitleRes())
                }
                PreferenceItem(
                    title = androidx.compose.ui.res.stringResource(R.string.pref_alarm_sound),
                    currentValue = soundDisplayValue,
                    iconName = "alarm_sound",
                    onClick = { showAlarmSoundDialog = true }
                )
            }

            item {
                // Alarm Ring Duration Preference
                val currentDuration = currentUser?.alarmDurationSeconds ?: 0
                val durationText = when (currentDuration) {
                    30 -> stringResource(R.string.alarm_duration_30s)
                    60 -> stringResource(R.string.alarm_duration_1m)
                    120 -> stringResource(R.string.alarm_duration_2m)
                    300 -> stringResource(R.string.alarm_duration_5m)
                    else -> stringResource(R.string.alarm_duration_continuous)
                }
                PreferenceItem(
                    title = androidx.compose.ui.res.stringResource(R.string.pref_alarm_duration_title),
                    currentValue = durationText,
                    iconName = "alarm_duration",
                    onClick = { showAlarmDurationDialog = true }
                )
            }

            item {
                // Snooze Duration Preference
                PreferenceItem(
                    title = androidx.compose.ui.res.stringResource(R.string.pref_snooze_duration),
                    currentValue = currentUser?.snoozeDuration?.let {
                        androidx.compose.ui.res.stringResource(R.string.minutes_format, it)
                    } ?: androidx.compose.ui.res.stringResource(R.string.minutes_format, 10),
                    iconName = "snooze",
                    onClick = { showSnoozeDialog = true }
                )
            }

            // Follow-up / Nagging Reminders Switch & Settings
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.pref_nagging_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.pref_nagging_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = currentUser?.naggingRemindersEnabled == true,
                                onCheckedChange = { isChecked ->
                                    currentUser?.let { user ->
                                        userViewModel.updateUser(user.copy(naggingRemindersEnabled = isChecked))
                                    }
                                }
                            )
                        }

                        AnimatedVisibility(
                            visible = currentUser?.naggingRemindersEnabled == true,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                // Sub-option 1: Interval (Indented)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showNaggingIntervalDialog = true }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(R.string.pref_nagging_interval),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = stringResource(R.string.minutes_format, currentUser?.naggingIntervalMinutes ?: 10),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Sub-option 2: Repeats (Indented)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showNaggingRepeatsDialog = true }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Snooze,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(R.string.pref_nagging_max_repeats),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = stringResource(R.string.pref_nagging_repeats_format, currentUser?.naggingMaxRepeats ?: 3),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Dose Tracking Section ──
            item {
                Text(
                    text = stringResource(R.string.pref_section_dose_tracking),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
                )
            }

            item {
                // Consider Late After Preference
                PreferenceItem(
                    title = androidx.compose.ui.res.stringResource(R.string.pref_late_after),
                    currentValue = currentUser?.considerLateAfter?.let {
                        androidx.compose.ui.res.stringResource(R.string.hours_format, it)
                    } ?: androidx.compose.ui.res.stringResource(R.string.hours_format, 3),
                    iconName = "late_after",
                    onClick = { showLateAfterDialog = true }
                )
            }

            item {
                // Consider Missed After Preference
                PreferenceItem(
                    title = androidx.compose.ui.res.stringResource(R.string.pref_missed_after),
                    currentValue = currentUser?.considerMissedAfter?.let {
                        androidx.compose.ui.res.stringResource(R.string.hours_format, it)
                    } ?: androidx.compose.ui.res.stringResource(R.string.hours_format, 6),
                    iconName = "missed_after",
                    onClick = { showMissedAfterDialog = true }
                )
            }

            item {
                // Time Format Preference
                PreferenceItem(
                    title = androidx.compose.ui.res.stringResource(R.string.pref_time_format),
                    currentValue = currentUser?.timeFormat?.let {
                        when (it) {
                            TimeFormat.HOUR_12 -> androidx.compose.ui.res.stringResource(R.string.time_format_12)
                            TimeFormat.HOUR_24 -> androidx.compose.ui.res.stringResource(R.string.time_format_24)
                        }
                    } ?: androidx.compose.ui.res.stringResource(R.string.time_format_12),
                    iconName = "schedule",
                    onClick = { showTimeFormatDialog = true }
                )
            }
        }

        // Nagging Interval Dialog
        if (showNaggingIntervalDialog) {
            val options = listOf(5, 10, 15, 20, 30).map { mins ->
                androidx.compose.ui.res.stringResource(R.string.minutes_format, mins) to mins
            }
            com.example.dosezy.ui.components.SelectionDialog(
                title = androidx.compose.ui.res.stringResource(R.string.pref_nagging_interval),
                options = options,
                currentSelection = currentUser?.naggingIntervalMinutes ?: 10,
                onOptionSelected = { mins ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(naggingIntervalMinutes = mins))
                    }
                    showNaggingIntervalDialog = false
                },
                onDismiss = { showNaggingIntervalDialog = false }
            )
        }

        // Nagging Repeats Dialog
        if (showNaggingRepeatsDialog) {
            val options = listOf(1, 2, 3, 5).map { repeats ->
                androidx.compose.ui.res.stringResource(R.string.pref_nagging_repeats_format, repeats) to repeats
            }
            com.example.dosezy.ui.components.SelectionDialog(
                title = androidx.compose.ui.res.stringResource(R.string.pref_nagging_max_repeats),
                options = options,
                currentSelection = currentUser?.naggingMaxRepeats ?: 3,
                onOptionSelected = { count ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(naggingMaxRepeats = count))
                    }
                    showNaggingRepeatsDialog = false
                },
                onDismiss = { showNaggingRepeatsDialog = false }
            )
        }

        // Theme Selection Dialog
        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = currentUser?.theme ?: Theme.SYSTEM,
                onThemeSelected = { newTheme ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(theme = newTheme))
                    }
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }

        // Time Format Selection Dialog
        if (showTimeFormatDialog) {
            TimeFormatSelectionDialog(
                currentTimeFormat = currentUser?.timeFormat ?: TimeFormat.HOUR_12,
                onTimeFormatSelected = { newTimeFormat ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(timeFormat = newTimeFormat))
                    }
                    showTimeFormatDialog = false
                },
                onDismiss = { showTimeFormatDialog = false }
            )
        }

        // Late After Selection Dialog
        if (showLateAfterDialog) {
            LateAfterSelectionDialog(
                currentHours = currentUser?.considerLateAfter ?: 3,
                onHoursSelected = { newHours ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(considerLateAfter = newHours))
                    }
                    showLateAfterDialog = false
                },
                onDismiss = { showLateAfterDialog = false }
            )
        }

        // Missed After Selection Dialog
        if (showMissedAfterDialog) {
            MissedAfterSelectionDialog(
                currentHours = currentUser?.considerMissedAfter ?: 6,
                onHoursSelected = { newHours ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(considerMissedAfter = newHours))
                    }
                    showMissedAfterDialog = false
                },
                onDismiss = { showMissedAfterDialog = false }
            )
        }

        // Language Selection Dialog
        if (showLanguageDialog) {
            val context = androidx.compose.ui.platform.LocalContext.current
            LanguageSelectionDialog(
                currentLanguage = currentUser?.language ?: Language.SYSTEM,
                onLanguageSelected = { newLanguage ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(language = newLanguage))
                        com.example.dosezy.utils.LocaleHelper.applyLanguage(context, newLanguage, forceRecreate = true)
                    }
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false }
            )
        }

        // Snooze Selection Dialog
        if (showSnoozeDialog) {
            SnoozeSelectionDialog(
                currentMinutes = currentUser?.snoozeDuration ?: 10,
                onMinutesSelected = { newMinutes ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(snoozeDuration = newMinutes))
                    }
                    showSnoozeDialog = false
                },
                onDismiss = { showSnoozeDialog = false }
            )
        }

        // Alarm Sound Selection Dialog
        if (showAlarmSoundDialog) {
            AlarmSoundSelectionDialog(
                currentSound = currentUser?.alarmSound ?: AlarmSound.SYSTEM_DEFAULT,
                customSoundTitle = currentUser?.customAlarmSoundTitle,
                customSoundPath = currentUser?.customAlarmSoundPath,
                onSoundSelected = { newSound, customPath, customTitle ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(
                            user.copy(
                                alarmSound = newSound,
                                customAlarmSoundPath = customPath ?: user.customAlarmSoundPath,
                                customAlarmSoundTitle = customTitle ?: user.customAlarmSoundTitle
                            )
                        )
                    }
                },
                onDismiss = { showAlarmSoundDialog = false }
            )
        }

        // Alarm Duration Selection Dialog
        if (showAlarmDurationDialog) {
            AlarmDurationSelectionDialog(
                currentDuration = currentUser?.alarmDurationSeconds ?: 0,
                onDurationSelected = { newDuration ->
                    currentUser?.let { user ->
                        userViewModel.updateUser(user.copy(alarmDurationSeconds = newDuration))
                    }
                    showAlarmDurationDialog = false
                },
                onDismiss = { showAlarmDurationDialog = false }
            )
        }
    }
}

// Dialog Composable for Alarm Duration Selection
@Composable
fun AlarmDurationSelectionDialog(
    currentDuration: Int,
    onDurationSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        0 to androidx.compose.ui.res.stringResource(R.string.alarm_duration_continuous),
        30 to androidx.compose.ui.res.stringResource(R.string.alarm_duration_30s),
        60 to androidx.compose.ui.res.stringResource(R.string.alarm_duration_1m),
        120 to androidx.compose.ui.res.stringResource(R.string.alarm_duration_2m),
        300 to androidx.compose.ui.res.stringResource(R.string.alarm_duration_5m)
    )

    com.example.dosezy.ui.components.SelectionDialog(
        title = androidx.compose.ui.res.stringResource(R.string.pref_alarm_duration_title),
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
                        afd.close()
                        setOnCompletionListener {
                            previewingSound = null
                        }
                        prepare()
                        start()
                    }
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
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
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
                                        onSoundSelected(sound, currentCustomPath, currentCustomTitle)
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
    com.example.dosezy.ui.components.SelectionDialog(
        title = androidx.compose.ui.res.stringResource(R.string.pref_select_theme),
        options = listOf(
            androidx.compose.ui.res.stringResource(R.string.theme_light) to Theme.LIGHT,
            androidx.compose.ui.res.stringResource(R.string.theme_dark) to Theme.DARK,
            androidx.compose.ui.res.stringResource(R.string.theme_system) to Theme.SYSTEM
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
    com.example.dosezy.ui.components.SelectionDialog(
        title = androidx.compose.ui.res.stringResource(R.string.pref_select_time_format),
        options = listOf(
            androidx.compose.ui.res.stringResource(R.string.time_format_12) to TimeFormat.HOUR_12,
            androidx.compose.ui.res.stringResource(R.string.time_format_24) to TimeFormat.HOUR_24
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
    onHoursSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val hourOptionStrings = (1..3).map { hours ->
        androidx.compose.ui.res.stringResource(R.string.hours_format, hours) to hours
    }
    com.example.dosezy.ui.components.SelectionDialog(
        title = androidx.compose.ui.res.stringResource(R.string.pref_late_after),
        options = hourOptionStrings,
        currentSelection = currentHours,
        onOptionSelected = onHoursSelected,
        onDismiss = onDismiss
    )
}

// Dialog Composable for Missed After Selection
@Composable
fun MissedAfterSelectionDialog(
    currentHours: Int,
    onHoursSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val hourOptionStrings = (3..9).map { hours ->
        androidx.compose.ui.res.stringResource(R.string.hours_format, hours) to hours
    }
    com.example.dosezy.ui.components.SelectionDialog(
        title = androidx.compose.ui.res.stringResource(R.string.pref_mark_missed_after),
        options = hourOptionStrings,
        currentSelection = currentHours,
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
    val sysDefaultStr = androidx.compose.ui.res.stringResource(R.string.system_default)
    com.example.dosezy.ui.components.SelectionDialog(
        title = androidx.compose.ui.res.stringResource(R.string.select_language),
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
        androidx.compose.ui.res.stringResource(R.string.minutes_format, mins) to mins
    }
    com.example.dosezy.ui.components.SelectionDialog(
        title = androidx.compose.ui.res.stringResource(R.string.pref_select_snooze_duration),
        options = options,
        currentSelection = currentMinutes,
        onOptionSelected = onMinutesSelected,
        onDismiss = onDismiss
    )
}