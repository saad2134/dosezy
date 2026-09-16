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
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
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

    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeInstance = java.lang.ref.WeakReference(this)

        // Turn screen on and unlock keyguard
        setupWindowFlags()

        // Start vibration immediately
        startAlarmVibration()

        val entryId = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_ENTRY_ID) ?: ""
        val entryIds = intent.getStringArrayListExtra(MedicineAlarmReceiver.EXTRA_ENTRY_IDS) ?: arrayListOf(entryId)
        val initialMedicineName = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME) ?: "Medication"
        val initialScheduledTime = intent.getStringExtra(MedicineAlarmReceiver.EXTRA_SCHEDULED_TIME) ?: ""

        // Fetch user alarm sound & duration preference and start audio playback + auto-silence
        lifecycleScope.launch(Dispatchers.IO) {
            var targetSound: AlarmSound = AlarmSound.SYSTEM_DEFAULT
            var autoSilenceSeconds = 0
            try {
                if (entryId.isNotEmpty()) {
                    val entry = database.scheduleDao().getScheduleEntryById(entryId)
                    if (entry != null) {
                        val u = database.userDao().getUserByIdDirect(entry.userId)
                        if (u != null) {
                            targetSound = u.alarmSound
                            autoSilenceSeconds = u.alarmDurationSeconds
                        }
                    }
                }
                if (targetSound == AlarmSound.SYSTEM_DEFAULT || autoSilenceSeconds == 0) {
                    val users = database.userDao().getAllUsersDirect()
                    val currentUser = users.find { it.isCurrentUser } ?: users.firstOrNull()
                    if (currentUser != null) {
                        if (targetSound == AlarmSound.SYSTEM_DEFAULT) {
                            targetSound = currentUser.alarmSound
                        }
                        if (autoSilenceSeconds == 0) {
                            autoSilenceSeconds = currentUser.alarmDurationSeconds
                        }
                    }
                }
            } catch (_: Exception) {}

            withContext(Dispatchers.Main) {
                playAlarmSound(targetSound)
            }

            if (autoSilenceSeconds > 0) {
                kotlinx.coroutines.delay(autoSilenceSeconds * 1000L)
                withContext(Dispatchers.Main) {
                    stopAlarm()
                }
            }
        }

        setContent {
            var user by remember { mutableStateOf<User?>(null) }
            
            LaunchedEffect(entryId) {
                if (entryId.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        try {
                            val entry = database.scheduleDao().getScheduleEntryById(entryId)
                            entry?.let { e ->
                                user = database.userDao().getUserByIdDirect(e.userId)
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            val systemIsDark = isSystemInDarkTheme()
            val isDark = when (user?.theme) {
                com.example.dosezy.data.model.Theme.DARK -> true
                com.example.dosezy.data.model.Theme.LIGHT -> false
                else -> systemIsDark
            }

            DosezyTheme(darkTheme = isDark) {
                GroupedAlarmScreenContent(
                    entryIds = entryIds.filter { it.isNotEmpty() },
                    initialMedicineName = initialMedicineName,
                    initialScheduledTime = initialScheduledTime,
                    database = database,
                    isDarkTheme = isDark,
                    onDismiss = {
                        entryIds.forEach { id ->
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

    private fun startAlarmVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibrator = vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (_: Exception) {}
    }

    private fun playAlarmSound(sound: AlarmSound) {
        try {
            if (sound.rawResId != null) {
                mediaPlayer = MediaPlayer.create(applicationContext, sound.rawResId).apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    start()
                }
            } else {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone?.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                ringtone?.play()
            }
        } catch (_: Exception) {}
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
        try {
            ringtone?.stop()
            ringtone = null
        } catch (_: Exception) {}
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
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

    val view = androidx.compose.ui.platform.LocalView.current
    val bgColor = MaterialTheme.colorScheme.background
    val isDark = !isDarkTheme
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window
        window?.statusBarColor = bgColor.toArgb()
        window?.let {
            androidx.core.view.WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = isDark
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                        if (user?.profilePicPath != null && File(user!!.profilePicPath!!).exists()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(File(user!!.profilePicPath!!))
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
                                text = "Reminder for:",
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

                Text(
                    text = if (medicinesList.size > 1) "Medication Reminder (${medicinesList.size})" else "Medication Reminder",
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
                        text = initialScheduledTime.ifEmpty { "Time to take your meds!" },
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

                    medicinesList.forEach { (_, med) ->
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
                                        if (med.imageUri != null && File(med.imageUri).exists()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(File(med.imageUri))
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
                                            text = med.getDosageDisplay(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        med.currentStock?.let { stock ->
                                            Text(
                                                text = "Stock: $stock units remaining",
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
                                val nowStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                if (medicinesList.isNotEmpty()) {
                                    medicinesList.forEach { (entry, _) ->
                                        scheduleRepository.recordDoseTaken(entry.entryId, "TAKEN_ON_TIME", nowStr, context)
                                    }
                                } else if (entryIds.isNotEmpty()) {
                                    entryIds.forEach { id ->
                                        scheduleRepository.recordDoseTaken(id, "TAKEN_ON_TIME", nowStr, context)
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
                        text = if (medicinesList.size > 1) "TAKE ALL (${medicinesList.size})" else "TAKE MEDICINE",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        fontSize = 17.sp
                    )
                }

                // "Snooze (10 minutes)" Orange Secondary Button (Identical Size 56dp)
                Button(
                    onClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                val alarmScheduler = AlarmScheduler(context)
                                if (medicinesList.isNotEmpty()) {
                                    medicinesList.forEach { (entry, med) ->
                                        alarmScheduler.scheduleSnooze(entry.entryId, 10, med.medicationName)
                                    }
                                } else if (entryIds.isNotEmpty()) {
                                    entryIds.forEach { id ->
                                        alarmScheduler.scheduleSnooze(id, 10, initialMedicineName)
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
                        containerColor = Color(0xFFF59E0B),
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.Snooze, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SNOOZE (10 MINUTES)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        fontSize = 17.sp
                    )
                }
            }
        }
    }
}
