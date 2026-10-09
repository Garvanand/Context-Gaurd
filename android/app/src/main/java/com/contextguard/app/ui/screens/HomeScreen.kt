package com.contextguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.core.privacy.NetworkMode
import com.contextguard.app.theme.*
import com.contextguard.app.ui.viewmodel.MainViewModel

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
    val state by viewModel.appState.collectAsState()
    val scrollState = rememberScrollState()

    val isServiceConnected by com.contextguard.app.core.accessibility.ScreenGuardStateManager.isServiceConnected.collectAsState()
    val isMonitoringPaused by com.contextguard.app.core.accessibility.ScreenGuardStateManager.isMonitoringPaused.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyanAccent.copy(alpha = 0.15f))
                        .border(1.5.dp, CyanAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Logo",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "ContextGuard",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Pre-Action Digital Safety",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onNavigateToSupervisor) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Supervisor Mode",
                        tint = CyanAccent
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary
                    )
                }
            }
        }

        // PERSISTENT LIVE MONITORING STATUS BAR
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (isServiceConnected && !isMonitoringPaused) ActGreen.copy(alpha = 0.5f) else SurfaceBorder,
                    RoundedCornerShape(16.dp)
                )
                .clickable { onNavigateToConsent() },
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusColor = when {
                    isServiceConnected && !isMonitoringPaused -> ActGreen
                    isServiceConnected && isMonitoringPaused -> WarnOrange
                    else -> CyanAccent
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Column {
                        Text(
                            text = when {
                                isServiceConnected && !isMonitoringPaused -> "Screen Protection Active"
                                isServiceConnected && isMonitoringPaused -> "Screen Protection Paused"
                                else -> "Screen Protection: Setup Required"
                            },
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when {
                                isServiceConnected && !isMonitoringPaused -> "Monitoring allowlisted apps in real time"
                                isServiceConnected && isMonitoringPaused -> "Kill-switch engaged (tap to resume)"
                                else -> "Tap to review consent & enable service"
                            },
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }

                if (isServiceConnected) {
                    Button(
                        onClick = { com.contextguard.app.core.accessibility.ScreenGuardStateManager.toggleMonitoring() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMonitoringPaused) ActGreen else WarnOrange
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isMonitoringPaused) "Resume" else "Pause",
                            color = BackgroundDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Setup",
                        tint = TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // HERO CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceBorderSubtle, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live Shield Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(ActGreen, CircleShape)
                    )
                    Text(
                        text = "PRE-ACTION DEFENSE ACTIVE",
                        color = ActGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Required Hero Title & Supporting Text
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Before you act, ContextGuard.",
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp
                    )
                    Text(
                        text = "Understand the risk before you send, upload, post, sign, or approve.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                HorizontalDivider(color = DividerColor)

                // The 3 Required CTAs
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 1. Main CTA
                    Button(
                        onClick = onNavigateToAnalyze,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Analyze",
                            tint = BackgroundDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analyze something",
                            color = BackgroundDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 2. Secondary CTA
                    OutlinedButton(
                        onClick = onNavigateToDemo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = AccentGradient),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Demos",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Demo scenarios",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // 3. Tertiary CTA
                    TextButton(
                        onClick = onNavigateToPrivacy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Privacy",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Privacy center",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Live Security Status Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SYSTEM ARCHITECTURE STATE",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "API 34 + ML Kit",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            StatusRow(
                title = "Network Posture",
                value = state.backendConfig.networkMode.name,
                statusColor = if (state.backendConfig.networkMode == NetworkMode.OFFLINE) ActGreen else CyanAccent
            )
            StatusRow(
                title = "Edge Redaction Engine",
                value = if (state.isRedactionEnabled) "Active (${state.redactionStyle.name})" else "Disabled",
                statusColor = if (state.isRedactionEnabled) ActGreen else WarnOrange
            )
            StatusRow(
                title = "Policy Equation",
                value = "rho = s * (1 + 0.75 * r)",
                statusColor = TextPrimary
            )
            StatusRow(
                title = "Latest Artifact Fingerprint",
                value = state.lastResult?.hashSha256?.take(16) ?: "e3b0c44298fc1c14...",
                statusColor = TextSecondary
            )
        }

        // Action Conditioning Proof Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToDemo() }
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(IndigoAccent.copy(alpha = 0.15f))
                        .border(1.dp, IndigoAccent, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = "Research Proof",
                        tint = IndigoAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Viva Demonstration Suite",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "8 presentation-grade scenarios validating action-conditioning",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Go",
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusRow(title: String, value: String, statusColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = TextSecondary, fontSize = 12.sp)
        Text(
            text = value,
            color = statusColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}
