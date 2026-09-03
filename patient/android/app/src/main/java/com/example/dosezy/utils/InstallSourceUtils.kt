package com.example.dosezy.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

data class InstallSourceDetails(
    val sourceName: String,
    val installerPackage: String?,
    val storeUri: String,
    val webFallbackUrl: String
)

object InstallSourceUtils {

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
                webFallbackUrl = "https://appgallery.huawei.com"
            )
            "com.sec.android.app.samsungapps" -> InstallSourceDetails(
                sourceName = "Samsung Galaxy Store",
                installerPackage = installerPackage,
                storeUri = "samsungapps://ProductDetail/$packageName",
                webFallbackUrl = "https://galaxystore.samsung.com"
            )
            "com.xiaomi.mipick" -> InstallSourceDetails(
                sourceName = "Xiaomi GetApps",
                installerPackage = installerPackage,
                storeUri = "mimarket://details?id=$packageName",
                webFallbackUrl = "https://global.app.mi.com"
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
                sourceName = "GitHub Release / Direct Sideload",
                installerPackage = installerPackage,
                storeUri = "https://github.com/saad2134/dosezy/releases/latest",
                webFallbackUrl = "https://github.com/saad2134/dosezy/releases/latest"
            )
        }
    }

    fun checkForUpdates(context: Context) {
        val sourceDetails = detectInstallSource(context)
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sourceDetails.storeUri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(sourceDetails.webFallbackUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }
}
