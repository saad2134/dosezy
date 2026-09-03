# 🏛️ Dosezy Architecture & F-Droid Compliance Plan

> **Document Status:** Draft / Planned Roadmap  
> **Target Version:** v2.3.0+  
> **Domain Alignment:** `dosezy.app`, `api.dosezy.app`, `patient.dosezy.app`, `caregiver.dosezy.app`, `status.dosezy.app`, `docs.dosezy.app`

---

## 1. 🛡️ F-Droid Compliance & Anti-Feature Management

To preserve Dosezy's zero-anti-feature standing on F-Droid (`Pass` on all network connection, tracking, `NonFreeNet`, and `TetheredNet` checks), the app strictly adheres to local-first principles:

### A. Flavor-Based Build System (`build.gradle.kts`)
- **`fdroid` Flavor:**
  - Zero background network pings or auto-updater engines compiled in (`BuildConfig.ENABLE_AUTO_UPDATE = false`).
  - `INTERNET` permission disabled by default or restricted to explicit user opt-in sync.
  - Check for updates button opens the native F-Droid app client page or web fallback.
- **`github` / `play` Flavor:**
  - Includes direct GitHub Release update checker and optional Play Store In-App Updates API.

### B. Core Offline Rules
1. **Zero Startup Network Requests:** Cold starts and navigation execute zero network calls.
2. **External System Browser for All Links:** FAQs, manuals, support pages, and missing store fallbacks open via system browser intents (`Intent.ACTION_VIEW`), keeping zero WebViews inside the application binary.

---

## 2. 📱 Updated Menu Screen Layout

The **Menu Screen** is structured with distinct **Network** and **Data** sections:

```
┌─────────────────────────────────────────────────────────────┐
│ 🌐 NETWORK                                                  │
├─────────────────────────────────────────────────────────────┤
│ • Dosezy Cloud Configuration               [ Optional ]     │
│   (Coming Soon)                                             │
│                                                             │
│ • Caregiver Sharing                                         │
│   (Coming Soon)                                             │
│                                                             │
│ • Check for Updates                                         │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 📄 DATA                                                     │
├─────────────────────────────────────────────────────────────┤
│ • Export Data (PDF Report, JSON Backup, CSV)                │
│ • Backup & Restore                                          │
│   (Coming Soon)                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 🔍 Multi-Store Detection & Missing Store Fallback Logic

Detect where the user installed Dosezy across all Android app stores (Google Play, F-Droid, Huawei AppGallery, Samsung Galaxy Store, Xiaomi GetApps, Amazon Appstore, Aurora Store, and manual Sideload/GitHub):

```kotlin
data class InstallSourceDetails(
    val sourceName: String,
    val installerPackage: String?,
    val storeUri: String,
    val webFallbackUrl: String
)

fun detectInstallSource(context: Context): InstallSourceDetails {
    val pm = context.packageManager
    val packageName = context.packageName

    val installerPackage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            pm.getInstallSourceInfo(packageName).installingPackageName
        } catch (e: Exception) {
            null
        }
    } else {
        @Suppress("DEPRECATION")
        pm.getInstallerPackageName(packageName)
    }

    return when (installerPackage) {
        "com.android.vending" -> InstallSourceDetails(
            sourceName = "Google Play Store",
            installerPackage = installerPackage,
            storeUri = "market://details?id=$packageName",
            webFallbackUrl = "https://play.google.com/store/apps/details?id=$packageName"
        )
        "org.fdroid.fdroid", "org.fdroid.fdroid.privileged" -> InstallSourceDetails(
            sourceName = "F-Droid",
            installerPackage = installerPackage,
            storeUri = "fdroid.app://details?id=$packageName",
            webFallbackUrl = "https://fdroid.org/en/packages/$packageName"
        )
        "com.huawei.appmarket" -> InstallSourceDetails(
            sourceName = "Huawei AppGallery",
            installerPackage = installerPackage,
            storeUri = "appmarket://details?id=$packageName",
            webFallbackUrl = "https://appgallery.huawei.com/app/C$packageName"
        )
        "com.sec.android.app.samsungapps" -> InstallSourceDetails(
            sourceName = "Samsung Galaxy Store",
            installerPackage = installerPackage,
            storeUri = "samsungapps://ProductDetail/$packageName",
            webFallbackUrl = "https://galaxystore.samsung.com/detail/$packageName"
        )
        "com.xiaomi.mipick" -> InstallSourceDetails(
            sourceName = "Xiaomi GetApps",
            installerPackage = installerPackage,
            storeUri = "mimarket://details?id=$packageName",
            webFallbackUrl = "https://global.app.mi.com/details?id=$packageName"
        )
        "com.amazon.venezia" -> InstallSourceDetails(
            sourceName = "Amazon Appstore",
            installerPackage = installerPackage,
            storeUri = "amzn://apps/android?p=$packageName",
            webFallbackUrl = "https://www.amazon.com/gp/mas/dl/android?p=$packageName"
        )
        "com.aurora.store" -> InstallSourceDetails(
            sourceName = "Aurora Store",
            installerPackage = installerPackage,
            storeUri = "market://details?id=$packageName",
            webFallbackUrl = "https://aurorastore.org"
        )
        else -> InstallSourceDetails(
            sourceName = "GitHub Release / Direct APK Sideload",
            installerPackage = installerPackage,
            storeUri = "https://github.com/saad2134/dosezy/releases/latest",
            webFallbackUrl = "https://github.com/saad2134/dosezy/releases/latest"
        )
    }
}
```

### Missing Store Fallback Mechanism
When the user taps "Check for Updates" or attempts to view store details:
1. Try launching native store app client via `Intent(ACTION_VIEW, Uri.parse(storeDetails.storeUri))`.
2. If `ActivityNotFoundException` is caught (native store app is uninstalled, disabled, or missing), **fall back smoothly** to opening `storeDetails.webFallbackUrl` in the system default web browser!

```kotlin
fun launchStoreOrFallback(context: Context, sourceDetails: InstallSourceDetails) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sourceDetails.storeUri)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Native store app is uninstalled or missing — fallback to web store URL in default browser
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(sourceDetails.webFallbackUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    }
}
```

---

## 4. 🔄 Update Checker Strategy Matrix

| Detected Store | Native Intent | Missing Store Fallback |
| :--- | :--- | :--- |
| **Google Play** | `market://details?id=...` | `https://play.google.com/store/apps/details?id=...` |
| **F-Droid** | `fdroid.app://details?id=...` | `https://fdroid.org/en/packages/...` |
| **Huawei AppGallery** | `appmarket://details?id=...` | `https://appgallery.huawei.com/app/...` |
| **Samsung Galaxy Store** | `samsungapps://ProductDetail/...` | `https://galaxystore.samsung.com/detail/...` |
| **Xiaomi GetApps** | `mimarket://details?id=...` | `https://global.app.mi.com/details?id=...` |
| **Amazon Appstore** | `amzn://apps/android?p=...` | `https://www.amazon.com/gp/mas/dl/android?p=...` |
| **GitHub Sideload** | GitHub Releases API query or URL | `https://github.com/saad2134/dosezy/releases/latest` |
