package com.humblecoders.smartattendance.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// FIXED: ActiveSession without default values for better Firestore deserialization
data class ActiveSession(
    @PropertyName("isActive") val isActive: Boolean,
    @PropertyName("subject") val subject: String,
    @PropertyName("room") val room: String,
    @PropertyName("type") val type: String, // "lect", "lab", "tut"
    @PropertyName("sessionId") val sessionId: String,
    @PropertyName("date") val date: String, // YYYY-MM-DD format
    @PropertyName("isExtra") val isExtra: Boolean  // NEW FIELD

) {
    // No-argument constructor required by Firestore
    constructor() : this(
        isActive = false,
        subject = "",
        room = "",
        type = "",
        sessionId = "",
        date = "",
        isExtra = false

    )
}



// Keep the rest of your models unchanged
data class AttendanceRecord(
    @PropertyName("date") val date: String = "",
    @PropertyName("rollNumber") val rollNumber: String = "",
    @PropertyName("subject") val subject: String = "",
    @PropertyName("group") val group: String = "", // class group like "2S12"
    @PropertyName("type") val type: String = "", // "lect", "lab", "tut"
    @PropertyName("present") val present: Boolean = true,
    @PropertyName("timestamp") val timestamp: Timestamp? = null,
    @PropertyName("deviceRoom") val deviceRoom: String = "" ,// BLE device name with 3 digits
    @PropertyName("isExtra") val isExtra: Boolean = false // NEW FIELD

) {
    // Helper function to check if attendance is for today
    fun isToday(): Boolean {
        val today = LocalDateTime.now().toLocalDate().toString()
        return date == today
    }

    // Helper function to get formatted time
    fun getFormattedTime(): String {
        return timestamp?.let {
            try {
                val instant = it.toDate().toInstant()
                val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                localDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            } catch (e: Exception) {
                "Unknown"
            }
        } ?: "Unknown"
    }
}

// Updated Student model (keeping existing for compatibility)
data class Student(
    @PropertyName("rollNumber") val rollNumber: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("email") val email: String = "",
    @PropertyName("faceId") val faceId: String = "",
    @PropertyName("createdAt") val createdAt: Timestamp = Timestamp.now(),
    @PropertyName("updatedAt") val updatedAt: Timestamp = Timestamp.now()
)

// Data class for session check result
data class SessionCheckResult(
    val isActive: Boolean,
    val session: ActiveSession? = null,
    val message: String = ""
)

// Data class for attendance marking response
data class AttendanceResponse(
    val success: Boolean,
    val message: String,
    val attendanceId: String? = null,
    val alreadyMarked: Boolean = false
)

// Data class for attendance statistics (updated for new structure)
data class AttendanceStats(
    val totalClasses: Int,
    val presentClasses: Int,
    val absentClasses: Int,
    val attendancePercentage: Float
)