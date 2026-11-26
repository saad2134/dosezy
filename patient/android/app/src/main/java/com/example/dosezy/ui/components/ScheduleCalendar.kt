package com.example.dosezy.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dosezy.R
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.ScheduleWithMedicine
import com.example.dosezy.data.model.TimeFormat
import com.example.dosezy.ui.screens.MedicineImage
import com.example.dosezy.utils.TimeFormatUtils
import com.example.dosezy.utils.TimeCalculationUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

@Composable
fun ScheduleCalendar(
    selectedDate: LocalDate,
    scheduleEntries: List<ScheduleEntry>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    // Guard: Default to week strip view with one-tap month view toggle to prevent calendar from consuming half the screen (#F07)
    var isMonthView by rememberSaveable { mutableStateOf(false) }
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }

    val userViewModel: com.example.dosezy.ui.viewmodels.UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
    val currentUser by userViewModel.currentUser.collectAsState()
    val targetLocale = remember(currentUser?.language) {
        com.example.dosezy.utils.LocaleHelper.getLocale(currentUser?.language ?: com.example.dosezy.data.model.Language.SYSTEM)
    }
    val firstDayOfWeek = remember(targetLocale) {
        WeekFields.of(targetLocale).firstDayOfWeek
    }

    var currentWeekStart by remember(firstDayOfWeek) {
        mutableStateOf(selectedDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek)))
    }

    // Guard: Synchronize currentMonth and currentWeekStart when selectedDate changes externally (e.g. "Jump to Today" or quick date picker) to prevent stale calendar display
    androidx.compose.runtime.LaunchedEffect(selectedDate, firstDayOfWeek) {
        val targetMonth = YearMonth.from(selectedDate)
        if (currentMonth != targetMonth) {
            currentMonth = targetMonth
        }
        val targetWeekStart = selectedDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        if (currentWeekStart != targetWeekStart) {
            currentWeekStart = targetWeekStart
        }
    }

    val displayedYearMonth = if (isMonthView) {
        currentMonth
    } else {
        YearMonth.from(currentWeekStart.plusDays(3))
    }

    val monthDisplay = displayedYearMonth.month.getDisplayName(java.time.format.TextStyle.FULL, targetLocale)
        .replaceFirstChar { it.titlecase(targetLocale) }
    val title = "$monthDisplay ${displayedYearMonth.year}"

    Column(modifier = modifier) {
        // Navigation Header with One-Tap View Mode Toggle
        CalendarNavigation(
            title = title,
            isMonthView = isMonthView,
            prevDescription = stringResource(if (isMonthView) R.string.calendar_prev_month else R.string.calendar_prev_week),
            nextDescription = stringResource(if (isMonthView) R.string.calendar_next_month else R.string.calendar_next_week),
            onPrevious = {
                if (isMonthView) {
                    currentMonth = currentMonth.minusMonths(1)
                } else {
                    currentWeekStart = currentWeekStart.minusWeeks(1)
                    currentMonth = YearMonth.from(currentWeekStart.plusDays(3))
                }
            },
            onNext = {
                if (isMonthView) {
                    currentMonth = currentMonth.plusMonths(1)
                } else {
                    currentWeekStart = currentWeekStart.plusWeeks(1)
                    currentMonth = YearMonth.from(currentWeekStart.plusDays(3))
                }
            },
            onToggleViewMode = {
                if (!isMonthView) {
                    currentMonth = YearMonth.from(currentWeekStart.plusDays(3))
                } else {
                    currentWeekStart = selectedDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
                }
                isMonthView = !isMonthView
            }
        )

        // Weekday headers
        WeekdayHeaders(
            firstDayOfWeek = firstDayOfWeek,
            targetLocale = targetLocale
        )

        // Calendar Body: Compact Week Strip (default) or Full Month Grid with smooth animation
        AnimatedContent(
            targetState = isMonthView,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "calendar_view_mode"
        ) { monthViewActive ->
            if (monthViewActive) {
                CalendarGrid(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    scheduleEntries = scheduleEntries,
                    missedAfterMinutes = currentUser?.missedAfterMinutes ?: 360,
                    thickerHighlight = currentUser?.thickerCalendarDayHighlight == true,
                    firstDayOfWeek = firstDayOfWeek,
                    onDateSelected = { date ->
                        onDateSelected(date)
                        currentMonth = YearMonth.from(date)
                        currentWeekStart = date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
                    }
                )
            } else {
                WeekGrid(
                    weekStartDate = currentWeekStart,
                    selectedDate = selectedDate,
                    scheduleEntries = scheduleEntries,
                    missedAfterMinutes = currentUser?.missedAfterMinutes ?: 360,
                    thickerHighlight = currentUser?.thickerCalendarDayHighlight == true,
                    onDateSelected = { date ->
                        onDateSelected(date)
                        currentMonth = YearMonth.from(date)
                    }
                )
            }
        }
    }
}

