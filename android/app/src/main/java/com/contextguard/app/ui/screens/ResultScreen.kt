package com.contextguard.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.ActLime
import com.contextguard.app.theme.AskAmber
import com.contextguard.app.theme.BackgroundDark
import com.contextguard.app.theme.BodyFont
import com.contextguard.app.theme.ContourBorder
import com.contextguard.app.theme.ContourBorderActive
import com.contextguard.app.theme.DeepSurface
import com.contextguard.app.theme.DisplayFont
import com.contextguard.app.theme.ElectricViolet
import com.contextguard.app.theme.ElevatedSurface
import com.contextguard.app.theme.Ink
import com.contextguard.app.theme.IonCyan
import com.contextguard.app.theme.MutedText
import com.contextguard.app.theme.SignalLime
import com.contextguard.app.theme.SoftWhite
import com.contextguard.app.theme.StopCoral
import com.contextguard.app.theme.StopRed
import com.contextguard.app.theme.SubtleText
import com.contextguard.app.theme.TechnicalMono
import com.contextguard.app.theme.WarnOrange
import com.contextguard.app.ui.components.ContextComparisonMode
import com.contextguard.app.ui.components.ContextField
import com.contextguard.app.ui.components.ContextFieldMode
import com.contextguard.app.ui.components.DecisionTracePipeline
import com.contextguard.app.ui.components.EvidenceCard
import com.contextguard.app.ui.viewmodel.InterventionType
import com.contextguard.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Redesigned Intervention Decision & Evidence Inspection Screen.
 *
 * Implements:
 * - Step 4: Controlled Result Reveal:
 *   1. Context Field settles into its decisive harmonic state.
 *   2. Intervention accent appears.
 *   3. Evidence inspection area enters with subtle stagger.
 *   4. Primary action becomes available.
 * - Step 5: Evidence Inspection:
 *   - Grounded perceptual and OCR evidence
 *   - XGBoost URL probability and lexical features
 *   - Redaction before/after inspection
 *   - Uncertainty / epistemic confidence explanation
 *   - Guided focus transition on evidence items
 * - Step 6: Change-Context Comparison:
 *   - Dynamic interactive simulation of the same artifact under SAVE, SEND, and POST.
 */
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
    var focusedEvidenceIndex by remember { mutableStateOf<Int?>(null) }

    // Controlled 4-phase reveal progression state
    var revealPhase by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        // Phase 1: Context Field settles (0ms)
        revealPhase = 1
        delay(160)
        // Phase 2: Intervention accent and badge (160ms)
        revealPhase = 2
        delay(160)
        // Phase 3: Evidence and metrics stagger (320ms)
        revealPhase = 3
        delay(140)
        // Phase 4: Primary actions enabled (460ms)
        revealPhase = 4
    }

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
                        tint = StopCoral
                    )
                    Text(
                        text = "Engage Intentional Override?",
                        color = StopCoral,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        fontFamily = DisplayFont
                    )
                }
            },
            text = {
                Text(
                    text = "ContextGuard identified this action as irreversible exposure (rho = ${String.format("%.2f", result.riskScore)} >= 0.65).\n\nOverriding will bypass safety protections and proceed with '${result.intendedAction}' to '${result.destination}'.\n\nDo you confirm this action?",
                    color = SoftWhite,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontFamily = BodyFont
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverrideWarning = false
                        viewModel.overrideStopIntervention()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StopCoral),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Confirm & Override", color = SoftWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverrideWarning = false }) {
                    Text(text = "Cancel", color = MutedText)
                }
            },
            containerColor = DeepSurface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
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
                        tint = SoftWhite
                    )
                }
                Column {
                    Text(
                        text = "Intervention Decision",
                        color = SoftWhite,
                        fontSize = 22.sp,
                        fontFamily = DisplayFont,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Action-conditioned pre-action safety",
                        color = MutedText,
                        fontSize = 11.sp,
                        fontFamily = TechnicalMono
                    )
                }
            }

            // Latency chip
            if (result != null) {
                Box(
                    modifier = Modifier
                        .background(ElevatedSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, ContourBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${result.latencyMs} ms",
                        color = IonCyan,
                        fontSize = 11.sp,
                        fontFamily = TechnicalMono,
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
                    color = MutedText,
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
                    .border(1.dp, StopCoral, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = ElevatedSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Override", tint = StopCoral)
                    Text(
                        text = "Deliberate User Override Active: Safety intervention bypassed by user consent.",
                        color = StopCoral,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = TechnicalMono
                    )
                }
            }
        }

        // ==========================================
        // PHASE 1 & 2: SETTLING CONTEXT FIELD & INTERVENTION ACCENT
        // ==========================================
        val (interventionColor, interventionIcon, interventionCopy) = when (result.intervention) {
            InterventionType.ACT -> Triple(
                ActLime,
                Icons.Default.CheckCircle,
                "Looks safe to proceed."
            )
            InterventionType.ASK -> Triple(
                AskAmber,
                Icons.AutoMirrored.Filled.Help,
                "Before you continue, we need to clarify something."
            )
            InterventionType.WARN -> Triple(
                WarnOrange,
                Icons.Default.Warning,
                "This action carries a meaningful risk."
            )
            InterventionType.STOP -> Triple(
                StopCoral,
                Icons.Default.Block,
                "We strongly recommend not proceeding."
            )
        }

        DominantInterventionCard(
            intervention = result.intervention,
            color = interventionColor,
            icon = interventionIcon,
            copy = interventionCopy,
            riskScore = result.riskScore,
            isRevealed = revealPhase >= 2,
            focusedEvidenceAngle = focusedEvidenceIndex?.let { (it * 45f - 20f) }
        )

        // ==========================================
        // PHASE 3: KEY METRICS & REASONING BOUNDS
        // ==========================================
        AnimatedVisibility(
            visible = revealPhase >= 3,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { 20 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepSurface, RoundedCornerShape(18.dp))
                    .border(1.dp, ContourBorder, RoundedCornerShape(18.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SAFETY METRICS",
                        color = IonCyan,
                        fontSize = 11.sp,
                        fontFamily = TechnicalMono,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "LAMBDA = ${String.format("%.2f", result.lambda)}",
                        color = MutedText,
                        fontSize = 10.sp,
                        fontFamily = TechnicalMono
                    )
                }

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
                        accentColor = SoftWhite
                    )
                    MetricColumn(
                        title = "Severity",
                        symbol = "s",
                        value = String.format("%.2f", result.severity),
                        accentColor = SoftWhite
                    )
                    MetricColumn(
                        title = "Reversibility",
                        symbol = "r",
                        value = String.format("%.2f", result.irreversibility),
                        accentColor = SoftWhite
                    )
                }

                HorizontalDivider(color = ContourBorder)

                Text(
                    text = "rho = s * (1 + lambda * r) = ${String.format("%.2f", result.severity)} * (1 + 0.75 * ${String.format("%.2f", result.irreversibility)}) = ${String.format("%.3f", result.riskScore)}",
                    color = IonCyan,
                    fontSize = 11.sp,
                    fontFamily = TechnicalMono,
                    fontWeight = FontWeight.SemiBold
                )

                // Epistemic Uncertainty Calibration Note
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ElevatedSurface, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Uncertainty",
                        tint = IonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Confidence c = ${(result.confidence * 100).toInt()}%: Calibrated via ML Kit OCR text length and XGBoost feature density.",
                        color = SubtleText,
                        fontSize = 10.sp,
                        fontFamily = TechnicalMono
                    )
                }
            }
        }

        // ==========================================
        // RECOMMENDED ALTERNATIVE CARD
        // ==========================================
        AnimatedVisibility(
            visible = revealPhase >= 3,
            enter = fadeIn(tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ElevatedSurface, RoundedCornerShape(18.dp))
                    .border(1.dp, ContourBorderActive, RoundedCornerShape(18.dp))
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
                        tint = IonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "RECOMMENDED ALTERNATIVE",
                        color = IonCyan,
                        fontSize = 11.sp,
                        fontFamily = TechnicalMono,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = result.alternativeAction,
                    color = SoftWhite,
                    fontSize = 14.sp,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp
                )
            }
        }

        // ==========================================
        // DECISION RATIONALE
        // ==========================================
        AnimatedVisibility(
            visible = revealPhase >= 3,
            enter = fadeIn(tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepSurface, RoundedCornerShape(18.dp))
                    .border(1.dp, ContourBorder, RoundedCornerShape(18.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "DECISION RATIONALE",
                    color = IonCyan,
                    fontSize = 11.sp,
                    fontFamily = TechnicalMono,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = result.rationale,
                    color = SoftWhite,
                    fontSize = 13.sp,
                    fontFamily = BodyFont,
                    lineHeight = 20.sp
                )
            }
        }

        // ==========================================
        // VISUAL DECISION TRACE PIPELINE
        // ==========================================
        AnimatedVisibility(
            visible = revealPhase >= 3,
            enter = fadeIn(tween(300))
        ) {
            DecisionTracePipeline(
                result = result,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ==========================================
        // STEP 5: EVIDENCE INSPECTION
        // ==========================================
        AnimatedVisibility(
            visible = revealPhase >= 3,
            enter = fadeIn(tween(300))
        ) {
            EvidenceCard(
                evidenceItems = result.evidence,
                perceptionResult = state.lastPerceptionResult,
                artifactType = if (state.rawBitmap != null) "IMAGE" else "DOCUMENT",
                urlRiskScore = state.currentUrlRiskScore,
                selectedEvidenceIndex = focusedEvidenceIndex,
                onEvidenceSelected = { focusedEvidenceIndex = it },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ==========================================
        // STEP 6: CHANGE-CONTEXT COMPARISON
        // ==========================================
        AnimatedVisibility(
            visible = revealPhase >= 3,
            enter = fadeIn(tween(300))
        ) {
            ContextComparisonMode(
                artifactTitle = state.currentArtifactTitle,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ==========================================
        // PHASE 4: PRIMARY ACTION BUTTONS
        // ==========================================
        AnimatedVisibility(
            visible = revealPhase >= 4,
            enter = fadeIn(tween(250))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (result.intervention == InterventionType.STOP) {
                    // STOP Specification: Continue anyway, Review evidence, Change action
                    Button(
                        onClick = { showOverrideWarning = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StopCoral),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Override",
                            tint = SoftWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Continue anyway",
                            color = SoftWhite,
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftWhite),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(IonCyan, ElectricViolet))
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Evidence",
                            tint = IonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Review evidence",
                            color = SoftWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Change action / Cancel", color = MutedText, fontSize = 13.sp)
                    }
                } else {
                    // ACT, ASK, WARN Specification: Continue, Review evidence, Change action
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (result.intervention) {
                                InterventionType.ACT -> ActLime
                                InterventionType.ASK -> AskAmber
                                InterventionType.WARN -> WarnOrange
                                else -> IonCyan
                            }
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Continue",
                            color = Ink,
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftWhite),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(ContourBorderActive, ContourBorder))
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Evidence",
                            tint = IonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Review evidence",
                            color = SoftWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedText),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(ContourBorder, ContourBorder))
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change",
                            tint = MutedText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Change action",
                            color = MutedText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
    riskScore: Float,
    isRevealed: Boolean,
    focusedEvidenceAngle: Float? = null
) {
    val fieldMode = when (intervention) {
        InterventionType.ACT -> ContextFieldMode.ACT
        InterventionType.ASK -> ContextFieldMode.ASK
        InterventionType.WARN -> ContextFieldMode.WARN
        InterventionType.STOP -> ContextFieldMode.STOP
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0.96f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "InterventionCardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .border(1.5.dp, color.copy(alpha = 0.65f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = ElevatedSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.16f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(180f, 120f),
                        radius = 450f
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = intervention.name,
                                tint = color,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "INTERVENTION PROTOCOL",
                                color = MutedText,
                                fontSize = 10.sp,
                                fontFamily = TechnicalMono,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Text(
                                text = "RHO: ${String.format("%.2f", riskScore)}",
                                color = color,
                                fontSize = 11.sp,
                                fontFamily = TechnicalMono,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = intervention.name,
                            color = color,
                            fontSize = 36.sp,
                            fontFamily = DisplayFont,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    // Context Field Animated Motif settles into final harmonic state
                    ContextField(
                        mode = fieldMode,
                        size = 92.dp,
                        focusedEvidenceAngle = focusedEvidenceAngle
                    )
                }

                HorizontalDivider(color = ContourBorder)

                // Exact required user copy:
                Text(
                    text = copy,
                    color = SoftWhite,
                    fontSize = 17.sp,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )
            }
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
            text = title.uppercase(),
            color = MutedText,
            fontSize = 10.sp,
            fontFamily = TechnicalMono,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            color = accentColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = TechnicalMono
        )
        Text(
            text = "($symbol)",
            color = MutedText.copy(alpha = 0.7f),
            fontSize = 9.sp,
            fontFamily = TechnicalMono
        )
    }
}
