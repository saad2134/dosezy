package com.example.dosezy.data.export

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.getLocalizedName
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
    private val scheduleRepository: ScheduleRepository,
    private val pdfReportGenerator: PdfReportGenerator = PdfReportGenerator(context)
) {

    suspend fun getActiveMedicinesForUser(userId: String): List<Medicine> {
        return medicineRepository.getMedicinesByUserSync(userId).filter { !it.isArchived }
    }

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

    // Guard: Strip directory separators and illegal filesystem characters to prevent FileNotFoundException or invalid paths
    private fun sanitizeFileName(name: String): String {
        val clean = name.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_").trim('_', '.')
        return clean.ifBlank { "user" }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun exportUserToCsv(userId: String): File = withContext(Dispatchers.IO) {
        val user = userRepository.getUserByIdSync(userId) ?: throw Exception("User not found")
        // Guard: Use getMedicinesByUserDirect to include archived/discontinued medicines and prevent broken foreign key references in historical exports
        val medicines = medicineRepository.getMedicinesByUserDirect(userId)
        val schedules = scheduleRepository.getSchedulesByUserSync(userId)

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val safeName = sanitizeFileName(user.fullName)
        val fileName = "dosezy_export_${safeName}_$timestamp.csv"

        val csvContent = buildCsvContent(user, medicines, schedules)
        return@withContext saveExportToFile(csvContent, fileName)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun exportUserToJson(userId: String): File = withContext(Dispatchers.IO) {
        val user = userRepository.getUserByIdSync(userId) ?: throw Exception("User not found")
        // Guard: Use getMedicinesByUserDirect to include archived/discontinued medicines and prevent broken foreign key references in historical exports
        val medicines = medicineRepository.getMedicinesByUserDirect(userId)
        val schedules = scheduleRepository.getSchedulesByUserSync(userId)

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val safeName = sanitizeFileName(user.fullName)
        val fileName = "dosezy_export_${safeName}_$timestamp.json"

        val jsonContent = buildJsonContent(user, medicines, schedules)
        return@withContext saveExportToFile(jsonContent, fileName)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun exportUserToPdf(userId: String): File = withContext(Dispatchers.IO) {
        val user = userRepository.getUserByIdSync(userId) ?: throw Exception("User not found")
        // Guard: Use getMedicinesByUserDirect to include archived/discontinued medicines and prevent "Unknown" labels in historical reports
        val medicines = medicineRepository.getMedicinesByUserDirect(userId)
        val schedules = scheduleRepository.getSchedulesByUserSync(userId)

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val safeName = sanitizeFileName(user.fullName)
        val fileName = "dosezy_report_${safeName}_$timestamp.pdf"

        val pdfDoc = pdfReportGenerator.buildPdfDocument(user, medicines, schedules)
        // Guard: Use getExportDirectory() fallback to filesDir when externalFilesDir is null/unmounted to prevent NPE
        val exportDir = getExportDirectory()
        val file = File(exportDir, fileName)
        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        return@withContext file
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun createUsersZipFormatted(users: List<User>, format: ExportFormat): File = withContext(Dispatchers.IO) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        // Guard: Use getExportDirectory() fallback to filesDir when externalFilesDir is null/unmounted to prevent NPE
        val exportDir = getExportDirectory()
        val zipFile = File(exportDir, "dosezy_export_$timestamp.zip")

        val usedFolders = mutableSetOf<String>()
        FileOutputStream(zipFile).use { fileOutputStream ->
            ZipOutputStream(fileOutputStream).use { zipOutputStream ->
                users.forEach { user ->
                    try {
                        // Guard: Use getMedicinesByUserDirect to include archived/discontinued medicines
                        val medicines = medicineRepository.getMedicinesByUserDirect(user.userId)
                        val schedules = scheduleRepository.getSchedulesByUserSync(user.userId)

                        // Guard: Deduplicate zip entry folder names across multi-user exports to prevent ZipException duplicate entry drops
                        var baseFolder = sanitizeFileName(user.fullName)
                        if (usedFolders.contains(baseFolder)) {
                            baseFolder = "${baseFolder}_${user.userId.take(6)}"
                        }
                        usedFolders.add(baseFolder)
                        val userFolder = baseFolder

                        when (format) {
                            ExportFormat.CSV -> {
                                val content = buildCsvContent(user, medicines, schedules)
                                val entry = ZipEntry("$userFolder/medication_data.csv")
                                zipOutputStream.putNextEntry(entry)
                                // Guard: Explicitly specify UTF-8 charset to prevent corrupted non-ASCII characters in zip exports on devices with non-UTF-8 default charsets
                                zipOutputStream.write(content.toByteArray(Charsets.UTF_8))
                                zipOutputStream.closeEntry()
                            }
                            ExportFormat.JSON -> {
                                val content = buildJsonContent(user, medicines, schedules)
                                val entry = ZipEntry("$userFolder/medication_data.json")
                                zipOutputStream.putNextEntry(entry)
                                // Guard: Explicitly specify UTF-8 charset to prevent corrupted non-ASCII characters in zip exports on devices with non-UTF-8 default charsets
                                zipOutputStream.write(content.toByteArray(Charsets.UTF_8))
                                zipOutputStream.closeEntry()
                            }
                            ExportFormat.PDF -> {
                                val pdfDoc = pdfReportGenerator.buildPdfDocument(user, medicines, schedules)
                                val tempPdf = File(context.cacheDir, "${user.userId}.pdf")
                                try {
                                    FileOutputStream(tempPdf).use { pdfDoc.writeTo(it) }
                                    pdfDoc.close()

                                    val entry = ZipEntry("$userFolder/health_report.pdf")
                                    zipOutputStream.putNextEntry(entry)
                                    // Guard: Stream tempPdf to zipOutputStream to prevent high heap byte array allocation and OOM on low-memory devices
                                    tempPdf.inputStream().use { input ->
                                        input.copyTo(zipOutputStream)
                                    }
                                    zipOutputStream.closeEntry()
                                } finally {
                                    // Guard: Ensure temporary PDF file in cacheDir is deleted even if zip write throws an exception to prevent cache leak
                                    tempPdf.delete()
                                }
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
    internal fun buildJsonContent(user: User, medicines: List<Medicine>, schedules: List<ScheduleEntry>): String {
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
        userObj.put("profilePicPath", user.profilePicPath ?: "")
        userObj.put("isCurrentUser", user.isCurrentUser)
        userObj.put("theme", user.theme.name)
        userObj.put("timeFormat", user.timeFormat.name)
        userObj.put("language", user.language.name)
        userObj.put("considerLateAfter", user.considerLateAfter)
        userObj.put("considerMissedAfter", user.considerMissedAfter)
        userObj.put("snoozeDuration", user.snoozeDuration)
        userObj.put("allergies", user.allergies ?: "")
        userObj.put("medicalConditions", user.medicalConditions ?: "")
        userObj.put("naggingRemindersEnabled", user.naggingRemindersEnabled)
        userObj.put("naggingIntervalMinutes", user.naggingIntervalMinutes)
        userObj.put("naggingMaxRepeats", user.naggingMaxRepeats)
        userObj.put("alarmSound", user.alarmSound.name)
        userObj.put("customAlarmSoundPath", user.customAlarmSoundPath ?: "")
        userObj.put("customAlarmSoundTitle", user.customAlarmSoundTitle ?: "")
        userObj.put("alarmDurationSeconds", user.alarmDurationSeconds)
        userObj.put("allowDoseSkipping", user.allowDoseSkipping)
        userObj.put("allowCustomDoseTime", user.allowCustomDoseTime)
        userObj.put("hideAddMedicineNavButton", user.hideAddMedicineNavButton)
        userObj.put("allowDoseUndo", user.allowDoseUndo)
        userObj.put("allowDoseNotes", user.allowDoseNotes)
        userObj.put("promptDoseNotes", user.promptDoseNotes)
        // Guard: Explicitly serialize slideActionsEnabled, timelineModeEnabled, and thickerCalendarDayHighlight to prevent dropping UI preferences on backup restore
        userObj.put("slideActionsEnabled", user.slideActionsEnabled)
        userObj.put("timelineModeEnabled", user.timelineModeEnabled)
        userObj.put("thickerCalendarDayHighlight", user.thickerCalendarDayHighlight)
        // Guard: Explicitly serialize intake safeguard preferences to retain ADHD support settings on backup restore (#104)
        userObj.put("intakeSafeguardEnabled", user.intakeSafeguardEnabled)
        userObj.put("intakeCountdownSeconds", user.intakeCountdownSeconds)
        root.put("user", userObj)

        val medArray = JSONArray()
        medicines.forEach { med ->
            val mObj = JSONObject()
            mObj.put("medicineId", med.medicineId)
            // Guard: Explicitly serialize userId for medicine to retain profile relational integrity in export backups
            mObj.put("userId", med.userId)
            mObj.put("medicationName", med.medicationName)
            mObj.put("dosage", med.dosage)
            mObj.put("dosageUnit", med.dosageUnit.name)
            mObj.put("timesPerDay", med.timesPerDay)
            mObj.put("frequencyPattern", med.frequency.pattern.name)
            mObj.put("daysPerWeek", med.frequency.daysPerWeek)
            mObj.put("daysPerMonth", med.frequency.daysPerMonth)
            med.frequency.selectedDaysOfWeek?.let { days ->
                val arr = JSONArray()
                days.forEach { arr.put(it) }
                mObj.put("selectedDaysOfWeek", arr)
            }
            med.frequency.selectedDaysOfMonth?.let { days ->
                val arr = JSONArray()
                days.forEach { arr.put(it) }
                mObj.put("selectedDaysOfMonth", arr)
            }
            mObj.put("intervalHours", med.frequency.intervalHours)
            mObj.put("intervalDays", med.frequency.intervalDays)
            mObj.put("intervalWeeks", med.frequency.intervalWeeks)

            // Guard: Serialize nested frequency object alongside top-level keys for dual compatibility with Gson and BackupRestoreManager
            val freqObj = JSONObject()
            freqObj.put("pattern", med.frequency.pattern.name)
            freqObj.put("daysPerWeek", med.frequency.daysPerWeek)
            freqObj.put("daysPerMonth", med.frequency.daysPerMonth)
            med.frequency.selectedDaysOfWeek?.let { days ->
                val arr = JSONArray()
                days.forEach { arr.put(it) }
                freqObj.put("selectedDaysOfWeek", arr)
            }
            med.frequency.selectedDaysOfMonth?.let { days ->
                val arr = JSONArray()
                days.forEach { arr.put(it) }
                freqObj.put("selectedDaysOfMonth", arr)
            }
            freqObj.put("intervalHours", med.frequency.intervalHours)
            freqObj.put("intervalDays", med.frequency.intervalDays)
            freqObj.put("intervalWeeks", med.frequency.intervalWeeks)
            mObj.put("frequency", freqObj)
            mObj.put("scheduledTimes", JSONArray(med.scheduledTimes.map { it.toString() }))
            mObj.put("imageUri", med.imageUri ?: "")
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
            med.customDosages?.let { cd ->
                val cdObj = JSONObject()
                cd.forEach { (timeStr, dosageVal) ->
                    cdObj.put(timeStr, dosageVal)
                }
                mObj.put("customDosages", cdObj)
            }
            medArray.put(mObj)
        }
        root.put("medicines", medArray)

        val schedArray = JSONArray()
        schedules.forEach { sch ->
            val sObj = JSONObject()
            sObj.put("entryId", sch.entryId)
            // Guard: Explicitly serialize userId on schedule entry so deserialization and external backups preserve user profile linkage
            sObj.put("userId", sch.userId)
            sObj.put("medicineId", sch.medicineId)
            sObj.put("scheduledDateTime", sch.scheduledDateTime.toString())
            sObj.put("status", sch.status.name)
            // Guard: Omit null fields rather than writing empty strings to prevent ambiguous import semantics (null vs explicitly-cleared)
            sch.skipReason?.let { sObj.put("skipReason", it) }
            sch.takenAt?.let { sObj.put("takenAt", it.toString()) }
            if (sch.dosage != null) {
                sObj.put("dosage", sch.dosage)
            }
            sch.doseNotes?.let { sObj.put("doseNotes", it) }
            schedArray.put(sObj)
        }
        root.put("schedules", schedArray)

        return root.toString(4)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal fun buildPdfDocument(user: User, medicines: List<Medicine>, schedules: List<ScheduleEntry>): PdfDocument {
        return pdfReportGenerator.buildPdfDocument(user, medicines, schedules)
    }

    companion object {
        internal fun escapeCsv(value: Any?): String {
            val str = value?.toString() ?: ""
            return "\"${str.replace("\"", "\"\"")}\""
        }

        internal fun buildCsvContent(user: User, medicines: List<Medicine>, schedules: List<ScheduleEntry>): String {
            val csvBuilder = StringBuilder()

            // User Information Section
            csvBuilder.append("USER INFORMATION\n")
            csvBuilder.append("User ID,Full Name,Age,Gender,Contact Number,Allergies,Medical Conditions,Profile Picture Path,Is Current User\n")
            csvBuilder.append(
                "${escapeCsv(user.userId)}," +
                "${escapeCsv(user.fullName)}," +
                "${user.age}," +
                "${user.gender}," +
                "${escapeCsv(user.contactNumber)}," +
                "${escapeCsv(user.allergies ?: "")}," +
                "${escapeCsv(user.medicalConditions ?: "")}," +
                "${escapeCsv(user.profilePicPath ?: "")}," +
                "${user.isCurrentUser}\n\n"
            )

            // Medicines Section
            csvBuilder.append("MEDICINES\n")
            // Guard: Include Refill Threshold, Auto Deduct Stock, Is Archived, Custom Dosages, and Days of Week/Month to preserve complete inventory and frequency metadata in CSV exports
            csvBuilder.append("Medicine ID,User ID,Medication Name,Dosage,Dosage Unit,Times Per Day,Frequency Pattern,Interval Hours,Interval Days,Interval Weeks,Days Per Week,Days Per Month,Selected Days Of Week,Selected Days Of Month,Scheduled Times,Pill Shape,Pill Color,Doctor Notes,Start Date,End Date,Duration Days,Stock,Refill Threshold,Auto Deduct Stock,Is Archived,Custom Dosages,Image URI\n")
            medicines.forEach { medicine ->
                val scheduledTimesStr = medicine.scheduledTimes.joinToString(";") { it.toString() }
                val customDosagesStr = medicine.customDosages?.entries?.joinToString(";") { "${it.key}:${it.value}" } ?: ""
                val selectedDaysOfWeekStr = medicine.frequency.selectedDaysOfWeek?.joinToString(";") ?: ""
                val selectedDaysOfMonthStr = medicine.frequency.selectedDaysOfMonth?.joinToString(";") ?: ""
                csvBuilder.append(
                    "${escapeCsv(medicine.medicineId)}," +
                    "${escapeCsv(medicine.userId)}," +
                    "${escapeCsv(medicine.medicationName)}," +
                    "${medicine.dosage}," +
                    "${medicine.dosageUnit}," +
                    "${medicine.timesPerDay}," +
                    "${medicine.frequency.pattern}," +
                    "${medicine.frequency.intervalHours ?: ""}," +
                    "${medicine.frequency.intervalDays ?: ""}," +
                    "${medicine.frequency.intervalWeeks ?: ""}," +
                    "${medicine.frequency.daysPerWeek ?: ""}," +
                    "${medicine.frequency.daysPerMonth ?: ""}," +
                    "${escapeCsv(selectedDaysOfWeekStr)}," +
                    "${escapeCsv(selectedDaysOfMonthStr)}," +
                    "${escapeCsv(scheduledTimesStr)}," +
                    "${escapeCsv(medicine.pillShape.name)}," +
                    "${escapeCsv(medicine.pillColor)}," +
                    "${escapeCsv(medicine.notes ?: "")}," +
                    "${escapeCsv(medicine.startDate ?: "")}," +
                    "${escapeCsv(medicine.endDate ?: "")}," +
                    "${medicine.durationDays ?: ""}," +
                    "${medicine.currentStock ?: ""}," +
                    "${medicine.refillThreshold ?: ""}," +
                    "${medicine.autoDeductOnTake}," +
                    "${medicine.isArchived}," +
                    "${escapeCsv(customDosagesStr)}," +
                    "${escapeCsv(medicine.imageUri ?: "")}\n"
                )
            }
            csvBuilder.append("\n")

            // Schedules Section
            csvBuilder.append("SCHEDULES\n")
            csvBuilder.append("Entry ID,User ID,Medicine ID,Scheduled DateTime,Status,Skip Reason,Taken At,Dosage,Dose Notes\n")
            schedules.forEach { schedule ->
                csvBuilder.append(
                    "${escapeCsv(schedule.entryId)}," +
                    "${escapeCsv(schedule.userId)}," +
                    "${escapeCsv(schedule.medicineId)}," +
                    "${escapeCsv(schedule.scheduledDateTime)}," +
                    "${schedule.status}," +
                    "${escapeCsv(schedule.skipReason ?: "")}," +
                    "${escapeCsv(schedule.takenAt ?: "")}," +
                    "${schedule.dosage ?: ""}," +
                    "${escapeCsv(schedule.doseNotes ?: "")}\n"
                )
            }

            return csvBuilder.toString()
        }
    }

    suspend fun saveExportToFile(content: String, fileName: String): File {
        return withContext(Dispatchers.IO) {
            // Guard: Use getExportDirectory() fallback to filesDir when externalFilesDir is null/unmounted to prevent NPE
            val file = File(getExportDirectory(), fileName)
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
            clipData = android.content.ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share Export File").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
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

                val totalNeeded = customQuantities[med.medicineId] ?: med.calculateRefillQuantity(supplyDays)

                val showDosageInTitle = includeDosage && when (med.dosageUnit) {
                    com.example.dosezy.data.model.DosageUnit.MG,
                    com.example.dosezy.data.model.DosageUnit.MCG,
                    com.example.dosezy.data.model.DosageUnit.ML,
                    com.example.dosezy.data.model.DosageUnit.MEQ -> true
                    else -> med.dosage > 0
                }
                val dosageDisplay = if (med.dosage > 0) {
                    if (med.dosage % 1.0 == 0.0) "${med.dosage.toInt()}" else "${med.dosage}"
                } else ""
                val strengthUnitStr = med.dosageUnit.getLocalizedName(context)

                val orderUnitStr = if (isDrop) {
                    if (totalNeeded > 1) context.getString(com.example.dosezy.R.string.unit_bottles) else context.getString(com.example.dosezy.R.string.unit_bottle)
                } else when (med.dosageUnit) {
                    com.example.dosezy.data.model.DosageUnit.MG,
                    com.example.dosezy.data.model.DosageUnit.MCG,
                    com.example.dosezy.data.model.DosageUnit.MEQ -> {
                        context.getString(com.example.dosezy.R.string.unit_units)
                    }
                    else -> med.dosageUnit.getLocalizedName(context)
                }

                sb.append("${index + 1}. *${med.medicationName}*")
                if (showDosageInTitle && dosageDisplay.isNotBlank()) {
                    sb.append(" ($dosageDisplay $strengthUnitStr)")
                }
                sb.append("\n")
                sb.append("   • ").append(context.getString(com.example.dosezy.R.string.pharmacy_order_qty_needed, totalNeeded, orderUnitStr)).append("\n")

                if (includeStock && med.currentStock != null) {
                    val stockUnitStr = if (isDrop) {
                        context.getString(com.example.dosezy.R.string.unit_drop)
                    } else {
                        med.dosageUnit.getLocalizedName(context)
                    }
                    sb.append("   • ").append(context.getString(com.example.dosezy.R.string.pharmacy_order_current_stock, med.currentStock, stockUnitStr)).append("\n")
                }
                sb.append("\n")
            }
        }

        val footerConfirm = context.getString(com.example.dosezy.R.string.pharmacy_order_footer_confirm)
        val formattedConfirm = footerConfirm.lines().filter { it.isNotBlank() }.joinToString("\n") { "_${it.trim()}_" }
        sb.append(formattedConfirm).append("\n\n")

        val brandText = context.getString(com.example.dosezy.R.string.pharmacy_order_footer_brand)
        val formattedBrand = brandText.lines().filter { it.isNotBlank() }.joinToString("\n") { "_${it.trim()}_" }
        sb.append(formattedBrand)

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
