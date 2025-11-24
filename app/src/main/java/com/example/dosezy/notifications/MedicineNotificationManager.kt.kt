// MedicineNotificationManager.kt
package com.example.dosezy.notifications

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicineNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: DosezyDatabase
) {

    private val alarmScheduler = AlarmScheduler(context)
    private val userRepository = UserRepository(database)
    private val scheduleRepository = ScheduleRepository(database)

    companion object {
        private const val TAG = "MedicineNotificationManager"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleAllAlarmsForCurrentUser() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val users = userRepository.getAllUsersList()
                val currentUser = users.find { it.isCurrentUser }

                currentUser?.let { user ->
                    scheduleRepository.rescheduleAllAlarms(user.userId, context)
                    Log.d(TAG, "Scheduled all alarms for current user: ${user.fullName}")
                } ?: run {
                    Log.w(TAG, "No current user found to schedule alarms for")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling alarms for current user", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleAlarmsForUser(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduleRepository.rescheduleAllAlarms(userId, context)
                Log.d(TAG, "Scheduled all alarms for user: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling alarms for user: $userId", e)
            }
        }
    }

    fun cancelAllAlarmsForUser(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allEntries = scheduleRepository.getSchedulesByUserSync(userId)
                allEntries.forEach { entry ->
                    alarmScheduler.cancelAlarm(entry.entryId)
                    alarmScheduler.cancelSnooze(entry.entryId)
                }
                Log.d(TAG, "Cancelled all alarms for user: $userId (${allEntries.size} entries)")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling alarms for user: $userId", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun rescheduleAllAlarmsForAllUsers() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allUsers = userRepository.getAllUsersList()

                allUsers.forEach { user ->
                    scheduleRepository.rescheduleAllAlarms(user.userId, context)
                }

                Log.d(TAG, "Rescheduled alarms for all ${allUsers.size} users")
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling alarms for all users", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun checkAlarmStatus(userId: String, callback: (scheduledCount: Int, totalCount: Int) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allEntries = scheduleRepository.getSchedulesByUserSync(userId)
                val futureEntries = allEntries.filter {
                    it.scheduledDateTime.isAfter(LocalDateTime.now()) &&
                            it.status == MedicationStatus.PENDING
                }

                callback(futureEntries.size, allEntries.size)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking alarm status", e)
                callback(0, 0)
            }
        }
    }

    // Add these additional utility methods:

    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleAlarmsForMedicine(medicineId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduleRepository.scheduleAlarmsForMedicine(medicineId, context)
                Log.d(TAG, "Scheduled alarms for medicine: $medicineId")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling alarms for medicine: $medicineId", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun cancelAlarmsForMedicine(medicineId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduleRepository.cancelAlarmsForMedicine(medicineId, context)
                Log.d(TAG, "Cancelled alarms for medicine: $medicineId")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling alarms for medicine: $medicineId", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getScheduledAlarmCount(userId: String, callback: (count: Int) -> Unit) {
        checkAlarmStatus(userId) { scheduled, total ->
            callback(scheduled)
        }
    }
}