package com.humblecoders.smartattendance.data.repository

import android.Manifest
import android.bluetooth.BluetoothAdapter
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

    private val _esp32DeviceFound = MutableStateFlow(false)
    val esp32DeviceFound: StateFlow<Boolean> = _esp32DeviceFound.asStateFlow()

    private val _subjectCode = MutableStateFlow<String?>(null)
    val subjectCode: StateFlow<String?> = _subjectCode.asStateFlow()

    private var central: BluetoothCentralManager? = null

    fun initializeBle() {
        try {
            // Create BluetoothCentralManager without callback initially
            central = BluetoothCentralManager(context)
            Timber.d("BLE Central Manager initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize BLE Central Manager")
        }
    }

    fun startScanning() {
        if (!hasRequiredPermissions()) {
            _bleState.value = BleState.NO_PERMISSION
            return
        }

        central?.let { centralManager ->
            try {
                _esp32DeviceFound.value = false
                _subjectCode.value = null
                _bleState.value = BleState.SCANNING

                // Scan for peripherals with the specific name "Humble Coders"
                // Use Set for blessed-kotlin
                val deviceNames = arrayOf("Humble Coders")
                centralManager.scanForPeripheralsWithNames(
                    peripheralNames = deviceNames,
                    resultCallback = { peripheral, scanResult ->
                        handleDeviceDiscovered(peripheral, scanResult)
                    },
                    scanError = { scanFailure ->
                        Timber.e("Scan failed with reason: $scanFailure")
                        _bleState.value = BleState.IDLE
                    }
                )

                Timber.d("Started scanning for devices with name: Humble Coders")
            } catch (e: Exception) {
                Timber.e(e, "Failed to start scanning")
                _bleState.value = BleState.IDLE
            }
        } ?: run {
            Timber.e("Central manager not initialized")
            initializeBle()
        }
    }

    fun stopScanning() {
        central?.let { centralManager ->
            try {
                centralManager.stopScan()
                _bleState.value = BleState.IDLE
                _esp32DeviceFound.value = false
                _subjectCode.value = null
                Timber.d("Stopped scanning")
            } catch (e: Exception) {
                Timber.e(e, "Failed to stop scanning")
            }
        }
    }

    private fun handleDeviceDiscovered(peripheral: BluetoothPeripheral, scanResult: ScanResult) {
        val deviceName = peripheral.name
        Timber.d("Discovered device: $deviceName, RSSI: ${scanResult.rssi}")

        // Check if this is our ESP32 device with name "Humble Coders"
        if (deviceName != null && deviceName == "Humble Coders") {
            Timber.d("Found Humble Coders device!")

            // Extract manufacturer data to get the subject code
            val scanRecord = scanResult.scanRecord
            var extractedSubjectCode: String? = null

            scanRecord?.let { record ->
                try {
                    // ESP32 uses manufacturer ID 0xFFFF (65535)
                    val manufacturerData = record.getManufacturerSpecificData(0xFFFF)
                    manufacturerData?.let { data ->
                        try {
                            // The manufacturer data contains the subject code directly
                            // ESP32 code adds manufacturer ID (2 bytes) + subject code
                            if (data.size > 2) {
                                extractedSubjectCode = String(data.copyOfRange(2, data.size), Charsets.UTF_8).trim()
                                Timber.d("Extracted subject code from 0xFFFF: $extractedSubjectCode")
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to extract subject code from manufacturer data")
                        }
                    }

                    // If no manufacturer data found with 0xFFFF, try all available manufacturer data
                    if (extractedSubjectCode == null) {
                        val allManufacturerData = record.manufacturerSpecificData
                        if (allManufacturerData != null) {
                            Timber.d("Available manufacturer IDs: ${allManufacturerData.size()}")

                            // Try all available manufacturer data
                            for (i in 0 until allManufacturerData.size()) {
                                val manufacturerId = allManufacturerData.keyAt(i)
                                val data = allManufacturerData.valueAt(i)
                                Timber.d("Manufacturer ID: $manufacturerId, Data size: ${data?.size}")

                                data?.let { bytes ->
                                    if (bytes.isNotEmpty()) {
                                        try {
                                            // Try to parse as subject code
                                            val possibleSubjectCode = String(bytes, Charsets.UTF_8).trim()
                                            // Check if it looks like a subject code (alphanumeric, reasonable length)
                                            if (possibleSubjectCode.matches(Regex("[A-Za-z0-9]{3,10}")) &&
                                                possibleSubjectCode.isNotBlank()) {
                                                extractedSubjectCode = possibleSubjectCode
                                                Timber.d("Found subject code in manufacturer ID $manufacturerId: $extractedSubjectCode")
                                                return@let // Return from the current lambda instead of using break
                                            }
                                        } catch (e: Exception) {
                                            Timber.w("Failed to parse data from manufacturer ID $manufacturerId")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Also try to get device name from scan record
                    if (extractedSubjectCode == null) {
                        val deviceNameFromRecord = record.deviceName
                        Timber.d("Device name from record: $deviceNameFromRecord")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error processing scan record")
                }
            }

            // Update state
            _subjectCode.value = extractedSubjectCode ?: "UNKNOWN"
            _esp32DeviceFound.value = true
            _bleState.value = BleState.DEVICE_FOUND

            Timber.i("ESP32 attendance device detected! Subject code: ${_subjectCode.value}")
        }
    }

    fun resetDeviceFound() {
        _esp32DeviceFound.value = false
        _subjectCode.value = null
        if (_bleState.value == BleState.DEVICE_FOUND) {
            _bleState.value = BleState.IDLE
        }
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