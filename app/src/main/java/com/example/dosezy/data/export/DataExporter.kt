package com.example.dosezy.data.export

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DataExporter(
    private val context: Context,
    private val userRepository: UserRepository,
    private val medicineRepository: MedicineRepository,
    private val scheduleRepository: ScheduleRepository
) {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun exportAllUsersToCsv(): File = withContext(Dispatchers.IO) {
        val allUsers = userRepository.getAllUsersList()

        if (allUsers.size == 1) {
            // Single user - export as single CSV
            exportUserToCsv(allUsers.first().userId)
        } else {
            // Multiple users - create zip with folders
            createUsersZip(allUsers)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun exportUserToCsv(userId: String): File = withContext(Dispatchers.IO) {
        val user = userRepository.getUserByIdSync(userId) ?: throw Exception("User not found")

        val medicines = medicineRepository.getMedicinesByUserSync(userId)
        val schedules = scheduleRepository.getSchedulesByUserSync(userId)

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "dosezy_export_${user.fullName.replace(" ", "_")}_$timestamp.csv"

        val csvContent = buildCsvContent(user, medicines, schedules)
        return@withContext saveExportToFile(csvContent, fileName)
    }




    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun createUsersZip(users: List<User>): File = withContext(Dispatchers.IO) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val zipFile = File(context.getExternalFilesDir(null), "dosezy_export_$timestamp.zip")

        FileOutputStream(zipFile).use { fileOutputStream ->
            ZipOutputStream(fileOutputStream).use { zipOutputStream ->
                users.forEach { user ->
                    try {

                        val medicines = medicineRepository.getMedicinesByUserSync(user.userId)
                        val schedules = scheduleRepository.getSchedulesByUserSync(user.userId)

                        val csvContent = buildCsvContent(user, medicines, schedules)
                        val entryName = "${user.fullName.replace(" ", "_")}/medication_data.csv"

                        val entry = ZipEntry(entryName)
                        zipOutputStream.putNextEntry(entry)
                        zipOutputStream.write(csvContent.toByteArray())
                        zipOutputStream.closeEntry()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Continue with next user even if one fails
                    }
                }
            }
        }

        return@withContext zipFile
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildCsvContent(user: User, medicines: List<Medicine>, schedules: List<ScheduleEntry>): String {
        val csvBuilder = StringBuilder()

        // User Information Section
        csvBuilder.append("USER INFORMATION\n")
        csvBuilder.append("User ID,Full Name,Age,Gender,Contact Number,Profile Picture Path,Is Current User\n")
        csvBuilder.append("\"${user.userId}\",\"${user.fullName}\",${user.age},${user.gender},\"${user.contactNumber}\",\"${user.profilePicPath ?: ""}\",${user.isCurrentUser}\n\n")

        // Medicines Section
        csvBuilder.append("MEDICINES\n")
        csvBuilder.append("Medicine ID,User ID,Medication Name,Dosage,Dosage Unit,Times Per Day,Frequency Pattern,Days Per Week,Days Per Month,Scheduled Times,Image URI\n")
        medicines.forEach { medicine ->
            val scheduledTimesStr = medicine.scheduledTimes.joinToString(";") { it.toString() }
            csvBuilder.append(
                "\"${medicine.medicineId}\"," +
                        "\"${medicine.userId}\"," +
                        "\"${medicine.medicationName}\"," +
                        "${medicine.dosage}," +
                        "${medicine.dosageUnit}," +
                        "${medicine.timesPerDay}," +
                        "${medicine.frequency.pattern}," +
                        "${medicine.frequency.daysPerWeek ?: ""}," +
                        "${medicine.frequency.daysPerMonth ?: ""}," +
                        "\"$scheduledTimesStr\"," +
                        "\"${medicine.imageUri ?: ""}\"\n"
            )
        }
        csvBuilder.append("\n")

        // Schedules Section
        csvBuilder.append("SCHEDULES\n")
        csvBuilder.append("Entry ID,User ID,Medicine ID,Scheduled DateTime,Status,Taken At\n")
        schedules.forEach { schedule ->
            csvBuilder.append(
                "\"${schedule.entryId}\"," +
                        "\"${schedule.userId}\"," +
                        "\"${schedule.medicineId}\"," +
                        "\"${schedule.scheduledDateTime}\"," +
                        "${schedule.status}," +
                        "\"${schedule.takenAt ?: ""}\"\n"
            )
        }

        return csvBuilder.toString()
    }

    suspend fun saveExportToFile(content: String, fileName: String): File {
        return withContext(Dispatchers.IO) {
            val file = File(context.getExternalFilesDir(null), fileName)
            file.writeText(content)
            file
        }
    }

    suspend fun getExportDirectory(): File {
        return withContext(Dispatchers.IO) {
            context.getExternalFilesDir(null) ?: context.filesDir
        }
    }

    // Helper function to share the file
    fun shareFile(file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = when (file.extension) {
                "csv" -> "text/csv"
                "zip" -> "application/zip"
                else -> "*/*"
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share Export File").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }
}


