// Replace your existing AttendanceRepository.kt with this:

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
            Timber.d("Starting attendance marking process for $rollNumber in $subjectCode")

            val request = MarkAttendanceRequest(
                rollNumber = rollNumber,
                subjectCode = subjectCode,
                studentName = studentName,
                verificationMethod = "FACE_RECOGNITION",
                location = location
            )

            val result = firebaseRepository.markAttendance(request)

            if (result.isSuccess) {
                val response = result.getOrNull()!!
                if (response.success) {
                    Timber.i("Attendance marked successfully: ${response.attendanceId}")
                } else {
                    Timber.w("Attendance marking failed: ${response.message}")
                }
                Result.success(response)
            } else {
                val error = result.exceptionOrNull()!!
                Timber.e(error, "Firebase attendance marking failed")
                Result.success(AttendanceResponse(
                    success = false,
                    message = "Network error: ${error.message}"
                ))
            }
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during attendance marking")
            Result.success(AttendanceResponse(
                success = false,
                message = "Unexpected error: ${e.message}"
            ))
        }
    }

    /**
     * Get attendance history for a student
     */
    suspend fun getAttendanceHistory(rollNumber: String): Result<List<AttendanceRecord>> {
        return try {
            Timber.d("Fetching attendance history for $rollNumber")
            firebaseRepository.getAttendanceHistory(rollNumber)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch attendance history")
            Result.failure(e)
        }
    }

    /**
     * Get attendance statistics
     */
    suspend fun getAttendanceStats(rollNumber: String, subjectCode: String? = null): Result<AttendanceStats> {
        return try {
            Timber.d("Fetching attendance stats for $rollNumber")
            firebaseRepository.getAttendanceStats(rollNumber, subjectCode)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch attendance stats")
            Result.failure(e)
        }
    }

    /**
     * Check if subject is active for attendance
     */
    suspend fun isSubjectActive(subjectCode: String): Result<Boolean> {
        return try {
            firebaseRepository.isSubjectActiveForAttendance(subjectCode)
        } catch (e: Exception) {
            Timber.e(e, "Failed to check subject status")
            Result.failure(e)
        }
    }

    /**
     * Save or update student profile
     */
    suspend fun saveStudentProfile(rollNumber: String, name: String, faceId: String = ""): Result<String> {
        return try {
            val student = Student(
                rollNumber = rollNumber,
                name = name,
                faceId = faceId
            )
            firebaseRepository.saveStudentProfile(student)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save student profile")
            Result.failure(e)
        }
    }

    /**
     * Update face ID for student
     */
    suspend fun updateStudentFaceId(rollNumber: String, faceId: String): Result<String> {
        return try {
            firebaseRepository.updateStudentFaceId(rollNumber, faceId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update face ID")
            Result.failure(e)
        }
    }

    /**
     * Validate attendance eligibility (simplified - only checks for duplicate attendance)
     */
    suspend fun validateAttendanceEligibility(
        rollNumber: String,
        subjectCode: String
    ): Result<AttendanceEligibility> {
        return try {
            // Only check if already marked today (removed subject active validation)
            val alreadyMarkedResult = firebaseRepository.isAttendanceAlreadyMarked(rollNumber, subjectCode)
            if (alreadyMarkedResult.isFailure) {
                return Result.success(AttendanceEligibility(
                    isEligible = false,
                    reason = "Failed to verify existing attendance"
                ))
            }

            if (alreadyMarkedResult.getOrNull() == true) {
                return Result.success(AttendanceEligibility(
                    isEligible = false,
                    reason = "Attendance already marked for today",
                    alreadyMarked = true
                ))
            }

            // Always eligible if not already marked
            Result.success(AttendanceEligibility(
                isEligible = true,
                reason = "Eligible for attendance"
            ))

        } catch (e: Exception) {
            Timber.e(e, "Failed to validate attendance eligibility")
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