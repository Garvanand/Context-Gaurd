package com.contextguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.*
import com.contextguard.app.ui.components.EvidenceCard
import com.contextguard.app.ui.components.InterventionBadge
import com.contextguard.app.ui.viewmodel.InterventionType
import com.contextguard.app.ui.viewmodel.MainViewModel

@Composable
fun ResultScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.appState.collectAsState()
    val result = state.lastResult
    val scrollState = rememberScrollState()

    var showOverrideWarning by remember { mutableStateOf(false) }

    if (showOverrideWarning && result != null) {
        AlertDialog(
            onDismissRequest = { showOverrideWarning = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = StopRed
                    )
                    Text(text = "Engage Intentional User Override?", color = StopRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "ContextGuard identified this action as irreversible catastrophic exposure (rho = ${String.format("%.2f", result.riskScore)} >= 0.65).\n\nOverriding will bypass safety protections and proceed with '${result.intendedAction}' to '${result.destination}'.\n\nDo you confirm this action?",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverrideWarning = false
                        viewModel.overrideStopIntervention()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StopRed)
                ) {
                    Text(text = "Confirm & Override", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverrideWarning = false }) {
                    Text(text = "Cancel", color = TextPrimary)
                }
            },
            containerColor = SurfaceDark
        )
    }

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
                text = "Intervention Decision",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (result == null) {
            Text(
                text = "No evaluation result available.",
                color = TextSecondary,
                fontSize = 14.sp
            )
            return
        }

        // Active Override Banner
        if (state.isOverrideEngaged) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StopRed, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Override", tint = StopRed)
                    Text(
                        text = "Deliberate User Override Active: Safety intervention bypassed by user consent.",
                        color = StopRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Intervention Badge (Large)
        InterventionBadge(
            intervention = result.intervention,
            large = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Mathematical Policy Trace Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "DETERMINISTIC POLICY TRACE",
                color = CyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(title = "Severity (s)", value = "${result.severity}")
                MetricColumn(title = "Irreversibility (r)", value = "${result.irreversibility}")
                MetricColumn(title = "Risk (rho)", value = String.format("%.3f", result.riskScore), isAccent = true)
                MetricColumn(title = "Confidence", value = "${(result.confidence * 100).toInt()}%")
            }

            HorizontalDivider(color = DividerColor)

            Text(
                text = "Formula: rho = ${result.severity} * (1 + ${result.lambda} * ${result.irreversibility}) = ${String.format("%.3f", result.riskScore)}",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Rationale Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "DECISION RATIONALE",
                color = CyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = result.rationale,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        // Evidence Card
        EvidenceCard(evidenceItems = result.evidence)

        // Bottom User Actions
        when (result.intervention) {
            InterventionType.STOP -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(text = "Cancel Action (Recommended)", color = TextPrimary)
                    }

                    if (result.canOverride) {
                        OutlinedButton(
                            onClick = { showOverrideWarning = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StopRed),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(text = "Emergency User Override (Proceed)", fontSize = 13.sp)
                        }
                    }
                }
            }
            InterventionType.WARN, InterventionType.ASK -> {
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarnOrange),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = "Acknowledge & Confirm Action", color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            }
            InterventionType.ACT -> {
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ActGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = "Proceed Safely", color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(
    title: String,
    value: String,
    isAccent: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            color = TextTertiary,
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = if (isAccent) CyanAccent else TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
