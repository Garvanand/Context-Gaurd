package com.contextguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.core.state.UiState
import com.contextguard.app.theme.*
import com.contextguard.app.ui.components.LoadingOverlay
import com.contextguard.app.ui.viewmodel.MainViewModel

@Composable
fun AnalyzeScreen(
    viewModel: MainViewModel,
    onResultReady: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.appState.collectAsState()
    val analysisState by viewModel.analysisState.collectAsState()
    val scrollState = rememberScrollState()

    val actionOptions = listOf(
        Pair("Save to Personal Encrypted Vault", "Personal Drive (Encrypted)"),
        Pair("Send via Instant Messaging Chat", "Unverified Telegram Contact"),
        Pair("Broadcast on Social Media Timeline", "Public Twitter/X Feed")
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
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
                    text = "Pre-Action Evaluation",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Artifact Information Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(16.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "CURRENT ARTIFACT",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "File",
                        tint = CyanAccent,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = state.currentArtifactTitle,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Origin: ${state.currentSourceApp}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                HorizontalDivider(color = DividerColor)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "On-Device Masking",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${state.maskedPiiCount} PII entities masked locally",
                            color = ActGreen,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = state.isRedactionEnabled,
                        onCheckedChange = { viewModel.toggleRedaction(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                    )
                }
            }

            // Intended Action Selector
            Text(
                text = "SELECT INTENDED ACTION",
                color = TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            actionOptions.forEach { (actionName, destName) ->
                val isSelected = state.selectedAction == actionName
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) SurfaceGlass else SurfaceDark,
                            RoundedCornerShape(14.dp)
                        )
                        .border(
                            1.5.dp,
                            if (isSelected) CyanAccent else SurfaceBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.setAction(actionName, destName) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { viewModel.setAction(actionName, destName) },
                        colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
                    )
                    Column {
                        Text(
                            text = actionName,
                            color = if (isSelected) CyanAccent else TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Target: $destName",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Button
            Button(
                onClick = {
                    viewModel.executeAnalysis {
                        onResultReady()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Evaluate Safety Policy",
                    color = BackgroundDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (analysisState is UiState.Loading) {
            val msg = (analysisState as UiState.Loading).message
            LoadingOverlay(message = msg)
        }
    }
}
