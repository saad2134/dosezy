package com.example.dosezy.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object SoundUtils {
    private const val TAG = "SoundUtils"

    fun getFileName(context: Context, uri: Uri): String {
        var name = "Custom Sound"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val displayName = cursor.getString(nameIndex)
                    if (!displayName.isNullOrBlank()) {
                        name = displayName
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file name from uri", e)
        }
        return name
    }

    fun saveCustomAudio(context: Context, uri: Uri): Pair<File?, String> {
        val fileName = getFileName(context, uri)
        return try {
            val customSoundsDir = File(context.filesDir, "custom_sounds").apply { mkdirs() }
            val extension = if (fileName.contains('.')) fileName.substringAfterLast('.') else "mp3"
            val targetFile = File(customSoundsDir, "custom_alarm_${System.currentTimeMillis()}.$extension")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            Pair(targetFile, fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy custom audio file to app storage", e)
            Pair(null, fileName)
        }
    }
}
