package com.contextguard.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.*

data class StageInfo(
    val step: Int,
    val name: String,
    val description: String
)

private val PIPELINE_STAGES = listOf(
    StageInfo(1, "Context", "Artifact metadata, recipient & channel parameters"),
    StageInfo(2, "Intent", "Action-conditioned mapping & harm taxonomy"),
    StageInfo(3, "Evidence", "ML Kit OCR, sensitive PII & XGBoost signals"),
    StageInfo(4, "Consequence", "Projecting severity (s) and irreversibility (r)"),
    StageInfo(5, "Uncertainty", "Epistemic confidence calibration"),
    StageInfo(6, "Intervention", "Deterministic policy threshold gating")
)

@Composable
fun SixStageProgressOverlay(
    currentMessage: String,
    modifier: Modifier = Modifier
) {
    // Determine active stage from message text
    val activeStep = when {
        currentMessage.contains("1/6", true) || currentMessage.contains("Context", true) -> 1
        currentMessage.contains("2/6", true) || currentMessage.contains("Intent", true) -> 2
        currentMessage.contains("3/6", true) || currentMessage.contains("Evidence", true) -> 3
        currentMessage.contains("4/6", true) || currentMessage.contains("Consequence", true) -> 4
        currentMessage.contains("5/6", true) || currentMessage.contains("Uncertainty", true) -> 5
        currentMessage.contains("6/6", true) || currentMessage.contains("Intervention", true) -> 6
        else -> 2
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(SurfaceDark, RoundedCornerShape(24.dp))
                .border(1.5.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyanAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Evaluating",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Analyzing Pre-Action Risk",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Six-Stage Multimodal Reasoning Pipeline",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // The Six Stages
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PIPELINE_STAGES.forEach { stage ->
                    val isDone = stage.step < activeStep
                    val isActive = stage.step == activeStep

                    val badgeColor = when {
                        isDone -> ActGreen
                        isActive -> CyanAccent
                        else -> TextTertiary.copy(alpha = 0.3f)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isActive) SurfaceGlassHigh else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Node status indicator
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(badgeColor.copy(alpha = if (isActive || isDone) 0.2f else 0.1f))
                                .border(1.dp, badgeColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isDone -> {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Done",
                                        tint = ActGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                isActive -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = CyanAccent,
                                        strokeWidth = 2.dp
                                    )
                                }
                                else -> {
                                    Text(
                                        text = "${stage.step}",
                                        color = TextTertiary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Label
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = stage.name,
                                    color = if (isActive) CyanAccent else if (isDone) TextPrimary else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                )
                                if (isActive) {
                                    Text(
                                        text = "ACTIVE",
                                        color = CyanAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                            Text(
                                text = stage.description,
                                color = TextTertiary,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Current message display
            Text(
                text = currentMessage,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 2
            )
        }
    }
}
