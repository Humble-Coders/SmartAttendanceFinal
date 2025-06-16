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
            Timber.d("📋 Details: $rollNumber, $subject, $group, $type , $deviceRoom, isExtra: $isExtra")

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

            // Create attendance record with ALL required fields in correct format
            val currentTime = Timestamp.now()
            val attendanceData = mapOf(
                "date" to today,
                "rollNumber" to rollNumber,
                "subject" to subject,
                "group" to group,
                "type" to type,
                "present" to true,
                "timestamp" to currentTime,
                "deviceRoom" to deviceRoom,
                "isExtra" to isExtra
            )

            Timber.d("📝 Created attendance data: $attendanceData")

            // Save to current month's collection with auto-generated ID
            val attendanceCollection = getCurrentAttendanceCollection()
            val documentRef = attendanceCollection.add(attendanceData).await()
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

            // Strategy: Get all documents from collections and filter/sort in memory
            // This avoids needing composite indexes

            // Check current month
            var currentCollection = getAttendanceCollection(now.year, now.monthValue)
            Timber.d("📂 Checking current month collection: ${currentCollection.path}")

            try {
                // Simple query - only filter by rollNumber (single field index exists by default)
                val currentMonthQuery = currentCollection
                    .whereEqualTo("rollNumber", rollNumber)
                    .get()
                    .await()

                val currentMonthRecords = currentMonthQuery.toObjects(AttendanceRecord::class.java)
                attendanceList.addAll(currentMonthRecords)
                Timber.d("📊 Current month: ${currentMonthRecords.size} records")
            } catch (e: Exception) {
                Timber.w("⚠️ Current month query failed: ${e.message}")
            }

            // Check previous month if we need more records
            if (attendanceList.size < limit) {
                val previousMonth = now.minusMonths(1)
                val previousCollection = getAttendanceCollection(previousMonth.year, previousMonth.monthValue)
                Timber.d("📂 Checking previous month collection: ${previousCollection.path}")

                try {
                    val previousMonthQuery = previousCollection
                        .whereEqualTo("rollNumber", rollNumber)
                        .get()
                        .await()

                    val previousMonthRecords = previousMonthQuery.toObjects(AttendanceRecord::class.java)
                    attendanceList.addAll(previousMonthRecords)
                    Timber.d("📊 Previous month: ${previousMonthRecords.size} records")
                } catch (e: Exception) {
                    Timber.w("⚠️ Previous month collection might not exist: ${e.message}")
                }
            }

            // Check month before previous if we still need more records
            if (attendanceList.size < limit) {
                val twoMonthsAgo = now.minusMonths(2)
                val oldCollection = getAttendanceCollection(twoMonthsAgo.year, twoMonthsAgo.monthValue)
                Timber.d("📂 Checking two months ago collection: ${oldCollection.path}")

                try {
                    val oldMonthQuery = oldCollection
                        .whereEqualTo("rollNumber", rollNumber)
                        .get()
                        .await()

                    val oldMonthRecords = oldMonthQuery.toObjects(AttendanceRecord::class.java)
                    attendanceList.addAll(oldMonthRecords)
                    Timber.d("📊 Two months ago: ${oldMonthRecords.size} records")
                } catch (e: Exception) {
                    Timber.w("⚠️ Two months ago collection might not exist: ${e.message}")
                }
            }

            // Sort in memory by timestamp descending (newest first)
            val sortedList = attendanceList
                .filter { it.timestamp != null } // Filter out records without timestamp
                .sortedByDescending { it.timestamp!!.toDate().time }
                .take(limit) // Take only the required number

            Timber.d("✅ Total retrieved and sorted: ${sortedList.size} attendance records for $rollNumber")

            // Log each record for debugging
            sortedList.forEachIndexed { index, record ->
                Timber.d("📝 Record $index: ${record.subject} on ${record.date} at ${record.getFormattedTime()}")
            }

            Result.success(sortedList)
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

            // Get attendance from current and previous months for comprehensive stats
            for (monthOffset in 0..3) { // Current month and 3 previous months
                val targetMonth = now.minusMonths(monthOffset.toLong())
                val collection = getAttendanceCollection(targetMonth.year, targetMonth.monthValue)

                try {
                    // Simple query without orderBy to avoid index requirements
                    val query = collection
                        .whereEqualTo("rollNumber", rollNumber)
                        .get()
                        .await()

                    val monthRecords = query.toObjects(AttendanceRecord::class.java)
                    attendanceList.addAll(monthRecords)

                    Timber.d("📊 Month ${targetMonth.year}-${targetMonth.monthValue}: ${monthRecords.size} records")
                } catch (e: Exception) {
                    Timber.w("⚠️ Collection for ${targetMonth.year}-${targetMonth.monthValue} might not exist")
                }
            }

            // Filter by subject if specified (done in memory)
            val filteredList = if (subject != null) {
                attendanceList.filter { it.subject.equals(subject, ignoreCase = true) }
            } else {
                attendanceList
            }

            val totalClasses = filteredList.size
            val presentClasses = filteredList.count { it.present }
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