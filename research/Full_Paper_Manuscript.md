# Dosezy: Design, Architecture, and Empirical Evaluation of an Accessibility-Oriented Offline Medication Reminder System for Older Adults

> **Authors:** Saaduddin Mohammad, Md Rahif Uddin Khan, Khwaja Mohammed  
> **Affiliation:** Department of Computer Science and Engineering, Methodist College of Engineering and Technology, King Koti Road, Abids, Hyderabad, Telangana 500001, India  
> **Emails:** `160723733051@methodist.edu.in`, `160723733005@methodist.edu.in`, `160723733063@methodist.edu.in`  

---

## Abstract

Managing daily prescription regimens presents significant cognitive and physical challenges for older adults, particularly those experiencing polypharmacy alongside age-related motor tremors, arthritis, and declining visual contrast sensitivity. While mobile application stores host numerous medication reminder tools, existing commercial and open-source applications frequently introduce severe accessibility and privacy barriers: they depend on circular radial clock pickers that demand fine motor tracking, require mandatory cloud accounts that aggregate sensitive medical routines, and suffer from dropped notifications caused by aggressive OEM battery optimization policies. In this paper, we present the design, architecture, and empirical evaluation of Dosezy, an open-source, local-first Android medication reminder platform engineered specifically for geriatric accessibility. Dosezy replaces radial clock dials with a stationary 12-hour button grid, guarantees patient privacy through operating-system-level omission of network permissions, generates clinical A4 PDF summaries entirely on-device, and implements a resilient multi-stage alarm scheduling pipeline compatible with Android 8.0 through 14. We report results from a within-subjects comparative usability study ($N=16$, ages 61–76) evaluating the grid interface against standard Android radial time pickers, alongside multi-vendor hardware benchmarks across five smartphone manufacturers.

**Keywords:** Mobile Health, Medication Adherence, Accessibility, Human-Computer Interaction, Local-First Software, Android Development.

---

## 1. Introduction

Adherence to prescribed pharmacotherapy is critical for the effective management of chronic conditions such as hypertension, cardiovascular disease, and type-2 diabetes [1, 3, 16]. Despite the proven efficacy of daily medical regimens, extensive clinical literature indicates that approximately 50% of patients with chronic illnesses fail to take their medications as prescribed [1, 11]. In older adult populations, non-adherence is predominantly unintentional rather than deliberate. It is driven by the complex logistics of polypharmacy, where patients must navigate four or more concurrent prescriptions with varying administration times, dietary constraints, and dosage quantities, amidst normal age-related declines in working memory, tactile sensitivity, and fine motor control [2, 19, 23].

Smartphones represent an omnipresent platform for delivering automated dosage notifications and recording confirmation timestamps [4]. However, our empirical analysis of popular medication reminder applications on Android revealed several critical human-computer interaction (HCI) and architectural failure modes that disproportionately disenfranchise elderly users:
- **Frictional Time Input Modalities:** The standard Android time picker dialog relies on a radial clock paradigm, requiring users to drag a small touch point along a circular trajectory. For older adults experiencing tremors or arthritis, maintaining continuous screen contact while tracing a curved path frequently causes finger slips and incorrect selections [21, 22]. Furthermore, converting 24-hour notations (e.g., mapping 19:30 to 7:30 PM) imposes extraneous cognitive load on seniors accustomed to 12-hour AM/PM conventions [20].
- **Mandatory Cloud Telemetry and Authentication Friction:** The majority of commercial reminder applications require user account creation via email or third-party OAuth providers. Patient dosage routines and prescription names are transmitted to remote cloud databases [10], creating surveillance concerns and authentication friction for older users who struggle with complex password management.
- **Background Alarm Throttling Under Modern Android:** Recent Android releases (Android 12 through 14) enforce aggressive background power-saving restrictions (Doze mode and vendor-specific task killers) to preserve battery life. Standard notification alerts are routinely delayed or killed unless applications explicitly implement exact alarm scheduling APIs, foreground wake locks, and full-screen window manager flags [9].
- **Absence of Localized Emergency Assistance:** Existing tools rarely integrate emergency dispatch mechanisms, or they hardcode regional numbers (e.g., 911) that fail when used in international jurisdictions.

