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
    val esp32DeviceFound: StateFlow<Boolean> = bleRepository.esp32DeviceFound
    val subjectCode: StateFlow<String?> = bleRepository.subjectCode

    init {
        initializeBle()
    }

    private fun initializeBle() {
        viewModelScope.launch {
            try {
                bleRepository.initializeBle()
                // Start scanning automatically when app launches
                startScanning()
                Timber.d("BLE ViewModel initialized and scanning started")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize BLE in ViewModel")
            }
        }
    }

    fun startScanning() {
        viewModelScope.launch {
            try {
                bleRepository.startScanning()
                Timber.d("Scanning started from ViewModel")
            } catch (e: Exception) {
                Timber.e(e, "Failed to start scanning from ViewModel")
            }
        }
    }

    fun stopScanning() {
        viewModelScope.launch {
            try {
                bleRepository.stopScanning()
                Timber.d("Scanning stopped from ViewModel")
            } catch (e: Exception) {
                Timber.e(e, "Failed to stop scanning from ViewModel")
            }
        }
    }

    fun resetDeviceFound() {
        viewModelScope.launch {
            try {
                // Reset the device found state and restart scanning
                bleRepository.resetDeviceFound()
                // Wait a bit before restarting scan to avoid rapid scanning
                kotlinx.coroutines.delay(1000)
                bleRepository.startScanning()
                Timber.d("Device found state reset and scanning restarted")
            } catch (e: Exception) {
                Timber.e(e, "Failed to reset device found state")
            }
        }
    }

    /**
     * Get the current subject code if available
     */
    fun getCurrentSubjectCode(): String? {
        return subjectCode.value
    }

    /**
     * Check if Bluetooth is enabled and permissions are granted
     */
    fun isBluetoothReady(): Boolean {
        return bleState.value != BleState.BLUETOOTH_OFF &&
                bleState.value != BleState.NO_PERMISSION
    }

    /**
     * Force restart of scanning (useful when permissions are granted)
     */
    fun restartScanning() {
        viewModelScope.launch {
            try {
                bleRepository.stopScanning()
                kotlinx.coroutines.delay(500) // Small delay to ensure stop is processed
                bleRepository.startScanning()
                Timber.d("Scanning restarted from ViewModel")
            } catch (e: Exception) {
                Timber.e(e, "Failed to restart scanning from ViewModel")
            }
        }
    }

    /**
     * Check if device was found and get subject code
     */
    fun getDetectedDeviceInfo(): Pair<Boolean, String?> {
        return Pair(esp32DeviceFound.value, subjectCode.value)
    }

    override fun onCleared() {
        super.onCleared()
        // Stop scanning when ViewModel is cleared
        viewModelScope.launch {
            try {
                bleRepository.stopScanning()
                Timber.d("BLE scanning stopped due to ViewModel clearing")
            } catch (e: Exception) {
                Timber.e(e, "Failed to stop scanning in onCleared")
            }
        }
    }
}