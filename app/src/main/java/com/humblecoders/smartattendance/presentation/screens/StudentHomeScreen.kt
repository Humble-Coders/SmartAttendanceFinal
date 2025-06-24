package com.humblecoders.smartattendance.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.humblecoders.smartattendance.utils.BluetoothManager
import com.humblecoders.smartattendance.presentation.components.BluetoothEnableDialog
import com.humblecoders.smartattendance.presentation.components.BluetoothPermissionInstructionsDialog
import com.humblecoders.smartattendance.utils.AppLifecycleObserver
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    bleViewModel: BleViewModel,
    profileViewModel: ProfileViewModel,
    attendanceViewModel: AttendanceViewModel,
    onAttendanceClick: () -> Unit,
    onLogout: () -> Unit,
    bluetoothManager: BluetoothManager, // ADD this parameter

) {
    // Collect state from ViewModels
    val bleState by bleViewModel.bleState.collectAsState()
    val deviceFound by bleViewModel.deviceFound.collectAsState()
    val detectedDeviceRoom by bleViewModel.detectedDeviceRoom.collectAsState()
    val detectedSubjectCode by bleViewModel.detectedSubjectCode.collectAsState()
    val profileData by profileViewModel.profileData.collectAsState()
    val attendanceHistory by attendanceViewModel.attendanceHistory.collectAsState()

    // NEW: Bluetooth permission and state management
    var showBluetoothDialog by remember { mutableStateOf(false) }
    var showPermissionInstructions by remember { mutableStateOf(false) }
    var bluetoothPermissionsGranted by remember { mutableStateOf(false) }

    var overlaySequenceInProgress by remember { mutableStateOf(false) } // ADD this new variable
    var lastDetectedDevice by remember { mutableStateOf<String?>(null) } // A
    val autoScanEnabled by attendanceViewModel.autoScanEnabled.collectAsState()


    // Local UI state
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isSessionActive by remember { mutableStateOf(false) }
    var currentSubject by remember { mutableStateOf("") }
    var currentRoom by remember { mutableStateOf("") }
    var currentType by remember { mutableStateOf("") }
    var isCheckingSession by remember { mutableStateOf(false) }
    var hasCheckedInitialSession by remember { mutableStateOf(false) } // Track initial check
    var isAttendanceMarked by remember { mutableStateOf(false) } // NEW: Track if attendance is marked for current session

    // Overlay state management
    var overlayState by remember { mutableStateOf(OverlayState.NONE) }

    // Collect ViewModel state for session checking
    val isCheckingSessionVM by attendanceViewModel.isCheckingSession.collectAsState()

    val isAttendanceCompletedToday by attendanceViewModel.isAttendanceCompletedToday.collectAsState()



    // Add this LaunchedEffect in StudentHomeScreen.kt, after the other LaunchedEffects:
    LaunchedEffect(profileData.rollNumber) {
        if (profileData.rollNumber.isNotBlank()) {
            Timber.d("📚 Loading attendance history for home screen")
            attendanceViewModel.loadAttendanceHistory()
        }
    }

    fun requestBluetoothPermissions(
        bluetoothManager: BluetoothManager,
        onResult: (Boolean) -> Unit
    ) {
        bluetoothManager.requestPermissions { granted ->
            onResult(granted)
        }
    }


    LaunchedEffect(Unit) {
        // Check initial Bluetooth state
        when {
            !bluetoothManager.isBluetoothSupported() -> {
                Timber.w("📡 Bluetooth not supported on this device")
            }
            !bluetoothManager.isBluetoothEnabled() -> {
                Timber.d("📡 Bluetooth disabled, will show enable dialog when needed")
                showBluetoothDialog = true
            }
            bluetoothManager.hasRequiredPermissions() -> {
                Timber.d("📡 Bluetooth permissions already granted")
                bluetoothPermissionsGranted = true
            }
            bluetoothManager.getPermissionDenialCount() >= 2 -> {
                Timber.d("📡 Permissions denied 2+ times, need settings redirect")
                showPermissionInstructions = true
            }
            else -> {
                Timber.d("📡 Need to request Bluetooth permissions")
                requestBluetoothPermissions(bluetoothManager) { granted ->
                    bluetoothPermissionsGranted = granted
                    if (!granted && bluetoothManager.getPermissionDenialCount() >= 2) {
                        showPermissionInstructions = true
                    }
                }
            }
        }
    }

    // Override local checking state with ViewModel state
    LaunchedEffect(isCheckingSessionVM) {
        isCheckingSession = isCheckingSessionVM
    }

    // NEW: Enhanced Bluetooth permission handling


    // Handle Bluetooth permissions (but don't auto-start scanning)
    BluetoothPermissionHandler(
        onPermissionsGranted = {
            Timber.d("📡 Bluetooth permissions granted, but waiting for active session to start scanning")
            // Don't auto-start scanning here anymore
        }
    )

    // UPDATED: Check active session when profile data is available (only once initially)
    LaunchedEffect(profileData.className) {
        if (profileData.className.isNotBlank() && !hasCheckedInitialSession) {
            Timber.d("🔍 Performing initial session check for class: ${profileData.className}")
            hasCheckedInitialSession = true

            attendanceViewModel.checkActiveSession { result ->
                isSessionActive = result.isActive
                if (result.session != null) {
                    currentSubject = result.session!!.subject
                    currentRoom = result.session!!.room
                    currentType = result.session!!.type

                    // NEW: Reset attendance marked flag when session changes
                    isAttendanceMarked = false

                    Timber.d("📋 Initial session check result: isActive=$isSessionActive")
                    if (isSessionActive) {
                        Timber.d("✅ Active session found: $currentSubject in $currentRoom")
                    } else {
                        Timber.d("⚪ No active session found")
                    }
                } else {
                    currentSubject = ""
                    currentRoom = ""
                    currentType = ""
                    isAttendanceMarked = false
                    Timber.d("⚪ No session data available")
                }
            }
        }
    }

    // UPDATED: Only start BLE scanning when session becomes active
    // REPLACE the existing LaunchedEffect(isSessionActive, currentRoom, isAttendanceMarked) with:
    // UPDATED: Start BLE scanning with Bluetooth checks

// UPDATED: Start BLE scanning with better state management
    LaunchedEffect(isSessionActive, currentRoom, isAttendanceCompletedToday, autoScanEnabled, bluetoothPermissionsGranted, hasCheckedInitialSession) {
        Timber.d("🔍 BLE scanning LaunchedEffect triggered")
        Timber.d("📋 Conditions: isSessionActive=$isSessionActive, room='$currentRoom', attendanceComplete=$isAttendanceCompletedToday")
        Timber.d("📋 autoScan=$autoScanEnabled, btPermissions=$bluetoothPermissionsGranted, sessionChecked=$hasCheckedInitialSession")

        if (isSessionActive &&
            currentRoom.isNotBlank() &&
            !isAttendanceCompletedToday &&
            autoScanEnabled &&
            bluetoothPermissionsGranted &&
            bluetoothManager.isBluetoothEnabled() &&
            hasCheckedInitialSession // FIXED: Only start after session check is complete
        ) {
            Timber.d("🚀 All conditions met - starting BLE scanning for room: $currentRoom")

            // FIXED: Clear any previous detection states
            overlaySequenceInProgress = false
            lastDetectedDevice = null

            // Show room detection starting overlay
            triggerRoomDetectionSequence(
                onStateChange = { overlayState = it },
                roomName = currentRoom
            )

            // FIXED: Add small delay before starting scan to ensure clean state
            kotlinx.coroutines.delay(300)

            // Start BLE scanning for specific room
            bleViewModel.startScanningForRoom(currentRoom)

            Timber.d("✅ BLE scanning started for room: $currentRoom")
        } else {
            // Stop BLE scanning if any condition is not met
            val reason = when {
                !isSessionActive -> "No active session"
                currentRoom.isBlank() -> "No room specified"
                isAttendanceCompletedToday -> "Attendance completed today"
                !autoScanEnabled -> "Auto-scan disabled (manual restart required)"
                !bluetoothPermissionsGranted -> "Bluetooth permissions not granted"
                !bluetoothManager.isBluetoothEnabled() -> "Bluetooth is disabled"
                !hasCheckedInitialSession -> "Session check not completed yet"
                else -> "Unknown reason"
            }
            Timber.d("⏹️ Stopping BLE scanning: $reason")
            bleViewModel.stopScanning()
        }
    }


    // Handle device detection and validation
    // ALSO UPDATE the device detection LaunchedEffect in StudentHomeScreen.kt:

// FIXED: Handle device detection with better duplicate prevention
    LaunchedEffect(deviceFound, detectedDeviceRoom, detectedSubjectCode, overlaySequenceInProgress) {
        if (deviceFound &&
            isSessionActive &&
            detectedDeviceRoom != null &&
            !overlaySequenceInProgress && // Prevent if sequence is already in progress
            lastDetectedDevice != detectedDeviceRoom // Prevent duplicate processing of same device
        ) {
            val detectedRoomName = bleViewModel.getDetectedRoomName()
            val detectedSubject = detectedSubjectCode

            Timber.d("📡 Device detected: room=$detectedRoomName, subject=$detectedSubject")
            Timber.d("📚 Current session: room=$currentRoom, subject=$currentSubject")
            Timber.d("🔄 Overlay in progress: $overlaySequenceInProgress, lastDevice: $lastDetectedDevice")

            // Check if detected room matches session room
            if (bleViewModel.isDetectedRoomMatching(currentRoom)) {
                // Mark that we've processed this device and sequence is starting
                lastDetectedDevice = detectedDeviceRoom
                overlaySequenceInProgress = true

                Timber.d("✅ Room match confirmed, starting overlay sequence")

                // Show classroom detected overlay
                triggerClassroomDetectedSequence(
                    onStateChange = { overlayState = it },
                    roomName = currentRoom
                )
            } else {
                Timber.w("❌ Room mismatch. Detected: $detectedRoomName, Session: $currentRoom")
                // FIXED: Don't reset overlaySequenceInProgress here, just reset device detection
                bleViewModel.resetDeviceFound()
            }
        }
    }


    // NEW: Monitor app lifecycle to re-check permissions when returning from settings
    AppLifecycleObserver(
        onAppResumed = {
            // Re-check Bluetooth state when app resumes
            val bluetoothState = bluetoothManager.recheckBluetoothState()

            Timber.d("📡 App resumed - Bluetooth state: $bluetoothState")

            // Update permission state
            bluetoothPermissionsGranted = bluetoothState.hasPermissions

            // Close dialogs if permissions/Bluetooth are now ready
            if (bluetoothState.isFullyReady) {
                showBluetoothDialog = false
                showPermissionInstructions = false
                Timber.d("✅ Bluetooth fully ready after app resume")
            } else if (!bluetoothState.isEnabled) {
                // Bluetooth was turned off while away
                if (!showBluetoothDialog) {
                    showBluetoothDialog = true
                    Timber.d("📡 Bluetooth disabled while away, showing enable dialog")
                }
            }
        },
        onAppPaused = {
            // Optional: You can add logic here if needed when app goes to background
            Timber.d("⏸️ App paused from home screen")
        }
    )


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

    // NEW: Show Bluetooth enable dialog
    if (showBluetoothDialog) {
        BluetoothEnableDialog(
            bluetoothManager = bluetoothManager,
            onBluetoothEnabled = {
                showBluetoothDialog = false
                // Check permissions after Bluetooth is enabled
                if (bluetoothManager.hasRequiredPermissions()) {
                    bluetoothPermissionsGranted = true
                } else {
                    requestBluetoothPermissions(bluetoothManager) { granted ->
                        bluetoothPermissionsGranted = granted
                        if (!granted && bluetoothManager.getPermissionDenialCount() >= 2) {
                            showPermissionInstructions = true
                        }
                    }
                }
            },
            onCancel = {
                showBluetoothDialog = false
            }
        )
    }

// NEW: Show permission instructions dialog
    if (showPermissionInstructions) {
        BluetoothPermissionInstructionsDialog(
            bluetoothManager = bluetoothManager,
            onDismiss = {
                showPermissionInstructions = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Smart Attend",
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
            // Replace the existing Header Section Card with this:
            // 1. UPDATED Header Section Card (with symmetrical layout):
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side - Text content
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Welcome text
                        Text(
                            text = "Welcome back,",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF8E8E93),
                            letterSpacing = 0.5.sp
                        )

                        // Student name
                        Text(
                            text = profileData.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1D1F),
                            lineHeight = 26.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Student details in symmetrical columns
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Roll Number Column
                            Column {
                                Text(
                                    text = "Roll No",
                                    fontSize = 12.sp,
                                    color = Color(0xFF007AFF),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = profileData.rollNumber,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1D1D1F),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Class Column
                            Column {
                                Text(
                                    text = "Class",
                                    fontSize = 12.sp,
                                    color = Color(0xFF34C759),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = profileData.className,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1D1D1F),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Right side - Profile Avatar (same as before)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF007AFF).copy(alpha = 0.1f),
                                        Color(0xFF007AFF).copy(alpha = 0.05f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(66.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF007AFF).copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF007AFF).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Student Avatar",
                                    modifier = Modifier.size(28.dp),
                                    tint = Color(0xFF007AFF)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


// 2. UPDATED Session Status Card (remove settings icon, add icons, show type):
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
                    }

                    if (isSessionActive) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Subject
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Subject",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF8E8E93)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Subject: $currentSubject",
                                    fontSize = 14.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }

                            // Room
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Room,
                                    contentDescription = "Room",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF8E8E93)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Room: $currentRoom",
                                    fontSize = 14.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }

                            // Type
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = "Type",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF8E8E93)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Type: $currentType",
                                    fontSize = 14.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (profileData.className.isNotBlank()) {
                        Timber.d("🔍 Check session triggered")

                        if (!bluetoothManager.isBluetoothEnabled()) {
                            showBluetoothDialog = true
                            return@Button
                        }

                        if (!bluetoothPermissionsGranted) {
                            if (bluetoothManager.getPermissionDenialCount() >= 2) {
                                showPermissionInstructions = true
                            } else {
                                requestBluetoothPermissions(bluetoothManager) { granted ->
                                    bluetoothPermissionsGranted = granted
                                    if (!granted && bluetoothManager.getPermissionDenialCount() >= 2) {
                                        showPermissionInstructions = true
                                    }
                                }
                            }
                            return@Button
                        }

                        // Re-enable auto-scanning when user manually checks session
                        attendanceViewModel.enableAutoScan()

                        // FIXED: Clear ALL overlay and BLE states properly
                        overlayState = OverlayState.NONE
                        overlaySequenceInProgress = false
                        lastDetectedDevice = null

                        // FIXED: Stop any current BLE scanning and reset detection state completely
                        bleViewModel.stopScanning()
                        bleViewModel.resetDeviceFound()

                        // FIXED: Add small delay to ensure BLE state is fully reset
                        kotlinx.coroutines.GlobalScope.launch {
                            kotlinx.coroutines.delay(200) // Small delay for BLE cleanup

                            // Reset attendance status and session check state
                            hasCheckedInitialSession = false
                            attendanceViewModel.resetAttendanceStatus()

                            // Now check session
                            attendanceViewModel.checkActiveSession { result ->
                                isSessionActive = result.isActive
                                if (result.session != null) {
                                    currentSubject = result.session!!.subject
                                    currentRoom = result.session!!.room
                                    currentType = result.session!!.type
                                    isAttendanceMarked = false

                                    if (result.isActive) {
                                        Timber.d("✅ Session check found active session: $currentSubject in $currentRoom")
                                        // BLE scanning will start automatically due to the LaunchedEffect
                                    } else {
                                        Timber.d("⚪ Session check found no active session")
                                    }
                                } else {
                                    currentSubject = ""
                                    currentRoom = ""
                                    currentType = ""
                                    isAttendanceMarked = false
                                }
                                hasCheckedInitialSession = true
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                        imageVector = Icons.Default.Search,
                        contentDescription = "Check Session",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Check Session", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // UPDATED: Current Activity Card - Shows BLE status only when session is active
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

                    // Session Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📋",
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isCheckingSession -> "Session Check: In Progress..."
                                isSessionActive -> "Session Status: Active"
                                else -> "Session Status: No Active Session"
                            },
                            fontSize = 14.sp,
                            color = if (isSessionActive) Color(0xFF34C759) else Color(0xFF8E8E93),
                            fontWeight = if (isSessionActive) FontWeight.Medium else FontWeight.Normal
                        )

                        if (isCheckingSession) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = Color(0xFF007AFF),
                                strokeWidth = 1.5.dp
                            )
                        }
                    }

                    // Only show BLE status if session is active
                    if (isSessionActive) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Show attendance status if marked
                        if (isAttendanceCompletedToday) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✅",
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Attendance Status: Already Marked",
                                    fontSize = 14.sp,
                                    color = Color(0xFF34C759),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📡",
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        isAttendanceCompletedToday -> "BLE Scanning: Stopped (Attendance Complete)"
                                        bleState == BleState.IDLE && isSessionActive -> "BLE Scanning: Stopped (Restart needed)"
                                        else -> "BLE Scanning: Stopped (Attendance Complete)"
                                    },
                                    fontSize = 14.sp,
                                    color = Color(0xFF8E8E93),
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        } else {
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
                                        BleState.DEVICE_FOUND -> "Device Found: ${bleViewModel.getDetectedRoomName() ?: "Unknown"}"
                                        BleState.IDLE -> "Bluetooth Detection: Ready"
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
                                        deviceFound -> "Room Detection: Found (${bleViewModel.getDetectedRoomName()})"
                                        bleState == BleState.SCANNING -> "Room Detection: Searching..."
                                        else -> "Room Detection: Waiting for proximity"
                                    },
                                    fontSize = 14.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }

                            // Subject Code Detection Status (only if detected)
                            if (detectedSubjectCode != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📚",
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Subject Detected: $detectedSubjectCode",
                                        fontSize = 14.sp,
                                        color = if (detectedSubjectCode.equals(currentSubject, ignoreCase = true)) {
                                            Color(0xFF34C759) // Green if matches
                                        } else {
                                            Color(0xFFFF9500) // Orange if different
                                        },
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        // Show message when no session is active
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💤",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Waiting for active session to start BLE scanning",
                                fontSize = 14.sp,
                                color = Color(0xFF8E8E93),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))



