package com.humblecoders.smartattendance.presentation.components

import androidx.compose.runtime.*
import timber.log.Timber

enum class OverlayState {
    NONE,
    ROOM_DETECTION_STARTING,
    CLASSROOM_DETECTED
}

@Composable
fun AttendanceOverlayManager(
    overlayState: OverlayState,
    roomName: String,
    onOverlayDismissed: () -> Unit,
    onSequenceComplete: () -> Unit
) {
    var currentState by remember(overlayState) { mutableStateOf(overlayState) }

    // Handle overlay state changes
    LaunchedEffect(overlayState) {
        currentState = overlayState
        if (overlayState != OverlayState.NONE) {
            Timber.d("🎭 Overlay state changed to: $overlayState")
        }
    }

    when (currentState) {
        OverlayState.ROOM_DETECTION_STARTING -> {
            RoomDetectionStartingOverlay(
                roomName = roomName,
                onDismiss = {
                    Timber.d("🎭 Room detection overlay dismissed")
                    currentState = OverlayState.NONE
                    onOverlayDismissed()
                }
            )
        }

        OverlayState.CLASSROOM_DETECTED -> {
            ClassroomDetectedOverlay(
                roomName = roomName,
                onDismiss = {
                    Timber.d("🎭 Classroom detected overlay dismissed, starting face auth")
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