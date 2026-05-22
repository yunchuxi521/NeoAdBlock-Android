package com.adblock.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adblock.app.accessibility.AccessibilityRuleEntity
import com.adblock.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AccessibilityRuleManageDialog(
    rules: List<AccessibilityRuleEntity>,
    onDismiss: () -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBgEnd,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "无障碍规则管理",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            if (rules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无规则", color = TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(rules, key = { it.id }) { rule ->
                        RuleRow(
                            rule = rule,
                            onToggle = { onToggle(rule.id, it) },
                            onDelete = { onDelete(rule.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成", color = LaserGreen)
            }
        }
    )
}

@Composable
private fun RuleRow(
    rule: AccessibilityRuleEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TileBg, RoundedCornerShape(10.dp))
            .border(1.dp, TileBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = rule.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                SourceBadge(source = rule.source)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "添加于 ${dateFormat.format(Date(rule.createdAt))}",
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted
            )
        }

        Switch(
            checked = rule.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LaserGreen,
                checkedTrackColor = LaserGreen.copy(alpha = 0.3f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceBorder
            )
        )

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "删除",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SourceBadge(source: String) {
    val label = when (source) {
        "builtin" -> "内置"
        "upstream" -> "上游"
        else -> source
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (source == "builtin") CardGreenBg else ChartGreenLight
    ) {
        Text(
            text = label,
            color = LaserGreen,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
