package com.example.dosezy.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.example.dosezy.R

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
            "org.fdroid.fdroid", "org.fdroid.fdroid.privileged", "com.looker.fdroid" -> InstallSourceDetails(
                sourceName = "F-Droid / IzzyOnDroid",
                installerPackage = installerPackage,
                storeUri = "fdroid.app://details?id=$packageName",
                webFallbackUrl = "https://fdroid.org/en/packages/$packageName"
            )
            "com.apkpure.aosp" -> InstallSourceDetails(
                sourceName = "APKPure",
                installerPackage = installerPackage,
                storeUri = "market://details?id=$packageName",
                webFallbackUrl = "https://apkpure.com/p/$packageName"
            )
            "cm.aptoide.pt" -> InstallSourceDetails(
                sourceName = "Aptoide",
                installerPackage = installerPackage,
                storeUri = "aptoideinstall://package=$packageName",
                webFallbackUrl = "https://aptoide.com/app/$packageName"
            )
            "com.uptodown" -> InstallSourceDetails(
                sourceName = "Uptodown",
                installerPackage = installerPackage,
                storeUri = "uptodown://app/$packageName",
                webFallbackUrl = "https://uptodown.com/android"
            )
            "com.apkmirror.helper.prod" -> InstallSourceDetails(
                sourceName = "APKMirror",
                installerPackage = installerPackage,
                storeUri = "https://www.apkmirror.com/uploads/?q=dosezy",
                webFallbackUrl = "https://www.apkmirror.com/uploads/?q=dosezy"
            )
            "com.phonepe.appstore" -> InstallSourceDetails(
                sourceName = "Indus Appstore",
                installerPackage = installerPackage,
                storeUri = "indus://details?id=$packageName",
                webFallbackUrl = "https://www.indusappstore.com"
            )
            "com.skt.skaf.A000Z00001" -> InstallSourceDetails(
                sourceName = "ONE Store",
                installerPackage = installerPackage,
                storeUri = "onestore://common/product/$packageName",
                webFallbackUrl = "https://m.onestore.co.kr"
            )
            "com.tencent.android.qqdownloader" -> InstallSourceDetails(
                sourceName = "Tencent MyApp (应用宝)",
                installerPackage = installerPackage,
                storeUri = "tmast://appdetails?pname=$packageName",
                webFallbackUrl = "https://sj.qq.com"
            )
            "com.bbk.appstore", "com.vivo.appstore" -> InstallSourceDetails(
                sourceName = "Vivo App Store",
                installerPackage = installerPackage,
                storeUri = "vivomarket://details?id=$packageName",
                webFallbackUrl = "https://developer.vivo.com"
            )
            "com.oppo.market", "com.heos.market" -> InstallSourceDetails(
                sourceName = "OPPO App Market",
                installerPackage = installerPackage,
                storeUri = "oppomarket://details?packagename=$packageName",
                webFallbackUrl = "https://store.oppo.com"
            )
            "com.hihonor.appmarket" -> InstallSourceDetails(
                sourceName = "HONOR App Market",
                installerPackage = installerPackage,
                storeUri = "appmarket://details?id=$packageName",
                webFallbackUrl = "https://developer.hihonor.com"
            )
            "com.mobile.indiapp" -> InstallSourceDetails(
                sourceName = "9Apps",
                installerPackage = installerPackage,
                storeUri = "market://details?id=$packageName",
                webFallbackUrl = "https://www.9apps.com"
            )
            "com.transsion.palmstor", "com.transsion.store" -> InstallSourceDetails(
                sourceName = "Palm Store (Transsion)",
                installerPackage = installerPackage,
                storeUri = "palmstore://details?id=$packageName",
                webFallbackUrl = "https://www.palmstore.net"
            )
            "com.qooapp.qoohelper" -> InstallSourceDetails(
                sourceName = "QooApp",
                installerPackage = installerPackage,
                storeUri = "qooapp://detail?id=$packageName",
                webFallbackUrl = "https://www.qoo-app.com"
            )
            "com.farsitel.bazaar" -> InstallSourceDetails(
                sourceName = "Cafebazaar",
                installerPackage = installerPackage,
                storeUri = "bazaar://details?id=$packageName",
                webFallbackUrl = "https://cafebazaar.ir/app/$packageName"
            )
            "ru.vk.store" -> InstallSourceDetails(
                sourceName = "RuStore",
                installerPackage = installerPackage,
                storeUri = "rustore://apps/details?id=$packageName",
                webFallbackUrl = "https://www.rustore.ru/catalog/app/$packageName"
            )
            "ru.store.nashstore" -> InstallSourceDetails(
                sourceName = "NashStore",
                installerPackage = installerPackage,
                storeUri = "nashstore://details?id=$packageName",
                webFallbackUrl = "https://nashstore.ru"
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
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(sourceDetails.webFallbackUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (e2: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.no_browser_found),
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.no_browser_found),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
