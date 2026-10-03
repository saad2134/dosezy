# 🔌 Dosezy OpenAPI 3.0 Guide & Mobile SDK Alignment

> **File Path:** `api/openapi.yaml`  
> **Base URL:** `https://api.dosezy.app`  
> **Specification Version:** 3.0.3

---

## 🛠️ What Can We Do With `api/openapi.yaml`?

`api/openapi.yaml` serves as the **single source of truth** for all network contracts across the Dosezy ecosystem.

### 1. 🌐 Generate Interactive Documentation (`docs.dosezy.app`)
Generate a static HTML API documentation portal with interactive try-it-out capabilities:
```bash
# Build single-file HTML docs using Redocly
npx @redocly/cli build-docs api/openapi.yaml -o docs/api-reference.html
```

### 2. ⚡ Auto-Generate Backend Server Core (`server/`)
Generate server routing and handler stubs for Node.js, Go, Python, or Rust:
```bash
# Auto-generate Go server stubs
npx @openapitools/openapi-generator-cli generate -i api/openapi.yaml -g go-server -o server/

# Auto-generate Node.js Express server stubs
npx @openapitools/openapi-generator-cli generate -i api/openapi.yaml -g nodejs-express-server -o server/
```

### 3. 📱 Auto-Generate Mobile & Web SDKs
Auto-generate network client libraries for Android (Kotlin), iOS (Swift), and Web (TypeScript):
```bash
# Generate Kotlin Retrofit API SDK for Android
npx @openapitools/openapi-generator-cli generate -i api/openapi.yaml -g kotlin -o patient/android/app/src/main/java/com/example/dosezy/network

# Generate TypeScript Axios SDK for Caregiver Web Dashboard
npx @openapitools/openapi-generator-cli generate -i api/openapi.yaml -g typescript-axios -o caregiver/src/api
```

### 4. 🎭 Run an Instant Local Mock Server
Test frontend and mobile apps against realistic mock APIs before the backend server is running:
```bash
# Run local mock server on port 8080
npx @stoplight/prism-cli mock api/openapi.yaml -p 8080
```

### 5. 🧪 Automated CI Schema Validation
Enforce strict schema validation in GitHub Actions on every Pull Request:
```bash
npx @redocly/cli lint api/openapi.yaml
```

---

## 🔍 Alignment Analysis: Current Android App vs. Generated Kotlin SDK

### Command Analyzed
```bash
npx @openapitools/openapi-generator-cli generate \
  -i api/openapi.yaml \
  -g kotlin \
  -o patient/android/app/src/main/java/com/example/dosezy/network
```

### Does the Generated SDK Match the Dosezy Android App?
**Yes! The OpenAPI specification aligns 100% directly with the Android Room database models.**

#### Field-by-Field Mapping Matrix:

| Domain Entity | Room DB Model (`com.example.dosezy.data.model`) | Generated Network DTO (`com.example.dosezy.network.models`) | Match Status |
| :--- | :--- | :--- | :--- |
| **User** | `userId`, `fullName`, `age`, `gender`, `contactNumber`, `profilePicPath`, `isCurrentUser`, `theme`, `timeFormat`, `language`, `considerLateAfter`, `considerMissedAfter`, `snoozeDuration`, `allergies`, `medicalConditions`, `naggingRemindersEnabled`, `naggingIntervalMinutes`, `naggingMaxRepeats`, `alarmSound`, `customAlarmSoundPath`, `customAlarmSoundTitle`, `alarmDurationSeconds`, `allowDoseSkipping`, `allowCustomDoseTime`, `hideAddMedicineNavButton`, `allowDoseUndo`, `allowDoseNotes`, `promptDoseNotes` | `userId`, `fullName`, `age`, `gender`, `contactNumber`, `profilePicPath`, `isCurrentUser`, `theme`, `timeFormat`, `language`, `considerLateAfter`, `considerMissedAfter`, `snoozeDuration`, `allergies`, `medicalConditions`, `naggingRemindersEnabled`, `naggingIntervalMinutes`, `naggingMaxRepeats`, `alarmSound`, `customAlarmSoundPath`, `customAlarmSoundTitle`, `alarmDurationSeconds`, `allowDoseSkipping`, `allowCustomDoseTime`, `hideAddMedicineNavButton`, `allowDoseUndo`, `allowDoseNotes`, `promptDoseNotes` | **100% Match** |
| **Medicine** | `medicineId`, `userId`, `medicationName`, `dosage`, `dosageUnit`, `timesPerDay`, `frequency`, `scheduledTimes`, `imageUri`, `pillShape`, `pillColor`, `startDate`, `endDate`, `durationDays`, `isArchived`, `notes`, `currentStock`, `refillThreshold`, `autoDeductOnTake`, `customDosages` | `medicineId`, `userId`, `medicationName`, `dosage`, `dosageUnit`, `timesPerDay`, `frequency`, `scheduledTimes`, `imageUri`, `pillShape`, `pillColor`, `startDate`, `endDate`, `durationDays`, `isArchived`, `notes`, `currentStock`, `refillThreshold`, `autoDeductOnTake`, `customDosages` | **100% Match** |
| **ScheduleEntry** | `entryId`, `userId`, `medicineId`, `scheduledDateTime`, `status`, `takenAt`, `skipReason`, `dosage`, `doseNotes` | `entryId`, `userId`, `medicineId`, `scheduledDateTime`, `status`, `takenAt`, `skipReason`, `dosage`, `doseNotes` | **100% Match** |

