package com.humblecoders.smartattendance.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import com.humblecoders.smartattendance.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.delay
import timber.log.Timber

@Composable
fun SplashScreen(
    profileViewModel: ProfileViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    var titleOpacity by remember { mutableStateOf(0f) }
    var loadingOpacity by remember { mutableStateOf(0f) }
    var brandingOpacity by remember { mutableStateOf(0f) }

    // Animation progress from 0 to 1
    var iconAnimProgress by remember { mutableStateOf(0f) }

    val profileData by profileViewModel.profileData.collectAsState()

    // Animate icon transition from center to top
    val animatedProgress by animateFloatAsState(
        targetValue = iconAnimProgress,
        animationSpec = tween(durationMillis = 1500, easing = EaseOutQuart),
        label = "iconAnimProgress"
    )

    // Keep icon size and shadow stable during animation to prevent flickering
    val iconSize = 140.dp
    val shadowElevation = 16.dp

    // Animate title and subtitle opacity
    val animatedTitleOpacity by animateFloatAsState(
        targetValue = titleOpacity,
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "titleOpacity"
    )

    // Animate loading indicator opacity
    val animatedLoadingOpacity by animateFloatAsState(
        targetValue = loadingOpacity,
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "loadingOpacity"
    )

    // Animate branding opacity
    val animatedBrandingOpacity by animateFloatAsState(
        targetValue = brandingOpacity,
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "brandingOpacity"
    )

    // Start animations
    LaunchedEffect(Unit) {
        // Display icon in center
        delay(500)

        // Start moving icon from center to top
        iconAnimProgress = 1f
        delay(1300) // Wait for icon animation to complete

        // Show title and subtitle
        titleOpacity = 1f
        delay(400)

        // Show loading indicator
        loadingOpacity = 1f
        delay(500)

        // Show branding
        brandingOpacity = 1f
        delay(1500)

        // Navigate after animations complete
        checkProfileAndNavigate(profileData, onNavigateToLogin, onNavigateToHome)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF007AFF).copy(alpha = 0.6f), // Blue
                        Color(0xFF5856D6).copy(alpha = 0.4f)  // Purple
                    )
                )
            )
    ) {
        // Create constraint set for icon animation
        val constraints = ConstraintSet {
            val iconRef = createRefFor("icon")
            val titleRef = createRefFor("title")
            val subtitleRef = createRefFor("subtitle")
            val loadingRef = createRefFor("loading")
            val brandingRef = createRefFor("branding")

            // Icon constraints - smooth transition using linkTo with bias
            constrain(iconRef) {
                centerHorizontallyTo(parent)
                // Use linkTo with bias for smooth vertical animation
                linkTo(
                    parent.top,
                    parent.bottom,
                    topMargin = lerp(0.dp, 120.dp, animatedProgress),
                    bias = lerp(0.5f, 0f, animatedProgress)
                )
            }

            constrain(titleRef) {
                top.linkTo(iconRef.bottom, 16.dp)
                centerHorizontallyTo(parent)
                width = Dimension.wrapContent
            }

            constrain(subtitleRef) {
                top.linkTo(titleRef.bottom, 4.dp)
                centerHorizontallyTo(parent)
                width = Dimension.wrapContent
            }

            constrain(loadingRef) {
                top.linkTo(subtitleRef.bottom, 32.dp)
                centerHorizontallyTo(parent)
                width = Dimension.wrapContent
            }

            constrain(brandingRef) {
                bottom.linkTo(parent.bottom, 24.dp)
                centerHorizontallyTo(parent)
                width = Dimension.wrapContent
            }
        }

        ConstraintLayout(
            constraintSet = constraints,
            modifier = Modifier.fillMaxSize()
        ) {
            // Icon with stable size and shadow to prevent flickering
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "Smart Attend Icon",
                modifier = Modifier
                    .layoutId("icon")
                    .size(iconSize),
                tint = Color.White
            )

            // Title
            Text(
                text = "Smart Attend",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .layoutId("title")
                    .alpha(animatedTitleOpacity)
            )

            // Subtitle
            Text(
                text = "Student Portal",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .layoutId("subtitle")
                    .alpha(animatedTitleOpacity)
            )

            // Loading indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .layoutId("loading")
                    .alpha(animatedLoadingOpacity)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )

                Text(
                    text = "Loading your experience...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }

            // Branding at bottom
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .layoutId("branding")
                    .alpha(animatedBrandingOpacity)
            ) {
                // Divider line
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(1.5.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.6f),
                                    Color.White.copy(alpha = 0.7f),
                                    Color.White.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Thapar Institute of Engineering & Technology",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Helper function to interpolate between values based on progress
private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}

private fun lerp(start: Int, stop: Int, fraction: Float): Int {
    return start + (fraction * (stop - start)).toInt()
}

private fun lerp(start: androidx.compose.ui.unit.Dp, stop: androidx.compose.ui.unit.Dp, fraction: Float): androidx.compose.ui.unit.Dp {
    return androidx.compose.ui.unit.Dp(start.value + fraction * (stop.value - start.value))
}

private fun checkProfileAndNavigate(
    profileData: com.humblecoders.smartattendance.data.model.ProfileData,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val isProfileComplete = profileData.name.isNotBlank() &&
            profileData.rollNumber.isNotBlank() &&
            profileData.className.isNotBlank()

    Timber.d("🧭 SplashScreen: Profile check - complete: $isProfileComplete")
    Timber.d("📋 Profile: name='${profileData.name}', roll='${profileData.rollNumber}', class='${profileData.className}'")

    if (isProfileComplete) {
        Timber.d("🏠 SplashScreen: Navigating to Home")
        onNavigateToHome()
    } else {
        Timber.d("🔐 SplashScreen: Navigating to Login")
        onNavigateToLogin()
    }
}