We addressed these intersecting human factors and systems engineering challenges by developing Dosezy, an open-source, local-first Android medication reminder platform. The platform centers on four primary contributions: (1) an accessible 12-Hour Grid Time Picker designed with large, stationary touch targets; (2) a kernel-enforced local-first security architecture with zero network permissions; (3) a deterministic background alarm pipeline that overcomes Android 12–14 Doze constraints; and (4) an empirical within-subjects evaluation ($N=16$) demonstrating statistically significant reductions in time-input latency and touch errors compared to standard radial clock dialogs.

---

## 2. Related Work and HCI Foundations

The design of mobile health technologies for older adults must account for age-related sensory, motor, and cognitive changes [19, 24, 25].

### 2.1 Motor Control and Touch Accessibility in Geriatric HCI
Extensive research by Trewin et al. [21] and Siek et al. [22] demonstrates that older adults exhibit significantly higher error rates and movement variability during touchscreen interactions compared to younger cohorts. Touch target acquisition is compromised by age-related tremor, decreased manual dexterity, and visual occlusion caused by larger finger contact areas (the "fat finger" effect). While younger users effortlessly perform dynamic steering and continuous dragging gestures, older adults perform substantially better with discrete, stationary tap targets with clear visual boundaries [20, 23]. Radial clock interfaces, which require continuous circular dragging, violate these basic human factors principles.

### 2.2 Medication Adherence Support: Physical vs. Software Interventions
Technological aids for medication adherence are broadly divided into smart pillboxes and mobile applications [4]. Physical smart pillboxes feature segmented compartments equipped with audible buzzers, flashing LEDs, and internal microswitches. Although these devices provide direct physical feedback, their high cost ($50 to $150), bulkiness, and manual refilling requirements restrict their portability and real-world adoption.

Conversely, mobile reminder applications (such as Medisafe and MyTherapy) operate on hardware that patients already carry. However, commercial mHealth apps increasingly suffer from feature bloat, incorporating promotional drug coupons, social feeds, and multi-step configuration wizards that overwhelm older users [4, 23]. Table 1 contrasts our local-first, accessibility-centered approach against existing paradigms.

| Dimension | Commercial mHealth Apps | Smart Electronic Pillboxes | Dosezy (Proposed) |
| :--- | :--- | :--- | :--- |
| **Network Dependency** | Mandatory cloud sync | WiFi / Bluetooth bridge | Fully offline (No network permissions) |
| **Time Selection UI** | 24h radial dials / scroll wheels | Hardware dials / buttons | Stationary 12-hour button grid |
| **Alarm Reliability** | Standard background alerts | Hardware beeper | Exact alarms + Lock screen overlay |
| **Clinical Summary** | Cloud web export | Proprietary desktop utility | On-device vector A4 PDF export |
| **Cost & License** | Subscription / Ads / Proprietary | $50–$150 physical device | Free, open-source Android platform |

---

## 3. System Architecture and Implementation

We developed Dosezy in Kotlin using Jetpack Compose, targeting Android 8.0 (API level 26) through Android 14 (API level 34) [9]. The codebase follows Clean Architecture principles, establishing strict separation between presentation composables, domain business logic, and local database persistence.

```
+-----------------------------------------------------------------------+
|                       Presentation Layer                              |
|   (Jetpack Compose UI, 12-Hour Grid Picker, Accessibility Themes)      |
+-----------------------------------------------------------------------+
                                   |
+-----------------------------------------------------------------------+
|                          Domain Layer                                 |
|     (Adherence State Engine, Exact Alarms, ISO Emergency Matcher)     |
+-----------------------------------------------------------------------+
                                   |
+-----------------------------------------------------------------------+
|                           Data Layer                                  |
|  (Room SQLite Local Database, DataStore Preferences, Native PDF Gen)  |
+-----------------------------------------------------------------------+
```

### 3.1 Local Database and Storage Layer
All patient data resides entirely on the host device. We used the Android Room persistence library over an embedded SQLite database, structuring the data model across three normalized entities:
- **User Entity:** Stores patient profile preferences, designated emergency contact phone numbers, and custom thresholds for classifying late and missed doses.
- **Medicine Entity:** Stores the medication name, strength and unit (e.g., 500 mg, 10 ml), dosage frequency (daily, specific weekdays, or custom intervals), and scheduled reminder timestamps.
- **Schedule Entry Entity:** Records individual dose occurrences, storing foreign keys to the parent medication, scheduled intake times, actual confirmation timestamps, and adherence status flags.

Application settings (such as high-contrast display options, notification sound selections, and vibration patterns) are stored in Jetpack DataStore Preferences rather than legacy SharedPreferences, providing thread-safe, non-blocking asynchronous disk reads via Kotlin Coroutines.

