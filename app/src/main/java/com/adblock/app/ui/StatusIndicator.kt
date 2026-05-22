package com.adblock.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.adblock.app.ui.theme.*

@Composable
fun StatusTopBar(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vpn_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(12.dp).alpha(if (isActive) pulseAlpha else 0.3f)) {
            drawCircle(color = if (isActive) LaserGreen else TextMuted)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isActive) "VPN 已连接" else "VPN 未连接",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isActive) LaserGreen.copy(alpha = pulseAlpha) else TextMuted
        )
    }
}
