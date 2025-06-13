// Replace your existing MainActivity.kt with this:

package com.humblecoders.smartattendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.humblecoders.smartattendance.data.repository.AttendanceRepository
import com.humblecoders.smartattendance.data.repository.BleRepository
import com.humblecoders.smartattendance.data.repository.ProfileRepository
import com.humblecoders.smartattendance.presentation.viewmodel.AttendanceViewModel
import com.humblecoders.smartattendance.presentation.viewmodel.BleViewModel
import com.humblecoders.smartattendance.presentation.viewmodel.ProfileViewModel
import com.humblecoders.smartattendance.presentation.navigation.AppNavigation
import com.humblecoders.smartattendance.ui.theme.SmartAttendanceTheme
import kotlinx.coroutines.launch
import timber.log.Timber

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

        // Initialize Firebase
        initializeFirebase()

        // Initialize repositories
        bleRepository = BleRepository(applicationContext)
        profileRepository = ProfileRepository(applicationContext)
        attendanceRepository = AttendanceRepository()

        // Initialize ViewModels with repositories
        bleViewModel = BleViewModel(bleRepository)
        profileViewModel = ProfileViewModel(profileRepository)
        attendanceViewModel = AttendanceViewModel(attendanceRepository, profileRepository)

        // Initialize attendance ViewModel
        attendanceViewModel.initialize()

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

    private fun initializeFirebase() {
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)
            Timber.d("Firebase initialized successfully")

            // Test Firebase connection (optional)
            testFirebaseConnection()

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Firebase")
        }
    }

    private fun testFirebaseConnection() {
        lifecycleScope.launch {
            try {
                // This is a simple test to ensure Firebase is working
                // You can remove this in production
                Timber.d("Testing Firebase connection...")

                // The actual connection test will happen when we try to write data
                Timber.d("Firebase connection test completed")

            } catch (e: Exception) {
                Timber.e(e, "Firebase connection test failed")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up BLE scanning
        if (::bleViewModel.isInitialized) {
            bleViewModel.stopScanning()
        }
    }
}