package com.humblecoders.smartattendance.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.humblecoders.smartattendance.utils.BluetoothManager
import timber.log.Timber

/**
 * Dialog to enable Bluetooth when it's turned off
 */
@Composable
fun BluetoothEnableDialog(
    bluetoothManager: BluetoothManager,
    onBluetoothEnabled: () -> Unit,
    onCancel: () -> Unit
) {
    var isEnabling by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isEnabling) onCancel() },
        properties = DialogProperties(
            dismissOnBackPress = !isEnabling,
            dismissOnClickOutside = !isEnabling
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bluetooth Icon
                Icon(
                    imageVector = Icons.Default.BluetoothDisabled,
                    contentDescription = "Bluetooth Disabled",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFFF3B30)
                )

                // Title
                Text(
                    text = "Bluetooth Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1D1F),
                    textAlign = TextAlign.Center
                )

                // Description
                Text(
                    text = "This app needs Bluetooth to detect attendance devices in your classroom. Please enable Bluetooth to continue.",
                    fontSize = 16.sp,
                    color = Color(0xFF8E8E93),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                // Enable Button
                Button(
                    onClick = {
                        isEnabling = true
                        bluetoothManager.requestEnableBluetooth { enabled ->
                            isEnabling = false
                            if (enabled) {
                                Timber.d("📡 Bluetooth enabled successfully")
                                onBluetoothEnabled()
                            } else {
                                Timber.w("📡 Bluetooth enable failed or cancelled")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isEnabling,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF)
                    )
                ) {
                    if (isEnabling) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enabling...", fontWeight = FontWeight.Medium)
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = "Enable Bluetooth",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enable Bluetooth", fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Cancel Button
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isEnabling
                ) {
                    Text("Cancel", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/**
 * Dialog for permission instructions and settings redirect
 */
@Composable
fun BluetoothPermissionInstructionsDialog(
    bluetoothManager: BluetoothManager,
    onDismiss: () -> Unit
) {
    var hasOpenedSettings by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Settings Icon
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF007AFF)
                )

                // Title
                Text(
                    text = "Bluetooth Permissions Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1D1F),
                    textAlign = TextAlign.Center
                )

                // Instructions
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "To mark attendance automatically, this app needs Bluetooth permissions.",
                        fontSize = 16.sp,
                        color = Color(0xFF8E8E93),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF007AFF).copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Follow these steps:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF007AFF)
                            )
                            Text(
                                text = "1. Tap 'Open Settings' below",
                                fontSize = 14.sp,
                                color = Color(0xFF1D1D1F)
                            )
                            Text(
                                text = "2. Find 'Permissions' section",
                                fontSize = 14.sp,
                                color = Color(0xFF1D1D1F)
                            )
                            Text(
                                text = "3. Enable all Bluetooth permissions",
                                fontSize = 14.sp,
                                color = Color(0xFF1D1D1F)
                            )
                            Text(
                                text = "4. Return to the app",
                                fontSize = 14.sp,
                                color = Color(0xFF1D1D1F)
                            )
                        }
                    }
                }

                // Open Settings Button
                Button(
                    onClick = {
                        hasOpenedSettings = true
                        bluetoothManager.openAppSettings()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Settings", fontWeight = FontWeight.Medium)
                    }
                }

                // Additional info if settings opened
                if (hasOpenedSettings) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF34C759).copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "After enabling permissions, return to this app and the attendance feature will work automatically.",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            color = Color(0xFF34C759),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Cancel Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/**
 * Enhanced Bluetooth permission handler with state management
 */
@Composable
fun EnhancedBluetoothPermissionHandler(
    bluetoothManager: BluetoothManager,
    onPermissionsGranted: () -> Unit,
    onPermissionsPermanentlyDenied: () -> Unit
) {
    var permissionState by remember { mutableStateOf(PermissionState.CHECKING) }
    var showEnableDialog by remember { mutableStateOf(false) }
    var showInstructionsDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Check initial state
    LaunchedEffect(Unit) {
        when {
            !bluetoothManager.isBluetoothSupported() -> {
                permissionState = PermissionState.NOT_SUPPORTED
            }
            !bluetoothManager.isBluetoothEnabled() -> {
                permissionState = PermissionState.BLUETOOTH_DISABLED
                showEnableDialog = true
            }
            bluetoothManager.hasRequiredPermissions() -> {
                permissionState = PermissionState.GRANTED
                onPermissionsGranted()
            }
            bluetoothManager.getPermissionDenialCount() >= 2 -> {
                permissionState = PermissionState.PERMANENTLY_DENIED
                showInstructionsDialog = true
            }
            else -> {
                permissionState = PermissionState.NEEDS_PERMISSION
                requestPermissions(bluetoothManager) { granted ->
                    if (granted) {
                        permissionState = PermissionState.GRANTED
                        onPermissionsGranted()
                    } else {
                        if (bluetoothManager.getPermissionDenialCount() >= 2) {
                            permissionState = PermissionState.PERMANENTLY_DENIED
                            showInstructionsDialog = true
                            onPermissionsPermanentlyDenied()
                        }
                    }
                }
            }
        }
    }

    // Show Bluetooth enable dialog
    if (showEnableDialog) {
        BluetoothEnableDialog(
            bluetoothManager = bluetoothManager,
            onBluetoothEnabled = {
                showEnableDialog = false
                // Re-check permissions after Bluetooth is enabled
                if (bluetoothManager.hasRequiredPermissions()) {
                    permissionState = PermissionState.GRANTED
                    onPermissionsGranted()
                } else {
                    permissionState = PermissionState.NEEDS_PERMISSION
                    requestPermissions(bluetoothManager) { granted ->
                        if (granted) {
                            permissionState = PermissionState.GRANTED
                            onPermissionsGranted()
                        }
                    }
                }
            },
            onCancel = {
                showEnableDialog = false
                permissionState = PermissionState.CANCELLED
            }
        )
    }

    // Show instructions dialog
    if (showInstructionsDialog) {
        BluetoothPermissionInstructionsDialog(
            bluetoothManager = bluetoothManager,
            onDismiss = {
                showInstructionsDialog = false
            }
        )
    }
}

private fun requestPermissions(
    bluetoothManager: BluetoothManager,
    onResult: (Boolean) -> Unit
) {
    bluetoothManager.requestPermissions { granted ->
        onResult(granted)
    }
}

enum class PermissionState {
    CHECKING,
    NEEDS_PERMISSION,
    GRANTED,
    PERMANENTLY_DENIED,
    BLUETOOTH_DISABLED,
    NOT_SUPPORTED,
    CANCELLED
}