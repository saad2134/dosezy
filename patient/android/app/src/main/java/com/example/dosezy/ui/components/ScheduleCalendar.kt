package com.example.dosezy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.ScheduleWithMedicine
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.ui.screens.MedicineImage
import com.example.dosezy.utils.TimeFormatUtils
import com.example.dosezy.utils.TimeCalculationUtils
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun ScheduleCalendar(
    selectedDate: LocalDate,
    scheduleEntries: List<ScheduleEntry>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }

    // Guard: Synchronize currentMonth when selectedDate changes externally (e.g. "Jump to Today" or quick date picker) to prevent stale month display
    androidx.compose.runtime.LaunchedEffect(selectedDate) {
        val targetMonth = YearMonth.from(selectedDate)
        if (currentMonth != targetMonth) {
            currentMonth = targetMonth
        }
    }

    val userViewModel: com.example.dosezy.ui.viewmodels.UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
    val currentUser by userViewModel.currentUser.collectAsState()
    val targetLocale = remember(currentUser?.language) {
        com.example.dosezy.utils.LocaleHelper.getLocale(currentUser?.language ?: com.example.dosezy.data.model.Language.SYSTEM)
    }
    val firstDayOfWeek = remember(targetLocale) {
        java.time.temporal.WeekFields.of(targetLocale).firstDayOfWeek
    }

    Column(modifier = modifier) {
        // Month Navigation
        MonthNavigation(
            currentMonth = currentMonth,
            targetLocale = targetLocale,
            onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
            onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
        )

        // Weekday headers
        WeekdayHeaders(
            firstDayOfWeek = firstDayOfWeek,
            targetLocale = targetLocale
        )

        // Calendar grid
        CalendarGrid(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            scheduleEntries = scheduleEntries,
            missedAfterHours = currentUser?.considerMissedAfter ?: 6,
            thickerHighlight = currentUser?.thickerCalendarDayHighlight == true,
            firstDayOfWeek = firstDayOfWeek,
            onDateSelected = { date ->
                onDateSelected(date)
                currentMonth = YearMonth.from(date)
            }
        )
    }
}

@Composable
private fun MonthNavigation(
    currentMonth: YearMonth,
    targetLocale: java.util.Locale,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous month"
            )
        }

        val monthDisplay = currentMonth.month.getDisplayName(java.time.format.TextStyle.FULL, targetLocale).replaceFirstChar { it.titlecase(targetLocale) }

        Text(
            text = "$monthDisplay ${currentMonth.year}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next month"
            )
        }
    }
}

