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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.*
import com.contextguard.app.ui.components.InterventionBadge
import com.contextguard.app.ui.components.SixStageProgressOverlay
import com.contextguard.app.ui.viewmodel.MainViewModel
import com.contextguard.app.ui.viewmodel.ReadyDemoScenario

@Composable
fun DemoScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onResultReady: () -> Unit
) {
    val scenarios = viewModel.readyDemoScenarios
    val scrollState = rememberScrollState()
    var runningScenarioId by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Column {
                    Text(
                        text = "Demo Scenarios",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "8 Ready-to-Run Action-Conditioned Safety Proofs",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Central Viva Scenario Banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceGlass, RoundedCornerShape(18.dp))
                    .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(CyanAccent, CircleShape)
                    )
                    Text(
                        text = "RESEARCH HYPOTHESIS VALIDATION",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "Risk is not an intrinsic property of the artifact alone.",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Select any scenario below to evaluate how ContextGuard dynamically computes risk conditioning on Artifact + Context + Intent.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            Text(
                text = "READY-TO-RUN SCENARIOS (8)",
                color = TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // The 8 Ready-to-Run Cards
            scenarios.forEach { scenario ->
                ReadyDemoCard(
                    scenario = scenario,
                    isRunning = runningScenarioId == scenario.id,
                    onExecute = {
                        runningScenarioId = scenario.id
                        viewModel.executeReadyDemo(scenario) {
                            runningScenarioId = null
                            onResultReady()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Active execution progress overlay
        if (runningScenarioId != null) {
            SixStageProgressOverlay(
                currentMessage = "Simulating pre-action safety pipeline for ${scenarios.firstOrNull { it.id == runningScenarioId }?.title ?: "Scenario"}..."
            )
        }
    }
}

@Composable
private fun ReadyDemoCard(
    scenario: ReadyDemoScenario,
    isRunning: Boolean,
    onExecute: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Number + Title + Badge
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
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(CyanAccent.copy(alpha = 0.15f))
                            .border(1.dp, CyanAccent.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${scenario.number}",
                            color = CyanAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = scenario.title,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = scenario.tag,
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }

                InterventionBadge(intervention = scenario.expectedIntervention)
            }

            // Description
            Text(
                text = scenario.description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            // Parameters Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundDark, RoundedCornerShape(10.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ParamItem(label = "Action", value = scenario.action, isAccent = true)
                ParamItem(label = "Target", value = scenario.destination.take(16) + if (scenario.destination.length > 16) "..." else "")
                ParamItem(label = "Severity", value = String.format("%.2f", scenario.severity))
                ParamItem(label = "Irrev", value = String.format("%.2f", scenario.irreversibility))
            }

            // Run Button
            Button(
                onClick = onExecute,
                enabled = !isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Run",
                    tint = BackgroundDark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Run Scenario",
                    color = BackgroundDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ParamItem(
    label: String,
    value: String,
    isAccent: Boolean = false
) {
    Column {
        Text(text = label, color = TextTertiary, fontSize = 9.sp)
        Text(
            text = value,
            color = if (isAccent) CyanAccent else TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
