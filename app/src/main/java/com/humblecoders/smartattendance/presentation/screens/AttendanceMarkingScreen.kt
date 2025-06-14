package com.humblecoders.smartattendance.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.humblecoders.smartattendance.presentation.components.CameraPermissionHandler
import com.humblecoders.smartattendance.presentation.components.FaceIoAuthWebView
import com.humblecoders.smartattendance.presentation.viewmodel.AttendanceViewModel
import kotlinx.coroutines.delay
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceMarkingScreen(
    attendanceViewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit
) {
    var showSuccess by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    // FIX: Add comprehensive logging for the screen lifecycle
    LaunchedEffect(Unit) {
        Timber.d("🎬 AttendanceMarkingScreen: Screen launched")
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mark Attendance") },
                navigationIcon = {
                    IconButton(onClick = {
                        Timber.d("🔙 User clicked back button")
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
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
                    // Show permission denied message
                    PermissionDeniedContent(
                        onCancel = {
                            Timber.d("🔙 User cancelled due to permission denial")
                            onNavigateBack()
                        }
                    )
                }
                !hasCameraPermission -> {
                    Timber.d("🎬 Waiting for camera permission")
                    // Show loading while waiting for permission
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Requesting camera permission...")
                        }
                    }
                }
                showSuccess -> {
                    Timber.d("🎬 Showing success content")
                    // Show success message
                    AttendanceSuccessContent(
                        message = successMessage,
                        onDone = {
                            Timber.d("🔙 User completed attendance marking successfully")
                            onNavigateBack()
                        }
                    )
                }
                errorMessage != null -> {
                    Timber.d("🎬 Showing error content: $errorMessage")
                    // Show error message
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
                else -> {
                    Timber.d("🎬 Showing Face.io authentication WebView")
                    // Show Face.io authentication WebView
                    FaceIoAuthWebView(
                        modifier = Modifier.fillMaxSize(),
                        onAuthenticated = { rollNumber ->
                            // FIX: Add comprehensive logging for the callback
                            Timber.d("🔥 WEBVIEW CALLBACK TRIGGERED!")
                            Timber.d("🆔 Authenticated roll number: $rollNumber")

                            try {
                                // Mark attendance for this roll number
                                Timber.d("📞 Calling attendanceViewModel.markAttendance...")
                                attendanceViewModel.markAttendance(
                                    rollNumber = rollNumber,
                                    subjectCode = "Unknown", // TODO: Get from BLE if needed
                                    onSuccess = {
                                        Timber.i("🎉 Attendance marking SUCCESS callback triggered")
                                        successMessage = "Attendance marked for Roll No: $rollNumber"
                                        showSuccess = true
                                        errorMessage = null
                                    },
                                    onError = { error ->
                                        Timber.e("❌ Attendance marking ERROR callback triggered: $error")
                                        errorMessage = error
                                        showSuccess = false
                                    }
                                )
                                Timber.d("✅ attendanceViewModel.markAttendance call completed")
                            } catch (e: Exception) {
                                Timber.e(e, "💥 Exception while calling markAttendance")
                                errorMessage = "Failed to process attendance: ${e.message}"
                            }
                        },
                        onError = { error ->
                            Timber.e("❌ WebView authentication error: $error")
                            errorMessage = error
                        }
                    )
                }
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
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📷",
                    style = MaterialTheme.typography.displayLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Attendance marking requires camera access for face verification.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go Back")
                }
            }
        }
    }
}

@Composable
private fun AttendanceSuccessContent(
    message: String,
    onDone: () -> Unit
) {
    LaunchedEffect(Unit) {
        Timber.d("⏰ Auto-navigation timer started (3 seconds)")
        delay(3000) // Auto navigate after 3 seconds
        Timber.d("⏰ Auto-navigation triggered")
        onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Attendance Marked!",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        Timber.d("✅ User manually clicked Done button")
                        onDone()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
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
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚠",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Attendance Failed",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = {
                            Timber.d("🔙 User cancelled after error")
                            onCancel()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = {
                            Timber.d("🔄 User retrying after error")
                            onRetry()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}