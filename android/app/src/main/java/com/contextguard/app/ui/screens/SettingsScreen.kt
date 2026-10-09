package com.contextguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.core.network.InferenceMode
import com.contextguard.app.theme.*
import com.contextguard.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.appState.collectAsState()
    var hostInput by remember { mutableStateOf(state.backendConfig.host) }
    var portInput by remember { mutableStateOf(state.backendConfig.port.toString()) }
    var selectedMode by remember { mutableStateOf(state.backendConfig.inferenceMode) }
    var showSavedMessage by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Bar
        Row(
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
            Text(
                text = "System Settings",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        val context = androidx.compose.ui.platform.LocalContext.current
        val isNotificationAccessGranted = remember {
            com.contextguard.app.core.notification.NotificationGuardStateManager.isNotificationAccessGranted(context)
        }
        val isListenerConnected by com.contextguard.app.core.notification.NotificationGuardStateManager.isListenerConnected.collectAsState()
        val isProtectionPaused by com.contextguard.app.core.notification.NotificationGuardStateManager.isProtectionPaused.collectAsState()
        val targetApps by com.contextguard.app.core.accessibility.AllowlistManager.targetApps.collectAsState()
        val communicationApps = targetApps.filter {
            it.category == com.contextguard.app.core.accessibility.AppCategory.MESSAGING ||
            it.category == com.contextguard.app.core.accessibility.AppCategory.EMAIL ||
            it.category == com.contextguard.app.core.accessibility.AppCategory.SOCIAL
        }

        // Notification Threat Triage Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NOTIFICATION THREAT TRIAGE",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Surface(
                    color = if (isNotificationAccessGranted && isListenerConnected) ActGreen.copy(alpha = 0.15f) else WarnOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isNotificationAccessGranted && isListenerConnected) "ACTIVE" else if (isNotificationAccessGranted) "ENABLED (CONNECTING)" else "PERMISSION REQUIRED",
                        color = if (isNotificationAccessGranted && isListenerConnected) ActGreen else WarnOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "ContextGuard inspects notifications from selected messaging and email apps in real-time, detecting phishing links, credential harvesting, and impersonation attempts before you tap them.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Surface(
                color = BackgroundDark.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Text(
                    text = "🛡️ Privacy Guarantee: 100% on-device triage. Zero SMS or Contacts permissions requested or needed. Raw notification text is never stored or transmitted.",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp),
                    lineHeight = 16.sp
                )
            }

            // Pause / Resume Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Notification Monitoring",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isProtectionPaused) "Monitoring currently paused" else "Monitoring active for allowlisted apps",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = !isProtectionPaused,
                    onCheckedChange = { com.contextguard.app.core.notification.NotificationGuardStateManager.toggleProtection() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyanAccent,
                        checkedTrackColor = CyanAccent.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceBorder
                    )
                )
            }

            // Open Android Notification Access Settings Button
            Button(
                onClick = {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val generalIntent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                        context.startActivity(generalIntent)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isNotificationAccessGranted) SurfaceBorder else WarnOrange),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isNotificationAccessGranted) "Manage Android Notification Access" else "Enable Notification Access in Settings",
                    color = if (isNotificationAccessGranted) TextPrimary else BackgroundDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Package Selection Section
            Text(
                text = "PERMITTED COMMUNICATION APPS",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            communicationApps.forEach { app ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.appName,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = app.packageName,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Checkbox(
                        checked = app.isEnabled,
                        onCheckedChange = { enabled ->
                            com.contextguard.app.core.accessibility.AllowlistManager.toggleApp(app.packageName, enabled)
                        },
                        colors = CheckboxDefaults.colors(checkedColor = CyanAccent)
                    )
                }
            }
        }

        // Backend Endpoint Configuration Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "LOCAL BACKEND ENDPOINT",
                color = CyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            OutlinedTextField(
                value = hostInput,
                onValueChange = { hostInput = it },
                label = { Text("Host / IP Address") },
                supportingText = { Text("10.0.2.2 for Android emulator host loopback") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            OutlinedTextField(
                value = portInput,
                onValueChange = { portInput = it },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        // Inference Mode Selection
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "INFERENCE OPERATING MODE",
                color = CyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            InferenceMode.values().forEach { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode },
                        colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = "${mode.displayName} (${mode.name})",
                            color = if (mode.isBenchmarkOnly) StopRed else if (selectedMode == mode) CyanAccent else TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when (mode) {
                                InferenceMode.ON_DEVICE -> "Zero outbound bytes. Pure on-device ML Kit perception."
                                InferenceMode.REDACTED_LOCAL_BACKEND -> "ContextGuard standard mode. Masked PII & obscured faces sent to local backend."
                                InferenceMode.RAW_CLOUD_EVALUATION -> "⚠️ Academic benchmark evaluation only. Never permitted in production."
                            },
                            color = if (mode.isBenchmarkOnly) StopRed.copy(alpha = 0.8f) else TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Domain Network Protection (Experimental Extension) Card
        val vpnStatus by com.contextguard.app.core.network.DomainProtectionManager.status.collectAsState()
        val vpnTelemetry by com.contextguard.app.core.network.DomainProtectionManager.telemetry.collectAsState()
        var showVpnDisclosureDialog by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DOMAIN NETWORK PROTECTION",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Surface(
                    color = when (vpnStatus) {
                        com.contextguard.app.core.network.ProtectionStatus.ACTIVE -> ActGreen.copy(alpha = 0.15f)
                        com.contextguard.app.core.network.ProtectionStatus.FALLBACK_INACTIVE -> CyanAccent.copy(alpha = 0.15f)
                        com.contextguard.app.core.network.ProtectionStatus.REVOKED -> WarnOrange.copy(alpha = 0.15f)
                        else -> TextSecondary.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when (vpnStatus) {
                            com.contextguard.app.core.network.ProtectionStatus.ACTIVE -> "ACTIVE"
                            com.contextguard.app.core.network.ProtectionStatus.FALLBACK_INACTIVE -> "DIAGNOSTIC (SAFE)"
                            com.contextguard.app.core.network.ProtectionStatus.REVOKED -> "REVOKED"
                            com.contextguard.app.core.network.ProtectionStatus.STOPPED -> "STOPPED"
                            else -> "OFF"
                        },
                        color = when (vpnStatus) {
                            com.contextguard.app.core.network.ProtectionStatus.ACTIVE -> ActGreen
                            com.contextguard.app.core.network.ProtectionStatus.FALLBACK_INACTIVE -> CyanAccent
                            com.contextguard.app.core.network.ProtectionStatus.REVOKED -> WarnOrange
                            else -> TextSecondary
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Opt-in on-device destination domain evaluation. Only visible hostnames are evaluated; HTTPS web pages, passwords, and encrypted payloads are NEVER decrypted or inspected. Never drops network packets.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            if (vpnStatus == com.contextguard.app.core.network.ProtectionStatus.ACTIVE ||
                vpnStatus == com.contextguard.app.core.network.ProtectionStatus.FALLBACK_INACTIVE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Inspections: ${vpnTelemetry.totalInspections} (${vpnTelemetry.threatsDetected} flagged)",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = {
                            com.contextguard.app.core.network.DomainProtectionManager.stopProtection()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StopRed.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Stop Protection", color = StopRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { showVpnDisclosureDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Enable Domain Protection (Opt-In)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (showVpnDisclosureDialog) {
            AlertDialog(
                onDismissRequest = { showVpnDisclosureDialog = false },
                title = { Text("Network Protection Consent", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "ContextGuard evaluates destination hostnames locally on-device to flag known phishing domains.\n\n" +
                        "• Does NOT decrypt HTTPS traffic\n" +
                        "• Does NOT install CA certificates\n" +
                        "• Does NOT inspect web pages or passwords\n" +
                        "• Does NOT forward data to external servers\n" +
                        "• Completely separate from offline mode\n\n" +
                        "Would you like to enable on-device domain protection?",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showVpnDisclosureDialog = false
                            com.contextguard.app.core.network.DomainProtectionManager.setStatus(
                                com.contextguard.app.core.network.ProtectionStatus.FALLBACK_INACTIVE
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                    ) {
                        Text("Enable Diagnostic Mode", color = BackgroundDark, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVpnDisclosureDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = SurfaceDark,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Save Button

        Button(
            onClick = {
                val parsedPort = portInput.toIntOrNull() ?: 8000
                viewModel.updateBackendConfig(hostInput, parsedPort, selectedMode)
                showSavedMessage = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (showSavedMessage) "Configuration Saved!" else "Save Configuration",
                color = BackgroundDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
