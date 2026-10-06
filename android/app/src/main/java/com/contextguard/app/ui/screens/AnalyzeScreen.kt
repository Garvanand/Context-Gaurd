package com.contextguard.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.core.privacy.RedactionStyle
import com.contextguard.app.core.state.UiState
import com.contextguard.app.theme.*
import com.contextguard.app.ui.components.SixStageProgressOverlay
import com.contextguard.app.ui.viewmodel.MainViewModel

private val ACTION_CHIPS = listOf(
    "SAVE",
    "SEND",
    "UPLOAD",
    "POST",
    "SIGN",
    "LOGIN",
    "APPROVE",
    "OPEN"
)

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

    // Context form fields
    var recipient by remember(state.currentRecipient) { mutableStateOf(state.currentRecipient) }
    var destination by remember(state.selectedDestination) { mutableStateOf(state.selectedDestination) }
    var sourceApp by remember(state.currentSourceApp) { mutableStateOf(state.currentSourceApp) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        text = "Pre-Action Evaluation",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Mode: ${state.backendConfig.networkMode.name} • On-Device Pipeline",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ==========================================
            // STEP 1: ARTIFACT PREVIEW
            // ==========================================
            StepHeader(stepNumber = "1", title = "Artifact Preview")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(18.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Artifact Meta Row
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
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CyanAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Artifact",
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = state.currentArtifactTitle,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = "Origin: ${state.currentSourceApp}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Before vs After Toggle Pills
                    Row(
                        modifier = Modifier
                            .background(BackgroundDark, RoundedCornerShape(8.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (previewMode == "BEFORE") SurfaceGlass else BackgroundDark,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { previewMode = "BEFORE" }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "BEFORE",
                                color = if (previewMode == "BEFORE") WarnOrange else TextSecondary,
                                fontSize = 10.sp,
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
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "AFTER REDACTION",
                                color = if (previewMode == "AFTER") ActGreen else TextSecondary,
                                fontSize = 10.sp,
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
                            .height(170.dp)
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
                    // Document / OCR Text Box
                    val displayText = if (previewMode == "AFTER") {
                        state.lastRedactionResult?.redactedText
                            ?: state.lastPerceptionResult?.ocrText
                            ?: "Account Number: [REDACTED_ACCOUNT] ending in 5671\nPhone: [REDACTED_PHONE]\nOTP: [REDACTED_SECRET]"
                    } else {
                        state.lastPerceptionResult?.ocrText?.ifEmpty {
                            "HDFC Bank Statement - Oct 2026\nAccount: 4532 0150 1234 5671 | IFSC: HDFC0000128\nAvailable Balance: INR 1,48,290.40"
                        } ?: "HDFC Bank Statement - Oct 2026\nAccount: 4532 0150 1234 5671 | IFSC: HDFC0000128\nAvailable Balance: INR 1,48,290.40"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
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

                // Redaction Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Redaction Style:",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${state.maskedPiiCount} masked",
                            color = ActGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = state.isRedactionEnabled,
                            onCheckedChange = { viewModel.toggleRedaction(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                        )
                    }
                }
            }

            // ==========================================
            // STEP 2: WHAT ARE YOU ABOUT TO DO?
            // ==========================================
            StepHeader(stepNumber = "2", title = "What are you about to do?")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(18.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "SELECT ACTION INTENT",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // 8 Action Chips Grid (2 rows of 4)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val row1 = ACTION_CHIPS.take(4)
                    val row2 = ACTION_CHIPS.drop(4)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row1.forEach { action ->
                            val isSelected = state.selectedAction.equals(action, ignoreCase = true)
                            ActionChipItem(
                                action = action,
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setActionChip(action) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row2.forEach { action ->
                            val isSelected = state.selectedAction.equals(action, ignoreCase = true)
                            ActionChipItem(
                                action = action,
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setActionChip(action) }
                            )
                        }
                    }
                }

                // Active Action Description Note
                Text(
                    text = "Selected Action: ${state.selectedAction} • Risk assessment will condition on this exact operation.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            // ==========================================
            // STEP 3: CONTEXT
            // ==========================================
            StepHeader(stepNumber = "3", title = "Context")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(18.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "ROUTING & BOUNDARIES",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // Recipient Field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Recipient", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = recipient,
                        onValueChange = {
                            recipient = it
                            viewModel.setContext(it, destination, sourceApp)
                        },
                        placeholder = { Text("e.g., Unverified Telegram Contact, alice@company.com", fontSize = 12.sp, color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Quick Pills for Recipient
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Personal Vault (Self)", "Telegram Contact", "alice@company.com", "Support Bot").forEach { pill ->
                            ContextSuggestionPill(text = pill) {
                                recipient = pill
                                viewModel.setContext(pill, destination, sourceApp)
                            }
                        }
                    }
                }

                // Destination Field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Destination", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = destination,
                        onValueChange = {
                            destination = it
                            viewModel.setContext(recipient, it, sourceApp)
                        },
                        placeholder = { Text("e.g., Public Twitter/X Feed, Encrypted Vault", fontSize = 12.sp, color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Quick Pills for Destination
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Personal Encrypted Drive", "Unverified Telegram Chat", "Public Twitter/X Feed", "Payment Gateway").forEach { pill ->
                            ContextSuggestionPill(text = pill) {
                                destination = pill
                                viewModel.setContext(recipient, pill, sourceApp)
                            }
                        }
                    }
                }

                // Source App Field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Source App", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = sourceApp,
                        onValueChange = {
                            sourceApp = it
                            viewModel.setContext(recipient, destination, it)
                        },
                        placeholder = { Text("e.g., HDFC Mobile Banking, Chrome, WhatsApp", fontSize = 12.sp, color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Quick Pills for Source App
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("HDFC Mobile Banking", "WhatsApp", "Chrome", "DocuSign").forEach { pill ->
                            ContextSuggestionPill(text = pill) {
                                sourceApp = pill
                                viewModel.setContext(recipient, destination, pill)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // STEP 4: ANALYZE CTA
            // ==========================================
            StepHeader(stepNumber = "4", title = "Analyze")

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
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Analyze Pre-Action Risk",
                    color = BackgroundDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Animated Six-Stage Progress Overlay
        if (analysisState is UiState.Loading) {
            val msg = (analysisState as UiState.Loading).message
            SixStageProgressOverlay(currentMessage = msg)
        }
    }
}

@Composable
private fun StepHeader(stepNumber: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(CyanAccent.copy(alpha = 0.2f))
                .border(1.dp, CyanAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = CyanAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ActionChipItem(
    action: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                if (isSelected) SurfaceGlassHigh else SurfaceDark,
                RoundedCornerShape(12.dp)
            )
            .border(
                1.5.dp,
                if (isSelected) CyanAccent else SurfaceBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = action,
            color = if (isSelected) CyanAccent else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun ContextSuggestionPill(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(BackgroundDark, RoundedCornerShape(8.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}
