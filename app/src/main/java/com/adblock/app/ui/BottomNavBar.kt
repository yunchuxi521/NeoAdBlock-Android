package com.adblock.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.adblock.app.ui.theme.*

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val navItems = listOf(
    NavItem("控制台", Icons.Filled.Dashboard, "dashboard"),
    NavItem("应用", Icons.Filled.Apps, "apps"),
    NavItem("规则", Icons.Filled.Rule, "rules"),
    NavItem("设置", Icons.Filled.Settings, "settings")
)

@Composable
fun BottomNavBar(
    selectedRoute: String = "dashboard",
    onItemSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0x0D000000),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .background(Color.Transparent),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = item.route == selectedRoute
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onItemSelected(item.route) }
                        .padding(vertical = 4.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) NavActive else NavInactive,
                            modifier = Modifier.size(24.dp)
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-2).dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(LaserGreen)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        color = if (isSelected) NavActive else NavInactive,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
