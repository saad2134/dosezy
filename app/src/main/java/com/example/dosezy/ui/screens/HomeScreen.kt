package com.example.dosezy.ui.screens

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dosezy.ui.components.TopBar
import com.example.dosezy.ui.viewmodels.UserViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(navController: NavController) {
    val userViewModel: UserViewModel = hiltViewModel()
    val currentUser by userViewModel.currentUser.collectAsState()

    // Get current day for subtitle
    val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopBar(
            navController = navController,
            currentUser = currentUser,
            title = "Today's Meds",
            subtitle = "($dayOfWeek)",
            actions = {} // Add empty actions
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Home Screen Content", color = Color.Black)
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen(navController = androidx.navigation.compose.rememberNavController())
}