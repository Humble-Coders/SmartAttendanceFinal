package com.humblecoders.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblecoders.smartattendance.data.model.ProfileData
import com.humblecoders.smartattendance.data.repository.ProfileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    // Profile data from repository
    val profileData: StateFlow<ProfileData> = profileRepository.profileData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileData()
        )

    // Local state for form inputs
    private val _nameInput = MutableStateFlow("")
    val nameInput: StateFlow<String> = _nameInput.asStateFlow()

    private val _rollNumberInput = MutableStateFlow("")
    val rollNumberInput: StateFlow<String> = _rollNumberInput.asStateFlow()

    // UI state
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        // Load existing profile data into input fields
        viewModelScope.launch {
            profileData.collect { profile ->
                _nameInput.value = profile.name
                _rollNumberInput.value = profile.rollNumber
            }
        }
    }

    fun updateNameInput(name: String) {
        _nameInput.value = name
    }

    fun updateRollNumberInput(rollNumber: String) {
        _rollNumberInput.value = rollNumber
    }

    fun saveProfile(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                profileRepository.saveProfile(
                    name = _nameInput.value,
                    rollNumber = _rollNumberInput.value
                )
                onSuccess()
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateFaceRegistrationStatus(isRegistered: Boolean) {
        viewModelScope.launch {
            profileRepository.updateFaceRegistrationStatus(isRegistered)
        }
    }
}