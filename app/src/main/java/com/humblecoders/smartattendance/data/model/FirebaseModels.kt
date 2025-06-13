
package com.humblecoders.smartattendance.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.time.LocalDateTime
import java.time.ZoneId

data class Student(
    @PropertyName("rollNumber") val rollNumber: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("email") val email: String = "",
    @PropertyName("faceId") val faceId: String = "",
    @PropertyName("createdAt") val createdAt: Timestamp = Timestamp.now(),
    @PropertyName("updatedAt") val updatedAt: Timestamp = Timestamp.now()
)

data class Subject(
    @PropertyName("code") val code: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("isActive") val isActive: Boolean = false,
    @PropertyName("teacher") val teacher: String = "",
    @PropertyName("room") val room: String = "",
    @PropertyName("createdAt") val createdAt: Timestamp = Timestamp.now()
)

data class AttendanceRecord(
    @PropertyName("id") val id: String = "",
    @PropertyName("rollNumber") val rollNumber: String = "",
    @PropertyName("studentName") val studentName: String = "",
    @PropertyName("subjectCode") val subjectCode: String = "",
    @PropertyName("timestamp") val timestamp: Timestamp = Timestamp.now(),
    @PropertyName("date") val date: String = "",
    @PropertyName("status") val status: String = "PRESENT",
    @PropertyName("verificationMethod") val verificationMethod: String = "FACE_RECOGNITION",
    @PropertyName("location") val location: String = "",
    @PropertyName("deviceId") val deviceId: String = ""
) {
    // Helper function to convert to local date string
    fun getLocalDateString(): String {
        return try {
            val instant = timestamp.toDate().toInstant()
            val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            localDateTime.toLocalDate().toString()
        } catch (e: Exception) {
            date
        }
    }

    // Helper function to check if attendance is for today
    fun isToday(): Boolean {
        val today = LocalDateTime.now().toLocalDate().toString()
        return getLocalDateString() == today
    }
}

// Request/Response models for API calls
data class MarkAttendanceRequest(
    val rollNumber: String,
    val subjectCode: String,
    val studentName: String,
    val verificationMethod: String = "FACE_RECOGNITION",
    val location: String = ""
)

data class AttendanceResponse(
    val success: Boolean,
    val message: String,
    val attendanceId: String? = null,
    val alreadyMarked: Boolean = false
)