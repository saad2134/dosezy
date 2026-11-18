package com.example.dosezy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Guard: Do not programmatically lock portrait orientation so tablets, foldables, and desktop displays resize cleanly (#120)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}