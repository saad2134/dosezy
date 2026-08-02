// ui/components/ProfilePicturePicker.kt
package com.example.dosezy.ui.components

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dosezy.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfilePicturePicker(
    profilePicPath: String?,
    onProfilePictureSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    val savedPath = saveImageToInternalStorage(context, uri, profilePicPath)
                    onProfilePictureSelected(savedPath)
                } catch (e: Exception) {
                    Log.e("ProfilePicturePicker", "Gallery error: ${e.message}", e)
                    errorMessage = "Failed to load image from gallery"
                    showError = true
                }
            }
        }
    )

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                tempUri?.let { uri ->
                    try {
                        val savedPath = saveImageToInternalStorage(context, uri, profilePicPath)
                        onProfilePictureSelected(savedPath)
                    } catch (e: Exception) {
                        Log.e("ProfilePicturePicker", "Camera save error: ${e.message}", e)
                        errorMessage = "Failed to save captured image"
                        showError = true
                    }
                }
            } else {
                Log.e("ProfilePicturePicker", "Camera capture failed")
                errorMessage = "Camera capture failed"
                showError = true
            }
            tempUri = null
        }
    )

    // Camera open action (lambda)
    val openCamera = {
        try {
            val uri = createImageFileUri(context)
            if (uri != null) {
                tempUri = uri
                cameraLauncher.launch(uri)
            } else {
                errorMessage = "Cannot create temporary file for camera"
                showError = true
            }
        } catch (e: Exception) {
            Log.e("ProfilePicturePicker", "Camera open error: ${e.message}", e)
            errorMessage = "Cannot open camera: ${e.message}"
            showError = true
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Permission granted, open camera
                openCamera()
            } else {
                // Permission denied
                errorMessage = "Camera permission is required to take photos"
                showError = true
            }
        }
    )

    fun openCameraWithPermissionCheck() {
        try {
            val permission = android.Manifest.permission.CAMERA
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                openCamera()
            } else {
                // Request camera permission
                cameraPermissionLauncher.launch(permission)
            }
        } catch (e: Exception) {
            Log.e("ProfilePicturePicker", "Camera permission check error: ${e.message}", e)
            errorMessage = "Cannot access camera: ${e.message}"
            showError = true
        }
    }

    fun openGallery() {
        try {
            galleryLauncher.launch("image/*")
        } catch (e: Exception) {
            Log.e("ProfilePicturePicker", "Gallery open error: ${e.message}", e)
            errorMessage = "Cannot open gallery"
            showError = true
        }
    }

    Box(
        modifier = modifier
            .size(128.dp)
            .clickable { showImageSourceDialog = true },
        contentAlignment = Alignment.Center
    ) {
        // Profile Image or Placeholder
        if (!profilePicPath.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(profilePicPath))
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(128.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
                placeholder = painterResource(id = R.drawable.default_profile),
                error = painterResource(id = R.drawable.default_profile),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.size(128.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Profile Picture",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Camera Icon Overlay
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(40.dp)
                .clickable { showImageSourceDialog = true }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Change Profile Picture",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Image Source Selection Dialog
    if (showImageSourceDialog) {
        ImageSourceDialog(
            onDismiss = { showImageSourceDialog = false },
            onCameraSelected = {
                showImageSourceDialog = false
                openCameraWithPermissionCheck()
            },
            onGallerySelected = {
                showImageSourceDialog = false
                openGallery()
            }
        )
    }

    // Error Dialog
    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text("OK")
                }
            }
        )
    }
}



@Composable
fun ImageSourceDialog(
    onDismiss: () -> Unit,
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Image Source") },
        text = {
            Column {
                Text("Choose how you want to add your profile picture")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Camera: Take a new photo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Gallery: Choose from your photos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onGallerySelected) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text("Gallery", modifier = Modifier.padding(start = 8.dp))
            }
        },
        dismissButton = {
            TextButton(onClick = onCameraSelected) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text("Camera", modifier = Modifier.padding(start = 8.dp))
            }
        }
    )
}

// Utility function to create a file URI for camera
private fun createImageFileUri(context: Context): Uri? {
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
            Log.d("ProfilePicturePicker", "Created temp file: $uri")
        }
    } catch (e: Exception) {
        Log.e("ProfilePicturePicker", "Error creating temp file: ${e.message}", e)
        null
    }
}

// Utility function to save image to internal storage
private fun saveImageToInternalStorage(context: Context, uri: Uri, oldPath: String? = null): String {
    // Delete old profile picture if it exists to prevent storage leak
    if (!oldPath.isNullOrEmpty()) {
        try {
            val oldFile = File(oldPath)
            if (oldFile.exists() && oldFile.parentFile?.absolutePath == context.filesDir.absolutePath) {
                val deleted = oldFile.delete()
                Log.d("ProfilePicturePicker", "Deleted old image: $oldPath, success=$deleted")
            }
        } catch (e: Exception) {
            Log.e("ProfilePicturePicker", "Failed to delete old image: ${e.message}")
        }
    }

    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open input stream")

        val timeStamp = System.currentTimeMillis()
        val file = File(context.filesDir, "profile_$timeStamp.jpg")

        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        Log.d("ProfilePicturePicker", "Image saved to: ${file.absolutePath}")
        file.absolutePath
    } catch (e: Exception) {
        Log.e("ProfilePicturePicker", "Error saving image: ${e.message}", e)
        throw e
    }
}