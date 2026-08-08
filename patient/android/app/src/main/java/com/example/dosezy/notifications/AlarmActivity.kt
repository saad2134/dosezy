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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.example.dosezy.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.utils.TimeFormatUtils
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.User
import com.example.dosezy.data.model.getLocalizedDosageDisplay
import com.example.dosezy.ui.screens.MedicineImage
import com.example.dosezy.ui.theme.DosezyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    @Inject
    lateinit var database: DosezyDatabase

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Turn screen on and unlock keyguard
        setupWindowFlags()

        // Play alarm sound and vibration
        startAlarmSoundAndVibration()

        val entryId = intent.getStringExtra("entry_id") ?: ""
        val initialMedicineName = intent.getStringExtra("medicine_name") ?: "Medication"
        val initialScheduledTime = intent.getStringExtra("scheduled_time") ?: ""

        setContent {
            var user by remember { mutableStateOf<User?>(null) }
            
            LaunchedEffect(entryId) {
                if (entryId.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        val entry = database.scheduleDao().getScheduleEntryById(entryId)
                        entry?.let { e ->
                            user = database.userDao().getUserByIdDirect(e.userId)
                        }
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
                AlarmScreenContent(
                    entryId = entryId,
                    initialMedicineName = initialMedicineName,
                    initialScheduledTime = initialScheduledTime,
                    database = database,
                    isDarkTheme = isDark,
                    onDismiss = {
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
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun startAlarmSoundAndVibration() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            ringtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone?.play()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibrator = vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 500, 500, 500, 500)
            vibrator?.vibrate(android.os.VibrationEffect.createWaveform(pattern, 0))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm() {
        try {
            ringtone?.stop()
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AlarmScreenContent(
    entryId: String,
    initialMedicineName: String,
    initialScheduledTime: String,
    database: DosezyDatabase,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var scheduleEntry by remember { mutableStateOf<ScheduleEntry?>(null) }
    var medicine by remember { mutableStateOf<Medicine?>(null) }
    var user by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(entryId) {
        if (entryId.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val entry = database.scheduleDao().getScheduleEntryById(entryId)
                scheduleEntry = entry
                entry?.let { e ->
                    medicine = database.medicineDao().getMedicineByIdDirect(e.medicineId)
                    user = database.userDao().getUserByIdDirect(e.userId)
                }
            }
        }
    }

    // Prevent flash of unpopulated data
    if (user == null || medicine == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }

    val context = LocalContext.current
    val medicineName = medicine?.medicationName ?: initialMedicineName
    val dosageText = medicine?.getLocalizedDosageDisplay() ?: ""
    val userName = user?.fullName ?: stringResource(R.string.profile)

    val timeFormat = user?.timeFormat ?: TimeFormat.HOUR_12
    val targetLocale = remember(user?.language) {
        com.example.dosezy.utils.LocaleHelper.getLocale(user?.language ?: com.example.dosezy.data.model.Language.SYSTEM)
    }
    val formattedTime = remember(scheduleEntry, timeFormat, targetLocale) {
        scheduleEntry?.scheduledDateTime?.let {
            TimeFormatUtils.formatTime(it, timeFormat, targetLocale)
        } ?: initialScheduledTime
    }

    val snoozeMins = user?.snoozeDuration ?: 10

    // Fix status bar color to match the alarm screen background
    val view = androidx.compose.ui.platform.LocalView.current
    val bgColor = MaterialTheme.colorScheme.background
    val isDark = !isDarkTheme
    androidx.compose.runtime.SideEffect {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // ═══ TOP SECTION: App logo & name + Squircle icon + title ═══
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
            ) {
                // App Branding: Logo + Name row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.loader_icon),
                        contentDescription = "Dosezy Logo",
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Medicine icon in squircle
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.alarm_medication_reminder),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
            }

            // ═══ MIDDLE SECTION: Profile badge + Medicine card (Centered Vertically) ═══
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // "Reminder for profile_name" badge above medicine card
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.alarm_reminder_for, userName),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Medicine card with horizontal layout
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(elevation = 4.dp, shape = RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LEFT: Large medicine image taking exactly 50% width
                        Box(
                            modifier = Modifier
                                .weight(0.5f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(20.dp))
                        ) {
                            MedicineImage(
                                imageUri = medicine?.imageUri,
                                iconSize = 64.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // RIGHT: Medicine details taking the remaining 50% width
                        Column(
                            modifier = Modifier.weight(0.5f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = medicineName,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (dosageText.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = dosageText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (formattedTime.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = formattedTime,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ═══ BOTTOM SECTION: Action buttons (Take Now + Snooze) ═══
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // TAKE NOW BUTTON (Green)
                Button(
                    onClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                if (entryId.isNotEmpty()) {
                                    val now = LocalDateTime.now().toString()
                                    database.scheduleDao().updateMedicationStatus(entryId, com.example.dosezy.data.model.MedicationStatus.TAKEN_ON_TIME.name, now)
                                }
                            }
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.alarm_take_now),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // SNOOZE BUTTON (Orange filled)
                Button(
                    onClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                if (entryId.isNotEmpty()) {
                                    val alarmScheduler = AlarmScheduler(context)
                                    alarmScheduler.scheduleSnooze(
                                        entryId = entryId,
                                        minutes = snoozeMins,
                                        medicineName = medicineName
                                    )
                                }
                            }
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))
                ) {
                    Icon(imageVector = Icons.Default.Snooze, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.alarm_snooze, snoozeMins),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
