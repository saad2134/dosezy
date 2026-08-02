package com.example.dosezy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.luminance
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun CustomNavigationBar(
    navController: NavController,
    currentDestination: NavDestination?,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        BottomNavItem("Home", Icons.Default.Home, "home"),
        BottomNavItem("Schedule", Icons.Default.CalendarMonth, "schedule"),
        BottomNavItem("Add", Icons.Default.Add, "add_med"),
        BottomNavItem("Medicines", Icons.Default.Medication, "medicines"),
        BottomNavItem("Menu", Icons.Default.Menu, "menu")
    )

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val borderColor = if (isDark) Color(0xFF303235) else Color(0xFFD1D5DB)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Home
        CustomNavItem(
            item = navItems[0],
            isSelected = currentDestination?.route == navItems[0].route,
            onClick = {
                navController.navigate(navItems[0].route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier.weight(1f)
        )

        // Schedule
        CustomNavItem(
            item = navItems[1],
            isSelected = currentDestination?.route == navItems[1].route,
            onClick = {
                navController.navigate(navItems[1].route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier.weight(1f)
        )

        // Center Add Button with Squircle Shape
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = {
                    navController.navigate(navItems[2].route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp), // Squircle shape
                        clip = false
                    )
                    .background(
                        color = Color(0xFF2084E4),
                        shape = RoundedCornerShape(16.dp) // Squircle shape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add New Medicine",
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
            }
        }

        // Medicines
        CustomNavItem(
            item = navItems[3],
            isSelected = currentDestination?.route == navItems[3].route,
            onClick = {
                navController.navigate(navItems[3].route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier.weight(1f)
        )

        // Menu
        CustomNavItem(
            item = navItems[4],
            isSelected = currentDestination?.route == navItems[4].route,
            onClick = {
                navController.navigate(navItems[4].route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun CustomNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = if (isSelected) Color(0xFF2084E4) else Color(0xFF9CA3AF)
    val iconColor = if (isSelected) Color(0xFF2084E4) else Color(0xFF9CA3AF)

    Box(
        modifier = modifier
            .height(70.dp)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(
                    onClick = onClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(24.dp),
                tint = iconColor
            )
            Text(
                text = item.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center, // Center align text
                maxLines = 1 // Prevent text wrapping
            )
        }
    }
}

// Simple preview without complex navigation mocking
@Preview(showBackground = true, widthDp = 360, heightDp = 80)
@Composable
fun CustomNavigationBarPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color.LightGray),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Create a simple preview that simulates the navigation bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home - selected
                PreviewNavItem(
                    item = BottomNavItem("Home", Icons.Default.Home, "home"),
                    isSelected = true,
                    modifier = Modifier.weight(1f)
                )

                // Schedule - not selected
                PreviewNavItem(
                    item = BottomNavItem("Schedule", Icons.Default.CalendarMonth, "schedule"),
                    isSelected = false,
                    modifier = Modifier.weight(1f)
                )

                // Center Add Button with Squircle
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(16.dp),
                                clip = false
                            )
                            .background(
                                color = Color(0xFF2084E4),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add New Medicine",
                            modifier = Modifier.size(36.dp),
                            tint = Color.White
                        )
                    }
                }

                // Medicines - not selected
                PreviewNavItem(
                    item = BottomNavItem("Medicines", Icons.Default.Medication, "medicines"),
                    isSelected = false,
                    modifier = Modifier.weight(1f)
                )

                // Menu - not selected
                PreviewNavItem(
                    item = BottomNavItem("Menu", Icons.Default.Menu, "menu"),
                    isSelected = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Preview with all items unselected
@Preview(showBackground = true, widthDp = 360, heightDp = 80)
@Composable
fun CustomNavigationBarAllUnselectedPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color.LightGray),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All items unselected
                PreviewNavItem(
                    item = BottomNavItem("Home", Icons.Default.Home, "home"),
                    isSelected = false,
                    modifier = Modifier.weight(1f)
                )

                PreviewNavItem(
                    item = BottomNavItem("Schedule", Icons.Default.CalendarMonth, "schedule"),
                    isSelected = false,
                    modifier = Modifier.weight(1f)
                )

                // Center Add Button with Squircle
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(16.dp),
                                clip = false
                            )
                            .background(
                                color = Color(0xFF2084E4),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add New Medicine",
                            modifier = Modifier.size(36.dp),
                            tint = Color.White
                        )
                    }
                }

                PreviewNavItem(
                    item = BottomNavItem("Medicines", Icons.Default.Medication, "medicines"),
                    isSelected = false,
                    modifier = Modifier.weight(1f)
                )

                PreviewNavItem(
                    item = BottomNavItem("Menu", Icons.Default.Menu, "menu"),
                    isSelected = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Helper composable for simple preview
@Composable
fun PreviewNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (isSelected) Color(0xFF2084E4) else Color(0xFF9CA3AF)
    val iconColor = if (isSelected) Color(0xFF2084E4) else Color(0xFF9CA3AF)

    Box(
        modifier = modifier
            .height(70.dp) // Fixed height to match navigation bar
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(
                    onClick = { },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(24.dp),
                tint = iconColor
            )
            Text(
                text = item.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}