package com.humblecoders.smartattendance.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblecoders.smartattendance.presentation.components.CameraPermissionHandler
import com.humblecoders.smartattendance.presentation.components.FaceIoAuthWebView
import com.humblecoders.smartattendance.presentation.viewmodel.AttendanceViewModel
import com.humblecoders.smartattendance.presentation.viewmodel.ProfileViewModel
import com.humblecoders.smartattendance.presentation.viewmodel.BleViewModel
import com.humblecoders.smartattendance.data.model.AttendanceSuccessData
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceMarkingScreen(
    attendanceViewModel: AttendanceViewModel,
    profileViewModel: ProfileViewModel,
    bleViewModel: BleViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSuccess: (AttendanceSuccessData) -> Unit
) {
    // Collect state from ViewModels
    val profileData by profileViewModel.profileData.collectAsState()
    val currentSession by attendanceViewModel.currentSession.collectAsState()
    val detectedDeviceRoom by bleViewModel.detectedDeviceRoom.collectAsState()

    // Local UI state
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var isProcessingAttendance by remember { mutableStateOf(false) }

    // Screen lifecycle logging
    LaunchedEffect(Unit) {
        Timber.d("🎬 AttendanceMarkingScreen: Screen launched")
        Timber.d("📋 Profile: ${profileData.name}, Roll: ${profileData.rollNumber}, Class: ${profileData.className}")
        Timber.d("📚 Session: ${currentSession?.subject}, Room: ${currentSession?.room}")
        Timber.d("📡 BLE Device Room: $detectedDeviceRoom")
    }

    DisposableEffect(Unit) {
        onDispose {
            Timber.d("🎬 AttendanceMarkingScreen: Screen disposed")
        }
    }

    // Handle camera permission
    CameraPermissionHandler(
        onPermissionGranted = {
            Timber.d("📷 Camera permission granted")
            hasCameraPermission = true
            permissionDenied = false
        },
        onPermissionDenied = {
            Timber.w("📷 Camera permission denied")
            permissionDenied = true
        }
    )

    // Function to mark attendance with all required data
// In AttendanceMarkingScreen.kt - Replace the function inside the composable

    // Function to mark attendance with all required data
// In AttendanceMarkingScreen.kt - Replace the function inside the composable

    // Function to mark attendance with all required data
// REPLACE in AttendanceMarkingScreen.kt
// Replace the markAttendanceWithSession function:

    fun markAttendanceWithSession(rollNumber: String) {
        if (isProcessingAttendance) {
            Timber.w("⚠️ Attendance already being processed, ignoring duplicate request")
            return
        }

        val session = currentSession
        if (session == null) {
            Timber.e("❌ No active session available for attendance marking")
            errorMessage = "No active session found. Please return to home and try again."
            return
        }

        if (profileData.className.isBlank()) {
            Timber.e("❌ No class information available")
            errorMessage = "Class information not found. Please update your profile."
            return
        }

        isProcessingAttendance = true
        val isExtra = session.isExtra
        val attendanceType = if (isExtra) "extra" else "regular"

        Timber.d("🎯 Starting $attendanceType attendance marking process")
        Timber.d("📋 Student: ${profileData.name} (${rollNumber}) from ${profileData.className}")
        Timber.d("📚 Session: ${session.subject} in ${session.room} (${session.type}) - Extra: ${session.isExtra}")
        Timber.d("📡 Device Room: $detectedDeviceRoom")

        attendanceViewModel.markAttendance(
            rollNumber = rollNumber,
            deviceRoom = detectedDeviceRoom ?: "",
            isExtra = isExtra,
            onSuccess = {
                Timber.i("🎉 $attendanceType attendance marked successfully!")

                // FIX: Create success data with proper string encoding for navigation
                val successData = AttendanceSuccessData(
                    rollNumber = rollNumber,
                    studentName = profileData.name,
                    subject = session.subject,
                    room = session.room,
                    type = session.type,
                    deviceRoom = detectedDeviceRoom ?: "",
                    attendanceId = "att_${System.currentTimeMillis()}"
                )

                Timber.d("✅ Created success data: $successData")
                isProcessingAttendance = false

                // FIX: Navigate to success screen with proper route encoding
                onNavigateToSuccess(successData)
            },
            onError = { error ->
                Timber.e("❌ $attendanceType attendance marking failed: $error")
                errorMessage = error
                isProcessingAttendance = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Face Authentication") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            Timber.d("🔙 User clicked back button")
                            onNavigateBack()
                        },
                        enabled = !isProcessingAttendance
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF2F2F7)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                permissionDenied -> {
                    Timber.d("🎬 Showing permission denied content")
                    PermissionDeniedContent(
                        onCancel = {
                            Timber.d("🔙 User cancelled due to permission denial")
                            onNavigateBack()
                        }
                    )
                }

                !hasCameraPermission -> {
                    Timber.d("🎬 Waiting for camera permission")
                    LoadingContent(message = "Requesting camera permission...")
                }

                errorMessage != null -> {
                    Timber.d("🎬 Showing error content: $errorMessage")
                    AttendanceErrorContent(
                        errorMessage = errorMessage!!,
                        onRetry = {
                            Timber.d("🔄 User retrying after error")
                            errorMessage = null
                        },
                        onCancel = {
                            Timber.d("🔙 User cancelled after error")
                            onNavigateBack()
                        }
                    )
                }

                isProcessingAttendance -> {
                    Timber.d("🎬 Showing processing content")
                    ProcessingAttendanceContent()
                }

                else -> {
                    Timber.d("🎬 Showing Face.io authentication WebView")
                    // Validate session before showing WebView
                    if (currentSession == null) {
                        LaunchedEffect(Unit) {
                            errorMessage = "No active session found. Please return to home and try again."
                        }
                    } else {
                        FaceIoAuthWebView(
                            modifier = Modifier.fillMaxSize(),
                            onAuthenticated = { rollNumber ->
                                Timber.d("🔥 WEBVIEW CALLBACK TRIGGERED!")
                                Timber.d("🆔 Authenticated roll number: $rollNumber")

                                // Validate roll number matches profile
                                if (rollNumber != profileData.rollNumber) {
                                    Timber.w("⚠️ Roll number mismatch: authenticated=$rollNumber, profile=${profileData.rollNumber}")
                                    errorMessage = "Roll number mismatch. Please contact administrator."
                                    return@FaceIoAuthWebView
                                }

                                // Mark attendance
                                markAttendanceWithSession(rollNumber)
                            },
                            onError = { error ->
                                Timber.e("❌ WebView authentication error: $error")
                                errorMessage = "Face authentication failed: $error"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = Color(0xFF007AFF)
            )
            Text(
                text = message,
                fontSize = 16.sp,
                color = Color(0xFF8E8E93),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProcessingAttendanceContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color(0xFF007AFF),
                    strokeWidth = 4.dp
                )

                Text(
                    text = "Processing Attendance",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1D1F),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Please wait while we mark your attendance...",
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFF3B30).copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "📷",
                    fontSize = 64.sp
                )

                Text(
                    text = "Camera Permission Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1D1F),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Face authentication requires camera access to verify your identity and mark attendance.",
                    fontSize = 16.sp,
                    color = Color(0xFF8E8E93),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF)
                    )
                ) {
                    Text("Go Back", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun AttendanceErrorContent(
    errorMessage: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFF3B30).copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "⚠️",
                    fontSize = 64.sp
                )

                Text(
                    text = "Attendance Failed",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1D1F),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = errorMessage,
                    fontSize = 16.sp,
                    color = Color(0xFF8E8E93),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        colors = buttonColors(
                            contentColor = Color(0xFF8E8E93)
                        )
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF)
                        )
                    ) {
                        Text("Try Again", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}