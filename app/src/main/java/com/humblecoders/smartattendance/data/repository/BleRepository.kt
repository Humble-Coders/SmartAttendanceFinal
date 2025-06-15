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
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.bluetooth.BluetoothAdapter

class BleRepository(private val context: Context) {

    private val _bleState = MutableStateFlow(BleState.IDLE)
    val bleState: StateFlow<BleState> = _bleState.asStateFlow()

    private val _deviceFound = MutableStateFlow(false)
    val deviceFound: StateFlow<Boolean> = _deviceFound.asStateFlow()

    private val _detectedDeviceRoom = MutableStateFlow<String?>(null)
    val detectedDeviceRoom: StateFlow<String?> = _detectedDeviceRoom.asStateFlow()

    // NEW: Store the subject code from manufacturer data
    private val _detectedSubjectCode = MutableStateFlow<String?>(null)
    val detectedSubjectCode: StateFlow<String?> = _detectedSubjectCode.asStateFlow()

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

        // NEW: Check Bluetooth state
        if (getBluetoothState() != BluetoothAdapterState.ENABLED) {
            _bleState.value = BleState.BLUETOOTH_OFF
            Timber.w("📡 Bluetooth is not enabled")
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

        if (getBluetoothState() != BluetoothAdapterState.ENABLED) {
            _bleState.value = BleState.BLUETOOTH_OFF
            Timber.w("📡 Bluetooth is not enabled")
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

                Timber.d("📡 Starting BLE scan for devices with names starting with room codes")
                if (targetRoom != null) {
                    Timber.d("🎯 Target room: $targetRoom")
                }

                // Updated to scan for devices with names starting with room codes (like LT101, CR203, etc.)
                centralManager.scanForPeripherals(
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

        val deviceName = peripheral.name ?: return
        Timber.d("📡 Discovered device: $deviceName, RSSI: ${scanResult.rssi}")

        // Extract room from device name (e.g., "LT101123" -> "LT101")
        val extractedRoom = extractRoomFromDeviceName(deviceName)

        // Extract subject code from manufacturer data
        val subjectCode = extractSubjectFromManufacturerData(scanResult)

        if (extractedRoom != null && subjectCode != null) {
            Timber.d("📡 Extracted room: $extractedRoom, subject: $subjectCode")

            // Check if this matches our target room (if we have one)
            if (targetRoom != null) {
                if (isRoomMatch(extractedRoom, targetRoom!!)) {
                    handleRoomMatch(deviceName, extractedRoom, subjectCode)
                } else {
                    Timber.d("📡 Room mismatch: detected=$extractedRoom, target=$targetRoom")
                }
            } else {
                // No specific target, accept any valid room
                handleRoomMatch(deviceName, extractedRoom, subjectCode)
            }
        } else {
            if (extractedRoom == null) {
                Timber.w("📡 Could not extract room from device name: $deviceName")
            }
            if (subjectCode == null) {
                Timber.w("📡 Could not extract subject code from manufacturer data")
            }
        }
    }

    /**
     * Extract room name from device name (e.g., "LT101123" -> "LT101")
     */
    private fun extractRoomFromDeviceName(deviceName: String): String? {
        try {
            // Check if device name has at least 6 characters and ends with 3 digits
            if (deviceName.length >= 6) {
                val last3 = deviceName.takeLast(3)
                if (last3.all { it.isDigit() }) {
                    val roomName = deviceName.dropLast(3)
                    Timber.d("📡 Extracted room '$roomName' from device name '$deviceName'")
                    return roomName
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "📡 Error extracting room from device name: $deviceName")
        }
        return null
    }

    /**
     * Extract subject code from manufacturer data
     */
    private fun extractSubjectFromManufacturerData(scanResult: ScanResult): String? {
        val scanRecord = scanResult.scanRecord ?: return null

        try {
            // Try manufacturer data with ID 0xFFFF first (as per your ESP32 code)
            val manufacturerData = scanRecord.getManufacturerSpecificData(0xFFFF)
            manufacturerData?.let { data ->
                if (data.size > 2) { // Should have at least manufacturer ID (2 bytes) + message
                    // Skip the first 2 bytes (manufacturer ID) and get the message
                    val messageBytes = data.sliceArray(2 until data.size)
                    val subjectCode = String(messageBytes, Charsets.UTF_8).trim()

                    if (subjectCode.isNotBlank()) {
                        Timber.d("📡 Extracted subject code: '$subjectCode' from manufacturer data")
                        return subjectCode
                    }
                }
            }

            // Fallback: Try all available manufacturer data
            val allManufacturerData = scanRecord.manufacturerSpecificData
            if (allManufacturerData != null) {
                for (i in 0 until allManufacturerData.size()) {
                    val manufacturerId = allManufacturerData.keyAt(i)
                    val data = allManufacturerData.valueAt(i)

                    Timber.d("📡 Checking manufacturer data for ID: 0x${manufacturerId.toString(16)}")

                    data?.let { bytes ->
                        if (bytes.size > 2) {
                            // For manufacturer ID 0xFFFF, skip first 2 bytes
                            val messageBytes = if (manufacturerId == 0xFFFF) {
                                bytes.sliceArray(2 until bytes.size)
                            } else {
                                bytes
                            }

                            val subjectCode = String(messageBytes, Charsets.UTF_8).trim()
                            if (subjectCode.isNotBlank() && isValidSubjectCode(subjectCode)) {
                                Timber.d("📡 Found valid subject code: '$subjectCode' from manufacturer ID: 0x${manufacturerId.toString(16)}")
                                return subjectCode
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "📡 Error extracting subject code from manufacturer data")
        }

        return null
    }

    /**
     * Check if the extracted string looks like a valid subject code
     */
    private fun isValidSubjectCode(code: String): Boolean {
        // Basic validation for subject codes (adjust as needed)
        return code.length >= 3 &&
                code.length <= 10 &&
                code.matches(Regex("^[A-Z0-9]+$"))
    }

    private fun isRoomMatch(detectedRoom: String, targetRoom: String): Boolean {
        return targetRoom.equals(detectedRoom, ignoreCase = true)
    }

    private fun handleRoomMatch(deviceName: String, roomName: String, subjectCode: String) {
        // Stop scanning when correct device is found
        central?.stopScan()
        isScanning = false

        // Update state with both room and subject information
        _detectedDeviceRoom.value = deviceName // Full device name (e.g., "LT101123")
        _detectedSubjectCode.value = subjectCode // Subject code from manufacturer data
        _deviceFound.value = true
        _bleState.value = BleState.DEVICE_FOUND

        Timber.i("📡 ✅ Room device detected: $deviceName (Room: $roomName, Subject: $subjectCode)")
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
        _detectedSubjectCode.value = null
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

    /**
     * Get detected subject code
     */
    fun getDetectedSubjectCode(): String? = _detectedSubjectCode.value

    /**
     * Get room name from detected device (without random digits)
     */
    fun getDetectedRoomName(): String? {
        val deviceName = _detectedDeviceRoom.value ?: return null
        return extractRoomFromDeviceName(deviceName)
    }

    /**
     * Check if detected room matches target room
     */
    fun isDetectedRoomMatching(targetRoom: String): Boolean {
        val detectedRoom = getDetectedRoomName()
        return detectedRoom?.equals(targetRoom, ignoreCase = true) == true
    }

    fun isBluetoothAvailable(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as AndroidBluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Get Bluetooth adapter state
     */
    fun getBluetoothState(): BluetoothAdapterState {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as AndroidBluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        return when {
            bluetoothAdapter == null -> BluetoothAdapterState.NOT_SUPPORTED
            !bluetoothAdapter.isEnabled -> BluetoothAdapterState.DISABLED
            else -> BluetoothAdapterState.ENABLED
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


enum class BluetoothAdapterState {
    NOT_SUPPORTED,
    DISABLED,
    ENABLED
}