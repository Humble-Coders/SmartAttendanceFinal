package com.humblecoders.smartattendance.presentation.components


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
fun RoomDetectionStartingOverlay(
    roomName: String,
    onDismiss: () -> Unit,
    autoHideAfterMs: Long = 1500L
) {
    // Auto-hide after specified time
    LaunchedEffect(Unit) {
        delay(autoHideAfterMs)
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // WiFi/Signal Icon
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Room Detection Signal",
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF007AFF)
                    )

                    // Main Title
                    Text(
                        text = "Room Presence Detection",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1D1F),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Starting",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF007AFF),
                        textAlign = TextAlign.Center
                    )

                    // Room Information
                    if (roomName.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF007AFF).copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Looking for room:",
                                    fontSize = 14.sp,
                                    color = Color(0xFF8E8E93),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = roomName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF007AFF),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Loading Indicator
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color(0xFF007AFF),
                        strokeWidth = 3.dp
                    )

                    // Status Text
                    Text(
                        text = "Scanning for nearby devices...",
                        fontSize = 14.sp,
                        color = Color(0xFF8E8E93),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}