# Dosezy User Manual

Welcome to **Dosezy**! Dosezy is your personal, accessible, and 100% private companion for managing daily medications, setting critical reminders, tracking pill inventory, monitoring adherence, and keeping your whole family healthy.

This guide provides a complete, step-by-step walkthrough of all features in Dosezy in clear, everyday language.

---

## 📋 Table of Contents

1. [App Overview, 12 Languages & Dark/Light Themes](#1-app-overview-12-languages--darklight-themes)
2. [Home Dashboard, Live Status Badges & Intake Undo](#2-home-dashboard-live-status-badges--intake-undo)
3. [Clinical Dose Skipping & Mandatory Reason Logging](#3-clinical-dose-skipping--mandatory-reason-logging)
4. [Daily Completion Banner & Full Schedule Review](#4-daily-completion-banner--full-schedule-review)
5. [Adding, Editing, Searching & Archiving Medications](#5-adding-editing-searching--archiving-medications)
   - [Live Medication Search & Instant Feedback](#live-medication-search--instant-feedback)
   - [Medicine Photos & Food Instructions](#medicine-photos--food-instructions)
   - [Dosage Units & Frequency Patterns](#dosage-units--frequency-patterns)
   - [Multi-Dose Quick Presets (1x, 2x, 3x, 4x)](#multi-dose-quick-presets-1x-2x-3x-4x)
   - [12-First Grid Time Picker](#12-first-grid-time-picker)
   - [Discontinuing & Archiving Medications](#discontinuing--archiving-medications)
   - [Form Validation Feedback & Confirmation](#form-validation-feedback--confirmation)
6. [Critical Full-Screen Alarms, Volume Rocker Silence & Custom Tones](#6-critical-full-screen-alarms-volume-rocker-silence--custom-tones)
   - [Grouped Alarms & "Take All"](#grouped-alarms--take-all)
   - [Hardware Volume Rocker Silence](#hardware-volume-rocker-silence)
   - [Back-Gesture Snooze](#back-gesture-snooze)
   - [Customizable Alarm Duration & Audio Volume](#customizable-alarm-duration--audio-volume)
   - [Custom Ringtone Selection](#custom-ringtone-selection)
   - [Dynamic Snooze Presets](#dynamic-snooze-presets)
7. [Notification Reliability Shield & Diagnostics](#7-notification-reliability-shield--diagnostics)
8. [Pill Inventory, Automatic Refill Warnings & Pharmacy Refill Orders](#8-pill-inventory-automatic-refill-warnings--pharmacy-refill-orders)
9. [Interactive Schedule Calendar & TalkBack Accessibility](#9-interactive-schedule-calendar--talkback-accessibility)
10. [Adherence Analytics & Multi-Ring Scores](#10-adherence-analytics--multi-ring-scores)
11. [Structured Preferences & Dose Tracking Settings](#11-structured-preferences--dose-tracking-settings)
12. [International Emergency Dialing & Profile Contacts](#12-international-emergency-dialing--profile-contacts)
13. [Managing Multiple Family Profiles & Deletion Safeguards](#13-managing-multiple-family-profiles--deletion-safeguards)
14. [Full ZIP Backup, Selective Restore & Conflict Resolution](#14-full-zip-backup-selective-restore--conflict-resolution)
15. [Doctor PDF Reports & CSV Adherence Logs](#15-doctor-pdf-reports--csv-adherence-logs)
16. [Help, Support & Checking for Updates](#16-help-support--checking-for-updates)

---

## 1. App Overview, 12 Languages & Dark/Light Themes

Dosezy is built with accessibility and visual clarity as foundational principles: high-contrast color palettes, large typography, direct navigation, and zero confusing multi-tier submenus.

### 12 Supported World Languages:
Dosezy is fully translated across all screens, notification actions, alarms, and settings in **12 native languages**:
- 🇬🇧 English
- 🇪🇸 Spanish (Español)
- 🇮🇳 Hindi (हिन्दी)
- 🇨🇳 Chinese (中文)
- 🇵🇹 Portuguese (Português)
- 🇸🇦 Arabic (العربية)
- 🇫🇷 French (Français)
- 🇩🇪 German (Deutsch)
- 🇯🇵 Japanese (日本語)
- 🇷🇺 Russian (Русский)
- 🇮🇹 Italian (Italiano)
- 🇧🇩 Bengali (বাংলা)

### Changing Language, Theme, or Time Format:
1. Tap the **Menu** tab at the bottom right.
2. Tap **Preferences**.
3. Under the **General** section:
   - **Language**: Select your preferred language.
   - **Theme**: Choose between **System Default**, **Light Mode**, or sleek **Dark Mode**.
   - **Time Format**: Select **12-Hour (AM/PM)** or **24-Hour** display.

---

## 2. Home Dashboard, Live Status Badges & Intake Undo

The **Home** screen displays your daily schedule grouped into chronological time headers (e.g., `08:00 AM`, `01:00 PM`, `08:00 PM`).

### Time Section Headers:
Below each time header, Dosezy displays real-time countdown badges:
- `To be taken in 45m`: An upcoming dose.
- `1h 15m ago`: A dose whose time has passed.
- `All medications taken`: All pills in this time slot are logged.

### Medication Card Badges & Actions:
Each medicine card displays its pill visual, name, dosage, meal instructions, stock warning, and direct action buttons:
- 🟢 **Taken**: Green button displaying the recorded completion timestamp (e.g., `Taken at 08:05 AM`).
- ⚪ **Take**: Primary action button for due or upcoming medications.
- 🟧 **Mark Late**: Appears when a dose is past due but within your configured late window.
- 🔴 **Missed**: Indicates a dose that passed your missed window.
- 📝 **Skipped**: Shows when a dose was paused for clinical reasons with its recorded explanation (e.g., `Skipped: Doctor advised pause`).

### Configurable Dose Undo with Safety Confirmation:
To prevent accidental reversals, Dosezy includes an optional Dose Undo safety setting:
1. Enable **Allow Dose Undo** under **Menu > Preferences > Dose Tracking** (disabled by default to protect your stock counts and compliance logs).
2. When enabled, if you mark a dose as Taken or Late:
   - A 5-second snackbar appears at the bottom with an **"Undo"** button.
   - The medication card also displays an interactive **"Undo"** button.
3. Tapping Undo displays a confirmation dialog asking you to confirm the reversal. Once confirmed, Dosezy resets the dose status back to **Pending** and safely returns any deducted tablet counts to your inventory.
4. When disabled, recorded doses remain safely locked to prevent inadvertent taps.

### Per-Dose Notes & Meal Logging:
Track how your medications affect you or record meal conditions at intake:
1. Enable **Add Note on Dose Taken** in **Menu > Preferences > Dose Tracking**.
2. When marking a dose as Taken or Late, the **Dose Notes** dialog opens automatically.
3. Tap quick suggestion tags (such as *"With food"*, *"Empty stomach"*, *"Before meal"*, *"After meal"*, *"Mild nausea"*, *"Headache"*, *"Dizziness"*) or type custom symptoms and notes.
4. Tap **Save Note** to log the note with your dose, or tap **Skip** if you do not need to add notes.
5. Recorded notes appear directly on the dose card and in your schedule calendar.
6. **View & Edit Anytime**: Tap the note text or icon on any taken card or calendar item to update your notes whenever needed.

### Manually Record Dose Time:
If you took a medication earlier but forgot to mark it on time, you can record the exact past time you took the dose:
1. When logging a dose, select or enter the exact time the medication was consumed.
2. If note logging is enabled, you can also attach intake notes directly within the time picker dialog.
3. Dosezy logs your specified intake time rather than just the current moment, ensuring your adherence timeline and medical compliance history remain strictly accurate.

### Crossing Time Zones (Doses Shift With You):
When traveling across time zones:
- Dosezy automatically keeps your scheduled medication routine anchored to your daily local routine without time distortion.
- Reminders shift seamlessly with your active timezone so you never have to manually recalibrate schedules while traveling.

---

## 3. Clinical Dose Skipping & Mandatory Reason Logging

Patients occasionally need to pause medications due to adverse reactions, doctor instructions, fasting, or illness. Dosezy provides a medically sound dose skipping workflow that protects both clinical safety and adherence records.

### Enabling Dose Skipping:
1. Tap **Menu > Preferences**.
2. Scroll to the **Dose Tracking** section.
3. Switch on **Allow Dose Skipping**.

### Skipping a Dose:
1. On your Home screen dose cards or on full-screen alarm alerts, an outlined **"Skip"** button will appear.
2. Tapping Skip opens the **Clinical Skip Reason** dialog.
3. Select from 6 standardized clinical reasons:
   - 🩺 **Doctor advised pause**: Temporary suspension recommended by your physician.
   - ⚠️ **Side effects / adverse reaction**: Experiencing intolerance, allergy, or adverse symptoms.
   - 🍽️ **Fasting / medical procedure**: Required to fast before lab tests or clinical procedures.
   - 🤢 **Nausea / vomiting / illness**: Acute sickness preventing oral intake.
   - 📦 **Ran out of medicine**: Awaiting pharmacy refills.
   - 💬 **Other reason**: Freeform clinical notes.
4. Tap **Confirm Skip**. The dose card is marked with a clear `📝 Skipped: <Reason>` badge.
5. **Adherence Protection**: Clinically skipped doses are intentionally excluded from missed penalties in your Analytics compliance score.
6. **Reversible Undo**: If taken later or logged in error, tap **Undo** on the skipped card to reset status back to Pending.

---

## 4. Daily Completion Banner & Full Schedule Review

When all medications scheduled for today are logged:
- A cheerful **All Good** completion banner is displayed at the top of the Home screen.
- Crucially, **today's full medication list remains visible underneath** in a clean, checked review state.
- Patients and caregivers can inspect exact dosages, meal notes, and timestamps throughout the day, or tap Undo if any entry was logged by mistake.

---

## 5. Adding, Editing, Searching & Archiving Medications

### Adding a Medication:
1. Tap **Medicines** in the bottom navigation bar.
2. Tap **"+ Add Medicine"** (or the **"Add Your First Medication"** button on an empty list).
3. Enter the **Medicine Name** (e.g., *Metformin*, *Atorvastatin*, *Lisinopril*).

### Live Medication Search & Instant Feedback:
At the top of the **Medicines** screen, an instant search bar filters large prescription lists in real time by medicine name or instructions. If no medications match your query, Dosezy presents an encouraging empty state with a 1-tap **"Clear search"** action button.

### Medicine Photos & Food Instructions:
- **Medicine Photo**: Tap the camera icon to photograph your pill box or choose an image from your gallery with built-in cropping.
- **Meal Instructions**: Choose between:
  - 🍽️ *Before Food*
  - 🍲 *With Food*
  - 🥗 *After Food*
  - ⏱️ *No Food Instructions*

### Dosage Units & Frequency Patterns:
- **Dosage**: Enter the quantity (e.g., `1`, `500`) and select the unit:
  - `tablet`, `capsule`, `mg`, `mcg`, `g`, `mL`, `drops`, `puffs`, or `units`.
- **Frequency Pattern**:
  - **Daily**: Every day.
  - **Weekly**: Choose specific days of the week (e.g., Mon, Wed, Fri).
  - **Monthly**: Choose specific dates of the month (e.g., 1st, 15th).
  - **As Needed (PRN)**: For pain relievers or temporary medications taken only when needed.

### Multi-Dose Quick Presets (1x, 2x, 3x, 4x):
Populate standard time intervals with a single tap:
- **1x Daily**: `08:00 AM`
- **2x Daily**: `08:00 AM`, `08:00 PM`
- **3x Daily**: `08:00 AM`, `02:00 PM`, `08:00 PM`
- **4x Daily**: `08:00 AM`, `12:00 PM`, `04:00 PM`, `08:00 PM`

### 12-First Grid Time Picker:
Tap any dose time chip to open the intuitive time picker:
- **12 is at the top**: Hour numbers start with 12 followed by 1 to 11 for natural morning/night selection.
- **AM / PM Toggle**: 1-tap switching.
- **Quick Minute Chips**: Tap `:00`, `:15`, `:30`, or `:45`, or slide the minute bar for exact minute precision.

### Discontinuing & Archiving Medications:
When a treatment course finishes:
1. Open the medication from the Medicines screen and tap **Edit**.
2. Tap **Discontinue / Archive**.
3. The medicine moves to your **Discontinued Medications** section at the bottom of the Medicines screen. It is neatly organized within an expandable and collapsible accordion so active medications remain clutter-free. Its past calendar logs, historical compliance rates, and doctor reports are preserved permanently. You can expand the section and reactivate any medication at any time.

### Flexible Add Medicine Placement:
When the navigation '+' button is hidden via Preferences, an **"Add Medicine"** button appears cleanly directly below your active medications roster (and above the Discontinued Medications section). On an empty roster, the friendly initial "Add Your First Medication" button guides you.

### Form Validation Feedback & Confirmation:
When adding or editing a medication, Dosezy highlights missing fields in red with clear guidance. Saving a medication confirms success with a brief on-screen confirmation toast.

---

## 6. Critical Full-Screen Alarms, Volume Rocker Silence & Custom Tones

### Grouped Alarms & "Take All":
When multiple medications are scheduled for the exact same minute:
- Dosezy rings with a **single grouped full-screen alarm** instead of multiple overlapping sound alerts.
- The screen displays an interactive checklist with pill names, dosages, meal instructions, and photos.
- **TAKE ALL (Green Button)**: Confirms all scheduled medicines in that slot with 1 tap and deducts stock automatically.
- **SNOOZE (Orange Button)**: Pauses the alarm for your chosen snooze duration.
- Individual pills can also be checked off one by one if taking only a subset.

### Hardware Volume Rocker Silence:
If an alarm rings during a meeting or while resting, press the **Volume Up** or **Volume Down** button on your phone. Dosezy instantly silences the ringtone and stops vibration without dismissing the alarm or requiring you to unlock your screen.

### Back-Gesture Snooze:
Pressing your device's physical or gesture **Back** button during an active alarm automatically triggers a safe snooze for your configured duration, ensuring doses are never accidentally dismissed into a missed state.

### Customizable Alarm Duration & Audio Volume:
Go to **Menu > Preferences > Notifications & Alarms**:
- **Alarm Sound Duration**: Choose how long alarms ring before auto-stopping:
  - `30 seconds`, `1 minute`, `2 minutes`, `3 minutes`, `5 minutes`, or **Continuous loop**.
- **Alarm Sound Volume**: Dedicated slider to set the ideal volume level.

### Custom Ringtone Selection:
Choose the exact sound you want to wake you up:
- Select from Dosezy's built-in alert chimes (*Gentle Chime*, *Crystal Pulse*, *Alert Beep*, *Harmonic Rise*), system alarms, notification tones, or your own custom device ringtones.
- Includes live audio preview playback directly inside the selector.

### Dynamic Snooze Presets:
Configure your preferred snooze duration under **Menu > Preferences > Notifications & Alarms > Default Snooze Duration**:
- Choose `5 minutes`, `10 minutes`, `15 minutes`, `20 minutes`, or `30 minutes`. Your preferred duration is dynamically reflected on alarm buttons and notification actions.

---

## 7. Notification Reliability Shield & Diagnostics

To guarantee alarms ring reliably even during Android deep sleep (Doze mode):
- A persistent **Notification Reliability Shield** button is visible on the top bar across all main screens.
- When green, all system permissions are verified.
- Tapping the shield opens a live diagnostic checklist:
  - **Exact Alarms**: Verifies precision alarm dispatch permission.
  - **Notifications**: Verifies notification channel status.
  - **Battery Optimization**: Checks if background power restrictions are disabled so Android does not kill Dosezy in the background.
- Direct links provide 1-tap access to your device's settings, including manufacturer-specific guides for Samsung, Xiaomi, Huawei, OnePlus, and Oppo devices.

---

## 8. Pill Inventory, Automatic Refill Warnings & Pharmacy Refill Orders

Dosezy includes automatic inventory management and prescription refill order generation so you never run out of vital medications:

### Tracking Inventory & Refill Warnings:
1. In the Add/Edit Medicine screen, turn on **Track Stock Inventory**.
2. Enter your **Current Stock** count (e.g., `60` tablets).
3. Enter your **Refill Warning Threshold** (e.g., `10` tablets).
4. **Auto-Deduction**: Whenever you tap **Taken** (from alarms or the Home screen), Dosezy automatically subtracts your dosage amount from your stock.
5. **Low Stock Alerts**: When remaining stock reaches or drops below your threshold, an orange **`⚠️ Refill Warning: X left`** badge is displayed on your Home screen and Medicines list.

### Dosezy Pharmacy Refill Order:
When you need to reorder medicines from your local pharmacy or doctor:
1. Tap the **Menu** tab at the bottom right.
2. Under the **Data Management** section, tap **Pharmacy Refill Order**.
3. **Multi-Profile Family Consolidation**: Combine prescriptions for multiple family members into a single unified refill order, or generate an order for a single profile.
4. Choose your filter:
   - **Low Stock Only**: Quickly displays only medications currently at or below their refill warning threshold.
   - **All Active**: Displays all currently active prescriptions in your profile.
5. Select your **Supply Duration**:
   - Choose `15 Days`, `30 Days`, `60 Days`, or `90 Days`.
   - **Smart Bottle Supply**: For liquid medications and eye drops, Dosezy automatically computes orders in whole bottles rather than raw drops or milliliters, accounting for standard bottle volumes.
6. Review individual medications using checkboxes, or tap **Select All / Deselect All**.
7. **Live Preview**: Check the formatted prescription order in the preview box with clean WhatsApp markdown formatting (patient name, date, required supplies, current stock, and dosage instructions).
8. Tap **Share via Chat** to instantly send the order via WhatsApp, Telegram, SMS, or any installed messaging app, or tap **Copy to Clipboard** to paste it anywhere.

---

## 9. Interactive Schedule Calendar & TalkBack Accessibility

Tap **Schedule** in the bottom navigation bar to view your full medication calendar:
- **Week & Month Views**: Tap any day (Mon–Sun) or date on the calendar to view past or future schedules.
- **Color-Coded Adherence Dots**:
  - 🟢 **Green**: All scheduled doses taken on time.
  - 🟧 **Orange**: Taken late.
  - 🔴 **Red**: Missed doses.
  - 🔵 **Blue**: Clinically skipped doses.
- **Fast Date Switching**: Tapping any day instantly loads that date's chronological schedule.
- **Dose Notes in Schedule View**: Taken medications display their recorded meal and symptom notes directly under each entry. Tap any entry or note to review or update notes for that day.
- **TalkBack Screen Reader Support**: For visually impaired users, every status badge and icon announces its exact status (e.g., *"Taken"*, *"Late"*, *"Skipped"*, *"Missed"*, *"Pending"*), rather than generic labels.

---

## 10. Adherence Analytics & Multi-Ring Scores

Tap **Analytics** from the Menu to view comprehensive compliance charts:
- **Time Range Selector**: View metrics across **Today**, **7 Days**, **30 Days**, **90 Days**, and **All Time**.
- **Multi-Ring Breakdown**: Compares adherence rates across multiple timeframes side-by-side.
- **Total Progress Ring**: Displays your cumulative adherence percentage (e.g., `94% Adherence`).
- **Detailed Dose Metrics**: Total counts of **On-Time**, **Late**, **Skipped**, and **Missed** doses.
- **Per-Medicine Compliance**: See individual compliance rates for each medicine to identify specific challenges.

---

## 11. Structured Preferences & Dose Tracking Settings

The **Preferences** screen is organized into dedicated categories:
- **General**:
  - **Language**: Selection across 12 languages.
  - **Theme**: System Default, Light Mode, or Dark Mode.
  - **Time Format**: 12-Hour (AM/PM) or 24-Hour.
  - **Hide Navigation Add Button**: Toggle to hide the center '+' button from the bottom navigation bar. When enabled, the remaining 4 tabs (`Home`, `Schedule`, `Medicines`, `Menu`) expand symmetrically across the bar (25% each), and an "Add Medicine" button is cleanly placed on the Medicines screen below your active prescriptions.
- **Notifications & Alarms**: Default Snooze Duration (5–30 min), Custom Ringtone, Sound Volume, Alarm Duration (30s to 5m/loop).
- **Dose Tracking**:
  - **Allow Dose Undo**: Toggle whether taken or late doses can be undone from dose cards or reminder snackbars (default is disabled for safety). Includes a confirmation dialog before reversing status and restoring pill stock.
  - **Add Note on Dose Taken**: Prompt for notes (meal context, side effects, symptoms) whenever a dose is marked as taken (default is disabled).
  - **Allow Dose Skipping**: Toggle the clinical dose skipping option.
  - **Consider Late After**: Choose `1 hour`, `2 hours`, or `3 hours` (default is **3 hours**).
  - **Consider Missed After**: Choose between `3 hours` and `9 hours` (default is **6 hours**).

---

## 12. International Emergency Dialing & Profile Contacts

In urgent situations, Dosezy provides rapid, 1-tap emergency calling:

1. Tap **Menu > Emergency Contacts**.
2. **Official Emergency Services**:
   - Dosezy automatically detects your country or lets you select from 10 regions:
     - 🇮🇳 **India** (108 / 101 / 100 / 112)
     - 🇺🇸🇨🇦 **USA & Canada** (911)
     - 🇬🇧 **United Kingdom** (999 / 112)
     - 🇪🇺 **European Union** (112)
     - 🇨🇳 **China** (120 / 119 / 110)
     - 🇯🇵 **Japan** (119 / 110)
     - 🇷🇺 **Russia** (103 / 101 / 102 / 112)
     - 🇧🇷 **Brazil** (192 / 193 / 190)
     - 🇧🇩 **Bangladesh** (999)
     - 🇦🇺 **Australia** (000)
3. **Personal Emergency Contacts**:
   - Tap **"+ Add Emergency Contact"** to save personal numbers (family doctor, primary caregiver, son, daughter, neighbor).
   - Emergency contacts are scoped per user profile, ensuring personal doctor numbers remain isolated between household members.

---

## 13. Managing Multiple Family Profiles & Deletion Safeguards

Manage medications for your whole household on a single phone:

1. Tap **Menu > Switch Profile**.
2. Tap **Add Profile** to create a profile for an elderly parent, child, or partner.
3. Enter their Name, Age, Gender, and optional Profile Picture.
4. Tap any profile card to switch active users. Tapping the currently selected profile returns smoothly to your dashboard. Each profile maintains its own separate medicine list, schedules, stock counts, personal emergency contacts, and adherence history.
5. **Deletion Safeguards & Countdown Protection**:
   - Single-profile accounts cannot delete their only profile (at least one profile must always remain active).
   - Deleting a secondary profile requires opening profile settings and waiting through a deliberate **5-second safety countdown** before the confirm button enables with an explicit **"Delete Profile"** action, eliminating accidental loss of medical histories.

---

## 14. Full ZIP Backup, Selective Restore & Conflict Resolution

Protect your records or transfer them seamlessly to a new phone:

### Exporting Backups:
1. Go to **Menu > Backup & Restore**.
2. Tap **Export Backup** to generate a complete encrypted ZIP archive containing all profiles, active & archived medicines, compliance logs, and emergency contacts.
3. Use the direct system share sheet or save the file to Google Drive, internal storage, or email.

### Selective Profile Restore & Conflict Resolution:
1. On your new phone, install Dosezy, open **Menu > Backup & Restore**, and tap **Restore Backup**.
2. Select your backup ZIP file. Dosezy inspects the archive with an in-front loading spinner.
3. The **Select Profiles to Import** dialog displays each profile found in the archive.
4. For each conflicting profile, choose your preferred strategy:
   - **Create New Profile**: Imports the profile as a separate new user with an updated name.
   - **Merge Records**: Intelligently merges incoming medicines and history into the existing profile.
   - **Replace Profile**: Overwrites existing data with the backup's state.
5. Tap **Import Selected** to complete the restore.

---

## 15. Doctor PDF Reports & CSV Adherence Logs

Export health records for doctor visits or personal analysis:

1. **Doctor PDF Report**: Go to **Menu > Export Data** and select **PDF Report** to generate a clean, printable medical report with current prescriptions, dosages, meal instructions, skipped dose summaries, and adherence rates.
2. **CSV Adherence Data**: Export a spreadsheet-compatible CSV file containing full timestamps, statuses, and clinical skip reasons.
3. **JSON Raw Archive**: Export standard JSON data for clinical integration or offline archiving.

---

## 16. Help, Support, About Dosezy & Updates

Under the **More** section in **Menu**:
- **Help & Support**: Step-by-step guides, FAQs, and developer contact.
- **About Dosezy**:
  - **Core Pillars**: Highlights Dosezy's core architectural commitments:
    - 🛡️ *100% Offline & Private* (zero accounts, zero cloud dependencies, on-device encryption).
    - ⏰ *Reliable Alarms & Timezones* (exact alarm dispatch, deep sleep bypass, timezone preservation).
    - 👨‍👩‍👧 *Multi-Profile Family Care* (independent medicines, logs, and emergency contacts for every family member).
    - ♿ *Accessibility First* (48dp touch targets, high contrast, TalkBack screen reader support).
  - **Meet the Developer**: Creator and lead maintainer attribution (`Saad` / `@saad2134`) with direct GitHub repository links.
  - **Open-Source License**: Licensed under the MIT License with tamper-resistant bytecode author attribution.
- **Check for Updates**: 1-tap check across Google Play, F-Droid, or GitHub Releases.


---

### Need Help?
Go to **Menu > More > Help & Support** in the app or contact developer support at **reach.saad@outlook.com**.
