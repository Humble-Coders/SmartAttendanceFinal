package com.humblecoders.smartattendance.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
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
fun ClassroomDetectedOverlay(
    roomName: String,
    onDismiss: () -> Unit,
    autoHideAfterMs: Long = 2000L
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
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Location Pin Icon
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location Detected",
                        modifier = Modifier.size(72.dp),
                        tint = Color(0xFF34C759)
                    )

                    // Main Title
                    Text(
                        text = "Classroom Presence",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1D1F),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Detected!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34C759),
                        textAlign = TextAlign.Center
                    )

                    // Room Information Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF34C759).copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Room:",
                                fontSize = 14.sp,
                                color = Color(0xFF8E8E93),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = roomName,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34C759),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Status Message
                    Text(
                        text = "Starting face authentication...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF007AFF),
                        textAlign = TextAlign.Center
                    )

                    // Success Indicator with Animation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Pulsing success indicator
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF34C759),
                            strokeWidth = 3.dp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "✓ Connected",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF34C759)
                        )
                    }
                }
            }
        }
    }
}