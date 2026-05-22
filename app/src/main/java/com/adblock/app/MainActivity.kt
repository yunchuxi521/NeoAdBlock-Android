package com.adblock.app

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adblock.app.accessibility.AccessibilityRuleEntity
import com.adblock.app.accessibility.CapturedInteraction
import com.adblock.app.db.AppDatabase
import com.adblock.app.db.AppPreferenceEntity
import com.adblock.app.db.RuleListMetaEntity
import com.adblock.app.db.UserRuleEntity
import com.adblock.app.sync.RuleSyncWorker
import com.adblock.app.ui.*
import com.adblock.app.ui.theme.AdBlockTheme
import com.adblock.app.ui.theme.TextSecondary
import com.adblock.app.accessibility.AdSkipperService
import com.adblock.app.vpn.AdBlockVpnService
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    private val isVpnActive: Boolean get() = AdBlockVpnService.currentInstance != null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = (application as AdBlockApplication).database

        setContent {
            AdBlockTheme {
                val apps = remember { mutableStateListOf<AppInfo>() }
                val userRules = remember { mutableStateListOf<UserRuleEntity>() }
                val ruleListMetas = remember { mutableStateListOf<RuleListMetaEntity>() }
                val accessibilityRules = remember { mutableStateListOf<AccessibilityRuleEntity>() }
                var showReportDialog by remember { mutableStateOf(false) }
                var showRuleListManageDialog by remember { mutableStateOf(false) }
                var showAccessibilityRuleManageDialog by remember { mutableStateOf(false) }
                var showRecordingDialog by remember { mutableStateOf(false) }
                var recentDomains by remember { mutableStateOf(listOf<String>()) }

                // Load apps
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        loadInstalledApps(database, apps)
                    }
                }

                // Load user rules
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        val rules = database.userRuleDao().getUserRules()
                        withContext(Dispatchers.Main) {
                            userRules.clear()
                            userRules.addAll(rules)
                        }
                    }
                }

                // Load rule list metas
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        val metas = database.ruleListMetaDao().getAll()
                        withContext(Dispatchers.Main) {
                            ruleListMetas.clear()
                            ruleListMetas.addAll(metas)
                        }
                    }
                }

                // Load accessibility rules
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        val rules = database.accessibilityRuleDao().getAll()
                        withContext(Dispatchers.Main) {
                            accessibilityRules.clear()
                            accessibilityRules.addAll(rules)
                        }
                    }
                }

                MainScreen(
                    isActive = isVpnActive,
                    onToggleVpn = {
                        if (isVpnActive) stopVpn() else startVpn()
                    },
                    appsScreen = {
                        AppsScreen(
                            apps = apps.toList(),
                            onToggleBypass = { packageName, bypass ->
                                scope.launch {
                                    database.appPreferenceDao().setBypass(packageName, bypass)
                                }
                                val index = apps.indexOfFirst { it.packageName == packageName }
                                if (index >= 0) {
                                    apps[index] = apps[index].copy(bypassVpn = bypass)
                                }
                            }
                        )
                    },
                    rulesScreen = {
                        RulesScreen(
                            userRules = userRules.toList(),
                            onDeleteRule = { id ->
                                scope.launch {
                                    database.userRuleDao().deleteById(id)
                                    userRules.removeAll { it.id == id }
                                }
                            },
                            onReportAd = {
                                recentDomains = AdBlockVpnService.currentInstance
                                    ?.requestLogger
                                    ?.getRecentUnblockedDomains() ?: emptyList()
                                showReportDialog = true
                            },
                            onSyncNow = {
                                RuleSyncWorker.syncNow(this@MainActivity)
                                Toast.makeText(
                                    this@MainActivity,
                                    "同步已开始",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onManageSources = { showRuleListManageDialog = true }
                        )
                    },
                    settingsScreen = {
                        val accessibilityEnabled = remember {
                            try {
                                val enabledServices = Settings.Secure.getString(
                                    contentResolver,
                                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                                )
                                enabledServices?.contains("com.adblock.app/.accessibility.AdSkipperService") == true
                            } catch (e: Exception) { false }
                        }
                        SettingsScreen(
                            accessibilityEnabled = accessibilityEnabled,
                            onManageAccessibilityRules = { showAccessibilityRuleManageDialog = true },
                            onOpenRecording = { showRecordingDialog = true }
                        )
                    }
                )

                if (showReportDialog) {
                    ReportAdDialog(
                        recentDomains = recentDomains,
                        onDismiss = { showReportDialog = false },
                        onBlockDomain = { domain ->
                            scope.launch {
                                database.userRuleDao().insert(
                                    UserRuleEntity(
                                        domain = domain,
                                        ruleType = "block",
                                        source = "report"
                                    )
                                )
                                val rules = database.userRuleDao().getUserRules()
                                userRules.clear()
                                userRules.addAll(rules)
                            }
                            showReportDialog = false
                        },
                        onCustomDomain = { domain ->
                            scope.launch {
                                database.userRuleDao().insert(
                                    UserRuleEntity(
                                        domain = domain,
                                        ruleType = "block",
                                        source = "manual"
                                    )
                                )
                                val rules = database.userRuleDao().getUserRules()
                                userRules.clear()
                                userRules.addAll(rules)
                            }
                            showReportDialog = false
                        }
                    )
                }

                if (showRuleListManageDialog) {
                    RuleListManageDialog(
                        ruleLists = ruleListMetas.toList(),
                        onDismiss = { showRuleListManageDialog = false },
                        onToggle = { name, enabled ->
                            scope.launch {
                                val existing = ruleListMetas.find { it.name == name } ?: return@launch
                                database.ruleListMetaDao().upsert(existing.copy(enabled = enabled))
                                ruleListMetas.removeAll { it.name == name }
                                ruleListMetas.add(existing.copy(enabled = enabled))
                            }
                        },
                        onDelete = { name ->
                            scope.launch {
                                database.ruleListMetaDao().delete(name)
                                ruleListMetas.removeAll { it.name == name }
                            }
                        },
                        onAdd = { name, url, type ->
                            scope.launch {
                                database.ruleListMetaDao().upsert(
                                    RuleListMetaEntity(
                                        name = name,
                                        url = url,
                                        enabled = true,
                                        type = type
                                    )
                                )
                                val metas = database.ruleListMetaDao().getAll()
                                ruleListMetas.clear()
                                ruleListMetas.addAll(metas)
                            }
                        },
                        onSyncNow = {
                            RuleSyncWorker.syncNow(this@MainActivity)
                            Toast.makeText(
                                this@MainActivity,
                                "同步已开始",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                if (showAccessibilityRuleManageDialog) {
                    AccessibilityRuleManageDialog(
                        rules = accessibilityRules.toList(),
                        onDismiss = { showAccessibilityRuleManageDialog = false },
                        onToggle = { id, enabled ->
                            scope.launch {
                                database.accessibilityRuleDao().setEnabled(id, enabled)
                                val index = accessibilityRules.indexOfFirst { it.id == id }
                                if (index >= 0) {
                                    accessibilityRules[index] = accessibilityRules[index].copy(enabled = enabled)
                                }
                            }
                        },
                        onDelete = { id ->
                            scope.launch {
                                database.accessibilityRuleDao().deleteById(id)
                                accessibilityRules.removeAll { it.id == id }
                            }
                        }
                    )
                }

                if (showRecordingDialog) {
                    var interactions by remember { mutableStateOf(listOf<CapturedInteraction>()) }
                    var recordingState by remember { mutableStateOf(false) }

                    LaunchedEffect(showRecordingDialog) {
                        while (showRecordingDialog) {
                            interactions = AdSkipperService.recordedInteractions.toList()
                            recordingState = AdSkipperService.isRecording
                            delay(500)
                        }
                    }

                    RecordingDialog(
                        interactions = interactions,
                        isRecording = recordingState,
                        onStartRecording = {
                            AdSkipperService.startRecording()
                        },
                        onStopRecording = {
                            AdSkipperService.stopRecording()
                        },
                        onDismiss = {
                            if (AdSkipperService.isRecording) {
                                AdSkipperService.stopRecording()
                            }
                            showRecordingDialog = false
                        },
                        onSaveRule = { json, packageName ->
                            scope.launch {
                                database.accessibilityRuleDao().insert(
                                    AccessibilityRuleEntity(
                                        packageName = packageName,
                                        ruleJson = json,
                                        enabled = true,
                                        source = "manual"
                                    )
                                )
                                Toast.makeText(
                                    this@MainActivity,
                                    "规则已保存",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            }
        }
    }

    private suspend fun loadInstalledApps(
        database: AppDatabase,
        apps: MutableList<AppInfo>
    ) {
        withContext(Dispatchers.IO) {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val prefs = database.appPreferenceDao().getAll()
            val prefMap = prefs.associate { it.packageName to it.bypassVpn }

            val appInfos = resolveInfos.mapNotNull { ri ->
                try {
                    val pkgName = ri.activityInfo.packageName
                    val appName = ri.loadLabel(pm).toString()
                    val icon = ri.loadIcon(pm)
                    AppInfo(pkgName, appName, icon, prefMap[pkgName] ?: false)
                } catch (e: Exception) { null }
            }.sortedBy { it.appName }

            for (info in appInfos) {
                database.appPreferenceDao().upsert(
                    AppPreferenceEntity(info.packageName, info.appName, info.bypassVpn)
                )
            }

            withContext(Dispatchers.Main) {
                apps.clear()
                apps.addAll(appInfos)
            }
        }
    }

    private fun startVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, REQUEST_VPN)
            return
        }
        doStartVpn()
    }

    private fun doStartVpn() {
        val intent = Intent(this, AdBlockVpnService::class.java)
        intent.action = AdBlockVpnService.ACTION_START
        startForegroundService(intent)
        isVpnActive = true
    }

    private fun stopVpn() {
        val intent = Intent(this, AdBlockVpnService::class.java)
        intent.action = AdBlockVpnService.ACTION_STOP
        startService(intent)
        isVpnActive = false
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VPN) {
            if (resultCode == RESULT_OK) doStartVpn()
            else Toast.makeText(this, "VPN 权限被拒绝", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQUEST_VPN = 100
    }
}
