/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Google Play and closed marketplace review prompter (#146).
 *
 * Utilizes Google Play In-App Review API when available, and provides automated
 * fallback to Samsung Galaxy Store, Amazon Appstore, or standard market intent when
 * Google Play Services or the Play Store client is absent.
 */
class ReviewPrompterImpl(private val context: Context) : ReviewPrompter {

    override fun maybePromptReview(activity: Activity, triggerReason: String) {
        // Guard: Check engagement threshold and cooldown before requesting review to avoid exceeding Google Play quota or annoying users
        if (!ReviewPreferences.isEligibleForPrompt(context)) return

        val manager = try {
            ReviewManagerFactory.create(context)
        } catch (e: Exception) {
            // Guard: Fall back to direct marketplace intent if ReviewManagerFactory initialization fails on non-GMS devices
            ReviewPreferences.recordPromptShown(context)
            launchStoreFallback(activity)
            return
        }

        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener {
                    // Guard: Google Play in-app review flow completes regardless of whether the user rated or dismissed
                    ReviewPreferences.recordPromptShown(context)
                }
            } else {
                // Guard: If In-App Review API fails (e.g. non-GMS device, Samsung Galaxy Store, or Huawei), route to appropriate marketplace
                ReviewPreferences.recordPromptShown(context)
                launchStoreFallback(activity)
            }
        }
    }

    private fun launchStoreFallback(activity: Activity) {
        val packageName = activity.packageName
        val installer = getInstallerPackage(activity)

        val uriString = when (installer) {
            "com.sec.android.app.samsungapps" -> "samsungapps://ProductDetail/$packageName"
            "com.amazon.venezia" -> "amzn://apps/android?p=$packageName"
            else -> "market://details?id=$packageName"
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val webUrl = when (installer) {
                "com.sec.android.app.samsungapps" -> "https://galaxystore.samsung.com/detail/$packageName"
                "com.amazon.venezia" -> "https://www.amazon.com/gp/mas/dl/android?p=$packageName"
                else -> "https://play.google.com/store/apps/details?id=$packageName"
            }
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Exception) {
                // Silently ignore if no web browser is available
            }
        }
    }

    private fun getInstallerPackage(context: Context): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        } catch (_: Exception) {
            null
        }
    }
}

