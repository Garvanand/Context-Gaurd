package com.contextguard.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.ActLime
import com.contextguard.app.theme.AskAmber
import com.contextguard.app.theme.BodyFont
import com.contextguard.app.theme.ContourBorder
import com.contextguard.app.theme.ContourBorderActive
import com.contextguard.app.theme.DeepSurface
import com.contextguard.app.theme.DisplayFont
import com.contextguard.app.theme.ElectricViolet
import com.contextguard.app.theme.ElevatedSurface
import com.contextguard.app.theme.IonCyan
import com.contextguard.app.theme.MutedText
import com.contextguard.app.theme.SignalLime
import com.contextguard.app.theme.SoftWhite
import com.contextguard.app.theme.StopCoral
import com.contextguard.app.theme.SubtleText
import com.contextguard.app.theme.TechnicalMono
import com.contextguard.app.theme.WarnOrange
import com.contextguard.app.ui.viewmodel.InterventionType

data class ComparisonScenario(
    val id: String,
    val title: String,
    val action: String,
    val recipient: String,
    val destination: String,
    val visibility: String,
    val severity: Float,
    val irreversibility: Float,
    val lambda: Float = 0.75f,
    val icon: ImageVector,
    val rationale: String
) {
    val rho: Float get() = severity * (1.0f + lambda * irreversibility)

    val intervention: InterventionType
        get() = when {
            rho >= 0.65f -> InterventionType.STOP
            rho >= 0.35f -> InterventionType.WARN
            else -> InterventionType.ACT
        }
}

val COMPARISON_SCENARIOS = listOf(
    ComparisonScenario(
        id = "SAVE_VAULT",
        title = "Scenario 1: Save Privately",
        action = "SAVE",
        recipient = "Self (Vault)",
        destination = "Personal Encrypted Drive",
        visibility = "Private Perimeter",
        severity = 0.05f,
        irreversibility = 0.00f,
        icon = Icons.Default.Save,
        rationale = "Zero external transmission. The artifact remains strictly bounded inside user's local hardware enclave."
    ),
    ComparisonScenario(
        id = "SEND_UNVERIFIED",
        title = "Scenario 2: Send to Contact",
        action = "SEND",
        recipient = "Unverified Telegram User",
        destination = "Direct Messaging Channel",
        visibility = "Direct Channel",
        severity = 0.45f,
        irreversibility = 0.50f,
        icon = Icons.Default.Send,
        rationale = "Direct transmission to an unverified external entity. Partial irreversibility once packets are sent."
    ),
    ComparisonScenario(
        id = "POST_PUBLIC",
        title = "Scenario 3: Post Publicly",
        action = "POST",
        recipient = "Public Audience",
        destination = "Public Twitter/X Feed",
        visibility = "Public Broadcast",
        severity = 0.90f,
        irreversibility = 1.00f,
        icon = Icons.Default.Share,
        rationale = "Full public broadcast of sensitive credential/financial statement. Maximum irreversibility across the open web."
    )
)

/**
 * Step 6: Change-Context Comparison Component.
 *
 * Demonstrates why ContextGuard is truly context-aware:
 * Shows how the EXACT SAME ARTIFACT receives completely different interventions
 * depending on: Action, Recipient, Destination, and Visibility.
 * Runs the authentic policy calculation formula (rho = s * (1 + lambda * r))
 * and animates the decision boundary shifts.
 */
