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
import timber.log.Timber

@Composable
fun AppNavigation(
    bleViewModel: BleViewModel,
    profileViewModel: ProfileViewModel,
    attendanceViewModel: AttendanceViewModel
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
                navArgument("rollNumber") { type = NavType.StringType },
                navArgument("subject") { type = NavType.StringType },
                navArgument("room") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType },
                navArgument("deviceRoom") { type = NavType.StringType },
                navArgument("attendanceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rollNumber = backStackEntry.arguments?.getString("rollNumber") ?: ""
            val subject = backStackEntry.arguments?.getString("subject") ?: ""
            val room = backStackEntry.arguments?.getString("room") ?: ""
            val type = backStackEntry.arguments?.getString("type") ?: ""
            val deviceRoom = backStackEntry.arguments?.getString("deviceRoom") ?: ""
            val attendanceId = backStackEntry.arguments?.getString("attendanceId") ?: ""

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
                        // Clear success screen and any remaining screens from back stack
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
            return "attendance_success/$rollNumber/$subject/$room/$type/$deviceRoom/$attendanceId"
        }
    }
}