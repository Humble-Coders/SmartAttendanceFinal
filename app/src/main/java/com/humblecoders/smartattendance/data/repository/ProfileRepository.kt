package com.humblecoders.smartattendance.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.humblecoders.smartattendance.data.model.ProfileData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
        ProfileData(
            name = preferences[NAME_KEY] ?: "",
            rollNumber = preferences[ROLL_NUMBER_KEY] ?: "",
            isFaceRegistered = preferences[FACE_REGISTERED_KEY] ?: false
        )
    }

    // Save profile data
    suspend fun saveProfile(name: String, rollNumber: String) {
        context.dataStore.edit { preferences ->
            preferences[NAME_KEY] = name
            preferences[ROLL_NUMBER_KEY] = rollNumber
        }
    }

    // Update face registration status
    suspend fun updateFaceRegistrationStatus(isRegistered: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FACE_REGISTERED_KEY] = isRegistered
        }
    }

    // Clear all profile data

}