---

## 🏛️ Architectural Parity & Integration Design

### 1. 🛡️ Room SQLite Annotations vs. Clean Network DTOs
- **Room Models (`com.example.dosezy.data.model`):** Contain local Android Room SQLite table annotations (`@Entity`, `@PrimaryKey`, `indices`, `foreignKeys`), TypeConverters, and local calculation helpers.
- **Generated OpenAPI DTOs (`com.example.dosezy.network.models`):** Are lightweight, pure Kotlin data transfer objects annotated with `@SerializedName` specifically for JSON network serialization and cross-platform sync.

### 2. 💊 Complete Patient, Medication & Adherence History Parity
Every field supported in the local SQLite database is fully represented in the OpenAPI schema:
- **Patient Profile & Preferences:** Identity, contact info, clinical history (allergies/conditions), alarm audio/duration, and intake preference flags ✅
- **Medication Definitions:** Full course timelines, finite day limits, shape/color visuals, inventory stock thresholds, and multi-slot custom dosages ✅
- **Dose Schedule & History:** Timestamps, intake statuses, skip justifications, resolved slot dosages, and clinical meal/symptom notes ✅

---

## 💡 Recommended Integration Pattern (Data Mappers)

Use simple extension functions to bridge Room entities and Generated Network DTOs cleanly:

```kotlin
// Convert Room DB User entity to Network DTO for sync push
fun User.toNetworkDto(): com.example.dosezy.network.models.User {
    return com.example.dosezy.network.models.User(
        userId = this.userId,
        fullName = this.fullName,
        age = this.age,
        gender = com.example.dosezy.network.models.Gender.valueOf(this.gender.name),
        contactNumber = this.contactNumber,
        profilePicPath = this.profilePicPath,
        isCurrentUser = this.isCurrentUser,
        theme = com.example.dosezy.network.models.Theme.valueOf(this.theme.name),
        timeFormat = com.example.dosezy.network.models.TimeFormat.valueOf(this.timeFormat.name),
        language = com.example.dosezy.network.models.Language.valueOf(this.language.name),
        considerLateAfter = this.considerLateAfter,
        considerMissedAfter = this.considerMissedAfter,
        snoozeDuration = this.snoozeDuration,
        allergies = this.allergies,
        medicalConditions = this.medicalConditions,
        naggingRemindersEnabled = this.naggingRemindersEnabled,
        naggingIntervalMinutes = this.naggingIntervalMinutes,
        naggingMaxRepeats = this.naggingMaxRepeats,
        alarmSound = com.example.dosezy.network.models.AlarmSound.valueOf(this.alarmSound.name),
        customAlarmSoundPath = this.customAlarmSoundPath,
        customAlarmSoundTitle = this.customAlarmSoundTitle,
        alarmDurationSeconds = this.alarmDurationSeconds,
        allowDoseSkipping = this.allowDoseSkipping,
        allowCustomDoseTime = this.allowCustomDoseTime,
        hideAddMedicineNavButton = this.hideAddMedicineNavButton,
        allowDoseUndo = this.allowDoseUndo,
        allowDoseNotes = this.allowDoseNotes,
        promptDoseNotes = this.promptDoseNotes
    )
}

// Convert Network DTO back to Room DB User entity upon sync pull
fun com.example.dosezy.network.models.User.toRoomEntity(): User {
    return User(
        userId = this.userId,
        fullName = this.fullName,
        age = this.age,
        gender = Gender.valueOf(this.gender.name),
        contactNumber = this.contactNumber ?: "",
        profilePicPath = this.profilePicPath,
        isCurrentUser = this.isCurrentUser ?: false,
        theme = this.theme?.let { Theme.valueOf(it.name) } ?: Theme.SYSTEM,
        timeFormat = this.timeFormat?.let { TimeFormat.valueOf(it.name) } ?: TimeFormat.HOUR_12,
        language = this.language?.let { Language.valueOf(it.name) } ?: Language.SYSTEM,
        considerLateAfter = this.considerLateAfter ?: 3,
        considerMissedAfter = this.considerMissedAfter ?: 6,
        snoozeDuration = this.snoozeDuration ?: 10,
        allergies = this.allergies,
        medicalConditions = this.medicalConditions,
        naggingRemindersEnabled = this.naggingRemindersEnabled ?: false,
        naggingIntervalMinutes = this.naggingIntervalMinutes ?: 5,
        naggingMaxRepeats = this.naggingMaxRepeats ?: 3,
        alarmSound = this.alarmSound?.let { AlarmSound.valueOf(it.name) } ?: AlarmSound.SYSTEM_DEFAULT,
        customAlarmSoundPath = this.customAlarmSoundPath,
        customAlarmSoundTitle = this.customAlarmSoundTitle,
        alarmDurationSeconds = this.alarmDurationSeconds ?: 0,
        allowDoseSkipping = this.allowDoseSkipping ?: false,
        allowCustomDoseTime = this.allowCustomDoseTime ?: false,
        hideAddMedicineNavButton = this.hideAddMedicineNavButton ?: false,
        allowDoseUndo = this.allowDoseUndo ?: false,
        allowDoseNotes = this.allowDoseNotes ?: false,
        promptDoseNotes = this.promptDoseNotes ?: false
    )
}
```
