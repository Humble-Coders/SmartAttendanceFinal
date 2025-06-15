package com.humblecoders.smartattendance.presentation.components

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import timber.log.Timber

enum class OverlayState {
    NONE,
    ROOM_DETECTION_STARTING,
    CLASSROOM_DETECTED
}

// ADD these new state variables at the top of AttendanceOverlayManager

@Composable
fun AttendanceOverlayManager(
    overlayState: OverlayState,
    roomName: String,
    onOverlayDismissed: () -> Unit,
    onSequenceComplete: () -> Unit
) {
    var currentState by remember { mutableStateOf(OverlayState.NONE) }
    var roomDetectionStartTime by remember { mutableStateOf(0L) }
    var pendingClassroomDetected by remember { mutableStateOf(false) }

    // Handle incoming overlay state changes
    LaunchedEffect(overlayState) {
        when (overlayState) {
            OverlayState.ROOM_DETECTION_STARTING -> {
                roomDetectionStartTime = System.currentTimeMillis()
                currentState = OverlayState.ROOM_DETECTION_STARTING
                pendingClassroomDetected = false
            }
            OverlayState.CLASSROOM_DETECTED -> {
                if (currentState == OverlayState.ROOM_DETECTION_STARTING) {
                    val elapsedTime = System.currentTimeMillis() - roomDetectionStartTime
                    if (elapsedTime < 2000L) {
                        // Mark that classroom detected is pending, but don't change state yet
                        pendingClassroomDetected = true
                        return@LaunchedEffect
                    }
                }
                // If room detection has run for 2+ seconds or not showing, show classroom detected
                currentState = OverlayState.CLASSROOM_DETECTED
                pendingClassroomDetected = false
            }
            OverlayState.NONE -> {
                currentState = OverlayState.NONE
                pendingClassroomDetected = false
            }
        }
    }

    // Render only the current state
    when (currentState) {
        OverlayState.ROOM_DETECTION_STARTING -> {
            RoomDetectionStartingOverlay(
                roomName = roomName,
                autoHideAfterMs = 2000L,
                onDismiss = {
                    if (pendingClassroomDetected) {
                        // Transition to classroom detected
                        currentState = OverlayState.CLASSROOM_DETECTED
                        pendingClassroomDetected = false
                    } else {
                        currentState = OverlayState.NONE
                        onOverlayDismissed()
                    }
                }
            )
        }

        OverlayState.CLASSROOM_DETECTED -> {
            ClassroomDetectedOverlay(
                roomName = roomName,
                onDismiss = {
                    currentState = OverlayState.NONE
                    onOverlayDismissed()
                    onSequenceComplete()
                }
            )
        }

        OverlayState.NONE -> {
            // No overlay shown
        }
    }
}

/**
 * Extension function to trigger overlay sequence
 */
fun triggerRoomDetectionSequence(
    onStateChange: (OverlayState) -> Unit,
    roomName: String
) {
    Timber.d("🎭 Starting room detection overlay sequence for room: $roomName")
    onStateChange(OverlayState.ROOM_DETECTION_STARTING)
}

/**
 * Extension function to trigger classroom detected overlay
 */
fun triggerClassroomDetectedSequence(
    onStateChange: (OverlayState) -> Unit,
    roomName: String
) {
    Timber.d("🎭 Triggering classroom detected overlay for room: $roomName")
    onStateChange(OverlayState.CLASSROOM_DETECTED)
}