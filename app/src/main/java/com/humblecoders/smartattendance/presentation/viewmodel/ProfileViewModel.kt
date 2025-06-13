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

    // UI state
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

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
                _isFormInitialized.value = true
                Timber.d("ProfileViewModel - Form inputs initialized with: name='${profile.name}', rollNumber='${profile.rollNumber}'")
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

    fun updateFaceRegistrationStatus(
        isRegistered: Boolean,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                Timber.d("ProfileViewModel - Updating face registration status to: $isRegistered")

                // Update in repository
                profileRepository.updateFaceRegistrationStatus(isRegistered)

                // Wait a moment for DataStore to propagate changes
                kotlinx.coroutines.delay(100)

                // Verify the update was successful by checking current state
                val currentStatus = profileRepository.getFaceRegistrationStatus()
                Timber.d("ProfileViewModel - Face registration status verification: expected=$isRegistered, actual=$currentStatus")

                if (currentStatus == isRegistered) {
                    Timber.i("ProfileViewModel - Face registration status successfully updated to: $isRegistered")
                    onSuccess()
                } else {
                    val errorMsg = "Face registration status update verification failed. Expected: $isRegistered, Got: $currentStatus"
                    Timber.e(errorMsg)
                    onError(errorMsg)
                }

            } catch (e: Exception) {
                val errorMsg = "Failed to update face registration status: ${e.message}"
                Timber.e(e, "ProfileViewModel - $errorMsg")
                onError(errorMsg)
            }
        }
    }

    /**
     * Delete all faces - resets face registration status in the app
     * This should be called after Face.io deletion is complete
     */
    fun deleteAllFaces(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isDeleting.value = true
            try {
                Timber.d("ProfileViewModel - Starting face deletion process")

                // Reset face registration status
                profileRepository.updateFaceRegistrationStatus(false)

                // Wait for DataStore to propagate
                kotlinx.coroutines.delay(200)

                // Verify deletion
                val currentStatus = profileRepository.getFaceRegistrationStatus()
                if (!currentStatus) {
                    Timber.i("ProfileViewModel - All faces deleted successfully")
                    onSuccess()
                } else {
                    throw Exception("Face deletion verification failed - status still shows registered")
                }

            } catch (e: Exception) {
                val errorMsg = "Failed to delete faces: ${e.message}"
                Timber.e(e, "ProfileViewModel - $errorMsg")
                onError(errorMsg)
            } finally {
                _isDeleting.value = false
            }
        }
    }

    /**
     * Complete profile reset - clears all data including name and roll number
     * Use this for complete app reset
     */
    fun resetCompleteProfile(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                Timber.d("ProfileViewModel - Starting complete profile reset")

                profileRepository.clearAllProfile()

                // Reset form inputs
                _nameInput.value = ""
                _rollNumberInput.value = ""

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
     * Quick face reset - only resets face registration, keeps profile data
     * Good for testing face registration repeatedly
     */
    fun resetFaceRegistrationOnly(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        updateFaceRegistrationStatus(false, onSuccess, onError)
    }

    /**
     * Check if any face data exists
     */
    fun hasFaceRegistered(): Boolean {
        return profileData.value.isFaceRegistered
    }

    /**
     * Get current profile summary for logging/debugging
     */
    fun getProfileSummary(): String {
        val profile = profileData.value
        return "Profile: name='${profile.name}', rollNumber='${profile.rollNumber}', faceRegistered=${profile.isFaceRegistered}"
    }

    // Method to reload form inputs if needed (optional)
    fun reloadFormInputs() {
        viewModelScope.launch {
            val currentProfile = profileData.first()
            _nameInput.value = currentProfile.name
            _rollNumberInput.value = currentProfile.rollNumber
            Timber.d("ProfileViewModel - Form inputs reloaded from saved profile")
        }
    }

    // Validation helpers
    fun isFormValid(): Boolean {
        return _nameInput.value.isNotBlank() && _rollNumberInput.value.isNotBlank()
    }

    fun canRegisterFace(): Boolean {
        return isProfileSaved.value
    }

    /**
     * Validate form inputs and provide specific error messages
     */
    fun validateForm(): Pair<Boolean, String?> {
        return when {
            _nameInput.value.isBlank() -> false to "Name is required"
            _rollNumberInput.value.isBlank() -> false to "Roll number is required"
            _nameInput.value.length < 2 -> false to "Name must be at least 2 characters"
            _rollNumberInput.value.length < 3 -> false to "Roll number must be at least 3 characters"
            else -> true to null
        }
    }

    /**
     * Get deletion status for UI feedback
     */
    fun getDeletionStatus(): Boolean {
        return _isDeleting.value
    }

    /**
     * Force refresh profile data (useful for debugging)
     */
    fun refreshProfileData() {
        viewModelScope.launch {
            try {
                val currentProfile = profileRepository.profileData.first()
                Timber.d("ProfileViewModel - Force refreshed profile data: ${getProfileSummary()}")
            } catch (e: Exception) {
                Timber.e(e, "ProfileViewModel - Failed to refresh profile data")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("ProfileViewModel - ViewModel cleared")
    }
}