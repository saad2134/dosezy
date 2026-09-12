package com.example.dosezy.data.export

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.*
import com.example.dosezy.data.repository.ScheduleRepository
import com.google.gson.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupResult(
    val success: Boolean,
    val profilesRestored: Int = 0,
    val medicinesRestored: Int = 0,
    val schedulesRestored: Int = 0,
    val message: String = ""
)

enum class ConflictStrategy {
    CREATE_NEW,   // Create alongside as new profile (e.g. "John Doe (Imported)")
    MERGE,        // Merge medicines & dose logs under existing profile
    OVERWRITE,    // Overwrite existing profile completely
    SKIP          // Skip importing this profile
}

data class ImportableProfileSummary(
    val originalUserId: String,
    val name: String,
    val age: Int,
    val gender: String,
    val medicineCount: Int,
    val scheduleCount: Int,
    val avatarTempPath: String?,
    val isConflict: Boolean,
    val existingLocalUserId: String?,
    val profileFolder: File
)

data class ZipInspectionResult(
    val success: Boolean,
    val tempDir: File? = null,
    val exportedAt: String = "",
    val profiles: List<ImportableProfileSummary> = emptyList(),
    val message: String = ""
)

data class ProfileImportDecision(
    val summary: ImportableProfileSummary,
    val isSelected: Boolean = true,
    val conflictStrategy: ConflictStrategy = ConflictStrategy.CREATE_NEW
)

