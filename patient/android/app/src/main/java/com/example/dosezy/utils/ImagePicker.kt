// utils/ImagePicker.kt
package com.example.dosezy.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImagePicker(
    private val context: Context,
    private val onImageSelected: (Uri?) -> Unit
) {
    private var tempFileUri: Uri? = null
    private val tag = "ImagePicker"

    @Composable
    fun getCameraLauncher() = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            Log.d(tag, "Camera result: $success")
            if (success) {
                tempFileUri?.let {
                    onImageSelected(it)
                    Log.d(tag, "Image captured: $it")
                }
            } else {
                onImageSelected(null)
                Log.e(tag, "Camera capture failed")
            }
        }
    )

    @Composable
    fun getGalleryLauncher() = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            Log.d(tag, "Gallery result: $uri")
            onImageSelected(uri)
        }
    )

    fun createTempFile(): Uri? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = context.externalCacheDir ?: context.cacheDir

            // Ensure directory exists
            storageDir?.mkdirs()

            val tempFile = File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            ).apply {
                createNewFile()
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                tempFile
            ).also { uri ->
                Log.d(tag, "Created temp file: $uri")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error creating temp file: ${e.message}")
            null
        }
    }

    fun prepareCameraIntent(): Uri? {
        tempFileUri = createTempFile()
        return tempFileUri.also {
            Log.d(tag, "Prepared camera intent with URI: $it")
        }
    }
}

@Composable
fun rememberImagePicker(
    context: Context,
    onImageSelected: (Uri?) -> Unit
): ImagePicker {
    return remember {
        ImagePicker(context, onImageSelected)
    }
}