package com.humblecoders.smartattendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.google.firebase.FirebaseApp
import com.humblecoders.smartattendance.data.repository.AdminToggleRepository
import com.humblecoders.smartattendance.data.repository.AttendanceRepository
import com.humblecoders.smartattendance.data.repository.BleRepository
import com.humblecoders.smartattendance.data.repository.ProfileRepository
import com.humblecoders.smartattendance.presentation.viewmodel.AttendanceViewModel
import com.humblecoders.smartattendance.presentation.viewmodel.BleViewModel
import com.humblecoders.smartattendance.presentation.viewmodel.ProfileViewModel
import com.humblecoders.smartattendance.presentation.navigation.AppNavigation
import com.humblecoders.smartattendance.ui.theme.SmartAttendanceTheme
import com.humblecoders.smartattendance.utils.BluetoothManager
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : ComponentActivity() {

    // Late init repositories
    private lateinit var bleRepository: BleRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var attendanceRepository: AttendanceRepository
    private lateinit var adminToggleRepository: AdminToggleRepository

    // Late init ViewModels
    lateinit var bleViewModel: BleViewModel
    lateinit var profileViewModel: ProfileViewModel
    lateinit var attendanceViewModel: AttendanceViewModel

    // Bluetooth Manager
    private lateinit var bluetoothManager: BluetoothManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install and immediately dismiss splash screen
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }

        super.onCreate(savedInstanceState)

        // Switch to main theme after splash
        setTheme(R.style.Theme_SmartAttendance)

        // Enable edge-to-edge display
        enableEdgeToEdge()
        

        // Initialize Firebase
        initializeFirebase()

        // Initialize Bluetooth Manager
        initializeBluetoothManager()

        // Initialize repositories
        initializeRepositories()

        // Initialize ViewModels
        initializeViewModels()

        // Initialize attendance ViewModel
        attendanceViewModel.initialize()

        setContent {
            SmartAttendanceTheme {
                val isDarkMode = isSystemInDarkTheme()
                LaunchedEffect(Unit) {
                    try {
                        val statusBarColor = if (isDarkMode) {
                            Color(0xED062970).toArgb() // Dark background color
                        } else {
                            Color(0x955A9DEE).toArgb() // Your light mode color
                        }

                        window.statusBarColor = statusBarColor
                        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !isDarkMode

                        Timber.d("🎨 Status bar color set in Compose - Dark mode: $isDarkMode")
                    } catch (e: Exception) {
                        Timber.e(e, "🎨 Failed to set status bar color in Compose")
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        bleViewModel = bleViewModel,
                        profileViewModel = profileViewModel,
                        attendanceViewModel = attendanceViewModel,
                        bluetoothManager = bluetoothManager
                    )
                }
            }
        }
    }

    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            Timber.d("🔥 Firebase initialized successfully")
            testFirebaseConnection()
        } catch (e: Exception) {
            Timber.e(e, "🔥 Failed to initialize Firebase")
        }
    }

    private fun initializeBluetoothManager() {
        try {
            bluetoothManager = BluetoothManager(this)
            Timber.d("📡 BluetoothManager initialized successfully")
            Timber.d("📡 ${bluetoothManager.getBluetoothStateSummary()}")
        } catch (e: Exception) {
            Timber.e(e, "📡 Failed to initialize BluetoothManager")
        }
    }

    private fun initializeRepositories() {
        try {
            bleRepository = BleRepository(applicationContext)
            profileRepository = ProfileRepository(applicationContext)
            attendanceRepository = AttendanceRepository()
            adminToggleRepository = AdminToggleRepository()

            Timber.d("📦 All repositories initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "📦 Failed to initialize repositories")
        }
    }

    private fun initializeViewModels() {
        try {
            bleViewModel = BleViewModel(bleRepository)
            profileViewModel = ProfileViewModel(profileRepository)
            attendanceViewModel = AttendanceViewModel(attendanceRepository, profileRepository, adminToggleRepository)

            Timber.d("🎭 All ViewModels initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "🎭 Failed to initialize ViewModels")
        }
    }

    private fun testFirebaseConnection() {
        lifecycleScope.launch {
            try {
                Timber.d("🔥 Testing Firebase connection...")
                Timber.d("🔥 Firebase connection test completed")
            } catch (e: Exception) {
                Timber.e(e, "🔥 Firebase connection test failed")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.d("📱 MainActivity: onResume - App became active")

        if (::bluetoothManager.isInitialized) {
            Timber.d("📡 ${bluetoothManager.getBluetoothStateSummary()}")
        }

        if (::bleViewModel.isInitialized) {
            bleViewModel.initializeBle()
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.d("📱 MainActivity: onPause - App going to background")

        if (::bleViewModel.isInitialized) {
            bleViewModel.stopScanning()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("📱 MainActivity: onDestroy - Cleaning up resources")

        if (::bleViewModel.isInitialized) {
            bleViewModel.stopScanning()
        }

        if (::attendanceViewModel.isInitialized) {
            attendanceViewModel.clearAttendanceData()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        Timber.d("📱 MainActivity: Back button pressed")
        super.onBackPressed()
    }
}