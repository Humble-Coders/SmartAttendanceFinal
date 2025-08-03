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
    onNavigateToSuccess: (AttendanceSuccessData) -> Unit,
    presetDeviceRoom: String = "" // Add this parameter

) {
    // Collect state from ViewModels
    val profileData by profileViewModel.profileData.collectAsState()
    val currentSession by attendanceViewModel.currentSession.collectAsState()
    val detectedDeviceRoom by bleViewModel.detectedDeviceRoom.collectAsState()
    val preservedDeviceRoom by attendanceViewModel.preservedDeviceRoom.collectAsState()
    val isFaceRecognitionEnabled by attendanceViewModel.isFaceRecognitionEnabled.collectAsState()


    // ADD: Capture device room in local state to preserve it
    var capturedDeviceRoom by remember { mutableStateOf(presetDeviceRoom.ifBlank { detectedDeviceRoom }) }

    // Local UI state
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var isProcessingAttendance by remember { mutableStateOf(false) }
    var isValidatingAttendance by remember { mutableStateOf(false) }
    var attendanceValidated by remember { mutableStateOf(false) }

    fun stopScanningAndNavigateBack() {
        Timber.d("🛑 Stopping BLE scanning due to navigation back from attendance screen")
        bleViewModel.stopScanning()
        bleViewModel.resetDeviceFound()
        onNavigateBack()
    }

    // Capture device room when screen launches
    LaunchedEffect(Unit) {
        Timber.d("🎬 AttendanceMarkingScreen: Screen launched")
        Timber.d("📋 Profile: ${profileData.name}, Roll: ${profileData.rollNumber}, Class: ${profileData.className}")
        Timber.d("📚 Session: ${currentSession?.subject}, Room: ${currentSession?.room}")
        Timber.d("📡 BLE Device Room: $detectedDeviceRoom")
        Timber.d("📡 Preserved device room from ViewModel: '$preservedDeviceRoom'")

        capturedDeviceRoom = preservedDeviceRoom ?: ""
        Timber.d("📡 Using device room: '$capturedDeviceRoom'")


        kotlinx.coroutines.delay(100)

        attendanceViewModel.clearPreservedDeviceRoom()


        Timber.d("📡 Final Captured Device Room: $capturedDeviceRoom")

        Timber.d("🎬 AttendanceMarkingScreen: Clearing any overlay states")
        // Stop BLE scanning and reset detection state to prevent overlays
        bleViewModel.stopScanning()
        bleViewModel.resetDeviceFound()
    }

    DisposableEffect(Unit) {
        onDispose {
            Timber.d("🎬 AttendanceMarkingScreen: Screen disposed")
        }
    }


    fun markAttendanceDirectly() {
        if (isProcessingAttendance) {
            Timber.w("⚠️ Attendance already being processed, ignoring duplicate request")
            return
        }

        val session = currentSession!!
        isProcessingAttendance = true
        val isExtra = session.isExtra
        val currentDetectedDeviceRoom = attendanceViewModel.deviceRoom

        Timber.d("🎯 Marking attendance directly (no face recognition)")
        Timber.d("📋 Student: ${profileData.name} (${profileData.rollNumber})")
        Timber.d("📡 Using Device room: '$currentDetectedDeviceRoom'")

        attendanceViewModel.markAttendanceAfterAuth(
            rollNumber = profileData.rollNumber,
            deviceRoom = currentDetectedDeviceRoom.toString(),
            isExtra = isExtra,
            onSuccess = {
                Timber.i("🎉 Attendance marked successfully (direct)!")

                val successData = AttendanceSuccessData(
                    rollNumber = profileData.rollNumber,
                    studentName = profileData.name,
                    subject = session.subject,
                    room = session.room,
                    type = session.type,
                    deviceRoom = currentDetectedDeviceRoom.toString(),
                    attendanceId = "att_${System.currentTimeMillis()}"
                )

                isProcessingAttendance = false
                onNavigateToSuccess(successData)
            },
            onError = { error ->
                Timber.e("❌ Direct attendance marking failed: $error")
                errorMessage = error
                isProcessingAttendance = false
            }
        )
    }

    // Replace the existing performAttendanceValidation function
    fun performAttendanceValidation() {
        val session = currentSession
        if (session == null) {
            Timber.e("❌ No active session available for attendance validation")
            errorMessage = "No active session found. Please return to home and try again."
            return
        }

        if (profileData.className.isBlank()) {
            Timber.e("❌ No class information available")
            errorMessage = "Class information not found. Please update your profile."
            return
        }

        isValidatingAttendance = true
        val isExtra = session.isExtra
        val attendanceType = if (isExtra) "extra" else "regular"

        Timber.d("🔍 Starting pre-authentication attendance validation")
        Timber.d("🔧 Face recognition enabled: $isFaceRecognitionEnabled")

        attendanceViewModel.validateAttendanceBeforeAuth(
            rollNumber = profileData.rollNumber,
            deviceRoom = capturedDeviceRoom ?: "",
            isExtra = isExtra,
            onSuccess = {
                Timber.i("✅ $attendanceType attendance validation successful")
                attendanceValidated = true
                isValidatingAttendance = false

                // If face recognition is disabled, mark attendance directly
                if (isFaceRecognitionEnabled == false) {
                    Timber.d("🚀 Face recognition disabled, marking attendance directly")
                    markAttendanceDirectly()
                }
            },
            onError = { error ->
                Timber.e("❌ $attendanceType attendance validation failed: $error")
                errorMessage = error
                isValidatingAttendance = false
            },
            onStopScanning = {
                Timber.d("🛑 Stopping BLE scanning due to validation failure")
                bleViewModel.stopScanning()
            }
        )
    }

    // Add this new function to mark attendance without face recognition


    if (isFaceRecognitionEnabled == true) {
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
    } else if (isFaceRecognitionEnabled == false) {
        // If face recognition is disabled, mark camera permission as granted
        LaunchedEffect(Unit) {
            hasCameraPermission = true
            permissionDenied = false
        }
    }

    // NEW: Perform attendance validation when camera permission is granted
    // Update this LaunchedEffect
    LaunchedEffect(hasCameraPermission, isFaceRecognitionEnabled) {
        if (isFaceRecognitionEnabled == false) {
            // Skip camera permission for direct attendance
            if (!attendanceValidated && !isValidatingAttendance) {
                performAttendanceValidation()
            }
        } else if (isFaceRecognitionEnabled == true && hasCameraPermission && !attendanceValidated && !isValidatingAttendance) {
            performAttendanceValidation()
        }
    }
    // Function to mark attendance with roll number validation only
    fun markAttendanceWithRollNumber(rollNumber: String) {
        if (isProcessingAttendance) {
            Timber.w("⚠️ Attendance already being processed, ignoring duplicate request")
            return
        }

        // Only validate roll number mismatch
        if (rollNumber != profileData.rollNumber) {
            Timber.w("⚠️ Roll number mismatch: authenticated=$rollNumber, profile=${profileData.rollNumber}")
            errorMessage = "Roll number mismatch. Please contact administrator."
            return
        }

        val session = currentSession!!
        isProcessingAttendance = true
        val isExtra = session.isExtra

        // Use captured device room instead of current detected device room
        val currentDetectedDeviceRoom =attendanceViewModel.deviceRoom

        Timber.d("🎯 Starting final attendance marking (post Face.io auth)")
        Timber.d("📋 Authenticated roll number: $rollNumber")
        Timber.d("📡 Current Device Room: $detectedDeviceRoom") // Will be null
        Timber.d("📡 Using Captured Device room: '$currentDetectedDeviceRoom'") // Should have value

        attendanceViewModel.markAttendanceAfterAuth(
            rollNumber = rollNumber,
            deviceRoom = currentDetectedDeviceRoom.toString(), // Use captured value
            isExtra = isExtra,
            onSuccess = {
                Timber.i("🎉 Attendance marked successfully after Face.io auth!")

                val successData = AttendanceSuccessData(
                    rollNumber = rollNumber,
                    studentName = profileData.name,
                    subject = session.subject,
                    room = session.room,
                    type = session.type,
                    deviceRoom = currentDetectedDeviceRoom.toString(), // Use captured value
                    attendanceId = "att_${System.currentTimeMillis()}"
                )

                Timber.d("✅ Created success data: $successData")
                isProcessingAttendance = false
                onNavigateToSuccess(successData)
            },
            onError = { error ->
                Timber.e("❌ Final attendance marking failed: $error")
                errorMessage = error
                isProcessingAttendance = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { if(isFaceRecognitionEnabled==true) Text("Face Authentication") else Text("Marking Attendance") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            Timber.d("🔙 User clicked back button")
                            // Disable auto-scanning and stop current scanning
                            attendanceViewModel.disableAutoScan()
                            bleViewModel.stopScanning()
                            bleViewModel.resetDeviceFound()
                            onNavigateBack()
                        },
                        enabled = !isProcessingAttendance && !isValidatingAttendance
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
                isFaceRecognitionEnabled == null -> {
                    LoadingContent(message = "Loading configuration...")
                }

                permissionDenied && isFaceRecognitionEnabled == true -> {
                    PermissionDeniedContent(
                        onCancel = { stopScanningAndNavigateBack() },
                        bleViewModel = bleViewModel,
                        onNavigateBack = { stopScanningAndNavigateBack() },
                        attendanceViewModel = attendanceViewModel
                    )
                }

                !hasCameraPermission && isFaceRecognitionEnabled == true -> {
                    LoadingContent(message = "Requesting camera permission...")
                }

                isValidatingAttendance -> {
                    LoadingContent(message = "Validating attendance eligibility...")
                }

                errorMessage != null -> {
                    AttendanceErrorContent(
                        errorMessage = errorMessage!!,
                        onRetry = {
                            errorMessage = null
                            attendanceValidated = false
                            if (isFaceRecognitionEnabled == true && hasCameraPermission) {
                                performAttendanceValidation()
                            } else if (isFaceRecognitionEnabled == false) {
                                performAttendanceValidation()
                            }
                        },
                        onCancel = { stopScanningAndNavigateBack() }
                    )
                }

                isProcessingAttendance -> {
                    ProcessingAttendanceContent()
                }

                !attendanceValidated -> {
                    LoadingContent(message = "Preparing authentication...")
                }

                isFaceRecognitionEnabled == false -> {
                    // This case is handled in performAttendanceValidation
                    LoadingContent(message = "Processing attendance...")
                }

                else -> {
                    // Only show Face.io WebView if face recognition is enabled
                    FaceIoAuthWebView(
                        modifier = Modifier.fillMaxSize(),
                        onAuthenticated = { rollNumber ->
                            markAttendanceWithRollNumber(rollNumber)
                        },
                        onError = { error ->
                            errorMessage = "Face authentication failed: $error"
                        }
                    )
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
    onCancel: () -> Unit,
    bleViewModel: BleViewModel,
    onNavigateBack: () -> Unit,
    attendanceViewModel: AttendanceViewModel
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
                    onClick = {
                        Timber.d("🔙 User cancelled due to permission denial")
                        // Disable auto-scanning and stop current scanning
                        attendanceViewModel.disableAutoScan()
                        bleViewModel.stopScanning()
                        bleViewModel.resetDeviceFound()
                        onNavigateBack()
                    },
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

