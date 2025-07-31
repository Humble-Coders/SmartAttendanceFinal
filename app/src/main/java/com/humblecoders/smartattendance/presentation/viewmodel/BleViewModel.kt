package com.humblecoders.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblecoders.smartattendance.data.repository.BleRepository
import com.humblecoders.smartattendance.data.repository.BleState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class BleViewModel(
    private val bleRepository: BleRepository
) : ViewModel() {

    val bleState: StateFlow<BleState> = bleRepository.bleState
    val deviceFound: StateFlow<Boolean> = bleRepository.deviceFound
    val detectedDeviceRoom: StateFlow<String?> = bleRepository.detectedDeviceRoom
    val detectedSubjectCode: StateFlow<String?> = bleRepository.detectedSubjectCode

    init {
        initializeBle()
    }

    internal fun initializeBle() {
        viewModelScope.launch {
            try {
                bleRepository.initializeBle()
                Timber.d("📡 BLE ViewModel initialized")
            } catch (e: Exception) {
                Timber.e(e, "📡 Failed to initialize BLE in ViewModel")
            }
        }
    }

    /**
     * Debug method to log current detection state
     */
    fun logCurrentDetectionState() {
        val deviceRoom = detectedDeviceRoom.value
        val subjectCode = detectedSubjectCode.value
        val roomName = getDetectedRoomName()

        Timber.d("📡 BLE Detection State:")
        Timber.d("   Device Room: '$deviceRoom'")
        Timber.d("   Subject Code: '$subjectCode'")
        Timber.d("   Room Name: '$roomName'")
        Timber.d("   Device Found: ${deviceFound.value}")
        Timber.d("   BLE State: ${bleState.value}")
    }

    /**
     * Start scanning for specific room
     */
    fun startScanningForRoom(roomName: String) {
        viewModelScope.launch {
            try {
                Timber.d("📡 Starting scan for room: $roomName")
                bleRepository.startScanningForRoom(roomName)
            } catch (e: Exception) {
                Timber.e(e, "📡 Failed to start scanning for room: $roomName")
            }
        }
    }

    /**
     * Start general scanning (backward compatibility)
     */
    fun startScanning() {
        viewModelScope.launch {
            try {
                Timber.d("📡 Starting general BLE scanning")
                bleRepository.startScanning()
            } catch (e: Exception) {
                Timber.e(e, "📡 Failed to start scanning")
            }
        }
    }

    /**
     * Stop all scanning
     */
    fun stopScanning() {
        viewModelScope.launch {
            try {
                bleRepository.stopScanning()
                Timber.d("📡 BLE scanning stopped")
            } catch (e: Exception) {
                Timber.e(e, "📡 Failed to stop scanning")
            }
        }
    }

    /**
     * Reset device detection state
     */
    fun resetDeviceFound() {
        viewModelScope.launch {
            try {
                bleRepository.resetDeviceFound()
                Timber.d("📡 Device detection reset")
            } catch (e: Exception) {
                Timber.e(e, "📡 Failed to reset device detection")
            }
        }
    }

    /**
     * Reset and continue scanning for next device
     */
    fun resetAndContinueScanning() {
        viewModelScope.launch {
            try {
                bleRepository.resetAndContinueScanning()
                Timber.d("📡 Reset and continued scanning")
            } catch (e: Exception) {
                Timber.e(e, "📡 Failed to reset and continue scanning")
            }
        }
    }

    /**
     * Restart scanning (stop and start again)
     */
    fun restartScanning() {
        viewModelScope.launch {
            try {
                bleRepository.stopScanning()
                kotlinx.coroutines.delay(500) // Small delay
                bleRepository.startScanning()
                Timber.d("📡 BLE scanning restarted")
            } catch (e: Exception) {
                Timber.e(e, "📡 Failed to restart scanning")
            }
        }
    }

    /**
     * Get current detected room (full device name with digits)
     */
    fun getDetectedDeviceRoom(): String? {
        return detectedDeviceRoom.value
    }

    /**
     * Get detected subject code
     */
    fun getDetectedSubjectCode(): String? {
        return bleRepository.getDetectedSubjectCode()
    }

    /**
     * Get room name from detected device (without digits)
     */
    fun getDetectedRoomName(): String? {
        return bleRepository.getDetectedRoomName()
    }

    /**
     * Check if detected room matches target room
     */
    fun isDetectedRoomMatching(targetRoom: String): Boolean {
        return bleRepository.isDetectedRoomMatching(targetRoom)
    }

    /**
     * Get current scanning status
     */
    fun isCurrentlyScanning(): Boolean {
        return bleState.value == BleState.SCANNING
    }

    /**
     * Check if BLE is ready (permissions and state)
     */
    fun isBluetoothReady(): Boolean {
        return bleState.value != BleState.BLUETOOTH_OFF &&
                bleState.value != BleState.NO_PERMISSION
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                bleRepository.stopScanning()
                Timber.d("📡 BLE scanning stopped due to ViewModel clearing")
            } catch (e: Exception) {
                Timber.e(e, "📡 Failed to stop scanning in onCleared")
            }
        }
    }
}