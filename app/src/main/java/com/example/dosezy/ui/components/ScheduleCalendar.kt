package com.example.dosezy.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.ScheduleWithMedicine
import com.example.dosezy.ui.screens.MedicineImage
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleCalendar(
    selectedDate: LocalDate,
    scheduleEntries: List<ScheduleEntry>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }

    Column(modifier = modifier) {
        // Month Navigation
        MonthNavigation(
            currentMonth = currentMonth,
            onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
            onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
        )

        // Weekday headers
        WeekdayHeaders()

        // Calendar grid
        CalendarGrid(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            scheduleEntries = scheduleEntries,
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

        Text(
            text = currentMonth.month.toString() + " " + currentMonth.year.toString(),
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
private fun WeekdayHeaders(modifier: Modifier = Modifier) {
    val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        weekdays.forEach { day ->
            Text(
                text = day,
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B)
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
    modifier: Modifier = Modifier
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val startOffset = (firstDayOfMonth.dayOfWeek.value % 7) // Sunday = 0

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
                    val date = try {
                        currentMonth.atDay(currentDay)
                    } catch (e: Exception) {
                        null
                    }

                    // FIXED: Use a different variable name to avoid conflict
                    val dayEntries = date?.let { currentDate ->
                        scheduleEntries.filter { entry ->
                            entry.scheduledDateTime.toLocalDate() == currentDate
                        }
                    } ?: emptyList()

                    val statusColor = getDateStatusColor(dayEntries)

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
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    dayCounter++
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    day: Int,
    date: LocalDate,
    isSelected: Boolean,
    statusColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color(0xFF2084E4) else Color.Transparent
    val textColor = if (isSelected) Color.White else Color(0xFF0D171B)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(CircleShape)
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
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            if (statusColor != Color.Transparent) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }
        }
    }
}

private fun getDateStatusColor(entries: List<ScheduleEntry>): Color {
    if (entries.isEmpty()) return Color.Transparent

    val hasMissed = entries.any { it.status == MedicationStatus.MISSED }
    val hasLate = entries.any { it.status == MedicationStatus.TAKEN_LATE }
    val allTaken = entries.all {
        it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE
    }

    return when {
        hasMissed -> Color(0xFFEF4444) // Red
        hasLate -> Color(0xFFF59E0B) // Amber
        allTaken -> Color(0xFF10B981) // Green
        else -> Color(0xFF6B7280) // Gray for pending
    }
}

@Composable
fun ScheduleList(
    scheduleWithMedicine: List<ScheduleWithMedicine>,
    onMarkAsTaken: (String, String) -> Unit,
    onMarkAsLate: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // FIXED: Add constraints to LazyColumn
    LazyColumn(
        modifier = modifier
    ) {
        items(scheduleWithMedicine) { item ->
            ScheduleListItem(
                scheduleWithMedicine = item,
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
    onMarkAsTaken: (String, String) -> Unit,
    onMarkAsLate: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val entry = scheduleWithMedicine.scheduleEntry
    val medicine = scheduleWithMedicine.medicine

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Medication Icon with image support - FIXED
        MedicineImage(
            imageUri = medicine?.imageUri,
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
                color = Color(0xFF0D171B)
            )
            Text(
                text = medicine?.getDosageDisplay() ?: "Unknown dosage",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )
            Text(
                text = entry.scheduledDateTime.format(
                    DateTimeFormatter.ofPattern("h:mm a")
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Status Button
        val (icon, color, onClick) = when (entry.status) {
            MedicationStatus.TAKEN_ON_TIME ->
                Triple(Icons.Default.Done, Color(0xFF10B981), null as (() -> Unit)?)
            MedicationStatus.TAKEN_LATE ->
                Triple(Icons.Default.Done, Color(0xFFF59E0B), null)
            MedicationStatus.MISSED ->
                Triple(Icons.Default.Close, Color(0xFFEF4444), null)
            MedicationStatus.PENDING ->
                Triple(Icons.Default.HorizontalRule, Color(0xFF94A3B8)) {
                    val takenAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    onMarkAsTaken(entry.entryId, takenAt)
                }
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
                .clickable(enabled = onClick != null) { onClick?.invoke() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Status",
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