package com.contextguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.*
import com.contextguard.app.ui.viewmodel.MainViewModel

@Composable
fun PrivacyScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.appState.collectAsState()
    val scrollState = rememberScrollState()

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
                text = "Privacy & Redaction Protocol",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Guarantee Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceGlass, RoundedCornerShape(14.dp))
                .border(1.dp, ActGreen.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = "Privacy Verified",
                tint = ActGreen,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = "Zero Raw Persistence Invariant",
                    color = ActGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "No raw artifacts are stored on disk or sent unredacted.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Technical Invariants List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "TECHNICAL PRIVACY GUARANTEES",
                color = CyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            PrivacyRow(
                title = "1. On-Device Edge Perception",
                detail = "ML Kit Text Recognition & Face Detection run directly in memory on the Android device."
            )
            PrivacyRow(
                title = "2. Pre-Upload Canvas Masking",
                detail = "Detected financial account numbers and faces are masked on the local bitmap before transmission."
            )
            PrivacyRow(
                title = "3. Volatile RAM Processing",
                detail = "Buffers exist only during inference lifecycle and are discarded immediately after policy calculation."
            )
            PrivacyRow(
                title = "4. Cryptographic Telemetry Only",
                detail = "Logs record only the SHA-256 digest of artifacts. No plaintext or unmasked pixels leave device."
            )
        }

        // Audit Log Sample Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "NETWORK AUDIT LOG PREVIEW",
                color = CyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Text(
                text = "Active Target: ${state.backendConfig.baseUrl}/api/v1/analyze",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = "Digest: SHA-256 (32 bytes cryptographic hash)",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = "Redaction Status: Masked (${state.maskedPiiCount} PII entities)",
                color = ActGreen,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PrivacyRow(title: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = detail,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}