@Composable
private fun WeekdayHeaders(
    firstDayOfWeek: java.time.DayOfWeek = java.time.temporal.WeekFields.of(java.util.Locale.getDefault()).firstDayOfWeek,
    targetLocale: java.util.Locale = java.util.Locale.getDefault(),
    modifier: Modifier = Modifier
) {
    val weekdays = remember(firstDayOfWeek) {
        (0..6).map { firstDayOfWeek.plus(it.toLong()) }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        weekdays.forEach { day ->
            val shortInitial = day.getDisplayName(java.time.format.TextStyle.NARROW, targetLocale)
            val fullDayName = day.getDisplayName(java.time.format.TextStyle.FULL, targetLocale)
            Text(
                text = shortInitial,
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .semantics { contentDescription = fullDayName },
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    scheduleEntries: List<ScheduleEntry>,
    onDateSelected: (LocalDate) -> Unit,
    missedAfterHours: Int = 6,
    thickerHighlight: Boolean = false,
    firstDayOfWeek: java.time.DayOfWeek = java.time.temporal.WeekFields.of(java.util.Locale.getDefault()).firstDayOfWeek,
    modifier: Modifier = Modifier
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val startOffset = (firstDayOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7

    // Guard: Pre-group schedule entries by LocalDate once per schedule list update to avoid O(N * 42) filter iterations across all calendar cells on every recomposition frame
    val entriesByDate = remember(scheduleEntries) {
        scheduleEntries.groupBy { it.scheduledDateTime.toLocalDate() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Create rows for the calendar
        var dayCounter = 1 - startOffset

        repeat(6) { weekIndex -> // Maximum 6 weeks in a month
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(7) { dayIndex ->
                    val currentDay = dayCounter
                    val date = if (currentDay in 1..daysInMonth) {
                        currentMonth.atDay(currentDay)
                    } else {
                        null
                    }

                    val dayEntries = date?.let { currentDate ->
                        entriesByDate[currentDate] ?: emptyList()
                    } ?: emptyList()

                    val statusColor = getDateStatusColor(dayEntries, missedAfterHours)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                    ) {
                        if (date != null && currentDay in 1..daysInMonth) {
                            CalendarDay(
                                day = currentDay,
                                date = date,
                                isSelected = date == selectedDate,
                                statusColor = statusColor,
                                thickerHighlight = thickerHighlight,
                                onClick = { onDateSelected(date) }
                            )
                        } else if (currentDay > 0 && currentDay <= daysInMonth + 7) {
                            // Empty cell for days outside current month
                            Text(
                                text = if (currentDay > 0 && currentDay <= daysInMonth) currentDay.toString() else "",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(enabled = false) {},
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    dayCounter++
                }
            }
        }
    }
}

// Guard: When thickerCalendarDayHighlight is enabled, every scheduled day receives a full colored frame instead of only an underline
@Composable
private fun CalendarDay(
    day: Int,
    date: LocalDate,
    isSelected: Boolean,
    statusColor: Color,
    thickerHighlight: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isColored = statusColor != Color.Transparent
    val backgroundColor = when {
        isSelected -> Color(0xFF2084E4)
        thickerHighlight && isColored -> statusColor.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    val cellBorder = when {
        isSelected && thickerHighlight && isColored -> BorderStroke(2.5.dp, statusColor)
        thickerHighlight && isColored -> BorderStroke(2.dp, statusColor)
        else -> null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .then(if (cellBorder != null) Modifier.border(cellBorder, RoundedCornerShape(12.dp)) else Modifier)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.toString(),
                color = textColor,
                fontSize = 21.sp,
                fontWeight = if (thickerHighlight && isColored) FontWeight.ExtraBold else FontWeight.Bold
            )

            if (isColored && !thickerHighlight) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isSelected) Color.White else statusColor)
                )
            } else if (isColored && isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White)
                )
            }
        }
    }
}

private fun getDateStatusColor(entries: List<ScheduleEntry>, missedAfterHours: Int = 6): Color {
    if (entries.isEmpty()) return Color.Transparent

    val hasMissed = entries.any { it.status == MedicationStatus.MISSED }
    val hasLate = entries.any { it.status == MedicationStatus.TAKEN_LATE }
    val allCompleted = entries.all {
        it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE || it.status == MedicationStatus.SKIPPED
    }
    val hasTaken = entries.any {
        it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE
    }
    val allSkipped = entries.all { it.status == MedicationStatus.SKIPPED }

    // Guard: Only flag pending doses as missed (red) after considerMissedAfter threshold; prevents 1-minute-old pending doses from turning the calendar day red
    val now = java.time.LocalDateTime.now()
    val hasPastPending = entries.any {
        it.status == MedicationStatus.PENDING &&
                now.isAfter(it.scheduledDateTime) &&
                TimeCalculationUtils.isMissed(it.scheduledDateTime, now, missedAfterHours)
    }

    return when {
        hasMissed || hasPastPending -> Color(0xFFEF4444) // Red
        hasLate -> Color(0xFFF59E0B) // Amber
        allCompleted && hasTaken -> Color(0xFF10B981) // Green
        allSkipped -> Color(0xFF9CA3AF) // Slate Gray for skipped (neutral)
        else -> Color(0xFF6B7280) // Gray for pending
    }
}

