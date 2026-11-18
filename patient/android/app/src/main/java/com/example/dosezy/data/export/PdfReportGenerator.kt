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
import com.example.dosezy.data.model.DosageUnit
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
            com.example.dosezy.utils.TimeCalculationUtils.isMissed(entry.scheduledDateTime, evalNow, missedAfterHours))

    // Guard: Evaluate effective status so past overdue pending doses render as Missed matching Adherence Summary (#BUG-PDF-OVERDUE-STATUS)
    internal fun resolveDisplayStatus(
        entry: ScheduleEntry,
        evalNow: LocalDateTime,
        missedAfterHours: Int
    ): MedicationStatus {
        return if (entry.status == MedicationStatus.PENDING && isEntryMissed(entry, evalNow, missedAfterHours)) {
            MedicationStatus.MISSED
        } else {
            entry.status
        }
    }

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
        // Guard: Factor overdue pending doses on or before today into decidedCount matching AnalyticsViewModel so overdue unlogged doses accurately decrease compliance (#BUG-PDF-OVERDUE-ADHERENCE)
        val overduePendingCount = adherenceEntries.count {
            it.status == MedicationStatus.PENDING &&
            it.scheduledDateTime.isBefore(evalNow) &&
            !isEntryMissed(it, evalNow, missedAfterHours)
        }
        val decidedCount = takenTotal + missedCount + overduePendingCount
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

    // Guard: Separate active vs discontinued/archived medications so doctors reading clinical reports are not misled into believing stopped drugs are currently active
    internal fun partitionMedicines(medicines: List<Medicine>): Pair<List<Medicine>, List<Medicine>> =
        medicines.partition { !it.isArchived }

    internal fun formatActiveMedicationsHeader(count: Int): String = "Active Medications ($count)"

    internal fun formatDiscontinuedMedicationsHeader(count: Int): String = "Discontinued Medications ($count)"

    // Guard: Support multiline text (newlines), CJK unspaced text, and unbroken long tokens via breakText to prevent canvas overflow (#BUG-PDF-WRAP-CJK)
    internal fun wrapTextToLines(prefix: String, fullText: String, paint: Paint, maxWidth: Float): List<String> {
        val totalText = "$prefix$fullText"
        val rawLines = totalText.split(Regex("\r?\n"))
        val result = mutableListOf<String>()

        for (rawLine in rawLines) {
            if (rawLine.isEmpty()) {
                result.add("")
                continue
            }
            if (paint.measureText(rawLine) <= maxWidth) {
                result.add(rawLine)
                continue
            }
            val words = rawLine.split(" ")
            var currentLine = ""
            for (w in words) {
                if (paint.measureText(w) > maxWidth) {
                    var remaining = w
                    if (currentLine.isNotEmpty()) {
                        val spacePrefix = "$currentLine "
                        var fitCount = 0
                        for (i in 1..remaining.length) {
                            if (paint.measureText(spacePrefix + remaining.substring(0, i)) <= maxWidth) {
                                fitCount = i
                            } else {
                                break
                            }
                        }
                        if (fitCount > 0) {
                            result.add(spacePrefix + remaining.substring(0, fitCount))
                            remaining = remaining.substring(fitCount)
                            currentLine = ""
                        } else {
                            result.add(currentLine)
                            currentLine = ""
                        }
                    }
                    while (remaining.isNotEmpty()) {
                        var count = paint.breakText(remaining, true, maxWidth, null)
                        if (count <= 0 || paint.measureText(remaining.substring(0, count)) > maxWidth) {
                            var fitLen = 0
                            for (i in 1..remaining.length) {
                                if (paint.measureText(remaining.substring(0, i)) <= maxWidth) {
                                    fitLen = i
                                } else {
                                    break
                                }
                            }
                            count = fitLen.coerceAtLeast(1)
                        }
                        result.add(remaining.substring(0, count))
                        remaining = remaining.substring(count)
                    }
                } else {
                    val candidate = if (currentLine.isEmpty()) w else "$currentLine $w"
                    if (paint.measureText(candidate) <= maxWidth) {
                        currentLine = candidate
                    } else {
                        if (currentLine.isNotEmpty()) {
                            result.add(currentLine)
                        }
                        currentLine = w
                    }
                }
            }
            if (currentLine.isNotEmpty()) {
                result.add(currentLine)
            }
        }
        return result
    }

    internal fun truncateTextToWidth(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated...") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return "$truncated..."
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
            color = Color.parseColor("#111827")
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
            // Guard: Non-clinical self-reported log disclaimer to prevent SaMD/MDR misclassification (#FDA, #MDR)
            val footerText = context.getString(R.string.pdf_report_disclaimer_footer)
            c.drawText(footerText, 40f, 816f, subtitlePaint)
            c.drawText("Page $pNum", 520f, 816f, subtitlePaint)
        }

        var y = 38f

        // Guard: Support re-rendering table headers on page breaks to maintain column labels on multi-page reports (#BUG-PDF-MULTI-PAGE-HEADERS)
        fun checkPageBreak(neededHeight: Float, onNewPage: (() -> Unit)? = null) {
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
                onNewPage?.invoke()
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

        fun drawWrappedLine(prefix: String, fullText: String, paint: Paint, maxWidth: Float, lineAdvance: Float) {
            val lines = wrapTextToLines(prefix, fullText, paint, maxWidth)
            lines.forEach { line ->
                checkPageBreak(lineAdvance)
                canvas.drawText(line, 40f, y, paint)
                y += lineAdvance
            }
        }

        val allergyPaint = if (!user.allergies.isNullOrBlank()) alertHeaderPaint else textPaint
        // Guard: Wrap lengthy patient allergy records and chronic conditions across lines to prevent off-page clipping
        drawWrappedLine("Allergies: ", allergiesText, allergyPaint, 515f, 15f)
        drawWrappedLine("Chronic Conditions: ", conditionsText, textPaint, 515f, 15f)
        y += 3f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        // Analytics & Adherence Summary Section (Calculated from past decided doses)
        val evalNow = LocalDateTime.now()
        val evalToday = LocalDate.now()
        val missedAfterHours = user.missedAfterMinutes

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

        // Active & Discontinued Medications Tables
        // Guard: Separate active vs discontinued/archived medications so doctors reading clinical reports are not misled into believing stopped drugs are currently active
        val (activeMeds, archivedMeds) = partitionMedicines(medicines)

        checkPageBreak(50f)
        canvas.drawText(formatActiveMedicationsHeader(activeMeds.size), 40f, y, headerPaint)
        y += 18f

        subtitlePaint.isFakeBoldText = true
        canvas.drawText("Medication", 40f, y, subtitlePaint)
        canvas.drawText("Dosage", 220f, y, subtitlePaint)
        canvas.drawText("Frequency", 360f, y, subtitlePaint)
        y += 12f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 16f

        subtitlePaint.isFakeBoldText = false
        activeMeds.forEach { med ->
            checkPageBreak(20f)
            // Guard: Truncate lengthy medication names to prevent overlapping adjacent Dosage column at 220f
            val displayMed = truncateTextToWidth(med.medicationName, textPaint, 170f)
            canvas.drawText(displayMed, 40f, y, textPaint)
            // Guard: Render standard pharmaceutical dosage format rather than raw decimal and uppercase enum name (#BUG-05)
            canvas.drawText(med.getDosageDisplay(), 220f, y, textPaint)
            val freqLabel = med.getFrequencyDisplay()
            canvas.drawText(freqLabel, 360f, y, textPaint)
            y += 16f
        }
        if (activeMeds.isEmpty()) {
            canvas.drawText("No active medications recorded.", 40f, y, textPaint)
            y += 16f
        }

        if (archivedMeds.isNotEmpty()) {
            y += 10f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 20f

            checkPageBreak(50f)
            canvas.drawText(formatDiscontinuedMedicationsHeader(archivedMeds.size), 40f, y, headerPaint)
            y += 18f

            subtitlePaint.isFakeBoldText = true
            canvas.drawText("Medication", 40f, y, subtitlePaint)
            canvas.drawText("Dosage", 220f, y, subtitlePaint)
            canvas.drawText("Status", 360f, y, subtitlePaint)
            y += 12f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 16f

            subtitlePaint.isFakeBoldText = false
            archivedMeds.forEach { med ->
                checkPageBreak(20f)
                // Guard: Truncate lengthy medication names to prevent overlapping adjacent Dosage column at 220f
                val displayMed = truncateTextToWidth(med.medicationName, textPaint, 170f)
                canvas.drawText(displayMed, 40f, y, textPaint)
                // Guard: Render standard pharmaceutical dosage format rather than raw decimal and uppercase enum name (#BUG-05)
                canvas.drawText(med.getDosageDisplay(), 220f, y, textPaint)
                canvas.drawText("Discontinued", 360f, y, textPaint)
                y += 16f
            }
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
        val drawScheduleHeaders: () -> Unit = {
            subtitlePaint.isFakeBoldText = true
            canvas.drawText("Scheduled Date/Time", 40f, y, subtitlePaint)
            canvas.drawText("Medication", 155f, y, subtitlePaint)
            canvas.drawText("Status", 340f, y, subtitlePaint)
            canvas.drawText("Recorded Time", 440f, y, subtitlePaint)
            y += 12f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 16f
            subtitlePaint.isFakeBoldText = false
        }

        sortedSchedules.take(150).forEach { sch ->
            // Guard: Re-draw schedule table column headers on subsequent pages so multi-page clinical reports retain column definitions (#BUG-PDF-MULTI-PAGE-HEADERS)
            checkPageBreak(20f, onNewPage = drawScheduleHeaders)
            val dateStr = sch.scheduledDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            canvas.drawText(dateStr, 40f, y, textPaint)

            val med = medMap[sch.medicineId]
            val medName = med?.medicationName ?: "Unknown"
            // Guard: Route through Medicine.getDosageDisplay so schedule overrides format with standard vulgar fractions and clinical units (#BUG-PDF-FRACTIONAL-DOSAGE)
            val doseStr = med?.let { " (${it.getDosageDisplay(sch.scheduledDateTime.toLocalTime(), sch.dosage)})" } ?: ""
            val fullMed = "$medName$doseStr"
            // Guard: Truncate medication name and dosage using exact canvas pixel width to prevent overlapping Status column at 340f
            val truncatedMed = truncateTextToWidth(fullMed, textPaint, 175f)
            canvas.drawText(truncatedMed, 155f, y, textPaint)

            // Guard: Evaluate resolveDisplayStatus so past overdue pending doses render as Missed matching Adherence Summary (#BUG-PDF-OVERDUE-STATUS)
            val effectiveStatus = resolveDisplayStatus(sch, evalNow, missedAfterHours)
            val statusLabel = when (effectiveStatus) {
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
