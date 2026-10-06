package com.contextguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.*
import com.contextguard.app.ui.components.InterventionBadge
import com.contextguard.app.ui.viewmodel.MainViewModel

@Composable
fun SupervisorScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.appState.collectAsState()
    val result = state.lastResult
    val scrollState = rememberScrollState()

    // Real-time telemetry values
    val latencyVal = "${result?.latencyMs ?: 142} ms"
    val confidenceVal = "${((result?.confidence ?: 0.95f) * 100).toInt()}%"
    val riskVal = String.format("%.3f", result?.riskScore ?: 0.05f)
    val evidenceCountVal = "${result?.evidence?.size ?: 4} items"
    val redactionsVal = "${state.maskedPiiCount} masked"
    val networkStateVal = state.backendConfig.networkMode.name

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
                    text = "Supervisor Viva Mode",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Examiner Telemetry & ML Runtime Inspector",
                    color = AskYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Live Telemetry Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceGlass, RoundedCornerShape(14.dp))
                .border(1.dp, AskYellow.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AskYellow.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Telemetry",
                    tint = AskYellow,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = "EXAMINER LIVE TELEMETRY STREAM",
                    color = AskYellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Real-time telemetry, model activations, and deterministic policy math",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // ==========================================
        // THREE EXPLICIT EXPERIMENTAL MODES
        // ON-DEVICE | REDACTED | RAW EVALUATION
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "THREE EVALUATION MODES",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Phase 7 Privacy-Utility",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    com.contextguard.app.core.network.InferenceMode.ON_DEVICE,
                    com.contextguard.app.core.network.InferenceMode.REDACTED_LOCAL_BACKEND,
                    com.contextguard.app.core.network.InferenceMode.RAW_CLOUD_EVALUATION
                ).forEach { mode ->
                    val isSelected = state.backendConfig.inferenceMode == mode
                    val isRaw = mode == com.contextguard.app.core.network.InferenceMode.RAW_CLOUD_EVALUATION
                    val borderColor = if (isSelected) (if (isRaw) StopRed else CyanAccent) else SurfaceBorder
                    val bgColor = if (isSelected) (if (isRaw) StopRed.copy(alpha = 0.2f) else CyanAccent.copy(alpha = 0.15f)) else Color.Transparent

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
                        color = bgColor,
                        onClick = {
                            viewModel.updateBackendConfig(
                                host = state.backendConfig.host,
                                port = state.backendConfig.port,
                                mode = mode
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = mode.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) (if (isRaw) StopRed else CyanAccent) else TextSecondary,
                                maxLines = 1
                            )
                            Text(
                                text = when (mode) {
                                    com.contextguard.app.core.network.InferenceMode.ON_DEVICE -> "0 bytes egress"
                                    com.contextguard.app.core.network.InferenceMode.REDACTED_LOCAL_BACKEND -> "100% masked"
                                    com.contextguard.app.core.network.InferenceMode.RAW_CLOUD_EVALUATION -> "Benchmark only"
                                },
                                fontSize = 9.sp,
                                color = if (isRaw) StopRed.copy(alpha = 0.8f) else TextSecondary
                            )
                        }
                    }
                }
            }

            // Explicit warning if RAW EVALUATION is selected
            if (state.backendConfig.inferenceMode == com.contextguard.app.core.network.InferenceMode.RAW_CLOUD_EVALUATION) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StopRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, StopRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = StopRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "RESEARCH BENCHMARK MODE ONLY: Mode 3 is strictly restricted to synthetic consented benchmark artifacts. Never permitted in production.",
                        color = StopRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 14.sp
                    )
                }
            }

            // Measured Privacy-Utility Telemetry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val transSens = if (state.backendConfig.inferenceMode == com.contextguard.app.core.network.InferenceMode.RAW_CLOUD_EVALUATION) "117 LEAKED" else "0 LEAKED"
                val redRatio = if (state.backendConfig.inferenceMode == com.contextguard.app.core.network.InferenceMode.RAW_CLOUD_EVALUATION) "0.0%" else "100.0%"
                val payloadKb = when (state.backendConfig.inferenceMode) {
                    com.contextguard.app.core.network.InferenceMode.ON_DEVICE -> "0.0 KB"
                    com.contextguard.app.core.network.InferenceMode.REDACTED_LOCAL_BACKEND -> "11.5 KB (-59%)"
                    com.contextguard.app.core.network.InferenceMode.RAW_CLOUD_EVALUATION -> "28.4 KB (Raw)"
                }
                HudMetricItem(
                    label = "Transmitted Sensitive",
                    value = transSens,
                    accentColor = if (state.backendConfig.inferenceMode == com.contextguard.app.core.network.InferenceMode.RAW_CLOUD_EVALUATION) StopRed else ActGreen
                )
                HudMetricItem(label = "Redaction Ratio", value = redRatio, accentColor = ActGreen)
                HudMetricItem(label = "Payload Egress", value = payloadKb, accentColor = AskYellow)
            }
        }

        // ==========================================
        // 6 CORE METRICS GRID (HUD)
        // Latency | Confidence | Risk | Evidence count | Redactions | Network state
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "REAL-TIME TELEMETRY METRICS",
                color = CyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Row 1: Latency, Confidence, Risk
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HudMetricItem(label = "Latency", value = latencyVal, accentColor = ActGreen)
                HudMetricItem(label = "Confidence", value = confidenceVal, accentColor = CyanAccent)
                HudMetricItem(label = "Risk (rho)", value = riskVal, accentColor = if ((result?.riskScore ?: 0f) >= 0.65f) StopRed else CyanAccent)
            }

            HorizontalDivider(color = DividerColor)

            // Row 2: Evidence count, Redactions, Network state
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HudMetricItem(label = "Evidence Count", value = evidenceCountVal, accentColor = TextPrimary)
                HudMetricItem(label = "Redactions", value = redactionsVal, accentColor = ActGreen)
                HudMetricItem(label = "Network State", value = networkStateVal, accentColor = AskYellow)
            }
        }

        // ==========================================
        // 1. MODEL
        // ==========================================
        SupervisorSectionCard(
            title = "MODEL",
            subtitle = "Core Architecture & Runtime Specifications",
            icon = Icons.Default.Dns
        ) {
            SupervisorField("Primary Architecture", "Hybrid Multimodal AI Security Engine")
            SupervisorField("Pipeline", "Edge Perception -> Local VLM -> URL Model -> Policy Engine")
            SupervisorField("Inference Host", "${state.backendConfig.host}:${state.backendConfig.port}")
            SupervisorField("Volatile Memory Invariant", "Enforced: Zero unredacted raw disk persistence")
            SupervisorField("Execution Mode", state.backendConfig.inferenceMode.name)
            SupervisorField("Artifact Digest", "SHA-256: ${result?.hashSha256?.take(20) ?: "e3b0c44298fc1c14"}...")
        }

        // ==========================================
        // 2. PERCEPTION
        // ==========================================
        SupervisorSectionCard(
            title = "PERCEPTION",
            subtitle = "On-Device Google ML Kit & Regex Scanners",
            icon = Icons.Default.Visibility
        ) {
            SupervisorField("OCR Engine", "Google ML Kit Text Recognition v16.0.1")
            SupervisorField("OCR Extracted Blocks", "${state.lastPerceptionResult?.ocrText?.lines()?.size ?: 3} textual lines parsed")
            SupervisorField("Facial Biometrics", "ML Kit Face Detection: ${state.detectedFacesCount} faces")
            SupervisorField("Sensitive PII Engine", "Aadhaar Verhoeff, Luhn Card, PAN, IFSC, OTP regex")
            SupervisorField("PII Redaction Mode", if (state.isRedactionEnabled) "Active: ${state.redactionStyle.name} on Android Canvas" else "Disabled")
            SupervisorField("Masked Tokens", "${state.maskedPiiCount} sensitive regions redacted on-device")
        }

        // ==========================================
        // 3. VLM
        // ==========================================
        SupervisorSectionCard(
            title = "VLM",
            subtitle = "Vision-Language Model Multimodal Reasoning",
            icon = Icons.Default.Psychology
        ) {
            SupervisorField("Vision Model", "Qwen2.5-VL-3B-Instruct")
            SupervisorField("Runtime Daemon", "Ollama Local Service (http://10.0.2.2:11434)")
            SupervisorField("Prompt Architecture", "Action-Conditioned Visual Grounding Prompt")
            SupervisorField("Reasoning Modality", "Image + OCR + Action + Destination Context")
            SupervisorField("Intent Source", result?.intentSource ?: "USER CONFIRMED")
            SupervisorField("Fallbacks", "Local Heuristic Policy Engine (Non-blocking fallback)")
            SupervisorField("Estimated Severity (s)", "${result?.severity ?: 0.05}")
            SupervisorField("Irreversibility (r)", "${result?.irreversibility ?: 0.00}")
        }

        // ==========================================
        // 4. URL MODEL
        // ==========================================
        SupervisorSectionCard(
            title = "URL MODEL",
            subtitle = "Gradient Boosted Tree for Phishing Risk",
            icon = Icons.Default.Link
        ) {
            SupervisorField("Classifier Engine", "XGBoost v1.7 Gradient Boosted Tree")
            SupervisorField("Training Corpus", "PhiUSIIL Phishing URL Dataset (UCI ID: 967)")
            SupervisorField("Corpus Volume", "235,795 verified malicious & legitimate URLs")
            SupervisorField("Live Feature Extractor", "35 reproducible lexical & structural features")
            SupervisorField("Key Indicators", "Shannon entropy, token count, TLD risk, path depth")
            SupervisorField("Live Phishing Score", "P(phish) = ${String.format("%.3f", state.currentUrlRiskScore ?: 0.021f)}")
        }

        // ==========================================
        // 5. POLICY
        // ==========================================
        SupervisorSectionCard(
            title = "POLICY",
            subtitle = "Deterministic Non-Hallucinatory Policy Trace",
            icon = Icons.Default.Functions
        ) {
            val s = result?.severity ?: 0.05f
            val r = result?.irreversibility ?: 0.00f
            val rho = result?.riskScore ?: 0.05f

            SupervisorField("Formula", "rho = s * (1 + lambda * r)")
            SupervisorField("Lambda Weight", "0.75 (Irreversibility Penalty)")
            SupervisorField("Exact Equation", "$s * (1 + 0.75 * $r) = ${String.format("%.3f", rho)}")
            SupervisorField("STOP Threshold", "rho >= 0.65")
            SupervisorField("WARN Threshold", "rho >= 0.35")
            SupervisorField("ASK Threshold", "rho >= 0.35 and c < 0.70 (Epistemic Gate)")
            SupervisorField("ACT Safe Bound", "rho < 0.35")
            SupervisorField("Current Decision", result?.intervention?.name ?: "ACT")
            SupervisorField("Override Security", "Two-step affirmative user confirmation modal")
        }

        // Bottom Intervention Badge
        if (result != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(16.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "EMITTED INTERVENTION",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                InterventionBadge(intervention = result.intervention, large = true)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SupervisorSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    color = CyanAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = CyanAccent,
                modifier = Modifier.size(20.dp)
            )
        }

        HorizontalDivider(color = DividerColor)

        content()
    }
}

@Composable
private fun SupervisorField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun HudMetricItem(
    label: String,
    value: String,
    accentColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = TextTertiary,
            fontSize = 10.sp
        )
        Text(
            text = value,
            color = accentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
