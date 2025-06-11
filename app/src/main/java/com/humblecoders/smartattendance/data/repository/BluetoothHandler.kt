package com.humblecoders.smartattendance.data.repository

import android.content.Context
import android.os.Handler
import android.os.Looper

object BluetoothHandler {
    private var handler: Handler? = null

    fun getInstance(context: Context): Handler {
        if (handler == null) {
            handler = Handler(Looper.getMainLooper())
        }
        return handler!!
    }
}