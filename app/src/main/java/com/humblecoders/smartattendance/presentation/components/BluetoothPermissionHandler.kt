package com.humblecoders.smartattendance.presentation.components

import android.Manifest
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.humblecoders.smartattendance.utils.BluetoothManager
import timber.log.Timber

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BluetoothPermissionHandler(
    onPermissionsGranted: () -> Unit
) {
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = bluetoothPermissions
    )

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            onPermissionsGranted()
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }
}

/**
 * Enhanced permission handler with BluetoothManager integration
 */
@Composable
fun EnhancedBluetoothPermissionHandler(
    bluetoothManager: BluetoothManager,
    onPermissionsGranted: () -> Unit,
    onBluetoothDisabled: () -> Unit,
    onPermissionsPermanentlyDenied: () -> Unit
) {
    var permissionState by remember { mutableStateOf(PermissionState.CHECKING) }

    LaunchedEffect(Unit) {
        when {
            !bluetoothManager.isBluetoothSupported() -> {
                permissionState = PermissionState.NOT_SUPPORTED
                Timber.w("📡 Bluetooth not supported")
            }
            !bluetoothManager.isBluetoothEnabled() -> {
                permissionState = PermissionState.BLUETOOTH_DISABLED
                onBluetoothDisabled()
            }
            bluetoothManager.hasRequiredPermissions() -> {
                permissionState = PermissionState.GRANTED
                onPermissionsGranted()
            }
            bluetoothManager.getPermissionDenialCount() >= 2 -> {
                permissionState = PermissionState.PERMANENTLY_DENIED
                onPermissionsPermanentlyDenied()
            }
            else -> {
                permissionState = PermissionState.NEEDS_PERMISSION
                bluetoothManager.requestPermissions { granted ->
                    if (granted) {
                        permissionState = PermissionState.GRANTED
                        onPermissionsGranted()
                    } else {
                        if (bluetoothManager.getPermissionDenialCount() >= 2) {
                            permissionState = PermissionState.PERMANENTLY_DENIED
                            onPermissionsPermanentlyDenied()
                        }
                    }
                }
            }
        }
    }
}

