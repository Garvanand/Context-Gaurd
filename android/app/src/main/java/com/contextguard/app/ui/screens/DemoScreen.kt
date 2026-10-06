package com.contextguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.*
import com.contextguard.app.ui.components.InterventionBadge
import com.contextguard.app.ui.components.LoadingOverlay
import com.contextguard.app.ui.viewmodel.DemoAction
import com.contextguard.app.ui.viewmodel.MainViewModel

@Composable
fun DemoScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onResultReady: () -> Unit
) {
    val scenario = viewModel.demoScenarios.first()
    val scrollState = rememberScrollState()
    var isRunningAction by remember { mutableStateOf(false) }

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
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "Central Viva Demonstration",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Scenario Explanation Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(16.dp))
                    .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ACTION-CONDITIONING PROOF OF CONCEPT",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Identical Base Artifact: ${scenario.artifactName}",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = scenario.description,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Text(
                text = "EXECUTE SCENARIO TRIAD",
                color = TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // 3 Actions
            scenario.actions.forEachIndexed { index, action ->
                DemoActionCard(
                    index = index + 1,
                    action = action,
                    onExecute = {
                        isRunningAction = true
                        viewModel.executeDemoAction(action) {
                            isRunningAction = false
                            onResultReady()
                        }
                    }
                )
            }
        }

        if (isRunningAction) {
            LoadingOverlay(message = "Simulating pre-action policy pipeline...")
        }
    }
}

@Composable
private fun DemoActionCard(
    index: Int,
    action: DemoAction,
    onExecute: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$index. ${action.title}",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            InterventionBadge(intervention = action.expectedIntervention)
        }

        Text(
            text = "Intended Action: ${action.actionName}",
            color = TextSecondary,
            fontSize = 13.sp
        )
        Text(
            text = "Destination: ${action.destination}",
            color = TextTertiary,
            fontSize = 12.sp
        )

        HorizontalDivider(color = DividerColor)

        Text(
            text = action.rationale,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        Button(
            onClick = onExecute,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (action.expectedIntervention) {
                    com.contextguard.app.ui.viewmodel.InterventionType.ACT -> ActGreen
                    com.contextguard.app.ui.viewmodel.InterventionType.ASK -> AskYellow
                    com.contextguard.app.ui.viewmodel.InterventionType.WARN -> WarnOrange
                    com.contextguard.app.ui.viewmodel.InterventionType.STOP -> StopRed
                }
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Run",
                    tint = BackgroundDark
                )
                Text(
                    text = "Evaluate Action $index",
                    color = BackgroundDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
