package com.example.dosezy.notifications

import android.app.Service
import android.content.Intent
import android.os.IBinder

class MedicineForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}