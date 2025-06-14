package com.humblecoders.smartattendance.data.repository

import android.Manifest
import android.bluetooth.le.ScanResult
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

    private val _deviceFound = MutableStateFlow(false)
    val deviceFound: StateFlow<Boolean> = _deviceFound.asStateFlow()

    private val _detectedDeviceRoom = MutableStateFlow<String?>(null)
    val detectedDeviceRoom: StateFlow<String?> = _detectedDeviceRoom.asStateFlow()

    private var central: BluetoothCentralManager? = null
    private var isScanning = false
    private var targetRoom: String? = null

    fun initializeBle() {
        try {
            central = BluetoothCentralManager(context)
            Timber.d("📡 BLE Central Manager initialized")
        } catch (e: Exception) {
            Timber.e(e, "📡 Failed to initialize BLE Central Manager")
        }
    }

    /**
     * Start scanning for specific room device
     */
    fun startScanningForRoom(roomName: String) {
        if (!hasRequiredPermissions()) {
            _bleState.value = BleState.NO_PERMISSION
            Timber.w("📡 BLE permissions not granted")
            return
        }

        if (isScanning) {
            Timber.d("📡 Already scanning, updating target room to: $roomName")
            targetRoom = roomName
            return
        }

        targetRoom = roomName
        startScanning()
    }

    /**
     * Start general scanning (for backward compatibility)
     */
    fun startScanning() {
        if (!hasRequiredPermissions()) {
            _bleState.value = BleState.NO_PERMISSION
            return
        }

        if (isScanning) {
            Timber.d("📡 Already scanning, ignoring start request")
            return
        }

        central?.let { centralManager ->
            try {
                clearDetectionState()
                _bleState.value = BleState.SCANNING
                isScanning = true

                Timber.d("📡 Starting BLE scan for devices with name: 'Humble Coders'")
                if (targetRoom != null) {
                    Timber.d("🎯 Target room: $targetRoom")
                }

                centralManager.scanForPeripheralsWithNames(
                    peripheralNames = arrayOf("Humble Coders"),
                    resultCallback = { peripheral, scanResult ->
                        if (isScanning) {
                            handleDeviceDiscovered(peripheral, scanResult)
                        }
                    },
                    scanError = { scanFailure ->
                        Timber.e("📡 Scan failed: $scanFailure")
                        isScanning = false
                        _bleState.value = BleState.IDLE
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "📡 Failed to start scanning")
                isScanning = false
                _bleState.value = BleState.IDLE
            }
        } ?: run {
            Timber.e("📡 Central manager not initialized")
            initializeBle()
        }
    }

    fun stopScanning() {
        central?.let { centralManager ->
            try {
                centralManager.stopScan()
                isScanning = false
                _bleState.value = BleState.IDLE
                clearDetectionState()
                Timber.d("📡 Stopped BLE scanning")
            } catch (e: Exception) {
                Timber.e(e, "📡 Failed to stop scanning")
                isScanning = false
                _bleState.value = BleState.IDLE
            }
        }
    }

    private fun handleDeviceDiscovered(peripheral: BluetoothPeripheral, scanResult: ScanResult) {
        if (!isScanning) {
            return
        }

        val deviceName = peripheral.name
        Timber.d("📡 Discovered device: $deviceName, RSSI: ${scanResult.rssi}")

        if (deviceName == "Humble Coders") {
            val extractedRoom = extractRoomFromDevice(scanResult)

            if (extractedRoom != null) {
                Timber.d("📡 Extracted room: $extractedRoom")

                // Check if this matches our target room (if we have one)
                if (targetRoom != null) {
                    if (isRoomMatch(extractedRoom, targetRoom!!)) {
                        handleRoomMatch(extractedRoom)
                    } else {
                        Timber.d("📡 Room mismatch: detected=$extractedRoom, target=$targetRoom")
                    }
                } else {
                    // No specific target, accept any valid room
                    handleRoomMatch(extractedRoom)
                }
            } else {
                Timber.w("📡 Could not extract room from device")
            }
        }
    }

    private fun extractRoomFromDevice(scanResult: ScanResult): String? {
        val scanRecord = scanResult.scanRecord ?: return null

        try {
            // Try manufacturer data with ID 0xFFFF first
            val manufacturerData = scanRecord.getManufacturerSpecificData(0xFFFF)
            manufacturerData?.let { data ->
                if (data.isNotEmpty()) {
                    val roomData = String(data, Charsets.UTF_8).trim()
                    if (isValidRoomFormat(roomData)) {
                        return roomData
                    }
                }
            }

            // Try all available manufacturer data
            val allManufacturerData = scanRecord.manufacturerSpecificData
            if (allManufacturerData != null) {
                for (i in 0 until allManufacturerData.size()) {
                    val data = allManufacturerData.valueAt(i)
                    data?.let { bytes ->
                        if (bytes.isNotEmpty()) {
                            val roomData = String(bytes, Charsets.UTF_8).trim()
                            if (isValidRoomFormat(roomData)) {
                                return roomData
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "📡 Error extracting room data")
        }

        return null
    }

    private fun isValidRoomFormat(roomData: String): Boolean {
        // Valid format: Room name + 3 digits (e.g., "LT101123")
        // Minimum 5 characters (2 for room + 3 digits)
        if (roomData.length < 5) return false

        val last3 = roomData.takeLast(3)
        return last3.all { it.isDigit() }
    }

    private fun isRoomMatch(detectedRoom: String, targetRoom: String): Boolean {
        // Extract room name from detected room (remove 3 digits)
        val roomName = if (detectedRoom.length >= 3) {
            val suffix = detectedRoom.takeLast(3)
            if (suffix.all { it.isDigit() }) {
                detectedRoom.dropLast(3)
            } else {
                detectedRoom
            }
        } else {
            detectedRoom
        }

        return targetRoom.equals(roomName, ignoreCase = true)
    }

    private fun handleRoomMatch(detectedRoom: String) {
        // Stop scanning when correct device is found
        central?.stopScan()
        isScanning = false

        // Update state
        _detectedDeviceRoom.value = detectedRoom
        _deviceFound.value = true
        _bleState.value = BleState.DEVICE_FOUND

        Timber.i("📡 ✅ Room device detected: $detectedRoom")
    }

    fun resetDeviceFound() {
        clearDetectionState()
        if (_bleState.value == BleState.DEVICE_FOUND) {
            _bleState.value = BleState.IDLE
        }
        Timber.d("📡 Device detection state reset")
    }

    /**
     * Reset and continue scanning for next device
     */
    fun resetAndContinueScanning() {
        clearDetectionState()
        if (targetRoom != null) {
            startScanningForRoom(targetRoom!!)
        } else {
            startScanning()
        }
        Timber.d("📡 Reset detection and continued scanning")
    }

    private fun clearDetectionState() {
        _deviceFound.value = false
        _detectedDeviceRoom.value = null
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

    /**
     * Get current target room
     */
    fun getCurrentTargetRoom(): String? = targetRoom

    /**
     * Check if currently scanning for specific room
     */
    fun isScanningForRoom(): Boolean = isScanning && targetRoom != null
}

enum class BleState {
    IDLE,
    SCANNING,
    DEVICE_FOUND,
    BLUETOOTH_OFF,
    NO_PERMISSION
}