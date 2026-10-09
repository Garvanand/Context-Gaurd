package com.contextguard.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.core.accessibility.AllowlistManager
import com.contextguard.app.core.accessibility.ScreenGuardStateManager
import com.contextguard.app.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isConnected by ScreenGuardStateManager.isServiceConnected.collectAsState()
    val isPaused by ScreenGuardStateManager.isMonitoringPaused.collectAsState()
    val auditLogs by ScreenGuardStateManager.auditLogs.collectAsState()
    val targetApps by AllowlistManager.targetApps.collectAsState()

    var hasAffirmativeConsent by remember { mutableStateOf(false) }
    var showEraseSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AllowlistManager.refreshInstalledApps(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Column {
                Text(
                    text = "Screen Protection & Consent",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Opt-in on-device pre-action monitoring",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            }
        }

        // PERSISTENT LIVE MONITORING STATUS CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (isConnected && !isPaused) ActGreen.copy(alpha = 0.5f) else SurfaceBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusColor = when {
                        isConnected && !isPaused -> ActGreen
                        isConnected && isPaused -> WarnOrange
                        else -> StopRed
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = when {
                                isConnected && !isPaused -> "PROTECTION ACTIVE"
                                isConnected && isPaused -> "PROTECTION PAUSED"
                                else -> "SERVICE NOT BOUND"
                            },
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    if (isConnected) {
                        Button(
                            onClick = { ScreenGuardStateManager.toggleMonitoring() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPaused) ActGreen else WarnOrange
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isPaused) "Resume" else "Pause Kill-Switch",
                                color = BackgroundDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = when {
                        isConnected && !isPaused -> "Actively monitoring allowlisted applications for pre-action hazards."
                        isConnected && isPaused -> "Protection temporarily paused by user. Zero events are being processed."
                        else -> "Accessibility service is disabled in Android settings. Complete the consent flow below to enable."
                    },
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // STATUTORY PURPOSE DISCLAIMER (NON-ASSISTIVE DISCLOSURE)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CyanAccent.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Notice",
                    tint = CyanAccent,
                    modifier = Modifier.size(22.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Statutory Transparency Notice",
                        color = CyanAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ContextGuard is an AI-driven digital safety monitor, NOT an assistive technology for users with disabilities. It requests Android Accessibility APIs strictly to inspect visible screen context (such as phishing URLs and data leaks) before irreversible actions are taken.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // THE 7 TRANSPARENCY CRITERIA CARDS
        Text(
            text = "HOW SCREEN PROTECTION WORKS",
            color = CyanAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        ConsentPointItem(
            icon = Icons.Default.FilterList,
            title = "1. Which screens can be inspected?",
            description = "ContextGuard ONLY inspects third-party applications you explicitly enable in the allowlist below (e.g. Chrome, WhatsApp). All unselected apps are completely ignored by the system."
        )

        ConsentPointItem(
            icon = Icons.Default.Visibility,
            title = "2. What elements are examined?",
            description = "We examine visible web address bar URLs, uncommitted text drafts in allowlisted messaging apps, and high-consequence action buttons (e.g. 'Send', 'Pay', 'Login')."
        )

        ConsentPointItem(
            icon = Icons.Default.Security,
            title = "3. Why is this necessary?",
            description = "Attackers exploit the moment of action. Real-time pre-action inspection detects phishing links, credential harvesting, and inadvertent PII disclosures before the user commits."
        )

        ConsentPointItem(
            icon = Icons.Default.Lock,
            title = "4. Does anything leave your device?",
            description = "ZERO BYTES LEAVE YOUR PHONE. All screen context evaluation is executed 100% locally in on-device volatile memory. No cloud servers, no third-party APIs, and no telemetry tracking."
        )

        ConsentPointItem(
            icon = Icons.Default.Block,
            title = "5. What CANNOT be inspected?",
            description = "Password fields are strictly skipped (isPassword controls are never read). Banking apps protected by Android FLAG_SECURE, device lock screens, and custom games cannot be read."
        )

        ConsentPointItem(
            icon = Icons.Default.PowerSettingsNew,
            title = "6. How to enable and disable access?",
            description = "You can immediately pause protection anytime using the Kill-Switch button in the app, or revoke the Android Accessibility permission completely in System Settings."
        )

        ConsentPointItem(
            icon = Icons.Default.DeleteOutline,
            title = "7. Reviewing and erasing event metadata",
            description = "ContextGuard retains only an ephemeral in-memory ledger of action decisions (no raw messages or passwords). You can review or erase all records in one tap below."
        )

        // AFFIRMATIVE CONSENT & ACTIVATION BUTTON
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "AFFIRMATIVE USER CONSENT",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { hasAffirmativeConsent = !hasAffirmativeConsent },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(
                        checked = hasAffirmativeConsent,
                        onCheckedChange = { hasAffirmativeConsent = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CyanAccent,
                            uncheckedColor = TextSecondary
                        )
                    )
                    Text(
                        text = "I have read and consent to on-device pre-action screen analysis for selected apps.",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    enabled = hasAffirmativeConsent,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = BackgroundDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Accessibility Settings",
                        color = BackgroundDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // APP ALLOWLIST SECTION
        Text(
            text = "APPLICATION ALLOWLIST",
            color = CyanAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                targetApps.forEach { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = app.appName,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (!app.isInstalled) {
                                    Text(
                                        text = "(Not Installed)",
                                        color = TextTertiary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Text(
                                text = "${app.category.name.lowercase().capitalize()} • ${app.packageName}",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = app.isEnabled,
                            onCheckedChange = { AllowlistManager.toggleApp(app.packageName, it) },
                            enabled = app.isInstalled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundDark,
                                checkedTrackColor = CyanAccent,
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = SurfaceBorder
                            )
                        )
                    }
                    Divider(color = SurfaceBorderSubtle, thickness = 0.5.dp)
                }
            }
        }

        // EPHEMERAL AUDIT LEDGER & ERASE DATA SECTION
        Text(
            text = "EPHEMERAL EVENT AUDIT LEDGER",
            color = CyanAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recorded Events: ${auditLogs.size}",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(
                        onClick = {
                            ScreenGuardStateManager.clearAuditLog()
                            showEraseSuccess = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StopRed),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(StopRed)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Erase All Logs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (showEraseSuccess) {
                    Text(
                        text = "All ephemeral in-memory event metadata erased successfully.",
                        color = ActGreen,
                        fontSize = 12.sp
                    )
                }

                if (auditLogs.isEmpty()) {
                    Text(
                        text = "No events recorded yet. In-memory log is completely clean.",
                        color = TextTertiary,
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                } else {
                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    auditLogs.take(5).forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${entry.packageName} (${entry.candidateAction})",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${sdf.format(Date(entry.timestampMs))} • ${entry.evidenceSummary}",
                                    color = TextTertiary,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = entry.intervention,
                                color = when (entry.intervention) {
                                    "STOP" -> StopRed
                                    "WARN" -> WarnOrange
                                    "ASK" -> CyanAccent
                                    else -> ActGreen
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConsentPointItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorderSubtle, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CyanAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
