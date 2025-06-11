package com.humblecoders.smartattendance.presentation.navigation


import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.humblecoders.smartattendance.presentation.screens.HomeScreen
import com.humblecoders.smartattendance.presentation.viewmodel.AttendanceViewModel
import com.humblecoders.smartattendance.presentation.viewmodel.BleViewModel
import com.humblecoders.smartattendance.presentation.viewmodel.ProfileViewModel

@Composable
fun AppNavigation(
    bleViewModel: BleViewModel,
    profileViewModel: ProfileViewModel,
    attendanceViewModel: AttendanceViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                bleViewModel = bleViewModel,
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(Screen.Profile.route) {
            // Profile screen will be implemented next
        }
    }
}

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Profile : Screen("profile")
}