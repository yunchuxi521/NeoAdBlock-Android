package com.adblock.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adblock.app.ui.theme.*

@Composable
fun ReportAdDialog(
    recentDomains: List<String>,
    onDismiss: () -> Unit,
    onBlockDomain: (String) -> Unit,
    onCustomDomain: (String) -> Unit
) {
    var customDomain by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBgEnd,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "举报漏网广告",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "选择以下最近请求的域名，或手动输入域名",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 250.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(recentDomains.take(20)) { domain ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TileBg, RoundedCornerShape(8.dp))
                                .clickable { onBlockDomain(domain) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = domain,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Block",
                                tint = LaserGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = { showCustomInput = !showCustomInput }) {
                    Text(
                        text = if (showCustomInput) "收起" else "+ 手动输入域名",
                        color = LaserGreen
                    )
                }

                if (showCustomInput) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customDomain,
                            onValueChange = { customDomain = it },
                            placeholder = { Text("example.com", color = TextMuted) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LaserGreen,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = LaserGreen
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        IconButton(
                            onClick = {
                                if (customDomain.isNotBlank()) {
                                    onCustomDomain(customDomain.trim())
                                    customDomain = ""
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Add",
                                tint = LaserGreen
                            )
                        }
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
