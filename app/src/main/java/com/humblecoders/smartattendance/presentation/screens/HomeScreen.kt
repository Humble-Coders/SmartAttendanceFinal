package com.humblecoders.smartattendance.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.humblecoders.smartattendance.data.repository.BleState
import com.humblecoders.smartattendance.presentation.components.AttendanceConfirmationDialog
import com.humblecoders.smartattendance.presentation.components.BluetoothPermissionHandler
import com.humblecoders.smartattendance.presentation.components.FaceIoDeleteWebView
import com.humblecoders.smartattendance.presentation.viewmodel.BleViewModel
import com.humblecoders.smartattendance.presentation.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    bleViewModel: BleViewModel,
    onProfileClick: () -> Unit,
    onAttendanceClick: () -> Unit,
    profileViewModel: ProfileViewModel
) {
    val bleState by bleViewModel.bleState.collectAsState()
    val esp32DeviceFound by bleViewModel.esp32DeviceFound.collectAsState()
    val subjectCode by bleViewModel.subjectCode.collectAsState()
    val profileData by profileViewModel.profileData.collectAsState()
    val isDeleting by profileViewModel.isDeleting.collectAsState()
    var showAttendanceDialog by remember { mutableStateOf(false) }
    var showDeleteFacesDialog by remember { mutableStateOf(false) }
    var showDeleteWebView by remember { mutableStateOf(false) }
    var showDeleteCompleteDialog by remember { mutableStateOf(false) }
    var deleteResultMessage by remember { mutableStateOf("") }

    // Handle Bluetooth permissions
    BluetoothPermissionHandler(
        onPermissionsGranted = {
            // Restart scanning after permissions are granted
            bleViewModel.restartScanning()
        }
    )

    // Show attendance dialog when ESP32 is detected
    LaunchedEffect(esp32DeviceFound) {
        if (esp32DeviceFound) {
            showAttendanceDialog = true
        }
    }

    // Enhanced Attendance Confirmation Dialog with Subject Code
    if (showAttendanceDialog) {
        AttendanceConfirmationDialog(
            subjectCode = subjectCode ?: "Unknown Subject",
            onConfirm = {
                showAttendanceDialog = false
                // Navigate to attendance marking
                onAttendanceClick()
                // Reset BLE detection and continue scanning for next device
                bleViewModel.resetDeviceFoundAndContinueScanning()
            },
            onDismiss = {
                showAttendanceDialog = false
                // Stop scanning completely when user cancels
                bleViewModel.stopScanning()
                // Reset BLE detection
                bleViewModel.resetDeviceFound()
            }
        )
    }

    // Delete Registered Faces Confirmation Dialog
    if (showDeleteFacesDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteFacesDialog = false },
            icon = {
                Text(
                    text = "🗑️",
                    style = MaterialTheme.typography.displayMedium
                )
            },
            title = {
                Text(
                    text = "Delete All Registered Faces",
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "This will completely reset the face recognition system:",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Delete all faces from Face.io service",
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Reset app's face registration status",
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Clear all biometric data",
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This action cannot be undone!",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteFacesDialog = false
                        showDeleteWebView = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteFacesDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete WebView for Face.io deletion
    if (showDeleteWebView) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Deleting All Faces...",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isDeleting) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Please wait...")
                    }

                    FaceIoDeleteWebView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        onDeleteComplete = { success, message ->
                            // Handle Face.io deletion result
                            profileViewModel.deleteAllFaces(
                                onSuccess = {
                                    deleteResultMessage = if (success) {
                                        "All faces deleted successfully! Face recognition has been reset."
                                    } else {
                                        "Local face data cleared. $message"
                                    }
                                    showDeleteWebView = false
                                    showDeleteCompleteDialog = true
                                },
                                onError = { error ->
                                    deleteResultMessage = "Error: $error"
                                    showDeleteWebView = false
                                    showDeleteCompleteDialog = true
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    // Delete Complete Dialog
    if (showDeleteCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCompleteDialog = false },
            icon = {
                Text(
                    text = "✅",
                    style = MaterialTheme.typography.displayMedium
                )
            },
            title = {
                Text(
                    text = "Deletion Complete",
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = deleteResultMessage,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDeleteCompleteDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Attendance") },
                actions = {
                    // Delete Faces button (for testing)
                    if (profileData.isFaceRegistered) {
                        IconButton(
                            onClick = { showDeleteFacesDialog = true }
                        ) {
                            Text(
                                text = "🗑️",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    // Refresh button for manual scan restart
                    IconButton(
                        onClick = {
                            bleViewModel.restartScanning()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart Scan"
                        )
                    }

                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Spacer(modifier = Modifier.height(32.dp))

            // Welcome Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📡",
                        style = MaterialTheme.typography.displayLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Smart Attendance System",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Automatic attendance via BLE detection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // BLE Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (bleState) {
                        BleState.SCANNING -> MaterialTheme.colorScheme.secondaryContainer
                        BleState.DEVICE_FOUND -> MaterialTheme.colorScheme.tertiaryContainer
                        BleState.BLUETOOTH_OFF -> MaterialTheme.colorScheme.errorContainer
                        BleState.NO_PERMISSION -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status Icon
                    Text(
                        text = when (bleState) {
                            BleState.SCANNING -> "🔍"
                            BleState.DEVICE_FOUND -> "✅"
                            BleState.BLUETOOTH_OFF -> "📴"
                            BleState.NO_PERMISSION -> "⚠️"
                            else -> "⚪"
                        },
                        style = MaterialTheme.typography.displaySmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "BLE Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when (bleState) {
                            BleState.IDLE -> "Press refresh to start scanning"
                            BleState.SCANNING -> "Scanning for devices..."
                            BleState.DEVICE_FOUND -> "Attendance device detected!"
                            BleState.BLUETOOTH_OFF -> "Bluetooth is disabled"
                            BleState.NO_PERMISSION -> "Permission required"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        fontWeight = if (bleState == BleState.DEVICE_FOUND) FontWeight.Medium else FontWeight.Normal
                    )

                    // Show scanning animation
                    if (bleState == BleState.SCANNING) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // Show subject code when device is found
                    if (bleState == BleState.DEVICE_FOUND && subjectCode != null) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Subject Code",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = subjectCode!!,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Instructions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "How it works:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val instructions = listOf(
                        "📍 Keep Bluetooth enabled",
                        "🔍 Press refresh button to start scanning",
                        "📱 When detected, confirm to mark attendance",
                        "😊 Face verification completes the process"
                    )

                    instructions.forEach { instruction ->
                        Text(
                            text = instruction,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            when (bleState) {
                BleState.NO_PERMISSION -> {
                    Button(
                        onClick = {
                            bleViewModel.restartScanning()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Bluetooth Permissions")
                    }
                }
                BleState.BLUETOOTH_OFF -> {
                    OutlinedButton(
                        onClick = {
                            // Note: In a real app, you might want to guide user to settings
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enable Bluetooth in Settings")
                    }
                }
                BleState.IDLE -> {
                    Button(
                        onClick = {
                            bleViewModel.startScanning()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Scanning")
                    }
                }
                BleState.SCANNING -> {
                    OutlinedButton(
                        onClick = {
                            bleViewModel.stopScanning()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Stop Scanning")
                    }
                }
                else -> {
                    // For DEVICE_FOUND state, scanning is handled by dialog
                }
            }
        }
    }
}