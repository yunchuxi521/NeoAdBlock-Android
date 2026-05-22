package com.adblock.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adblock.app.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ShieldRing(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val dotPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots"
    )

    Box(
        modifier = modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glow background
        if (isActive) {
            Canvas(modifier = Modifier.size(220.dp)) {
                drawCircle(
                    brush = RadialGradient(
                        colors = listOf(
                            LaserGreen.copy(alpha = glowAlpha * 0.15f),
                            Color.Transparent,
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension / 2
                    )
                )
            }
        }

        // Outer animated ring
        Canvas(modifier = Modifier.size(200.dp)) {
            val strokeWidth = 2.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2

            // Base ring
            drawCircle(
                color = SurfaceBorder,
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            if (isActive) {
                // Sweep gradient arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            LaserGreen,
                            LaserGreen.copy(alpha = 0.3f),
                            Color.Transparent,
                            LaserGreen.copy(alpha = 0.3f),
                            LaserGreen
                        ),
                        center = center
                    ),
                    startAngle = rotation,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth * 1.5f, cap = StrokeCap.Round),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - strokeWidth,
                        size.height - strokeWidth
                    )
                )

                // Inner counter-rotating ring
                drawArc(
                    color = LaserGreen.copy(alpha = 0.2f),
                    startAngle = -rotation,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(strokeWidth * 3, strokeWidth * 3),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - strokeWidth * 6,
                        size.height - strokeWidth * 6
                    )
                )

                // Orbiting dots
                val dotRadius = 3.dp.toPx()
                val orbitRadius = radius - 4.dp.toPx()
                for (i in 0 until 3) {
                    val angle = Math.toRadians((dotPhase + i * 120f).toDouble())
                    val dx = (orbitRadius * cos(angle)).toFloat()
                    val dy = (orbitRadius * sin(angle)).toFloat()
                    drawCircle(
                        color = LaserGreen.copy(alpha = 0.6f),
                        radius = dotRadius,
                        center = center + Offset(dx, dy)
                    )
                }
            }
        }

        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "全局防护",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box {
                if (isActive) {
                    Canvas(modifier = Modifier.size(72.dp)) {
                        drawCircle(
                            color = LaserGreen.copy(alpha = glowAlpha * 0.2f),
                            radius = size.minDimension / 2
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = "Status",
                    modifier = Modifier.size(48.dp),
                    tint = if (isActive) LaserGreen else TextMuted
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "∿ ${if (isActive) "已开启" else "已关闭"} ∿",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (isActive) LaserGreen.copy(alpha = 0.6f) else TextMuted,
                    fontWeight = FontWeight.Light
                )
            )
        }
    }
}
