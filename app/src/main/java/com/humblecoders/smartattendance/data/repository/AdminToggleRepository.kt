package com.humblecoders.smartattendance.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Singleton

@Singleton
class AdminToggleRepository(
) {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun isFaceRecognitionEnabled(): Boolean {
        return try {
            val document = firestore.collection("adminToggles")
                .document("faceRecognition")
                .get()
                .await()

            val isActivated = document.getBoolean("isActivated") ?: true
            Timber.d("🔧 Face recognition toggle: $isActivated")
            isActivated
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to fetch face recognition toggle, defaulting to true")
            true // Default to enabled if fetch fails
        }
    }
}