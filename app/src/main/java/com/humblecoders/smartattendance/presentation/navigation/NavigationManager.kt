package com.humblecoders.smartattendance.presentation.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Navigation Manager to handle complex navigation scenarios
 */
class NavigationManager : ViewModel() {

    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()

    private val _currentScreen = MutableStateFlow<String?>(null)
    val currentScreen: StateFlow<String?> = _currentScreen.asStateFlow()

    /**
     * Navigate with loading state management
     */
    fun navigateWithLoading(
        navController: NavController,
        route: String,
        clearBackStack: Boolean = false,
        popUpToRoute: String? = null,
        inclusive: Boolean = false
    ) {
        if (_isNavigating.value) {
            Timber.w("🧭 Navigation already in progress, ignoring duplicate navigation")
            return
        }

        viewModelScope.launch {
            try {
                _isNavigating.value = true
                Timber.d("🧭 NavigationManager: Navigating to $route")

                if (clearBackStack) {
                    navController.navigate(route) {
                        popUpTo(0) { this.inclusive = true }
                    }
                } else if (popUpToRoute != null) {
                    navController.navigate(route) {
                        popUpTo(popUpToRoute) { this.inclusive = inclusive }
                    }
                } else {
                    navController.navigate(route)
                }

                _currentScreen.value = route
                Timber.d("🧭 NavigationManager: Successfully navigated to $route")

            } catch (e: Exception) {
                Timber.e(e, "🧭 NavigationManager: Navigation failed to $route")
            } finally {
                _isNavigating.value = false
            }
        }
    }

    /**
     * Navigate back with state management
     */
    fun navigateBack(navController: NavController) {
        if (_isNavigating.value) {
            Timber.w("🧭 Navigation in progress, ignoring back navigation")
            return
        }

        viewModelScope.launch {
            try {
                _isNavigating.value = true
                Timber.d("🧭 NavigationManager: Navigating back")

                val result = navController.popBackStack()
                if (result) {
                    Timber.d("🧭 NavigationManager: Successfully navigated back")
                } else {
                    Timber.w("🧭 NavigationManager: No screens to pop from back stack")
                }

            } catch (e: Exception) {
                Timber.e(e, "🧭 NavigationManager: Back navigation failed")
            } finally {
                _isNavigating.value = false
            }
        }
    }

    /**
     * Handle logout with complete cleanup
     */
    fun handleLogout(
        navController: NavController,
        onClearData: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isNavigating.value = true
                Timber.d("🧭 NavigationManager: Handling logout")

                // Clear all data
                onClearData()

                // Navigate to login and clear back stack
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }

                _currentScreen.value = Screen.Login.route
                Timber.d("🧭 NavigationManager: Logout completed")

            } catch (e: Exception) {
                Timber.e(e, "🧭 NavigationManager: Logout failed")
            } finally {
                _isNavigating.value = false
            }
        }
    }
}

/**
 * Composable to provide NavigationManager
 */
@Composable
fun rememberNavigationManager(): NavigationManager {
    return remember { NavigationManager() }
}

/**
 * Extension functions for easier navigation
 */
fun NavController.navigateToHome(clearBackStack: Boolean = false) {
    if (clearBackStack) {
        navigate(Screen.Home.route) {
            popUpTo(0) { inclusive = true }
        }
    } else {
        navigate(Screen.Home.route)
    }
}

fun NavController.navigateToLogin(clearBackStack: Boolean = true) {
    navigate(Screen.Login.route) {
        if (clearBackStack) {
            popUpTo(0) { inclusive = true }
        }
    }
}

fun NavController.navigateToProfile() {
    navigate(Screen.Profile.route)
}

fun NavController.navigateToAttendanceMarking() {
    navigate(Screen.AttendanceMarking.route)
}

fun NavController.navigateToAttendanceSuccess(
    rollNumber: String,
    subject: String,
    room: String,
    type: String,
    deviceRoom: String,
    attendanceId: String
) {
    val route = Screen.AttendanceSuccess.createRoute(
        rollNumber = rollNumber,
        subject = subject,
        room = room,
        type = type,
        deviceRoom = deviceRoom,
        attendanceId = attendanceId
    )
    navigate(route) {
        popUpTo(Screen.AttendanceMarking.route) { inclusive = true }
    }
}