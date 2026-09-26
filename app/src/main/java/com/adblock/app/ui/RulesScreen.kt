package com.adblock.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adblock.app.db.UserRuleEntity
import com.adblock.app.ui.theme.*

@Composable
fun RulesScreen(
    userRules: List<UserRuleEntity>,
    ruleListMeta: String = "最新",
    onDeleteRule: (Long) -> Unit,
    onReportAd: () -> Unit,
    onSyncNow: () -> Unit,
    onManageSources: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .systemBarsPadding()
    ) {
        Text(
            text = "规则管理",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "规则库状态: $ruleListMeta",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onReportAd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LaserGreen.copy(alpha = 0.15f)
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Filled.Flag,
                    contentDescription = null,
                    tint = LaserGreen
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("举报广告", color = LaserGreen)
            }

            OutlinedButton(
                onClick = onSyncNow,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = SolidColor(SurfaceBorder)
                )
            ) {
                Text("同步规则")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onManageSources,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CardGreenBg
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = LaserGreen
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("规则源管理", color = LaserGreen)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "用户规则 (${userRules.size})",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (userRules.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无用户规则\n点击\"举报广告\"添加",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(userRules, key = { it.id }) { rule ->
                    RuleRow(rule = rule, onDelete = { onDeleteRule(rule.id) })
                }
            }
        }
    }
}

@Composable
private fun RuleRow(rule: UserRuleEntity, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TileBg, RoundedCornerShape(12.dp))
            .border(1.dp, TileBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rule.domain,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when (rule.source) {
                    "manual" -> "手动添加"
                    "report" -> "举报"
                    "upstream" -> "上游同步"
                    else -> rule.source
                },
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete",
                tint = TextMuted
            )
        }
    }
}
