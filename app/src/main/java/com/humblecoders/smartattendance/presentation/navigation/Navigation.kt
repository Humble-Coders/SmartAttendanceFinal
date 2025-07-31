package com.humblecoders.smartattendance.presentation.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.humblecoders.smartattendance.presentation.screens.*
import com.humblecoders.smartattendance.presentation.viewmodel.AttendanceViewModel
import com.humblecoders.smartattendance.presentation.viewmodel.BleViewModel
import com.humblecoders.smartattendance.presentation.viewmodel.ProfileViewModel
import com.humblecoders.smartattendance.data.model.AttendanceSuccessData
import com.humblecoders.smartattendance.utils.BluetoothManager
import timber.log.Timber

@Composable
fun AppNavigation(
    bleViewModel: BleViewModel,
    profileViewModel: ProfileViewModel,
    attendanceViewModel: AttendanceViewModel,
    bluetoothManager: BluetoothManager
) {
    val navController = rememberNavController()

    // Always start with splash screen
    val startDestination = Screen.Splash.route

    Timber.d("🧭 Navigation: Starting with splash screen")

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            Timber.d("🧭 Navigating to Splash Screen")
            SplashScreen(
                profileViewModel = profileViewModel,
                onNavigateToLogin = {
                    Timber.d("🧭 Splash -> Login")
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    Timber.d("🧭 Splash -> Home")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Login Screen
        composable(Screen.Login.route) {
            Timber.d("🧭 Navigating to Login Screen")
            LoginScreen(
                profileViewModel = profileViewModel,
                onLoginSuccess = {
                    Timber.d("🧭 Login successful, navigating to Home")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Home Screen
        composable(Screen.Home.route) {
            Timber.d("🧭 Navigating to Home Screen")
            StudentHomeScreen(
                bleViewModel = bleViewModel,
                profileViewModel = profileViewModel,
                attendanceViewModel = attendanceViewModel,
                bluetoothManager = bluetoothManager,
                onAttendanceClick = {
                    Timber.d("🧭 Navigating to Attendance Marking from Home")
                    navController.navigate(Screen.AttendanceMarking.route)
                },
                onLogout = {
                    Timber.d("🧭 Logout triggered, navigating to Splash")
                    // Clear all ViewModels data
                    attendanceViewModel.clearAttendanceData()
                    bleViewModel.stopScanning()

                    navController.navigate(Screen.Splash.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Profile Screen
        composable(Screen.Profile.route) {
            Timber.d("🧭 Navigating to Profile Screen")
            ProfileScreen(
                profileViewModel = profileViewModel,
                onNavigateBack = {
                    Timber.d("🧭 Navigating back from Profile")
                    navController.popBackStack()
                }
            )
        }

        // Attendance Marking Screen
        composable(Screen.AttendanceMarking.route,
                arguments = listOf(
                navArgument("deviceRoom") {
                    type = NavType.StringType
                    defaultValue = ""
                }
                )
        ) {
                backStackEntry ->
            val deviceRoom = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("deviceRoom") ?: "",
                "UTF-8"
            ).let { if (it == "unknown") "" else it }

            Timber.d("🧭 Navigating to Attendance Marking Screen with device room: $deviceRoom")
            AttendanceMarkingScreen(
                presetDeviceRoom = deviceRoom,
                attendanceViewModel = attendanceViewModel,
                profileViewModel = profileViewModel,
                bleViewModel = bleViewModel,
                onNavigateBack = {
                    Timber.d("🧭 Navigating back from Attendance Marking")
                    navController.popBackStack()
                },
                onNavigateToSuccess = { successData ->
                    Timber.d("🧭 Navigating to Success Screen with data: ${successData.rollNumber}:DeviceRoom: ${successData.deviceRoom}")

                    val route = Screen.AttendanceSuccess.createRoute(
                        rollNumber = successData.rollNumber,
                        subject = successData.subject,
                        room = successData.room,
                        type = successData.type,
                        deviceRoom = successData.deviceRoom,
                        attendanceId = successData.attendanceId
                    )

                    navController.navigate(route) {
                        popUpTo(Screen.AttendanceMarking.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.FaceRegistration.route,
            arguments = listOf(
                navArgument("rollNumber") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val rollNumber = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("rollNumber") ?: "",
                "UTF-8"
            )

            Timber.d("🧭 Navigating to Face Registration Screen for: $rollNumber")

            FaceRegistrationScreen(
                rollNumber = rollNumber,
                onRegistrationSuccess = { faceId ->
                    Timber.d("✅ Face registration successful, navigating back to login")
                    navController.popBackStack()
                },
                onRegistrationError = { error ->
                    Timber.e("❌ Face registration failed: $error")
                    navController.popBackStack()
                },
                onNavigateBack = {
                    Timber.d("🔙 Navigating back from face registration")
                    navController.popBackStack()
                }
            )
        }


        // Attendance Success Screen
        composable(
            route = Screen.AttendanceSuccess.route,
            arguments = listOf(
                navArgument("rollNumber") {
                    type = NavType.StringType
                    defaultValue = "unknown"
                },
                navArgument("subject") {
                    type = NavType.StringType
                    defaultValue = "unknown"
                },
                navArgument("room") {
                    type = NavType.StringType
                    defaultValue = "unknown"
                },
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = "unknown"
                },
                navArgument("deviceRoom") {
                    type = NavType.StringType
                    defaultValue = "unknown"
                },
                navArgument("attendanceId") {
                    type = NavType.StringType
                    defaultValue = "unknown"
                }
            )
        ) { backStackEntry ->
            val rollNumber = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("rollNumber") ?: "unknown",
                "UTF-8"
            )
            val subject = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("subject") ?: "unknown",
                "UTF-8"
            )
            val room = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("room") ?: "unknown",
                "UTF-8"
            )
            val type = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("type") ?: "unknown",
                "UTF-8"
            )
            val deviceRoom = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("deviceRoom") ?: "unknown",
                "UTF-8"
            )
            val attendanceId = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("attendanceId") ?: "unknown",
                "UTF-8"
            )

            Timber.d("🧭 Navigating to Success Screen")
            Timber.d("📋 Success data - Roll: $rollNumber, Subject: $subject, Room: $room")

            AttendanceSuccessScreen(
                rollNumber = rollNumber,
                subject = subject,
                room = room,
                type = type,
                deviceRoom = deviceRoom,
                onBackToHome = {
                    Timber.d("🧭 Navigating back to Home from Success")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                attendanceViewModel = attendanceViewModel
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object Profile : Screen("profile")
    // In Screen sealed class, update AttendanceMarking to accept device room
    object AttendanceMarking : Screen("attendance_marking/{deviceRoom}") {
        fun createRoute(deviceRoom: String = ""): String {
            val encodedDeviceRoom = java.net.URLEncoder.encode(deviceRoom.ifBlank { "unknown" }, "UTF-8")
            return "attendance_marking/$encodedDeviceRoom"
        }
    }
    object AttendanceSuccess : Screen(
        "attendance_success/{rollNumber}/{subject}/{room}/{type}/{deviceRoom}/{attendanceId}"
    ) {
        fun createRoute(
            rollNumber: String,
            subject: String,
            room: String,
            type: String,
            deviceRoom: String,
            attendanceId: String
        ): String {
            val encodedRollNumber = java.net.URLEncoder.encode(rollNumber.ifBlank { "unknown" }, "UTF-8")
            val encodedSubject = java.net.URLEncoder.encode(subject.ifBlank { "unknown" }, "UTF-8")
            val encodedRoom = java.net.URLEncoder.encode(room.ifBlank { "unknown" }, "UTF-8")
            val encodedType = java.net.URLEncoder.encode(type.ifBlank { "unknown" }, "UTF-8")
            val encodedDeviceRoom = java.net.URLEncoder.encode(deviceRoom.ifBlank { "unknown" }, "UTF-8")
            val encodedAttendanceId = java.net.URLEncoder.encode(attendanceId.ifBlank { "unknown" }, "UTF-8")

            return "attendance_success/$encodedRollNumber/$encodedSubject/$encodedRoom/$encodedType/$encodedDeviceRoom/$encodedAttendanceId"
        }
    }
    object FaceRegistration : Screen("face_registration/{rollNumber}") {
        fun createRoute(rollNumber: String): String {
            return "face_registration/${java.net.URLEncoder.encode(rollNumber, "UTF-8")}"
        }
    }
}