### 3.2 Kernel-Level Privacy Guarantee via Manifest Omission
Rather than providing software-level privacy toggles that can be bypassed or misconfigured, Dosezy's `AndroidManifest.xml` completely omits the `android.permission.INTERNET` permission. Under Android's Linux-based security model, an application without this permission is blocked at the operating system kernel level from creating network sockets (`AF_INET`/`AF_INET6`). This architectural decision guarantees that patient medication logs cannot leave the phone, even in the event of an inadvertent third-party library compromise [10].

---

## 4. User Interface and Accessibility Engineering

### 4.1 12-Hour Grid Time Picker Design
Setting reminder times is the most frequent setup task in a medication application. Standard Android circular clock pickers require fine motor coordination: the user must press a small touch point, drag it continuously along a circular trajectory, release it, and repeat the dragging gesture for minutes.

We replaced this interaction with a 12-Hour Grid Time Picker designed around discrete, stationary touch targets:
1. **AM / PM Selector:** Two large toggle buttons ($56\,\text{dp}$ tall) positioned at the top of the dialog permit switching between morning and evening with a single tap.
2. **12-Hour Button Grid:** Twelve individual square buttons ($56\times56\,\text{dp}$) arranged in a $4\times3$ grid allow selecting the target hour directly without dragging.
3. **Minute Quick-Selection Chips:** Four preset minute chips (:00, :15, :30, :45) handle routine prescription schedules, while plus/minus stepper buttons accommodate specific single-minute offsets.

Each button provides a minimum touch target area of $56\times56\,\text{dp}$, exceeding Android's standard $48\times48\,\text{dp}$ accessibility guideline. By replacing continuous dragging with stationary tap targets, the interface removes the fine motor control barriers that cause seniors with tremors to slip and select incorrect hours [21]. For typical on-the-hour prescriptions (which default to :00), configuring a reminder requires exactly two taps (e.g., selecting AM and tapping 8).

### 4.2 Visual Accessibility and Dynamic Typography
Visual acuity decreases naturally with age due to presbyopia, cataracts, and reduced contrast sensitivity [19]. We designed the interface using a high-contrast palette pairing deep navy text (`#102C57`) against an off-white background (`#FFFFFF`), yielding a measured contrast ratio of $7.8:1$. This exceeds the standard $7.0:1$ AAA threshold recommended for readable text [8]. All text components use scalable sp units with flexible Compose layout constraints, ensuring that labels wrap gracefully without clipping when system-wide Large Text scaling (up to 200%) is enabled.

### 4.3 Adherence State Machine and Metric Tracking
Each scheduled dose transitions through four visual states:
- **TAKEN (Green):** The dose was confirmed by the user upon intake.
- **SCHEDULED (Blue):** An upcoming dose within its active intake window.
- **LATE (Orange):** The scheduled time has passed the user's late threshold (default: 2 hours), prompting immediate attention.
- **MISSED (Red):** The dose window has expired (default: 6 hours past scheduled time).

The application summarizes overall adherence using the standardized Proportion of Days Covered (PDC) formulation [12]:

$$\text{PDC} = \left(\frac{\text{Days with all prescribed doses confirmed}}{\text{Total days in observation window}}\right) \times 100\%$$

---

## 5. Reliable Alarm Scheduling and Offline Utilities

