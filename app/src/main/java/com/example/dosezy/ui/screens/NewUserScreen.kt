package com.example.dosezy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun NewUserScreen(
    currentFrame: Int,
    onNext: (Int) -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome to Dosezy!")
        Text(text = "Frame $currentFrame of 3")

        // Add your actual onboarding content for each frame here
        when (currentFrame) {
            1 -> Text("Frame 1: Introduction to the app")
            2 -> Text("Frame 2: How to add medicines")
            3 -> Text("Frame 3: Setting up your schedule")
        }

        Button(
            onClick = { onNext(currentFrame + 1) },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(if (currentFrame == 3) "Get Started" else "Next")
        }

        if (currentFrame < 3) {
            Button(
                onClick = onSkip,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Skip")
            }
        }
    }
}

@Preview
@Composable
fun PreviewNewUserScreen() {
    NewUserScreen(
        currentFrame = 1,
        onNext = { },
        onSkip = { }
    )
}