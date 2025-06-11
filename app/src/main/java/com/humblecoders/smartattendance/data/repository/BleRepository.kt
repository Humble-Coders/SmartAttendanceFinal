package com.humblecoders.smartattendance.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.welie.blessed.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class BleRepository(private val context: Context) {

    private val _bleState = MutableStateFlow(BleState.IDLE)
    val bleState: StateFlow<BleState> = _bleState.asStateFlow()

    private val _esp32DeviceFound = MutableStateFlow(false)
    val esp32DeviceFound: StateFlow<Boolean> = _esp32DeviceFound.asStateFlow()

    private lateinit var central: BluetoothCentralManager

    fun initializeBle() {
        central = BluetoothCentralManager(context)
    }

    fun startScanning() {
        if (hasRequiredPermissions()) {
            _bleState.value = BleState.SCANNING
            _esp32DeviceFound.value = false

            // Scan for all peripherals
            central.scanForPeripherals(
                resultCallback = { peripheral, scanResult ->
                    // Check if this is our ESP32 device
                    val deviceName = peripheral.name
                    if (deviceName != null && deviceName.contains("ESP32", ignoreCase = true)) {
                        _esp32DeviceFound.value = true
                        _bleState.value = BleState.DEVICE_FOUND
                    }
                },
                scanError = { scanFailure ->
                    Timber.e("Scan failed with reason: $scanFailure")
                    _bleState.value = BleState.IDLE
                }
            )
        } else {
            _bleState.value = BleState.NO_PERMISSION
        }
    }

    fun stopScanning() {
        central.stopScan()
        _bleState.value = BleState.IDLE
        _esp32DeviceFound.value = false
    }

    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

enum class BleState {
    IDLE,
    SCANNING,
    DEVICE_FOUND,
    BLUETOOTH_OFF,
    NO_PERMISSION
}