<h1 align="center"> 💊 Dosezy – Medicine Adherence Simplified</h1>

> <p align="center">🚨 <strong> An Android medicine tracking app. **Dosezy** transforms medication management into a simple, stress-free experience. Built with accessibility at its core, the app features **clear, large text** and **intuitive navigation**: perfect for elderly users and anyone managing multiple prescriptions.</strong></p>  


<div align="center">

![Phase](https://img.shields.io/badge/🛠️%20Phase-Released%20v2%2E1%2E0-blue?style=for-the-badge)
![Platforms](https://img.shields.io/badge/🌐%20Platforms-Android-28a745?style=for-the-badge)

</div>

## 🛍️ Download Now  

<div align="center">
  <a href="https://github.com/saad2134/dosezy/releases">
    <img width="197" height="59" alt="get-github-1189403918" src="https://github.com/user-attachments/assets/e9ae5d41-fb6c-468b-b1cc-cc4fb534aa10" />
  </a>
</div>

## ✨ Features  

Once you input the basics: name, dosage, timing, and frequency, the app intelligently handles your medication schedule.

- 📅 **Medication Adherence** – Automated reminders ensure timely medication consumption.
- 🌐 **12 Global Languages** – Full native localization in English, Chinese (中文), Spanish (Español), Hindi (हिन्दी), Portuguese (Português), Arabic (العربية), French (Français), German (Deutsch), Japanese (日本語), Russian (Русский), Italian (Italiano), and Bengali (বাংলা).
- 🌍 **International Emergency Services** – Interactive country/region emergency dialer supporting India, USA/Canada, UK, EU, China, Japan, Russia, Brazil, Bangladesh, and Australia with automatic country detection based on language.
- ⏰ **Configurable Late & Missed Thresholds** – Custom **Consider Late After** (1-3h) and **Consider Missed After** (3-9h) settings with orange high-visibility late status badges (`Xh Xm ago`) and "Mark Late" actions.
- 🕒 **12-First Grid Time Picker** – Easy 12-hour (12 first layout) and 24-hour interactive grid time selector with quick minute chips.
- ♿ **Accessibility-First Design** – Large text, high-contrast colors, and simple controls for elderly users.  
- 🔔 **Reliable Notification System** – Persistent alerts with precise battery & sound indicators and snooze/taken/late action triggers.  
- 🎨 **Adaptive System Dark/Light Theme** – Complete theme support respecting the Android system default settings with zero white flash transitions.
- 📅 **Custom Calendar Scheduling** – Full interactive support for daily, weekly (specific days of the week), and monthly (specific days of the month) frequencies.
- 👤 **Multi-Profile Management** – Create, edit, and switch between family member profiles.
- 📄 **Data Security & Export** – Local `Room` database storage with PDF report and JSON data export capabilities.  
- 👨‍👩‍👧 **Caregiver Support** – Logs and adherence history help caregivers track missed and late doses.  

---

## ⚙️ Platforms

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
  </tbody>
</table>


## 🛠️ Tech Stack

- Language: Kotlin
- UI Framework: Jetpack Compose
- Build System: Gradle (Kotlin DSL)
- Storage & Data: DataStore (evidenced by libdatastore_shared_counter.so native libs). Shared Preferences for simple data persistence. File-based storage for assets and resources.


## 🚀 Getting Started

### Android (Using the app)
  1. Enable `Install from Unknown Sources` in your android device settings.
  2. Download the latest `.apk` file from the [`apks`](apks/) directory and install it on your device.

### Android (Source)
  1. Fork the repo.
  2. Download & Install Android Studio
  3. Enable Git Version Control & Clone the Repo
  4. Wait for gradle to initialize
  5. Enjoy.


## 📁 Project Architecture
```
dosezy/
├── app/
│ ├── build.gradle.kts
│ ├── proguard-rules.pro
│ ├── src/
│ │ ├── main/
│ │ │ ├── java/com/example/dosezy/
│ │ │ │ ├── MainActivity.kt
│ │ │ │ ├── SplashActivity.kt
│ │ │ │ ├── notifications/
│ │ │ │ │ ├── MedicineAlarmReceiver.kt
│ │ │ │ │ ├── MedicineForegroundService.kt
│ │ │ │ │ └── MedicineReminderService.kt
│ │ │ │ ├── ui/
│ │ │ │ │ ├── components/
│ │ │ │ │ │ ├── NavigationBar.kt
│ │ │ │ │ │ ├── TopBar.kt
│ │ │ │ │ │ └── ...
│ │ │ │ │ ├── screens/
│ │ │ │ │ │ ├── HomeScreen.kt
│ │ │ │ │ │ ├── MedicinesScreen.kt
│ │ │ │ │ │ ├── NewUserScreen.kt
│ │ │ │ │ │ └── ...
│ │ │ │ │ ├── subscreens/
│ │ │ │ │ │ ├── AddMedScreen.kt
│ │ │ │ │ │ ├── EditMedScreen.kt
│ │ │ │ │ │ └── ...
│ │ │ │ │ └── theme/
│ │ │ │ │ ├── Color.kt
│ │ │ │ │ ├── Theme.kt
│ │ │ │ │ └── Type.kt
│ │ │ │ └── utils/
│ │ │ ├── res/
│ │ │ │ ├── drawable/
│ │ │ │ ├── mipmap-*/
│ │ │ │ ├── values/
│ │ │ │ └── xml/
│ │ │ └── AndroidManifest.xml
│ │ ├── androidTest/
│ │ └── test/
│ └── build/ (generated build outputs)
├── assets/
│ ├── banner.png
│ └── icon-squircle-1000px.png
├── gradle/
│ ├── libs.versions.toml
│ └── wrapper/
└── build/ (project build files)
```


### Key Components

**Main Source Files:**
- `MainActivity.kt` - Main application entry point
- `SplashActivity.kt` - Splash screen activity
- **UI Components** - Jetpack Compose based UI
- **Notification Services** - Medicine reminder system
- **Theme System** - Custom theming with colors and typography

**UI Structure:**
- Screens: Home, Medicines, Schedule, Settings, New User
- Subscreens: Add/Edit Medicine, Emergency, Help, Preferences
- Components: Navigation bars, dialogs, custom elements

**Resources:**
- Drawables for icons and backgrounds
- Multiple mipmap densities for launcher icons
- Values for colors, strings, and themes
- XML configurations for backup and data extraction

## 📱 Screenshots

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

## ⭐ Star History

<a href="https://www.star-history.com/?type=date&repos=saad2134%2Fdosezy">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=saad2134/dosezy&type=date&theme=dark&legend=top-left&sealed_token=IxQzYbJCwvWZotY69Wsw4z2IOr0LpyI7E4jAS22tbkCxUE8_kUqbiS7jvvT397WpfrmlQHlrWKB58gn3Rgx5xHBZNpCUKv0JGNCDWCXPyrEc6JKZNbR3rGWEAzU_TVVzUPzmwyILtubIjhwtBfDSORidBUPEXYQWscAu-tAxnh_UnDgwW9UYzmunEr0b" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=saad2134/dosezy&type=date&legend=top-left&sealed_token=IxQzYbJCwvWZotY69Wsw4z2IOr0LpyI7E4jAS22tbkCxUE8_kUqbiS7jvvT397WpfrmlQHlrWKB58gn3Rgx5xHBZNpCUKv0JGNCDWCXPyrEc6JKZNbR3rGWEAzU_TVVzUPzmwyILtubIjhwtBfDSORidBUPEXYQWscAu-tAxnh_UnDgwW9UYzmunEr0b" />
   <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=saad2134/dosezy&type=date&legend=top-left&sealed_token=IxQzYbJCwvWZotY69Wsw4z2IOr0LpyI7E4jAS22tbkCxUE8_kUqbiS7jvvT397WpfrmlQHlrWKB58gn3Rgx5xHBZNpCUKv0JGNCDWCXPyrEc6JKZNbR3rGWEAzU_TVVzUPzmwyILtubIjhwtBfDSORidBUPEXYQWscAu-tAxnh_UnDgwW9UYzmunEr0b" />
 </picture>
</a>

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

`android` `mobile-app` `android-application`  
`elderly-people` `medicine-management` `medicine-reminder` `elderly-care` `medicine-tracking` `dosezy`  
