package com.adblock.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adblock.app.accessibility.AccessibilityRuleParser
import com.adblock.app.accessibility.AdSkipperService
import com.adblock.app.accessibility.CapturedInteraction
import com.adblock.app.ui.theme.*

enum class RecordingState { IDLE, RECORDING, REVIEW }

@Composable
fun RecordingDialog(
    interactions: List<CapturedInteraction>,
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onDismiss: () -> Unit,
    onSaveRule: (json: String, packageName: String) -> Unit
) {
    var selectedInteraction by remember { mutableStateOf<CapturedInteraction?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBgEnd,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(LaserGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isRecording) "录制中..." else "规则录制",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column {
                if (isRecording) {
                    Text(
                        text = "正在记录你的点击操作\n切换到目标应用，手动点击广告关闭按钮",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "已捕获 ${interactions.size} 次交互",
                        color = LaserGreen,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onStopRecording,
                        colors = ButtonDefaults.buttonColors(containerColor = LaserGreen.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null, tint = LaserGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("停止录制", color = LaserGreen)
                    }
                } else if (interactions.isEmpty()) {
                    Text(
                        text = "开启录制后切换到目标应用，手动点击广告关闭按钮，系统会记录你的操作并生成规则。",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onStartRecording,
                        colors = ButtonDefaults.buttonColors(containerColor = LaserGreen.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.FiberManualRecord, contentDescription = null, tint = LaserGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("开始录制", color = LaserGreen)
                    }
                } else {
                    // Review mode
                    Text(
                        text = "点击已捕获的交互查看生成的规则",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 250.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(interactions, key = { it.timestamp }) { interaction ->
                            val isSelected = selectedInteraction?.timestamp == interaction.timestamp
                            CapturedRow(
                                interaction = interaction,
                                isSelected = isSelected,
                                onClick = { selectedInteraction = interaction }
                            )
                        }
                    }

                    selectedInteraction?.let { interaction ->
                        Spacer(modifier = Modifier.height(12.dp))
                        val ruleJson = remember(interaction) {
                            val rule = AdSkipperService.generateRule(interaction)
                            AccessibilityRuleParser.toJson(listOf(rule))
                        }
                        Text(
                            text = "生成的规则",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceBorder.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = ruleJson,
                                color = TextMuted,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(8.dp),
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onSaveRule(ruleJson, interaction.packageName) },
                            colors = ButtonDefaults.buttonColors(containerColor = LaserGreen.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, tint = LaserGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("保存规则", color = LaserGreen)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = LaserGreen)
            }
        }
    )
}

@Composable
private fun CapturedRow(
    interaction: CapturedInteraction,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) LaserGreen.copy(alpha = 0.5f) else TileBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TileBg, RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = interaction.packageName,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row {
                if (!interaction.viewId.isNullOrBlank()) {
                    Text(
                        text = "id: ${interaction.viewId}",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMuted
                    )
                } else if (!interaction.text.isNullOrBlank()) {
                    Text(
                        text = "text: ${interaction.text}",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMuted
                    )
                } else if (!interaction.contentDescription.isNullOrBlank()) {
                    Text(
                        text = "desc: ${interaction.contentDescription}",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
