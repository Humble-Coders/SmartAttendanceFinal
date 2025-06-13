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
import androidx.compose.ui.platform.LocalContext
import com.humblecoders.smartattendance.presentation.components.CameraPermissionHandler
import com.humblecoders.smartattendance.presentation.components.FaceIoWebView
import com.humblecoders.smartattendance.presentation.viewmodel.ProfileViewModel
import com.humblecoders.smartattendance.utils.SettingsUtil
import kotlinx.coroutines.delay
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceRegistrationScreen(
    profileViewModel: ProfileViewModel,
    onNavigateBack: () -> Unit
) {
    val profileData by profileViewModel.profileData.collectAsState()
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var isProcessingRegistration by remember { mutableStateOf(false) }

    // Handle camera permission
    CameraPermissionHandler(
        onPermissionGranted = {
            hasCameraPermission = true
            permissionDenied = false
            Timber.d("FaceRegistrationScreen - Camera permission granted")
        },
        onPermissionDenied = {
            permissionDenied = true
            Timber.w("FaceRegistrationScreen - Camera permission denied")
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Face Registration") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                    // Show permission denied message
                    PermissionDeniedContent(
                        onRetry = {
                            // This will trigger permission request again
                            permissionDenied = false
                        },
                        onCancel = onNavigateBack
                    )
                }
                !hasCameraPermission -> {
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
                isProcessingRegistration -> {
                    // Show processing state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Processing Registration...",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Please wait while we save your face data",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                showSuccess -> {
                    // Show success message
                    SuccessContent(
                        onDone = onNavigateBack
                    )
                }
                errorMessage != null -> {
                    // Show error message
                    ErrorContent(
                        errorMessage = errorMessage!!,
                        onRetry = {
                            errorMessage = null
                        },
                        onCancel = onNavigateBack
                    )
                }
                else -> {
                    // Show Face.io WebView only when camera permission is granted
                    FaceIoWebView(
                        modifier = Modifier.fillMaxSize(),
                        rollNumber = profileData.rollNumber,
                        onFaceRegistered = { faceId ->
                            Timber.d("FaceRegistrationScreen - Face registered with ID: $faceId")
                            isProcessingRegistration = true

                            // Update face registration status with comprehensive error handling
                            profileViewModel.updateFaceRegistrationStatus(
                                isRegistered = true,
                                onSuccess = {
                                    Timber.i("FaceRegistrationScreen - Face registration status updated successfully")
                                    isProcessingRegistration = false
                                    showSuccess = true
                                },
                                onError = { error ->
                                    Timber.e("FaceRegistrationScreen - Failed to update registration status: $error")
                                    isProcessingRegistration = false
                                    errorMessage = "Registration successful but failed to save status: $error"
                                }
                            )
                        },
                        onError = { error ->
                            Timber.e("FaceRegistrationScreen - Face registration error: $error")
                            isProcessingRegistration = false
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
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

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
                    text = "Face registration requires camera access to capture your face.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            SettingsUtil.openAppSettings(context)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Settings")
                    }

                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Try Again")
                    }

                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    onDone: () -> Unit
) {
    // Auto navigate after 3 seconds instead of 2 for better UX
    LaunchedEffect(Unit) {
        delay(3000)
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
                    text = "Face Registered Successfully!",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You can now mark attendance using face recognition",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Returning to profile in 3 seconds...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
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
                    text = "Registration Failed",
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
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}