// Replace the existing Recent Attendance Card content with this:
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
                            // Header Row (keep this same)
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

                                IconButton(
                                    onClick = {
                                        if (profileData.rollNumber.isNotBlank()) {
                                            Timber.d("🔄 Manual refresh of attendance history")
                                            attendanceViewModel.loadAttendanceHistory()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        modifier = Modifier.size(20.dp),
                                        tint = Color(0xFF8E8E93)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // REPLACE the existing if-else block with this LazyColumn implementation:
                            if (attendanceHistory.isEmpty()) {
                                Text(
                                    text = "No recent attendance records",
                                    fontSize = 14.sp,
                                    color = Color(0xFF8E8E93),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                // LazyColumn for scrollable attendance history
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp), // Set fixed height for scrollable area
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(attendanceHistory) { record ->
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
            // FIXED: Reset sequence state when overlay is dismissed manually
            overlaySequenceInProgress = false
            lastDetectedDevice = null
            Timber.d("🎭 Overlay dismissed, states reset")
        },
        onSequenceComplete = {
            // Classroom detected overlay completed
            Timber.d("🎭 Overlay sequence complete, navigating to face authentication")

            // Reset all overlay-related states
            overlayState = OverlayState.NONE
            overlaySequenceInProgress = false
            lastDetectedDevice = null

            // Navigate immediately without delay
            onAttendanceClick()
            bleViewModel.resetAndContinueScanning()
        }
    )


    // NEW: Monitor attendance history to detect when new attendance is marked
    LaunchedEffect(attendanceHistory) {
        if (isSessionActive && currentSubject.isNotBlank() && attendanceHistory.isNotEmpty()) {
            // Check if there's a recent attendance record for current session
            val today = java.time.LocalDate.now().toString()
            val todayAttendance = attendanceHistory.filter { record ->
                record.date == today &&
                        record.subject == currentSubject &&
                        record.type == currentType &&
                        record.present
            }

            if (todayAttendance.isNotEmpty() && !isAttendanceMarked) {
                Timber.d("🎉 Attendance detected in history - marking as completed")
                isAttendanceMarked = true
                // Stop BLE scanning since attendance is marked
                bleViewModel.stopScanning()
            }
        }
    }

    // Helper function for requesting Bluetooth permissions

}