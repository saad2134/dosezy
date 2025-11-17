package com.example.dosezy.ui.subscreens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.viewmodels.UserViewModel

@Composable
fun EmergencyScreen(navController: NavController) {
    val userViewModel: UserViewModel = hiltViewModel()
    val currentUser by userViewModel.currentUser.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopBar(
            navController = navController,
            currentUser = currentUser,
            title = "Emergency",
            showBackButton = true,
            titleColor = Color(0xFFEF4444),
            actions = {} // Add empty actions
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Emergency Screen Content", color = Color.Black)
        }
    }
}