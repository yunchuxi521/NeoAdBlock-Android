package com.adblock.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.adblock.app.db.AppDatabase
import com.adblock.app.stats.StatsTracker
import com.adblock.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun DashboardScreen(
    isActive: Boolean = true
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    var blockedToday by remember { mutableIntStateOf(0) }
    var totalBlocked by remember { mutableIntStateOf(0) }
    var savedMb by remember { mutableDoubleStateOf(0.0) }
    var savedSec by remember { mutableIntStateOf(0) }
    var ruleCount by remember { mutableStateOf("14万") }
    var hourlyData by remember { mutableStateOf<List<Int>>(emptyList()) }
    var adSkipsToday by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val todayStart = now / 86_400_000L * 86_400_000L
                val blocked = db.statsDao().getTodayBlockedCount(todayStart)
                val total = db.statsDao().getTotalBlockedCount()
                val recentHours = db.statsDao().getRecentHours()
                val bytes = StatsTracker.estimateDataSavedBytes(blocked.toLong())
                val secs = StatsTracker.estimateTimeSavedSec(blocked.toLong())
                val skipCount = db.adSkipLogDao().getCountSince(todayStart)
                val userRuleCount = db.userRuleDao().count()
                val accRuleCount = db.accessibilityRuleDao().count()
                val totalRules = userRuleCount + accRuleCount
                val formattedCount = if (totalRules >= 10000) {
                    "${totalRules / 10000}万"
                } else {
                    "%,d".format(totalRules)
                }

                withContext(Dispatchers.Main) {
                    blockedToday = blocked
                    totalBlocked = total
                    savedMb = bytes / 1_000_000.0
                    savedSec = secs.toInt()
                    ruleCount = formattedCount
                    adSkipsToday = skipCount
                    hourlyData = recentHours
                        .take(12)
                        .reversed()
                        .map { it.blockedCount }
                }
            }
            delay(10_000L)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = TextMuted.copy(alpha = 0.04f)
            for (i in 0 until 6) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, i * size.height / 5f),
                    end = Offset(size.width, size.height / 3f + (i * 40f)),
                    strokeWidth = 1f
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            StatusTopBar(isActive = isActive)
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ShieldRing(isActive = isActive)
            }

            Spacer(modifier = Modifier.height(32.dp))

            BentoGrid(
                blockedToday = blockedToday,
                dataSavedMb = savedMb,
                timeSavedSec = savedSec,
                adSkipsToday = adSkipsToday,
                ruleCount = ruleCount,
                isActive = isActive,
                hourlyData = hourlyData
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
