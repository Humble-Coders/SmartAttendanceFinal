package com.humblecoders.smartattendance.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * Handles Bluetooth permissions and state management
 */
class BluetoothManager(private val activity: ComponentActivity) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var permissionDenialCount = 0
    private var onPermissionResult: ((Boolean) -> Unit)? = null
    private var onBluetoothEnabled: ((Boolean) -> Unit)? = null

    // Permission launcher
    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }

            if (allGranted) {
                Timber.d("📡 All Bluetooth permissions granted")
                permissionDenialCount = 0
                onPermissionResult?.invoke(true)
            } else {
                permissionDenialCount++
                Timber.w("📡 Bluetooth permissions denied. Count: $permissionDenialCount")

                if (permissionDenialCount >= 2) {
                    Timber.d("📡 Permission denied 2+ times, need to open settings")
                }
                onPermissionResult?.invoke(false)
            }
        }

    // Bluetooth enable launcher
    private val bluetoothEnableLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val isEnabled = bluetoothAdapter?.isEnabled == true
            Timber.d("📡 Bluetooth enable result: enabled=$isEnabled")
            onBluetoothEnabled?.invoke(isEnabled)
        }

    /**
     * Check if Bluetooth is available on device
     */
    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }

    fun recheckBluetoothState(): BluetoothStateResult {
        val isSupported = isBluetoothSupported()
        val isEnabled = isBluetoothEnabled()
        val hasPermissions = hasRequiredPermissions()

        Timber.d("📡 Bluetooth state recheck: supported=$isSupported, enabled=$isEnabled, permissions=$hasPermissions")

        return BluetoothStateResult(
            isSupported = isSupported,
            isEnabled = isEnabled,
            hasPermissions = hasPermissions,
            denialCount = permissionDenialCount
        )
    }

    /**
     * Check if Bluetooth is currently enabled
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Check if required permissions are granted
     */
    fun hasRequiredPermissions(): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Get required permissions based on Android version
     */
    private fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /**
     * Check if we should show permission rationale
     */
    fun shouldShowPermissionRationale(): Boolean {
        return getRequiredPermissions().any { permission ->
            activity.shouldShowRequestPermissionRationale(permission)
        }
    }

    /**
     * Request Bluetooth permissions
     */
    fun requestPermissions(onResult: (Boolean) -> Unit) {
        onPermissionResult = onResult

        val permissions = getRequiredPermissions().toTypedArray()
        Timber.d("📡 Requesting permissions: ${permissions.joinToString()}")

        try {
            permissionLauncher.launch(permissions)
        } catch (e: Exception) {
            Timber.e(e, "📡 Failed to launch permission request")
            onResult(false)
        }
    }

    /**
     * Request to enable Bluetooth
     */
    fun requestEnableBluetooth(onResult: (Boolean) -> Unit) {
        onBluetoothEnabled = onResult

        if (!isBluetoothSupported()) {
            Timber.e("📡 Bluetooth not supported on this device")
            onResult(false)
            return
        }

        if (isBluetoothEnabled()) {
            Timber.d("📡 Bluetooth already enabled")
            onResult(true)
            return
        }

        // For Android 12+, need BLUETOOTH_CONNECT permission first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED) {
                Timber.w("📡 BLUETOOTH_CONNECT permission not granted")
                onResult(false)
                return
            }
        }

        try {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(enableBtIntent)
            Timber.d("📡 Bluetooth enable request launched")
        } catch (e: Exception) {
            Timber.e(e, "📡 Failed to launch Bluetooth enable request")
            onResult(false)
        }
    }

    /**
     * Open app settings for manual permission grant
     */
    fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            activity.startActivity(intent)
            Timber.d("📡 Opened app settings")
        } catch (e: Exception) {
            Timber.e(e, "📡 Failed to open app settings")
        }
    }

    /**
     * Get permission denial count
     */
    fun getPermissionDenialCount(): Int = permissionDenialCount

    /**
     * Reset permission denial count
     */
    fun resetPermissionDenialCount() {
        permissionDenialCount = 0
    }

    /**
     * Get Bluetooth state summary for debugging
     */
    fun getBluetoothStateSummary(): String {
        return "Bluetooth: supported=${isBluetoothSupported()}, " +
                "enabled=${isBluetoothEnabled()}, " +
                "permissions=${hasRequiredPermissions()}, " +
                "denialCount=$permissionDenialCount"
    }
}



data class BluetoothStateResult(
    val isSupported: Boolean,
    val isEnabled: Boolean,
    val hasPermissions: Boolean,
    val denialCount: Int
) {
    val isFullyReady: Boolean get() = isSupported && isEnabled && hasPermissions
}