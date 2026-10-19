package com.example.dosezy.data.export

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.withTransaction
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.*
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.utils.SoundUtils
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
            try {
                val str = json.asString.trim()
                str.toLongOrNull()?.let { millis ->
                    java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                } ?: LocalDate.parse(str)
            } catch (_: Exception) { null }
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
        val zipFile = File(exportDir, "dosezy_backup_v${com.example.dosezy.BuildConfig.VERSION_NAME}_$timestamp.zip")

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
                // Guard: Remove file:// URI prefix from profilePicPath so File(cleanPath).exists() accurately resolves avatar files and avoids silently omitting avatars from backups
                user.profilePicPath?.let { picPath ->
                    val cleanPath = picPath.removePrefix("file://")
                    val picFile = File(cleanPath)
                    if (picFile.exists()) {
                        zos.putNextEntry(ZipEntry("$profileDir/avatar.jpg"))
                        picFile.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }

                // Custom alarm sound asset if available
                // Guard: Package custom alarm sound file into backup zip so custom ringtone audio is preserved across device migration
                user.customAlarmSoundPath?.let { soundPath ->
                    val cleanPath = soundPath.removePrefix("file://")
                    val soundFile = File(cleanPath)
                    if (soundFile.exists()) {
                        val ext = soundFile.extension.ifEmpty { "mp3" }
                        zos.putNextEntry(ZipEntry("$profileDir/custom_alarm.$ext"))
                        soundFile.inputStream().use { it.copyTo(zos) }
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

                // emergency_contacts.json (per-profile)
                val emPrefs = context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)
                // Guard: Fallback to contacts_json and contacts_json_default for active user if namespaced key is absent to preserve emergency contacts in backups
                val emContacts = emPrefs.getString("contacts_json_${user.userId}", null)
                    ?: if (user.isCurrentUser) {
                        (emPrefs.getString("contacts_json", null) ?: emPrefs.getString("contacts_json_default", null))?.also {
                            emPrefs.edit().putString("contacts_json_${user.userId}", it).apply()
                        }
                    } else null
                if (!emContacts.isNullOrEmpty()) {
                    zos.putNextEntry(ZipEntry("$profileDir/emergency_contacts.json"))
                    zos.write(emContacts.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }

                // Guard: Export per-profile emergency country selection to preserve emergency dialing preferences across restore
                val countryPrefs = context.getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
                val countryCode = countryPrefs.getString("selected_country_code_${user.userId}", null)
                    ?: if (user.isCurrentUser) countryPrefs.getString("selected_country_code", null) else null
                if (!countryCode.isNullOrEmpty()) {
                    zos.putNextEntry(ZipEntry("$profileDir/emergency_country.txt"))
                    zos.write(countryCode.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }
        }

        return@withContext zipFile
    }

    suspend fun inspectBackupZip(uri: Uri): ZipInspectionResult = withContext(Dispatchers.IO) {
        var tempDir: File? = null
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext ZipInspectionResult(false, message = "Could not open backup file.")

            val inspectDir = File(context.cacheDir, "dosezy_inspect_${System.currentTimeMillis()}")
            inspectDir.mkdirs()
            tempDir = inspectDir

            // Guard: Android 7.0/7.1 (API 24/25) lacks File.toPath(); use canonicalPath to prevent crash and Zip Slip vulnerabilities
            val canonicalTempDir = inspectDir.canonicalPath
            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
                        throw SecurityException("Invalid backup archive: path traversal detected for entry '$entryName'.")
                    }

                    val destFile = File(tempDir, entryName)
                    val canonicalDest = destFile.canonicalPath
                    if (!canonicalDest.startsWith(canonicalTempDir + File.separator) && canonicalDest != canonicalTempDir) {
                        throw SecurityException("Invalid backup archive: path traversal detected for entry '$entryName'.")
                    }
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

                // Guard: Defensively parse individual profile JSON to prevent a single corrupt profile folder from crashing inspection for remaining valid profiles
                val user = try {
                    parseUserFromJson(profileJsonFile.readText(Charsets.UTF_8))
                } catch (_: Exception) {
                    continue
                }
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
            // Guard: Clean up temporary extraction folder on corrupt or interrupted backup inspection to prevent disk cache leakage
            try { tempDir?.deleteRecursively() } catch (_: Exception) {}
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

                // Guard: Defensively parse profile JSON to prevent corrupted profile folder from crashing selective restore batch
                val originalUser = try {
                    parseUserFromJson(profileJsonFile.readText(Charsets.UTF_8))
                } catch (_: Exception) {
                    continue
                }
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

                        val soundAssetFile = pDir.listFiles { f -> f.isFile && f.name.startsWith("custom_alarm.") }?.firstOrNull()
                        // Guard: Unpack custom alarm sound into internal storage and update path, or fallback to SYSTEM_DEFAULT if asset is missing
                        val finalSoundPath = if (soundAssetFile != null && soundAssetFile.exists()) {
                            val soundDir = File(context.filesDir, "custom_sounds")
                            soundDir.mkdirs()
                            val ext = soundAssetFile.extension.ifEmpty { "mp3" }
                            val targetSound = File(soundDir, "custom_alarm_${System.currentTimeMillis()}_${newUserId.take(8)}.$ext")
                            soundAssetFile.copyTo(targetSound, overwrite = true)
                            targetSound.absolutePath
                        } else null

                        val resolvedAlarmSound = if (finalSoundPath != null) {
                            originalUser.alarmSound
                        } else if (originalUser.alarmSound == AlarmSound.CUSTOM) {
                            AlarmSound.SYSTEM_DEFAULT
                        } else {
                            originalUser.alarmSound
                        }

                        val existingUsers = database.userDao().getAllUsersDirect()
                        // Guard: Promote first restored user if no existing local profile is currently active to prevent leaving app with 0 active users
                        val shouldBeCurrent = existingUsers.none { it.isCurrentUser } && totalProfiles == 0
                        val newUser = originalUser.copy(
                            userId = newUserId,
                            fullName = newName,
                            profilePicPath = finalPicPath,
                            customAlarmSoundPath = finalSoundPath,
                            alarmSound = resolvedAlarmSound,
                            isCurrentUser = shouldBeCurrent
                        )

                        val medIdMap = mutableMapOf<String, String>()
                        val newMedicines = mutableListOf<Medicine>()
                        for (med in medicines) {
                            val newMedId = UUID.randomUUID().toString()
                            medIdMap[med.medicineId] = newMedId

                            val medAssetFile = File(pDir, "assets/${med.medicineId}.jpg")
                            val finalImageUri = if (medAssetFile.exists()) {
                                val medAssetsDir = File(context.filesDir, "medicines/$newMedId")
                                medAssetsDir.mkdirs()
                                val targetMedAsset = File(medAssetsDir, "image.jpg")
                                medAssetFile.copyTo(targetMedAsset, overwrite = true)
                                targetMedAsset.absolutePath
                            } else null

                            val newMed = med.copy(
                                medicineId = newMedId,
                                userId = newUserId,
                                imageUri = finalImageUri
                            )
                            newMedicines.add(newMed)
                        }

                        val newSchedules = schedules.mapNotNull { sch ->
                            val newMedId = medIdMap[sch.medicineId] ?: return@mapNotNull null
                            sch.copy(
                                entryId = createRestoredEntryId(newMedId, sch.entryId, sch.scheduledDateTime),
                                userId = newUserId,
                                medicineId = newMedId
                            )
                        }

                        // Guard: Execute database inserts within an atomic Room transaction to ensure partial restore state cannot be persisted if an error occurs
                        database.withTransaction {
                            database.userDao().insertUser(newUser)
                            for (med in newMedicines) {
                                database.medicineDao().insertMedicine(med)
                            }
                            if (newSchedules.isNotEmpty()) {
                                database.scheduleDao().insertScheduleEntries(newSchedules)
                            }
                        }

                        if (firstRestoredUserId == null) firstRestoredUserId = newUserId
                        totalProfiles++
                        totalMedicines += newMedicines.size
                        totalSchedules += newSchedules.size

                        scheduleRepository.reconcileLegacyScheduleEntries(newUserId, context)
                        scheduleRepository.rescheduleAllAlarms(newUserId, context)

                        val emFile = File(pDir, "emergency_contacts.json")
                        if (emFile.exists()) {
                            val emJson = emFile.readText(Charsets.UTF_8)
                            if (emJson.isNotBlank()) {
                                val emPrefs = context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)
                                emPrefs.edit().putString("contacts_json_$newUserId", emJson).apply()
                            }
                        }

                        // Guard: Restore per-profile emergency country selection for newly created profile
                        val countryFile = File(pDir, "emergency_country.txt")
                        if (countryFile.exists()) {
                            val cCode = countryFile.readText(Charsets.UTF_8).trim()
                            if (cCode.isNotBlank()) {
                                val emergencyPrefs = context.getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
                                emergencyPrefs.edit().putString("selected_country_code_$newUserId", cCode).apply()
                            }
                        }
                    }

                    ConflictStrategy.OVERWRITE -> {
                        val targetUserId = decision.summary.existingLocalUserId ?: originalUser.userId

                        // Cancel existing alarms before deleting records to prevent orphaned alarms
                        try {
                            val alarmScheduler = com.example.dosezy.notifications.AlarmScheduler(context)
                            val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                            val existingSchedules = scheduleRepository.getSchedulesByUserSync(targetUserId)
                            existingSchedules.forEach { entry ->
                                // Guard: Pass entry directly to cancelAlarm to bypass sequential blocking DB lookups
                                alarmScheduler.cancelAlarm(entry)
                                alarmScheduler.cancelSnooze(entry.entryId)
                                alarmScheduler.cancelNagging(entry.entryId)
                                alarmScheduler.cancelSlotAlarm(entry.userId, entry.scheduledDateTime)
                                // Guard: Cancel any active intake safeguard alarms for this entry to prevent orphaned re-alerts
                                alarmScheduler.cancelIntakeSafeguardAlarm(entry.entryId)
                                // Guard: Dismiss active reminder notifications from shade when overwriting profile
                                notifManager?.cancel(entry.entryId.hashCode())
                            }
                            val existingLocalMedsBefore = database.medicineDao().getMedicinesByUserDirect(targetUserId)
                            existingLocalMedsBefore.forEach { med ->
                                notifManager?.cancel((med.medicineId + "_refill").hashCode())
                            }
                            // Guard: Dismiss active ADHD intake countdown notification if active countdown references overwritten profile
                            val activeCountdown = com.example.dosezy.notifications.IntakeSafeguardManager.activeCountdown.value
                            if (activeCountdown != null && existingSchedules.any { it.entryId == activeCountdown.entryId || activeCountdown.entryIds.contains(it.entryId) }) {
                                com.example.dosezy.notifications.IntakeSafeguardManager.cancelCountdown(context)
                            }
                        } catch (_: Exception) {}

                        val localUsers = database.userDao().getAllUsersDirect()
                        val existingLocalUser = localUsers.firstOrNull { it.userId == targetUserId }
                        // Guard: Preserve existing local user's active status, or promote only if no local profile is currently active and this is the first restored profile, preventing duplicate active users
                        val shouldBeCurrent = existingLocalUser?.isCurrentUser ?: (localUsers.none { it.isCurrentUser } && totalProfiles == 0)
                        val existingLocalMeds = database.medicineDao().getMedicinesByUserDirect(targetUserId)

                        val avatarFile = File(pDir, "avatar.jpg")
                        // Guard: If backup archive lacks an avatar asset, fall back to existing local user's avatar if valid, or null to prevent storing dead file paths from previous devices
                        val finalPicPath = if (avatarFile.exists()) {
                            val userAssetsDir = File(context.filesDir, "profiles/$targetUserId")
                            userAssetsDir.mkdirs()
                            val targetAvatar = File(userAssetsDir, "avatar.jpg")
                            avatarFile.copyTo(targetAvatar, overwrite = true)
                            targetAvatar.absolutePath
                        } else existingLocalUser?.profilePicPath?.takeIf { File(it.removePrefix("file://")).exists() }

                        val soundAssetFile = pDir.listFiles { f -> f.isFile && f.name.startsWith("custom_alarm.") }?.firstOrNull()
                        // Guard: Unpack backup custom alarm sound or fall back to valid local sound, and delete overwritten old sound file
                        val finalSoundPath = if (soundAssetFile != null && soundAssetFile.exists()) {
                            val soundDir = File(context.filesDir, "custom_sounds")
                            soundDir.mkdirs()
                            val ext = soundAssetFile.extension.ifEmpty { "mp3" }
                            val targetSound = File(soundDir, "custom_alarm_${System.currentTimeMillis()}_${targetUserId.take(8)}.$ext")
                            soundAssetFile.copyTo(targetSound, overwrite = true)
                            targetSound.absolutePath
                        } else existingLocalUser?.customAlarmSoundPath?.takeIf { File(it.removePrefix("file://")).exists() }

                        if (existingLocalUser?.customAlarmSoundPath != null && existingLocalUser.customAlarmSoundPath != finalSoundPath) {
                            SoundUtils.deleteCustomAudio(context, existingLocalUser.customAlarmSoundPath)
                        }

                        val resolvedAlarmSound = if (finalSoundPath != null) {
                            originalUser.alarmSound
                        } else if (originalUser.alarmSound == AlarmSound.CUSTOM) {
                            AlarmSound.SYSTEM_DEFAULT
                        } else {
                            originalUser.alarmSound
                        }

                        val restoredUser = originalUser.copy(
                            userId = targetUserId,
                            profilePicPath = finalPicPath,
                            customAlarmSoundPath = finalSoundPath,
                            alarmSound = resolvedAlarmSound,
                            isCurrentUser = shouldBeCurrent
                        )

                        val medIdMap = mutableMapOf<String, String>()
                        val restoredMedicines = mutableListOf<Medicine>()
                        for (med in medicines) {
                            // Guard: Generate new unique medicineId in OVERWRITE to prevent ID collisions with other profiles on the target device
                            val newMedId = UUID.randomUUID().toString()
                            medIdMap[med.medicineId] = newMedId

                            val medAssetFile = File(pDir, "assets/${med.medicineId}.jpg")
                            // Guard: Fall back to existing local medicine image if valid, or null to avoid storing dead file paths from previous devices
                            val finalImageUri = if (medAssetFile.exists()) {
                                val medAssetsDir = File(context.filesDir, "medicines/$newMedId")
                                medAssetsDir.mkdirs()
                                val targetMedAsset = File(medAssetsDir, "image.jpg")
                                medAssetFile.copyTo(targetMedAsset, overwrite = true)
                                targetMedAsset.absolutePath
                            } else existingLocalMeds.firstOrNull { it.medicationName.trim().equals(med.medicationName.trim(), ignoreCase = true) }?.imageUri?.takeIf { File(it.removePrefix("file://")).exists() }

                            val restoredMed = med.copy(medicineId = newMedId, userId = targetUserId, imageUri = finalImageUri)
                            restoredMedicines.add(restoredMed)
                        }

                        // Guard: Map schedule entries to remapped medicine IDs, generate deterministic entryIds, and filter out orphaned schedules referencing missing medicines to prevent ForeignKey constraint failures
                        val restoredSchedules = schedules.mapNotNull { sch ->
                            val mappedMedId = medIdMap[sch.medicineId] ?: return@mapNotNull null
                            sch.copy(
                                entryId = createRestoredEntryId(mappedMedId, sch.entryId, sch.scheduledDateTime),
                                userId = targetUserId,
                                medicineId = mappedMedId
                            )
                        }

                        // Guard: Delete orphaned internal storage medicine images not retained by restored medicines to prevent storage leaks
                        try {
                            val retainedPaths = restoredMedicines.mapNotNull { it.imageUri?.removePrefix("file://") }.toSet()
                            val filesDir = context.filesDir
                            existingLocalMeds.forEach { oldMed ->
                                oldMed.imageUri?.let { uriStr ->
                                    val cleanPath = uriStr.removePrefix("file://")
                                    if (!retainedPaths.contains(cleanPath)) {
                                        val file = File(cleanPath)
                                        if (file.exists() && (file.parentFile?.canonicalPath == filesDir.canonicalPath || file.canonicalPath.startsWith(filesDir.canonicalPath + File.separator))) {
                                            file.delete()
                                            file.parentFile?.delete()
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {}

                        // Guard: Execute clear and restore within an atomic Room transaction so that if any failure occurs midway, previous profile data is rolled back rather than permanently lost
                        database.withTransaction {
                            // Guard: Purge conflicting original user record if existingLocalUserId differs from originalUser.userId to prevent duplicate user entities and orphan foreign keys (#BUG-RESTORE-ID-COLLISION)
                            if (decision.summary.existingLocalUserId != null && decision.summary.existingLocalUserId != originalUser.userId) {
                                database.scheduleDao().deleteScheduleByUser(originalUser.userId)
                                database.medicineDao().deleteMedicinesByUser(originalUser.userId)
                                database.userDao().deleteUserById(originalUser.userId)
                            }

                            database.scheduleDao().deleteScheduleByUser(targetUserId)
                            database.medicineDao().deleteMedicinesByUser(targetUserId)

                            database.userDao().insertUser(restoredUser)
                            for (med in restoredMedicines) {
                                database.medicineDao().insertMedicine(med)
                            }
                            if (restoredSchedules.isNotEmpty()) {
                                database.scheduleDao().insertScheduleEntries(restoredSchedules)
                            }
                        }

                        if (firstRestoredUserId == null) firstRestoredUserId = targetUserId
                        totalProfiles++
                        totalMedicines += restoredMedicines.size
                        totalSchedules += restoredSchedules.size

                        scheduleRepository.reconcileLegacyScheduleEntries(targetUserId, context)
                        scheduleRepository.rescheduleAllAlarms(targetUserId, context)

                        val emFile = File(pDir, "emergency_contacts.json")
                        val emPrefs = context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)
                        if (emFile.exists()) {
                            val emJson = emFile.readText(Charsets.UTF_8)
                            if (emJson.isNotBlank()) {
                                emPrefs.edit().putString("contacts_json_$targetUserId", emJson).apply()
                            } else {
                                // Guard: Scrub stale emergency contacts when overwriting a profile with an empty emergency contacts file
                                emPrefs.edit().remove("contacts_json_$targetUserId").apply()
                            }
                        } else {
                            // Guard: Scrub stale emergency contacts when overwriting a profile with a backup archive lacking emergency contacts
                            emPrefs.edit().remove("contacts_json_$targetUserId").apply()
                        }

                        // Guard: Restore emergency country selection or scrub stale country code when overwriting profile
                        val countryFile = File(pDir, "emergency_country.txt")
                        val emergencyPrefs = context.getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
                        if (countryFile.exists()) {
                            val cCode = countryFile.readText(Charsets.UTF_8).trim()
                            if (cCode.isNotBlank()) {
                                emergencyPrefs.edit().putString("selected_country_code_$targetUserId", cCode).apply()
                            } else {
                                emergencyPrefs.edit().remove("selected_country_code_$targetUserId").apply()
                            }
                        } else {
                            emergencyPrefs.edit().remove("selected_country_code_$targetUserId").apply()
                        }

                        // Guard: Scrub stale preferences for conflicting original user ID when overwritten into existing local profile (#BUG-RESTORE-ID-COLLISION)
                        if (decision.summary.existingLocalUserId != null && decision.summary.existingLocalUserId != originalUser.userId) {
                            emPrefs.edit().remove("contacts_json_${originalUser.userId}").apply()
                            emergencyPrefs.edit().remove("selected_country_code_${originalUser.userId}").apply()
                        }
                    }

                    ConflictStrategy.MERGE -> {
                        val targetUserId = decision.summary.existingLocalUserId ?: originalUser.userId
                        val existingMeds = database.medicineDao().getMedicinesByUserDirect(targetUserId)
                        if (firstRestoredUserId == null) firstRestoredUserId = targetUserId

                        val medIdMap = mutableMapOf<String, String>()
                        val newMedsToInsert = mutableListOf<Medicine>()
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
                                    // Guard: Save canonical absolutePath without file:// scheme to ensure consistent image loading across Coil and file pickers
                                    targetMedAsset.absolutePath
                                } else null

                                val newMed = med.copy(
                                    medicineId = newMedId,
                                    userId = targetUserId,
                                    imageUri = finalImageUri
                                )
                                newMedsToInsert.add(newMed)
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
                                        entryId = createRestoredEntryId(mappedMedId, sch.entryId, sch.scheduledDateTime),
                                        userId = targetUserId,
                                        medicineId = mappedMedId
                                    )
                                )
                            }
                        }

                        // Guard: Execute MERGE inserts within an atomic Room transaction to ensure database consistency on failure
                        database.withTransaction {
                            // Guard: Ensure parent User entity exists before inserting child medicines and schedules to prevent SQLiteConstraintException FOREIGN KEY failures when merging into missing or deleted local profiles
                            if (database.userDao().getUserByIdDirect(targetUserId) == null) {
                                val localUsers = database.userDao().getAllUsersDirect()
                                val avatarFile = File(pDir, "avatar.jpg")
                                val finalPicPath = if (avatarFile.exists()) {
                                    val userAssetsDir = File(context.filesDir, "profiles/$targetUserId")
                                    userAssetsDir.mkdirs()
                                    val targetAvatar = File(userAssetsDir, "avatar.jpg")
                                    avatarFile.copyTo(targetAvatar, overwrite = true)
                                    targetAvatar.absolutePath
                                } else null

                                val soundAssetFile = pDir.listFiles { f -> f.isFile && f.name.startsWith("custom_alarm.") }?.firstOrNull()
                                val finalSoundPath = if (soundAssetFile != null && soundAssetFile.exists()) {
                                    val soundDir = File(context.filesDir, "custom_sounds")
                                    soundDir.mkdirs()
                                    val ext = soundAssetFile.extension.ifEmpty { "mp3" }
                                    val targetSound = File(soundDir, "custom_alarm_${System.currentTimeMillis()}_${targetUserId.take(8)}.$ext")
                                    soundAssetFile.copyTo(targetSound, overwrite = true)
                                    targetSound.absolutePath
                                } else null

                                val resolvedAlarmSound = if (finalSoundPath != null) {
                                    originalUser.alarmSound
                                } else if (originalUser.alarmSound == AlarmSound.CUSTOM) {
                                    AlarmSound.SYSTEM_DEFAULT
                                } else {
                                    originalUser.alarmSound
                                }

                                val shouldBeCurrent = localUsers.none { it.isCurrentUser } && totalProfiles == 0
                                val restoredUser = originalUser.copy(
                                    userId = targetUserId,
                                    profilePicPath = finalPicPath,
                                    customAlarmSoundPath = finalSoundPath,
                                    alarmSound = resolvedAlarmSound,
                                    isCurrentUser = shouldBeCurrent
                                )
                                database.userDao().insertUser(restoredUser)
                            }

                            for (newMed in newMedsToInsert) {
                                database.medicineDao().insertMedicine(newMed)
                            }
                            if (mergedSchedules.isNotEmpty()) {
                                database.scheduleDao().insertScheduleEntries(mergedSchedules)
                            }
                        }

                        totalMedicines += newMedsToInsert.size
                        totalSchedules += mergedSchedules.size

                        scheduleRepository.reconcileLegacyScheduleEntries(targetUserId, context)
                        scheduleRepository.rescheduleAllAlarms(targetUserId, context)

                        val emFile = File(pDir, "emergency_contacts.json")
                        if (emFile.exists()) {
                            val emJson = emFile.readText(Charsets.UTF_8)
                            if (emJson.isNotBlank()) {
                                val emPrefs = context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)
                                if (!emPrefs.contains("contacts_json_$targetUserId")) {
                                    emPrefs.edit().putString("contacts_json_$targetUserId", emJson).apply()
                                }
                            }
                        }

                        // Guard: Restore per-profile emergency country selection for merged profile if not already set
                        val countryFile = File(pDir, "emergency_country.txt")
                        if (countryFile.exists()) {
                            val cCode = countryFile.readText(Charsets.UTF_8).trim()
                            if (cCode.isNotBlank()) {
                                val emergencyPrefs = context.getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
                                if (!emergencyPrefs.contains("selected_country_code_$targetUserId")) {
                                    emergencyPrefs.edit().putString("selected_country_code_$targetUserId", cCode).apply()
                                }
                            }
                        }
                        totalProfiles++
                    }

                    ConflictStrategy.SKIP -> {}
                }
            }

            tempDir.deleteRecursively()

            // Guard: Update active home screen app widgets upon successful restore so restored schedules and profiles are immediately visible (#BUG-RESTORE-WIDGET-REFRESH)
            if (totalProfiles > 0) {
                try {
                    com.example.dosezy.widget.DosezyAppWidgetProvider.updateAppWidgets(context)
                } catch (_: Exception) {}
            }

            // Guard: Require totalProfiles > 0 for success to prevent zero-profile imports from falsely reporting success or advancing onboarding
            return@withContext BackupResult(
                success = totalProfiles > 0,
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

    internal fun parseUserFromJson(jsonStr: String): User {
        val json = JsonParser.parseString(jsonStr).asJsonObject
        // Guard: Defensively parse user enums with .trim().uppercase() to prevent IllegalArgumentException resetting preferences to defaults on lowercase or padded values
        val genderVal = json.get("gender")?.takeUnless { it.isJsonNull }?.asString?.let { runCatching { Gender.valueOf(it.trim().uppercase()) }.getOrNull() } ?: Gender.DO_NOT_SPECIFY
        val themeVal = json.get("theme")?.takeUnless { it.isJsonNull }?.asString?.let { runCatching { Theme.valueOf(it.trim().uppercase()) }.getOrNull() } ?: Theme.SYSTEM
        val timeFormatVal = json.get("timeFormat")?.takeUnless { it.isJsonNull }?.asString?.let { runCatching { TimeFormat.valueOf(it.trim().uppercase()) }.getOrNull() } ?: TimeFormat.HOUR_12
        val languageVal = json.get("language")?.takeUnless { it.isJsonNull }?.asString?.let { runCatching { Language.valueOf(it.trim().uppercase()) }.getOrNull() } ?: Language.SYSTEM
        val alarmSoundVal = json.get("alarmSound")?.takeUnless { it.isJsonNull }?.asString?.let { runCatching { AlarmSound.valueOf(it.trim().uppercase()) }.getOrNull() } ?: AlarmSound.SYSTEM_DEFAULT

        return User(
            userId = json.get("userId")?.takeUnless { it.isJsonNull }?.asString ?: UUID.randomUUID().toString(),
            fullName = json.get("fullName")?.takeUnless { it.isJsonNull }?.asString ?: "Restored User",
            age = json.get("age")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull() ?: 30,
            gender = genderVal,
            contactNumber = json.get("contactNumber")?.takeUnless { it.isJsonNull }?.asString ?: "",
            profilePicPath = json.get("profilePicPath")?.let { if (it.isJsonNull) null else it.asString },
            isCurrentUser = json.get("isCurrentUser")?.takeUnless { it.isJsonNull }?.runCatching { asBoolean }?.getOrNull() ?: false,
            theme = themeVal,
            timeFormat = timeFormatVal,
            language = languageVal,
            considerLateAfter = json.get("considerLateAfter")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull() ?: 3,
            considerMissedAfter = json.get("considerMissedAfter")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull() ?: 6,
            snoozeDuration = json.get("snoozeDuration")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull() ?: 10,
            allergies = json.get("allergies")?.let { if (it.isJsonNull) null else it.asString },
            medicalConditions = json.get("medicalConditions")?.let { if (it.isJsonNull) null else it.asString },
            naggingRemindersEnabled = json.get("naggingRemindersEnabled")?.takeUnless { it.isJsonNull }?.runCatching { asBoolean }?.getOrNull() ?: false,
            naggingIntervalMinutes = json.get("naggingIntervalMinutes")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull() ?: 5,
            naggingMaxRepeats = json.get("naggingMaxRepeats")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull() ?: 3,
            alarmSound = alarmSoundVal,
            customAlarmSoundPath = json.get("customAlarmSoundPath")?.let { if (it.isJsonNull || it.asString.isBlank()) null else it.asString },
            customAlarmSoundTitle = json.get("customAlarmSoundTitle")?.let { if (it.isJsonNull || it.asString.isBlank()) null else it.asString },
            alarmDurationSeconds = json.get("alarmDurationSeconds")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull() ?: 0,
            allowDoseSkipping = json.get("allowDoseSkipping")?.takeUnless { it.isJsonNull }?.runCatching { asBoolean }?.getOrNull() ?: false,
            allowCustomDoseTime = json.get("allowCustomDoseTime")?.takeUnless { it.isJsonNull }?.runCatching { asBoolean }?.getOrNull() ?: false,
            hideAddMedicineNavButton = json.get("hideAddMedicineNavButton")?.takeUnless { it.isJsonNull }?.runCatching { asBoolean }?.getOrNull() ?: false,
            allowDoseUndo = json.get("allowDoseUndo")?.takeUnless { it.isJsonNull }?.runCatching { asBoolean }?.getOrNull() ?: false,
            allowDoseNotes = json.get("allowDoseNotes")?.takeUnless { it.isJsonNull }?.runCatching { asBoolean }?.getOrNull() ?: false,
            promptDoseNotes = json.get("promptDoseNotes")?.takeUnless { it.isJsonNull }?.runCatching { asBoolean }?.getOrNull() ?: false,
            // Guard: Defensively parse slide actions, timeline mode, and thicker calendar highlight with non-breaking defaults for legacy backup archives
            slideActionsEnabled = json.get("slideActionsEnabled")?.takeUnless { it.isJsonNull }?.runCatching { asBoolean }?.getOrNull() ?: true,
            timelineModeEnabled = json.get("timelineModeEnabled")?.takeUnless { it.isJsonNull }?.runCatching { asBoolean }?.getOrNull() ?: false,
            thickerCalendarDayHighlight = json.get("thickerCalendarDayHighlight")?.takeUnless { it.isJsonNull }?.runCatching { asBoolean }?.getOrNull() ?: false,
            // Guard: Defensively parse intake safeguard preferences with non-breaking defaults for legacy backup archives (#104)
            intakeSafeguardEnabled = json.get("intakeSafeguardEnabled")?.takeUnless { it.isJsonNull }?.runCatching { asBoolean }?.getOrNull() ?: false,
            intakeCountdownSeconds = json.get("intakeCountdownSeconds")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull() ?: 120,
            // Guard: Defensively parse lockscreen privacy and custom snooze popup settings for legacy backup archives
            lockscreenDetailsEnabled = json.get("lockscreenDetailsEnabled")?.takeUnless { it.isJsonNull }?.runCatching { asBoolean }?.getOrNull() ?: true,
            customSnoozePopupEnabled = json.get("customSnoozePopupEnabled")?.takeUnless { it.isJsonNull }?.runCatching { asBoolean }?.getOrNull() ?: false
        )
    }

    internal fun parseMedicinesFromJson(jsonStr: String): List<Medicine> {
        val array = try { JsonParser.parseString(jsonStr).asJsonArray } catch (_: Exception) { return emptyList() }
        val list = mutableListOf<Medicine>()
        for (elem in array) {
            val obj = try { elem.asJsonObject } catch (_: Exception) { continue }
            val timesArray = if (obj.has("scheduledTimes") && !obj.get("scheduledTimes").isJsonNull && obj.get("scheduledTimes").isJsonArray) {
                obj.getAsJsonArray("scheduledTimes")
            } else JsonArray()
            val timesList = timesArray.mapNotNull {
                try { LocalTime.parse(it.asString) } catch (_: Exception) { null }
            }

            // Guard: Defensively parse frequency object (nested or flattened from DataExporter/legacy backups) to prevent resetting non-daily schedules to DAILY
            val freqObj = if (obj.has("frequency") && !obj.get("frequency").isJsonNull && obj.get("frequency").isJsonObject) {
                obj.getAsJsonObject("frequency")
            } else null

            val frequency = if (freqObj != null) {
                val freqPattern = try {
                    FrequencyPattern.valueOf((freqObj.get("pattern")?.takeUnless { it.isJsonNull }?.asString ?: "DAILY").trim().uppercase())
                } catch (_: Exception) {
                    FrequencyPattern.DAILY
                }
                val selectedDaysOfWeek = freqObj.getAsJsonArray("selectedDaysOfWeek")?.mapNotNull { if (it.isJsonNull) null else runCatching { it.asInt }.getOrNull() }
                val selectedDaysOfMonth = freqObj.getAsJsonArray("selectedDaysOfMonth")?.mapNotNull { if (it.isJsonNull) null else runCatching { it.asInt }.getOrNull() }
                Frequency(
                    pattern = freqPattern,
                    daysPerWeek = freqObj.get("daysPerWeek")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull(),
                    daysPerMonth = freqObj.get("daysPerMonth")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull(),
                    selectedDaysOfWeek = selectedDaysOfWeek,
                    selectedDaysOfMonth = selectedDaysOfMonth,
                    intervalHours = freqObj.get("intervalHours")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull(),
                    intervalDays = freqObj.get("intervalDays")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull(),
                    intervalWeeks = freqObj.get("intervalWeeks")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull()
                )
            } else if (obj.has("frequencyPattern") || obj.has("pattern")) {
                // Guard: Support flattened frequency attributes from DataExporter exports and legacy backups to prevent silent schedule corruption
                val rawPattern = (obj.get("frequencyPattern") ?: obj.get("pattern"))?.takeUnless { it.isJsonNull }?.asString ?: "DAILY"
                val freqPattern = try {
                    FrequencyPattern.valueOf(rawPattern.trim().uppercase())
                } catch (_: Exception) {
                    FrequencyPattern.DAILY
                }
                val selectedDaysOfWeek = obj.getAsJsonArray("selectedDaysOfWeek")?.mapNotNull { if (it.isJsonNull) null else runCatching { it.asInt }.getOrNull() }
                val selectedDaysOfMonth = obj.getAsJsonArray("selectedDaysOfMonth")?.mapNotNull { if (it.isJsonNull) null else runCatching { it.asInt }.getOrNull() }
                Frequency(
                    pattern = freqPattern,
                    daysPerWeek = obj.get("daysPerWeek")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull(),
                    daysPerMonth = obj.get("daysPerMonth")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull(),
                    selectedDaysOfWeek = selectedDaysOfWeek,
                    selectedDaysOfMonth = selectedDaysOfMonth,
                    intervalHours = obj.get("intervalHours")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull(),
                    intervalDays = obj.get("intervalDays")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull(),
                    intervalWeeks = obj.get("intervalWeeks")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull()
                )
            } else {
                Frequency(FrequencyPattern.DAILY)
            }

            // Guard: Defensively parse pill shape with .trim().uppercase() to prevent silent fallback to ROUND on lowercase strings
            val pillShape = obj.get("pillShape")?.takeUnless { it.isJsonNull }?.asString?.let { runCatching { PillShape.valueOf(it.trim().uppercase()) }.getOrNull() } ?: PillShape.ROUND
            val pillColor = obj.get("pillColor")?.takeUnless { it.isJsonNull }?.asString ?: "#1193D4"
            // Guard: Parse epoch-millis and ISO-8601 date strings to prevent dropping course bounds on restore
            val startDate = obj.get("startDate")?.takeUnless { it.isJsonNull }?.asString?.let { raw ->
                raw.trim().toLongOrNull()?.let { millis ->
                    java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                } ?: runCatching { LocalDate.parse(raw.trim()) }.getOrNull()
            }
            val endDate = obj.get("endDate")?.takeUnless { it.isJsonNull }?.asString?.let { raw ->
                raw.trim().toLongOrNull()?.let { millis ->
                    java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                } ?: runCatching { LocalDate.parse(raw.trim()) }.getOrNull()
            }

            // Guard: Safe-extract required primitives with sane defaults to prevent NullPointerException from aborting archive inspection
            val medName = obj.get("medicationName")?.takeUnless { it.isJsonNull }?.asString ?: "Unknown Medication"
            val medId = obj.get("medicineId")?.takeUnless { it.isJsonNull }?.asString ?: UUID.randomUUID().toString()
            val uId = obj.get("userId")?.takeUnless { it.isJsonNull }?.asString ?: ""
            val dosageVal = obj.get("dosage")?.takeUnless { it.isJsonNull }?.runCatching { asDouble }?.getOrNull() ?: 1.0
            // Guard: Safe-parse dosage unit with normalization for aliases (puffs, actuations, ampules, mEq) and fallback to TABLET
            val dosageUnitVal = obj.get("dosageUnit")?.takeUnless { it.isJsonNull }?.asString?.let { raw ->
                val normalized = raw.trim().uppercase()
                when (normalized) {
                    "MEQ", "MILLIEQUIVALENT", "MILLIEQUIVALENTS" -> DosageUnit.MEQ
                    "PUFF", "PUFFS", "ACTUATION", "ACTUATIONS" -> DosageUnit.PUFF
                    "AMPULE", "AMPULES", "AMP", "AMPOULE", "AMPOULES" -> DosageUnit.AMPULE
                    else -> runCatching { DosageUnit.valueOf(normalized) }.getOrNull()
                }
            } ?: DosageUnit.TABLET
            val timesPerDayVal = obj.get("timesPerDay")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull() ?: 1

            val med = Medicine(
                medicineId = medId,
                userId = uId,
                medicationName = medName,
                dosage = dosageVal,
                dosageUnit = dosageUnitVal,
                timesPerDay = timesPerDayVal,
                frequency = frequency,
                scheduledTimes = timesList,
                imageUri = obj.get("imageUri")?.takeUnless { it.isJsonNull }?.asString,
                currentStock = obj.get("currentStock")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull(),
                refillThreshold = obj.get("refillThreshold")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull(),
                autoDeductOnTake = obj.get("autoDeductOnTake")?.takeUnless { it.isJsonNull }?.runCatching { asBoolean }?.getOrNull() ?: true,
                pillShape = pillShape,
                pillColor = pillColor,
                notes = obj.get("notes")?.takeUnless { it.isJsonNull }?.asString,
                startDate = startDate,
                endDate = endDate,
                durationDays = obj.get("durationDays")?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull(),
                isArchived = obj.get("isArchived")?.takeUnless { it.isJsonNull }?.runCatching { asBoolean }?.getOrNull() ?: false,
                customDosages = if (obj.has("customDosages") && !obj.get("customDosages").isJsonNull && obj.get("customDosages").isJsonObject) {
                    val cObj = obj.getAsJsonObject("customDosages")
                    val map = mutableMapOf<String, Double>()
                    cObj.entrySet().forEach { (k, v) ->
                        try { map[k] = v.asDouble } catch (_: Exception) {}
                    }
                    if (map.isNotEmpty()) map else null
                } else null
            )
            list.add(med)
        }
        return list
    }

    internal fun parseSchedulesFromJson(jsonStr: String): List<ScheduleEntry> {
        val array = try { JsonParser.parseString(jsonStr).asJsonArray } catch (_: Exception) { return emptyList() }
        val list = mutableListOf<ScheduleEntry>()
        for (elem in array) {
            val obj = try { elem.asJsonObject } catch (_: Exception) { continue }
            // Guard: Safe-extract scheduledDateTime and skip entry if missing or invalid to prevent unhandled NPE aborting zip inspection
            val rawScheduledTime = obj.get("scheduledDateTime")?.takeUnless { it.isJsonNull }?.asString ?: continue
            // Guard: Decode epoch-millis and UTC timestamps using ZoneOffset.UTC matching Converters.kt to prevent local timezone drift on import
            val parsedScheduledTime = rawScheduledTime.toLongOrNull()?.let {
                java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDateTime()
            } ?: try {
                LocalDateTime.parse(rawScheduledTime)
            } catch (_: Exception) {
                try {
                    java.time.OffsetDateTime.parse(rawScheduledTime).toLocalDateTime()
                } catch (_: Exception) {
                    try {
                        java.time.Instant.parse(rawScheduledTime).atZone(java.time.ZoneOffset.UTC).toLocalDateTime()
                    } catch (_: Exception) {
                        LocalDateTime.now()
                    }
                }
            }

            val entryIdVal = obj.get("entryId")?.takeUnless { it.isJsonNull }?.asString ?: UUID.randomUUID().toString()
            val userIdVal = obj.get("userId")?.takeUnless { it.isJsonNull }?.asString ?: ""
            val medicineIdVal = obj.get("medicineId")?.takeUnless { it.isJsonNull }?.asString ?: continue
            // Guard: Defensively parse medication status with .trim().uppercase() to prevent resetting completed or skipped history to PENDING on lowercase values
            val statusVal = obj.get("status")?.takeUnless { it.isJsonNull }?.asString?.let { runCatching { MedicationStatus.valueOf(it.trim().uppercase()) }.getOrNull() } ?: MedicationStatus.PENDING

            val entry = ScheduleEntry(
                entryId = entryIdVal,
                userId = userIdVal,
                medicineId = medicineIdVal,
                scheduledDateTime = parsedScheduledTime,
                status = statusVal,
                // Guard: Handle epoch-millis, standard ISO, offset ISO, and UTC Instant strings to prevent dropping completion timestamps
                takenAt = obj.get("takenAt")?.takeUnless { it.isJsonNull }?.asString?.let { raw ->
                    raw.toLongOrNull()?.let { millis ->
                        java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDateTime()
                    } ?: runCatching { LocalDateTime.parse(raw) }.getOrNull()
                      ?: runCatching { java.time.OffsetDateTime.parse(raw).toLocalDateTime() }.getOrNull()
                      ?: runCatching { java.time.Instant.parse(raw).atZone(java.time.ZoneOffset.UTC).toLocalDateTime() }.getOrNull()
                },
                skipReason = obj.get("skipReason")?.takeUnless { it.isJsonNull }?.asString,
                dosage = obj.get("dosage")?.takeUnless { it.isJsonNull }?.runCatching { asDouble }?.getOrNull(),
                doseNotes = obj.get("doseNotes")?.takeUnless { it.isJsonNull }?.asString
            )
            list.add(entry)
        }
        return list
    }

    // Guard: Generate deterministic entryId identical to ScheduleRecurrenceEngine to prevent duplicate schedule entries on auto-extend or medication update
    internal fun createRestoredEntryId(medicineId: String, originalEntryId: String, scheduledDateTime: LocalDateTime): String {
        return if (originalEntryId.startsWith("PRN_")) {
            "PRN_${UUID.randomUUID()}"
        } else {
            val cleanTime = scheduledDateTime.toLocalTime().withSecond(0).withNano(0)
            "${medicineId}_${scheduledDateTime.toLocalDate()}_$cleanTime".replace(":", "_").replace("-", "_")
        }
    }
}
