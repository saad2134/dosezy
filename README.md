<h1 align="center"> 💊 Dosezy – Medicine Adherence Simplified</h1>

> <p align="center">🚨 <strong> An Android medicine tracking app. **Dosezy** transforms medication management into a simple, stress-free experience. Built with accessibility at its core, the app features **clear, large text** and **intuitive navigation**: perfect for elderly users and anyone managing multiple prescriptions.</strong></p>  


<div align="center">

![Phase](https://img.shields.io/badge/🛠️%20Phase-Released%20v1%2E3%2E2-blue?style=for-the-badge)
![Platforms](https://img.shields.io/badge/🌐%20Platforms-Android-28a745?style=for-the-badge)

</div>
 
## ✨ Features  

Once you input the basics: name, dosage, timing, and frequency, the app intelligently handles your medication schedule.

- 📅 **Medication Adherence** – Automated reminders ensure timely medication consumption.  
- ♿ **Accessibility-First Design** – Large text, high-contrast colors, and simple controls for elderly users.  
- 🔔 **Reliable Notification System** – Persistent alerts with precise emulator battery level indicators and snooze/taken action triggers.  
- 🎨 **Adaptive System Dark/Light Theme** – Complete theme support respecting the Android system default settings with fully optimized high-contrast elements.
- 📅 **Custom Calendar Scheduling** – Full interactive support for weekly (specific days of the week) and monthly (specific days of the month) frequencies.
- 🔒 **Data Security & Portability** – Local `Room` database storage with CSV export capabilities.  
- 👨‍👩‍👧 **Caregiver Support** – Logs and adherence history help caregivers track missed doses.  

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
dosezy-uidesign-webview/
├── apks/
│ └── dosezy-uidesign-webview.apk
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

<a href="https://www.star-history.com/#saad2134/dosezy&Date">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=saad2134/dosezy&type=Date&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=saad2134/dosezy&type=Date" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=saad2134/dosezy&type=Date" />
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
