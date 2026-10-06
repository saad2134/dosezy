package com.example.dosezy.notifications

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import com.example.dosezy.R
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.data.model.User
import com.example.dosezy.data.model.getLocalizedName
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.ui.theme.DosezyTheme
import com.example.dosezy.utils.TimeCalculationUtils
import com.example.dosezy.utils.TimeFormatUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.media.MediaPlayer
import androidx.lifecycle.lifecycleScope
import com.example.dosezy.data.model.AlarmSound
import javax.inject.Inject

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    companion object {
        private var activeInstance: java.lang.ref.WeakReference<AlarmActivity>? = null

        fun stopActiveAlarm() {
            try {
                AlarmAudioPlayer.stop()
                activeInstance?.get()?.let { activity ->
                    activity.runOnUiThread {
                        activity.stopAlarm()
                        activity.finish()
                    }
                }
            } catch (_: Exception) {}
            activeInstance = null
        }
    }

    @Inject
    lateinit var database: DosezyDatabase

    private val activeEntryIdsState = mutableStateOf<List<String>>(emptyList())
    private val activeMedicineNameState = mutableStateOf("Medication")
    private val activeScheduledTimeState = mutableStateOf("")


    override fun applyOverrideConfiguration(overrideConfiguration: android.content.res.Configuration?) {
        if (overrideConfiguration != null) {
            val savedLanguage = com.example.dosezy.utils.LocaleHelper.getSavedLanguage(this)
            val targetLocale = com.example.dosezy.utils.LocaleHelper.getLocale(savedLanguage)
            overrideConfiguration.setLocale(targetLocale)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                overrideConfiguration.setLocales(android.os.LocaleList(targetLocale))
            }
            overrideConfiguration.setLayoutDirection(targetLocale)
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setupWindowFlags()

        val newId = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID) ?: ""
        val newIds = intent.getStringArrayListExtra(MedicineAlarmReceiver.EXTRA_ENTRY_IDS) ?: if (newId.isNotEmpty()) arrayListOf(newId) else arrayListOf()
        val newMedName = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME) ?: "Medication"
        val newSchedTime = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME) ?: ""

        val merged = (activeEntryIdsState.value + newIds + listOf(newId)).filter { it.isNotEmpty() }.distinct()
        activeEntryIdsState.value = merged
        activeMedicineNameState.value = newMedName
        activeScheduledTimeState.value = newSchedTime
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activeInstance = java.lang.ref.WeakReference(this)

        // Turn screen on and unlock keyguard
        setupWindowFlags()

        val entryId = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID) ?: ""
        val entryIds = intent.getStringArrayListExtra(MedicineAlarmReceiver.EXTRA_ENTRY_IDS) ?: if (entryId.isNotEmpty()) arrayListOf(entryId) else arrayListOf()
        val initialMedicineName = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME) ?: "Medication"
        val initialScheduledTime = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME) ?: ""

        activeEntryIdsState.value = entryIds.filter { it.isNotEmpty() }.distinct()
        activeMedicineNameState.value = initialMedicineName
        activeScheduledTimeState.value = initialScheduledTime

        // Fallback: If AlarmAudioPlayer is not yet playing, start playback
        if (!AlarmAudioPlayer.isPlaying()) {
            lifecycleScope.launch(Dispatchers.IO) {
                var targetSound: AlarmSound = AlarmSound.SYSTEM_DEFAULT
                var customSoundPath: String? = null
                var autoSilenceSeconds: Int? = null
                try {
                    if (entryId.isNotEmpty()) {
                        val entry = database.scheduleDao().getScheduleEntryById(entryId)
                        if (entry != null) {
                            val u = database.userDao().getUserByIdDirect(entry.userId)
                            if (u != null) {
                                targetSound = u.alarmSound
                                customSoundPath = u.customAlarmSoundPath
                                autoSilenceSeconds = u.alarmDurationSeconds
                            }
                        }
                    }
                    if (autoSilenceSeconds == null) {
                        val users = database.userDao().getAllUsersDirect()
                        val currentUser = users.find { it.isCurrentUser } ?: users.firstOrNull()
                        if (currentUser != null) {
                            if (targetSound == AlarmSound.SYSTEM_DEFAULT && customSoundPath == null) {
                                targetSound = currentUser.alarmSound
                                customSoundPath = currentUser.customAlarmSoundPath
                            }
                            autoSilenceSeconds = currentUser.alarmDurationSeconds
                        }
                    }
                } catch (_: Exception) {}

                AlarmAudioPlayer.play(
                    context = applicationContext,
                    sound = targetSound,
                    customPath = customSoundPath,
                    autoSilenceSeconds = autoSilenceSeconds ?: 0
                )
            }
        }

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
            var spTheme by remember { mutableStateOf(prefs.getString("theme", "system")) }
            var user by remember { mutableStateOf<User?>(null) }
            
            LaunchedEffect(entryId) {
                withContext(Dispatchers.IO) {
                    try {
                        var targetUser: User? = null
                        if (entryId.isNotEmpty()) {
                            val entry = database.scheduleDao().getScheduleEntryById(entryId)
                            if (entry != null) {
                                targetUser = database.userDao().getUserByIdDirect(entry.userId)
                            }
                        }
                        if (targetUser == null) {
                            val users = database.userDao().getAllUsersDirect()
                            targetUser = users.find { it.isCurrentUser } ?: users.firstOrNull()
                        }
                        user = targetUser
                        targetUser?.theme?.let { theme ->
                            val themeStr = theme.name.lowercase()
                            if (spTheme == "system" || spTheme != themeStr) {
                                spTheme = themeStr
                            }
                        }
                    } catch (_: Exception) {}
                }
            }

            val systemIsDark = isSystemInDarkTheme()
            val isDark = when (spTheme) {
                "dark" -> true
                "light" -> false
                else -> when (user?.theme) {
                    com.example.dosezy.data.model.Theme.DARK -> true
                    com.example.dosezy.data.model.Theme.LIGHT -> false
                    else -> systemIsDark
                }
            }

            val currentLang = user?.language ?: com.example.dosezy.utils.LocaleHelper.getSavedLanguage(context)
            val currentLocale = com.example.dosezy.utils.LocaleHelper.getLocale(currentLang)
            val isRtl = currentLocale.language == "ar"
            val layoutDirection = if (isRtl) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr

            val localizedContext = remember(currentLang) {
                com.example.dosezy.utils.LocaleHelper.updateContextLocale(context, currentLang)
            }
            val localizedConfig = remember(currentLang) {
                val config = android.content.res.Configuration(context.resources.configuration)
                config.setLocale(currentLocale)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    config.setLocales(android.os.LocaleList(currentLocale))
                }
                config.setLayoutDirection(currentLocale)
                config
            }

            val currentEntryIds by activeEntryIdsState
            val currentMedName by activeMedicineNameState
            val currentSchedTime by activeScheduledTimeState

            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection,
                androidx.compose.ui.platform.LocalContext provides localizedContext,
                androidx.compose.ui.platform.LocalConfiguration provides localizedConfig
            ) {
                DosezyTheme(darkTheme = isDark) {
                    GroupedAlarmScreenContent(
                        entryIds = currentEntryIds.filter { it.isNotEmpty() },
                        initialMedicineName = currentMedName,
                        initialScheduledTime = currentSchedTime,
                        database = database,
                        isDarkTheme = isDark,
                        onDismiss = {
                            currentEntryIds.forEach { id ->
                                if (id.isNotEmpty()) {
                                    try {
                                        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                        notificationManager.cancel(id.hashCode())
                                    } catch (_: Exception) {}
                                }
                            }
                            stopAlarm()
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
    }

    fun stopAlarm() {
        AlarmAudioPlayer.stop()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    // Silence alarm audio immediately when physical volume keys are pressed
                    stopAlarm()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        if (activeInstance?.get() == this) {
            activeInstance = null
        }
    }
}

@Composable
fun GroupedAlarmScreenContent(
    entryIds: List<String>,
    initialMedicineName: String,
    initialScheduledTime: String,
    database: DosezyDatabase,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scheduleRepository = remember { ScheduleRepository(database) }

    var medicinesList by remember { mutableStateOf<List<Pair<ScheduleEntry, Medicine>>>(emptyList()) }
    var user by remember { mutableStateOf<User?>(null) }
    var isLoaded by remember { mutableStateOf(false) }
    var showSkipReasonDialog by remember { mutableStateOf(false) }

    val snoozeMinutes = user?.snoozeDuration ?: 10

    val snoozeAction = {
        // Guard: Notify user that alarm is snoozed so back press or dismissal does not leave user uncertain
        try {
            Toast.makeText(
                context,
                context.getString(R.string.alarm_snoozed_toast, snoozeMinutes),
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {}
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val alarmScheduler = AlarmScheduler(context)
                val allIds = if (medicinesList.isNotEmpty()) {
                    medicinesList.map { it.first.entryId }
                } else {
                    entryIds
                }
                allIds.forEach { id ->
                    alarmScheduler.cancelNagging(id)
                }
                if (medicinesList.isNotEmpty()) {
                    val ids = medicinesList.map { it.first.entryId }
                    val names = medicinesList.map { it.second.medicationName }
                    alarmScheduler.scheduleGroupedSnooze(ids, snoozeMinutes, names)
                } else if (entryIds.isNotEmpty()) {
                    alarmScheduler.scheduleGroupedSnooze(entryIds, snoozeMinutes, listOf(initialMedicineName))
                }
            }
            onDismiss()
        }
    }

    // Guard: Intercept hardware/system back gesture to safely trigger personalized snooze with alert feedback
    BackHandler {
        snoozeAction()
    }

    LaunchedEffect(entryIds) {
        withContext(Dispatchers.IO) {
            val list = mutableListOf<Pair<ScheduleEntry, Medicine>>()
            for (id in entryIds) {
                val entry = database.scheduleDao().getScheduleEntryById(id)
                if (entry != null) {
                    val med = database.medicineDao().getMedicineByIdDirect(entry.medicineId)
                    if (med != null) {
                        list.add(Pair(entry, med))
                        if (user == null) {
                            user = database.userDao().getUserByIdDirect(entry.userId)
                        }
                    }
                }
            }
            medicinesList = list
            isLoaded = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Dosezy Branding, Profile Card & Scheduled Time
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Dosezy Branding Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.loader_icon),
                        contentDescription = "Dosezy Logo",
                        modifier = Modifier.size(32.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Dosezy",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color(0xFF1193D4)
                    )
                }

                // Profile Card with "Reminder for:"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1193D4).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1193D4).copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        val profilePicPath = user?.profilePicPath?.removePrefix("file://")
                        if (profilePicPath != null && File(profilePicPath).exists()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(File(profilePicPath))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1193D4).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.default_profile),
                                    contentDescription = "Default Profile",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.alarm_reminder_for),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                            Text(
                                text = user?.fullName ?: stringResource(R.string.profile),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val ageStr = user?.let { "${it.age} yrs" } ?: ""
                            val genderStr = user?.gender?.getLocalizedName() ?: ""
                            val detailsStr = listOf(ageStr, genderStr).filter { it.isNotEmpty() }.joinToString(" • ")

                            Text(
                                text = if (detailsStr.isNotEmpty()) detailsStr else "Medication Profile",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF1193D4),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Guard: Format scheduled time or current time instead of fallback string to prevent duplicate "MEDICATION REMINDER" title
                val displayTime = remember(initialScheduledTime, medicinesList, user) {
                    if (initialScheduledTime.isNotBlank()) {
                        initialScheduledTime
                    } else {
                        val firstDt = medicinesList.firstOrNull()?.first?.scheduledDateTime
                        val tf = user?.timeFormat ?: com.example.dosezy.data.model.TimeFormat.HOUR_12
                        if (firstDt != null) {
                            com.example.dosezy.utils.TimeFormatUtils.formatTime(firstDt, tf)
                        } else {
                            val now = java.time.LocalTime.now()
                            com.example.dosezy.utils.TimeFormatUtils.formatLocalTime(now, tf)
                        }
                    }
                }

                Text(
                    text = if (medicinesList.size > 1) stringResource(R.string.alarm_medication_reminder_multi, medicinesList.size) else stringResource(R.string.alarm_medication_reminder),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Enlarged Time with Alarm Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = Color(0xFF1193D4),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = displayTime,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Middle Section: Scrollable Medicines Checklist Region (for 5-7+ tablets)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (medicinesList.isEmpty() && isLoaded) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = Color(0xFF1193D4),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = initialMedicineName,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                } else {
                    val cardBgColor = if (isDarkTheme) Color(0xFF1A1D24) else MaterialTheme.colorScheme.surfaceVariant
                    val cardBorder = if (isDarkTheme) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2D323E)) else null

                    medicinesList.forEach { (entry, med) ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            border = cardBorder,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Medicine Image in Squircle Frame
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF1193D4).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val medImagePath = med.imageUri?.removePrefix("file://")
                                        if (medImagePath != null && File(medImagePath).exists()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(File(medImagePath))
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Medicine Image",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(14.dp)),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            com.example.dosezy.ui.components.PillShapeVisual(
                                                shape = med.pillShape,
                                                colorHex = med.pillColor,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = med.medicationName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = med.getDosageDisplay(entry.scheduledDateTime.toLocalTime()),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        med.currentStock?.let { stock ->
                                            Text(
                                                text = stringResource(R.string.alarm_stock_remaining, stock),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        if (!med.notes.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val noteBg = if (isDarkTheme) Color(0xFF381A05) else Color(0xFFFEF3C7)
                                            val noteTextColor = if (isDarkTheme) Color(0xFFFDBA74) else Color(0xFF92400E)
                                            val noteBorderColor = if (isDarkTheme) Color(0xFFF59E0B).copy(alpha = 0.35f) else Color(0xFFF59E0B).copy(alpha = 0.5f)
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = noteBg,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, noteBorderColor)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "📝 ${med.notes}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = noteTextColor,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Section: Actions ("Take All" Green Primary & "Snooze" Orange Secondary)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // "Take / Take All" Green Primary Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                val alarmScheduler = AlarmScheduler(context)
                                val allIds = if (medicinesList.isNotEmpty()) {
                                    medicinesList.map { it.first.entryId }
                                } else {
                                    entryIds
                                }
                                allIds.forEach { id ->
                                    alarmScheduler.cancelNagging(id)
                                }
                                val now = LocalDateTime.now()
                                val nowStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                val lateAfter = user?.considerLateAfter ?: 3
                                val missedAfter = user?.considerMissedAfter ?: 6
                                if (medicinesList.isNotEmpty()) {
                                    medicinesList.forEach { (entry, _) ->
                                        // Guard: Use isTakenLate so delayed/snoozed or overdue doses past missedAfter are correctly recorded as TAKEN_LATE
                                        val status = if (TimeCalculationUtils.isTakenLate(entry.scheduledDateTime, now, lateAfter)) {
                                            "TAKEN_LATE"
                                        } else {
                                            "TAKEN_ON_TIME"
                                        }
                                        scheduleRepository.recordDoseTaken(entry.entryId, status, nowStr, context)
                                    }
                                } else if (entryIds.isNotEmpty()) {
                                    entryIds.forEach { id ->
                                        val entry = database.scheduleDao().getScheduleEntryById(id)
                                        val entryUser = entry?.let { database.userDao().getUserByIdDirect(it.userId) } ?: user
                                        val entryLateAfter = entryUser?.considerLateAfter ?: 3
                                        val entryMissedAfter = entryUser?.considerMissedAfter ?: 6
                                        // Guard: Use isTakenLate so delayed/snoozed or overdue doses past missedAfter are correctly recorded as TAKEN_LATE
                                        val status = if (entry != null && TimeCalculationUtils.isTakenLate(entry.scheduledDateTime, now, entryLateAfter)) {
                                            "TAKEN_LATE"
                                        } else {
                                            "TAKEN_ON_TIME"
                                        }
                                        scheduleRepository.recordDoseTaken(id, status, nowStr, context)
                                    }
                                }
                            }
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (medicinesList.size > 1) stringResource(R.string.alarm_take_all, medicinesList.size) else stringResource(R.string.alarm_take_medicine),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        fontSize = 17.sp
                    )
                }

                // Dynamic Snooze Orange Secondary Button (Identical Size 56dp)
                Button(
                    onClick = { snoozeAction() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B),
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.Snooze, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.alarm_snooze_format, snoozeMinutes),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        fontSize = 17.sp
                    )
                }

                // Opt-in Skip Dose Button (Outlined)
                if (user?.allowDoseSkipping == true) {
                    OutlinedButton(
                        onClick = { showSkipReasonDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDarkTheme) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Icon(imageVector = Icons.Default.FastForward, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (medicinesList.size > 1) stringResource(R.string.alarm_skip_all, medicinesList.size) else stringResource(R.string.alarm_skip_medicine),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            fontSize = 17.sp
                        )
                    }
                }
            }
        }
    }

    if (showSkipReasonDialog) {
        com.example.dosezy.ui.components.SkipReasonDialog(
            onConfirm = { reason ->
                showSkipReasonDialog = false
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        val alarmScheduler = AlarmScheduler(context)
                        val allIds = if (medicinesList.isNotEmpty()) {
                            medicinesList.map { it.first.entryId }
                        } else {
                            entryIds
                        }
                        allIds.forEach { id ->
                            alarmScheduler.cancelNagging(id)
                        }
                        if (medicinesList.isNotEmpty()) {
                            medicinesList.forEach { (entry, _) ->
                                scheduleRepository.recordDoseSkipped(entry.entryId, reason, context)
                            }
                        } else if (entryIds.isNotEmpty()) {
                            entryIds.forEach { id ->
                                scheduleRepository.recordDoseSkipped(id, reason, context)
                            }
                        }
                    }
                    onDismiss()
                }
            },
            onDismiss = { showSkipReasonDialog = false }
        )
    }
}
