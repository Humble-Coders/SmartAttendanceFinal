package com.humblecoders.smartattendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblecoders.smartattendance.data.repository.BleRepository
import com.humblecoders.smartattendance.data.repository.BleState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BleViewModel(
    private val bleRepository: BleRepository
) : ViewModel() {

    val bleState: StateFlow<BleState> = bleRepository.bleState
    val esp32DeviceFound: StateFlow<Boolean> = bleRepository.esp32DeviceFound

    init {
        initializeBle()
    }

    private fun initializeBle() {
        viewModelScope.launch {
            bleRepository.initializeBle()
            // Automatically start scanning when app launches
            startScanning()
        }
    }

    fun startScanning() {
        bleRepository.startScanning()
    }

    fun stopScanning() {
        bleRepository.stopScanning()
    }

    fun resetDeviceFound() {
        // This will be called after attendance marking process
        bleRepository.stopScanning()
        bleRepository.startScanning()
    }
}