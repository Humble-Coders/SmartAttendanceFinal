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
    bluetoothManager: BluetoothManager // ADD this parameter
) {
    val navController = rememberNavController()
    val profileData by profileViewModel.profileData.collectAsState()

    // Determine start destination based on profile state
    val startDestination = if (profileData.name.isNotBlank() &&
        profileData.rollNumber.isNotBlank() &&
        profileData.className.isNotBlank()) {
        Screen.Home.route
    } else {
        Screen.Login.route
    }

    // Log navigation state changes
    LaunchedEffect(profileData) {
        Timber.d("🧭 Navigation: Profile state changed")
        Timber.d("📋 Profile complete: ${profileData.name.isNotBlank() && profileData.rollNumber.isNotBlank() && profileData.className.isNotBlank()}")
        Timber.d("🎯 Start destination: $startDestination")
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Login Screen
        composable(Screen.Login.route) {
            Timber.d("🧭 Navigating to Login Screen")
            LoginScreen(
                profileViewModel = profileViewModel,
                onLoginSuccess = {
                    Timber.d("🧭 Login successful, navigating to Home")
                    navController.navigate(Screen.Home.route) {
                        // Clear login from back stack
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
                bluetoothManager = bluetoothManager, // ADD this line
                onAttendanceClick = {
                    Timber.d("🧭 Navigating to Attendance Marking from Home")
                    navController.navigate(Screen.AttendanceMarking.route)
                },
                onLogout = {
                    Timber.d("🧭 Logout triggered, navigating to Login")
                    // Clear all ViewModels data
                    attendanceViewModel.clearAttendanceData()
                    bleViewModel.stopScanning()

                    navController.navigate(Screen.Login.route) {
                        // Clear entire back stack
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
        composable(Screen.AttendanceMarking.route) {
            Timber.d("🧭 Navigating to Attendance Marking Screen")
            AttendanceMarkingScreen(
                attendanceViewModel = attendanceViewModel,
                profileViewModel = profileViewModel,
                bleViewModel = bleViewModel,
                onNavigateBack = {
                    Timber.d("🧭 Navigating back from Attendance Marking")
                    navController.popBackStack()
                },
                onNavigateToSuccess = { successData ->
                    Timber.d("🧭 Navigating to Success Screen with data: ${successData.rollNumber}")

                    // Navigate to success screen with data
                    val route = Screen.AttendanceSuccess.createRoute(
                        rollNumber = successData.rollNumber,
                        subject = successData.subject,
                        room = successData.room,
                        type = successData.type,
                        deviceRoom = successData.deviceRoom,
                        attendanceId = successData.attendanceId
                    )

                    navController.navigate(route) {
                        // Remove attendance marking from back stack
                        popUpTo(Screen.AttendanceMarking.route) { inclusive = true }
                    }
                }
            )
        }

        // Attendance Success Screen - UPDATED: Now receives BleViewModel
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
            // FIX: URL decode the parameters
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
    object Login : Screen("login")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object AttendanceMarking : Screen("attendance_marking")
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
            // FIX: URL encode special characters and handle empty values
            val encodedRollNumber = java.net.URLEncoder.encode(rollNumber.ifBlank { "unknown" }, "UTF-8")
            val encodedSubject = java.net.URLEncoder.encode(subject.ifBlank { "unknown" }, "UTF-8")
            val encodedRoom = java.net.URLEncoder.encode(room.ifBlank { "unknown" }, "UTF-8")
            val encodedType = java.net.URLEncoder.encode(type.ifBlank { "unknown" }, "UTF-8")
            val encodedDeviceRoom = java.net.URLEncoder.encode(deviceRoom.ifBlank { "unknown" }, "UTF-8")
            val encodedAttendanceId = java.net.URLEncoder.encode(attendanceId.ifBlank { "unknown" }, "UTF-8")

            return "attendance_success/$encodedRollNumber/$encodedSubject/$encodedRoom/$encodedType/$encodedDeviceRoom/$encodedAttendanceId"
        }
    }

}



