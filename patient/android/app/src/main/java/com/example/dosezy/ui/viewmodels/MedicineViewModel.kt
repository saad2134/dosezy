package com.example.dosezy.ui.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.MedicineSortMode
import com.example.dosezy.data.repository.MedicineOrderPreferences
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicineViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val userRepository: UserRepository,
    private val orderPreferences: MedicineOrderPreferences? = null
) : ViewModel() {

    private val _rawMedicines = MutableStateFlow<List<Medicine>>(emptyList())
    private val _medicines = MutableStateFlow<List<Medicine>>(emptyList())
    val medicines: StateFlow<List<Medicine>> = _medicines.asStateFlow()

    private val _archivedMedicines = MutableStateFlow<List<Medicine>>(emptyList())
    val archivedMedicines: StateFlow<List<Medicine>> = _archivedMedicines.asStateFlow()

    private val _sortMode = MutableStateFlow(MedicineSortMode.DEFAULT)
    val sortMode: StateFlow<MedicineSortMode> = _sortMode.asStateFlow()

    private val _customOrder = MutableStateFlow<List<String>>(emptyList())
    val customOrder: StateFlow<List<String>> = _customOrder.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var medicinesJob: kotlinx.coroutines.Job? = null
    private var archivedMedicinesJob: kotlinx.coroutines.Job? = null

    init {
        loadCurrentUserAndMedicines()
    }

    private fun loadCurrentUserAndMedicines() {
        viewModelScope.launch {
            userRepository.getAllUsers().collect { users ->
                val currentUser = users.find { it.isCurrentUser }
                _currentUserId.value = currentUser?.userId

                currentUser?.let { user ->
                    loadUserMedicines(user.userId)
                } ?: run {
                    // Guard: Cancel observation jobs and flush cached StateFlow lists when no active user exists to prevent stale patient data leakage across profile lifecycles
                    medicinesJob?.cancel()
                    archivedMedicinesJob?.cancel()
                    _medicines.value = emptyList()
                    _rawMedicines.value = emptyList()
                    _archivedMedicines.value = emptyList()
                    _sortMode.value = MedicineSortMode.DEFAULT
                    _customOrder.value = emptyList()
                    _isLoading.value = false
                }
            }
        }
    }

    fun setCurrentUser(userId: String) {
        if (_currentUserId.value != userId) {
            _currentUserId.value = userId
            loadUserMedicines(userId)
        }
    }

    private fun loadUserMedicines(userId: String) {
        medicinesJob?.cancel()
        archivedMedicinesJob?.cancel()
        _isLoading.value = true

        val initialMode = orderPreferences?.getSortMode(userId) ?: MedicineSortMode.DEFAULT
        val initialOrder = orderPreferences?.getCustomOrder(userId) ?: emptyList()
        _sortMode.value = initialMode
        _customOrder.value = initialOrder

        medicinesJob = viewModelScope.launch {
            medicineRepository.getMedicinesByUser(userId).collect { medicines ->
                _rawMedicines.value = medicines
                applySorting()
                _isLoading.value = false
            }
        }
        archivedMedicinesJob = viewModelScope.launch {
            medicineRepository.getArchivedMedicinesByUser(userId).collect { archived ->
                _archivedMedicines.value = archived
            }
        }
    }

    private fun applySorting() {
        _medicines.value = MedicineOrderPreferences.sortMedicines(
            medicines = _rawMedicines.value,
            mode = _sortMode.value,
            customOrder = _customOrder.value
        )
    }

    fun setSortMode(mode: MedicineSortMode) {
        val userId = _currentUserId.value ?: return
        _sortMode.value = mode
        orderPreferences?.setSortMode(userId, mode)
        applySorting()
    }

    fun setCustomOrder(orderedIds: List<String>) {
        val userId = _currentUserId.value ?: return
        _sortMode.value = MedicineSortMode.CUSTOM
        _customOrder.value = orderedIds
        orderPreferences?.setSortMode(userId, MedicineSortMode.CUSTOM)
        orderPreferences?.setCustomOrder(userId, orderedIds)
        applySorting()
    }

    fun moveMedicineUp(medicineId: String) {
        val userId = _currentUserId.value ?: return
        val currentList = if (_sortMode.value == MedicineSortMode.CUSTOM && _customOrder.value.isNotEmpty()) {
            _customOrder.value
        } else {
            _medicines.value.map { it.medicineId }
        }
        val newOrder = MedicineOrderPreferences.reorderMedicine(
            currentOrder = currentList,
            medicineId = medicineId,
            moveUp = true,
            allMedicines = _rawMedicines.value
        )
        // Guard: Switch to CUSTOM mode and persist order so Home screen intra-slot cards and medicines list reflect custom ordering immediately
        _sortMode.value = MedicineSortMode.CUSTOM
        _customOrder.value = newOrder
        orderPreferences?.setSortMode(userId, MedicineSortMode.CUSTOM)
        orderPreferences?.setCustomOrder(userId, newOrder)
        applySorting()
    }

    fun moveMedicineDown(medicineId: String) {
        val userId = _currentUserId.value ?: return
        val currentList = if (_sortMode.value == MedicineSortMode.CUSTOM && _customOrder.value.isNotEmpty()) {
            _customOrder.value
        } else {
            _medicines.value.map { it.medicineId }
        }
        val newOrder = MedicineOrderPreferences.reorderMedicine(
            currentOrder = currentList,
            medicineId = medicineId,
            moveUp = false,
            allMedicines = _rawMedicines.value
        )
        // Guard: Switch to CUSTOM mode and persist order so Home screen intra-slot cards and medicines list reflect custom ordering immediately
        _sortMode.value = MedicineSortMode.CUSTOM
        _customOrder.value = newOrder
        orderPreferences?.setSortMode(userId, MedicineSortMode.CUSTOM)
        orderPreferences?.setCustomOrder(userId, newOrder)
        applySorting()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addMedicine(medicine: Medicine) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                medicineRepository.insertMedicine(medicine)
            } finally {
                _isLoading.value = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateMedicine(
        medicine: Medicine,
        effectiveDate: com.example.dosezy.data.model.ScheduleEffectiveDate = com.example.dosezy.data.model.ScheduleEffectiveDate.TODAY
    ) {
        viewModelScope.launch {
            medicineRepository.updateMedicine(medicine, effectiveDate)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun archiveMedicine(medicine: Medicine) {
        viewModelScope.launch {
            medicineRepository.archiveMedicine(medicine)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun unarchiveMedicine(medicine: Medicine) {
        viewModelScope.launch {
            medicineRepository.unarchiveMedicine(medicine)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun deleteMedicinePermanently(medicine: Medicine) {
        viewModelScope.launch {
            medicineRepository.deleteMedicinePermanently(medicine)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            medicineRepository.deleteMedicine(medicine)
        }
    }
}