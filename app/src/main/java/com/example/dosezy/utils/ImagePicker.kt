// utils/ImagePicker.kt
package com.example.dosezy.utils

import android.content.Context
import android.net.Uri
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

    @Composable
    fun getCameraLauncher() = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                tempFileUri?.let { onImageSelected(it) }
            }
        }
    )

    @Composable
    fun getGalleryLauncher() = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            onImageSelected(uri)
        }
    )

    fun createTempFile(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.externalCacheDir ?: context.cacheDir
        val tempFile = File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            tempFile
        )
    }

    fun prepareCameraIntent(): Uri {
        tempFileUri = createTempFile()
        return tempFileUri!!
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