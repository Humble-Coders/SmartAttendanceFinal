package com.humblecoders.smartattendance.data.repository

import com.humblecoders.smartattendance.data.model.*
import timber.log.Timber

class AttendanceRepository {

    private val firebaseRepository = FirebaseRepository()

    /**
     * Mark attendance with Firebase integration
     */
    suspend fun markAttendance(
        rollNumber: String,
        studentName: String,
        subjectCode: String,
        location: String = ""
    ): Result<AttendanceResponse> {
        return try {
            Timber.d("🚀 Starting attendance marking process for $rollNumber in $subjectCode")

            val request = MarkAttendanceRequest(
                rollNumber = rollNumber,
                subjectCode = subjectCode,
                studentName = studentName,
                verificationMethod = "FACE_RECOGNITION",
                location = location
            )

            Timber.d("📋 Created attendance request: $request")

            val result = firebaseRepository.markAttendance(request)
            Timber.d("📡 Firebase repository returned: success=${result.isSuccess}")

            if (result.isSuccess) {
                val response = result.getOrNull()!!
                if (response.success) {
                    Timber.i("✅ Attendance marked successfully: ${response.attendanceId}")
                    Result.success(response)
                } else {
                    Timber.w("⚠️ Attendance marking failed: ${response.message}")
                    // FIX: Return failure instead of success with error message
                    Result.failure(Exception("Attendance marking failed: ${response.message}"))
                }
            } else {
                val error = result.exceptionOrNull()!!
                Timber.e(error, "❌ Firebase attendance marking failed")
                // FIX: Return failure instead of success with error message
                Result.failure(Exception("Firebase error: ${error.message}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "💥 Unexpected error during attendance marking")
            // FIX: Return failure instead of success with error message
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
    suspend fun getAttendanceStats(rollNumber: String, subjectCode: String? = null): Result<AttendanceStats> {
        return try {
            Timber.d("📊 Fetching attendance stats for $rollNumber")
            val result = firebaseRepository.getAttendanceStats(rollNumber, subjectCode)

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
     * Check if subject is active for attendance
     */
    suspend fun isSubjectActive(subjectCode: String): Result<Boolean> {
        return try {
            Timber.d("🔍 Checking if subject $subjectCode is active")
            val result = firebaseRepository.isSubjectActiveForAttendance(subjectCode)

            if (result.isSuccess) {
                val isActive = result.getOrNull() ?: false
                Timber.d("✅ Subject $subjectCode active status: $isActive")
                Result.success(isActive)
            } else {
                Timber.e("❌ Failed to check subject status: ${result.exceptionOrNull()}")
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Timber.e(e, "💥 Failed to check subject status")
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
     * Simplified attendance eligibility validation
     * FIX: Removed double validation - now only checks for duplicates
     */
    suspend fun validateAttendanceEligibility(
        rollNumber: String,
        subjectCode: String
    ): Result<AttendanceEligibility> {
        return try {
            Timber.d("🔍 Validating attendance eligibility for $rollNumber in $subjectCode")

            // Only check if already marked today (simplified validation)
            val alreadyMarkedResult = firebaseRepository.isAttendanceAlreadyMarked(rollNumber, subjectCode)

            if (alreadyMarkedResult.isFailure) {
                Timber.e("❌ Failed to verify existing attendance: ${alreadyMarkedResult.exceptionOrNull()}")
                return Result.success(AttendanceEligibility(
                    isEligible = false,
                    reason = "Failed to verify existing attendance"
                ))
            }

            val alreadyMarked = alreadyMarkedResult.getOrNull() ?: false

            if (alreadyMarked) {
                Timber.w("⚠️ Attendance already marked for today")
                return Result.success(AttendanceEligibility(
                    isEligible = false,
                    reason = "Attendance already marked for today",
                    alreadyMarked = true
                ))
            }

            // Always eligible if not already marked
            Timber.d("✅ Student is eligible for attendance")
            Result.success(AttendanceEligibility(
                isEligible = true,
                reason = "Eligible for attendance"
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