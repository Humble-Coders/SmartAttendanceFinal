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
        private val CLASS_NAME_KEY = stringPreferencesKey("class_name") // New key for class
    }

    // Get profile data as Flow (including class name)
    val profileData: Flow<ProfileData> = context.dataStore.data.map { preferences ->
        val profile = ProfileData(
            name = preferences[NAME_KEY] ?: "",
            rollNumber = preferences[ROLL_NUMBER_KEY] ?: "",
            className = preferences[CLASS_NAME_KEY] ?: "",
            isFaceRegistered = false // Always false since we don't manage this anymore
        )
        Timber.d("ProfileRepository - Reading profile data: name='${profile.name}', rollNumber='${profile.rollNumber}', className='${profile.className}'")
        profile
    }

    // Save profile data (name, roll number, and class)
    suspend fun saveProfile(name: String, rollNumber: String, className: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[NAME_KEY] = name
                preferences[ROLL_NUMBER_KEY] = rollNumber
                preferences[CLASS_NAME_KEY] = className
            }
            Timber.d("ProfileRepository - Profile saved: name='$name', rollNumber='$rollNumber', className='$className'")
        } catch (e: Exception) {
            Timber.e(e, "ProfileRepository - Failed to save profile")
            throw e
        }
    }

    // Overloaded method for backward compatibility
    suspend fun saveProfile(name: String, rollNumber: String) {
        saveProfile(name, rollNumber, "")
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

    // Get current class name
    suspend fun getCurrentClassName(): String {
        return try {
            val profile = profileData.first()
            profile.className
        } catch (e: Exception) {
            Timber.e(e, "ProfileRepository - Failed to get current class name")
            ""
        }
    }

    // Update only class name
    suspend fun updateClassName(className: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[CLASS_NAME_KEY] = className
            }
            Timber.d("ProfileRepository - Class name updated: '$className'")
        } catch (e: Exception) {
            Timber.e(e, "ProfileRepository - Failed to update class name")
            throw e
        }
    }
}