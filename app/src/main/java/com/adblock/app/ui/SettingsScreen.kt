package com.adblock.app.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adblock.app.ui.theme.*

@Composable
fun SettingsScreen(
    accessibilityEnabled: Boolean = false,
    onManageAccessibilityRules: () -> Unit = {},
    onOpenRecording: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openAccessibilitySettings(context) },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TileBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AdsClick,
                        contentDescription = null,
                        tint = if (accessibilityEnabled) LaserGreen else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "自动跳过开屏广告",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (accessibilityEnabled) "服务已开启"
                                   else "点击前往系统设置开启",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (accessibilityEnabled) LaserGreen else TextMuted
                        )
                    }
                    if (!accessibilityEnabled) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "前往设置",
                            tint = TextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onManageAccessibilityRules() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TileBg)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AdsClick,
                    contentDescription = null,
                    tint = LaserGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "管理无障碍规则",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "查看和启用/禁用已安装的规则",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMuted
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "管理",
                    tint = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenRecording() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TileBg)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.FiberManualRecord,
                    contentDescription = null,
                    tint = LaserGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "规则录制模式",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "录制点击操作，自动生成规则",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMuted
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "录制",
                    tint = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TileBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "使用说明",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. 点击上方卡片前往系统无障碍设置\n" +
                           "2. 找到 \"AdBlock 广告跳过\" 并开启\n" +
                           "3. 开启后 App 会自动检测并跳过开屏广告\n" +
                           "4. 规则会自动更新，无需手动操作",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

private fun openAccessibilitySettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    )
}