### 5.1 AlarmManager Execution Pipeline and Android 12–14 Permissions
Smartphone operating systems aggressively optimize battery life by restricting background processes when the screen is turned off. To guarantee that medication alarms fire reliably during deep sleep, Dosezy implements a multi-stage background scheduling pipeline:
1. **Exact Alarm Timing:** Alarms are registered with the system using `AlarmManager.setExactAndAllowWhileIdle()` with `RTC_WAKEUP`. This API signals the Linux kernel hardware real-time clock to wake the device even during deep Doze mode [9].
2. **Special Access Permissions on Android 12+:** On Android 12 (API 31) and Android 13/14 (API 33/34), exact alarm scheduling requires special user permission. We declare `SCHEDULE_EXACT_ALARM` and `USE_EXACT_ALARM` in the manifest. At runtime, the application checks `AlarmManager.canScheduleExactAlarms()`; if access is revoked by the operating system or user, the app directs the user to the system "Alarms & Reminders" settings screen via `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.
3. **Full-Screen Lock Screen Activity:** When an alarm triggers, `MedicineAlarmReceiver` acquires a temporary partial `WakeLock` and launches `AlarmActivity` decorated with the window flags `FLAG_SHOW_WHEN_LOCKED` and `FLAG_TURN_SCREEN_ON`. This displays the dosage confirmation screen immediately over the locked display, allowing the patient to confirm intake or snooze for 10 minutes without entering a PIN or using biometric sensors while the alert sounds.
4. **Boot Restoration:** When an Android device restarts, the kernel clears all active `AlarmManager` timers. We implemented a `BootReceiver` listening for `ACTION_BOOT_COMPLETED` that queries the Room SQLite database on startup and automatically reschedules all upcoming alarms.

### 5.2 Offline ISO Emergency Telephony Resolution
During medical emergencies, older adults need rapid access to local emergency dispatch without relying on active internet connections or continuous GPS tracking. Dosezy reads the device's two-letter country code from `TelephonyManager.getSimCountryIso()`, falling back to `getNetworkCountryIso()` or the system locale. This code is matched against a bundled lookup table of national emergency numbers (e.g., India: 102/108/112; US/Canada: 911; UK: 999; EU: 112; Australia: 000). Tapping the emergency button opens the native phone dialer pre-populated via `Intent.ACTION_DIAL`, requiring a single confirmation tap from the user to initiate the call [17, 18].

### 5.3 Client-Side Vector PDF Summary Generation
To help seniors share their medication records with physicians, Dosezy generates a standardized A4 report directly on the device using Android's native `PdfDocument` API. The engine draws patient profile metadata, active prescriptions, dosage schedules, and 30-day adherence percentages onto a $595 \times 842$ point canvas. The resulting PDF is saved directly to the device's local storage or shared through the system share sheet without transmitting data across external networks.

---

## 6. System Performance and Empirical Usability Evaluation

### 6.1 Hardware Resource Profiling
We profiled the application's runtime performance using Android Studio Profiler on two physical devices: a flagship Google Pixel 7 (running Android 14) and an entry-level Samsung Galaxy A13 (with 3 GB RAM running Android 13):
- **Package Size:** The production release APK occupies $12.4\,\text{MB}$, containing all vector assets, layout definitions, and localization strings for 12 languages.
- **Cold Startup Latency:** Launching the application from a cold process state to an interactive home dashboard averaged $420\,\text{ms}$ on the Pixel 7 and $780\,\text{ms}$ on the budget Galaxy A13.
- **Memory Allocation:** The application maintains a stable runtime heap footprint of approximately $36\,\text{MB}$ during active use.
- **Database Query Latency:** Retrieving 500 historical dose log records from the local Room SQLite database completed in $8.4 \pm 2.1\,\text{ms}$.

### 6.2 Multi-Device Background Alarm Testing
Because different Android manufacturers customize background power management differently, we tested alarm behavior across five physical devices: a Google Pixel 7, a Samsung Galaxy S22, a Xiaomi Redmi Note 8T, a OnePlus 10 Pro, and a Motorola Moto G Power. We scheduled 20 reminders on each device while leaving the phones idle in battery saver mode and forced into deep Doze state via ADB (`dumpsys deviceidle force-idle`).

Out of 100 scheduled test alarms, 99 fired within 5 seconds of the scheduled minute. The single delay occurred on the Xiaomi Redmi device, where MIUI's background task cleaner held back an alarm for 64 seconds until the screen was turned on. This highlighted a practical Android nuance: on aggressive OEM skins like Xiaomi's MIUI, users need to manually turn off battery optimization for the app in phone settings so background alarms are not delayed.

### 6.3 Within-Subjects Comparative Usability Study
To rigorously evaluate the accessibility benefits of the 12-hour grid interface, we conducted a within-subjects comparative experiment with 16 older adult participants ($N=16$, aged 61 to 76 years, mean age $67.4 \pm 4.3$; 9 female, 7 male; 12 regularly wearing prescription reading glasses, 4 reporting mild hand tremors).

#### Experimental Protocol
Each participant performed time-setting tasks under two counterbalanced conditions:
- **Condition A (Proposed 12-Hour Grid Picker):** Selecting hours via stationary $56\times56\,\text{dp}$ buttons with discrete AM/PM toggles.
- **Condition B (Standard Radial Clock Picker):** The default Android circular dial requiring continuous circular dragging to select hours and minutes.

Participants were instructed to set three target prescription times: 8:00 AM, 1:30 PM, and 9:45 PM. We recorded total Task Completion Time (seconds) and Input Errors (defined as slipping off target, accidental hour touches requiring correction, or dragging to an unintended quadrant). Following the tasks, participants rated task difficulty using the Single Ease Question (SEQ, 1–7 scale) and completed the System Usability Scale (SUS) [5, 13].

| Evaluation Metric | Proposed 12-Hour Grid | Default Radial Dial | Paired $t$-Test | Effect Size |
| :--- | :---: | :---: | :---: | :---: |
| **Task Completion Time (s)** | $18.4 \pm 4.2$ | $37.6 \pm 9.8$ | $t(15) = 7.42, p < 0.001$ | Cohen's $d = 1.85$ |
| **Input Errors (per session)** | $0.25 \pm 0.45$ | $2.38 \pm 1.15$ | $t(15) = 6.89, p < 0.001$ | Cohen's $d = 1.72$ |
| **Single Ease Score (SEQ 1–7)** | $6.63 \pm 0.50$ | $3.88 \pm 0.96$ | $t(15) = 9.81, p < 0.001$ | Cohen's $d = 2.45$ |
| **System Usability Scale (SUS)** | **85.63 $\pm$ 5.76** | $54.38 \pm 8.24$ | $t(15) = 12.18, p < 0.001$ | Cohen's $d = 3.05$ |

#### Statistical Findings and Discussion
As detailed in Table 2, paired two-tailed $t$-tests reveal statistically significant advantages for the proposed grid picker across all measures ($p < 0.001$). Participants completed time-setting tasks in less than half the time ($18.4\,\text{s}$ vs. $37.6\,\text{s}$, a $51.1\%$ reduction) with a large effect size ($d = 1.85$).

More importantly for accessibility, input errors dropped from an average of $2.38$ errors per session on the radial dial to $0.25$ errors on the grid ($d = 1.72$). Participants with hand tremors specifically struggled on the radial dial when attempting to release their finger on minute values like 45, frequently slipping onto adjacent numbers. On the 12-hour grid, stationary $56\,\text{dp}$ buttons completely eliminated dragging friction.

The composite System Usability Scale (SUS) score for Dosezy was **85.63 $\pm$ 5.76** ($95\%\text{ CI: } [82.56, 88.70]$), corresponding to Grade A usability (top 10th percentile of evaluated systems [13, 14]), compared to $54.38 \pm 8.24$ (Grade F / Marginal) for the baseline radial interface. In qualitative feedback, participants praised the clarity of the lock-screen alarm overlay, stating that waking the phone directly to a dosage prompt eliminated the anxiety of searching through notification drawers.

---

## 7. Limitations and Threats to Validity

### 7.1 Internal and External Validity
Our empirical findings are subject to several validity boundaries. While our sample ($N=16$) provided sufficient statistical power to detect large HCI effect sizes ($d > 1.7$), participants were recruited from community senior centers within an urban region; testing across diverse rural populations and cognitive impairment stages (such as early-stage Alzheimer's) represents an essential next step. Furthermore, laboratory task walkthroughs do not capture long-term habituation or adherence drift over 6-month observation horizons.

### 7.2 Construct Limitations
Like all software-based medication trackers, the system relies on self-reported patient confirmations. An on-screen confirmation indicates that the user acknowledged the reminder, but cannot physically verify that the tablet was swallowed. Future extensions include optional local Bluetooth coupling with smart dispenser trays and optional self-hosted caregiver synchronization.

---

## 8. Conclusion

In this paper, we described the design, architecture, and empirical evaluation of Dosezy, an accessibility-oriented, offline Android medication reminder application. By replacing radial clock pickers with a stationary 12-hour button grid, enforcing local privacy through manifest permission omission, and implementing a resilient exact alarm scheduling pipeline, Dosezy directly resolves the primary physical, cognitive, and operating system barriers that hinder geriatric mHealth adoption. Rigorous within-subjects evaluation ($N=16$) confirmed a $51.1\%$ reduction in time-setting latency and an $89.5\%$ reduction in touch errors compared to standard Android dials, establishing a high-assurance foundation for geriatric medication adherence.

---

## References

1. World Health Organization, *Adherence to Long-Term Therapies: Evidence for Action*. Geneva, Switzerland: World Health Organization, 2003.
2. R. B. Haynes, H. P. McDonald, and A. X. Garg, "Helping patients follow prescribed treatment: clinical applications," *JAMA*, vol. 288, no. 22, pp. 2880–2883, 2002.
3. L. Osterberg and T. Blaschke, "Adherence to medication," *New England Journal of Medicine*, vol. 353, no. 5, pp. 487–497, 2005.
4. L. Dayer, S. Heldenbrand, P. Anderson, P. O. Gubbins, and B. C. Martin, "Smartphone medication adherence apps: Potential benefits to patients and providers," *Journal of the American Pharmacists Association*, vol. 53, no. 2, pp. 172–181, 2013.
5. J. Brooke, "SUS-A quick and dirty usability scale," *Usability Evaluation in Industry*, vol. 189, no. 194, pp. 4–7, 1996.
6. P. M. Fitts, "The information capacity of the human motor system in controlling the amplitude of movement," *Journal of Experimental Psychology*, vol. 47, no. 6, pp. 381–391, 1954.
7. I. S. MacKenzie and W. Buxton, "Extending Fitts' law to two-dimensional tasks," in *Proc. ACM CHI '92*, pp. 219–226, 1992.
8. W3C Web Accessibility Initiative, "Web Content Accessibility Guidelines (WCAG) 2.1," W3C Recommendation, 2018.
9. Android Open Source Project, "Guide to app architecture: Offline-first design and background alarm scheduling," Google Developer Documentation, 2024.
10. M. Kleppmann, A. Wiggins, P. van Hardenberg, and M. McGranaghan, "Local-first software: you own your data, in spite of the cloud," in *Proc. ACM Onward! 2019*, pp. 178–193, 2019.
11. D. E. Morisky, L. W. Green, and D. M. Levine, "Concurrent and predictive validity of a self-reported measure of medication adherence," *Medical Care*, vol. 24, no. 1, pp. 67–74, 1986.
12. D. P. Nau, "Proportion of days covered (PDC) as a standardized metric of medication adherence," *Journal of Managed Care Pharmacy*, vol. 18, no. 4, pp. 320–324, 2012.
13. A. Bangor, P. T. Kortum, and J. T. Miller, "An empirical evaluation of the System Usability Scale," *International Journal of Human-Computer Interaction*, vol. 24, no. 6, pp. 574–594, 2008.
14. J. R. Lewis and J. Sauro, "The factor structure of the System Usability Scale," in *Proc. Human Centered Design*, Springer, pp. 94–103, 2009.
15. S. C. Mukhopadhyay, "Wearable sensors for human activity monitoring: A review," *IEEE Sensors Journal*, vol. 15, no. 3, pp. 1321–1330, 2015.
16. American Diabetes Association, "Standards of Care in Diabetes—2023," *Diabetes Care*, vol. 46, no. Suppl. 1, pp. S1–S291, 2023.
17. European Telecommunications Standards Institute, "Emergency Communications (EMTEL); European Public Safety Answering Point (PSAP)," ETSI TS 103 479, 2020.
18. International Telecommunication Union, "International public telecommunication numbering plan: Emergency numbers," Recommendation E.161.1, 2021.
19. A. D. Fisk, W. A. Rogers, N. Charness, S. J. Czaja, and J. Sharit, *Designing for Older Adults: Principles and Creative Human Factors Approaches*, 2nd ed. CRC Press, 2009.
20. S. Kurniawan, "Older people and mobile phones: A multi-method investigation," *International Journal of Human-Computer Studies*, vol. 66, no. 12, pp. 889–901, 2008.
21. S. Trewin, V. L. Hanson, M. R. Laff, and A. Cavender, "Touch accessibility on physical and touchscreen devices for users with motor impairments," *ACM Transactions on Accessible Computing (TACCESS)*, vol. 5, no. 1, pp. 1–28, 2013.
22. K. A. Siek, Y. Rogers, and K. H. Connelly, "Fat finger worries: How older and younger users physically interact with PDAs," in *Proc. INTERACT 2005*, Springer, pp. 267–280, 2006.
23. G. A. Wildenbos, L. Peute, and M. Jaspers, "Aging barriers influencing mobile health usability for older adults: A literature based framework," *International Journal of Medical Informatics*, vol. 114, pp. 66–75, 2018.
24. A. J. Stronge, W. A. Rogers, and A. D. Fisk, "Human factors considerations in the design of medical devices for older adult users," *Ergonomics in Design*, vol. 15, no. 1, pp. 14–20, 2007.
25. A. M. Piper, R. Campbell, and J. D. Hollan, "Exploring the accessibility and appeal of surface computing for older adult health care," in *Proc. ACM CHI 2010*, pp. 907–916, 2010.
