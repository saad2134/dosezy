package com.example.dosezy.ui.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dosezy.data.model.Medicine
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
    private val userRepository: UserRepository
) : ViewModel() {

    private val _medicines = MutableStateFlow<List<Medicine>>(emptyList())
    val medicines: StateFlow<List<Medicine>> = _medicines.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var medicinesJob: kotlinx.coroutines.Job? = null

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
        medicinesJob = viewModelScope.launch {
            medicineRepository.getMedicinesByUser(userId).collect { medicines ->
                _medicines.value = medicines
            }
        }
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
    fun updateMedicine(medicine: Medicine) {
        viewModelScope.launch {
            medicineRepository.updateMedicine(medicine)
        }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            medicineRepository.deleteMedicine(medicine)
        }
    }
}