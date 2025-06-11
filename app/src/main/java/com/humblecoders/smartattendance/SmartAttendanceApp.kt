package com.humblecoders.smartattendance

import android.app.Application
import timber.log.Timber

class SmartAttendanceApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        Timber.plant(Timber.DebugTree())
    }
}