/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dosezy.data.export.BackupRestoreManager
import com.example.dosezy.data.export.ProfileImportDecision
import com.example.dosezy.data.export.ZipInspectionResult
import com.example.dosezy.data.model.User
import com.example.dosezy.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * ViewModel encapsulating multi-step onboarding, profile creation, and backup restoration logic.
 * Decouples NewUserScreen from direct database operations and coroutine lifecycle management.
 */
@HiltViewModel
class NewUserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _loadingMessage = MutableStateFlow<String?>(null)
    val loadingMessage: StateFlow<String?> = _loadingMessage.asStateFlow()

    private val _inspectionResult = MutableStateFlow<ZipInspectionResult?>(null)
    val inspectionResult: StateFlow<ZipInspectionResult?> = _inspectionResult.asStateFlow()

    private val _isProfileValid = MutableStateFlow(false)
    val isProfileValid: StateFlow<Boolean> = _isProfileValid.asStateFlow()

    fun setProfileValid(valid: Boolean) {
        _isProfileValid.value = valid
    }

    fun setInspectionResult(result: ZipInspectionResult?) {
        _inspectionResult.value = result
    }

    fun inspectBackup(
        uri: Uri,
        backupManager: BackupRestoreManager,
        inspectingMessage: String,
        onResult: (ZipInspectionResult) -> Unit
    ) {
        viewModelScope.launch {
            _isImporting.value = true
            _loadingMessage.value = inspectingMessage
            val result = withContext(Dispatchers.IO) {
                backupManager.inspectBackupZip(uri)
            }
            _isImporting.value = false
            _loadingMessage.value = null
            if (result.success && result.profiles.isNotEmpty()) {
                _inspectionResult.value = result
            }
            onResult(result)
        }
    }

    fun executeRestore(
        tempDir: File,
        decisions: List<ProfileImportDecision>,
        backupManager: BackupRestoreManager,
        restoringMessage: String,
        onComplete: (Boolean, String, User?) -> Unit
    ) {
        viewModelScope.launch {
            _isImporting.value = true
            _loadingMessage.value = restoringMessage
            val result = withContext(Dispatchers.IO) {
                backupManager.executeSelectiveRestore(tempDir, decisions)
            }
            _isImporting.value = false
            _loadingMessage.value = null

            val targetUser = if (result.success) {
                withContext(Dispatchers.IO) {
                    val users = userRepository.getAllUsersList()
                    users.find { it.isCurrentUser } ?: users.firstOrNull()
                }
            } else null

            onComplete(result.success, result.message, targetUser)
        }
    }

    fun saveUser(user: User, onComplete: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                userRepository.insertUser(user)
            }
            onComplete()
        }
    }
}
