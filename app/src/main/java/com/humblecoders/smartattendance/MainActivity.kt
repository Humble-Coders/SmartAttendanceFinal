package com.humblecoders.smartattendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
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

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Initialize Firebase
        initializeFirebase()

        // Initialize repositories
        initializeRepositories()

        // Initialize ViewModels
        initializeViewModels()

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
            Timber.d("🔥 Firebase initialized successfully")

            // Test Firebase connection (optional)
            testFirebaseConnection()

        } catch (e: Exception) {
            Timber.e(e, "🔥 Failed to initialize Firebase")
        }
    }

    private fun initializeRepositories() {
        try {
            bleRepository = BleRepository(applicationContext)
            profileRepository = ProfileRepository(applicationContext)
            attendanceRepository = AttendanceRepository()

            Timber.d("📦 All repositories initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "📦 Failed to initialize repositories")
        }
    }

    private fun initializeViewModels() {
        try {
            // Initialize ViewModels with repositories
            bleViewModel = BleViewModel(bleRepository)
            profileViewModel = ProfileViewModel(profileRepository)
            attendanceViewModel = AttendanceViewModel(attendanceRepository, profileRepository)

            Timber.d("🎭 All ViewModels initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "🎭 Failed to initialize ViewModels")
        }
    }

    private fun testFirebaseConnection() {
        lifecycleScope.launch {
            try {
                // This is a simple test to ensure Firebase is working
                Timber.d("🔥 Testing Firebase connection...")

                // The actual connection test will happen when we try to read/write data
                Timber.d("🔥 Firebase connection test completed")

            } catch (e: Exception) {
                Timber.e(e, "🔥 Firebase connection test failed")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.d("📱 MainActivity: onResume - App became active")

        // Reinitialize BLE if needed
        if (::bleViewModel.isInitialized) {
            bleViewModel.initializeBle()
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.d("📱 MainActivity: onPause - App going to background")

        // Optionally pause BLE scanning to save battery
        if (::bleViewModel.isInitialized) {
            bleViewModel.stopScanning()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("📱 MainActivity: onDestroy - Cleaning up resources")

        // Clean up BLE scanning
        if (::bleViewModel.isInitialized) {
            bleViewModel.stopScanning()
        }

        // Clear attendance data
        if (::attendanceViewModel.isInitialized) {
            attendanceViewModel.clearAttendanceData()
        }
    }

    /**
     * Handle back button press
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        Timber.d("📱 MainActivity: Back button pressed")

        // Let the navigation component handle back navigation
        // The new navigation system will handle this automatically
        super.onBackPressed()
    }
}