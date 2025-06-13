// Replace your existing AttendanceViewModel.kt with this:

package com.humblecoders.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblecoders.smartattendance.data.repository.AttendanceRepository
import com.humblecoders.smartattendance.data.repository.ProfileRepository
import com.humblecoders.smartattendance.data.model.AttendanceRecord
import com.humblecoders.smartattendance.data.repository.AttendanceStats
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber

class AttendanceViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _attendanceHistory = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceHistory: StateFlow<List<AttendanceRecord>> = _attendanceHistory.asStateFlow()

    private val _attendanceStats = MutableStateFlow<AttendanceStats?>(null)
    val attendanceStats: StateFlow<AttendanceStats?> = _attendanceStats.asStateFlow()

    /**
     * Mark attendance with comprehensive validation and Firebase integration
     */
    fun markAttendance(
        rollNumber: String,
        subjectCode: String = "Unknown",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Get student profile data
                val profileData = profileRepository.profileData.first()
                val studentName = profileData.name.ifBlank { "Unknown Student" }

                Timber.d("Starting attendance marking for: $rollNumber, Subject: $subjectCode, Student: $studentName")

                // Simplified validation - only check for duplicate attendance
                val eligibilityResult = attendanceRepository.validateAttendanceEligibility(rollNumber, subjectCode)

                if (eligibilityResult.isFailure) {
                    val error = "Failed to validate attendance eligibility"
                    Timber.e("Eligibility validation failed")
                    onError(error)
                    return@launch
                }

                val eligibility = eligibilityResult.getOrNull()!!
                if (!eligibility.isEligible) {
                    Timber.w("Attendance not eligible: ${eligibility.reason}")
                    onError(eligibility.reason)
                    return@launch
                }

                // Mark attendance
                val result = attendanceRepository.markAttendance(
                    rollNumber = rollNumber,
                    studentName = studentName,
                    subjectCode = subjectCode,
                    location = "Mobile App" // You can add location detection later
                )

                if (result.isSuccess) {
                    val response = result.getOrNull()!!
                    if (response.success) {
                        Timber.i("Attendance marked successfully: ${response.attendanceId}")

                        // Refresh attendance data
                        refreshAttendanceData(rollNumber)

                        onSuccess()
                    } else {
                        Timber.w("Attendance marking failed: ${response.message}")
                        onError(response.message)
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Timber.e("Attendance marking failed: $error")
                    onError("Failed to mark attendance: $error")
                }

            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during attendance marking")
                onError("Unexpected error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load attendance history for current student
     */
    fun loadAttendanceHistory() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val profileData = profileRepository.profileData.first()
                val rollNumber = profileData.rollNumber

                if (rollNumber.isBlank()) {
                    Timber.w("Cannot load attendance history - no roll number available")
                    return@launch
                }

                val result = attendanceRepository.getAttendanceHistory(rollNumber)

                if (result.isSuccess) {
                    val history = result.getOrNull() ?: emptyList()
                    _attendanceHistory.value = history
                    Timber.d("Loaded ${history.size} attendance records")
                } else {
                    Timber.e("Failed to load attendance history: ${result.exceptionOrNull()}")
                }

            } catch (e: Exception) {
                Timber.e(e, "Error loading attendance history")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load attendance statistics for current student
     */
    fun loadAttendanceStats(subjectCode: String? = null) {
        viewModelScope.launch {
            try {
                val profileData = profileRepository.profileData.first()
                val rollNumber = profileData.rollNumber

                if (rollNumber.isBlank()) {
                    Timber.w("Cannot load attendance stats - no roll number available")
                    return@launch
                }

                val result = attendanceRepository.getAttendanceStats(rollNumber, subjectCode)

                if (result.isSuccess) {
                    val stats = result.getOrNull()
                    _attendanceStats.value = stats
                    Timber.d("Loaded attendance stats: $stats")
                } else {
                    Timber.e("Failed to load attendance stats: ${result.exceptionOrNull()}")
                }

            } catch (e: Exception) {
                Timber.e(e, "Error loading attendance stats")
            }
        }
    }

    /**
     * Sync student profile with Firebase
     */
    fun syncStudentProfile() {
        viewModelScope.launch {
            try {
                val profileData = profileRepository.profileData.first()

                if (profileData.rollNumber.isBlank() || profileData.name.isBlank()) {
                    Timber.w("Cannot sync profile - incomplete profile data")
                    return@launch
                }

                val result = attendanceRepository.saveStudentProfile(
                    rollNumber = profileData.rollNumber,
                    name = profileData.name,
                    faceId = if (profileData.isFaceRegistered) "registered" else ""
                )

                if (result.isSuccess) {
                    Timber.d("Student profile synced successfully")
                } else {
                    Timber.e("Failed to sync student profile: ${result.exceptionOrNull()}")
                }

            } catch (e: Exception) {
                Timber.e(e, "Error syncing student profile")
            }
        }
    }

    /**
     * Update face ID in Firebase after face registration
     */
    fun updateFaceIdInFirebase(faceId: String) {
        viewModelScope.launch {
            try {
                val profileData = profileRepository.profileData.first()
                val rollNumber = profileData.rollNumber

                if (rollNumber.isBlank()) {
                    Timber.w("Cannot update face ID - no roll number available")
                    return@launch
                }

                val result = attendanceRepository.updateStudentFaceId(rollNumber, faceId)

                if (result.isSuccess) {
                    Timber.d("Face ID updated in Firebase successfully")
                } else {
                    Timber.e("Failed to update face ID in Firebase: ${result.exceptionOrNull()}")
                }

            } catch (e: Exception) {
                Timber.e(e, "Error updating face ID in Firebase")
            }
        }
    }

    /**
     * Check if subject is currently active for attendance
     */
    fun checkSubjectStatus(
        subjectCode: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = attendanceRepository.isSubjectActive(subjectCode)

                if (result.isSuccess) {
                    val isActive = result.getOrNull() ?: false
                    val message = if (isActive) {
                        "Subject is active for attendance"
                    } else {
                        "Subject is not currently active for attendance"
                    }
                    onResult(isActive, message)
                } else {
                    onResult(false, "Failed to check subject status")
                }

            } catch (e: Exception) {
                Timber.e(e, "Error checking subject status")
                onResult(false, "Error checking subject status: ${e.message}")
            }
        }
    }

    /**
     * Refresh all attendance data for current student
     */
    private fun refreshAttendanceData(rollNumber: String) {
        viewModelScope.launch {
            try {
                // Refresh history
                val historyResult = attendanceRepository.getAttendanceHistory(rollNumber)
                if (historyResult.isSuccess) {
                    _attendanceHistory.value = historyResult.getOrNull() ?: emptyList()
                }

                // Refresh stats
                val statsResult = attendanceRepository.getAttendanceStats(rollNumber)
                if (statsResult.isSuccess) {
                    _attendanceStats.value = statsResult.getOrNull()
                }

            } catch (e: Exception) {
                Timber.e(e, "Error refreshing attendance data")
            }
        }
    }

    /**
     * Get today's attendance for display
     */
    fun getTodayAttendance(): List<AttendanceRecord> {
        return _attendanceHistory.value.filter { it.isToday() }
    }

    /**
     * Get attendance for specific subject
     */
    fun getAttendanceForSubject(subjectCode: String): List<AttendanceRecord> {
        return _attendanceHistory.value.filter { it.subjectCode == subjectCode }
    }

    /**
     * Clear attendance data (for logout/reset)
     */
    fun clearAttendanceData() {
        _attendanceHistory.value = emptyList()
        _attendanceStats.value = null
    }

    /**
     * Initialize ViewModel - sync profile and load data
     */
    fun initialize() {
        viewModelScope.launch {
            try {
                val profileData = profileRepository.profileData.first()

                if (profileData.rollNumber.isNotBlank() && profileData.name.isNotBlank()) {
                    // Sync profile with Firebase
                    syncStudentProfile()

                    // Load attendance data
                    loadAttendanceHistory()
                    loadAttendanceStats()
                }

            } catch (e: Exception) {
                Timber.e(e, "Error initializing AttendanceViewModel")
            }
        }
    }
}