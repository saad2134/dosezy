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
    val medicineRepository: MedicineRepository,
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
        root.put("appVersion", com.example.dosezy.BuildConfig.VERSION_NAME)
        root.put("exportedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        val userObj = JSONObject()
        userObj.put("userId", user.userId)
        userObj.put("fullName", user.fullName)
        userObj.put("age", user.age)
        userObj.put("gender", user.gender.name)
        userObj.put("contactNumber", user.contactNumber)
        userObj.put("allergies", user.allergies ?: "")
        userObj.put("medicalConditions", user.medicalConditions ?: "")
        userObj.put("alarmSound", user.alarmSound.name)
        userObj.put("customAlarmSoundPath", user.customAlarmSoundPath ?: "")
        userObj.put("customAlarmSoundTitle", user.customAlarmSoundTitle ?: "")
        userObj.put("alarmDurationSeconds", user.alarmDurationSeconds)
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
            mObj.put("intervalHours", med.frequency.intervalHours)
            mObj.put("intervalDays", med.frequency.intervalDays)
            mObj.put("scheduledTimes", JSONArray(med.scheduledTimes.map { it.toString() }))
            mObj.put("currentStock", med.currentStock)
            mObj.put("refillThreshold", med.refillThreshold)
            mObj.put("autoDeductOnTake", med.autoDeductOnTake)
            mObj.put("pillShape", med.pillShape.name)
            mObj.put("pillColor", med.pillColor)
            mObj.put("notes", med.notes ?: "")
            mObj.put("startDate", med.startDate?.toString() ?: "")
            mObj.put("endDate", med.endDate?.toString() ?: "")
            mObj.put("durationDays", med.durationDays)
            mObj.put("isArchived", med.isArchived)
            medArray.put(mObj)
        }
        root.put("medicines", medArray)

        val schedArray = JSONArray()
        schedules.forEach { sch ->
            val sObj = JSONObject()
            sObj.put("entryId", sch.entryId)
            sObj.put("medicineId", sch.medicineId)
            sObj.put("scheduledDateTime", sch.scheduledDateTime.toString())
            sObj.put("status", sch.status.name)
            sObj.put("skipReason", sch.skipReason ?: "")
            sObj.put("takenAt", sch.takenAt?.toString() ?: "")
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

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1193D4")
            textSize = 18f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B5563")
            textSize = 10f
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F2937")
            textSize = 12f
            isFakeBoldText = true
        }
        val alertHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DC2626")
            textSize = 10f
            isFakeBoldText = true
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#374151")
            textSize = 10f
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 1f
        }

        // Top-left App Brand Icon (matching New User & Alarm popup brand icon)
        val brandIcon = androidx.core.content.ContextCompat.getDrawable(context, com.example.dosezy.R.drawable.loader_icon)
        if (brandIcon != null) {
            brandIcon.setBounds(40, 24, 68, 52) // 28x28 pt at top-left
            brandIcon.draw(canvas)
        }

        var y = 38f
        canvas.drawText("DOSEZY - Patient Health & Medication Report", 76f, y, titlePaint)
        y += 16f
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        canvas.drawText("Generated: $timestamp | Private & Offline Export", 76f, y, subtitlePaint)
        y += 15f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 22f

        // Patient Info
        canvas.drawText("Patient Profile", 40f, y, headerPaint)
        y += 15f
        canvas.drawText("Name: ${user.fullName}", 40f, y, textPaint)
        canvas.drawText("Age: ${user.age}", 240f, y, textPaint)
        canvas.drawText("Gender: ${user.gender}", 360f, y, textPaint)
        y += 15f

        // Allergies & Medical Conditions in PDF
        val allergiesText = if (!user.allergies.isNullOrBlank()) user.allergies else "None recorded"
        val conditionsText = if (!user.medicalConditions.isNullOrBlank()) user.medicalConditions else "None recorded"
        
        if (!user.allergies.isNullOrBlank()) {
            canvas.drawText("Allergies: $allergiesText", 40f, y, alertHeaderPaint)
        } else {
            canvas.drawText("Allergies: $allergiesText", 40f, y, textPaint)
        }
        y += 15f
        canvas.drawText("Chronic Conditions: $conditionsText", 40f, y, textPaint)
        y += 18f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        // Analytics & Adherence Summary Section (Calculated from past decided doses)
        val totalCount = schedules.size
        val takenOnTimeCount = schedules.count { it.status == com.example.dosezy.data.model.MedicationStatus.TAKEN_ON_TIME }
        val takenLateCount = schedules.count { it.status == com.example.dosezy.data.model.MedicationStatus.TAKEN_LATE }
        val skippedCount = schedules.count { it.status == com.example.dosezy.data.model.MedicationStatus.SKIPPED }
        val missedCount = schedules.count { it.status == com.example.dosezy.data.model.MedicationStatus.MISSED }
        val pendingCount = schedules.count { it.status == com.example.dosezy.data.model.MedicationStatus.PENDING }

        val takenTotal = takenOnTimeCount + takenLateCount
        val decidedCount = takenTotal + missedCount
        val adherenceRate = if (decidedCount > 0) (takenTotal.toDouble() / decidedCount * 100.0) else 0.0
        val rateFormatted = String.format(java.util.Locale.US, "%.1f%%", adherenceRate)

        canvas.drawText("Adherence & Analytics Summary", 40f, y, headerPaint)
        y += 16f

        val cardRect = android.graphics.RectF(40f, y, 555f, y + 46f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F3F4F6")
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, cardPaint)

        val rateColor = if (adherenceRate >= 90.0) Color.parseColor("#10B981")
                        else if (adherenceRate >= 70.0) Color.parseColor("#1193D4")
                        else Color.parseColor("#EF4444")

        val ratePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = rateColor
            textSize = 15f
            isFakeBoldText = true
        }
        canvas.drawText("Overall Adherence: $rateFormatted", 52f, y + 20f, ratePaint)

        val cardSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B5563")
            textSize = 9.5f
        }
        val breakdownText = "On-Time: $takenOnTimeCount   |   Late: $takenLateCount   |   Skipped: $skippedCount   |   Missed: $missedCount   |   Pending: $pendingCount   |   Total: $totalCount"
        canvas.drawText(breakdownText, 52f, y + 36f, cardSubPaint)

        y += 56f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

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
        schedules.take(18).forEach { sch ->
            val dateStr = sch.scheduledDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            canvas.drawText(dateStr, 40f, y, textPaint)
            val statusLabel = when (sch.status) {
                com.example.dosezy.data.model.MedicationStatus.TAKEN_ON_TIME -> "Taken On Time"
                com.example.dosezy.data.model.MedicationStatus.TAKEN_LATE -> "Taken Late"
                com.example.dosezy.data.model.MedicationStatus.MISSED -> "Missed"
                com.example.dosezy.data.model.MedicationStatus.SKIPPED -> "Skipped"
                com.example.dosezy.data.model.MedicationStatus.PENDING -> "Pending"
            }
            canvas.drawText(statusLabel, 260f, y, textPaint)
            val takenStr = if (sch.takenAt != null && sch.takenAt.year > 1970) {
                sch.takenAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            } else if (sch.status == com.example.dosezy.data.model.MedicationStatus.TAKEN_ON_TIME || sch.status == com.example.dosezy.data.model.MedicationStatus.TAKEN_LATE) {
                sch.scheduledDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            } else {
                "-"
            }
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
        csvBuilder.append("User ID,Full Name,Age,Gender,Contact Number,Allergies,Medical Conditions,Profile Picture Path,Is Current User\n")
        csvBuilder.append("\"${user.userId}\",\"${user.fullName}\",${user.age},${user.gender},\"${user.contactNumber}\",\"${user.allergies ?: ""}\",\"${user.medicalConditions ?: ""}\",\"${user.profilePicPath ?: ""}\",${user.isCurrentUser}\n\n")

        // Medicines Section
        csvBuilder.append("MEDICINES\n")
        csvBuilder.append("Medicine ID,User ID,Medication Name,Dosage,Dosage Unit,Times Per Day,Frequency Pattern,Interval Hours,Interval Days,Scheduled Times,Pill Shape,Pill Color,Doctor Notes,Start Date,End Date,Duration Days,Stock,Image URI\n")
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
                        "${medicine.frequency.intervalHours ?: ""}," +
                        "${medicine.frequency.intervalDays ?: ""}," +
                        "\"$scheduledTimesStr\"," +
                        "\"${medicine.pillShape.name}\"," +
                        "\"${medicine.pillColor}\"," +
                        "\"${(medicine.notes ?: "").replace("\"", "\"\"")}\"," +
                        "\"${medicine.startDate ?: ""}\"," +
                        "\"${medicine.endDate ?: ""}\"," +
                        "${medicine.durationDays ?: ""}," +
                        "${medicine.currentStock ?: ""}," +
                        "\"${medicine.imageUri ?: ""}\"\n"
            )
        }
        csvBuilder.append("\n")

        // Schedules Section
        csvBuilder.append("SCHEDULES\n")
        csvBuilder.append("Entry ID,User ID,Medicine ID,Scheduled DateTime,Status,Skip Reason,Taken At\n")
        schedules.forEach { schedule ->
            csvBuilder.append(
                "\"${schedule.entryId}\"," +
                        "\"${schedule.userId}\"," +
                        "\"${schedule.medicineId}\"," +
                        "\"${schedule.scheduledDateTime}\"," +
                        "${schedule.status}," +
                        "\"${(schedule.skipReason ?: "").replace("\"", "\"\"")}\"," +
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

    fun generatePharmacyOrderText(
        user: User,
        medicines: List<Medicine>,
        supplyDays: Int = 30,
        customQuantities: Map<String, Int> = emptyMap(),
        includeStock: Boolean = false,
        includeDosage: Boolean = true
    ): String {
        return generateConsolidatedPharmacyOrderText(
            profilesWithMedicines = listOf(user to medicines),
            supplyDays = supplyDays,
            customQuantities = customQuantities,
            includeStock = includeStock,
            includeDosage = includeDosage
        )
    }

    fun generateConsolidatedPharmacyOrderText(
        profilesWithMedicines: List<Pair<User, List<Medicine>>>,
        supplyDays: Int = 30,
        customQuantities: Map<String, Int> = emptyMap(),
        includeStock: Boolean = false,
        includeDosage: Boolean = true
    ): String {
        val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")
        val dateStr = java.time.LocalDate.now().format(dateFormatter)

        val sb = StringBuilder()
        sb.append(context.getString(com.example.dosezy.R.string.pharmacy_order_header_title)).append("\n")

        val isMultiProfile = profilesWithMedicines.size > 1

        if (!isMultiProfile && profilesWithMedicines.isNotEmpty()) {
            val singleUser = profilesWithMedicines.first().first
            sb.append(context.getString(com.example.dosezy.R.string.pharmacy_order_patient_header, singleUser.fullName)).append("\n")
        }
        sb.append(context.getString(com.example.dosezy.R.string.pharmacy_order_date_header, dateStr)).append("\n\n")
        sb.append("*").append(context.getString(com.example.dosezy.R.string.pharmacy_order_supply_header, supplyDays)).append("*\n\n")

        profilesWithMedicines.forEach { (profileUser, profileMeds) ->
            if (profileMeds.isEmpty()) return@forEach

            if (isMultiProfile) {
                sb.append("👤 *${profileUser.fullName}:*\n")
            }

            profileMeds.forEachIndexed { index, med ->
                val isDrop = med.dosageUnit == com.example.dosezy.data.model.DosageUnit.DROP ||
                        med.pillShape == com.example.dosezy.data.model.PillShape.DROPS

                val totalNeeded = customQuantities[med.medicineId] ?: run {
                    if (isDrop) {
                        val dropsPerIntake = if (med.dosage > 0) med.dosage.toInt().coerceAtLeast(1) else 1
                        val totalDrops = (med.timesPerDay.coerceAtLeast(1)) * dropsPerIntake * supplyDays
                        kotlin.math.ceil(totalDrops / 100.0).toInt().coerceAtLeast(1)
                    } else {
                        val dosesPerIntake = when (med.dosageUnit) {
                            com.example.dosezy.data.model.DosageUnit.TABLET, com.example.dosezy.data.model.DosageUnit.CAPSULE -> {
                                if (med.dosage > 0) med.dosage.toInt().coerceAtLeast(1) else 1
                            }
                            com.example.dosezy.data.model.DosageUnit.ML -> {
                                if (med.dosage > 0) med.dosage.toInt().coerceAtLeast(1) else 1
                            }
                            com.example.dosezy.data.model.DosageUnit.MG, com.example.dosezy.data.model.DosageUnit.MCG, com.example.dosezy.data.model.DosageUnit.DROP -> 1
                        }
                        val dailyRequirement = (med.timesPerDay.coerceAtLeast(1)) * dosesPerIntake
                        (dailyRequirement * supplyDays).coerceAtLeast(1)
                    }
                }

                val showDosageInTitle = includeDosage && when (med.dosageUnit) {
                    com.example.dosezy.data.model.DosageUnit.MG, com.example.dosezy.data.model.DosageUnit.MCG, com.example.dosezy.data.model.DosageUnit.ML -> true
                    else -> med.dosage > 0
                }
                val dosageDisplay = if (med.dosage > 0) {
                    if (med.dosage % 1.0 == 0.0) "${med.dosage.toInt()}" else "${med.dosage}"
                } else ""
                val strengthUnitStr = med.dosageUnit.name.lowercase()

                val orderUnitStr = if (isDrop) {
                    if (totalNeeded > 1) context.getString(com.example.dosezy.R.string.unit_bottles) else context.getString(com.example.dosezy.R.string.unit_bottle)
                } else {
                    when (med.dosageUnit) {
                        com.example.dosezy.data.model.DosageUnit.TABLET -> if (totalNeeded > 1) "tablets" else "tablet"
                        com.example.dosezy.data.model.DosageUnit.CAPSULE -> if (totalNeeded > 1) "capsules" else "capsule"
                        com.example.dosezy.data.model.DosageUnit.DROP -> if (totalNeeded > 1) context.getString(com.example.dosezy.R.string.unit_bottles) else context.getString(com.example.dosezy.R.string.unit_bottle)
                        com.example.dosezy.data.model.DosageUnit.ML -> "ml"
                        com.example.dosezy.data.model.DosageUnit.MG, com.example.dosezy.data.model.DosageUnit.MCG -> if (totalNeeded > 1) "units" else "unit"
                    }
                }

                sb.append("${index + 1}. *${med.medicationName}*")
                if (showDosageInTitle && dosageDisplay.isNotBlank()) {
                    sb.append(" ($dosageDisplay $strengthUnitStr)")
                }
                sb.append("\n")
                sb.append("   • ").append(context.getString(com.example.dosezy.R.string.pharmacy_order_qty_needed, totalNeeded, orderUnitStr)).append("\n")

                if (includeStock && med.currentStock != null) {
                    val stockUnitStr = if (isDrop) {
                        if (med.currentStock > 1) "drops" else "drop"
                    } else {
                        when (med.dosageUnit) {
                            com.example.dosezy.data.model.DosageUnit.TABLET -> if (med.currentStock > 1) "tablets" else "tablet"
                            com.example.dosezy.data.model.DosageUnit.CAPSULE -> if (med.currentStock > 1) "capsules" else "capsule"
                            com.example.dosezy.data.model.DosageUnit.DROP -> if (med.currentStock > 1) "drops" else "drop"
                            com.example.dosezy.data.model.DosageUnit.ML -> "ml"
                            com.example.dosezy.data.model.DosageUnit.MG, com.example.dosezy.data.model.DosageUnit.MCG -> if (med.currentStock > 1) "units" else "unit"
                        }
                    }
                    sb.append("   • ").append(context.getString(com.example.dosezy.R.string.pharmacy_order_current_stock, med.currentStock, stockUnitStr)).append("\n")
                }
                sb.append("\n")
            }
        }

        sb.append("_").append(context.getString(com.example.dosezy.R.string.pharmacy_order_footer_confirm)).append("_\n\n")
        sb.append("_").append(context.getString(com.example.dosezy.R.string.pharmacy_order_footer_brand)).append("_")

        return sb.toString()
    }

    fun shareText(text: String, title: String? = null) {
        val shareTitle = title ?: context.getString(com.example.dosezy.R.string.pharmacy_order_title)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val chooserIntent = Intent.createChooser(shareIntent, shareTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(context.getString(com.example.dosezy.R.string.pharmacy_order_title), text)
        clipboard.setPrimaryClip(clip)
        android.widget.Toast.makeText(
            context,
            context.getString(com.example.dosezy.R.string.pharmacy_order_copied),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
