/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.data.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.dosezy.R
import com.example.dosezy.data.model.MedicationStatus
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.User
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AdherenceSummary(
    val totalCount: Int,
    val takenOnTimeCount: Int,
    val takenLateCount: Int,
    val skippedCount: Int,
    val missedCount: Int,
    val pendingCount: Int,
    val adherenceRate: Double,
    val rateFormatted: String,
    val breakdownText: String
)

class PdfReportGenerator(private val context: Context) {

    internal fun isEntryMissed(
        entry: ScheduleEntry,
        evalNow: LocalDateTime,
        missedAfterHours: Int
    ): Boolean =
        entry.status == MedicationStatus.MISSED ||
        (entry.status == MedicationStatus.PENDING && evalNow.isAfter(entry.scheduledDateTime) &&
            Duration.between(entry.scheduledDateTime, evalNow).toHours() >= missedAfterHours.toLong())

    internal fun calculateAdherence(
        schedules: List<ScheduleEntry>,
        evalNow: LocalDateTime = LocalDateTime.now(),
        evalToday: LocalDate = LocalDate.now(),
        missedAfterHours: Int
    ): AdherenceSummary {
        // Guard: Exclude PRN ad-hoc doses and future entries from adherence calculation to match in-app analytics
        val adherenceEntries = schedules.filter {
            !it.entryId.startsWith("PRN_") && it.scheduledDateTime.toLocalDate() <= evalToday
        }

        val totalCount = adherenceEntries.size
        val takenOnTimeCount = adherenceEntries.count { it.status == MedicationStatus.TAKEN_ON_TIME }
        val takenLateCount = adherenceEntries.count { it.status == MedicationStatus.TAKEN_LATE }
        val skippedCount = adherenceEntries.count { it.status == MedicationStatus.SKIPPED }
        val missedCount = adherenceEntries.count { isEntryMissed(it, evalNow, missedAfterHours) }
        val pendingCount = adherenceEntries.count { it.status == MedicationStatus.PENDING && !isEntryMissed(it, evalNow, missedAfterHours) }

        val takenTotal = takenOnTimeCount + takenLateCount
        val decidedCount = takenTotal + missedCount
        val adherenceRate = if (decidedCount > 0) (takenTotal.toDouble() / decidedCount * 100.0) else 0.0
        val rateFormatted = String.format(Locale.US, "%.1f%%", adherenceRate)
        val breakdownText = "On-Time: $takenOnTimeCount   |   Late: $takenLateCount   |   Skipped: $skippedCount   |   Missed: $missedCount   |   Pending: $pendingCount   |   Total: $totalCount"

        return AdherenceSummary(
            totalCount = totalCount,
            takenOnTimeCount = takenOnTimeCount,
            takenLateCount = takenLateCount,
            skippedCount = skippedCount,
            missedCount = missedCount,
            pendingCount = pendingCount,
            adherenceRate = adherenceRate,
            rateFormatted = rateFormatted,
            breakdownText = breakdownText
        )
    }

    internal fun formatHistoryHeader(totalCount: Int): String {
        return if (totalCount > 150) {
            "Recent Dose History (Latest 150 of $totalCount entries)"
        } else {
            "Recent Dose History ($totalCount entries)"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun buildPdfDocument(user: User, medicines: List<Medicine>, schedules: List<ScheduleEntry>): PdfDocument {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1193D4")
            textSize = 18f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B5563")
            textSize = 10f
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F2937")
            textSize = 12f
            isFakeBoldText = true
        }
        val alertHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DC2626")
            textSize = 10f
            isFakeBoldText = true
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#374151")
            textSize = 10f
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 1f
        }

        fun drawFooter(c: android.graphics.Canvas, pNum: Int) {
            c.drawLine(40f, 800f, 555f, 800f, linePaint)
            c.drawText("Dosezy Medicine Tracker • Confidential Patient Record", 40f, 816f, subtitlePaint)
            c.drawText("Page $pNum", 520f, 816f, subtitlePaint)
        }

        var y = 38f

        fun checkPageBreak(neededHeight: Float) {
            if (y + neededHeight > 780f) {
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
                canvas.drawText("Dosezy Report - ${user.fullName}", 40f, y, subtitlePaint)
                y += 12f
                canvas.drawLine(40f, y, 555f, y, linePaint)
                y += 20f
            }
        }

        // Top-left App Brand Icon (matching New User & Alarm popup brand icon)
        val brandIcon = ContextCompat.getDrawable(context, R.drawable.loader_icon)
        if (brandIcon != null) {
            brandIcon.setBounds(40, 24, 68, 52) // 28x28 pt at top-left
            brandIcon.draw(canvas)
        }

        canvas.drawText("DOSEZY - Patient Health & Medication Report", 76f, y, titlePaint)
        y += 16f
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        canvas.drawText("Generated: $timestamp | Private & Offline Export", 76f, y, subtitlePaint)
        y += 15f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 22f

        // Patient Info
        canvas.drawText("Patient Profile", 40f, y, headerPaint)
        y += 15f
        canvas.drawText("Name: ${user.fullName}", 40f, y, textPaint)
        canvas.drawText("Age: ${user.age}", 240f, y, textPaint)
        // Guard: Format gender with displayName to prevent drawing raw uppercase enum identifier (e.g. DO_NOT_SPECIFY) in patient medical report
        canvas.drawText("Gender: ${user.gender.displayName}", 360f, y, textPaint)
        y += 15f

        // Allergies & Medical Conditions in PDF
        val allergiesText = if (!user.allergies.isNullOrBlank()) user.allergies else "None recorded"
        val conditionsText = if (!user.medicalConditions.isNullOrBlank()) user.medicalConditions else "None recorded"

        if (!user.allergies.isNullOrBlank()) {
            canvas.drawText("Allergies: $allergiesText", 40f, y, alertHeaderPaint)
        } else {
            canvas.drawText("Allergies: $allergiesText", 40f, y, textPaint)
        }
        y += 15f
        canvas.drawText("Chronic Conditions: $conditionsText", 40f, y, textPaint)
        y += 18f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        // Analytics & Adherence Summary Section (Calculated from past decided doses)
        val evalNow = LocalDateTime.now()
        val evalToday = LocalDate.now()
        val missedAfterHours = user.considerMissedAfter

        val adherence = calculateAdherence(schedules, evalNow, evalToday, missedAfterHours)

        canvas.drawText("Adherence & Analytics Summary", 40f, y, headerPaint)
        y += 16f

        val cardRect = RectF(40f, y, 555f, y + 46f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F3F4F6")
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, cardPaint)

        val rateColor = if (adherence.adherenceRate >= 90.0) Color.parseColor("#10B981")
                        else if (adherence.adherenceRate >= 70.0) Color.parseColor("#1193D4")
                        else Color.parseColor("#EF4444")

        val ratePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = rateColor
            textSize = 15f
            isFakeBoldText = true
        }
        canvas.drawText("Overall Adherence: ${adherence.rateFormatted}", 52f, y + 20f, ratePaint)

