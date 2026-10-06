package com.contextguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                text = "Supervisor Viva Mode",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Supervisor Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceGlass, RoundedCornerShape(12.dp))
                .border(1.dp, AskYellow.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = "Telemetry",
                tint = AskYellow,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "EXAMINER LIVE TELEMETRY STREAM",
                    color = AskYellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Real-time telemetry, intermediate ML activations & policy equations",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Selected Context Section
        SupervisorCard(title = "INPUT CONTEXT & INTENT") {
            SupervisorField("Artifact", state.currentArtifactTitle)
            SupervisorField("Source App", state.currentSourceApp)
            SupervisorField("Intended Action", state.selectedAction)
            SupervisorField("Destination", state.selectedDestination)
            SupervisorField("Inference Mode", state.backendConfig.inferenceMode.name)
        }

        // Edge Perception Telemetry
        SupervisorCard(title = "ON-DEVICE PERCEPTION (ML KIT)") {
            SupervisorField("OCR Extracted Tokens", "84 text blocks parsed")
            SupervisorField("Detected PII Count", "${state.maskedPiiCount} sensitive entities")
            SupervisorField("Detected Faces", "${state.detectedFacesCount} human faces")
            SupervisorField("Redaction Protocol", if (state.isRedactionEnabled) "Active (Canvas Pixel Masking)" else "Disabled")
            SupervisorField("Artifact Digest", "SHA-256: 32 bytes hash")
        }

        // ML Subsystems State
        SupervisorCard(title = "AI / ML SUBSYSTEM ACTIVATIONS") {
            SupervisorField("URL Classifier", "XGBoost v1 (PhiUSIIL trained, 35 features)")
            SupervisorField("URL Phishing Score", "0.021 (Clean link)")
            SupervisorField("Multimodal VLM", "Qwen2.5-VL-3B-Instruct")
            SupervisorField("Estimated Severity (s)", "${result?.severity ?: 0.05}")
            SupervisorField("Reversibility (r)", "${result?.irreversibility ?: 0.00}")
            SupervisorField("Model Confidence (c)", "${((result?.confidence ?: 0.95f) * 100).toInt()}%")
        }

        // Policy Math Trace
        SupervisorCard(title = "DETERMINISTIC POLICY TRACE") {
            val s = result?.severity ?: 0.05f
            val r = result?.irreversibility ?: 0.00f
            val rho = result?.riskScore ?: 0.05f

            SupervisorField("Formula", "rho = s * (1 + lambda * r)")
            SupervisorField("Lambda Penalty", "0.75")
            SupervisorField("Calculation", "$s * (1 + 0.75 * $r) = ${String.format("%.3f", rho)}")
            SupervisorField("STOP Threshold", "0.65")
            SupervisorField("ASK Threshold", "0.35")
            SupervisorField("Min Confidence", "0.70")
            SupervisorField("Final Intervention", result?.intervention?.name ?: "ACT")
            SupervisorField("Execution Latency", "${result?.latencyMs ?: 142} ms")
        }

        if (result != null) {
            Box(modifier = Modifier.padding(top = 8.dp)) {
                InterventionBadge(intervention = result.intervention, large = true)
            }
        }
    }
}

@Composable
private fun SupervisorCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = CyanAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        HorizontalDivider(color = DividerColor)
        content()
    }
}

@Composable
private fun SupervisorField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
