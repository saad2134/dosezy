/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dosezy.R
import com.example.dosezy.data.export.BackupRestoreManager
import com.example.dosezy.data.export.ProfileImportDecision
import com.example.dosezy.data.export.ZipInspectionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class BackupRestoreUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val statusMessage: String? = null,
    val isStatusSuccess: Boolean = false,
    val inspectionResult: ZipInspectionResult? = null,
    val exportedBackupFile: File? = null,
    val showBackupSuccessDialog: Boolean = false
)

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRestoreManager: BackupRestoreManager
) : ViewModel() {

    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    constructor(
        context: Context,
        backupRestoreManager: BackupRestoreManager,
        ioDispatcher: CoroutineDispatcher
    ) : this(context, backupRestoreManager) {
        this.ioDispatcher = ioDispatcher
    }

    private val _uiState = MutableStateFlow(BackupRestoreUiState())
    val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

    fun inspectBackupZip(uri: Uri) {
        viewModelScope.launch {
            // Guard: Clean up any prior uncommitted inspection directory before unpacking new archive to prevent cache directory leakage
            try {
                _uiState.value.inspectionResult?.tempDir?.deleteRecursively()
            } catch (_: Exception) {}
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = context.getString(R.string.backup_status_inspecting),
                inspectionResult = null
            )
            val inspect = withContext(ioDispatcher) {
                backupRestoreManager.inspectBackupZip(uri)
            }
            if (inspect.success && inspect.profiles.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    statusMessage = null,
                    inspectionResult = inspect
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    isStatusSuccess = false,
                    statusMessage = inspect.message.ifEmpty { context.getString(R.string.backup_status_no_profiles) }
                )
            }
        }
    }

    fun createFullBackup() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = context.getString(R.string.backup_status_packaging)
            )
            try {
                val zipFile = withContext(ioDispatcher) {
                    backupRestoreManager.createFullBackupZip()
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    statusMessage = null,
                    exportedBackupFile = zipFile,
                    showBackupSuccessDialog = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    isStatusSuccess = false,
                    statusMessage = context.getString(R.string.backup_export_failed, e.localizedMessage ?: "")
                )
            }
        }
    }

    fun executeSelectiveRestore(
        tempDir: File,
        decisions: List<ProfileImportDecision>,
        onSuccess: (suspend (com.example.dosezy.data.export.BackupResult) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = context.getString(R.string.backup_status_restoring),
                inspectionResult = null,
                statusMessage = null
            )
            val result = withContext(ioDispatcher) {
                backupRestoreManager.executeSelectiveRestore(tempDir, decisions)
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                loadingMessage = null,
                isStatusSuccess = result.success,
                statusMessage = result.message
            )
            if (result.success && onSuccess != null) {
                onSuccess(result)
            }
        }
    }

    fun dismissInspection() {
        _uiState.value.inspectionResult?.tempDir?.deleteRecursively()
        _uiState.value = _uiState.value.copy(
            inspectionResult = null,
            statusMessage = null
        )
    }

    fun dismissBackupSuccessDialog() {
        _uiState.value = _uiState.value.copy(showBackupSuccessDialog = false)
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    // Guard: Clean up unzipped archive extraction directory from cache when ViewModel is destroyed or screen is exited
    override fun onCleared() {
        super.onCleared()
        try {
            _uiState.value.inspectionResult?.tempDir?.deleteRecursively()
        } catch (_: Exception) {}
    }
}
