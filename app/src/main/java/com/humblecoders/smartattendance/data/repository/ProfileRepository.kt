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
        // Remove FACE_REGISTERED_KEY - no longer needed
    }

    // Get profile data as Flow (without face registration)
    val profileData: Flow<ProfileData> = context.dataStore.data.map { preferences ->
        val profile = ProfileData(
            name = preferences[NAME_KEY] ?: "",
            rollNumber = preferences[ROLL_NUMBER_KEY] ?: "",
            isFaceRegistered = false // Always false since we don't manage this anymore
        )
        Timber.d("ProfileRepository - Reading profile data: name='${profile.name}', rollNumber='${profile.rollNumber}'")
        profile
    }

    // Save profile data (only name and roll number)
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

    // Remove all face registration related methods:
    // - updateFaceRegistrationStatus()
    // - getFaceRegistrationStatus()
}