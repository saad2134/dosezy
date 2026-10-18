package com.example.dosezy.ui.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dosezy.R
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import com.example.dosezy.utils.LocaleHelper
import com.example.dosezy.utils.TimeCalculationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

// Note: Intentional design distinction: ALL displays sub-ring charts breakdown across individual ranges/dosages, whereas TOTAL displays a single consolidated progress ring for all-time adherence.
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
    val takenCount: Int,
    val decidedCount: Int,
    val totalEntries: Int,
    val rate: Int,
    val breakdown: AnalyticsBreakdown = AnalyticsBreakdown()
)

data class DayStat(
    val date: LocalDate,
    val dayLabel: String,
    val total: Int,
    val taken: Int,
    val rate: Int
)

data class AnalyticsBreakdown(
    val totalEntries: Int = 0,
    val takenOnTime: Int = 0,
    val takenLate: Int = 0,
    val missed: Int = 0,
    val skipped: Int = 0,
    val totalTaken: Int = 0,
    val totalDecided: Int = 0
)

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val selectedRange: AdherenceRange = AdherenceRange.TOTAL,
    val rangeDataList: List<RangeAdherenceData> = emptyList(),
    val breakdown: AnalyticsBreakdown = AnalyticsBreakdown(),
    val past7Days: List<DayStat> = emptyList(),
    val earliestDate: LocalDate? = null,
    val currentUser: User? = null
) {
    val selectedData: RangeAdherenceData
        get() = rangeDataList.find { it.range == selectedRange }
            ?: rangeDataList.lastOrNull()
            ?: RangeAdherenceData(selectedRange, 0, 0, 0, 0)
}

@OptIn(ExperimentalCoroutinesApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(AdherenceRange.TOTAL)
    val selectedRange: StateFlow<AdherenceRange> = _selectedRange.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getAllUsers()
                .distinctUntilChanged()
                .collect { users ->
                    val user = users.firstOrNull { it.isCurrentUser }
                    _currentUser.value = user
                }
        }

        viewModelScope.launch {
            // Guard: Decouple range selection from database re-queries so switching tabs selects from in-memory precomputed rangeDataList without cancelling and re-executing SQLite queries
            _currentUser.flatMapLatest { user ->
                if (user == null) {
                    flowOf(AnalyticsUiState(isLoading = false, isEmpty = true, selectedRange = _selectedRange.value))
                } else {
                    scheduleRepository.getScheduleForUser(user.userId).map { entries ->
                        calculateAnalytics(entries, user, _selectedRange.value)
                    }
                }
            }.flowOn(Dispatchers.Default)
            .collect { state ->
                // Guard: Retain active range selection when fresh schedule entries are emitted by Room
                val currentRange = _selectedRange.value
                val matchingBreakdown = state.rangeDataList.find { it.range == currentRange }?.breakdown ?: state.breakdown
                _uiState.value = state.copy(selectedRange = currentRange, breakdown = matchingBreakdown)
            }
        }
    }

    fun selectRange(range: AdherenceRange) {
        _selectedRange.value = range
        // Guard: Synchronously update selectedRange and matching breakdown from precomputed rangeDataList without re-querying SQLite
        val matchingBreakdown = _uiState.value.rangeDataList.find { it.range == range }?.breakdown ?: _uiState.value.breakdown
        _uiState.value = _uiState.value.copy(selectedRange = range, breakdown = matchingBreakdown)
    }

    private fun calculateAnalytics(
        scheduleEntries: List<ScheduleEntry>,
        user: User,
        selectedRange: AdherenceRange
    ): AnalyticsUiState {
        val today = LocalDate.now()
        val now = LocalDateTime.now()
        val missedAfterHours = user.considerMissedAfter
        val targetLocale = LocaleHelper.getLocale(user.language)

        // Guard: Dynamic adherence evaluation for PENDING doses exceeding considerMissedAfter threshold
        fun isEntryMissed(entry: ScheduleEntry): Boolean =
            entry.status == MedicationStatus.MISSED ||
            (entry.status == MedicationStatus.PENDING && now.isAfter(entry.scheduledDateTime) &&
                TimeCalculationUtils.isMissed(entry.scheduledDateTime, now, missedAfterHours))

        fun getRangeEntries(range: AdherenceRange): List<ScheduleEntry> {
            return when (range) {
                // Guard: Align LAST_7_DAYS to today.minusDays(6)..today (exactly 7 days) to match 7-day weekly trend chart cardinality
                AdherenceRange.LAST_7_DAYS -> scheduleEntries.filter {
                    val d = it.scheduledDateTime.toLocalDate()
                    d >= today.minusDays(6) && d <= today
                }
                // Guard: Align LAST_30_DAYS to today.minusDays(29)..today (exactly 30 days) to prevent 31-day off-by-one window expansion
                AdherenceRange.LAST_30_DAYS -> scheduleEntries.filter {
                    val d = it.scheduledDateTime.toLocalDate()
                    d >= today.minusDays(29) && d <= today
                }
                AdherenceRange.SIX_MONTHS -> scheduleEntries.filter {
                    val d = it.scheduledDateTime.toLocalDate()
                    d >= today.minusMonths(6) && d <= today
                }
                AdherenceRange.TWELVE_MONTHS -> scheduleEntries.filter {
                    val d = it.scheduledDateTime.toLocalDate()
                    d >= today.minusMonths(12) && d <= today
                }
                AdherenceRange.TOTAL, AdherenceRange.ALL -> scheduleEntries.filter {
                    it.scheduledDateTime.toLocalDate() <= today
                }
            }
        }

        fun computeAdherence(range: AdherenceRange): RangeAdherenceData {
            // Guard: Exclude ad-hoc PRN doses from scheduled adherence calculation to prevent skewing compliance
            val entries = getRangeEntries(range).filter { !it.entryId.startsWith("PRN_") }
            val total = entries.size
            val takenOnTime = entries.count { it.status == MedicationStatus.TAKEN_ON_TIME }
            val takenLate = entries.count { it.status == MedicationStatus.TAKEN_LATE }
            val taken = takenOnTime + takenLate
            val missed = entries.count { isEntryMissed(it) }
            val skipped = entries.count { it.status == MedicationStatus.SKIPPED }
            // Guard: Factor overdue pending doses on current day into range adherence decided count without double-counting missed entries (#BUG-ANALYTICS-RANGE-OVERDUE)
            val overduePending = entries.count {
                it.scheduledDateTime.toLocalDate() == today &&
                it.status == MedicationStatus.PENDING &&
                it.scheduledDateTime.isBefore(now) &&
                !isEntryMissed(it)
            }
            val decided = taken + missed + overduePending
            val rate = if (decided > 0) ((taken.toDouble() / decided.toDouble()) * 100).toInt() else 0
            // Guard: Compute range-specific breakdown so metric cards dynamically match the selected time window
            val rangeBreakdown = AnalyticsBreakdown(
                totalEntries = total,
                takenOnTime = takenOnTime,
                takenLate = takenLate,
                missed = missed,
                skipped = skipped,
                totalTaken = taken,
                totalDecided = decided
            )
            return RangeAdherenceData(range, taken, decided, total, rate, rangeBreakdown)
        }

        val rangeDataList = listOf(
            computeAdherence(AdherenceRange.ALL),
            computeAdherence(AdherenceRange.LAST_7_DAYS),
            computeAdherence(AdherenceRange.LAST_30_DAYS),
            computeAdherence(AdherenceRange.SIX_MONTHS),
            computeAdherence(AdherenceRange.TWELVE_MONTHS),
            computeAdherence(AdherenceRange.TOTAL)
        )

        val activeBreakdown = rangeDataList.find { it.range == selectedRange }?.breakdown
            ?: rangeDataList.firstOrNull()?.breakdown
            ?: AnalyticsBreakdown()

        val earliestDate = scheduleEntries.minByOrNull { it.scheduledDateTime }?.scheduledDateTime?.toLocalDate()

        val past7Days = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val dayEntries = scheduleEntries.filter {
                it.scheduledDateTime.toLocalDate() == date && !it.entryId.startsWith("PRN_")
            }
            val dayTaken = dayEntries.count {
                it.status == MedicationStatus.TAKEN_ON_TIME || it.status == MedicationStatus.TAKEN_LATE
            }
            val dayMissed = dayEntries.count { isEntryMissed(it) }
            val isToday = date.isEqual(today)
            // Guard: Factor overdue pending doses on the current day into decided counts without double-counting entries already in dayMissed (#BUG-ANALYTICS-OVERDUE)
            val dayOverduePending = if (isToday) {
                dayEntries.count { it.status == MedicationStatus.PENDING && it.scheduledDateTime.isBefore(now) && !isEntryMissed(it) }
            } else 0
            val dayDecided = dayTaken + dayMissed + dayOverduePending
            val dayRate = if (dayDecided > 0) ((dayTaken.toDouble() / dayDecided.toDouble()) * 100).toInt() else 0
            DayStat(
                date = date,
                dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, targetLocale).uppercase(targetLocale),
                total = dayEntries.size,
                taken = dayTaken,
                rate = dayRate
            )
        }

        val totalAllTimeEntries = scheduleEntries.count {
            it.scheduledDateTime.toLocalDate() <= today && !it.entryId.startsWith("PRN_")
        }

        return AnalyticsUiState(
            isLoading = false,
            isEmpty = totalAllTimeEntries == 0,
            selectedRange = selectedRange,
            rangeDataList = rangeDataList,
            breakdown = activeBreakdown,
            past7Days = past7Days,
            earliestDate = earliestDate,
            currentUser = user
        )
    }
}
