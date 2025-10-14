package com.example.dosezy.notifications

import android.app.Service
import android.content.Intent
import android.os.IBinder

class MedicineReminderService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}