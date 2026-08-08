package com.example.dosezy.ui.subscreens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dosezy.R
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.viewmodels.UserViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(navController: NavController) {
    val userViewModel: UserViewModel = com.example.dosezy.utils.sharedUserViewModel()
    val currentUser by userViewModel.currentUser.collectAsState()

    // Fetch all schedule entries for current user
    val scheduleEntries by produceState<List<ScheduleEntry>>(initialValue = emptyList(), key1 = currentUser?.userId) {
        currentUser?.userId?.let { uid ->
            userViewModel.scheduleRepository.getScheduleForUser(uid).collect { list ->
                value = list
            }
        }
    }

    // Adherence computations
    val totalEntries = scheduleEntries.size
    val takenOnTime = scheduleEntries.count { it.status == MedicationStatus.TAKEN_ON_TIME }
    val takenLate = scheduleEntries.count { it.status == MedicationStatus.TAKEN_LATE }
    val missed = scheduleEntries.count { it.status == MedicationStatus.MISSED }
    val totalTaken = takenOnTime + takenLate
    val totalDecided = totalTaken + missed

    val adherenceRate = if (totalDecided > 0) {
        ((totalTaken.toDouble() / totalDecided.toDouble()) * 100).toInt()
    } else if (totalEntries > 0) {
        100
    } else {
        0
    }

    // Earliest recorded entry date or today
    val earliestDate = scheduleEntries.minByOrNull { it.scheduledDateTime }?.scheduledDateTime?.toLocalDate()
    val today = LocalDate.now()
    val sinceString = if (earliestDate != null) {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        stringResource(R.string.analytics_using_since, earliestDate.format(formatter))
    } else {
        stringResource(R.string.analytics_using_today)
    }

    // Past 7 days calculation for mini bar chart
    val past7Days = (6 downTo 0).map { daysAgo ->
        val date = today.minusDays(daysAgo.toLong())
        val dayEntries = scheduleEntries.filter { it.scheduledDateTime.toLocalDate() == date }
        val dayTaken = dayEntries.count { it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE }
        val dayMissed = dayEntries.count { it.status == MedicationStatus.MISSED }
        val dayDecided = dayTaken + dayMissed
        val dayRate = if (dayDecided > 0) ((dayTaken.toDouble() / dayDecided.toDouble()) * 100).toInt() else if (dayEntries.isNotEmpty()) 100 else 0
        DayStat(
            date = date,
            dayLabel = date.dayOfWeek.name.take(3),
            total = dayEntries.size,
            taken = dayTaken,
            rate = dayRate
        )
    }

    Scaffold(
        topBar = {
            TopBar(
                navController = navController,
                currentUser = currentUser,
                title = stringResource(R.string.analytics_title),
                showBackButton = true,
                actions = {}
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 1. Primary Adherence Hero Card
            item {
                AdherenceHeroCard(
                    adherenceRate = adherenceRate,
                    totalTaken = totalTaken,
                    totalDecided = totalDecided,
                    sinceText = sinceString
                )
            }

            // 2. 2x2 Metric Breakdown Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.analytics_taken_on_time),
                            value = "$takenOnTime",
                            subtitle = if (totalDecided > 0) "${((takenOnTime.toDouble() / totalDecided) * 100).toInt()}%" else "0%",
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF10B981),
                            bgColor = Color(0xFF10B981).copy(alpha = 0.12f)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.analytics_taken_late),
                            value = "$takenLate",
                            subtitle = if (totalDecided > 0) "${((takenLate.toDouble() / totalDecided) * 100).toInt()}%" else "0%",
                            icon = Icons.Default.HourglassTop,
                            color = Color(0xFFF59E0B),
                            bgColor = Color(0xFFF59E0B).copy(alpha = 0.12f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.analytics_missed_doses),
                            value = "$missed",
                            subtitle = if (totalDecided > 0) "${((missed.toDouble() / totalDecided) * 100).toInt()}%" else "0%",
                            icon = Icons.Default.AlarmOff,
                            color = Color(0xFFEF4444),
                            bgColor = Color(0xFFEF4444).copy(alpha = 0.12f)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.analytics_total_doses),
                            value = "$totalEntries",
                            subtitle = stringResource(R.string.status_taken) + ": $totalTaken",
                            icon = Icons.Default.Medication,
                            color = Color(0xFF0277BD),
                            bgColor = Color(0xFF0277BD).copy(alpha = 0.12f)
                        )
                    }
                }
            }

            // 3. Weekly Adherence Trend Visual
            item {
                WeeklyTrendCard(past7Days = past7Days)
            }

            // 4. Personalized Insights Banner
            item {
                InsightCard(
                    adherenceRate = adherenceRate,
                    totalEntries = totalEntries
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun AdherenceHeroCard(
    adherenceRate: Int,
    totalTaken: Int,
    totalDecided: Int,
    sinceText: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Circular Progress Ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                // Background Track
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.size(130.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    strokeWidth = 12.dp
                )
                // Active Adherence Ring
                CircularProgressIndicator(
                    progress = (adherenceRate / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.size(130.dp),
                    color = if (adherenceRate >= 80) Color(0xFF10B981) else if (adherenceRate >= 50) Color(0xFFF59E0B) else Color(0xFF0277BD),
                    strokeWidth = 12.dp,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$adherenceRate%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.analytics_adherence_rate),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.analytics_adherence_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle: Using Dosezy since...
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ) {
                Text(
                    text = sinceText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

data class DayStat(
    val date: LocalDate,
    val dayLabel: String,
    val total: Int,
    val taken: Int,
    val rate: Int
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeeklyTrendCard(past7Days: List<DayStat>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.analytics_weekly_overview),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                past7Days.forEach { stat ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (stat.total > 0) "${stat.rate}%" else "-",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stat.rate >= 80) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Vertical Bar
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            val barFill = if (stat.total > 0) (stat.rate / 100f).coerceIn(0.15f, 1f) else 0f
                            if (barFill > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((56 * barFill).dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(
                                            if (stat.rate >= 80) Color(0xFF10B981)
                                            else if (stat.rate >= 50) Color(0xFFF59E0B)
                                            else Color(0xFF0277BD)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stat.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = if (stat.date == LocalDate.now()) FontWeight.Bold else FontWeight.Normal,
                            color = if (stat.date == LocalDate.now()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InsightCard(adherenceRate: Int, totalEntries: Int) {
    val (insightText, insightColor) = when {
        totalEntries == 0 -> Pair(
            stringResource(R.string.analytics_insights_start),
            Color(0xFF0277BD)
        )
        adherenceRate >= 80 -> Pair(
            stringResource(R.string.analytics_insights_excellent),
            Color(0xFF10B981)
        )
        else -> Pair(
            stringResource(R.string.analytics_insights_good),
            Color(0xFFF59E0B)
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = insightColor.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, insightColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(insightColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = null,
                    tint = insightColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.analytics_insights_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = insightText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
