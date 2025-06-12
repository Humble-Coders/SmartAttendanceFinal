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

    // Track if we've initialized the form inputs
    private val _isFormInitialized = MutableStateFlow(false)
    val isFormInitialized: StateFlow<Boolean> = _isFormInitialized.asStateFlow()

    // Track if profile is saved (for validation)
    val isProfileSaved: StateFlow<Boolean> = profileData
        .map { it.name.isNotBlank() && it.rollNumber.isNotBlank() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    init {
        // Initialize form inputs only once when ViewModel is created
        initializeFormInputs()
    }

    private fun initializeFormInputs() {
        viewModelScope.launch {
            // Take only the first emission to initialize the form
            profileData.take(1).collect { profile ->
                _nameInput.value = profile.name
                _rollNumberInput.value = profile.rollNumber
                _isFormInitialized.value = true
            }
        }
    }

    fun updateNameInput(name: String) {
        _nameInput.value = name
    }

    fun updateRollNumberInput(rollNumber: String) {
        _rollNumberInput.value = rollNumber
    }

    fun saveProfile(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            if (_nameInput.value.isBlank() || _rollNumberInput.value.isBlank()) {
                onError("Please fill in all fields")
                return@launch
            }

            _isSaving.value = true
            try {
                profileRepository.saveProfile(
                    name = _nameInput.value.trim(),
                    rollNumber = _rollNumberInput.value.trim()
                )
                onSuccess()
            } catch (e: Exception) {
                onError("Failed to save profile: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateFaceRegistrationStatus(isRegistered: Boolean) {
        viewModelScope.launch {
            try {
                profileRepository.updateFaceRegistrationStatus(isRegistered)
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    // Method to reload form inputs if needed (optional)
    fun reloadFormInputs() {
        viewModelScope.launch {
            val currentProfile = profileData.first()
            _nameInput.value = currentProfile.name
            _rollNumberInput.value = currentProfile.rollNumber
        }
    }

    // Validation helpers
    fun isFormValid(): Boolean {
        return _nameInput.value.isNotBlank() && _rollNumberInput.value.isNotBlank()
    }

    fun canRegisterFace(): Boolean {
        return isProfileSaved.value
    }
}