@Composable
private fun CalendarNavigation(
    title: String,
    isMonthView: Boolean,
    prevDescription: String,
    nextDescription: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleViewMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = prevDescription
            )
        }

        // Center: Clickable Month/Year Title with expand/collapse chevron
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(if (isMonthView) R.string.calendar_view_week else R.string.calendar_view_month),
                    onClick = onToggleViewMode
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (isMonthView) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // View Mode Toggle Pill (One-tap Month/Week)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.clickable(onClick = onToggleViewMode)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isMonthView) Icons.Default.CalendarViewWeek else Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isMonthView) stringResource(R.string.calendar_view_week) else stringResource(R.string.calendar_view_month),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = nextDescription
                )
            }
        }
    }
}

@Composable
private fun WeekdayHeaders(
    firstDayOfWeek: java.time.DayOfWeek = WeekFields.of(java.util.Locale.getDefault()).firstDayOfWeek,
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
private fun WeekGrid(
    weekStartDate: LocalDate,
    selectedDate: LocalDate,
    scheduleEntries: List<ScheduleEntry>,
    onDateSelected: (LocalDate) -> Unit,
    missedAfterMinutes: Int = 360,
    thickerHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val entriesByDate = remember(scheduleEntries) {
        scheduleEntries.filter { !it.entryId.startsWith("PRN_") }.groupBy { it.scheduledDateTime.toLocalDate() }
    }

    val daysOfWeek = remember(weekStartDate) {
        (0..6).map { weekStartDate.plusDays(it.toLong()) }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        daysOfWeek.forEach { date ->
            val dayEntries = entriesByDate[date] ?: emptyList()
            val statusColor = getDateStatusColor(dayEntries, missedAfterMinutes)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(2.dp)
            ) {
                CalendarDay(
                    day = date.dayOfMonth,
                    isSelected = date == selectedDate,
                    statusColor = statusColor,
                    thickerHighlight = thickerHighlight,
                    onClick = { onDateSelected(date) }
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    scheduleEntries: List<ScheduleEntry>,
    onDateSelected: (LocalDate) -> Unit,
    missedAfterMinutes: Int = 360,
    thickerHighlight: Boolean = false,
    firstDayOfWeek: java.time.DayOfWeek = WeekFields.of(java.util.Locale.getDefault()).firstDayOfWeek,
    modifier: Modifier = Modifier
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val startOffset = (firstDayOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7

    val entriesByDate = remember(scheduleEntries) {
        scheduleEntries.filter { !it.entryId.startsWith("PRN_") }.groupBy { it.scheduledDateTime.toLocalDate() }
    }

    val totalSlots = startOffset + daysInMonth
    val numWeeks = kotlin.math.ceil(totalSlots / 7.0).toInt().coerceIn(4, 6)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        var dayCounter = 1 - startOffset

        repeat(numWeeks) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(7) {
                    val currentDay = dayCounter
                    val date = if (currentDay in 1..daysInMonth) {
                        currentMonth.atDay(currentDay)
                    } else {
                        null
                    }

                    val dayEntries = date?.let { currentDate ->
                        entriesByDate[currentDate] ?: emptyList()
                    } ?: emptyList()

                    val statusColor = getDateStatusColor(dayEntries, missedAfterMinutes)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                    ) {
                        if (date != null && currentDay in 1..daysInMonth) {
                            CalendarDay(
                                day = currentDay,
                                isSelected = date == selectedDate,
                                statusColor = statusColor,
                                thickerHighlight = thickerHighlight,
                                onClick = { onDateSelected(date) }
                            )
                        } else {
                            Spacer(modifier = Modifier.fillMaxSize())
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                color = textColor,
                fontSize = 20.sp,
                fontWeight = if (thickerHighlight && isColored) FontWeight.ExtraBold else FontWeight.Bold
            )

            // Guard: Always reserve indicator bar slot (2dp spacer + 4dp bar) so all day numbers across all rows remain 100% horizontally aligned whether or not a day has an indicator (#F07)
            Spacer(modifier = Modifier.height(2.dp))
            val showIndicatorBar = isColored && (!thickerHighlight || isSelected)
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            showIndicatorBar && isSelected -> Color.White
                            showIndicatorBar -> statusColor
                            else -> Color.Transparent
                        }
                    )
            )
        }
    }
}

// Guard: Exclude ad-hoc PRN doses from status color resolution to prevent as-needed doses skewing scheduled adherence (#BUG-CALENDAR-PRN)
internal fun getDateStatusColor(entries: List<ScheduleEntry>, missedAfterMinutes: Int = 360): Color {
    val scheduledEntries = entries.filter { !it.entryId.startsWith("PRN_") }
    if (scheduledEntries.isEmpty()) return Color.Transparent

    val hasMissed = scheduledEntries.any { it.status == MedicationStatus.MISSED }
    val hasLate = scheduledEntries.any { it.status == MedicationStatus.TAKEN_LATE }
    val allCompleted = scheduledEntries.all {
        it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE || it.status == MedicationStatus.SKIPPED
    }
    val hasTaken = scheduledEntries.any {
        it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE
    }
    val allSkipped = scheduledEntries.all { it.status == MedicationStatus.SKIPPED }

    // Guard: Only flag pending doses as missed (red) after considerMissedAfter threshold; prevents 1-minute-old pending doses from turning the calendar day red
    val now = java.time.LocalDateTime.now()
    val hasPastPending = scheduledEntries.any {
        it.status == MedicationStatus.PENDING &&
                now.isAfter(it.scheduledDateTime) &&
                TimeCalculationUtils.isMissed(it.scheduledDateTime, now, missedAfterMinutes)
    }
    val hasUpcomingPending = scheduledEntries.any { it.status == MedicationStatus.PENDING }

    // Guard: Prioritize pending doses ahead of partial hasLate to prevent partially complete days from rendering Amber settled indicators while untaken doses remain (#BUG-CALENDAR-PARTIAL-PENDING)
    return when {
        hasMissed || hasPastPending -> Color(0xFFEF4444) // Red
        allCompleted && hasLate -> Color(0xFFF59E0B) // Amber (all doses resolved, with late intake)
        allCompleted && hasTaken -> Color(0xFF10B981) // Green (all doses resolved on time)
        allSkipped -> Color(0xFF9CA3AF) // Slate Gray for skipped (neutral)
        hasUpcomingPending -> Color(0xFF6B7280) // Gray for pending / upcoming doses
        hasLate -> Color(0xFFF59E0B) // Amber fallback if day is otherwise resolved
        else -> Color(0xFF6B7280) // Gray for pending
    }
}

// In ScheduleCalendar.kt, update the ScheduleList composable:
@Composable
fun ScheduleList(
    scheduleWithMedicine: List<ScheduleWithMedicine>,
    timeFormat: TimeFormat, // time format parameter
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ) {
        items(scheduleWithMedicine, key = { it.scheduleEntry.entryId }) { item ->
            ScheduleListItem(
                scheduleWithMedicine = item,
                timeFormat = timeFormat, // Pass to list item
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
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val entry = scheduleWithMedicine.scheduleEntry
    val medicine = scheduleWithMedicine.medicine

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onClick)
                } else Modifier
            )
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
                // Guard: Use theme-aware high contrast amber (#9A4A00 on light giving >= 5.6:1, #FBBF24 on dark) to satisfy WCAG 2.2 AA (#F10)
                Text(
                    text = "📝 ${medicine?.notes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = com.example.dosezy.ui.theme.getMedicationNoteColor(),
                    maxLines = 1
                )
            }
            // Guard: Cleanly format dose notes, phrasing partial doses as "Took X of Y" without raw debug bracket tags
            val displayNotes = com.example.dosezy.utils.DosageFormatUtils.formatDoseNoteForDisplay(entry.doseNotes)
            val isPartialDose = (entry.status == MedicationStatus.TAKEN_ON_TIME || entry.status == MedicationStatus.TAKEN_LATE) &&
                    (entry.doseNotes?.contains("[Partial Stock:") == true)
            if (!displayNotes.isNullOrBlank()) {
                Text(
                    text = "💬 $displayNotes",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPartialDose) Color(0xFF0284C7) else MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
            Text(
                text = medicine?.getDosageDisplay(entry.scheduledDateTime.toLocalTime(), entry.dosage) ?: "Unknown dosage",
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

        // Status Indicator: Clear set of states (Taken, Late, Missed, Partial, Upcoming, Skipped) with distinct icon, colour and word
        val now = java.time.LocalDateTime.now()
        val isPassed = now.isAfter(entry.scheduledDateTime)
        val isTaken = entry.status == MedicationStatus.TAKEN_ON_TIME || entry.status == MedicationStatus.TAKEN_LATE
        val isSkipped = entry.status == MedicationStatus.SKIPPED
        val isMissed = entry.status == MedicationStatus.MISSED || (!isTaken && !isSkipped && isPassed && TimeCalculationUtils.isMissed(entry.scheduledDateTime, now, missedAfter))
        val isPartial = isTaken && (entry.doseNotes?.contains("[Partial Stock:") == true)
        val isLate = (entry.status == MedicationStatus.TAKEN_LATE && !isPartial) || (!isTaken && !isSkipped && isPassed && !isMissed)

        val (icon, color, statusText) = when {
            isPartial -> Triple(
                Icons.Default.PieChart,
                Color(0xFF0284C7), // Cyan / Blue
                androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.status_partial)
            )
            isTaken && !isLate -> Triple(
                Icons.Default.Done,
                Color(0xFF10B981), // Emerald Green
                androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.status_taken)
            )
            isLate -> Triple(
                Icons.Default.HourglassTop,
                Color(0xFFF59E0B), // Warm Amber
                androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.status_late)
            )
            isMissed -> Triple(
                Icons.Default.Close,
                Color(0xFFEF4444), // Rose Red
                androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.status_missed)
            )
            isSkipped -> Triple(
                Icons.Default.FastForward,
                Color(0xFF94A3B8), // Muted Slate
                androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.status_skipped)
            )
            else -> Triple(
                Icons.Default.Upcoming,
                Color(0xFF64748B), // Slate / Muted
                androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.status_upcoming)
            )
        }

        val isLocked = entry.scheduledDateTime.isBefore(now.minusHours(48))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status badge: distinct squircle pill with icon, colour and word
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }

            if (onClick != null) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.ChevronRight,
                    contentDescription = if (isLocked) {
                        androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.schedule_dose_locked_badge)
                    } else {
                        androidx.compose.ui.res.stringResource(com.example.dosezy.R.string.schedule_tap_to_manage)
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isLocked) 0.4f else 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
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