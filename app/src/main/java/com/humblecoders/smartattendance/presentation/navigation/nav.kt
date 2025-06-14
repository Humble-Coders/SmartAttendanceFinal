package com.humblecoders.smartattendance.presentation.navigation

import androidx.navigation.NavController
import com.humblecoders.smartattendance.data.model.ProfileData
import timber.log.Timber

/**
 * Navigation Flow Utilities
 *
 * Complete App Flow:
 * ==================
 *
 * 1. App Launch
 *    ↓
 * 2. Check Profile State
 *    ├─ Complete Profile → Home Screen
 *    └─ Incomplete Profile → Login Screen
 *
 * 3. Login Screen (if needed)
 *    ├─ Enter: Name, Roll Number, Class
 *    ├─ Validate & Save Profile
 *    └─ Navigate to Home
 *
 * 4. Home Screen
 *    ├─ Check Active Session (Firebase)
 *    ├─ Start BLE Scanning (if session active)
 *    ├─ Show Overlays (Room Detection → Classroom Detected)
 *    ├─ Manual Attendance Button
 *    └─ Logout Option
 *
 * 5. Attendance Marking Screen
 *    ├─ Camera Permission Check
 *    ├─ Face.io Authentication
 *    ├─ Validate Session & Profile
 *    ├─ Mark Attendance (Firebase)
 *    └─ Navigate to Success
 *
 * 6. Attendance Success Screen
 *    ├─ Show Attendance Details
 *    ├─ Auto-navigation (3s)
 *    └─ Return to Home
 *
 * 7. Profile Screen (Optional)
 *    ├─ Edit Profile Information
 *    └─ Return to Previous Screen
 */

object NavigationFlowUtils {

    /**
     * Validate if user can navigate to home screen
     */
    fun canNavigateToHome(profileData: ProfileData): Boolean {
        val isComplete = profileData.name.isNotBlank() &&
                profileData.rollNumber.isNotBlank() &&
                profileData.className.isNotBlank()

        Timber.d("🧭 Profile completeness check: $isComplete")
        Timber.d("📋 Name: '${profileData.name}', Roll: '${profileData.rollNumber}', Class: '${profileData.className}'")

        return isComplete
    }

    /**
     * Validate if user can mark attendance
     */
    fun canMarkAttendance(
        profileData: ProfileData,
        hasActiveSession: Boolean
    ): Pair<Boolean, String> {
        return when {
            !canNavigateToHome(profileData) -> {
                false to "Profile information incomplete. Please update your profile."
            }
            !hasActiveSession -> {
                false to "No active session found. Please wait for teacher to start the session."
            }
            else -> {
                true to "Ready to mark attendance"
            }
        }
    }

    /**
     * Handle navigation based on app state
     */
    fun handleAppStateNavigation(
        navController: NavController,
        profileData: ProfileData,
        currentRoute: String?
    ) {
        val shouldBeOnHome = canNavigateToHome(profileData)
        val isOnLogin = currentRoute == Screen.Login.route
        val isOnHome = currentRoute == Screen.Home.route

        Timber.d("🧭 App state navigation check")
        Timber.d("📍 Current route: $currentRoute")
        Timber.d("🏠 Should be on home: $shouldBeOnHome")

        when {
            shouldBeOnHome && isOnLogin -> {
                Timber.d("🧭 Profile complete but on login, navigating to home")
                navController.navigateToHome(clearBackStack = true)
            }
            !shouldBeOnHome && isOnHome -> {
                Timber.d("🧭 Profile incomplete but on home, navigating to login")
                navController.navigateToLogin(clearBackStack = true)
            }
            else -> {
                Timber.d("🧭 Navigation state is correct, no action needed")
            }
        }
    }

    /**
     * Handle deep link navigation
     */
    fun handleDeepLink(
        navController: NavController,
        deepLinkRoute: String,
        profileData: ProfileData
    ): Boolean {
        return try {
            if (!canNavigateToHome(profileData)) {
                Timber.w("🧭 Deep link blocked - profile incomplete")
                navController.navigateToLogin(clearBackStack = true)
                false
            } else {
                Timber.d("🧭 Processing deep link: $deepLinkRoute")
                navController.navigate(deepLinkRoute)
                true
            }
        } catch (e: Exception) {
            Timber.e(e, "🧭 Deep link navigation failed")
            false
        }
    }

    /**
     * Clear all app data and return to login
     */
    fun clearAppDataAndReturnToLogin(
        navController: NavController,
        onClearBleData: () -> Unit,
        onClearAttendanceData: () -> Unit,
        onClearProfileData: () -> Unit
    ) {
        try {
            Timber.d("🧭 Clearing all app data and returning to login")

            // Clear all data
            onClearBleData()
            onClearAttendanceData()
            onClearProfileData()

            // Navigate to login
            navController.navigateToLogin(clearBackStack = true)

            Timber.d("🧭 App data cleared successfully")
        } catch (e: Exception) {
            Timber.e(e, "🧭 Failed to clear app data")
        }
    }
}

/**
 * Navigation State Validator
 */
object NavigationValidator {

    /**
     * Validate navigation request
     */
    fun validateNavigation(
        fromRoute: String,
        toRoute: String,
        profileData: ProfileData
    ): NavigationValidationResult {

        return when (toRoute) {
            Screen.Home.route -> {
                if (canNavigateToHome(profileData)) {
                    NavigationValidationResult.ALLOWED
                } else {
                    NavigationValidationResult.BLOCKED_PROFILE_INCOMPLETE
                }
            }

            Screen.AttendanceMarking.route -> {
                if (canNavigateToHome(profileData)) {
                    NavigationValidationResult.ALLOWED
                } else {
                    NavigationValidationResult.BLOCKED_PROFILE_INCOMPLETE
                }
            }

            Screen.Profile.route -> {
                NavigationValidationResult.ALLOWED // Profile always accessible
            }

            Screen.Login.route -> {
                NavigationValidationResult.ALLOWED // Login always accessible
            }

            else -> {
                if (toRoute.startsWith("attendance_success")) {
                    // Success screen requires complete profile
                    if (canNavigateToHome(profileData)) {
                        NavigationValidationResult.ALLOWED
                    } else {
                        NavigationValidationResult.BLOCKED_PROFILE_INCOMPLETE
                    }
                } else {
                    NavigationValidationResult.UNKNOWN_ROUTE
                }
            }
        }
    }
}

enum class NavigationValidationResult {
    ALLOWED,
    BLOCKED_PROFILE_INCOMPLETE,
    BLOCKED_NO_SESSION,
    UNKNOWN_ROUTE
}

/**
 * Extension function to check if profile is complete
 */
private fun canNavigateToHome(profileData: ProfileData): Boolean {
    return NavigationFlowUtils.canNavigateToHome(profileData)
}