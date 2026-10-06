package com.contextguard.app.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.core.privacy.RedactionStyle
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

    var previewMode by remember { mutableStateOf("AFTER") } // "BEFORE" vs "AFTER"

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
                Column {
                    Text(
                        text = "Pre-Action Evaluation",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Mode: ${state.backendConfig.networkMode.name}",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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

            // Interactive BEFORE vs AFTER REDACTION Preview Card
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
                        text = "REDACTION PREVIEW",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Before vs After Switch Pills
                    Row(
                        modifier = Modifier
                            .background(BackgroundDark, RoundedCornerShape(8.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (previewMode == "BEFORE") SurfaceGlass else BackgroundDark,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { previewMode = "BEFORE" }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "BEFORE",
                                color = if (previewMode == "BEFORE") WarnOrange else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (previewMode == "AFTER") SurfaceGlass else BackgroundDark,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { previewMode = "AFTER" }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "AFTER REDACTION",
                                color = if (previewMode == "AFTER") ActGreen else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Visual Bitmap Preview or Text Preview
                val rawBmp = state.rawBitmap
                val redBmp = state.lastRedactionResult?.redactedBitmap

                if (rawBmp != null) {
                    val displayBmp = if (previewMode == "AFTER") (redBmp ?: rawBmp) else rawBmp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(BackgroundDark, RoundedCornerShape(12.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = displayBmp.asImageBitmap(),
                            contentDescription = "Artifact Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    // Text / OCR Preview
                    val displayText = if (previewMode == "AFTER") {
                        state.lastRedactionResult?.redactedText ?: state.lastPerceptionResult?.ocrText ?: "Sample Account PII masked locally: [REDACTED_ACCOUNT] ending in 5671"
                    } else {
                        state.lastPerceptionResult?.ocrText?.ifEmpty { "Account Number: 4532 0150 1234 5671\nPhone: +91 9876543210\nOTP: 482910" }
                            ?: "Account Number: 4532 0150 1234 5671\nPhone: +91 9876543210\nOTP: 482910"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(BackgroundDark, RoundedCornerShape(12.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = displayText,
                            color = if (previewMode == "AFTER") ActGreen else TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Redaction Style Options (Blackout vs Blur)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Style:", color = TextSecondary, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.redactionStyle == RedactionStyle.BLACKOUT,
                            onClick = { viewModel.setRedactionStyle(RedactionStyle.BLACKOUT) },
                            label = { Text("Blackout", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = state.redactionStyle == RedactionStyle.BLUR,
                            onClick = { viewModel.setRedactionStyle(RedactionStyle.BLUR) },
                            label = { Text("Blur", fontSize = 11.sp) }
                        )
                    }
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

            Spacer(modifier = Modifier.height(8.dp))

            // Action Button
            Button(
                onClick = {
                    viewModel.executeAnalysis {
                        onResultReady()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Evaluate",
                    tint = BackgroundDark,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Evaluate Pre-Action Safety",
                    color = BackgroundDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Loading overlay
        if (analysisState is UiState.Loading) {
            val msg = (analysisState as UiState.Loading).message
            LoadingOverlay(message = msg)
        }
    }
}
