package com.humblecoders.smartattendance.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.ButtonDefaults.outlinedButtonBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblecoders.smartattendance.presentation.viewmodel.ProfileViewModel
import timber.log.Timber

// Replace the LoginScreen composable content with this updated version:

@Composable
fun LoginScreen(
    profileViewModel: ProfileViewModel,
    onLoginSuccess: () -> Unit
) {
    val nameInput by profileViewModel.nameInput.collectAsState()
    val rollNumberInput by profileViewModel.rollNumberInput.collectAsState()
    val isSaving by profileViewModel.isSaving.collectAsState()

    var showError by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    var showFaceRegistration by remember { mutableStateOf(false) }
    var classInput by remember { mutableStateOf("") }
    var faceRegistrationCompleted by remember { mutableStateOf(false) }
    var registeredFaceId by remember { mutableStateOf<String?>(null) }

    // NEW: Toggle for already registered face
    var hasAlreadyRegisteredFace by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-hide error message
    LaunchedEffect(showError) {
        if (showError != null) {
            kotlinx.coroutines.delay(3000)
            showError = null
        }
    }

    // Auto-hide success message and navigate
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            kotlinx.coroutines.delay(1500)
            onLoginSuccess()
        }
    }

    // Face Registration Screen
    if (showFaceRegistration) {
        FaceRegistrationScreen(
            rollNumber = rollNumberInput,
            onRegistrationSuccess = { faceId ->
                Timber.d("✅ Face registration successful with ID: $faceId")
                registeredFaceId = faceId
                faceRegistrationCompleted = true
                hasAlreadyRegisteredFace = false // Reset toggle since we just registered
                showFaceRegistration = false
                showError = null
            },
            onRegistrationError = { error ->
                Timber.e("❌ Face registration failed: $error")
                showError = "Face registration failed: $error"
                showFaceRegistration = false
            },
            onNavigateBack = {
                Timber.d("🔙 Back from face registration")
                showFaceRegistration = false
            }
        )
        return
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // App Icon and Title Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Smart Attend Icon",
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(40.dp)
                        ),
                    tint = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Smart Attend",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Student Portal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }

            // Login Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {

                    // Card Title
                    Text(
                        text = "Sign In to Continue",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1D1D1F),
                        textAlign = TextAlign.Center
                    )

                    // Error Message
                    if (showError != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFF3B30).copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = showError!!,
                                color = Color(0xFFFF3B30),
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Success Message
                    if (showSuccess) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF34C759).copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Login successful! Welcome to Smart Attend",
                                color = Color(0xFF34C759),
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Face Registration Success Message
                    if (faceRegistrationCompleted) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF34C759).copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "✅ Face Registration Complete!",
                                    color = Color(0xFF34C759),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "You can now sign in with your registered face",
                                    color = Color(0xFF34C759),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Full Name Input
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { profileViewModel.updateNameInput(it) },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving && !showSuccess,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color(0xFF8E8E93).copy(alpha = 0.3f),
                            focusedContainerColor = Color(0xFFF2F2F7),
                            unfocusedContainerColor = Color(0xFFF2F2F7)
                        ),
                        isError = nameInput.isBlank() && showError != null
                    )

                    // Roll Number Input
                    OutlinedTextField(
                        value = rollNumberInput,
                        onValueChange = { profileViewModel.updateRollNumberInput(it) },
                        label = { Text("Roll Number") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving && !showSuccess,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color(0xFF8E8E93).copy(alpha = 0.3f),
                            focusedContainerColor = Color(0xFFF2F2F7),
                            unfocusedContainerColor = Color(0xFFF2F2F7)
                        ),
                        isError = rollNumberInput.isBlank() && showError != null
                    )

                    // Class Input
                    OutlinedTextField(
                        value = classInput,
                        onValueChange = { classInput = it },
                        label = { Text("Class") },
                        placeholder = { Text("e.g., 2S12, 3E15") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving && !showSuccess,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color(0xFF8E8E93).copy(alpha = 0.3f),
                            focusedContainerColor = Color(0xFFF2F2F7),
                            unfocusedContainerColor = Color(0xFFF2F2F7)
                        ),
                        isError = classInput.isBlank() && showError != null
                    )

                    // NEW: Face Registration Toggle
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF2F2F7)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "I have already registered my face",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1D1D1F)
                                )
                                Text(
                                    text = "Skip face registration if already completed",
                                    fontSize = 12.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }

                            Switch(
                                checked = hasAlreadyRegisteredFace,
                                onCheckedChange = {
                                    hasAlreadyRegisteredFace = it
                                    if (it) {
                                        // Reset registration completion state when toggle is enabled
                                        faceRegistrationCompleted = false
                                        registeredFaceId = null
                                        showError = null
                                    }
                                },
                                enabled = !isSaving && !showSuccess,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF34C759),
                                    checkedTrackColor = Color(0xFF34C759).copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color(0xFF8E8E93),
                                    uncheckedTrackColor = Color(0xFF8E8E93).copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    // Face Registration Button (Only show if toggle is OFF)
                    if (!hasAlreadyRegisteredFace) {
                        OutlinedButton(
                            onClick = {
                                keyboardController?.hide()

                                // Validation for face registration
                                when {
                                    nameInput.isBlank() -> {
                                        showError = "Please enter your full name first"
                                        return@OutlinedButton
                                    }
                                    rollNumberInput.isBlank() -> {
                                        showError = "Please enter your roll number first"
                                        return@OutlinedButton
                                    }
                                    rollNumberInput.length < 4 -> {
                                        showError = "Roll number must be at least 4 characters"
                                        return@OutlinedButton
                                    }
                                    else -> {
                                        showError = null
                                        Timber.d("Starting face registration for: ${rollNumberInput}")
                                        showFaceRegistration = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = !isSaving && !showSuccess && !faceRegistrationCompleted,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (faceRegistrationCompleted) Color(0xFF34C759) else Color(0xFF007AFF)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (faceRegistrationCompleted) "✅" else "📷",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = if (faceRegistrationCompleted) "Face Registered" else "Register Face",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Sign In Button
                    Button(
                        onClick = {
                            keyboardController?.hide()

                            // Validation
                            when {
                                nameInput.isBlank() -> {
                                    showError = "Please enter your full name"
                                    return@Button
                                }
                                nameInput.length < 2 -> {
                                    showError = "Name must be at least 2 characters"
                                    return@Button
                                }
                                rollNumberInput.isBlank() -> {
                                    showError = "Please enter your roll number"
                                    return@Button
                                }
                                rollNumberInput.length < 4 -> {
                                    showError = "Roll number must be at least 4 characters"
                                    return@Button
                                }
                                classInput.isBlank() -> {
                                    showError = "Please enter your class"
                                    return@Button
                                }
                                // NEW: Check face registration requirement
                                !hasAlreadyRegisteredFace && !faceRegistrationCompleted -> {
                                    showError = "Please complete face registration or toggle 'I have already registered my face'"
                                    return@Button
                                }
                                else -> {
                                    showError = null
                                    val registrationStatus = if (hasAlreadyRegisteredFace) "previously registered" else "newly registered"
                                    Timber.d("Login attempt: name='${nameInput}', roll='${rollNumberInput}', class='${classInput}', faceStatus=$registrationStatus")

                                    // Save profile with class and proceed
                                    profileViewModel.saveProfileWithClass(
                                        name = nameInput,
                                        rollNumber = rollNumberInput,
                                        className = classInput,
                                        onSuccess = {
                                            Timber.d("Profile saved successfully, showing success message")
                                            showSuccess = true
                                        },
                                        onError = { error ->
                                            Timber.e("Profile save failed: $error")
                                            showError = error
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isSaving && !showSuccess,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isSaving && !showSuccess) {
                                Color(0xFF007AFF)
                            } else {
                                Color(0xFF8E8E93)
                            },
                            contentColor = Color.White
                        )
                    ) {
                        if (isSaving) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Signing In...",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (showSuccess) {
                            Text(
                                "✓ Success",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                "Sign In",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Clear Form Button
                    OutlinedButton(
                        onClick = {
                            keyboardController?.hide()
                            profileViewModel.updateNameInput("")
                            profileViewModel.updateRollNumberInput("")
                            classInput = ""
                            faceRegistrationCompleted = false
                            registeredFaceId = null
                            hasAlreadyRegisteredFace = false
                            showError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isSaving && !showSuccess,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF8E8E93)
                        )
                    ) {
                        Text(
                            "Clear Form",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Testing Mode Info
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🧪 Testing Mode",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Face registration required only once per student",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}