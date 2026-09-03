<h1 align="center">
  <a href="https://github.com/saad2134/dosezy">
    <img width="1440" height="310" alt="💊 Dosezy – Medicine Tracking Simplified" src="https://github.com/user-attachments/assets/c57df39a-b714-4e82-8420-42c003c072f7" />
 </a>
</h1>

> <p align="center">🚨 <strong> A smart medicine tracking app that is free, open source, offline and private by default with coming soon, optional Caregiver Cloud Platform for analytics, multi-device sync and family sharing. Dosezy transforms medication management into a simple, stress-free experience. Built with accessibility at its core, the app features clear, large text and intuitive navigation: perfect for elderly users and anyone managing multiple prescriptions.</strong></p>  


<div align="center">

![Phase](https://img.shields.io/badge/🛠️%20Phase-Released%20v2%2E2%2E2-blue?style=for-the-badge)
![Platforms](https://img.shields.io/badge/🌐%20Platforms-Android-28a745?style=for-the-badge)

</div>

## 🛍️ Download Now 

### 💊 Patient App (Free, Open-source, Offline & Private by Default)

<div align="center">
  <a href="https://github.com/saad2134/dosezy/releases">
    <img width="197" height="59" alt="get-github-1189403918" src="https://github.com/user-attachments/assets/e9ae5d41-fb6c-468b-b1cc-cc4fb534aa10" />
  </a>
</div>

#### Coming Soon On

<div align="center">
  <!-- <a href="">
    <img width="197" height="59" alt="Google Play" src="https://github.com/user-attachments/assets/7c8720ee-4c1e-4c9a-86c0-8086acd12c71" />
  </a>
  <a href="">
    <img width="197" height="59" alt="Galaxy Store" src="https://github.com/user-attachments/assets/52b16b2f-1517-45b2-93b4-d3b6011b7608" />
  </a>
  <a href="">
    <img width="197" height="59" alt="Huawei AppGallery" src="https://github.com/user-attachments/assets/3cbafa74-a2eb-4f33-9b87-202db77e5280" />
  </a> -->
  <a href="https://gitlab.com/fdroid/fdroiddata/-/merge_requests/44626">
    <img width="197" height="59" alt="F-Droid" src="https://github.com/user-attachments/assets/9a463d23-368b-4974-9686-e28649e00b04" />
  </a>
</div>

### 🩺 Caregiver Cloud Platform (Optional, Self-Hostable, Coming Soon)

- coming soon

## ✨ Features  

### 💊 Patient App (Free, Open-source, Offline & Private by Default)

Once you input the basics: name, dosage, timings, and frequency, the app intelligently handles your medication schedule.

- 📅 **Medication Adherence** – Automated reminders ensure timely medication consumption.
- 🌐 **12 Global Languages** – Full native localization in English, Chinese (中文), Spanish (Español), Hindi (हिन्दी), Portuguese (Português), Arabic (العربية), French (Français), German (Deutsch), Japanese (日本語), Russian (Русский), Italian (Italiano), and Bengali (বাংলা).
- 🌍 **International Emergency Services** – Interactive country/region emergency dialer supporting India, USA/Canada, UK, EU, China, Japan, Russia, Brazil, Bangladesh, and Australia with automatic SIM/network country ISO detection and locale fallback.
- ⏰ **Configurable Late & Missed Thresholds** – Custom **Consider Late After** (1-3h) and **Consider Missed After** (3-9h) settings with orange high-visibility late status badges (`Xh Xm ago`) and "Mark Late" actions.
- 🕒 **12-First Grid Time Picker** – Easy 12-hour (12 first layout) and 24-hour interactive grid time selector with quick minute chips.
- ♿ **Accessibility-First Design** – Large text, high-contrast colors, and simple controls for elderly users.  
- 🔔 **Reliable Notification System** – Persistent alerts with precise battery & sound indicators and snooze/taken/late action triggers.  
- 🎨 **Adaptive System Dark/Light Theme** – Complete theme support respecting the Android system default settings with zero white flash transitions.
- 📅 **Custom Calendar Scheduling** – Full interactive support for daily, weekly (specific days of the week), and monthly (specific days of the month) frequencies.
- 👤 **Multi-Profile Management** – Create, edit, and switch between family member profiles.
- 📄 **Data Security & Native Exports** – Local `Room` database storage with native PDF medical reports (English format note), JSON backup archives, and CSV export capabilities.  
- 🛍️ **Multi-Store Update Check & Web Fallback** – Automatically detects installation store source (Google Play, F-Droid, Huawei AppGallery, Samsung Galaxy Store, Xiaomi GetApps, Amazon Appstore, Aurora Store, GitHub Sideload) and launches native store or web browser fallback.  
- 👨‍👩‍👧 **Caregiver Support** – Logs and adherence history help caregivers track missed and late doses.  

### 🩺 Caregiver Cloud Platform (Optional, Self-Hostable, Coming Soon)

- coming soon

---

## ⚙️ Platforms

### Patient App

<table border="1" cellpadding="10" cellspacing="0">
  <thead>
    <tr>
      <th>Platform</th>
      <th>Min Version</th>
      <th>Supported?</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Android</td>
      <td>v7.0 or later</td>
      <td>✅</td>
    </tr>
    <tr>
      <td>iOS</td>
      <td>Coming Soon</td>
      <td>⏳</td>
    </tr>
  </tbody>
</table>


## 🛠️ Tech Stack

### Patient App (Android)

- Language: Kotlin
- UI Framework: Jetpack Compose
- Build System: Gradle (Kotlin DSL)
- Storage & Data: Room Database, DataStore, SharedPreferences, File-based storage

### Caregiver Cloud Platform (Self-Hostable, Coming Soon)

- coming soon


## 🚀 Getting Started (Working on Source Code)

### Patient App (Android)
  1. Fork the repo.
  2. Download & Install Android Studio
  3. Enable Git Version Control & Clone the Repo
  4. Navigate to `/patient/android`
  4. Wait for gradle to initialize
  5. Enjoy.


## 🏛️ Core Philosophy & Product Architecture

Dosezy is architected as an **open-source, local-first monorepo** with an optional connected cloud layer:

> **"Dosezy Cloud enhances Dosezy, never holds Dosezy hostage."**

```mermaid
flowchart TB
 subgraph Clients["📱 Patient Client Applications (100% Local-First & Offline)"]
    direction LR
        AndroidApp["Android App<br>(patient/android)"]
        iOSApp["iOS App<br>(patient/ios)"]
  end
 subgraph ServerCloud["☁️ Cloud & Sync Platform (Optional & Self-Hostable)"]
    direction LR
        SyncServer["Self-Hostable Sync Server<br>(server/)"]
        ContractNote["📜 Shared OpenAPI 3.0 Contract (api/openapi.yaml)<br><i>Defines REST APIs, data schemas, and sync protocols.</i>"]
  end
 subgraph s1["🌐 Website"]
        MarketingWeb["Web Portal, Guides & Manuals<br>(website/)"]
  end
 subgraph s2["❤️ Caregiver Client Applications"]
        CaregiverWeb["Caregiver Web Dashboard<br>(caregiver/)"]
  end
    iOSApp <== "Optional Encrypted Sync" ==> ContractNote
    AndroidApp <== "Optional Encrypted Sync" ==> ContractNote
    SyncServer <--> CaregiverWeb
    ContractNote -- "Validates REST & Sync API" --> SyncServer

    ContractNote@{ shape: hex}
    style ContractNote stroke-width:2px,stroke-dasharray: 2
```

- **Local-First by Default:** All fundamental features—scheduling, persistent notifications, history tracking, data exports, and emergency services—operate completely offline without requiring an account or network connectivity.
- **Server-Agnostic Sync:** The app supports both the official hosted service (`cloud.dosezy.app`) and Self-Hostable community servers (`server/`).
- **Contract-Driven Development:** Client apps (`patient/android`, `patient/ios`, `caregiver/`) conform to a shared OpenAPI specification (`api/openapi.yaml`), ensuring consistent data models and seamless interoperability.



## 📁 Monorepo Architecture

```
dosezy/
├── .github/                  # CI/CD workflows, build pipelines, and repository automation
├── api/                      # OpenAPI 3.0 contract (openapi.yaml) defining shared client/server schemas
├── caregiver/                # Caregiver companion application & web dashboard (app.dosezy.com)
├── docs/                     # Architecture Decision Records (ADRs), domain specifications, and guides
│   ├── architecture/         # Data layer, sync protocols, and encryption designs
│   ├── privacy/              # Data safety declarations and health privacy compliance
│   └── product/              # Product specs, wireframes, and accessibility requirements
├── patient/                  # Patient application modules
│   ├── android/              # Native Android patient app (Jetpack Compose & Kotlin)
│   │   ├── app/              # Source code, UI components, Room DB, and background services
│   │   ├── assets/           # App branding artwork and density icons
│   │   ├── fastlane/         # F-Droid & store metadata and localized changelogs
│   │   ├── gradle/           # Build wrappers and version catalogs
│   │   └── ui/               # Interactive prototyping templates and wireframes
│   └── ios/                  # (Roadmap) Native iOS patient app (SwiftUI & Swift)
├── resources/                # Offline user documentation, FAQs, and support manuals
│   ├── FAQs.md
│   └── UserManual.md
├── scripts/                  # Code generation, release packaging, and translation sync automation
├── server/                   # Open-source self-hostable sync server (PostgreSQL + REST API)
├── website/                  # Next.js marketing and landing page web app (React 19 & Tailwind CSS v4)
├── CONTRIBUTING.md           # Contribution guidelines and coding standards
├── LICENSE                   # Open-source MIT License
└── README.md                 # Master project documentation
```

## 🗺️ Product Roadmap

<table border="1" cellpadding="10" cellspacing="0">
  <thead>
    <tr>
      <th>Phase / Version</th>
      <th>Milestone</th>
      <th>Focus Areas</th>
      <th>Key Deliverables</th>
      <th align="center">Status</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><strong>v1.0 – v2.1</strong></td>
      <td><strong>Local-First Android Client</strong></td>
      <td>Core Medication Adherence</td>
      <td>Jetpack Compose UI, 12 native localizations, Room offline database, customizable late/missed thresholds, full-screen alarms, emergency dialer, zero account barrier.</td>
      <td align="center">🟢 Released</td>
    </tr>
    <tr>
      <td><strong>v2.5</strong></td>
      <td><strong>Domain & API Contract</strong></td>
      <td>Schema & Boundary Decoupling</td>
      <td>Canonical domain models (<code>Medication</code>, <code>Schedule</code>, <code>DoseEvent</code>), <code>api/openapi.yaml</code> OpenAPI 3.0 specification, multi-platform client generation.</td>
      <td align="center">🟡 In Design</td>
    </tr>
    <tr>
      <td><strong>v3.0</strong></td>
      <td><strong>Self-Hostable Backend & Sync</strong></td>
      <td>Incremental Cloud Sync</td>
      <td>Open-source server (<code>server/</code>), PostgreSQL DB, conflict-free sync engine, offline mutation queues, optional cloud connection.</td>
      <td align="center">⏳ Planned</td>
    </tr>
    <tr>
      <td><strong>v3.5</strong></td>
      <td><strong>Native iOS Patient Client</strong></td>
      <td>Multi-Platform Expansion</td>
      <td>Native Swift & SwiftUI app (<code>patient/ios/</code>), SwiftData/CoreData local database, Apple UserNotifications, zero-account local operation.</td>
      <td align="center">⏳ Planned</td>
    </tr>
    <tr>
      <td><strong>v4.0</strong></td>
      <td><strong>Caregiver Cloud Platform</strong></td>
      <td>Connected Family Care</td>
      <td>Dedicated Caregiver web portal (<code>caregiver/</code> at <code>app.dosezy.com</code>), remote adherence monitoring, patient-controlled granular sharing permissions.</td>
      <td align="center">⏳ Planned</td>
    </tr>
  </tbody>
</table>



## 📱 Screenshots

### Patient App

<table> 

<tr> 
<td>
<strong>Home Screen</strong><br><br> <img width="411" height="915" alt="image" src="https://github.com/user-attachments/assets/116b4a0b-fa72-496c-b56c-305712dd8047" />
</td> 
<td>
<strong>Schedule Screen</strong><br><br> <img width="410" height="915" alt="image" src="https://github.com/user-attachments/assets/ae1b400c-594c-4575-a419-0a60b3a24b2e" />
</td>
<td>
<strong>Medicines Screen</strong><br><br> <img width="410" height="914" alt="image" src="https://github.com/user-attachments/assets/7f772ce5-1412-4781-a9cd-29c40661d6df" />
</td>
</tr> 
<tr> 
<td>
<strong>Add New Medicine Screen</strong><br><br> <img width="410" height="912" alt="image" src="https://github.com/user-attachments/assets/3684738e-a7c1-420f-aae3-9ce822a78ac7" />
</td> 
<td>
<strong>Menu List Screen</strong><br><br> <img width="410" height="912" alt="image" src="https://github.com/user-attachments/assets/9b7a72d3-5e09-4d12-9b08-feb1584eae17" />
</td>
<td>
<strong>Notification Status Dialog</strong><br><br> <img width="411" height="914" alt="image" src="https://github.com/user-attachments/assets/19baf470-34de-42b6-bc80-9caaaa84e613" />
</td>
</tr> 

<tr> 
<td>
<strong>Profile Actions Dialog</strong><br><br> <img width="410" height="913" alt="image" src="https://github.com/user-attachments/assets/8d2c10ee-0c32-48a7-b2fd-b606f0db61ae" />
</td> 
<td>
<strong>Data Export Feature</strong><br><br> <img width="410" height="911" alt="image" src="https://github.com/user-attachments/assets/bdc43e8a-1814-44b2-81e1-55b36fbf09df" />
</td>
<td>
<strong>Preferences Setting</strong><br><br> <img width="409" height="910" alt="image" src="https://github.com/user-attachments/assets/887d0b85-233e-4e13-8c08-389e2ec73c0c" />
</td>
</tr> 

</table>

---

## 📊 **Project Stats**

<div align="center">
  
![Repo Size](https://img.shields.io/github/repo-size/saad2134/dosezy)
![Last Commit](https://img.shields.io/github/last-commit/saad2134/dosezy)
![Open Issues](https://img.shields.io/github/issues/saad2134/dosezy)
![Open PRs](https://img.shields.io/github/issues-pr/saad2134/dosezy)
![License](https://img.shields.io/github/license/saad2134/dosezy)
![Forks](https://img.shields.io/github/forks/saad2134/dosezy?style=social)
![Stars](https://img.shields.io/github/stars/saad2134/dosezy?style=social)
![Watchers](https://img.shields.io/github/watchers/saad2134/dosezy?style=social)
![Contributors](https://img.shields.io/github/contributors/saad2134/dosezy)
![Languages](https://img.shields.io/github/languages/count/saad2134/dosezy)
![Top Language](https://img.shields.io/github/languages/top/saad2134/dosezy)

</div>

## 🖼️ App Icon 
<img src="https://github.com/user-attachments/assets/213a3fc0-b737-4df2-9c70-693a7f6d7467" alt="Dosezy Icon" title="Dosezy" style="width:500px;">

## 🔰 Banner
<img width="1280" height="640" alt="New Project" src="https://github.com/user-attachments/assets/d2331448-fc78-41a0-920d-e0e5d8e171e2" />

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

- ✅ Commercial use
- ✅ Modification
- ✅ Distribution
- ✅ Private use
- ❌ Liability
- ❌ Warranty

---

## 👤 Publisher

Developed and published by **Saad (@saad2134)**.

---

## ✍️ Endnote

<p align="center">⭐ Star this repo if you found it helpful! Thanks for reading.</p>

---

## 🏷 Tags  

`android` `ios` `swiftui` `jetpack-compose` `kotlin` `open-source` `local-first` `offline-first` `medicine-reminder` `medicine-management` `medication-adherence` `caregiver` `elderly-care` `health-tech` `self-hosted` `dosezy` `nextjs`
  
