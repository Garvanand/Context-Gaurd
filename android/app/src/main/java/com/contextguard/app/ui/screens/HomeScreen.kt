package com.contextguard.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.R
import com.contextguard.app.core.accessibility.AllowlistManager
import com.contextguard.app.core.accessibility.ScreenGuardStateManager
import com.contextguard.app.core.health.ModelHealthManager
import com.contextguard.app.core.notification.NotificationGuardStateManager
import com.contextguard.app.core.privacy.NetworkMode
import com.contextguard.app.theme.ActLime
import com.contextguard.app.theme.ContourBorder
import com.contextguard.app.theme.ContourBorderActive
import com.contextguard.app.theme.DeepSurface
import com.contextguard.app.theme.DisplayFont
import com.contextguard.app.theme.ElectricViolet
import com.contextguard.app.theme.ElevatedSurface
import com.contextguard.app.theme.Ink
import com.contextguard.app.theme.IonCyan
import com.contextguard.app.theme.Midnight
import com.contextguard.app.theme.MutedText
import com.contextguard.app.theme.SignalLime
import com.contextguard.app.theme.SoftWhite
import com.contextguard.app.theme.SubtleText
import com.contextguard.app.theme.TechnicalMono
import com.contextguard.app.theme.WarnOrange
import com.contextguard.app.ui.components.ActivityTimeline
import com.contextguard.app.ui.components.CapabilityStatus
import com.contextguard.app.ui.components.ContextFieldMode
import com.contextguard.app.ui.components.EventOrigin
import com.contextguard.app.ui.components.HomeNavTab
import com.contextguard.app.ui.components.ProtectionCapabilityConnector
import com.contextguard.app.ui.components.SpectralBottomNavigation
import com.contextguard.app.ui.components.TactilePrimaryButton
import com.contextguard.app.ui.components.TimelineEvent
import com.contextguard.app.ui.viewmodel.InterventionType
import com.contextguard.app.ui.viewmodel.MainViewModel

