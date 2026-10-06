package com.contextguard.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.*
import com.contextguard.app.ui.components.DecisionTracePipeline
import com.contextguard.app.ui.components.EvidenceCard
import com.contextguard.app.ui.viewmodel.InterventionType
import com.contextguard.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun ResultScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.appState.collectAsState()
    val result = state.lastResult
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var showOverrideWarning by remember { mutableStateOf(false) }

    // Override Confirmation Dialog
    if (showOverrideWarning && result != null) {
        AlertDialog(
            onDismissRequest = { showOverrideWarning = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = StopRed
                    )
                    Text(
                        text = "Engage Intentional Override?",
                        color = StopRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Text(
                    text = "ContextGuard identified this action as irreversible exposure (rho = ${String.format("%.2f", result.riskScore)} >= 0.65).\n\nOverriding will bypass safety protections and proceed with '${result.intendedAction}' to '${result.destination}'.\n\nDo you confirm this action?",
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
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(18.dp)
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Column {
                    Text(
                        text = "Intervention Decision",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Action-conditioned pre-action safety",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            // Latency chip
            if (result != null) {
                Box(
                    modifier = Modifier
                        .background(SurfaceDark, RoundedCornerShape(8.dp))
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${result.latencyMs} ms",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (result == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No evaluation result available.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
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

        // ==========================================
        // DOMINANT INTERVENTION BANNER
        // ==========================================
        val (interventionColor, interventionIcon, interventionCopy) = when (result.intervention) {
            InterventionType.ACT -> Triple(
                ActGreen,
                Icons.Default.CheckCircle,
                "Looks safe to proceed."
            )
            InterventionType.ASK -> Triple(
                AskYellow,
                Icons.AutoMirrored.Filled.Help,
                "Before you continue, we need to clarify something."
            )
            InterventionType.WARN -> Triple(
                WarnOrange,
                Icons.Default.Warning,
                "This action carries a meaningful risk."
            )
            InterventionType.STOP -> Triple(
                StopRed,
                Icons.Default.Block,
                "We strongly recommend not proceeding."
            )
        }

        DominantInterventionCard(
            intervention = result.intervention,
            color = interventionColor,
            icon = interventionIcon,
            copy = interventionCopy,
            riskScore = result.riskScore
        )

        // ==========================================
        // KEY METRICS CARD
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "SAFETY METRICS",
                color = CyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    title = "Risk score",
                    symbol = "rho",
                    value = String.format("%.3f", result.riskScore),
                    accentColor = interventionColor
                )
                MetricColumn(
                    title = "Confidence",
                    symbol = "c",
                    value = "${(result.confidence * 100).toInt()}%",
                    accentColor = TextPrimary
                )
                MetricColumn(
                    title = "Severity",
                    symbol = "s",
                    value = String.format("%.2f", result.severity),
                    accentColor = TextPrimary
                )
                MetricColumn(
                    title = "Reversibility",
                    symbol = "r",
                    value = String.format("%.2f", result.irreversibility),
                    accentColor = TextPrimary
                )
            }

            HorizontalDivider(color = DividerColor)

            Text(
                text = "Formula: rho = s * (1 + lambda * r) = ${String.format("%.2f", result.severity)} * (1 + 0.75 * ${String.format("%.2f", result.irreversibility)}) = ${String.format("%.3f", result.riskScore)}",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // ==========================================
        // ALTERNATIVE ACTION CARD
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceGlass, RoundedCornerShape(16.dp))
                .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AltRoute,
                    contentDescription = "Alternative",
                    tint = CyanAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "ALTERNATIVE ACTION",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = result.alternativeAction,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp
            )
        }

        // ==========================================
        // RATIONALE CARD
        // ==========================================
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
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }

        // ==========================================
        // VISUAL DECISION TRACE PIPELINE
        // ==========================================
        DecisionTracePipeline(
            result = result,
            modifier = Modifier.fillMaxWidth()
        )

        // ==========================================
        // EVIDENCE SECTION
        // ==========================================
        EvidenceCard(
            evidenceItems = result.evidence,
            perceptionResult = state.lastPerceptionResult,
            urlRiskScore = state.currentUrlRiskScore,
            modifier = Modifier.fillMaxWidth()
        )

        // ==========================================
        // ACTION BUTTONS (EXACT PROMPT SPECIFICATION)
        // ==========================================
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (result.intervention == InterventionType.STOP) {
                // STOP SPECIFICATION:
                // Continue anyway
                // Review evidence
                Button(
                    onClick = { showOverrideWarning = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StopRed.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Override",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Continue anyway",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(CyanAccent, CyanAccent.copy(alpha = 0.5f)))),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Evidence",
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Review evidence",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Change action / Cancel", color = TextSecondary, fontSize = 13.sp)
                }
            } else {
                // ACT, ASK, WARN SPECIFICATION:
                // Continue
                // Review evidence
                // Change action
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (result.intervention) {
                            InterventionType.ACT -> ActGreen
                            InterventionType.ASK -> AskYellow
                            InterventionType.WARN -> WarnOrange
                            else -> CyanAccent
                        }
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Continue",
                        color = BackgroundDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(SurfaceBorder, SurfaceBorderSubtle))),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Evidence",
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Review evidence",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Change",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Change action",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DominantInterventionCard(
    intervention: InterventionType,
    color: Color,
    icon: ImageVector,
    copy: String,
    riskScore: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, color, RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.18f))
                            .border(1.5.dp, color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = intervention.name,
                            tint = color,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column {
                        Text(
                            text = intervention.name,
                            color = color,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "INTERVENTION PROTOCOL",
                            color = color.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Compact Risk Badge
                Box(
                    modifier = Modifier
                        .background(color.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .border(1.dp, color, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "rho = ${String.format("%.2f", riskScore)}",
                        color = color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            HorizontalDivider(color = color.copy(alpha = 0.3f))

            // The required exact user copy:
            Text(
                text = copy,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
private fun MetricColumn(
    title: String,
    symbol: String,
    value: String,
    accentColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            color = TextTertiary,
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = accentColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "($symbol)",
            color = TextTertiary,
            fontSize = 10.sp
        )
    }
}
