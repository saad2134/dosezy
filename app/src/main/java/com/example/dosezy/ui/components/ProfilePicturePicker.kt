// ui/components/ProfilePicturePicker.kt
package com.example.dosezy.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dosezy.R
import com.example.dosezy.utils.rememberImagePicker
import java.io.File

@Composable
fun ProfilePicturePicker(
    profilePicPath: String?,
    onProfilePictureSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionMessage by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val imagePicker = rememberImagePicker(context) { uri ->
        if (uri != null) {
            val savedPath = saveImageToInternalStorage(context, uri)
            onProfilePictureSelected(savedPath)
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                pendingAction?.invoke()
            } else {
                permissionMessage = "Camera permission is required to take photos. Please enable it in app settings."
                showPermissionDialog = true
            }
            pendingAction = null
        }
    )

    // Storage Permission Launcher (for Android 12 and below, for Android 13+ use READ_MEDIA_IMAGES)
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                pendingAction?.invoke()
            } else {
                permissionMessage = "Storage permission is required to access photos. Please enable it in app settings."
                showPermissionDialog = true
            }
            pendingAction = null
        }
    )

    // Check and request permissions
    @Composable
    fun checkCameraPermission(action: @Composable () -> Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            action()
        } else {
            pendingAction = action as (() -> Unit)?
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    @Composable
    fun checkStoragePermission(action: @Composable () -> Unit) {
        // For Android 13+, use READ_MEDIA_IMAGES instead of READ_EXTERNAL_STORAGE
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            action()
        } else {
            pendingAction = action as (() -> Unit)?
            storagePermissionLauncher.launch(permission)
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
                    .clip(CircleShape),
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
                checkCameraPermission {
                    val tempUri = imagePicker.prepareCameraIntent()
                    imagePicker.getCameraLauncher().launch(tempUri)
                }
            },
            onGallerySelected = {
                showImageSourceDialog = false
                checkStoragePermission {
                    imagePicker.getGalleryLauncher().launch("image/*")
                }
            }
        )
    }

    // Permission Dialog
    if (showPermissionDialog) {
        PermissionAlertDialog(
            message = permissionMessage,
            onDismiss = { showPermissionDialog = false }
        )
    }
}

@Composable
fun ImageSourceDialog(
    onDismiss: () -> Unit,
    onCameraSelected: @Composable () -> Unit,
    onGallerySelected: @Composable () -> Unit
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
            TextButton(onClick = onGallerySelected as () -> Unit) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text("Gallery", modifier = Modifier.padding(start = 8.dp))
            }
        },
        dismissButton = {
            TextButton(onClick = onCameraSelected as () -> Unit) {
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

@Composable
fun PermissionAlertDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permission Required") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

// Utility function to save image to internal storage
private fun saveImageToInternalStorage(context: Context, uri: Uri): String {
    val inputStream = context.contentResolver.openInputStream(uri)
    val timeStamp = System.currentTimeMillis()
    val file = File(context.filesDir, "profile_$timeStamp.jpg")

    inputStream?.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }

    return file.absolutePath
}