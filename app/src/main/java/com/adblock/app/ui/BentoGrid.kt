package com.adblock.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adblock.app.ui.theme.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BentoGrid(
    blockedToday: Int = 1428,
    dataSavedMb: Double = 45.2,
    timeSavedSec: Int = 185,
    adSkipsToday: Int = 0,
    ruleCount: String = "14万",
    isActive: Boolean = true,
    hourlyData: List<Int> = emptyList(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.height(240.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BlockedTodayTile(
                count = blockedToday,
                isActive = isActive,
                hourlyData = hourlyData,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DataSavedTile(
                    mb = dataSavedMb,
                    isActive = isActive,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                TimeSavedTile(
                    seconds = timeSavedSec,
                    isActive = isActive,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdSkipTile(
                count = adSkipsToday,
                isActive = isActive,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            RulesStatusTile(
                ruleCount = ruleCount,
                isActive = isActive,
                modifier = Modifier.weight(2f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(TileBg, RoundedCornerShape(16.dp))
            .border(1.dp, TileBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun BlockedTodayTile(
    count: Int,
    isActive: Boolean,
    hourlyData: List<Int> = emptyList(),
    modifier: Modifier = Modifier
) {
    val animatedCount = remember { Animatable(0f) }

    LaunchedEffect(count) {
        animatedCount.snapTo(0f)
        animatedCount.animateTo(
            targetValue = count.toFloat(),
            animationSpec = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            )
        )
    }

    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "今日拦截",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Icon(
                    imageVector = Icons.Filled.Equalizer,
                    contentDescription = null,
                    tint = if (isActive) LaserGreen else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "%,d".format(animatedCount.value.toInt()),
                style = MaterialTheme.typography.headlineLarge,
                color = if (isActive) LaserGreen else TextMuted,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            MiniBarChart(
                isActive = isActive,
                data = hourlyData,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
        }
    }
}

@Composable
private fun MiniBarChart(
    isActive: Boolean,
    data: List<Int>? = null,
    modifier: Modifier = Modifier
) {
    val barValues = remember(data) {
        if (data.isNullOrEmpty()) {
            listOf(0.3f, 0.6f, 0.4f, 0.8f, 0.5f, 0.9f, 0.7f, 1.0f, 0.6f, 0.85f, 0.45f, 0.7f)
        } else {
            val max = data.max().coerceAtLeast(1)
            data.map { it.toFloat() / max }
        }
    }
    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * 1.5f)
        val gap = barWidth * 0.5f
        val chartColor = if (isActive) LaserGreen.copy(alpha = 0.7f) else TextMuted.copy(alpha = 0.4f)
        val chartColorDim = if (isActive) ChartGreenLight else TextMuted.copy(alpha = 0.1f)

        barValues.forEachIndexed { index, height ->
            val x = index * (barWidth + gap) + gap
            val barHeight = height * size.height * 0.8f
            drawRoundRect(
                color = if (index % 2 == 0) chartColor else chartColorDim,
                topLeft = Offset(x, size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}

private val barCount = 12

@Composable
private fun DataSavedTile(mb: Double, isActive: Boolean, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "节省流量", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isActive) LaserGreen.copy(alpha = 0.15f) else TextMuted.copy(alpha = 0.1f),
                            RoundedCornerShape(50)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = null,
                        tint = if (isActive) LaserGreen else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "%.1f MB".format(mb),
                style = MaterialTheme.typography.headlineMedium,
                color = if (isActive) TextPrimary else TextMuted,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TimeSavedTile(seconds: Int, isActive: Boolean, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "节省时间", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = if (isActive) LaserGreen else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "%,d 秒".format(seconds),
                style = MaterialTheme.typography.headlineMedium,
                color = if (isActive) TextPrimary else TextMuted,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AdSkipTile(count: Int, isActive: Boolean, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isActive) LaserGreen.copy(alpha = 0.15f) else TextMuted.copy(alpha = 0.1f),
                        RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AdsClick,
                    contentDescription = null,
                    tint = if (isActive) LaserGreen else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "跳过广告",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = "%,d 次".format(count),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isActive) TextPrimary else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RulesStatusTile(ruleCount: String, isActive: Boolean, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "规则库状态", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "最新 ($ruleCount 条)",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isActive) LaserGreen else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
            // Arc gauge
            Box(modifier = Modifier.size(64.dp)) {
                Canvas(modifier = Modifier.size(64.dp)) {
                    val strokeWidth = 4.dp.toPx()
                    val arcColor = if (isActive) LaserGreen else TextMuted.copy(alpha = 0.3f)
                    val bgArcColor = SurfaceBorder

                    drawArc(
                        color = bgArcColor,
                        startAngle = 140f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(strokeWidth, strokeWidth),
                        size = androidx.compose.ui.geometry.Size(size.width - strokeWidth * 2, size.height - strokeWidth * 2)
                    )

                    drawArc(
                        color = arcColor,
                        startAngle = 140f,
                        sweepAngle = 260f * 0.85f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(strokeWidth, strokeWidth),
                        size = androidx.compose.ui.geometry.Size(size.width - strokeWidth * 2, size.height - strokeWidth * 2)
                    )

                    val endAngleDeg = 140.0 + 260.0 * 0.85
                    val endAngleRad = Math.toRadians(endAngleDeg)
                    val dotRadius = (size.minDimension / 2) - strokeWidth / 2
                    val dotX = center.x + dotRadius * cos(endAngleRad).toFloat()
                    val dotY = center.y + dotRadius * sin(endAngleRad).toFloat()
                    drawCircle(
                        color = arcColor,
                        radius = 3.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }
            }
        }
    }
}
