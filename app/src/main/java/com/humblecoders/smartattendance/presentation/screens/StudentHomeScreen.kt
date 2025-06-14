package com.humblecoders.smartattendance.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblecoders.smartattendance.data.repository.BleState
import com.humblecoders.smartattendance.presentation.components.BluetoothPermissionHandler
import com.humblecoders.smartattendance.presentation.components.AttendanceOverlayManager
import com.humblecoders.smartattendance.presentation.components.OverlayState
import com.humblecoders.smartattendance.presentation.components.triggerRoomDetectionSequence
import com.humblecoders.smartattendance.presentation.components.triggerClassroomDetectedSequence
import com.humblecoders.smartattendance.presentation.viewmodel.BleViewModel
import com.humblecoders.smartattendance.presentation.viewmodel.ProfileViewModel
import com.humblecoders.smartattendance.presentation.viewmodel.AttendanceViewModel
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    bleViewModel: BleViewModel,
    profileViewModel: ProfileViewModel,
    attendanceViewModel: AttendanceViewModel,
    onAttendanceClick: () -> Unit,
    onLogout: () -> Unit
) {
    // Collect state from ViewModels
    val bleState by bleViewModel.bleState.collectAsState()
    val deviceFound by bleViewModel.deviceFound.collectAsState()
    val detectedDeviceRoom by bleViewModel.detectedDeviceRoom.collectAsState()
    val profileData by profileViewModel.profileData.collectAsState()
    val attendanceHistory by attendanceViewModel.attendanceHistory.collectAsState()

    // Local UI state
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isSessionActive by remember { mutableStateOf(false) }
    var currentSubject by remember { mutableStateOf("") }
    var currentRoom by remember { mutableStateOf("") }
    var currentType by remember { mutableStateOf("") }
    var isCheckingSession by remember { mutableStateOf(false) }

    // Overlay state management
    var overlayState by remember { mutableStateOf(OverlayState.NONE) }

    // Collect ViewModel state for session checking
    val isCheckingSessionVM by attendanceViewModel.isCheckingSession.collectAsState()

    // Override local checking state with ViewModel state
    LaunchedEffect(isCheckingSessionVM) {
        isCheckingSession = isCheckingSessionVM
    }

    // Handle Bluetooth permissions
    BluetoothPermissionHandler(
        onPermissionsGranted = {
            bleViewModel.restartScanning()
        }
    )

    // Check active session when profile data is available
    LaunchedEffect(profileData.className) {
        if (profileData.className.isNotBlank()) {
            attendanceViewModel.checkActiveSession { result ->
                isSessionActive = result.isActive
                if (result.session != null) {
                    currentSubject = result.session!!.subject
                    currentRoom = result.session!!.room
                    currentType = result.session!!.type
                } else {
                    currentSubject = ""
                    currentRoom = ""
                    currentType = ""
                }
            }
        }
    }

    // Auto-start BLE scanning if session is active
    LaunchedEffect(isSessionActive, currentRoom) {
        if (isSessionActive && currentRoom.isNotBlank()) {
            Timber.d("Active session detected for room: $currentRoom")
            // Show room detection starting overlay
            triggerRoomDetectionSequence(
                onStateChange = { overlayState = it },
                roomName = currentRoom
            )
            // Start BLE scanning for specific room
            bleViewModel.startScanningForRoom(currentRoom)
        }
    }

    // Handle device detection and room matching
    LaunchedEffect(deviceFound, detectedDeviceRoom) {
        if (deviceFound && isSessionActive && detectedDeviceRoom != null) {
            Timber.d("Device detected: $detectedDeviceRoom")

            // Check if detected room matches session room
            if (bleViewModel.isDetectedRoomMatching(currentRoom)) {
                Timber.d("Room matches! Detected device: $detectedDeviceRoom, Session room: $currentRoom")
                // Show classroom detected overlay
                triggerClassroomDetectedSequence(
                    onStateChange = { overlayState = it },
                    roomName = currentRoom
                )
            } else {
                val detectedRoomName = bleViewModel.getDetectedRoomName()
                Timber.w("Room mismatch. Detected: $detectedRoomName, Session: $currentRoom")
                bleViewModel.resetDeviceFound()
            }
        }
    }





    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout? This will clear your profile data.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        profileViewModel.resetCompleteProfile(
                            onSuccess = { onLogout() },
                            onError = { /* Handle error */ }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF3B30)
                    )
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Attendance",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showLogoutDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF2F2F7)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F7))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // Header Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Student Avatar
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Student Avatar",
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFF007AFF)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Welcome back, ${profileData.name}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1D1D1F)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            // Roll Number Chip
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF007AFF).copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Roll: ${profileData.rollNumber}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    color = Color(0xFF007AFF),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Class Chip
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF34C759).copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Class: ${profileData.className}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    color = Color(0xFF34C759),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Session Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status Icon
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSessionActive) Color(0xFF34C759)
                                    else if (isCheckingSession) Color(0xFFFF9500)
                                    else Color(0xFFFF3B30)
                                )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = when {
                                isCheckingSession -> "Checking Session..."
                                isSessionActive -> "Active Session"
                                else -> "No Active Session"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1D1D1F)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF8E8E93)
                        )
                    }

                    if (isSessionActive) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Subject: $currentSubject",
                                    fontSize = 14.sp,
                                    color = Color(0xFF8E8E93)
                                )
                                Text(
                                    text = "Room: $currentRoom",
                                    fontSize = 14.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }

                            Text(
                                text = "Type: $currentType",
                                fontSize = 14.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Refresh Button
                Button(
                    onClick = {
                        if (profileData.className.isNotBlank()) {
                            attendanceViewModel.checkActiveSession { result ->
                                isSessionActive = result.isActive
                                if (result.session != null) {
                                    currentSubject = result.session!!.subject
                                    currentRoom = result.session!!.room
                                    currentType = result.session!!.type
                                } else {
                                    currentSubject = ""
                                    currentRoom = ""
                                    currentType = ""
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF)
                    ),
                    enabled = !isCheckingSession
                ) {
                    if (isCheckingSession) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh", fontWeight = FontWeight.Medium)
                }

                // Manual Detection Button
                OutlinedButton(
                    onClick = { onAttendanceClick() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = buttonColors(
                        contentColor = Color(0xFFFF9500)
                    )
                ) {
                    Text("Manual", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Activity Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Activity",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1D1D1F)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Time",
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF8E8E93)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bluetooth Detection Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📶",
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (bleState) {
                                BleState.SCANNING -> "Bluetooth Detection: Active"
                                BleState.DEVICE_FOUND -> "Device Found: ${detectedDeviceRoom ?: "Unknown"}"
                                BleState.IDLE -> "Bluetooth Detection: Idle"
                                else -> "Bluetooth Detection: ${bleState.name}"
                            },
                            fontSize = 14.sp,
                            color = Color(0xFF8E8E93)
                        )

                        if (bleState == BleState.SCANNING) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = Color(0xFF007AFF),
                                strokeWidth = 1.5.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Room Detection Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔍",
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                deviceFound -> "Room Detection: Found ($currentRoom)"
                                bleState == BleState.SCANNING -> "Room Detection: Searching..."
                                isSessionActive -> "Room Detection: Waiting for proximity"
                                else -> "Room Detection: Inactive"
                            },
                            fontSize = 14.sp,
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recent Attendance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Attendance",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1D1D1F)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "View All",
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF8E8E93)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (attendanceHistory.isEmpty()) {
                        Text(
                            text = "No recent attendance records",
                            fontSize = 14.sp,
                            color = Color(0xFF8E8E93),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Show recent attendance items
                        attendanceHistory.take(3).forEach { record ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✅",
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = record.subject,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1D1D1F)
                                    )
                                    Text(
                                        text = "${record.type.uppercase()} • ${record.getFormattedTime()}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF8E8E93)
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Details",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF8E8E93)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Overlay Manager - handles all overlay states
    AttendanceOverlayManager(
        overlayState = overlayState,
        roomName = currentRoom,
        onOverlayDismissed = {
            overlayState = OverlayState.NONE
        },
        onSequenceComplete = {
            // Classroom detected overlay completed, start face authentication
            Timber.d("🎭 Overlay sequence complete, navigating to face authentication")
            onAttendanceClick()
            bleViewModel.resetAndContinueScanning()
        }
    )
}