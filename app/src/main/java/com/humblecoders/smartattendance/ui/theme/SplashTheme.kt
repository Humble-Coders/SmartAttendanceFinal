package com.humblecoders.smartattendance.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Special theme configuration for splash screen
 * Handles status bar colors and system UI for immersive experience
 */
@Composable
fun SplashTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Set status bar to gradient blue color (matching splash background)
            window.statusBarColor = Color(0xFF007AFF).toArgb()

            // Set navigation bar color
            window.navigationBarColor = Color(0xFF4B0082).toArgb()

            // Make status bar content light (white icons/text)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = SmartAttendanceLightColorScheme,
        typography = Typography,
        content = content
    )
}

// Custom color scheme for splash screen consistency
private val SmartAttendanceLightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF007AFF).copy(alpha = 0.1f),
    secondary = Color(0xFF34C759),
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF1D1D1F),
    surface = Color.White,
    onSurface = Color(0xFF1D1D1F),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF8E8E93)
)