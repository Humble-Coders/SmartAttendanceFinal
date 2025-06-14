package com.humblecoders.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblecoders.smartattendance.data.model.ProfileData
import com.humblecoders.smartattendance.data.repository.ProfileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

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

    private val _classNameInput = MutableStateFlow("")
    val classNameInput: StateFlow<String> = _classNameInput.asStateFlow()

    // UI state
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // Track if we've initialized the form inputs
    private val _isFormInitialized = MutableStateFlow(false)
    val isFormInitialized: StateFlow<Boolean> = _isFormInitialized.asStateFlow()

    // Track if profile is saved (for validation)
    val isProfileSaved: StateFlow<Boolean> = profileData
        .map { it.name.isNotBlank() && it.rollNumber.isNotBlank() && it.className.isNotBlank() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    init {
        // Initialize form inputs only once when ViewModel is created
        initializeFormInputs()

        // Log profile data changes for debugging
        viewModelScope.launch {
            profileData.collect { profile ->
                Timber.d("ProfileViewModel - Profile data updated: ${getProfileSummary()}")
            }
        }
    }

    private fun initializeFormInputs() {
        viewModelScope.launch {
            // Take only the first emission to initialize the form
            profileData.take(1).collect { profile ->
                _nameInput.value = profile.name
                _rollNumberInput.value = profile.rollNumber
                _classNameInput.value = profile.className
                _isFormInitialized.value = true
                Timber.d("ProfileViewModel - Form inputs initialized with: name='${profile.name}', rollNumber='${profile.rollNumber}', className='${profile.className}'")
            }
        }
    }

    fun updateNameInput(name: String) {
        _nameInput.value = name
    }

    fun updateRollNumberInput(rollNumber: String) {
        _rollNumberInput.value = rollNumber
    }

    fun updateClassNameInput(className: String) {
        _classNameInput.value = className
    }

    fun saveProfile(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            if (_nameInput.value.isBlank() || _rollNumberInput.value.isBlank() || _classNameInput.value.isBlank()) {
                onError("Please fill in all fields")
                return@launch
            }

            _isSaving.value = true
            try {
                profileRepository.saveProfile(
                    name = _nameInput.value.trim(),
                    rollNumber = _rollNumberInput.value.trim(),
                    className = _classNameInput.value.trim().uppercase()
                )
                Timber.d("ProfileViewModel - Profile saved successfully")
                onSuccess()
            } catch (e: Exception) {
                Timber.e(e, "ProfileViewModel - Failed to save profile")
                onError("Failed to save profile: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Save profile with class name (for login screen)
     */
    fun saveProfileWithClass(
        name: String,
        rollNumber: String,
        className: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (name.isBlank() || rollNumber.isBlank() || className.isBlank()) {
                onError("Please fill in all fields")
                return@launch
            }

            _isSaving.value = true
            try {
                profileRepository.saveProfile(
                    name = name.trim(),
                    rollNumber = rollNumber.trim(),
                    className = className.trim().uppercase()
                )

                // Update local inputs
                _nameInput.value = name.trim()
                _rollNumberInput.value = rollNumber.trim()
                _classNameInput.value = className.trim().uppercase()

                Timber.d("ProfileViewModel - Profile with class saved successfully")
                onSuccess()
            } catch (e: Exception) {
                Timber.e(e, "ProfileViewModel - Failed to save profile with class")
                onError("Failed to save profile: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Complete profile reset - clears all data including name, roll number, and class
     */
    fun resetCompleteProfile(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                Timber.d("ProfileViewModel - Starting complete profile reset")

                profileRepository.clearAllProfile()

                // Reset form inputs
                _nameInput.value = ""
                _rollNumberInput.value = ""
                _classNameInput.value = ""

                Timber.i("ProfileViewModel - Complete profile reset successful")
                onSuccess()

            } catch (e: Exception) {
                val errorMsg = "Failed to reset profile: ${e.message}"
                Timber.e(e, "ProfileViewModel - $errorMsg")
                onError(errorMsg)
            }
        }
    }

    /**
     * Get current profile summary for logging/debugging
     */
    fun getProfileSummary(): String {
        val profile = profileData.value
        return "Profile: name='${profile.name}', rollNumber='${profile.rollNumber}', className='${profile.className}'"
    }

    /**
     * Get current class name
     */
    fun getCurrentClassName(): String {
        return profileData.value.className
    }

    // Method to reload form inputs if needed (optional)
    fun reloadFormInputs() {
        viewModelScope.launch {
            val currentProfile = profileData.first()
            _nameInput.value = currentProfile.name
            _rollNumberInput.value = currentProfile.rollNumber
            _classNameInput.value = currentProfile.className
            Timber.d("ProfileViewModel - Form inputs reloaded from saved profile")
        }
    }

    // Validation helpers
    fun isFormValid(): Boolean {
        return _nameInput.value.isNotBlank() &&
                _rollNumberInput.value.isNotBlank() &&
                _classNameInput.value.isNotBlank()
    }

    /**
     * Validate form inputs and provide specific error messages
     */
    fun validateForm(): Pair<Boolean, String?> {
        return when {
            _nameInput.value.isBlank() -> false to "Name is required"
            _rollNumberInput.value.isBlank() -> false to "Roll number is required"
            _classNameInput.value.isBlank() -> false to "Class is required"
            _nameInput.value.length < 2 -> false to "Name must be at least 2 characters"
            _rollNumberInput.value.length < 3 -> false to "Roll number must be at least 3 characters"
            _classNameInput.value.length < 2 -> false to "Class must be at least 2 characters"
            else -> true to null
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("ProfileViewModel - ViewModel cleared")
    }
}