class BackupRestoreManager(
    private val context: Context,
    private val database: DosezyDatabase,
    private val scheduleRepository: ScheduleRepository
) {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, JsonDeserializer { json, _, _ ->
            try { LocalDateTime.parse(json.asString) } catch (_: Exception) { null }
        })
        .registerTypeAdapter(LocalDateTime::class.java, JsonSerializer<LocalDateTime> { src, _, _ ->
            JsonPrimitive(src.toString())
        })
        .registerTypeAdapter(LocalDate::class.java, JsonDeserializer { json, _, _ ->
            try { LocalDate.parse(json.asString) } catch (_: Exception) { null }
        })
        .registerTypeAdapter(LocalDate::class.java, JsonSerializer<LocalDate> { src, _, _ ->
            JsonPrimitive(src.toString())
        })
        .registerTypeAdapter(LocalTime::class.java, JsonDeserializer { json, _, _ ->
            try { LocalTime.parse(json.asString) } catch (_: Exception) { null }
        })
        .registerTypeAdapter(LocalTime::class.java, JsonSerializer<LocalTime> { src, _, _ ->
            JsonPrimitive(src.toString())
        })
        .create()

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createFullBackupZip(): File = withContext(Dispatchers.IO) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val exportDir = context.getExternalFilesDir(null) ?: context.filesDir
        val zipFile = File(exportDir, "dosezy_backup_v2.3.0_$timestamp.zip")

        val users = database.userDao().getAllUsersDirect()
        val profileIds = users.map { it.userId }

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            // 1. Write Manifest
            val manifestJson = JSONObject().apply {
                put("manifestVersion", 1)
                put("appVersion", com.example.dosezy.BuildConfig.VERSION_NAME)
                put("versionCode", com.example.dosezy.BuildConfig.VERSION_CODE)
                put("exportedAt", LocalDateTime.now().toString())
                put("profileCount", users.size)
                put("profileIds", JSONArray(profileIds))
            }.toString(2)

            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifestJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. Write Profiles Data & Assets
            for (user in users) {
                val profileDir = "profiles/${user.userId}"

                // profile.json
                zos.putNextEntry(ZipEntry("$profileDir/profile.json"))
                zos.write(gson.toJson(user).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Avatar image asset if available
                user.profilePicPath?.let { picPath ->
                    val picFile = File(picPath)
                    if (picFile.exists()) {
                        zos.putNextEntry(ZipEntry("$profileDir/avatar.jpg"))
                        picFile.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }

                // medicines.json
                val medicines = database.medicineDao().getMedicinesByUserDirect(user.userId)
                zos.putNextEntry(ZipEntry("$profileDir/medicines.json"))
                zos.write(gson.toJson(medicines).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Medicine image assets
                for (med in medicines) {
                    med.imageUri?.let { uriStr ->
                        val cleanPath = uriStr.removePrefix("file://")
                        val imgFile = File(cleanPath)
                        if (imgFile.exists()) {
                            zos.putNextEntry(ZipEntry("$profileDir/assets/${med.medicineId}.jpg"))
                            imgFile.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }

                // schedules.json
                val schedules = database.scheduleDao().getAllScheduleEntries(user.userId)
                zos.putNextEntry(ZipEntry("$profileDir/schedules.json"))
                zos.write(gson.toJson(schedules).toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }

        return@withContext zipFile
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun inspectBackupZip(uri: Uri): ZipInspectionResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext ZipInspectionResult(false, message = "Could not open backup file.")

            val tempDir = File(context.cacheDir, "dosezy_inspect_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val destFile = File(tempDir, entry.name)
                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { fos -> zis.copyTo(fos) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val manifestFile = File(tempDir, "manifest.json")
            if (!manifestFile.exists()) {
                tempDir.deleteRecursively()
                return@withContext ZipInspectionResult(false, message = "Invalid backup file: manifest.json missing.")
            }

            val profilesDir = File(tempDir, "profiles")
            if (!profilesDir.exists() || !profilesDir.isDirectory) {
                tempDir.deleteRecursively()
                return@withContext ZipInspectionResult(false, message = "Invalid backup file: profiles folder missing.")
            }

            val localUsers = database.userDao().getAllUsersDirect()
            val profileFolders = profilesDir.listFiles { f -> f.isDirectory } ?: emptyArray()
            val summaries = mutableListOf<ImportableProfileSummary>()

            for (pDir in profileFolders) {
                val profileJsonFile = File(pDir, "profile.json")
                if (!profileJsonFile.exists()) continue

                val user = parseUserFromJson(profileJsonFile.readText(Charsets.UTF_8))
                val medicinesFile = File(pDir, "medicines.json")
                val medicines = if (medicinesFile.exists()) parseMedicinesFromJson(medicinesFile.readText(Charsets.UTF_8)) else emptyList()

                val schedulesFile = File(pDir, "schedules.json")
                val schedules = if (schedulesFile.exists()) parseSchedulesFromJson(schedulesFile.readText(Charsets.UTF_8)) else emptyList()

                val avatarFile = File(pDir, "avatar.jpg")
                val avatarPath = if (avatarFile.exists()) avatarFile.absolutePath else null

                // Check for name or ID conflict
                val matchingLocalUser = localUsers.firstOrNull { 
                    it.userId == user.userId || it.fullName.trim().equals(user.fullName.trim(), ignoreCase = true)
                }

                summaries.add(
                    ImportableProfileSummary(
                        originalUserId = user.userId,
                        name = user.fullName,
                        age = user.age,
                        gender = user.gender.name,
                        medicineCount = medicines.size,
                        scheduleCount = schedules.size,
                        avatarTempPath = avatarPath,
                        isConflict = matchingLocalUser != null,
                        existingLocalUserId = matchingLocalUser?.userId,
                        profileFolder = pDir
                    )
                )
            }

            val exportedAtStr = try {
                val manifestObj = JsonParser.parseString(manifestFile.readText(Charsets.UTF_8)).asJsonObject
                manifestObj.get("exportedAt")?.asString ?: ""
            } catch (_: Exception) { "" }

            return@withContext ZipInspectionResult(
                success = true,
                tempDir = tempDir,
                exportedAt = exportedAtStr,
                profiles = summaries
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext ZipInspectionResult(false, message = "Inspection failed: ${e.localizedMessage}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun executeSelectiveRestore(
        tempDir: File,
        decisions: List<ProfileImportDecision>
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            var totalProfiles = 0
            var totalMedicines = 0
            var totalSchedules = 0
            var firstRestoredUserId: String? = null

            for (decision in decisions) {
                if (!decision.isSelected || decision.conflictStrategy == ConflictStrategy.SKIP) {
                    continue
                }

                val pDir = decision.summary.profileFolder
                val profileJsonFile = File(pDir, "profile.json")
                if (!profileJsonFile.exists()) continue

                val originalUser = parseUserFromJson(profileJsonFile.readText(Charsets.UTF_8))
                val medicines = if (File(pDir, "medicines.json").exists()) {
                    parseMedicinesFromJson(File(pDir, "medicines.json").readText(Charsets.UTF_8))
                } else emptyList()
                val schedules = if (File(pDir, "schedules.json").exists()) {
                    parseSchedulesFromJson(File(pDir, "schedules.json").readText(Charsets.UTF_8))
                } else emptyList()

                when (decision.conflictStrategy) {
                    ConflictStrategy.CREATE_NEW -> {
                        val newUserId = UUID.randomUUID().toString()
                        val newName = if (decision.summary.isConflict) "${originalUser.fullName} (Imported)" else originalUser.fullName

                        val avatarFile = File(pDir, "avatar.jpg")
                        val finalPicPath = if (avatarFile.exists()) {
                            val userAssetsDir = File(context.filesDir, "profiles/$newUserId")
                            userAssetsDir.mkdirs()
                            val targetAvatar = File(userAssetsDir, "avatar.jpg")
                            avatarFile.copyTo(targetAvatar, overwrite = true)
                            targetAvatar.absolutePath
                        } else null

                        val newUser = originalUser.copy(
                            userId = newUserId,
                            fullName = newName,
                            profilePicPath = finalPicPath,
                            isCurrentUser = (totalProfiles == 0)
                        )
                        database.userDao().insertUser(newUser)
                        if (firstRestoredUserId == null) firstRestoredUserId = newUserId
                        totalProfiles++

                        val medIdMap = mutableMapOf<String, String>()
                        for (med in medicines) {
                            val newMedId = UUID.randomUUID().toString()
                            medIdMap[med.medicineId] = newMedId

                            val medAssetFile = File(pDir, "assets/${med.medicineId}.jpg")
                            val finalImageUri = if (medAssetFile.exists()) {
                                val medAssetsDir = File(context.filesDir, "medicines/$newMedId")
                                medAssetsDir.mkdirs()
                                val targetMedAsset = File(medAssetsDir, "image.jpg")
                                medAssetFile.copyTo(targetMedAsset, overwrite = true)
                                "file://${targetMedAsset.absolutePath}"
                            } else null

                            val newMed = med.copy(
                                medicineId = newMedId,
                                userId = newUserId,
                                imageUri = finalImageUri
                            )
                            database.medicineDao().insertMedicine(newMed)
                            totalMedicines++
                        }

                        val newSchedules = schedules.mapNotNull { sch ->
                            val newMedId = medIdMap[sch.medicineId] ?: return@mapNotNull null
                            sch.copy(
                                entryId = UUID.randomUUID().toString(),
                                userId = newUserId,
                                medicineId = newMedId
                            )
                        }
                        if (newSchedules.isNotEmpty()) {
                            database.scheduleDao().insertScheduleEntries(newSchedules)
                            totalSchedules += newSchedules.size
                        }

                        scheduleRepository.rescheduleAllAlarms(newUserId, context)
                    }

                    ConflictStrategy.OVERWRITE -> {
                        val targetUserId = decision.summary.existingLocalUserId ?: originalUser.userId
                        
                        // Clear old records for this profile
                        database.scheduleDao().deleteScheduleByUser(targetUserId)
                        database.medicineDao().deleteMedicinesByUser(targetUserId)

                        val avatarFile = File(pDir, "avatar.jpg")
                        val finalPicPath = if (avatarFile.exists()) {
                            val userAssetsDir = File(context.filesDir, "profiles/$targetUserId")
                            userAssetsDir.mkdirs()
                            val targetAvatar = File(userAssetsDir, "avatar.jpg")
                            avatarFile.copyTo(targetAvatar, overwrite = true)
                            targetAvatar.absolutePath
                        } else originalUser.profilePicPath

                        val restoredUser = originalUser.copy(userId = targetUserId, profilePicPath = finalPicPath)
                        database.userDao().insertUser(restoredUser)
                        if (firstRestoredUserId == null) firstRestoredUserId = targetUserId
                        totalProfiles++

                        for (med in medicines) {
                            val medAssetFile = File(pDir, "assets/${med.medicineId}.jpg")
                            val finalImageUri = if (medAssetFile.exists()) {
                                val medAssetsDir = File(context.filesDir, "medicines/${med.medicineId}")
                                medAssetsDir.mkdirs()
                                val targetMedAsset = File(medAssetsDir, "image.jpg")
                                medAssetFile.copyTo(targetMedAsset, overwrite = true)
                                "file://${targetMedAsset.absolutePath}"
                            } else med.imageUri

                            val restoredMed = med.copy(userId = targetUserId, imageUri = finalImageUri)
                            database.medicineDao().insertMedicine(restoredMed)
                            totalMedicines++
                        }

                        val restoredSchedules = schedules.map { it.copy(userId = targetUserId) }
                        if (restoredSchedules.isNotEmpty()) {
                            database.scheduleDao().insertScheduleEntries(restoredSchedules)
                            totalSchedules += restoredSchedules.size
                        }

                        scheduleRepository.rescheduleAllAlarms(targetUserId, context)
                    }

                    ConflictStrategy.MERGE -> {
                        val targetUserId = decision.summary.existingLocalUserId ?: originalUser.userId
                        val existingMeds = database.medicineDao().getMedicinesByUserDirect(targetUserId)
                        if (firstRestoredUserId == null) firstRestoredUserId = targetUserId

                        val medIdMap = mutableMapOf<String, String>()
                        for (med in medicines) {
                            val match = existingMeds.firstOrNull { it.medicationName.trim().equals(med.medicationName.trim(), ignoreCase = true) }
                            if (match != null) {
                                medIdMap[med.medicineId] = match.medicineId
                            } else {
                                val newMedId = UUID.randomUUID().toString()
                                medIdMap[med.medicineId] = newMedId

                                val medAssetFile = File(pDir, "assets/${med.medicineId}.jpg")
                                val finalImageUri = if (medAssetFile.exists()) {
                                    val medAssetsDir = File(context.filesDir, "medicines/$newMedId")
                                    medAssetsDir.mkdirs()
                                    val targetMedAsset = File(medAssetsDir, "image.jpg")
                                    medAssetFile.copyTo(targetMedAsset, overwrite = true)
                                    "file://${targetMedAsset.absolutePath}"
                                } else null

                                val newMed = med.copy(
                                    medicineId = newMedId,
                                    userId = targetUserId,
                                    imageUri = finalImageUri
                                )
                                database.medicineDao().insertMedicine(newMed)
                                totalMedicines++
                            }
                        }

                        val existingSchedules = database.scheduleDao().getAllScheduleEntries(targetUserId)
                        val mergedSchedules = mutableListOf<ScheduleEntry>()

                        for (sch in schedules) {
                            val mappedMedId = medIdMap[sch.medicineId] ?: continue
                            val duplicate = existingSchedules.any { 
                                it.medicineId == mappedMedId && it.scheduledDateTime == sch.scheduledDateTime 
                            }
                            if (!duplicate) {
                                mergedSchedules.add(
                                    sch.copy(
                                        entryId = UUID.randomUUID().toString(),
                                        userId = targetUserId,
                                        medicineId = mappedMedId
                                    )
                                )
                            }
                        }

                        if (mergedSchedules.isNotEmpty()) {
                            database.scheduleDao().insertScheduleEntries(mergedSchedules)
                            totalSchedules += mergedSchedules.size
                        }

                        scheduleRepository.rescheduleAllAlarms(targetUserId, context)
                        totalProfiles++
                    }

                    ConflictStrategy.SKIP -> {}
                }
            }

            tempDir.deleteRecursively()

            return@withContext BackupResult(
                success = true,
                profilesRestored = totalProfiles,
                medicinesRestored = totalMedicines,
                schedulesRestored = totalSchedules,
                message = if (totalProfiles > 0) "Successfully imported $totalProfiles profile(s), $totalMedicines medicine(s), and $totalSchedules schedule entries!" else "No profiles were imported."
            )
        } catch (e: Exception) {
            e.printStackTrace()
            tempDir.deleteRecursively()
            return@withContext BackupResult(false, message = "Import failed: ${e.localizedMessage}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun restoreFullBackupFromUri(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val inspect = inspectBackupZip(uri)
        if (!inspect.success || inspect.tempDir == null) {
            return@withContext BackupResult(false, message = inspect.message)
        }
        val defaultDecisions = inspect.profiles.map { 
            ProfileImportDecision(
                summary = it,
                isSelected = true,
                conflictStrategy = if (it.isConflict) ConflictStrategy.CREATE_NEW else ConflictStrategy.CREATE_NEW
            )
        }
        return@withContext executeSelectiveRestore(inspect.tempDir, defaultDecisions)
    }

    private fun parseUserFromJson(jsonStr: String): User {
        val json = JsonParser.parseString(jsonStr).asJsonObject
        return User(
            userId = json.get("userId")?.asString ?: UUID.randomUUID().toString(),
            fullName = json.get("fullName")?.asString ?: "Restored User",
            age = json.get("age")?.asInt ?: 30,
            gender = try { Gender.valueOf(json.get("gender").asString) } catch (_: Exception) { Gender.DO_NOT_SPECIFY },
            contactNumber = json.get("contactNumber")?.asString ?: "",
            profilePicPath = json.get("profilePicPath")?.let { if (it.isJsonNull) null else it.asString },
            isCurrentUser = json.get("isCurrentUser")?.asBoolean ?: false,
            theme = try { Theme.valueOf(json.get("theme").asString) } catch (_: Exception) { Theme.SYSTEM },
            timeFormat = try { TimeFormat.valueOf(json.get("timeFormat").asString) } catch (_: Exception) { TimeFormat.HOUR_12 },
            language = try { Language.valueOf(json.get("language").asString) } catch (_: Exception) { Language.SYSTEM },
            considerLateAfter = json.get("considerLateAfter")?.asInt ?: 3,
            considerMissedAfter = json.get("considerMissedAfter")?.asInt ?: 6,
            snoozeDuration = json.get("snoozeDuration")?.asInt ?: 10
        )
    }

    private fun parseMedicinesFromJson(jsonStr: String): List<Medicine> {
        val array = JsonParser.parseString(jsonStr).asJsonArray
        val list = mutableListOf<Medicine>()
        for (elem in array) {
            val obj = elem.asJsonObject
            val timesArray = obj.getAsJsonArray("scheduledTimes") ?: JsonArray()
            val timesList = timesArray.mapNotNull {
                try { LocalTime.parse(it.asString) } catch (_: Exception) { null }
            }

            val freqObj = obj.getAsJsonObject("frequency")
            val freqPattern = try {
                FrequencyPattern.valueOf(freqObj.get("pattern").asString)
            } catch (_: Exception) {
                FrequencyPattern.DAILY
            }

            val selectedDaysOfWeek = freqObj.getAsJsonArray("selectedDaysOfWeek")?.mapNotNull { it.asInt }
            val selectedDaysOfMonth = freqObj.getAsJsonArray("selectedDaysOfMonth")?.mapNotNull { it.asInt }

            val frequency = Frequency(
                pattern = freqPattern,
                daysPerWeek = freqObj.get("daysPerWeek")?.let { if (it.isJsonNull) null else it.asInt },
                daysPerMonth = freqObj.get("daysPerMonth")?.let { if (it.isJsonNull) null else it.asInt },
                selectedDaysOfWeek = selectedDaysOfWeek,
                selectedDaysOfMonth = selectedDaysOfMonth
            )

            val med = Medicine(
                medicineId = obj.get("medicineId").asString,
                userId = obj.get("userId").asString,
                medicationName = obj.get("medicationName").asString,
                dosage = obj.get("dosage").asDouble,
                dosageUnit = try { DosageUnit.valueOf(obj.get("dosageUnit").asString) } catch (_: Exception) { DosageUnit.TABLET },
                timesPerDay = obj.get("timesPerDay").asInt,
                frequency = frequency,
                scheduledTimes = timesList,
                imageUri = obj.get("imageUri")?.let { if (it.isJsonNull) null else it.asString },
                currentStock = obj.get("currentStock")?.let { if (it.isJsonNull) null else it.asInt },
                refillThreshold = obj.get("refillThreshold")?.let { if (it.isJsonNull) null else it.asInt },
                autoDeductOnTake = obj.get("autoDeductOnTake")?.asBoolean ?: true
            )
            list.add(med)
        }
        return list
    }

    private fun parseSchedulesFromJson(jsonStr: String): List<ScheduleEntry> {
        val array = JsonParser.parseString(jsonStr).asJsonArray
        val list = mutableListOf<ScheduleEntry>()
        for (elem in array) {
            val obj = elem.asJsonObject
            val entry = ScheduleEntry(
                entryId = obj.get("entryId").asString,
                userId = obj.get("userId").asString,
                medicineId = obj.get("medicineId").asString,
                scheduledDateTime = LocalDateTime.parse(obj.get("scheduledDateTime").asString),
                status = try { MedicationStatus.valueOf(obj.get("status").asString) } catch (_: Exception) { MedicationStatus.PENDING },
                takenAt = obj.get("takenAt")?.let { if (it.isJsonNull) null else try { LocalDateTime.parse(it.asString) } catch (_: Exception) { null } }
            )
            list.add(entry)
        }
        return list
    }
}
