/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.notifications.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dosezy.R
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.data.model.User
import com.example.dosezy.data.model.getLocalizedName
import com.example.dosezy.ui.components.PillShapeVisual
import com.example.dosezy.ui.components.SkipReasonDialog
import com.example.dosezy.utils.LocaleHelper
import com.example.dosezy.utils.TimeFormatUtils
import java.io.File
import java.time.LocalTime

@Composable
fun AlarmScreenContent(
    initialMedicineName: String,
    initialScheduledTime: String,
    medicinesList: List<Pair<ScheduleEntry, Medicine>>,
    user: User?,
    isLoaded: Boolean,
    isDarkTheme: Boolean,
    isDetailsMasked: Boolean = false,
    onUnlockRequest: () -> Unit = {},
    onTakeAction: () -> Unit,
    onSnoozeAction: (Int) -> Unit,
    onSkipAction: (String) -> Unit
) {
    val context = LocalContext.current
    var showSkipReasonDialog by remember { mutableStateOf(false) }
    val defaultSnooze = user?.snoozeDuration ?: 10
    var selectedSnoozeMinutes by remember(user?.snoozeDuration) { mutableStateOf(defaultSnooze) }
    val snoozeOptions = remember(defaultSnooze) {
        (listOf(5, 10, 15, 30) + defaultSnooze).distinct().sorted()
    }

    // Guard: Intercept hardware/system back gesture to safely trigger personalized snooze with alert feedback
    BackHandler {
        onSnoozeAction(selectedSnoozeMinutes)
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
                    Image(
                        painter = painterResource(id = R.drawable.loader_icon),
                        contentDescription = "Dosezy Logo",
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
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
                    border = BorderStroke(1.dp, Color(0xFF1193D4).copy(alpha = 0.25f)),
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
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1193D4).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.default_profile),
                                    contentDescription = "Default Profile",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop
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
                            // Guard: Use localized strings for age, gender, and profile fallback rather than hardcoded English " yrs" and "Medication Profile"
                            val currentUser = user
                            val detailsStr = when {
                                currentUser != null && currentUser.age > 0 -> {
                                    stringResource(R.string.profile_age_gender_format, currentUser.age, currentUser.gender.getLocalizedName())
                                }
                                currentUser != null -> {
                                    currentUser.gender.getLocalizedName()
                                }
                                else -> ""
                            }

                            Text(
                                text = if (detailsStr.isNotEmpty()) detailsStr else stringResource(R.string.profile),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF1193D4),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Guard: Format scheduled time prioritizing loaded entry time with user's preferred 12h/24h format and active locale
                val displayTime = remember(initialScheduledTime, medicinesList, user) {
                    val firstDt = medicinesList.firstOrNull()?.first?.scheduledDateTime
                    val tf = user?.timeFormat ?: TimeFormat.HOUR_12
                    val savedLang = LocaleHelper.getSavedLanguage(context)
                    val activeLocale = LocaleHelper.getLocale(savedLang)
                    if (firstDt != null) {
                        TimeFormatUtils.formatTime(firstDt, tf, activeLocale)
                    } else if (initialScheduledTime.isNotBlank()) {
                        initialScheduledTime
                    } else {
                        val now = LocalTime.now()
                        TimeFormatUtils.formatLocalTime(now, tf, activeLocale)
                    }
                }

                Text(
                    text = if (isDetailsMasked) {
                        stringResource(R.string.notif_generic_lockscreen_reminder)
                    } else if (medicinesList.size > 1) {
                        stringResource(R.string.alarm_medication_reminder_multi, medicinesList.size)
                    } else {
                        stringResource(R.string.alarm_medication_reminder)
                    },
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

            // Middle Section: Scrollable Medicines Checklist Region
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isDetailsMasked) {
                    val cardBgColor = if (isDarkTheme) Color(0xFF1A1D24) else MaterialTheme.colorScheme.surfaceVariant
                    val cardBorder = if (isDarkTheme) BorderStroke(1.dp, Color(0xFF2D323E)) else null

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        border = cardBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF1193D4).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF1193D4),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.pref_lockscreen_details_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.alarm_lockscreen_details_hidden),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = onUnlockRequest,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1193D4),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth(0.88f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.alarm_unlock_to_view_details),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                } else if (medicinesList.isEmpty() && isLoaded) {
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
                    val cardBorder = if (isDarkTheme) BorderStroke(1.dp, Color(0xFF2D323E)) else null

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
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            PillShapeVisual(
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
                                                border = BorderStroke(1.dp, noteBorderColor)
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val useSlideActions = user?.slideActionsEnabled != false
                // Guard: When lockscreen details are masked, require unlock verification before marking dose taken to protect patient safety
                val effectiveTakeAction = if (isDetailsMasked) onUnlockRequest else onTakeAction

                if (useSlideActions) {
                    val slideTakeText = if (isDetailsMasked) {
                        stringResource(R.string.alarm_unlock_to_view_details)
                    } else if (medicinesList.size > 1) {
                        stringResource(R.string.alarm_slide_to_take_all, medicinesList.size)
                    } else {
                        stringResource(R.string.alarm_slide_to_take)
                    }
                    // Guard: Mark as taken dragged left-to-right (slideToLeft = false) for natural affirmative gesture
                    SlideToConfirmButton(
                        text = slideTakeText,
                        icon = if (isDetailsMasked) Icons.Default.LockOpen else Icons.Default.Check,
                        trackColor = Color(0xFF10B981),
                        thumbColor = Color(0xFF10B981),
                        slideToLeft = false,
                        onConfirmed = effectiveTakeAction
                    )
                } else {
                    // Standard Tap Buttons when sliding is turned off in preferences
                    Button(
                        onClick = effectiveTakeAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = if (isDetailsMasked) Icons.Default.LockOpen else Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDetailsMasked) {
                                stringResource(R.string.alarm_unlock_to_view_details)
                            } else if (medicinesList.size > 1) {
                                stringResource(R.string.alarm_take_all, medicinesList.size)
                            } else {
                                stringResource(R.string.alarm_take_medicine)
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            fontSize = 17.sp
                        )
                    }
                }

                // Guard: Dynamic snooze duration selector chips only shown when custom snooze popup preference is enabled
                if (user?.customSnoozePopupEnabled == true) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.alarm_snooze_duration_label),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            snoozeOptions.forEach { minutes ->
                                val isSelected = minutes == selectedSnoozeMinutes
                                Surface(
                                    onClick = { selectedSnoozeMinutes = minutes },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFFF59E0B) else if (isDarkTheme) Color(0xFF262A34) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    border = if (isSelected) null else BorderStroke(1.dp, if (isDarkTheme) Color(0xFF3B404E) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.snooze_duration_chip_format, minutes),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (useSlideActions) {
                    val slideSnoozeText = stringResource(R.string.alarm_slide_to_snooze, selectedSnoozeMinutes)
                    // Guard: Snooze dragged right-to-left (slideToLeft = true)
                    SlideToConfirmButton(
                        text = slideSnoozeText,
                        icon = Icons.Default.Snooze,
                        trackColor = Color(0xFFF59E0B),
                        thumbColor = Color(0xFFF59E0B),
                        slideToLeft = true,
                        onConfirmed = { onSnoozeAction(selectedSnoozeMinutes) }
                    )
                } else {
                    Button(
                        onClick = { onSnoozeAction(selectedSnoozeMinutes) },
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
                            text = stringResource(R.string.alarm_snooze_format, selectedSnoozeMinutes),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            fontSize = 17.sp
                        )
                    }
                }

                // Opt-in Skip Dose Button (Outlined)
                if (user?.allowDoseSkipping == true) {
                    OutlinedButton(
                        onClick = {
                            if (isDetailsMasked) onUnlockRequest() else showSkipReasonDialog = true
                        },
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
        SkipReasonDialog(
            onConfirm = { reason ->
                showSkipReasonDialog = false
                onSkipAction(reason)
            },
            onDismiss = { showSkipReasonDialog = false }
        )
    }
}
