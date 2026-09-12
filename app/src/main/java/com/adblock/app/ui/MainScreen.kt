package com.adblock.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.adblock.app.ui.theme.*

@Composable
fun MainScreen(
    isActive: Boolean = true,
    onToggleVpn: () -> Unit = {},
    appsScreen: @Composable () -> Unit = {},
    rulesScreen: @Composable () -> Unit = {},
    settingsScreen: @Composable () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("dashboard") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBgStart, DarkBgEnd)
                )
            )
    ) {
        when (selectedTab) {
            "dashboard" -> DashboardScreen(
                isActive = isActive,
                onToggleVpn = onToggleVpn
            )
            "apps" -> appsScreen()
            "rules" -> rulesScreen()
            "settings" -> settingsScreen()
        }

        BottomNavBar(
            selectedRoute = selectedTab,
            onItemSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
