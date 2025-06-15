package com.humblecoders.smartattendance.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.humblecoders.smartattendance.data.model.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FirebaseRepository {

    private val firestore = FirebaseFirestore.getInstance()

    // New collections for the updated structure
    private val activeSessionsCollection = firestore.collection("activeSessions")

    // For monthly attendance collections (format: attendance_YYYY_MM)
    private fun getAttendanceCollection(year: Int, month: Int): com.google.firebase.firestore.CollectionReference {
        val collectionName = "attendance_${year}_${String.format("%02d", month)}"
        return firestore.collection(collectionName)
    }

    // For current month attendance
    private fun getCurrentAttendanceCollection(): com.google.firebase.firestore.CollectionReference {
        val now = LocalDateTime.now()
        return getAttendanceCollection(now.year, now.monthValue)
    }

    init {
        Timber.d("🔧 FirebaseRepository initialized with new structure")
        Timber.d("📊 Firestore instance: ${firestore.app.name}")
    }

    /**
     * Check if there's an active session for the student's class
     */
    // Replace the checkActiveSession method in your FirebaseRepository.kt with this:

    /**
     * Check if there's an active session for the student's class (FIXED VERSION)
     */
    suspend fun checkActiveSession(className: String): Result<SessionCheckResult> {
        return try {
            Timber.d("🔍 Checking active session for class: $className")

            val document = activeSessionsCollection.document(className).get().await()

            if (document.exists()) {
                val rawData = document.data
                Timber.d("📄 Session document found for class: $className")

                if (rawData != null) {
                    // Manual session creation to avoid deserialization issues
                    val session = ActiveSession(
                        isActive = (rawData["isActive"] as? Boolean) == true,
                        subject = (rawData["subject"] as? String) ?: "",
                        room = (rawData["room"] as? String) ?: "",
                        type = (rawData["type"] as? String) ?: "",
                        sessionId = (rawData["sessionId"] as? String) ?: "",
                        date = (rawData["date"] as? String) ?: "",
                        isExtra = (rawData["isExtra"] as? Boolean) == true
                    )

                    Timber.d("📚 Session created: isActive=${session.isActive}, subject=${session.subject}, room=${session.room}")

                    if (session.isActive) {
                        Timber.d("✅ Active session found for $className: ${session.subject} in ${session.room}")
                        Result.success(SessionCheckResult(
                            isActive = true,
                            session = session,
                            message = "Active session found"
                        ))
                    } else {
                        Timber.d("⚪ Session exists but not active for $className")
                        Result.success(SessionCheckResult(
                            isActive = false,
                            session = session,
                            message = "Session not active"
                        ))
                    }
                } else {
                    Timber.w("⚠️ Session document exists but data is null for $className")
                    Result.success(SessionCheckResult(
                        isActive = false,
                        message = "Session data is null"
                    ))
                }
            } else {
                Timber.d("⚪ No session document found for $className")
                Result.success(SessionCheckResult(
                    isActive = false,
                    message = "No session found"
                ))
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to check active session for $className")
            Result.failure(e)
        }
    }

    // Add this enhanced session checking method to your FirebaseRepository.kt

    /**
     * Check if there's an active session for the student's class (with enhanced debugging)
     */

    /**
     * Check if attendance already marked today for student in subject
     */
    // In FirebaseRepository.kt - Replace the existing method
    suspend fun isAttendanceAlreadyMarked(
        rollNumber: String,
        subject: String,
        group: String,
        type: String,
        isExtra: Boolean = false
    ): Result<Boolean> {
        return try {
            val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            Timber.d("🔍 Checking if attendance already marked for $rollNumber in $subject on $today (isExtra: $isExtra)")

            val attendanceCollection = getCurrentAttendanceCollection()
            val query = attendanceCollection
                .whereEqualTo("rollNumber", rollNumber)
                .whereEqualTo("subject", subject)
                .whereEqualTo("group", group)
                .whereEqualTo("type", type)
                .whereEqualTo("date", today)
                .whereEqualTo("isExtra", isExtra)
                .limit(1)

            val documents = query.get().await()
            val alreadyMarked = !documents.isEmpty

            Timber.d("✅ Attendance already marked check: $alreadyMarked (found ${documents.size()} records) isExtra: $isExtra")
            Result.success(alreadyMarked)
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to check existing attendance for $rollNumber")
            Result.failure(e)
        }
    }

    /**
     * Mark attendance in the new structure
     */
    // In FirebaseRepository.kt - Replace the existing method
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
            Timber.d("🚀 Starting Firebase attendance marking process (isExtra: $isExtra)")
            Timber.d("📋 Details: $rollNumber, $subject, $group, $type")

            val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            // Check if already marked today for this type (regular or extra)
            val alreadyMarkedResult = isAttendanceAlreadyMarked(rollNumber, subject, group, type, isExtra)
            if (alreadyMarkedResult.isFailure) {
                return Result.success(AttendanceResponse(
                    success = false,
                    message = "Failed to verify existing attendance"
                ))
            }

            if (alreadyMarkedResult.getOrNull() == true) {
                val attendanceType = if (isExtra) "extra" else "regular"
                Timber.w("⚠️ $attendanceType attendance already marked for today")
                return Result.success(AttendanceResponse(
                    success = false,
                    message = "${attendanceType.capitalize()} attendance already marked for today",
                    alreadyMarked = true
                ))
            }

            // Create attendance record with isExtra field
            val currentTime = Timestamp.now()
            val attendanceRecord = AttendanceRecord(
                date = today,
                rollNumber = rollNumber,
                subject = subject,
                group = group,
                type = type,
                present = true,
                timestamp = currentTime,
                deviceRoom = deviceRoom,
                isExtra = isExtra
            )

            Timber.d("📝 Created attendance record: $attendanceRecord")

            // Save to current month's collection with auto-generated ID
            val attendanceCollection = getCurrentAttendanceCollection()
            val documentRef = attendanceCollection.add(attendanceRecord).await()
            val attendanceId = documentRef.id

            val attendanceType = if (isExtra) "extra" else "regular"
            Timber.i("🎉 $attendanceType attendance marked successfully: $attendanceId for $rollNumber")

            Result.success(AttendanceResponse(
                success = true,
                message = "${attendanceType.capitalize()} attendance marked successfully",
                attendanceId = attendanceId
            ))

        } catch (e: Exception) {
            Timber.e(e, "💥 Failed to mark attendance in Firebase")
            Result.success(AttendanceResponse(
                success = false,
                message = "Firebase error: ${e.message}"
            ))
        }
    }

    /**
     * Get attendance history for a student (from current and previous months)
     */
    suspend fun getAttendanceHistory(
        rollNumber: String,
        limit: Int = 50
    ): Result<List<AttendanceRecord>> {
        return try {
            Timber.d("📚 Fetching attendance history for $rollNumber (limit: $limit)")

            val attendanceList = mutableListOf<AttendanceRecord>()
            val now = LocalDateTime.now()

            // Get attendance from current month
            var currentCollection = getAttendanceCollection(now.year, now.monthValue)
            var query = currentCollection
                .whereEqualTo("rollNumber", rollNumber)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            var documents = query.get().await()
            attendanceList.addAll(documents.toObjects(AttendanceRecord::class.java))

            // If we need more records and haven't reached the limit, get from previous month
            if (attendanceList.size < limit) {
                val previousMonth = now.minusMonths(1)
                val remainingLimit = limit - attendanceList.size

                currentCollection = getAttendanceCollection(previousMonth.year, previousMonth.monthValue)
                query = currentCollection
                    .whereEqualTo("rollNumber", rollNumber)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(remainingLimit.toLong())

                try {
                    documents = query.get().await()
                    attendanceList.addAll(documents.toObjects(AttendanceRecord::class.java))
                } catch (e: Exception) {
                    Timber.w("⚠️ Previous month collection might not exist: ${e.message}")
                }
            }

            // Sort by timestamp descending
            attendanceList.sortByDescending { it.timestamp }

            Timber.d("✅ Retrieved ${attendanceList.size} attendance records for $rollNumber")
            Result.success(attendanceList)
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to get attendance history for $rollNumber")
            Result.failure(e)
        }
    }

    /**
     * Get attendance statistics for a student
     */
    suspend fun getAttendanceStats(rollNumber: String, subject: String? = null): Result<AttendanceStats> {
        return try {
            Timber.d("📊 Calculating attendance stats for $rollNumber" + if (subject != null) " in $subject" else "")

            val attendanceList = mutableListOf<AttendanceRecord>()
            val now = LocalDateTime.now()

            // Get attendance from current and previous months for more comprehensive stats
            for (monthOffset in 0..2) { // Current month and 2 previous months
                val targetMonth = now.minusMonths(monthOffset.toLong())
                val collection = getAttendanceCollection(targetMonth.year, targetMonth.monthValue)

                var query = collection.whereEqualTo("rollNumber", rollNumber)
                if (subject != null) {
                    query = query.whereEqualTo("subject", subject)
                }

                try {
                    val documents = query.get().await()
                    attendanceList.addAll(documents.toObjects(AttendanceRecord::class.java))
                } catch (e: Exception) {
                    Timber.w("⚠️ Collection for ${targetMonth.year}-${targetMonth.monthValue} might not exist")
                }
            }

            val totalClasses = attendanceList.size
            val presentClasses = attendanceList.count { it.present }
            val percentage = if (totalClasses > 0) {
                (presentClasses.toFloat() / totalClasses.toFloat() * 100)
            } else 0f

            val stats = AttendanceStats(
                totalClasses = totalClasses,
                presentClasses = presentClasses,
                absentClasses = totalClasses - presentClasses,
                attendancePercentage = percentage
            )

            Timber.d("✅ Calculated attendance stats for $rollNumber: $stats")
            Result.success(stats)
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to get attendance stats for $rollNumber")
            Result.failure(e)
        }
    }

    /**
     * Save or update student profile (keeping for compatibility)
     */
    suspend fun saveStudentProfile(student: Student): Result<String> {
        return try {
            Timber.d("👤 Saving student profile: ${student.rollNumber}")

            val studentsCollection = firestore.collection("students")
            studentsCollection.document(student.rollNumber)
                .set(student.copy(updatedAt = Timestamp.now()))
                .await()

            Timber.d("✅ Student profile saved successfully: ${student.rollNumber}")
            Result.success("Profile saved successfully")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to save student profile: ${student.rollNumber}")
            Result.failure(e)
        }
    }

    /**
     * Update student's face ID after face registration
     */
    suspend fun updateStudentFaceId(rollNumber: String, faceId: String): Result<String> {
        return try {
            Timber.d("🆔 Updating face ID for student: $rollNumber")

            val studentsCollection = firestore.collection("students")
            studentsCollection.document(rollNumber)
                .update(
                    mapOf(
                        "faceId" to faceId,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()

            Timber.d("✅ Face ID updated for student: $rollNumber")
            Result.success("Face ID updated successfully")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to update face ID for $rollNumber")
            Result.failure(e)
        }
    }
}