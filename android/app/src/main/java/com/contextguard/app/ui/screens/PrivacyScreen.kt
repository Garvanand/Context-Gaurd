package com.contextguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.core.privacy.NetworkAuditLogger
import com.contextguard.app.core.privacy.NetworkMode
import com.contextguard.app.core.privacy.RedactionStyle
import com.contextguard.app.theme.*
import com.contextguard.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrivacyScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.appState.collectAsState()
    val scrollState = rememberScrollState()
    val auditEntries = remember { NetworkAuditLogger.getEntries() }
    val telemetrySummary = remember { NetworkAuditLogger.getTelemetrySummary() }

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
            Column {
                Text(
                    text = "Privacy Center",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Zero Raw Persistence Invariant",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Prominent Privacy Center Motto Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ActGreen.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Shield",
                        tint = ActGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "ON-DEVICE FIRST PRINCIPLE",
                        color = ActGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "“Your artifact stays on this device until ContextGuard decides cloud/local-backend reasoning is necessary.”",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp
                )

                Text(
                    text = "All perceptual OCR, face contour scanning, and sensitive-region masking execute in volatile RAM before network transmission.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Live Telemetry Overview Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Detections & Redaction
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ON-DEVICE DETECTIONS",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.maskedPiiCount} PII | ${state.detectedFacesCount} Face(s)",
                        color = CyanAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.lastRedactionResult?.regionsRedacted?.size ?: state.maskedPiiCount} regions masked",
                        color = ActGreen,
                        fontSize = 11.sp
                    )
                }
            }

            // Card 2: Network Footprint
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "TRANSMISSION STATE",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val isOffline = state.backendConfig.networkMode == NetworkMode.OFFLINE
                    Text(
                        text = if (isOffline) "0 Bytes (Offline)" else "${state.lastAuditEntry?.payloadSizeBytes ?: 0} Bytes",
                        color = if (isOffline) ActGreen else WarnOrange,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isOffline) "Airplane-Safe Active" else "Redacted Payload Only",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Active Session Context Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "CURRENT SESSION CONTEXT",
                color = CyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            ContextField("Destination", state.selectedDestination)
            ContextField("Intended Action", state.selectedAction)
            ContextField("Payload Type", state.lastAuditEntry?.payloadType ?: "REDACTED_IMAGE_AND_METADATA")
            ContextField("Payload Size", "${state.lastAuditEntry?.payloadSizeBytes ?: 0} bytes")
            ContextField("Last Event", state.lastAuditEntry?.timestamp?.let { formatTime(it) } ?: "Session Initialized")
            ContextField("Artifact Fingerprint", state.lastAuditEntry?.artifactHashSha256?.take(16) ?: "e3b0c44298fc1c14...")
        }

        // Network Posture Mode Selector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "ENFORCE NETWORK MODE",
                color = CyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            NetworkMode.values().forEach { mode ->
                val isSelected = state.backendConfig.networkMode == mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) SurfaceGlass else SurfaceDark,
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) CyanAccent else SurfaceBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.setNetworkMode(mode) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { viewModel.setNetworkMode(mode) },
                        colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
                    )
                    Column {
                        Text(
                            text = mode.name,
                            color = if (isSelected) CyanAccent else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when (mode) {
                                NetworkMode.OFFLINE -> "Air-gapped. Zero network calls from application layer."
                                NetworkMode.LOCAL_BACKEND -> "Transmits ONLY redacted bitmaps and masked tokens."
                                NetworkMode.RESTRICTED_EVALUATION -> "Restricted academic benchmarking with consented inputs."
                            },
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Visual Redaction Style Selector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "REDACTION STYLE",
                color = CyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RedactionStyle.values().forEach { style ->
                    val isSelected = state.redactionStyle == style
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setRedactionStyle(style) }
                            .border(
                                1.5.dp,
                                if (isSelected) CyanAccent else SurfaceBorder,
                                RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) SurfaceGlass else SurfaceDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = style.name,
                                color = if (isSelected) CyanAccent else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (style == RedactionStyle.BLACKOUT) "Solid Masking" else "Pixelated Blur",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Live Network Audit Log Entries
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NETWORK AUDIT TRAIL (METADATA ONLY)",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${auditEntries.size} events",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            if (auditEntries.isEmpty()) {
                Text(
                    text = "No outbound network requests recorded. Device is operating in local/idle mode.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                auditEntries.take(5).forEach { entry ->
                    AuditEntryItem(entry)
                }
            }
        }
    }
}

@Composable
private fun ContextField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun AuditEntryItem(entry: com.contextguard.app.core.privacy.NetworkAuditEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark, RoundedCornerShape(10.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = entry.endpointCategory,
                color = CyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${entry.latencyMs}ms | ${entry.payloadSizeBytes}B",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SHA-256: ${entry.artifactHashSha256.take(16)}...",
                color = TextTertiary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = if (entry.isRedacted) "REDACTED" else "UNREDACTED",
                color = if (entry.isRedacted) ActGreen else StopRed,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatTime(epochMs: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(epochMs))
}
