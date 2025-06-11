package com.humblecoders.smartattendance.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.humblecoders.smartattendance.data.repository.BleState
import com.humblecoders.smartattendance.presentation.viewmodel.BleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    bleViewModel: BleViewModel,
    onProfileClick: () -> Unit
) {
    val bleState by bleViewModel.bleState.collectAsState()
    val esp32DeviceFound by bleViewModel.esp32DeviceFound.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Attendance") },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // BLE Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (bleState) {
                        BleState.SCANNING -> MaterialTheme.colorScheme.primaryContainer
                        BleState.DEVICE_FOUND -> MaterialTheme.colorScheme.tertiaryContainer
                        BleState.BLUETOOTH_OFF -> MaterialTheme.colorScheme.errorContainer
                        BleState.NO_PERMISSION -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "BLE Status",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when (bleState) {
                            BleState.IDLE -> "Idle"
                            BleState.SCANNING -> "Scanning..."
                            BleState.DEVICE_FOUND -> "Signal Found!"
                            BleState.BLUETOOTH_OFF -> "Bluetooth is OFF"
                            BleState.NO_PERMISSION -> "Permission Required"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )

                    if (bleState == BleState.SCANNING) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Status Message
            Text(
                text = when {
                    esp32DeviceFound -> "Attendance device detected!"
                    bleState == BleState.SCANNING -> "Looking for attendance device..."
                    bleState == BleState.BLUETOOTH_OFF -> "Please turn on Bluetooth"
                    bleState == BleState.NO_PERMISSION -> "Please grant Bluetooth permissions"
                    else -> "Ready to scan"
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}