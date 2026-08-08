package com.example.dosezy.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dosezy.R

@Composable
fun LoadingScreen(
    isLoading: Boolean = true,
    hasError: Boolean = false,
    message: String = "Loading..."
) {
    val displayMessage = if (hasError) {
        "Something went wrong. Starting onboarding..."
    } else {
        message
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo/Icon
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.loader_icon),
                contentDescription = "Dosezy Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading && !hasError) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color(0xFF2084E4)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                text = displayMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}