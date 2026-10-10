package com.contextguard.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.core.privacy.RedactionStyle
import com.contextguard.app.core.state.UiState
import com.contextguard.app.theme.ActLime
import com.contextguard.app.theme.BackgroundDark
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
import com.contextguard.app.theme.SubtleText
import com.contextguard.app.theme.TechnicalMono
import com.contextguard.app.ui.components.ActionSelectorWithConnector
import com.contextguard.app.ui.components.ArtifactIntakeSurface
import com.contextguard.app.ui.components.SixStageProgressOverlay
import com.contextguard.app.ui.components.TactilePrimaryButton
import com.contextguard.app.ui.viewmodel.MainViewModel

/**
 * Redesigned Artifact Analysis Screen.
 *
 * Implements:
 * - Step 1: Spacious Artifact Intake Surface with animated aperture illustration,
 *   clean file/link/message actions, before/after redaction inspection, and authentic metadata.
 * - Step 2: Deliberate Action Selection with dynamic animated signal connector
 *   connecting to the Context Field bus, with app capability disclosures and routing controls.
 * - Step 3: Progressive 6-stage reasoning pipeline overlay with real status disclosures and live accessibility.
 * - Tactile, spring-damped Pre-Action Evaluation trigger.
 */
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
            verticalArrangement = Arrangement.spacedBy(22.dp)
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
                            text = "Pre-Action Evaluation",
                            color = SoftWhite,
                            fontSize = 22.sp,
                            fontFamily = DisplayFont,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "SPECTRAL SIGNAL • REFINED INSTRUMENT",
                            color = IonCyan,
                            fontSize = 11.sp,
                            fontFamily = TechnicalMono,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            viewModel.cancelArtifactAnalysis()
                            onNavigateBack()
                        }
                    ) {
                        Text(
                            text = "Cancel",
                            color = MutedText,
                            fontSize = 13.sp,
                            fontFamily = TechnicalMono
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(ElevatedSurface, RoundedCornerShape(8.dp))
                            .border(1.dp, ContourBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = state.backendConfig.networkMode.name,
                            color = ActLime,
                            fontSize = 10.sp,
                            fontFamily = TechnicalMono,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ==========================================
            // STEP 1: ARTIFACT INTAKE SURFACE
            // ==========================================
            StepHeader(stepNumber = "1", title = "Artifact Intake & Perception")

            val fileType = if (state.rawBitmap != null) {
                "IMAGE / BITMAP"
            } else if (state.currentArtifactTitle.endsWith(".pdf", true)) {
                "PDF / DOCUMENT"
            } else if (state.currentArtifactTitle.contains("URL", true) || state.currentArtifactTitle.contains("http", true) || state.currentArtifactTitle.endsWith(".link", true)) {
                "URL / URI"
            } else if (state.currentArtifactTitle.isNotEmpty()) {
                "TEXT / CLIPBOARD"
            } else {
                "NO ARTIFACT LOADED"
            }

            val fileSize = if (state.rawBitmap != null) {
                "${(state.rawBitmap!!.byteCount / 1024)} KB"
            } else if (state.currentArtifactTitle.isNotEmpty()) {
                "${(state.lastPerceptionResult?.ocrText?.length ?: 42) * 2} B"
            } else {
                "0 B"
            }

            val previewText = if (state.currentArtifactTitle.isEmpty() && state.rawBitmap == null) {
                "No active artifact loaded. Paste a link or message below, or select a synthetic test fixture."
            } else if (previewMode == "AFTER") {
                state.lastRedactionResult?.redactedText
                    ?: state.lastPerceptionResult?.ocrText
                    ?: "Sensitive fields masked via local perception pipeline."
            } else {
                state.lastPerceptionResult?.ocrText?.ifEmpty {
                    "Artifact text extracted into RAM buffer."
                } ?: "Artifact text extracted into RAM buffer."
            }

            ArtifactIntakeSurface(
                artifactTitle = state.currentArtifactTitle.ifEmpty { "Manual Inspection Workspace" },
                sourceApp = state.currentSourceApp.ifEmpty { "ContextGuard Direct" },
                rawBitmap = state.rawBitmap,
                previewText = previewText,
                fileTypeLabel = fileType,
                fileSizeLabel = fileSize,
                previewMode = previewMode,
                onPreviewModeChanged = { previewMode = it },
                onSelectPreset = { preset ->
                    when (preset) {
                        "BANK_STATEMENT" -> {
                            viewModel.processTextArtifact(
                                text = "HDFC Bank Statement - Oct 2026\nAccount: 4532 0150 1234 5671 | IFSC: HDFC0000128\nAvailable Balance: INR 1,48,290.40",
                                title = "HDFC_Statement_Oct2026.pdf",
                                sourceApp = "HDFC Mobile Banking"
                            )
                        }
                        "PHISHING_URL" -> {
                            viewModel.processTextArtifact(
                                text = "https://secure-login-hdfcbank-verify.top/auth/session?id=98214",
                                title = "suspicious_login_url.txt",
                                sourceApp = "Chrome"
                            )
                        }
                        "URGENT_SMS" -> {
                            viewModel.processTextArtifact(
                                text = "URGENT: Your HDFC account has been locked. Verify OTP 492018 at http://bit.ly/hdfc-fix to unlock immediately.",
                                title = "Urgent_SMS_Alert.txt",
                                sourceApp = "Messages"
                            )
                        }
                    }
                },
                onClearArtifact = {
                    viewModel.cancelArtifactAnalysis()
                }
            )

            // Direct input field for pasting links/messages when opened without share
            if (!state.hasActiveArtifact && state.currentArtifactTitle.isEmpty()) {
                var manualInputText by remember { mutableStateOf("") }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DeepSurface)
                        .border(1.dp, ContourBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "PASTE URL OR MESSAGE TO INSPECT",
                        color = IonCyan,
                        fontSize = 11.sp,
                        fontFamily = TechnicalMono,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = manualInputText,
                        onValueChange = { manualInputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Paste candidate link or message...", color = SubtleText, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SoftWhite,
                            unfocusedTextColor = SoftWhite,
                            focusedBorderColor = ContourBorderActive,
                            unfocusedBorderColor = ContourBorder
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (manualInputText.isNotBlank()) {
                                    val isUrl = manualInputText.startsWith("http://") || manualInputText.startsWith("https://")
                                    viewModel.processTextArtifact(
                                        text = manualInputText,
                                        title = if (isUrl) "User_Candidate_Url.link" else "User_Message.txt",
                                        sourceApp = "Direct Inspection"
                                    )
                                }
                            },
                            enabled = manualInputText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Inspect Content", color = SoftWhite, fontSize = 11.sp, fontFamily = TechnicalMono)
                        }
                    }
                }
            }

            // Redaction Style Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeepSurface)
                    .border(1.dp, ContourBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ON-DEVICE REDACTION ENGINE",
                        color = IonCyan,
                        fontSize = 10.sp,
                        fontFamily = TechnicalMono,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${state.maskedPiiCount} masked",
                            color = ActLime,
                            fontSize = 11.sp,
                            fontFamily = TechnicalMono,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = state.isRedactionEnabled,
                            onCheckedChange = { viewModel.toggleRedaction(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = IonCyan,
                                checkedTrackColor = ElectricViolet
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mask Style:",
                        color = MutedText,
                        fontSize = 12.sp,
                        fontFamily = TechnicalMono
                    )
                    FilterChip(
                        selected = state.redactionStyle == RedactionStyle.BLACKOUT,
                        onClick = { viewModel.setRedactionStyle(RedactionStyle.BLACKOUT) },
                        label = { Text("Blackout", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricViolet.copy(alpha = 0.3f),
                            selectedLabelColor = IonCyan
                        )
                    )
                    FilterChip(
                        selected = state.redactionStyle == RedactionStyle.BLUR,
                        onClick = { viewModel.setRedactionStyle(RedactionStyle.BLUR) },
                        label = { Text("Blur", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricViolet.copy(alpha = 0.3f),
                            selectedLabelColor = IonCyan
                        )
                    )
                }
            }

            // ==========================================
            // STEP 2: ACTION INTENT SELECTION & BUS
            // ==========================================
            StepHeader(stepNumber = "2", title = "Action Intent & Signal Bus")

            Text(
                text = "⚡ Select your intended action to immediately evaluate pre-action safety risk.",
                color = IonCyan,
                fontSize = 11.5.sp,
                fontFamily = TechnicalMono
            )

            ActionSelectorWithConnector(
                selectedAction = state.selectedAction,
                onActionSelected = { descriptor ->
                    viewModel.setActionChip(descriptor.name)
                    val targetDest = if (destination.isEmpty() || destination == "Personal Encrypted Drive" || destination == "Unverified Telegram Contact") {
                        descriptor.defaultDestination
                    } else {
                        destination
                    }
                    destination = targetDest
                    viewModel.setContext(recipient, targetDest, sourceApp)

                    // Automatic analysis trigger! Removed the unnecessary manual "Analyze Now" bottleneck.
                    viewModel.executeAnalysis {
                        onResultReady()
                    }
                },
                recipient = recipient,
                onRecipientChanged = {
                    recipient = it
                    viewModel.setContext(it, destination, sourceApp)
                },
                destination = destination,
                onDestinationChanged = {
                    destination = it
                    viewModel.setContext(recipient, it, sourceApp)
                },
                sourceApp = sourceApp,
                onSourceAppChanged = {
                    sourceApp = it
                    viewModel.setContext(recipient, destination, it)
                }
            )

            // Contextual re-evaluation trigger (only if user manually modified destination or context)
            if (state.selectedAction.isNotBlank()) {
                StepHeader(stepNumber = "3", title = "Contextual Re-Evaluation")

                TactilePrimaryButton(
                    title = "Re-evaluate with Updated Context",
                    subtitle = "Re-evaluate policy on '${state.selectedAction}' ⟼ '$destination'",
                    icon = Icons.Default.Shield,
                    isPrimary = false,
                    onClick = {
                        viewModel.executeAnalysis {
                            onResultReady()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ==========================================
        // PROGRESSIVE 6-STAGE REASONING OVERLAY
        // ==========================================
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
                .background(ElectricViolet.copy(alpha = 0.2f))
                .border(1.dp, ContourBorderActive, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = IonCyan,
                fontSize = 12.sp,
                fontFamily = TechnicalMono,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = title,
            color = SoftWhite,
            fontSize = 16.sp,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.Bold
        )
    }
}