/**
 * Editorial, high-assurance Android Home Screen.
 * Implements the Spectral Signal design system with authentic runtime system telemetry.
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToAnalyze: () -> Unit,
    onNavigateToDemo: () -> Unit,
    onNavigateToSupervisor: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToConsent: () -> Unit = {}
) {
    val context = LocalContext.current
    val appState by viewModel.appState.collectAsState()

    // ------------------------------------------------------------------------
    // Live Service, Telemetry, and Permission States
    // ------------------------------------------------------------------------
    val isScreenConnected by ScreenGuardStateManager.isServiceConnected.collectAsState()
    val isScreenPaused by ScreenGuardStateManager.isMonitoringPaused.collectAsState()
    val screenAuditLogs by ScreenGuardStateManager.auditLogs.collectAsState()

    val isNotificationConnected by NotificationGuardStateManager.isListenerConnected.collectAsState()
    val isNotificationPaused by NotificationGuardStateManager.isProtectionPaused.collectAsState()
    val notificationAuditLogs by NotificationGuardStateManager.recentAuditRecords.collectAsState()

    var isScreenEnabledInSettings by remember { mutableStateOf(ScreenGuardStateManager.isAccessibilityEnabledInSettings(context)) }
    var isNotificationEnabledInSettings by remember { mutableStateOf(NotificationGuardStateManager.isNotificationAccessGranted(context)) }
    val targetApps by AllowlistManager.targetApps.collectAsState()

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isScreenEnabledInSettings = ScreenGuardStateManager.isAccessibilityEnabledInSettings(context)
                isNotificationEnabledInSettings = NotificationGuardStateManager.isNotificationAccessGranted(context)
                AllowlistManager.refreshInstalledApps(context)
                viewModel.refreshHealthState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val healthState = appState.healthState
    val isModelReady = healthState.urlModelStatus.isReady

    // ------------------------------------------------------------------------
    // State Derivation: Understated, Authentic System Status
    // ------------------------------------------------------------------------
    val isProtectionActive = (isScreenConnected && !isScreenPaused) || (isNotificationConnected && !isNotificationPaused)
    val isProtectionPaused = (isScreenConnected && isScreenPaused) || (isNotificationConnected && isNotificationPaused)
    val isSetupRequired = !isScreenEnabledInSettings && !isNotificationEnabledInSettings

    val systemStateLabel = when {
        isProtectionActive -> "Protection active"
        isProtectionPaused -> "Protection paused"
        (isScreenEnabledInSettings || isNotificationEnabledInSettings) && !isScreenConnected && !isNotificationConnected -> "Service unavailable"
        isSetupRequired -> "Setup required"
        else -> "Service unavailable"
    }

    val systemStateColor = when {
        isProtectionActive -> SignalLime
        isProtectionPaused -> WarnOrange
        isSetupRequired -> IonCyan
        else -> ElectricViolet
    }

    val fieldMode = when {
        isProtectionActive -> ContextFieldMode.PROTECTION_ACTIVE
        else -> ContextFieldMode.IDLE
    }

    // ------------------------------------------------------------------------
    // Unified Activity Ledger
    // Combines real screen audits, real notification audits, and user checks.
    // ------------------------------------------------------------------------
    val timelineEvents = remember(screenAuditLogs, notificationAuditLogs, appState.lastResult) {
        val list = mutableListOf<TimelineEvent>()

        // Real screen monitoring events
        screenAuditLogs.forEach { log ->
            val intervention = try {
                InterventionType.valueOf(log.intervention)
            } catch (e: Exception) {
                InterventionType.ACT
            }
            list.add(
                TimelineEvent(
                    id = log.id,
                    sourceApp = formatPackageName(log.packageName),
                    category = "Screen Action",
                    origin = EventOrigin.REAL_MONITORING,
                    intervention = intervention,
                    riskScore = log.riskScore,
                    timestampMs = log.timestampMs,
                    evidenceSummary = log.evidenceSummary,
                    candidateAction = log.candidateAction,
                    latencyMs = log.latencyMs
                )
            )
        }

        // Real notification monitoring events
        notificationAuditLogs.forEach { record ->
            val intervention = try {
                InterventionType.valueOf(record.intervention)
            } catch (e: Exception) {
                InterventionType.ACT
            }
            list.add(
                TimelineEvent(
                    id = record.id,
                    sourceApp = formatPackageName(record.sourcePackage),
                    category = "Notification Triage",
                    origin = EventOrigin.NOTIFICATION,
                    intervention = intervention,
                    riskScore = record.riskScore,
                    timestampMs = record.timestampMs,
                    evidenceSummary = record.evidenceSummary,
                    candidateAction = record.userAction,
                    latencyMs = null
                )
            )
        }

        // User manual check (if any recent)
        appState.lastResult?.let { res ->
            list.add(
                TimelineEvent(
                    id = "manual-${res.hashSha256}",
                    sourceApp = res.artifactTitle,
                    category = "Manual Scan",
                    origin = EventOrigin.USER_CHECK,
                    intervention = res.intervention,
                    riskScore = res.riskScore,
                    timestampMs = System.currentTimeMillis() - res.latencyMs,
                    evidenceSummary = res.rationale,
                    candidateAction = res.intendedAction,
                    latencyMs = res.latencyMs
                )
            )
        }

        list.sortedByDescending { it.timestampMs }
    }

    // ------------------------------------------------------------------------
    // Bottom Navigation State
    // ------------------------------------------------------------------------
    var currentTab by remember { mutableStateOf(HomeNavTab.PROTECT) }
    val scrollState = rememberScrollState()

    Scaffold(
        bottomBar = {
            SpectralBottomNavigation(
                selectedTab = currentTab,
                onTabSelected = { tab ->
                    when (tab) {
                        HomeNavTab.PROTECT -> currentTab = HomeNavTab.PROTECT
                        HomeNavTab.ACTIVITY -> currentTab = HomeNavTab.ACTIVITY
                        HomeNavTab.PRIVACY -> onNavigateToPrivacy()
                        HomeNavTab.SETTINGS -> onNavigateToSettings()
                    }
                }
            )
        },
        containerColor = Ink
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ================================================================
            // 1. TOP APP BAR / BRAND HEADER
            // ================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DeepSurface)
                            .border(1.dp, ContourBorderActive, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_aperture_signal_symbol),
                            contentDescription = "ContextGuard Aperture Signal",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ContextGuard",
                                color = SoftWhite,
                                fontSize = 18.sp,
                                fontFamily = DisplayFont,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            )
                            Box(
                                modifier = Modifier
                                    .background(ElectricViolet.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(1.dp, ElectricViolet.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "v2.1",
                                    color = ElectricViolet,
                                    fontSize = 9.sp,
                                    fontFamily = TechnicalMono,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Text(
                            text = "PRE-ACTION AI SAFETY",
                            color = SubtleText,
                            fontSize = 9.5.sp,
                            fontFamily = TechnicalMono,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onNavigateToSupervisor) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Supervisor Room",
                            tint = IonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Settings",
                            tint = MutedText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ================================================================
            // VIEW CONTENT SWITCHER: PROTECT TAB vs FULL ACTIVITY TAB
            // ================================================================
            if (currentTab == HomeNavTab.PROTECT) {
                // ------------------------------------------------------------
                // 2. HERO COMPOSITION
                // ------------------------------------------------------------
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Precise, understated state label derived genuinely from runtime
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(DeepSurface)
                            .border(1.dp, ContourBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(systemStateColor)
                        )
                        Text(
                            text = systemStateLabel,
                            color = SoftWhite,
                            fontSize = 11.5.sp,
                            fontFamily = TechnicalMono,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        )
                    }

                    // Editorial Hero Headline
                    Text(
                        text = "Your digital world.\nWith more clarity.",
                        color = SoftWhite,
                        fontSize = 28.sp,
                        fontFamily = DisplayFont,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp,
                        letterSpacing = (-0.5).sp
                    )

                    // Supporting Copy
                    Text(
                        text = "ContextGuard helps you notice meaningful risks before you act.",
                        color = MutedText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                // ------------------------------------------------------------
                // 3. ACTIVE PROTECTION VISUALIZATION (Context Field + Satellite Capabilities)
                // ------------------------------------------------------------
                ProtectionCapabilityConnector(
                    fieldMode = fieldMode,
                    screenCapability = CapabilityStatus(
                        title = "Screen Guard",
                        detail = when {
                            isScreenConnected && !isScreenPaused -> "Real-time window tree"
                            isScreenConnected && isScreenPaused -> "Monitoring paused"
                            else -> "Accessibility required"
                        },
                        isActive = isScreenConnected,
                        isPaused = isScreenPaused,
                        icon = Icons.Default.Visibility,
                        onClick = {
                            if (!isScreenConnected) onNavigateToConsent()
                            else ScreenGuardStateManager.toggleMonitoring()
                        }
                    ),
                    notificationCapability = CapabilityStatus(
                        title = "Notification Guard",
                        detail = when {
                            isNotificationConnected && !isNotificationPaused -> "Threat triage active"
                            isNotificationConnected && isNotificationPaused -> "Triage paused"
                            else -> "Listener ungranted"
                        },
                        isActive = isNotificationConnected,
                        isPaused = isNotificationPaused,
                        icon = Icons.Default.Notifications,
                        onClick = {
                            if (!isNotificationConnected) onNavigateToConsent()
                            else NotificationGuardStateManager.toggleProtection()
                        }
                    ),
                    localModelCapability = CapabilityStatus(
                        title = "On-Device Inference",
                        detail = if (isModelReady) "120 Trees | Zero egress" else "Perception heuristics",
                        isActive = isModelReady,
                        isPaused = false,
                        icon = Icons.Default.Memory,
                        onClick = onNavigateToSupervisor
                    )
                )

                // ------------------------------------------------------------
                // 3b. HONEST SUBSYSTEM DISCLOSURES (Protected Apps & Model Status)
                // ------------------------------------------------------------
                val enabledApps = targetApps.filter { it.isEnabled }
                val enabledAppNames = enabledApps.take(4).joinToString(", ") { it.appName } +
                    if (enabledApps.size > 4) " +${enabledApps.size - 4} more" else ""

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, ContourBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DeepSurface.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MONITORED APPLICATIONS",
                                color = SoftWhite,
                                fontSize = 11.sp,
                                fontFamily = TechnicalMono,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "${enabledApps.size} enabled",
                                color = if (enabledApps.isNotEmpty()) ActLime else WarnOrange,
                                fontSize = 11.sp,
                                fontFamily = TechnicalMono,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = if (enabledApps.isNotEmpty()) enabledAppNames else "No applications selected for background observation",
                            color = MutedText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        HorizontalDivider(color = ContourBorder.copy(alpha = 0.4f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "NETWORK / MODEL STATUS",
                                    color = SubtleText,
                                    fontSize = 9.5.sp,
                                    fontFamily = TechnicalMono,
                                    letterSpacing = 0.6.sp
                                )
                                Text(
                                    text = if (appState.backendConfig.isOffline) "Offline Local (Zero egress)" else "Backend: ${appState.backendConfig.host}:${appState.backendConfig.port}",
                                    color = SoftWhite,
                                    fontSize = 11.5.sp,
                                    fontFamily = TechnicalMono
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(if (isModelReady) SignalLime.copy(alpha = 0.15f) else WarnOrange.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .border(1.dp, if (isModelReady) SignalLime.copy(alpha = 0.4f) else WarnOrange.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isModelReady) "MODELS LOADED" else "HEURISTICS ONLY",
                                    color = if (isModelReady) SignalLime else WarnOrange,
                                    fontSize = 9.sp,
                                    fontFamily = TechnicalMono,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // ------------------------------------------------------------
                // 4. PRIMARY INTERACTIONS
                // Tactile press compression + violet-to-cyan light shift
                // ------------------------------------------------------------
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Action 1: Check something
                    TactilePrimaryButton(
                        title = "Check something",
                        subtitle = "Direct pre-action analysis for link, message, image, or doc",
                        icon = Icons.Default.Search,
                        isPrimary = true,
                        onClick = onNavigateToAnalyze
                    )

                    // Action 2: Configure protection
                    TactilePrimaryButton(
                        title = when {
                            isSetupRequired -> "Configure protection"
                            isProtectionPaused -> "Resume protection"
                            else -> "Configure protection"
                        },
                        subtitle = when {
                            isSetupRequired -> "Grant screen & notification permissions to activate monitoring"
                            isProtectionPaused -> "Kill-switch engaged (tap to resume background monitoring)"
                            else -> "Manage allowlisted apps and sensitivity thresholds"
                        },
                        icon = when {
                            isProtectionPaused -> Icons.Default.PlayCircle
                            isSetupRequired -> Icons.Default.Shield
                            else -> Icons.Default.Settings
                        },
                        isPrimary = false,
                        onClick = {
                            when {
                                isSetupRequired -> onNavigateToConsent()
                                isProtectionPaused -> {
                                    ScreenGuardStateManager.resumeMonitoring()
                                    NotificationGuardStateManager.resumeProtection()
                                }
                                else -> onNavigateToSettings()
                            }
                        }
                    )
                }

                // ------------------------------------------------------------
                // 5. RECENT ACTIVITY PREVIEW
                // Compact timeline rows with expandable summaries
                // ------------------------------------------------------------
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT ACTIVITY",
                            color = SoftWhite,
                            fontSize = 12.sp,
                            fontFamily = TechnicalMono,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )

                        if (timelineEvents.isNotEmpty()) {
                            TextButton(
                                onClick = { currentTab = HomeNavTab.ACTIVITY },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "View all (${timelineEvents.size})",
                                        color = IonCyan,
                                        fontSize = 11.sp,
                                        fontFamily = TechnicalMono
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "All",
                                        tint = IonCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    ActivityTimeline(
                        events = timelineEvents.take(3),
                        isSetupRequired = isSetupRequired,
                        isModelReady = isModelReady,
                        onSetupClicked = onNavigateToConsent
                    )
                }

                // ------------------------------------------------------------
                // 6. ACTION-CONDITIONED RESEARCH PROOF BANNER
                // ------------------------------------------------------------
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onNavigateToDemo)
                        .border(1.dp, ContourBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DeepSurface.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElectricViolet.copy(alpha = 0.15f))
                                .border(1.dp, ElectricViolet.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "Proof",
                                tint = ElectricViolet,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Action Policy Proof",
                                color = SoftWhite,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Compare Save Privately vs Post Publicly on identical bank statements",
                                color = MutedText,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Demo",
                            tint = SubtleText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

            } else {
                // ============================================================
                // FULL ACTIVITY TIMELINE TAB
                // ============================================================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Activity Ledger",
                                color = SoftWhite,
                                fontSize = 22.sp,
                                fontFamily = DisplayFont,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ephemeral pre-action timeline (Zero cloud egress)",
                                color = MutedText,
                                fontSize = 12.sp
                            )
                        }

                        if (timelineEvents.isNotEmpty()) {
                            IconButton(onClick = {
                                ScreenGuardStateManager.clearAuditLog()
                                NotificationGuardStateManager.clearAuditLog()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear Ephemeral Log",
                                    tint = MutedText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    ActivityTimeline(
                        events = timelineEvents,
                        isSetupRequired = isSetupRequired,
                        isModelReady = isModelReady,
                        onSetupClicked = onNavigateToConsent
                    )
                }
            }
        }
    }
}

/**
 * Clean format for Android package names into recognizable application labels.
 */
private fun formatPackageName(pkg: String): String {
    return when (pkg) {
        "com.whatsapp" -> "WhatsApp"
        "org.telegram.messenger" -> "Telegram"
        "com.google.android.apps.messaging" -> "Google Messages"
        "org.thoughtcrime.securesms" -> "Signal"
        "com.android.chrome" -> "Chrome Browser"
        "org.mozilla.firefox" -> "Firefox"
        "com.sec.android.app.sbrowser" -> "Samsung Browser"
        "com.google.android.gm" -> "Gmail"
        "com.twitter.android" -> "X / Twitter"
        "com.reddit.frontpage" -> "Reddit"
        else -> {
            if (pkg.contains(".")) {
                pkg.substringAfterLast(".").replaceFirstChar { it.uppercase() }
            } else {
                pkg
            }
        }
    }
}
