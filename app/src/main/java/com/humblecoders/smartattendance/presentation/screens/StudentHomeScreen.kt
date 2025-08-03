package com.humblecoders.smartattendance.presentation.screens

import android.util.Log
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
    val isFaceRecognitionEnabled by attendanceViewModel.isFaceRecognitionEnabled.collectAsState()

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

    var refreshTimer by remember { mutableStateOf(0) }
    var isRefreshEnabled by remember { mutableStateOf(true) }

    // Timer countdown effect
    LaunchedEffect(refreshTimer) {
        if (refreshTimer > 0) {
            kotlinx.coroutines.delay(1000)
            refreshTimer -= 1
        } else if (!isRefreshEnabled) {
            isRefreshEnabled = true
        }
    }



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

                    // Reset attendance marked flag when session changes
                    isAttendanceMarked = false

                    Timber.d("📋 Initial session check result: isActive=$isSessionActive")
                    if (isSessionActive) {
                        Timber.d("✅ Active session found: $currentSubject in $currentRoom")
                        Timber.d("📝 Session message: ${result.message}") // This will show if attendance is already marked
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

    // Replace the existing Scaffold content in StudentHomeScreen.kt with this:

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AttendX",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color(0xFF1D1D1F)
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showLogoutDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF007AFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8F9FA)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // Welcome Card - Similar to the image
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Welcome back,",
                            fontSize = 14.sp,
                            color = Color(0xFF8E8E93),
                            fontWeight = FontWeight.Normal
                        )

                        Text(
                            text = profileData.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1D1F)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(32.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Roll Number",
                                    fontSize = 12.sp,
                                    color = Color(0xFF007AFF),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = profileData.rollNumber,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1D1D1F),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Column {
                                Text(
                                    text = "Class",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF9500),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = profileData.className,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1D1D1F),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Avatar",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Session Status Card - Redesigned
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Session Status Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSessionActive) Color(0xFF34C759).copy(alpha = 0.1f)
                                        else Color(0xFF8E8E93).copy(alpha = 0.1f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Session Status",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSessionActive) Color(0xFF34C759) else Color(0xFF8E8E93)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "Session Status",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1D1D1F)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // ADD: Refresh Button with Timer
                            IconButton(
                                onClick = {
                                    if (isRefreshEnabled && profileData.className.isNotBlank()) {
                                        Timber.d("🔄 Session refresh button clicked")

                                        // Start timer
                                        isRefreshEnabled = false
                                        refreshTimer = 10

                                        // Reset session check state
                                        hasCheckedInitialSession = false

                                        // Check active session
                                        attendanceViewModel.checkActiveSession { result ->
                                            isSessionActive = result.isActive
                                            if (result.session != null) {
                                                currentSubject = result.session!!.subject
                                                currentRoom = result.session!!.room
                                                currentType = result.session!!.type

                                                if (result.isActive) {
                                                    Timber.d("✅ Refresh found active session: $currentSubject in $currentRoom")
                                                } else {
                                                    Timber.d("⚪ Refresh found no active session")
                                                }
                                            } else {
                                                currentSubject = ""
                                                currentRoom = ""
                                                currentType = ""
                                                Timber.d("⚪ No session data available")
                                            }
                                            hasCheckedInitialSession = true
                                        }
                                    }
                                },
                                enabled = isRefreshEnabled && !isCheckingSession,
                                modifier = Modifier.size(32.dp)
                            ) {
                                if (isCheckingSession) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color(0xFF007AFF),
                                        strokeWidth = 2.dp
                                    )
                                } else if (!isRefreshEnabled) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            progress = (10 - refreshTimer) / 10f,
                                            modifier = Modifier.size(20.dp),
                                            color = Color(0xFF8E8E93),
                                            strokeWidth = 2.dp,
                                            trackColor = Color(0xFF8E8E93).copy(alpha = 0.2f)
                                        )
                                        Text(
                                            text = refreshTimer.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8E8E93)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh Session",
                                        tint = Color(0xFF007AFF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Status Badge
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isAttendanceCompletedToday) Color(0xFF34C759).copy(alpha = 0.1f)
                                    else if (isSessionActive) Color(0xFF34C759).copy(alpha = 0.1f)
                                    else Color(0xFF8E8E93).copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = when {
                                        isAttendanceCompletedToday -> "Attended"
                                        isSessionActive -> "Active"
                                        else -> "Inactive"
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = when {
                                        isAttendanceCompletedToday -> Color(0xFF34C759)
                                        isSessionActive -> Color(0xFF34C759)
                                        else -> Color(0xFF8E8E93)
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = when {
                            isAttendanceCompletedToday -> "Session active - Attendance already marked"
                            isSessionActive -> "Session active - Ready for attendance"
                            isCheckingSession -> "Checking session status..."
                            else -> "No active session found"
                        },
                        fontSize = 14.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    if (isSessionActive) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Session Details
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = "Subject",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFF007AFF)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Subject",
                                        fontSize = 14.sp,
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                                Text(
                                    text = currentSubject,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1D1D1F)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Room",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFF007AFF)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Room",
                                        fontSize = 14.sp,
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                                Text(
                                    text = currentRoom,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1D1D1F)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = "Type",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFF007AFF)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Type",
                                        fontSize = 14.sp,
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                                Text(
                                    text = currentType,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1D1D1F)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = "Auth Method",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFF007AFF)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Auth Method",
                                        fontSize = 14.sp,
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                                if(isFaceRecognitionEnabled==true)
                                {
                                    Text(
                                        text = "Room Presence + Face Rec",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1D1D1F)
                                    )
                                }
                                else{
                                Text(
                                    text = "Room Presence",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1D1D1F)
                                )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Check Session Button
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

                        // Reset states
                        overlayState = OverlayState.NONE
                        overlaySequenceInProgress = false
                        lastDetectedDevice = null
                        bleViewModel.stopScanning()
                        bleViewModel.resetDeviceFound()

                        kotlinx.coroutines.GlobalScope.launch {
                            kotlinx.coroutines.delay(200)
                            hasCheckedInitialSession = false
                            attendanceViewModel.resetAttendanceStatus()

                            attendanceViewModel.checkActiveSession { result ->
                                isSessionActive = result.isActive
                                if (result.session != null) {
                                    currentSubject = result.session!!.subject
                                    currentRoom = result.session!!.room
                                    currentType = result.session!!.type
                                    isAttendanceMarked = false

                                    if (result.isActive) {
                                        Timber.d("✅ Session check found active session: $currentSubject in $currentRoom")
                                        Timber.d("📝 Attendance status: ${result.message}")
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
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAttendanceCompletedToday) Color(0xFF8E8E93) else Color(0xFF007AFF),
                    disabledContainerColor = Color(0xFF34C759)
                ),
                enabled = !isCheckingSession && !isAttendanceCompletedToday // DISABLE when attendance is completed
            ) {
                if (isCheckingSession) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector =if (isAttendanceCompletedToday) Icons.Default.Check else Icons.Default.Refresh,
                        contentDescription = "Check Session",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAttendanceCompletedToday) "Attendance Already Marked" else "Check for New Session",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color =  Color.White
                )
            }


            Spacer(modifier = Modifier.height(24.dp))

            // Detection Progress Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Detection Progress",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF007AFF)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Detection Progress",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1D1F)
                    )
                }

            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Steps
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Session Active Step
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSessionActive) Color(0xFF34C759).copy(alpha = 0.1f)
                                else Color(0xFF8E8E93).copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Session Active",
                            modifier = Modifier.size(24.dp),
                            tint = if (isSessionActive) Color(0xFF34C759) else Color(0xFF8E8E93)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Session Active",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1D1D1F)
                        )
                        Text(
                            text = if (isSessionActive) "Session active - Attendance marked" else "Waiting for active session",
                            fontSize = 14.sp,
                            color = Color(0xFF8E8E93)
                        )
                    }

                    Text(
                        text = if (isSessionActive) "Completed" else "Pending",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSessionActive) Color(0xFF34C759) else Color(0xFF8E8E93)
                    )
                }

                // Presence Detection Step
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (deviceFound || isAttendanceCompletedToday) Color(0xFF34C759).copy(alpha = 0.1f)
                                else Color(0xFF8E8E93).copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Presence Detection",
                            modifier = Modifier.size(24.dp),
                            tint = if (deviceFound || isAttendanceCompletedToday) Color(0xFF34C759) else Color(0xFF8E8E93)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Presence Detection",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1D1D1F)
                        )
                        Text(
                            text = if (isAttendanceCompletedToday) "Presence was detected successfully"
                            else if (deviceFound) "Device detected in ${bleViewModel.getDetectedRoomName()}"
                            else if (bleState == BleState.SCANNING) "Scanning for room devices..."
                            else "Waiting for room proximity",
                            fontSize = 14.sp,
                            color = Color(0xFF8E8E93)
                        )
                    }

                    Text(
                        text = if (deviceFound || isAttendanceCompletedToday) "Completed" else "Pending",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (deviceFound || isAttendanceCompletedToday) Color(0xFF34C759) else Color(0xFF8E8E93)
                    )
                }

                // Attendance Confirmation Step
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isAttendanceCompletedToday) Color(0xFF34C759).copy(alpha = 0.1f)
                                else Color(0xFF8E8E93).copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Attendance Confirmation",
                            modifier = Modifier.size(24.dp),
                            tint = if (isAttendanceCompletedToday) Color(0xFF34C759) else Color(0xFF8E8E93)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Attendance Confirmation",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1D1D1F)
                        )
                        Text(
                            text = if (isAttendanceCompletedToday) "Attendance marked successfully"
                            else "Waiting for confirmation",
                            fontSize = 14.sp,
                            color = Color(0xFF8E8E93)
                        )
                    }

                    Text(
                        text = if (isAttendanceCompletedToday) "Completed" else "Pending",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isAttendanceCompletedToday) Color(0xFF34C759) else Color(0xFF8E8E93)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }


    Log.d("StudentHomeScreen","Current Room: $currentRoom")
    attendanceViewModel.deviceRoom=currentRoom


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

            attendanceViewModel.disableAutoScan()


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

}