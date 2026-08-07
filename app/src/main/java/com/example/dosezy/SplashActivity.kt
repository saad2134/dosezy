package com.example.dosezy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.setBackgroundColor(android.graphics.Color.parseColor("#0F172A"))
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}