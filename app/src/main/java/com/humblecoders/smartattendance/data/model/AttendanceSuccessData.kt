package com.humblecoders.smartattendance.data.model

/**
 * Data class to hold attendance success information for display
 */
data class AttendanceSuccessData(
    val rollNumber: String,
    val studentName: String,
    val subject: String,
    val room: String,
    val type: String, // "lect", "lab", "tut"
    val deviceRoom: String = "", // Full BLE device name with digits
    val attendanceId: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Get formatted type for display
     */


    /**
     * Get formatted device room (without digits for display)
     */
    fun getFormattedDeviceRoom(): String {
        return if (deviceRoom.length >= 3) {
            val suffix = deviceRoom.takeLast(3)
            if (suffix.all { it.isDigit() }) {
                deviceRoom.dropLast(3)
            } else {
                deviceRoom
            }
        } else {
            deviceRoom
        }
    }

    /**
     * Check if device room matches session room
     */

}