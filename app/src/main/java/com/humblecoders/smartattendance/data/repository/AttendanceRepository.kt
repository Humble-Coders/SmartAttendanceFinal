package com.humblecoders.smartattendance.data.repository

import com.humblecoders.smartattendance.data.model.*
import timber.log.Timber

class AttendanceRepository {

    private val firebaseRepository = FirebaseRepository()

    /**
     * Check if there's an active session for the student's class
     */
    suspend fun checkActiveSession(className: String): Result<SessionCheckResult> {
        return try {
            Timber.d("🔍 Checking active session for class: $className")

            val result = firebaseRepository.checkActiveSession(className)

            if (result.isSuccess) {
                val sessionResult = result.getOrNull()!!
                Timber.d("✅ Session check completed: ${sessionResult.message}")
                Result.success(sessionResult)
            } else {
                Timber.e("❌ Failed to check active session: ${result.exceptionOrNull()}")
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Timber.e(e, "💥 Failed to check active session")
            Result.failure(e)
        }
    }

    /**
     * Mark attendance with the new structure
     */
    // In AttendanceRepository.kt - Replace the existing method
    suspend fun markAttendance(
        rollNumber: String,
        studentName: String,
        subject: String,
        group: String,
        type: String,
        deviceRoom: String,
        isExtra: Boolean = false
    ): Result<AttendanceResponse> {
        return try {
            val attendanceType = if (isExtra) "extra" else "regular"
            Timber.d("🚀 Starting $attendanceType attendance marking process")
            Timber.d("📋 Details: rollNumber=$rollNumber, subject=$subject, group=$group, type=$type")

            val result = firebaseRepository.markAttendance(
                rollNumber = rollNumber,
                studentName = studentName,
                subject = subject,
                group = group,
                type = type,
                deviceRoom = deviceRoom,
                isExtra = isExtra
            )

            if (result.isSuccess) {
                val response = result.getOrNull()!!
                if (response.success) {
                    Timber.i("✅ $attendanceType attendance marked successfully: ${response.attendanceId}")
                    Result.success(response)
                } else {
                    Timber.w("⚠️ $attendanceType attendance marking failed: ${response.message}")
                    Result.failure(Exception("Attendance marking failed: ${response.message}"))
                }
            } else {
                val error = result.exceptionOrNull()!!
                Timber.e(error, "❌ Firebase attendance marking failed")
                Result.failure(Exception("Firebase error: ${error.message}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "💥 Unexpected error during attendance marking")
            Result.failure(Exception("Unexpected error: ${e.message}"))
        }
    }
    /**
     * Get attendance history for a student
     */
    suspend fun getAttendanceHistory(rollNumber: String): Result<List<AttendanceRecord>> {
        return try {
            Timber.d("📚 Fetching attendance history for $rollNumber")
            val result = firebaseRepository.getAttendanceHistory(rollNumber)

            if (result.isSuccess) {
                val history = result.getOrNull() ?: emptyList()
                Timber.d("✅ Retrieved ${history.size} attendance records")
                Result.success(history)
            } else {
                Timber.e("❌ Failed to fetch attendance history: ${result.exceptionOrNull()}")
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Timber.e(e, "💥 Failed to fetch attendance history")
            Result.failure(e)
        }
    }

    /**
     * Get attendance statistics
     */
    suspend fun getAttendanceStats(rollNumber: String, subject: String? = null): Result<AttendanceStats> {
        return try {
            Timber.d("📊 Fetching attendance stats for $rollNumber")
            val result = firebaseRepository.getAttendanceStats(rollNumber, subject)

            if (result.isSuccess) {
                val stats = result.getOrNull()!!
                Timber.d("✅ Retrieved attendance stats: $stats")
                Result.success(stats)
            } else {
                Timber.e("❌ Failed to fetch attendance stats: ${result.exceptionOrNull()}")
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Timber.e(e, "💥 Failed to fetch attendance stats")
            Result.failure(e)
        }
    }

    /**
     * Save or update student profile
     */
    suspend fun saveStudentProfile(rollNumber: String, name: String, faceId: String = ""): Result<String> {
        return try {
            Timber.d("👤 Saving student profile for $rollNumber")
            val student = Student(
                rollNumber = rollNumber,
                name = name,
                faceId = faceId
            )
            val result = firebaseRepository.saveStudentProfile(student)

            if (result.isSuccess) {
                Timber.d("✅ Student profile saved successfully")
                Result.success(result.getOrNull()!!)
            } else {
                Timber.e("❌ Failed to save student profile: ${result.exceptionOrNull()}")
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Timber.e(e, "💥 Failed to save student profile")
            Result.failure(e)
        }
    }

    /**
     * Update face ID for student
     */
    suspend fun updateStudentFaceId(rollNumber: String, faceId: String): Result<String> {
        return try {
            Timber.d("🆔 Updating face ID for $rollNumber")
            val result = firebaseRepository.updateStudentFaceId(rollNumber, faceId)

            if (result.isSuccess) {
                Timber.d("✅ Face ID updated successfully")
                Result.success(result.getOrNull()!!)
            } else {
                Timber.e("❌ Failed to update face ID: ${result.exceptionOrNull()}")
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Timber.e(e, "💥 Failed to update face ID")
            Result.failure(e)
        }
    }

    /**
     * Validate attendance eligibility (check for duplicates)
     */
    // In AttendanceRepository.kt - Replace the existing method
    suspend fun validateAttendanceEligibility(
        rollNumber: String,
        subject: String,
        group: String,
        type: String,
        isExtra: Boolean = false
    ): Result<AttendanceEligibility> {
        return try {
            val attendanceType = if (isExtra) "extra" else "regular"
            Timber.d("🔍 Validating $attendanceType attendance eligibility for $rollNumber")

            // Check if already marked today for this specific type (regular or extra)
            val alreadyMarkedResult = firebaseRepository.isAttendanceAlreadyMarked(
                rollNumber, subject, group, type, isExtra
            )

            if (alreadyMarkedResult.isFailure) {
                Timber.e("❌ Failed to verify existing attendance: ${alreadyMarkedResult.exceptionOrNull()}")
                return Result.success(AttendanceEligibility(
                    isEligible = false,
                    reason = "Failed to verify existing attendance"
                ))
            }

            val alreadyMarked = alreadyMarkedResult.getOrNull() ?: false

            if (alreadyMarked) {
                Timber.w("⚠️ $attendanceType attendance already marked for today")
                return Result.success(AttendanceEligibility(
                    isEligible = false,
                    reason = "${attendanceType.capitalize()} attendance already marked for today",
                    alreadyMarked = true
                ))
            }

            // Eligible if not already marked
            Timber.d("✅ Student is eligible for $attendanceType attendance")
            Result.success(AttendanceEligibility(
                isEligible = true,
                reason = "Eligible for $attendanceType attendance"
            ))

        } catch (e: Exception) {
            Timber.e(e, "💥 Failed to validate attendance eligibility")
            Result.success(AttendanceEligibility(
                isEligible = false,
                reason = "Validation error: ${e.message}"
            ))
        }
    }
}

// Data class for attendance eligibility
data class AttendanceEligibility(
    val isEligible: Boolean,
    val reason: String,
    val alreadyMarked: Boolean = false
)