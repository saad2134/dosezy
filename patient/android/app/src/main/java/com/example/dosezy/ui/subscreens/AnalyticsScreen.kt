package com.example.dosezy.ui.subscreens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

enum class AdherenceRange(val stringResId: Int) {
    ALL(R.string.range_all),
    LAST_7_DAYS(R.string.range_7_days),
    LAST_30_DAYS(R.string.range_30_days),
    SIX_MONTHS(R.string.range_6_months),
    TWELVE_MONTHS(R.string.range_12_months),
    TOTAL(R.string.range_total)
}

data class RangeAdherenceData(
    val range: AdherenceRange,
    val label: String,
    val takenCount: Int,
    val decidedCount: Int,
    val totalEntries: Int,
    val rate: Int
)

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

    val today = LocalDate.now()
    var selectedRange by remember { mutableStateOf(AdherenceRange.TOTAL) }

    fun getRangeEntries(range: AdherenceRange): List<ScheduleEntry> {
        return when (range) {
            AdherenceRange.LAST_7_DAYS -> scheduleEntries.filter { it.scheduledDateTime.toLocalDate() >= today.minusDays(7) }
            AdherenceRange.LAST_30_DAYS -> scheduleEntries.filter { it.scheduledDateTime.toLocalDate() >= today.minusDays(30) }
            AdherenceRange.SIX_MONTHS -> scheduleEntries.filter { it.scheduledDateTime.toLocalDate() >= today.minusMonths(6) }
            AdherenceRange.TWELVE_MONTHS -> scheduleEntries.filter { it.scheduledDateTime.toLocalDate() >= today.minusMonths(12) }
            AdherenceRange.TOTAL -> scheduleEntries
            AdherenceRange.ALL -> scheduleEntries
        }
    }

    fun computeAdherence(range: AdherenceRange, label: String): RangeAdherenceData {
        val entries = getRangeEntries(range)
        val total = entries.size
        val taken = entries.count { it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE }
        val missed = entries.count { it.status == MedicationStatus.MISSED }
        val decided = taken + missed
        val rate = if (decided > 0) ((taken.toDouble() / decided.toDouble()) * 100).toInt() else if (total > 0) 100 else 0
        return RangeAdherenceData(range, label, taken, decided, total, rate)
    }

    val rangeDataList = listOf(
        computeAdherence(AdherenceRange.ALL, stringResource(R.string.range_all)),
        computeAdherence(AdherenceRange.LAST_7_DAYS, stringResource(R.string.range_7_days)),
        computeAdherence(AdherenceRange.LAST_30_DAYS, stringResource(R.string.range_30_days)),
        computeAdherence(AdherenceRange.SIX_MONTHS, stringResource(R.string.range_6_months)),
        computeAdherence(AdherenceRange.TWELVE_MONTHS, stringResource(R.string.range_12_months)),
        computeAdherence(AdherenceRange.TOTAL, stringResource(R.string.range_total))
    )

    val selectedData = rangeDataList.find { it.range == selectedRange } ?: rangeDataList.last()

    // Global overall stats for breakdown grid
    val totalEntries = scheduleEntries.size
    val takenOnTime = scheduleEntries.count { it.status == MedicationStatus.TAKEN_ON_TIME }
    val takenLate = scheduleEntries.count { it.status == MedicationStatus.TAKEN_LATE }
    val missed = scheduleEntries.count { it.status == MedicationStatus.MISSED }
    val totalTaken = takenOnTime + takenLate
    val totalDecided = totalTaken + missed

    // Earliest recorded entry date or today
    val earliestDate = scheduleEntries.minByOrNull { it.scheduledDateTime }?.scheduledDateTime?.toLocalDate()
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

            // 1. Unified Adherence Card combining main progress ring, range selector, and at-a-glance period breakdown
            item {
                UnifiedAdherenceCard(
                    rangeDataList = rangeDataList,
                    selectedRange = selectedRange,
                    selectedData = selectedData,
                    sinceText = sinceString,
                    onSelectRange = { range -> selectedRange = range }
                )
            }

            // 3. 2x2 Metric Breakdown Grid
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

            // 4. Weekly Adherence Trend Visual
            item {
                WeeklyTrendCard(past7Days = past7Days)
            }

            // 5. Personalized Insights Banner
            item {
                InsightCard(
                    adherenceRate = selectedData.rate,
                    totalEntries = totalEntries
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnifiedAdherenceCard(
    rangeDataList: List<RangeAdherenceData>,
    selectedRange: AdherenceRange,
    selectedData: RangeAdherenceData,
    sinceText: String,
    onSelectRange: (AdherenceRange) -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (selectedData.rate / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "AdherenceProgress"
    )

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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Horizontal Segmented Time Range Selector Pills (All Tab at far right)
            val sortedTabs = remember(rangeDataList) {
                rangeDataList.filter { it.range != AdherenceRange.ALL } + rangeDataList.filter { it.range == AdherenceRange.ALL }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sortedTabs.forEach { rData ->
                    val isSelected = rData.range == selectedRange
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onSelectRange(rData.range) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = rData.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedRange == AdherenceRange.ALL) {
                // ALL Tab: Show ONLY mini rings breakdown row ABOVE the sinceText badge
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.adherence_by_period),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rangeDataList.filter { it.range != AdherenceRange.ALL }.forEach { rData ->
                                val ringColor = when {
                                    rData.rate >= 80 -> Color(0xFF10B981)
                                    rData.rate >= 50 -> Color(0xFFF59E0B)
                                    else -> Color(0xFF0277BD)
                                }

                                Surface(
                                    modifier = Modifier
                                        .width(96.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onSelectRange(rData.range) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = rData.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.size(50.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                progress = 1f,
                                                modifier = Modifier.size(50.dp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                                strokeWidth = 5.dp
                                            )
                                            CircularProgressIndicator(
                                                progress = (rData.rate / 100f).coerceIn(0f, 1f),
                                                modifier = Modifier.size(50.dp),
                                                color = ringColor,
                                                strokeWidth = 5.dp,
                                                strokeCap = StrokeCap.Round
                                            )
                                            Text(
                                                text = "${rData.rate}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // Specific Period Tabs: Central Big Progress Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(135.dp)
                ) {
                    CircularProgressIndicator(
                        progress = 1f,
                        modifier = Modifier.size(135.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        strokeWidth = 12.dp
                    )
                    val ringColor = when {
                        selectedData.rate >= 80 -> Color(0xFF10B981)
                        selectedData.rate >= 50 -> Color(0xFFF59E0B)
                        else -> Color(0xFF0277BD)
                    }
                    CircularProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier.size(135.dp),
                        color = ringColor,
                        strokeWidth = 12.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${selectedData.rate}%",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${selectedData.takenCount}/${if (selectedData.decidedCount > 0) selectedData.decidedCount else selectedData.totalEntries}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = selectedData.label + " " + stringResource(R.string.analytics_adherence_rate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.analytics_adherence_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Subtitle: Using Dosezy since...
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ) {
                Text(
                    text = sinceText,
                    style = MaterialTheme.typography.labelSmall,
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