// In ScheduleCalendar.kt, update the ScheduleList composable:
@Composable
fun ScheduleList(
    scheduleWithMedicine: List<ScheduleWithMedicine>,
    timeFormat: TimeFormat, // time format parameter
    onMarkAsTaken: (String, String) -> Unit,
    onMarkAsLate: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ) {
        items(scheduleWithMedicine, key = { it.scheduleEntry.entryId }) { item ->
            ScheduleListItem(
                scheduleWithMedicine = item,
                timeFormat = timeFormat, // Pass to list item
                onMarkAsTaken = onMarkAsTaken,
                onMarkAsLate = onMarkAsLate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ScheduleListItem(
    scheduleWithMedicine: ScheduleWithMedicine,
    timeFormat: TimeFormat, // time format parameter
    targetLocale: java.util.Locale = java.util.Locale.getDefault(),
    missedAfter: Int = 6,
    modifier: Modifier = Modifier,
    onMarkAsTaken: ((String, String) -> Unit)? = null,
    onMarkAsLate: ((String, String) -> Unit)? = null
) {
    val entry = scheduleWithMedicine.scheduleEntry
    val medicine = scheduleWithMedicine.medicine

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Medication Icon with image & pill visual support
        MedicineImage(
            imageUri = medicine?.imageUri,
            pillShape = medicine?.pillShape,
            pillColor = medicine?.pillColor,
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Medication Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = medicine?.medicationName ?: "Unknown Medicine",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!medicine?.notes.isNullOrBlank()) {
                Text(
                    text = "📝 ${medicine?.notes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF59E0B),
                    maxLines = 1
                )
            }
            if (!entry.doseNotes.isNullOrBlank()) {
                Text(
                    text = "💬 ${entry.doseNotes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
            Text(
                text = medicine?.getDosageDisplay(entry.scheduledDateTime.toLocalTime()) ?: "Unknown dosage",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // TimeFormatUtils to format time according to user preference & locale
            Text(
                text = TimeFormatUtils.formatTime(entry.scheduledDateTime, timeFormat, targetLocale),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Status Indicator (Read-only status overview on Schedule page)
        val now = java.time.LocalDateTime.now()
        val isPassed = now.isAfter(entry.scheduledDateTime)
        
        val resolvedStatus = when {
            entry.status == MedicationStatus.TAKEN_ON_TIME || entry.status == MedicationStatus.TAKEN_LATE -> entry.status
            entry.status == MedicationStatus.SKIPPED -> MedicationStatus.SKIPPED
            entry.status == MedicationStatus.MISSED -> MedicationStatus.MISSED
            isPassed && TimeCalculationUtils.isMissed(entry.scheduledDateTime, now, missedAfter) -> MedicationStatus.MISSED
            else -> MedicationStatus.PENDING
        }

        val (icon, color) = when (resolvedStatus) {
            MedicationStatus.TAKEN_ON_TIME ->
                Pair(Icons.Default.Done, MaterialTheme.colorScheme.primary)
            MedicationStatus.TAKEN_LATE ->
                Pair(Icons.Default.Done, MaterialTheme.colorScheme.tertiary)
            MedicationStatus.SKIPPED ->
                Pair(Icons.Default.FastForward, MaterialTheme.colorScheme.outline)
            MedicationStatus.MISSED ->
                Pair(Icons.Default.Close, MaterialTheme.colorScheme.error)
            MedicationStatus.PENDING ->
                Pair(Icons.Default.HorizontalRule, MaterialTheme.colorScheme.onSurfaceVariant)
        }

        val statusDescription = when (resolvedStatus) {
            MedicationStatus.TAKEN_ON_TIME -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.status_taken)
            MedicationStatus.TAKEN_LATE -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.analytics_taken_late)
            MedicationStatus.SKIPPED -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.home_action_skipped)
            MedicationStatus.MISSED -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.home_action_missed)
            MedicationStatus.PENDING -> androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.status_pending)
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = statusDescription,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@Composable
private fun StatusItem(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(color.copy(alpha = 0.2f), MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = color
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = color.copy(alpha = 0.8f)
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = color
        )
    }
}