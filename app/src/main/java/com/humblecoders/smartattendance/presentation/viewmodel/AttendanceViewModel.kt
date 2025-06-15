package com.humblecoders.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblecoders.smartattendance.data.repository.AttendanceRepository
import com.humblecoders.smartattendance.data.repository.ProfileRepository
import com.humblecoders.smartattendance.data.model.AttendanceRecord
import com.humblecoders.smartattendance.data.model.SessionCheckResult
import com.humblecoders.smartattendance.data.model.ActiveSession
import com.humblecoders.smartattendance.data.model.AttendanceStats
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

    // NEW: Track attendance completion status
    private val _isAttendanceCompletedToday = MutableStateFlow(false)
    val isAttendanceCompletedToday: StateFlow<Boolean> = _isAttendanceCompletedToday.asStateFlow()

    private val _attendanceHistory = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceHistory: StateFlow<List<AttendanceRecord>> = _attendanceHistory.asStateFlow()

    private val _attendanceStats = MutableStateFlow<AttendanceStats?>(null)
    val attendanceStats: StateFlow<AttendanceStats?> = _attendanceStats.asStateFlow()

    // Session state
    private val _currentSession = MutableStateFlow<ActiveSession?>(null)
    val currentSession: StateFlow<ActiveSession?> = _currentSession.asStateFlow()

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _isCheckingSession = MutableStateFlow(false)
    val isCheckingSession: StateFlow<Boolean> = _isCheckingSession.asStateFlow()

    /**
     * Check if there's an active session for the student's class
     */
    fun isRoomMatching(detectedRoom: String): Boolean {
        val session = _currentSession.value
        if (session == null) return false

        // Remove the 3 digits from detected room for comparison
        val roomName = if (detectedRoom.length >= 3) {
            val suffix = detectedRoom.takeLast(3)
            if (suffix.all { it.isDigit() }) {
                detectedRoom.dropLast(3)
            } else {
                detectedRoom
            }
        } else {
            detectedRoom
        }

        val matches = session.room.equals(roomName, ignoreCase = true)
        Timber.d("🏢 Room matching: session.room='${session.room}', detectedRoom='$detectedRoom', extractedRoom='$roomName', matches=$matches")
        return matches
    }
    /**
     * Mark attendance with the new structure (updated to use session data)
     */
    fun markAttendance(
        rollNumber: String,
        deviceRoom: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                Timber.d("🎯 AttendanceViewModel: Starting attendance marking")
                Timber.d("📋 Parameters: rollNumber=$rollNumber, deviceRoom=$deviceRoom")

                // Get student profile data
                val profileData = profileRepository.profileData.first()
                val studentName = profileData.name.ifBlank { "Unknown Student" }
                val className = profileData.className

                if (className.isBlank()) {
                    onError("No class information available in profile")
                    return@launch
                }

                // Get current session data
                val session = _currentSession.value
                if (session == null) {
                    onError("No active session found")
                    return@launch
                }

                Timber.d("👤 Student info: name=$studentName, rollNumber=$rollNumber, class=$className")
                Timber.d("📚 Session info: subject=${session.subject}, room=${session.room}, type=${session.type}")

                // Validate attendance eligibility
                Timber.d("🔍 Validating attendance eligibility...")
                val eligibilityResult = attendanceRepository.validateAttendanceEligibility(
                    rollNumber = rollNumber,
                    subject = session.subject,
                    group = className,
                    type = session.type
                )

                if (eligibilityResult.isFailure) {
                    val error = "Failed to validate attendance eligibility"
                    Timber.e("❌ Eligibility validation failed: ${eligibilityResult.exceptionOrNull()}")
                    onError(error)
                    return@launch
                }

                val eligibility = eligibilityResult.getOrNull()!!
                if (!eligibility.isEligible) {
                    Timber.w("⚠️ Attendance not eligible: ${eligibility.reason}")
                    onError(eligibility.reason)
                    return@launch
                }

                Timber.d("✅ Eligibility check passed, proceeding to mark attendance")

                // Mark attendance with session data
                Timber.d("📡 Calling repository to mark attendance...")
                val result = attendanceRepository.markAttendance(
                    rollNumber = rollNumber,
                    studentName = studentName,
                    subject = session.subject,
                    group = className,
                    type = session.type,
                    deviceRoom = deviceRoom
                )

                if (result.isSuccess) {
                    val response = result.getOrNull()!!
                    Timber.i("🎉 Attendance marked successfully: ${response.attendanceId}")

                    // Refresh attendance data
                    refreshAttendanceData(rollNumber)

                    onSuccess()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Timber.e("❌ Attendance marking failed: $error")
                    onError(error)
                }

            } catch (e: Exception) {
                Timber.e(e, "💥 Unexpected error during attendance marking")
                onError("Unexpected error: ${e.message}")
            } finally {
                _isLoading.value = false
                Timber.d("🏁 AttendanceViewModel: Attendance marking process completed")
            }
        }
    }

    /**
     * Check if there's an active session for the student's class (overloaded for UI)
     */
    fun checkActiveSession(
        className: String,
        onResult: (Boolean, String, String, String) -> Unit
    ) {
        viewModelScope.launch {
            _isCheckingSession.value = true

            try {
                Timber.d("🔍 AttendanceViewModel: Checking active session for class: $className")

                if (className.isBlank()) {
                    Timber.w("⚠️ No class name provided")
                    onResult(false, "", "", "")
                    return@launch
                }

                val result = attendanceRepository.checkActiveSession(className)

                if (result.isSuccess) {
                    val sessionResult = result.getOrNull()!!
                    _isSessionActive.value = sessionResult.isActive
                    _currentSession.value = sessionResult.session

                    if (sessionResult.isActive && sessionResult.session != null) {
                        val session = sessionResult.session!!
                        Timber.d("✅ Active session found: ${session.subject} in ${session.room}")
                        onResult(true, session.subject, session.room, session.type)
                    } else {
                        Timber.d("⚪ No active session for class: $className")
                        onResult(false, "", "", "")
                    }
                } else {
                    Timber.e("❌ Failed to check session: ${result.exceptionOrNull()}")
                    _isSessionActive.value = false
                    _currentSession.value = null
                    onResult(false, "", "", "")
                }

            } catch (e: Exception) {
                Timber.e(e, "💥 Error checking active session for class: $className")
                _isSessionActive.value = false
                _currentSession.value = null
                onResult(false, "", "", "")
            } finally {
                _isCheckingSession.value = false
            }
        }
    }
    fun getCurrentSessionInfo(): Triple<String?, String?, String?> {
        val session = _currentSession.value
        return Triple(session?.subject, session?.room, session?.type)
    }

    /**
     * Check if room matches the detected device room
     */

    /**
     * Load attendance history for current student
     */
    fun loadAttendanceHistory() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Timber.d("📚 Loading attendance history...")

                val profileData = profileRepository.profileData.first()
                val rollNumber = profileData.rollNumber

                if (rollNumber.isBlank()) {
                    Timber.w("⚠️ Cannot load attendance history - no roll number available")
                    return@launch
                }

                val result = attendanceRepository.getAttendanceHistory(rollNumber)

                if (result.isSuccess) {
                    val history = result.getOrNull() ?: emptyList()
                    _attendanceHistory.value = history
                    Timber.d("✅ Loaded ${history.size} attendance records")
                } else {
                    Timber.e("❌ Failed to load attendance history: ${result.exceptionOrNull()}")
                }

            } catch (e: Exception) {
                Timber.e(e, "💥 Error loading attendance history")
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
                Timber.d("📊 Loading attendance stats...")

                val profileData = profileRepository.profileData.first()
                val rollNumber = profileData.rollNumber

                if (rollNumber.isBlank()) {
                    Timber.w("⚠️ Cannot load attendance stats - no roll number available")
                    return@launch
                }

                val result = attendanceRepository.getAttendanceStats(rollNumber, subjectCode)

                if (result.isSuccess) {
                    val stats = result.getOrNull()
                    _attendanceStats.value = stats
                    Timber.d("✅ Loaded attendance stats: $stats")
                } else {
                    Timber.e("❌ Failed to load attendance stats: ${result.exceptionOrNull()}")
                }

            } catch (e: Exception) {
                Timber.e(e, "💥 Error loading attendance stats")
            }
        }
    }

    /**
     * Sync student profile with Firebase
     */
    fun syncStudentProfile() {
        viewModelScope.launch {
            try {
                Timber.d("🔄 Syncing student profile with Firebase...")

                val profileData = profileRepository.profileData.first()

                if (profileData.rollNumber.isBlank() || profileData.name.isBlank()) {
                    Timber.w("⚠️ Cannot sync profile - incomplete profile data")
                    return@launch
                }

                val result = attendanceRepository.saveStudentProfile(
                    rollNumber = profileData.rollNumber,
                    name = profileData.name,
                    faceId = if (profileData.isFaceRegistered) "registered" else ""
                )

                if (result.isSuccess) {
                    Timber.d("✅ Student profile synced successfully")
                } else {
                    Timber.e("❌ Failed to sync student profile: ${result.exceptionOrNull()}")
                }

            } catch (e: Exception) {
                Timber.e(e, "💥 Error syncing student profile")
            }
        }
    }

    /**
     * Update face ID in Firebase after face registration
     */
    fun updateFaceIdInFirebase(faceId: String) {
        viewModelScope.launch {
            try {
                Timber.d("🆔 Updating face ID in Firebase...")

                val profileData = profileRepository.profileData.first()
                val rollNumber = profileData.rollNumber

                if (rollNumber.isBlank()) {
                    Timber.w("⚠️ Cannot update face ID - no roll number available")
                    return@launch
                }

                val result = attendanceRepository.updateStudentFaceId(rollNumber, faceId)

                if (result.isSuccess) {
                    Timber.d("✅ Face ID updated in Firebase successfully")
                } else {
                    Timber.e("❌ Failed to update face ID in Firebase: ${result.exceptionOrNull()}")
                }

            } catch (e: Exception) {
                Timber.e(e, "💥 Error updating face ID in Firebase")
            }
        }
    }

    /**
     * Refresh all attendance data for current student
     */
    private fun refreshAttendanceData(rollNumber: String) {
        viewModelScope.launch {
            try {
                Timber.d("🔄 Refreshing attendance data for $rollNumber")

                // Refresh history
                val historyResult = attendanceRepository.getAttendanceHistory(rollNumber)
                if (historyResult.isSuccess) {
                    _attendanceHistory.value = historyResult.getOrNull() ?: emptyList()
                    Timber.d("✅ Refreshed attendance history")
                } else {
                    Timber.e("❌ Failed to refresh attendance history")
                }

                // Refresh stats
                val statsResult = attendanceRepository.getAttendanceStats(rollNumber)
                if (statsResult.isSuccess) {
                    _attendanceStats.value = statsResult.getOrNull()
                    Timber.d("✅ Refreshed attendance stats")
                } else {
                    Timber.e("❌ Failed to refresh attendance stats")
                }

            } catch (e: Exception) {
                Timber.e(e, "💥 Error refreshing attendance data")
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
        return _attendanceHistory.value.filter { it.subject == subjectCode }
    }

    /**
     * Clear attendance data (for logout/reset)
     */
    fun clearAttendanceData() {
        _attendanceHistory.value = emptyList()
        _attendanceStats.value = null
        _currentSession.value = null
        _isSessionActive.value = false
        _isAttendanceCompletedToday.value = false // ADD THIS LINE
        Timber.d("🧹 Cleared attendance data")
    }

    /**
     * Initialize ViewModel - sync profile and load data
     */
    fun initialize() {
        viewModelScope.launch {
            try {
                Timber.d("🚀 Initializing AttendanceViewModel...")

                val profileData = profileRepository.profileData.first()

                if (profileData.rollNumber.isNotBlank() && profileData.name.isNotBlank()) {
                    Timber.d("✅ Profile data available, syncing and loading attendance data")

                    // Sync profile with Firebase
                    syncStudentProfile()

                    // Load attendance data
                    loadAttendanceHistory()
                    loadAttendanceStats()

                    // Check active session if class is available
                    if (profileData.className.isNotBlank()) {
                        checkActiveSession()
                    }
                } else {
                    Timber.w("⚠️ Profile data incomplete, skipping attendance data loading")
                }

            } catch (e: Exception) {
                Timber.e(e, "💥 Error initializing AttendanceViewModel")
            }
        }
    }

    fun checkActiveSession(onResult: (SessionCheckResult) -> Unit = {}) {
        viewModelScope.launch {
            _isCheckingSession.value = true

            try {
                Timber.d("🔍 AttendanceViewModel: Checking active session")

                val profileData = profileRepository.profileData.first()
                val className = profileData.className

                if (className.isBlank()) {
                    Timber.w("⚠️ No class name available in profile")
                    val result = SessionCheckResult(
                        isActive = false,
                        message = "No class information available"
                    )
                    _isSessionActive.value = false
                    _currentSession.value = null
                    onResult(result)
                    return@launch
                }

                Timber.d("📋 Checking session for class: $className")
                val result = attendanceRepository.checkActiveSession(className)

                if (result.isSuccess) {
                    val sessionResult = result.getOrNull()!!
                    _isSessionActive.value = sessionResult.isActive
                    _currentSession.value = sessionResult.session

                    Timber.d("✅ Session check completed: isActive=${sessionResult.isActive}")
                    if (sessionResult.isActive && sessionResult.session != null) {
                        Timber.d("📚 Active session details: ${sessionResult.session}")
                    }

                    onResult(sessionResult)
                } else {
                    Timber.e("❌ Failed to check session: ${result.exceptionOrNull()}")
                    val errorResult = SessionCheckResult(
                        isActive = false,
                        message = "Failed to check session status"
                    )
                    _isSessionActive.value = false
                    _currentSession.value = null
                    onResult(errorResult)
                }

            } catch (e: Exception) {
                Timber.e(e, "💥 Error checking active session")
                val errorResult = SessionCheckResult(
                    isActive = false,
                    message = "Error checking session: ${e.message}"
                )
                _isSessionActive.value = false
                _currentSession.value = null
                onResult(errorResult)
            } finally {
                _isCheckingSession.value = false
            }
        }
    }

    /**
     * Mark attendance as completed for today's session
     */
    fun markAttendanceCompleted() {
        _isAttendanceCompletedToday.value = true
        Timber.d("✅ Attendance marked as completed for today")
    }

    /**
     * Reset attendance completion status (for new sessions)
     */
    fun resetAttendanceStatus() {
        _isAttendanceCompletedToday.value = false
        Timber.d("🔄 Attendance completion status reset")
    }
}