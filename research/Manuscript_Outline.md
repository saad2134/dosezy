# 📝 Research Paper Manuscript Outline

**Title:** Dosezy: An Accessibility-First Mobile Platform for Medication Adherence and Tracking Among Elderly Users  
**Authors:** [Your Name / Research Team]  
**Target Venue:** DOAJ-Indexed No-APC Journal / IEEE Student Track  

---

## Abstract
Medication non-adherence among elderly patients represents a major public health challenge, frequently resulting in preventable hospitalizations and complications. Existing mobile reminder applications often suffer from steep learning curves, cluttered user interfaces, heavy reliance on cloud connectivity, and potential privacy leaks of sensitive medical data. In this paper, we present **Dosezy**, an open-source, accessibility-first, local-first mobile software framework designed specifically for senior medication management. Dosezy incorporates a 12-hour grid time picker, native support for 12 global languages, automatic emergency service dialer detection based on device SIM/network ISO standards, and a deterministic alarm manager sync state machine. Crucially, Dosezy operates 100% offline without requiring internet permissions, preserving user health privacy while enabling local PDF medical report generation and JSON data exports. We describe the platform architecture, evaluate its notification reliability across diverse OEM Android environments, and discuss usability feedback demonstrating low cognitive load for elderly patients.

**Keywords:** Mobile Health (mHealth), Medication Adherence, Accessibility, Human-Computer Interaction (HCI), Local-First Software, Privacy-Preserving Systems.

---

## I. Introduction
- Global demographic shift towards aging populations and rising prevalence of chronic conditions requiring multi-drug regimens.
- Key barriers in current mHealth solutions: digital literacy gaps, complex 24-hour time pickers, network latency dependency, and privacy concerns.
- Key contributions of Dosezy:
  1. Accessibility-first design with large touch targets and high-contrast UI.
  2. Local-first architecture ensuring zero network latency and complete health data privacy.
  3. Multilingual support (12 languages) combined with automatic SIM/locale country emergency service matching.
  4. Native clinical summary generation (PDF health report & JSON backup).

---

## II. Related Work
- Analysis of existing medication reminder applications (e.g. Medisafe, MyTherapy).
- Comparison matrix evaluating offline privacy, emergency features, multilingual capabilities, and UI complexity.

---

## III. System Architecture & Implementation

### A. Core Stack
- **Client Layer:** Android SDK (Kotlin), Jetpack Compose, Material Design 3.
- **Local Persistence:** Android Room SQLite Database, DataStore preferences.
- **Shared API Contract:** OpenAPI 3.0 specification (`api/openapi.yaml`).

```
┌─────────────────────────────────────────────────────────────┐
│ 📱 DOSEZY MOBILE CLIENT LAYER                              │
│ • Jetpack Compose UI  • 12 Localizations  • Emergency ISO   │
├─────────────────────────────────────────────────────────────┤
│ 💾 LOCAL STORAGE & EXPORT LAYER                             │
│ • Room SQLite DB  • Native PDF Exporter  • JSON Archives    │
└─────────────────────────────────────────────────────────────┘
```

### B. Notification & Adherence State Machine
- `AlarmManager` exact time scheduling with `BroadcastReceiver` triggers.
- Full-screen `AlarmActivity` interface overriding system shade notifications.
- Configurable **Consider Late After** (1–3h) and **Consider Missed After** (3–9h) threshold logic.

---

## IV. Accessibility & Human Factors Design
- **12-Hour First Grid Picker:** Empirical comparison against standard time wheels.
- **Visual Badging System:** Color-coded status indicators (`Normal`, `Late (Orange)`, `Missed (Red)`, `Taken (Green)`).
- **Offline Data Security:** Native PDF medical report generation formatted for clinical review.

---

## V. Experimental Results & Discussion
- **Usability Evaluation:** System Usability Scale (SUS) benchmark results.
- **Notification Reliability:** Alarm trigger timing accuracy across OEM battery optimization modes (MIUI, Samsung One UI, Stock Android).
- **Localization Efficiency:** Emergency dialer matching speed across SIM ISO and locale fallback parameters.

---

## VI. Conclusion & Future Work
- Summary of findings demonstrating high usability and zero-privacy-compromise adherence tracking.
- Roadmap for optional encrypted cloud sync server (`server/`) and caregiver web dashboard (`caregiver/`).

---

## References
1. World Health Organization (WHO), "Adherence to long-term therapies: evidence for action," 2003.
2. Android Developer Documentation, "Offline-First Application Architecture," 2024.
3. Open Source Health Informatics Standards & OpenAPI Specification 3.0.3.
