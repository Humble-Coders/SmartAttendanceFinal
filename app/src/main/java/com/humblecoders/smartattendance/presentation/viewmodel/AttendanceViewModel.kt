package com.humblecoders.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.humblecoders.smartattendance.data.repository.AttendanceRepository
import com.humblecoders.smartattendance.data.repository.ProfileRepository

class AttendanceViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {
    // Attendance functionality will be implemented later
}