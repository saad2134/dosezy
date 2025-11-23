package com.example.dosezy.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dosezy.ui.viewmodels.MedicineViewModel
import com.example.dosezy.ui.viewmodels.ScheduleViewModel
import com.example.dosezy.ui.viewmodels.UserViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DebugScreen(
    userViewModel: UserViewModel = hiltViewModel(),
    medicineViewModel: MedicineViewModel = hiltViewModel(),
    scheduleViewModel: ScheduleViewModel = hiltViewModel()
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val medicines by medicineViewModel.medicines.collectAsState()
    val scheduleWithMedicine by scheduleViewModel.scheduleWithMedicine.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Debug Info", style = MaterialTheme.typography.headlineMedium)

        Text("Current User: ${currentUser?.fullName ?: "None"}")
        Text("User ID: ${currentUser?.userId ?: "None"}")

        Spacer(modifier = Modifier.height(16.dp))

        Text("Medicines in DB: ${medicines.size}")
        medicines.forEach { medicine ->
            Text("- ${medicine.medicationName} (ID: ${medicine.medicineId})")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Schedule Entries Today: ${scheduleWithMedicine.size}")
        scheduleWithMedicine.forEach { item ->
            Text("- ${item.medicine?.medicationName ?: "Unknown"} at ${item.scheduleEntry.scheduledDateTime}")
        }
    }
}