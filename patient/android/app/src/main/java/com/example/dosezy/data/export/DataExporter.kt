package com.example.dosezy.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import com.example.dosezy.ui.components.ExportFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
    suspend fun exportUserData(userId: String?, format: ExportFormat): File = withContext(Dispatchers.IO) {
        val allUsers = userRepository.getAllUsersList()
        val targetUser = if (userId != null) userRepository.getUserByIdSync(userId) else null

        if (targetUser != null) {
            when (format) {
                ExportFormat.CSV -> exportUserToCsv(targetUser.userId)
                ExportFormat.JSON -> exportUserToJson(targetUser.userId)
                ExportFormat.PDF -> exportUserToPdf(targetUser.userId)
            }
        } else {
            if (allUsers.size == 1) {
                when (format) {
                    ExportFormat.CSV -> exportUserToCsv(allUsers.first().userId)
                    ExportFormat.JSON -> exportUserToJson(allUsers.first().userId)
                    ExportFormat.PDF -> exportUserToPdf(allUsers.first().userId)
                }
            } else {
                createUsersZipFormatted(allUsers, format)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun exportAllUsersToCsv(): File = exportUserData(null, ExportFormat.CSV)

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
    suspend fun exportUserToJson(userId: String): File = withContext(Dispatchers.IO) {
        val user = userRepository.getUserByIdSync(userId) ?: throw Exception("User not found")
        val medicines = medicineRepository.getMedicinesByUserSync(userId)
        val schedules = scheduleRepository.getSchedulesByUserSync(userId)

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "dosezy_export_${user.fullName.replace(" ", "_")}_$timestamp.json"

        val jsonContent = buildJsonContent(user, medicines, schedules)
        return@withContext saveExportToFile(jsonContent, fileName)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun exportUserToPdf(userId: String): File = withContext(Dispatchers.IO) {
        val user = userRepository.getUserByIdSync(userId) ?: throw Exception("User not found")
        val medicines = medicineRepository.getMedicinesByUserSync(userId)
        val schedules = scheduleRepository.getSchedulesByUserSync(userId)

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "dosezy_report_${user.fullName.replace(" ", "_")}_$timestamp.pdf"

        val pdfDoc = buildPdfDocument(user, medicines, schedules)
        val file = File(context.getExternalFilesDir(null), fileName)
        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        return@withContext file
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun createUsersZipFormatted(users: List<User>, format: ExportFormat): File = withContext(Dispatchers.IO) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val zipFile = File(context.getExternalFilesDir(null), "dosezy_export_$timestamp.zip")

        FileOutputStream(zipFile).use { fileOutputStream ->
            ZipOutputStream(fileOutputStream).use { zipOutputStream ->
                users.forEach { user ->
                    try {
                        val medicines = medicineRepository.getMedicinesByUserSync(user.userId)
                        val schedules = scheduleRepository.getSchedulesByUserSync(user.userId)

                        val userFolder = user.fullName.replace(" ", "_")
                        when (format) {
                            ExportFormat.CSV -> {
                                val content = buildCsvContent(user, medicines, schedules)
                                val entry = ZipEntry("$userFolder/medication_data.csv")
                                zipOutputStream.putNextEntry(entry)
                                zipOutputStream.write(content.toByteArray())
                                zipOutputStream.closeEntry()
                            }
                            ExportFormat.JSON -> {
                                val content = buildJsonContent(user, medicines, schedules)
                                val entry = ZipEntry("$userFolder/medication_data.json")
                                zipOutputStream.putNextEntry(entry)
                                zipOutputStream.write(content.toByteArray())
                                zipOutputStream.closeEntry()
                            }
                            ExportFormat.PDF -> {
                                val pdfDoc = buildPdfDocument(user, medicines, schedules)
                                val tempPdf = File(context.cacheDir, "${user.userId}.pdf")
                                FileOutputStream(tempPdf).use { pdfDoc.writeTo(it) }
                                pdfDoc.close()

                                val entry = ZipEntry("$userFolder/health_report.pdf")
                                zipOutputStream.putNextEntry(entry)
                                zipOutputStream.write(tempPdf.readBytes())
                                zipOutputStream.closeEntry()
                                tempPdf.delete()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        return@withContext zipFile
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildJsonContent(user: User, medicines: List<Medicine>, schedules: List<ScheduleEntry>): String {
        val root = JSONObject()
        root.put("appName", "Dosezy")
        root.put("exportedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        val userObj = JSONObject()
        userObj.put("userId", user.userId)
        userObj.put("fullName", user.fullName)
        userObj.put("age", user.age)
        userObj.put("gender", user.gender.name)
        userObj.put("contactNumber", user.contactNumber)
        root.put("user", userObj)

        val medArray = JSONArray()
        medicines.forEach { med ->
            val mObj = JSONObject()
            mObj.put("medicineId", med.medicineId)
            mObj.put("medicationName", med.medicationName)
            mObj.put("dosage", med.dosage)
            mObj.put("dosageUnit", med.dosageUnit.name)
            mObj.put("timesPerDay", med.timesPerDay)
            mObj.put("frequencyPattern", med.frequency.pattern.name)
            mObj.put("scheduledTimes", JSONArray(med.scheduledTimes.map { it.toString() }))
            medArray.put(mObj)
        }
        root.put("medicines", medArray)

        val schedArray = JSONArray()
        schedules.forEach { sch ->
            val sObj = JSONObject()
            sObj.put("entryId", sch.entryId)
            sObj.put("medicineId", sch.medicineId)
            sObj.put("scheduledDateTime", sch.scheduledDateTime)
            sObj.put("status", sch.status.name)
            sObj.put("takenAt", sch.takenAt ?: "")
            schedArray.put(sObj)
        }
        root.put("schedules", schedArray)

        return root.toString(4)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPdfDocument(user: User, medicines: List<Medicine>, schedules: List<ScheduleEntry>): PdfDocument {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 page
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1193D4")
            textSize = 20f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#4B5563")
            textSize = 11f
        }
        val headerPaint = Paint().apply {
            color = Color.parseColor("#1F2937")
            textSize = 13f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            color = Color.parseColor("#374151")
            textSize = 10f
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 1f
        }

        var y = 40f
        canvas.drawText("DOSEZY - Patient Health & Medication Report", 40f, y, titlePaint)
        y += 18f
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        canvas.drawText("Generated: $timestamp | Private & Offline Export", 40f, y, subtitlePaint)
        y += 15f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        // Patient Info
        canvas.drawText("Patient Profile", 40f, y, headerPaint)
        y += 16f
        canvas.drawText("Name: ${user.fullName}", 40f, y, textPaint)
        canvas.drawText("Age: ${user.age}", 240f, y, textPaint)
        canvas.drawText("Gender: ${user.gender}", 360f, y, textPaint)
        y += 18f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        // Medicines Table
        canvas.drawText("Active Medications (${medicines.size})", 40f, y, headerPaint)
        y += 18f

        subtitlePaint.isFakeBoldText = true
        canvas.drawText("Medication", 40f, y, subtitlePaint)
        canvas.drawText("Dosage", 220f, y, subtitlePaint)
        canvas.drawText("Frequency", 360f, y, subtitlePaint)
        y += 12f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 16f

        subtitlePaint.isFakeBoldText = false
        medicines.take(10).forEach { med ->
            canvas.drawText(med.medicationName, 40f, y, textPaint)
            canvas.drawText("${med.dosage} ${med.dosageUnit}", 220f, y, textPaint)
            canvas.drawText("${med.timesPerDay}x daily", 360f, y, textPaint)
            y += 16f
        }
        if (medicines.isEmpty()) {
            canvas.drawText("No active medications recorded.", 40f, y, textPaint)
            y += 16f
        }

        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        // Schedule Logs Table
        canvas.drawText("Recent Dose History (${schedules.size} entries)", 40f, y, headerPaint)
        y += 18f

        subtitlePaint.isFakeBoldText = true
        canvas.drawText("Scheduled Date/Time", 40f, y, subtitlePaint)
        canvas.drawText("Status", 260f, y, subtitlePaint)
        canvas.drawText("Recorded Time", 400f, y, subtitlePaint)
        y += 12f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 16f

        subtitlePaint.isFakeBoldText = false
        schedules.take(16).forEach { sch ->
            val dateStr = sch.scheduledDateTime.toString().take(16).replace("T", " ")
            canvas.drawText(dateStr, 40f, y, textPaint)
            canvas.drawText(sch.status.name, 260f, y, textPaint)
            val takenStr = sch.takenAt?.toString()?.take(16)?.replace("T", " ") ?: "-"
            canvas.drawText(takenStr, 400f, y, textPaint)
            y += 16f
        }
        if (schedules.isEmpty()) {
            canvas.drawText("No schedule history recorded.", 40f, y, textPaint)
            y += 16f
        }

        y = 810f
        canvas.drawLine(40f, y - 10f, 555f, y - 10f, linePaint)
        canvas.drawText("Dosezy Medicine Tracker • Confidential Patient Record", 40f, y, subtitlePaint)

        pdfDocument.finishPage(page)
        return pdfDocument
    }

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
                "json" -> "application/json"
                "pdf" -> "application/pdf"
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
