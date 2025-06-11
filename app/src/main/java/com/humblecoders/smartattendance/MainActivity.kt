package com.humblecoders.smartattendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.humblecoders.smartattendance.data.repository.AttendanceRepository
import com.humblecoders.smartattendance.data.repository.BleRepository
import com.humblecoders.smartattendance.data.repository.ProfileRepository
import com.humblecoders.smartattendance.presentation.viewmodel.AttendanceViewModel
import com.humblecoders.smartattendance.presentation.viewmodel.BleViewModel
import com.humblecoders.smartattendance.presentation.viewmodel.ProfileViewModel
import com.humblecoders.smartattendance.presentation.navigation.AppNavigation
import com.humblecoders.smartattendance.ui.theme.SmartAttendanceTheme

class MainActivity : ComponentActivity() {

    // Late init repositories
    private lateinit var bleRepository: BleRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var attendanceRepository: AttendanceRepository

    // Late init ViewModels
    lateinit var bleViewModel: BleViewModel
    lateinit var profileViewModel: ProfileViewModel
    lateinit var attendanceViewModel: AttendanceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize repositories
        bleRepository = BleRepository(applicationContext)
        profileRepository = ProfileRepository(applicationContext)
        attendanceRepository = AttendanceRepository()

        // Initialize ViewModels with repositories
        bleViewModel = BleViewModel(bleRepository)
        profileViewModel = ProfileViewModel(profileRepository)
        attendanceViewModel = AttendanceViewModel(attendanceRepository, profileRepository)

        setContent {
            SmartAttendanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        bleViewModel = bleViewModel,
                        profileViewModel = profileViewModel,
                        attendanceViewModel = attendanceViewModel
                    )
                }
            }
        }
    }
}