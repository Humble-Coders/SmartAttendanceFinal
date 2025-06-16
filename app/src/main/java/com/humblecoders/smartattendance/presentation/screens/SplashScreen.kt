package com.humblecoders.smartattendance.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblecoders.smartattendance.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.delay
import timber.log.Timber

@Composable
fun SplashScreen(
    profileViewModel: ProfileViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    var isAnimating by remember { mutableStateOf(false) }
    var opacity by remember { mutableStateOf(0f) }
    var brandingOpacity by remember { mutableStateOf(0f) }
    var logoScale by remember { mutableStateOf(0.8f) }

    val profileData by profileViewModel.profileData.collectAsState()

    // Animate logo scale
    val animatedLogoScale by animateFloatAsState(
        targetValue = logoScale,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "logoScale"
    )

    // Animate opacity
    val animatedOpacity by animateFloatAsState(
        targetValue = opacity,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "opacity"
    )

    // Animate branding opacity
    val animatedBrandingOpacity by animateFloatAsState(
        targetValue = brandingOpacity,
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "brandingOpacity"
    )

    // Pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "infiniteTransition")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Start animations
    LaunchedEffect(Unit) {
        Timber.d("🎬 SplashScreen: Starting animations")

        // Start logo scale animation
        logoScale = 1.0f

        // Delay then start main content fade in
        delay(300)
        opacity = 1.0f

        // Start pulsing animation
        delay(500)
        isAnimating = true

        // Start branding animation
        delay(1500)
        brandingOpacity = 1.0f

        // Check profile and navigate after animations complete
        delay(2500)
        checkProfileAndNavigate(profileData, onNavigateToLogin, onNavigateToHome)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF007AFF).copy(alpha = 0.85f), // Blue
                        Color(0xFF00BFFF).copy(alpha = 0.7f),  // Cyan
                        Color(0xFF4B0082).copy(alpha = 0.75f)  // Indigo
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // App Icon with enhanced animation and glow
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // Glow effect
                    if (isAnimating) {
                        Text(
                            text = "🎓",
                            fontSize = 110.sp,
                            modifier = Modifier
                                .scale(animatedLogoScale * if (isAnimating) 1.2f else 1.0f)
                                .alpha(glowAlpha * 0.3f)
                        )
                    }

                    // Main icon
                    Text(
                        text = "🎓",
                        fontSize = 110.sp,
                        modifier = Modifier
                            .scale(animatedLogoScale)
                            .shadow(
                                elevation = 20.dp,
                                ambientColor = Color.Black.copy(alpha = 0.3f),
                                spotColor = Color.Black.copy(alpha = 0.3f)
                            )
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.alpha(animatedOpacity)
                ) {
                    Text(
                        text = "Smart Attend",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.shadow(
                            elevation = 5.dp,
                            ambientColor = Color.Black.copy(alpha = 0.2f)
                        )
                    )

                    Text(
                        text = "Student Portal",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Loading indicator with animation
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.alpha(animatedOpacity)
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
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))

            // Elegant branding at bottom
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .alpha(animatedBrandingOpacity)
                    .padding(bottom = 50.dp)
            ) {
                // Enhanced divider line
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
                        .scale(animatedBrandingOpacity, 1.0f)
                )

                // Animated branding text
                AnimatedBrandingText(opacity = animatedBrandingOpacity)
            }
        }
    }
}

@Composable
private fun AnimatedBrandingText(opacity: Float) {
    val words = listOf("A", "Humble", "Solutions", "Product")

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        words.forEachIndexed { index, word ->
            val delay = index * 150L
            var wordOpacity by remember { mutableStateOf(0f) }
            var wordOffset by remember { mutableStateOf(-30f) }

            val animatedWordOpacity by animateFloatAsState(
                targetValue = wordOpacity,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 600f
                ),
                label = "wordOpacity"
            )

            val animatedWordOffset by animateFloatAsState(
                targetValue = wordOffset,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 600f
                ),
                label = "wordOffset"
            )

            LaunchedEffect(opacity) {
                if (opacity > 0) {
                    kotlinx.coroutines.delay(delay)
                    wordOpacity = 1.0f
                    wordOffset = 0f
                }
            }

            Text(
                text = word,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                letterSpacing = 2.0.sp,
                modifier = Modifier
                    .alpha(animatedWordOpacity)
                    .offset(x = animatedWordOffset.dp)
                    .shadow(
                        elevation = 8.dp,
                        ambientColor = Color.Black.copy(alpha = 0.4f)
                    )
            )
        }
    }
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