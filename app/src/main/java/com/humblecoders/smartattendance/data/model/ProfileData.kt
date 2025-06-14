package com.humblecoders.smartattendance.data.model



data class ProfileData(
    val name: String = "",
    val rollNumber: String = "",
    val className: String = "", // New field for class like "2S12", "3E15"
    val isFaceRegistered: Boolean = false
)