@Composable
fun ContextComparisonMode(
    artifactTitle: String,
    modifier: Modifier = Modifier
) {
    var selectedScenarioId by remember { mutableStateOf(COMPARISON_SCENARIOS[1].id) }
    val currentScenario = COMPARISON_SCENARIOS.firstOrNull { it.id == selectedScenarioId }
        ?: COMPARISON_SCENARIOS.first()

    val interventionColor by animateColorAsState(
        targetValue = when (currentScenario.intervention) {
            InterventionType.ACT -> ActLime
            InterventionType.WARN -> WarnOrange
            InterventionType.ASK -> AskAmber
            InterventionType.STOP -> StopCoral
        },
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "ScenarioInterventionColor"
    )

    val animatedRho by animateFloatAsState(
        targetValue = currentScenario.rho,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "AnimatedRho"
    )
    val animatedS by animateFloatAsState(
        targetValue = currentScenario.severity,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "AnimatedS"
    )
    val animatedR by animateFloatAsState(
        targetValue = currentScenario.irreversibility,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "AnimatedR"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DeepSurface)
            .border(1.dp, ContourBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ElectricViolet.copy(alpha = 0.2f))
                        .border(1.dp, ContourBorderActive, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = "Compare",
                        tint = IonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = "CHANGE-CONTEXT COMPARISON",
                        color = IonCyan,
                        fontSize = 11.sp,
                        fontFamily = TechnicalMono,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Same artifact '$artifactTitle' under 3 distinct contexts",
                        color = MutedText,
                        fontSize = 11.sp,
                        fontFamily = BodyFont
                    )
                }
            }
        }

        // 3 Scenario Selector Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            COMPARISON_SCENARIOS.forEach { scenario ->
                val isSelected = scenario.id == selectedScenarioId
                val tabBorderColor by animateColorAsState(
                    targetValue = if (isSelected) ContourBorderActive else ContourBorder,
                    label = "TabBorder"
                )
                val tabBgColor by animateColorAsState(
                    targetValue = if (isSelected) ElevatedSurface else DeepSurface,
                    label = "TabBg"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tabBgColor)
                        .border(1.dp, tabBorderColor, RoundedCornerShape(12.dp))
                        .clickable { selectedScenarioId = scenario.id }
                        .padding(vertical = 10.dp, horizontal = 6.dp)
                        .semantics { contentDescription = "Select ${scenario.title}" },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = scenario.icon,
                            contentDescription = scenario.action,
                            tint = if (isSelected) IonCyan else MutedText,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = scenario.action,
                            color = if (isSelected) SoftWhite else MutedText,
                            fontSize = 11.sp,
                            fontFamily = TechnicalMono,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when (scenario.intervention) {
                                        InterventionType.ACT -> ActLime.copy(alpha = 0.2f)
                                        InterventionType.WARN -> WarnOrange.copy(alpha = 0.2f)
                                        InterventionType.STOP -> StopCoral.copy(alpha = 0.2f)
                                        else -> IonCyan.copy(alpha = 0.2f)
                                    }
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = scenario.intervention.name,
                                color = when (scenario.intervention) {
                                    InterventionType.ACT -> ActLime
                                    InterventionType.WARN -> WarnOrange
                                    InterventionType.STOP -> StopCoral
                                    else -> IonCyan
                                },
                                fontSize = 8.sp,
                                fontFamily = TechnicalMono,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Active Scenario Policy Evaluation Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ElevatedSurface)
                .border(1.5.dp, interventionColor.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title and Big Intervention Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = currentScenario.title,
                        color = SoftWhite,
                        fontSize = 14.sp,
                        fontFamily = DisplayFont,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${currentScenario.visibility} • ${currentScenario.destination}",
                        color = SubtleText,
                        fontSize = 10.sp,
                        fontFamily = TechnicalMono
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(interventionColor.copy(alpha = 0.18f))
                        .border(1.dp, interventionColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = currentScenario.intervention.name,
                        color = interventionColor,
                        fontSize = 13.sp,
                        fontFamily = DisplayFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = ContourBorder)

            // Context Routing Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ContextParamItem(label = "ACTION", value = currentScenario.action)
                ContextParamItem(label = "RECIPIENT", value = currentScenario.recipient)
                ContextParamItem(label = "VISIBILITY", value = currentScenario.visibility)
            }

            // Policy Math Bars: Severity (s) & Irreversibility (r)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Severity Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Severity (s)",
                        color = MutedText,
                        fontSize = 11.sp,
                        fontFamily = TechnicalMono
                    )
                    Text(
                        text = String.format("%.2f", currentScenario.severity),
                        color = SoftWhite,
                        fontSize = 11.sp,
                        fontFamily = TechnicalMono,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { animatedS.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = interventionColor,
                    trackColor = DeepSurface,
                    strokeCap = StrokeCap.Round
                )

                // Irreversibility Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Irreversibility (r)",
                        color = MutedText,
                        fontSize = 11.sp,
                        fontFamily = TechnicalMono
                    )
                    Text(
                        text = String.format("%.2f", currentScenario.irreversibility),
                        color = SoftWhite,
                        fontSize = 11.sp,
                        fontFamily = TechnicalMono,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { animatedR.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ElectricViolet,
                    trackColor = DeepSurface,
                    strokeCap = StrokeCap.Round
                )
            }

            // The Actual Policy Math Formula Result
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, ContourBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "rho = s * (1 + 0.75 * r) = ${String.format("%.2f", currentScenario.severity)} * (1 + 0.75 * ${String.format("%.2f", currentScenario.irreversibility)}) = ${String.format("%.3f", animatedRho)}",
                    color = IonCyan,
                    fontSize = 10.sp,
                    fontFamily = TechnicalMono,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Rationale explanation
            Text(
                text = currentScenario.rationale,
                color = SoftWhite,
                fontSize = 12.sp,
                fontFamily = BodyFont,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun ContextParamItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = MutedText,
            fontSize = 9.sp,
            fontFamily = TechnicalMono
        )
        Text(
            text = value,
            color = SoftWhite,
            fontSize = 11.sp,
            fontFamily = TechnicalMono,
            fontWeight = FontWeight.SemiBold
        )
    }
}
