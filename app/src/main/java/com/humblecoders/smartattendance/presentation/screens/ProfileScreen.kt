package com.humblecoders.smartattendance.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.humblecoders.smartattendance.presentation.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onFaceRegistrationClick: () -> Unit
) {
    val profileData by profileViewModel.profileData.collectAsState()
    val nameInput by profileViewModel.nameInput.collectAsState()
    val rollNumberInput by profileViewModel.rollNumberInput.collectAsState()
    val isSaving by profileViewModel.isSaving.collectAsState()
    val isFormInitialized by profileViewModel.isFormInitialized.collectAsState()
    val isProfileSaved by profileViewModel.isProfileSaved.collectAsState()

    // Local state for UI feedback
    var showSaveSuccess by remember { mutableStateOf(false) }
    var saveErrorMessage by remember { mutableStateOf<String?>(null) }

    // Auto-hide success message
    LaunchedEffect(showSaveSuccess) {
        if (showSaveSuccess) {
            kotlinx.coroutines.delay(2000)
            showSaveSuccess = false
        }
    }

    // Auto-hide error message
    LaunchedEffect(saveErrorMessage) {
        if (saveErrorMessage != null) {
            kotlinx.coroutines.delay(5000)
            saveErrorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Setup") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = remember { SnackbarHostState() }
            )
        }
    ) { paddingValues ->
        if (!isFormInitialized) {
            // Show loading while form is being initialized
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Success Message
                if (showSaveSuccess) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Profile saved successfully!",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Error Message
                saveErrorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Profile Input Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Student Information",
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Name Input
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { profileViewModel.updateNameInput(it) },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isSaving,
                            isError = nameInput.isBlank() && isProfileSaved
                        )

                        // Roll Number Input
                        OutlinedTextField(
                            value = rollNumberInput,
                            onValueChange = { profileViewModel.updateRollNumberInput(it) },
                            label = { Text("Roll Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isSaving,
                            isError = rollNumberInput.isBlank() && isProfileSaved
                        )

                        // Save Button
                        Button(
                            onClick = {
                                profileViewModel.saveProfile(
                                    onSuccess = {
                                        showSaveSuccess = true
                                        saveErrorMessage = null
                                    },
                                    onError = { error ->
                                        saveErrorMessage = error
                                        showSaveSuccess = false
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = profileViewModel.isFormValid() && !isSaving
                        ) {
                            if (isSaving) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Saving...")
                                }
                            } else {
                                Text("Save Profile")
                            }
                        }
                    }
                }

                // Face Registration Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (profileData.isFaceRegistered) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (profileData.isFaceRegistered) {
                                    Icons.Default.Check
                                } else {
                                    Icons.Default.Close
                                },
                                contentDescription = null,
                                tint = if (profileData.isFaceRegistered) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = if (profileData.isFaceRegistered) {
                                    "Face Registered"
                                } else {
                                    "Face Not Registered"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Text(
                            text = if (profileData.isFaceRegistered) {
                                "Your face has been registered for attendance\nRoll Number: ${profileData.rollNumber}"
                            } else {
                                "Register your face to enable attendance marking"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        if (!profileData.isFaceRegistered) {
                            if (!isProfileSaved) {
                                Text(
                                    text = "Please save your profile first",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Button(
                                onClick = onFaceRegistrationClick,
                                enabled = isProfileSaved && !isSaving,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Register Face")
                            }
                        } else {
                            // Option to re-register face
                            OutlinedButton(
                                onClick = onFaceRegistrationClick,
                                enabled = isProfileSaved && !isSaving,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Re-register Face")
                            }
                        }
                    }
                }

                // Profile Summary (show current saved data)
                if (isProfileSaved) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Current Profile",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Name: ${profileData.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Roll Number: ${profileData.rollNumber}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Face Status: ${if (profileData.isFaceRegistered) "Registered" else "Not Registered"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}