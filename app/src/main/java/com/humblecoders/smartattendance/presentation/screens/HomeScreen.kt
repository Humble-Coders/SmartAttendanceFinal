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
    var showAttendanceDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Attendance") },
                actions = {
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

            // Student Info Card (if profile saved)
            if (profileData.name.isNotBlank() && profileData.rollNumber.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "👤",
                            style = MaterialTheme.typography.displaySmall
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Student Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = profileData.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "Roll No: ${profileData.rollNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // BLE Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (bleState) {
                        BleState.SCANNING -> MaterialTheme.colorScheme.tertiaryContainer
                        BleState.DEVICE_FOUND -> MaterialTheme.colorScheme.primaryContainer
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
                            color = MaterialTheme.colorScheme.onTertiaryContainer
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
                        "😊 Face verification completes the process",
                        "👨‍💼 Face registration managed by admin"
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