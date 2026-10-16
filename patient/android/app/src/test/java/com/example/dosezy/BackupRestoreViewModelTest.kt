/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.dosezy.data.export.BackupResult
import com.example.dosezy.data.export.BackupRestoreManager
import com.example.dosezy.data.export.ZipInspectionResult
import com.example.dosezy.ui.viewmodels.BackupRestoreViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRestoreViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val backupRestoreManager: BackupRestoreManager = mockk(relaxed = true)
    private lateinit var context: Context
    private lateinit var viewModel: BackupRestoreViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        viewModel = BackupRestoreViewModel(context, backupRestoreManager, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiStateIsEmpty() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.loadingMessage)
        assertNull(state.statusMessage)
        assertFalse(state.isStatusSuccess)
        assertNull(state.inspectionResult)
        assertNull(state.exportedBackupFile)
        assertFalse(state.showBackupSuccessDialog)
    }

    @Test
    fun testInspectBackupZipSuccess() = runTest(testDispatcher) {
        val mockUri = mockk<Uri>()
        val mockInspection = ZipInspectionResult(
            success = true,
            message = "Valid archive",
            profiles = listOf(mockk(relaxed = true))
        )
        coEvery { backupRestoreManager.inspectBackupZip(mockUri) } returns mockInspection

        viewModel.inspectBackupZip(mockUri)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(mockInspection, state.inspectionResult)
        assertNull(state.statusMessage)
    }

    @Test
    fun testInspectBackupZipFailure() = runTest(testDispatcher) {
        val mockUri = mockk<Uri>()
        val mockInspection = ZipInspectionResult(
            success = false,
            message = "Corrupted archive",
            profiles = emptyList()
        )
        coEvery { backupRestoreManager.inspectBackupZip(mockUri) } returns mockInspection

        viewModel.inspectBackupZip(mockUri)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.inspectionResult)
        assertFalse(state.isStatusSuccess)
        assertEquals("Corrupted archive", state.statusMessage)
    }

    @Test
    fun testCreateFullBackupSuccess() = runTest(testDispatcher) {
        val mockFile = File("fake_backup.zip")
        coEvery { backupRestoreManager.createFullBackupZip() } returns mockFile

        viewModel.createFullBackup()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.showBackupSuccessDialog)
        assertEquals(mockFile, state.exportedBackupFile)
        assertNull(state.statusMessage)
    }

    @Test
    fun testCreateFullBackupException() = runTest(testDispatcher) {
        coEvery { backupRestoreManager.createFullBackupZip() } throws RuntimeException("Disk full")

        viewModel.createFullBackup()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.showBackupSuccessDialog)
        assertFalse(state.isStatusSuccess)
        assertNotNull(state.statusMessage)
    }

    @Test
    fun testExecuteSelectiveRestoreSuccessWithCallback() = runTest(testDispatcher) {
        val tempDir = File("temp")
        val result = BackupResult(success = true, message = "Restored 1 user")
        coEvery { backupRestoreManager.executeSelectiveRestore(tempDir, any()) } returns result

        var callbackInvoked = false
        viewModel.executeSelectiveRestore(tempDir, emptyList()) {
            callbackInvoked = true
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isStatusSuccess)
        assertEquals("Restored 1 user", state.statusMessage)
        assertTrue(callbackInvoked)
    }

    @Test
    fun testDismissDialogsAndClearStatus() {
        viewModel.dismissBackupSuccessDialog()
        assertFalse(viewModel.uiState.value.showBackupSuccessDialog)

        viewModel.clearStatusMessage()
        assertNull(viewModel.uiState.value.statusMessage)
    }
}
