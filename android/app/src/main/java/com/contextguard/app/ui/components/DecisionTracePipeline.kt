package com.contextguard.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.*
import com.contextguard.app.ui.viewmodel.SafetyResult

data class PipelineStageNode(
    val id: String,
    val title: String,
    val summary: String,
    val icon: ImageVector,
    val accentColor: Color,
    val details: List<Pair<String, String>>
)

@Composable
fun DecisionTracePipeline(
    result: SafetyResult,
    modifier: Modifier = Modifier
) {
    val s = result.severity
    val r = result.irreversibility
    val c = result.confidence
    val rho = result.riskScore
    val lambda = result.lambda

    val stages = listOf(
        PipelineStageNode(
            id = "STAGE_ARTIFACT",
            title = "1. Artifact",
            summary = "${result.artifactTitle} | Ingested via Volatile Memory",
            icon = Icons.Default.Description,
            accentColor = CyanAccent,
            details = listOf(
                "Artifact Name" to result.artifactTitle,
                "SHA-256 Digest" to result.hashSha256.take(16) + "...",
                "RAM Invariant" to "Zero raw disk persistence; in-memory processing only"
            )
        ),
        PipelineStageNode(
            id = "STAGE_CONTEXT",
            title = "2. Context",
            summary = "Target: ${result.destination} | Source App Analyzed",
            icon = Icons.Default.Share,
            accentColor = IndigoAccent,
            details = listOf(
                "Destination Channel" to result.destination,
                "Recipient Verification" to if (result.destination.contains("Public", true) || result.destination.contains("Telegram", true)) "Unverified / External" else "Private / Enclosed",
                "Channel Exposure" to if (result.destination.contains("Public", true)) "Global Broadcast" else "Encrypted Perimeter"
            )
        ),
        PipelineStageNode(
            id = "STAGE_INTENT",
            title = "3. Intent",
            summary = "Action: ${result.intendedAction}",
            icon = Icons.Default.TouchApp,
            accentColor = VioletAccent,
            details = listOf(
                "Selected Action" to result.intendedAction,
                "Intent Source" to "Explicit User Action",
                "Action Tier" to if (result.intendedAction.contains("Post", true) || result.intendedAction.contains("Send", true)) "Transmission" else "Archival"
            )
        ),
        PipelineStageNode(
            id = "STAGE_EVIDENCE",
            title = "4. Evidence",
            summary = "${result.evidence.size} Grounded Perceptual & Lexical Items",
            icon = Icons.Default.Search,
            accentColor = CyanAccent,
            details = result.evidence.mapIndexed { idx, ev -> "Evidence #${idx + 1}" to ev }
        ),
        PipelineStageNode(
            id = "STAGE_CONSEQUENCE",
            title = "5. Consequence",
            summary = "Severity (s) = ${String.format("%.2f", s)} | Irreversibility (r) = ${String.format("%.2f", r)}",
            icon = Icons.Default.Assessment,
            accentColor = WarnOrange,
            details = listOf(
                "Harm Magnitude (s)" to String.format("%.3f", s),
                "Irreversibility Factor (r)" to String.format("%.3f", r),
                "Consequence Type" to if (s >= 0.7f) "Catastrophic Exposure" else if (s >= 0.3f) "Moderate Disclosure Hazard" else "Negligible"
            )
        ),
        PipelineStageNode(
            id = "STAGE_UNCERTAINTY",
            title = "6. Uncertainty",
            summary = "Epistemic Confidence (c) = ${(c * 100).toInt()}%",
            icon = Icons.Default.Psychology,
            accentColor = AskYellow,
            details = listOf(
                "Confidence Metric (c)" to "${String.format("%.2f", c)} (Scale: 0.0 - 1.0)",
                "Epistemic Uncertainty" to if (c < 0.70f) "High Uncertainty (Triggers ASK Gating)" else "Calibrated Low Uncertainty",
                "Safety Invariant" to "Uncertainty is NEVER treated as safety"
            )
        ),
        PipelineStageNode(
            id = "STAGE_POLICY",
            title = "7. Policy",
            summary = "Formula: rho = s * (1 + lambda * r) = ${String.format("%.3f", rho)}",
            icon = Icons.Default.Functions,
            accentColor = CyanAccent,
            details = listOf(
                "Composite Risk (rho)" to String.format("%.3f", rho),
                "Lambda Weight" to "$lambda (Irreversibility penalty)",
                "Mathematical Gating" to "STOP >= 0.65 | WARN >= 0.35 | ASK: rho >= 0.35 & c < 0.70 | ACT < 0.35",
                "Evaluation Mode" to "Deterministic Non-hallucinatory Lock"
            )
        ),
        PipelineStageNode(
            id = "STAGE_INTERVENTION",
            title = "8. Intervention",
            summary = "Emitted: ${result.intervention.name} | Override Allowed: ${result.canOverride}",
            icon = when (result.intervention) {
                com.contextguard.app.ui.viewmodel.InterventionType.ACT -> Icons.Default.CheckCircle
                com.contextguard.app.ui.viewmodel.InterventionType.ASK -> Icons.Default.Help
                com.contextguard.app.ui.viewmodel.InterventionType.WARN -> Icons.Default.Warning
                com.contextguard.app.ui.viewmodel.InterventionType.STOP -> Icons.Default.Block
            },
            accentColor = when (result.intervention) {
                com.contextguard.app.ui.viewmodel.InterventionType.ACT -> ActGreen
                com.contextguard.app.ui.viewmodel.InterventionType.ASK -> AskYellow
                com.contextguard.app.ui.viewmodel.InterventionType.WARN -> WarnOrange
                com.contextguard.app.ui.viewmodel.InterventionType.STOP -> StopRed
            },
            details = listOf(
                "Intervention Level" to result.intervention.name,
                "Grounding Rationale" to result.rationale,
                "Deliberate Override" to if (result.canOverride) "Permissible via confirmation dialog" else "Strictly Prohibited"
            )
        )
    )

    Column(
        modifier = modifier
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
            Column {
                Text(
                    text = "VISUAL DECISION TRACE",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Eight-stage causal pipeline (Tap node to expand)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Icon(
                imageVector = Icons.Default.AccountTree,
                contentDescription = "Pipeline",
                tint = CyanAccent,
                modifier = Modifier.size(20.dp)
            )
        }

        HorizontalDivider(color = DividerColor)

        stages.forEachIndexed { index, node ->
            PipelineNodeItem(
                node = node,
                isLast = index == stages.lastIndex
            )
        }
    }
}

@Composable
private fun PipelineNodeItem(
    node: PipelineStageNode,
    isLast: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Node icon & connecting line
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(node.accentColor.copy(alpha = 0.15f))
                        .border(1.5.dp, node.accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = node.icon,
                        contentDescription = node.title,
                        tint = node.accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }

                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(if (expanded) 24.dp else 16.dp)
                            .background(SurfaceBorder)
                    )
                }
            }

            // Node info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = if (isLast) 0.dp else 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = node.title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = node.summary,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                // Expanded Section
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(BackgroundDark, RoundedCornerShape(10.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        node.details.forEach { (label, value) ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = label,
                                    color = node.accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = value,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