        val cardSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B5563")
            textSize = 9.5f
        }
        canvas.drawText(adherence.breakdownText, 52f, y + 36f, cardSubPaint)

        y += 56f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        // Active Medications Table
        checkPageBreak(50f)
        canvas.drawText("Active Medications (${medicines.size})", 40f, y, headerPaint)
        y += 18f

        subtitlePaint.isFakeBoldText = true
        canvas.drawText("Medication", 40f, y, subtitlePaint)
        canvas.drawText("Dosage", 220f, y, subtitlePaint)
        canvas.drawText("Frequency", 360f, y, subtitlePaint)
        y += 12f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 16f

        subtitlePaint.isFakeBoldText = false
        medicines.forEach { med ->
            checkPageBreak(20f)
            canvas.drawText(med.medicationName, 40f, y, textPaint)
            canvas.drawText("${med.dosage} ${med.dosageUnit}", 220f, y, textPaint)
            val freqLabel = med.getFrequencyDisplay()
            canvas.drawText(freqLabel, 360f, y, textPaint)
            y += 16f
        }
        if (medicines.isEmpty()) {
            canvas.drawText("No active medications recorded.", 40f, y, textPaint)
            y += 16f
        }

        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        // Schedule Logs Table
        checkPageBreak(50f)
        val sortedSchedules = schedules.sortedByDescending { it.scheduledDateTime }
        val medMap = medicines.associateBy { it.medicineId }
        // Guard: Explicitly clarify entry count in dose history header when total schedules exceed the 150-item PDF page limit
        val historyHeaderText = formatHistoryHeader(sortedSchedules.size)
        canvas.drawText(historyHeaderText, 40f, y, headerPaint)
        y += 18f

        subtitlePaint.isFakeBoldText = true
        canvas.drawText("Scheduled Date/Time", 40f, y, subtitlePaint)
        canvas.drawText("Medication", 155f, y, subtitlePaint)
        canvas.drawText("Status", 340f, y, subtitlePaint)
        canvas.drawText("Recorded Time", 440f, y, subtitlePaint)
        y += 12f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 16f

        subtitlePaint.isFakeBoldText = false
        sortedSchedules.take(150).forEach { sch ->
            checkPageBreak(20f)
            val dateStr = sch.scheduledDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            canvas.drawText(dateStr, 40f, y, textPaint)

            val med = medMap[sch.medicineId]
            val medName = med?.medicationName ?: "Unknown"
            val doseStr = med?.let { " (${it.getDosageDisplay(sch.scheduledDateTime.toLocalTime())})" } ?: ""
            val fullMed = "$medName$doseStr"
            val truncatedMed = if (fullMed.length > 28) fullMed.take(25) + "..." else fullMed
            canvas.drawText(truncatedMed, 155f, y, textPaint)

            val statusLabel = when (sch.status) {
                MedicationStatus.TAKEN_ON_TIME -> "Taken On Time"
                MedicationStatus.TAKEN_LATE -> "Taken Late"
                MedicationStatus.MISSED -> "Missed"
                MedicationStatus.SKIPPED -> "Skipped"
                MedicationStatus.PENDING -> "Pending"
            }
            canvas.drawText(statusLabel, 340f, y, textPaint)
            val takenStr = if (sch.takenAt != null && sch.takenAt.year > 1970) {
                sch.takenAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            } else if (sch.status == MedicationStatus.TAKEN_ON_TIME || sch.status == MedicationStatus.TAKEN_LATE) {
                sch.scheduledDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            } else {
                "-"
            }
            canvas.drawText(takenStr, 440f, y, textPaint)
            y += 16f
        }
        if (schedules.isEmpty()) {
            canvas.drawText("No schedule history recorded.", 40f, y, textPaint)
            y += 16f
        }

        drawFooter(canvas, pageNumber)
        pdfDocument.finishPage(page)
        return pdfDocument
    }
}
