package com.humblecoders.smartattendance.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.humblecoders.smartattendance.data.model.ProfileData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber

// Extension property to get DataStore instance
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "profile_prefs")

class ProfileRepository(private val context: Context) {

    companion object {
        private val NAME_KEY = stringPreferencesKey("user_name")
        private val ROLL_NUMBER_KEY = stringPreferencesKey("roll_number")
        private val FACE_REGISTERED_KEY = booleanPreferencesKey("face_registered")
    }

    // Get profile data as Flow
    val profileData: Flow<ProfileData> = context.dataStore.data.map { preferences ->
        val profile = ProfileData(
            name = preferences[NAME_KEY] ?: "",
            rollNumber = preferences[ROLL_NUMBER_KEY] ?: "",
            isFaceRegistered = preferences[FACE_REGISTERED_KEY] ?: false
        )
        Timber.d("ProfileRepository - Reading profile data: name='${profile.name}', rollNumber='${profile.rollNumber}', faceRegistered=${profile.isFaceRegistered}")
        profile
    }

    // Save profile data
    suspend fun saveProfile(name: String, rollNumber: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[NAME_KEY] = name
                preferences[ROLL_NUMBER_KEY] = rollNumber
            }
            Timber.d("ProfileRepository - Profile saved: name='$name', rollNumber='$rollNumber'")
        } catch (e: Exception) {
            Timber.e(e, "ProfileRepository - Failed to save profile")
            throw e
        }
    }

    // Update face registration status
    suspend fun updateFaceRegistrationStatus(isRegistered: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[FACE_REGISTERED_KEY] = isRegistered
            }
            Timber.d("ProfileRepository - Face registration status updated to: $isRegistered")

            // Verify the update was successful by reading it back
            val updatedProfile = profileData.first()
            Timber.d("ProfileRepository - Verification read: faceRegistered=${updatedProfile.isFaceRegistered}")

            if (updatedProfile.isFaceRegistered != isRegistered) {
                Timber.e("ProfileRepository - Face registration status update failed! Expected: $isRegistered, Got: ${updatedProfile.isFaceRegistered}")
                throw Exception("Face registration status update verification failed")
            }
        } catch (e: Exception) {
            Timber.e(e, "ProfileRepository - Failed to update face registration status")
            throw e
        }
    }

    // Clear all profile data
    suspend fun clearAllProfile() {
        try {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
            Timber.d("ProfileRepository - All profile data cleared")
        } catch (e: Exception) {
            Timber.e(e, "ProfileRepository - Failed to clear profile data")
            throw e
        }
    }

    // Get current face registration status synchronously
    suspend fun getFaceRegistrationStatus(): Boolean {
        return try {
            val profile = profileData.first()
            Timber.d("ProfileRepository - Current face registration status: ${profile.isFaceRegistered}")
            profile.isFaceRegistered
        } catch (e: Exception) {
            Timber.e(e, "ProfileRepository - Failed to get face registration status")
            false
        }
    }
}