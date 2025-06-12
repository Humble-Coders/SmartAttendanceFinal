package com.humblecoders.smartattendance.presentation.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign

@Composable
fun AttendanceConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text(
                text = "📡",
                style = MaterialTheme.typography.displaySmall
            )
        },
        title = {
            Text(
                text = "Attendance Device Detected",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Mark attendance?",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}