// Create this file: app/src/main/java/com/humblecoders/smartattendance/data/repository/FirebaseRepository.kt

package com.humblecoders.smartattendance.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.humblecoders.smartattendance.data.model.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class FirebaseRepository {

    private val firestore = FirebaseFirestore.getInstance()

    // Collections
    private val studentsCollection = firestore.collection("students")
    private val subjectsCollection = firestore.collection("subjects")
    private val attendanceCollection = firestore.collection("attendance_records")

    /**
     * Register/Update student profile in Firebase
     */
    suspend fun saveStudentProfile(student: Student): Result<String> {
        return try {
            studentsCollection.document(student.rollNumber)
                .set(student.copy(updatedAt = Timestamp.now()))
                .await()

            Timber.d("Student profile saved successfully: ${student.rollNumber}")
            Result.success("Profile saved successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save student profile")
            Result.failure(e)
        }
    }

    /**
     * Get student profile by roll number
     */
    suspend fun getStudentProfile(rollNumber: String): Result<Student?> {
        return try {
            val document = studentsCollection.document(rollNumber).get().await()
            val student = document.toObject(Student::class.java)

            Timber.d("Retrieved student profile: $rollNumber")
            Result.success(student)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get student profile")
            Result.failure(e)
        }
    }

    /**
     * Check if subject is active and attendance is allowed
     */
    suspend fun isSubjectActiveForAttendance(subjectCode: String): Result<Boolean> {
        return try {
            val document = subjectsCollection.document(subjectCode).get().await()
            val subject = document.toObject(Subject::class.java)

            val isActive = subject?.isActive ?: false
            Timber.d("Subject $subjectCode active status: $isActive")

            Result.success(isActive)
        } catch (e: Exception) {
            Timber.e(e, "Failed to check subject status")
            Result.failure(e)
        }
    }

    /**
     * Check if attendance already marked today for student and subject
     */
    suspend fun isAttendanceAlreadyMarked(rollNumber: String, subjectCode: String): Result<Boolean> {
        return try {
            val today = LocalDateTime.now().toLocalDate().toString()

            val query = attendanceCollection
                .whereEqualTo("rollNumber", rollNumber)
                .whereEqualTo("subjectCode", subjectCode)
                .whereEqualTo("date", today)
                .limit(1)

            val documents = query.get().await()
            val alreadyMarked = !documents.isEmpty

            Timber.d("Attendance already marked for $rollNumber in $subjectCode today: $alreadyMarked")
            Result.success(alreadyMarked)
        } catch (e: Exception) {
            Timber.e(e, "Failed to check existing attendance")
            Result.failure(e)
        }
    }

    /**
     * Mark attendance for student
     */
    suspend fun markAttendance(request: MarkAttendanceRequest): Result<AttendanceResponse> {
        return try {
            // Only check if already marked today (removed subject active validation)
            val alreadyMarkedResult = isAttendanceAlreadyMarked(request.rollNumber, request.subjectCode)
            if (alreadyMarkedResult.isFailure) {
                return Result.success(AttendanceResponse(
                    success = false,
                    message = "Failed to verify existing attendance"
                ))
            }

            if (alreadyMarkedResult.getOrNull() == true) {
                return Result.success(AttendanceResponse(
                    success = false,
                    message = "Attendance already marked for today",
                    alreadyMarked = true
                ))
            }

            // Create attendance record
            val attendanceId = UUID.randomUUID().toString()
            val currentTime = Timestamp.now()
            val today = LocalDateTime.now().toLocalDate().toString()

            val attendanceRecord = AttendanceRecord(
                id = attendanceId,
                rollNumber = request.rollNumber,
                studentName = request.studentName,
                subjectCode = request.subjectCode,
                timestamp = currentTime,
                date = today,
                status = "PRESENT",
                verificationMethod = request.verificationMethod,
                location = request.location,
                deviceId = android.os.Build.MODEL // Device info
            )

            // Save to Firestore
            attendanceCollection.document(attendanceId)
                .set(attendanceRecord)
                .await()

            Timber.i("Attendance marked successfully: $attendanceId for ${request.rollNumber}")

            Result.success(AttendanceResponse(
                success = true,
                message = "Attendance marked successfully",
                attendanceId = attendanceId
            ))

        } catch (e: Exception) {
            Timber.e(e, "Failed to mark attendance")
            Result.success(AttendanceResponse(
                success = false,
                message = "Failed to mark attendance: ${e.message}"
            ))
        }
    }

    /**
     * Get attendance history for a student
     */
    suspend fun getAttendanceHistory(
        rollNumber: String,
        limit: Int = 50
    ): Result<List<AttendanceRecord>> {
        return try {
            val query = attendanceCollection
                .whereEqualTo("rollNumber", rollNumber)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            val documents = query.get().await()
            val attendanceList = documents.toObjects(AttendanceRecord::class.java)

            Timber.d("Retrieved ${attendanceList.size} attendance records for $rollNumber")
            Result.success(attendanceList)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get attendance history")
            Result.failure(e)
        }
    }

    /**
     * Get attendance statistics for a student
     */
    suspend fun getAttendanceStats(rollNumber: String, subjectCode: String? = null): Result<AttendanceStats> {
        return try {
            var query = attendanceCollection.whereEqualTo("rollNumber", rollNumber)

            if (subjectCode != null) {
                query = query.whereEqualTo("subjectCode", subjectCode)
            }

            val documents = query.get().await()
            val attendanceList = documents.toObjects(AttendanceRecord::class.java)

            val totalClasses = attendanceList.size
            val presentClasses = attendanceList.count { it.status == "PRESENT" }
            val percentage = if (totalClasses > 0) {
                (presentClasses.toFloat() / totalClasses.toFloat() * 100)
            } else 0f

            val stats = AttendanceStats(
                totalClasses = totalClasses,
                presentClasses = presentClasses,
                absentClasses = totalClasses - presentClasses,
                attendancePercentage = percentage
            )

            Timber.d("Attendance stats for $rollNumber: $stats")
            Result.success(stats)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get attendance stats")
            Result.failure(e)
        }
    }

    /**
     * Update student's face ID after face registration
     */
    suspend fun updateStudentFaceId(rollNumber: String, faceId: String): Result<String> {
        return try {
            studentsCollection.document(rollNumber)
                .update(
                    mapOf(
                        "faceId" to faceId,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()

            Timber.d("Face ID updated for student: $rollNumber")
            Result.success("Face ID updated successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to update face ID")
            Result.failure(e)
        }
    }

    /**
     * Get subject details
     */
    suspend fun getSubjectDetails(subjectCode: String): Result<Subject?> {
        return try {
            val document = subjectsCollection.document(subjectCode).get().await()
            val subject = document.toObject(Subject::class.java)

            Timber.d("Retrieved subject details: $subjectCode")
            Result.success(subject)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get subject details")
            Result.failure(e)
        }
    }
}

// Data class for attendance statistics
data class AttendanceStats(
    val totalClasses: Int,
    val presentClasses: Int,
    val absentClasses: Int,
    val attendancePercentage: Float
)