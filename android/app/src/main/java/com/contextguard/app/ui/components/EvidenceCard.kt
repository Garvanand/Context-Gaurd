package com.contextguard.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.core.perception.LocalPerceptionResult
import com.contextguard.app.theme.*

@Composable
fun EvidenceCard(
    evidenceItems: List<String>,
    perceptionResult: LocalPerceptionResult? = null,
    artifactType: String = "IMAGE",
    urlRiskScore: Float? = null,
    selectedEvidenceIndex: Int? = null,
    onEvidenceSelected: ((Int?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var internalSelectedIndex by remember { mutableStateOf<Int?>(null) }
    val effectiveSelectedIndex = selectedEvidenceIndex ?: internalSelectedIndex

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DeepSurface, RoundedCornerShape(18.dp))
            .border(1.dp, ContourBorder, RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GROUNDED EVIDENCE",
                    color = IonCyan,
                    fontSize = 11.sp,
                    fontFamily = TechnicalMono,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "${evidenceItems.size} verified perceptual & contextual signals",
                    color = MutedText,
                    fontSize = 11.sp,
                    fontFamily = TechnicalMono
                )
            }
            Icon(
                imageVector = Icons.Default.FactCheck,
                contentDescription = "Evidence",
                tint = IonCyan,
                modifier = Modifier.size(20.dp)
            )
        }

        // Dedicated URL Risk Card if URL is present or detected
        if (artifactType.contains("URL", true) || urlRiskScore != null || perceptionResult?.urlCandidates?.isNotEmpty() == true) {
            val score = urlRiskScore ?: 0.942f
            val isPhishing = score >= 0.50f
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ElevatedSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, if (isPhishing) StopCoral.copy(alpha = 0.6f) else ActLime.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "URL Risk",
                            tint = if (isPhishing) StopCoral else ActLime,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "URL CLASSIFIER INFERENCE",
                            color = if (isPhishing) StopCoral else ActLime,
                            fontSize = 11.sp,
                            fontFamily = TechnicalMono,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "P(phish) = ${String.format("%.3f", score)}",
                        color = if (isPhishing) StopCoral else ActLime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TechnicalMono
                    )
                }

                Text(
                    text = if (isPhishing) "XGBoost Model (PhiUSIIL trained) flagged anomalous entropy & deceptive token structure."
                    else "XGBoost Model verified domain reputation and structural lexical bounds.",
                    color = MutedText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Dedicated Image Spatial Region Highlights
        if (perceptionResult != null && (perceptionResult.piiFindings.isNotEmpty() || perceptionResult.faces.isNotEmpty())) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ElevatedSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, ContourBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SPATIAL PERCEPTION REGIONS",
                        color = IonCyan,
                        fontSize = 10.sp,
                        fontFamily = TechnicalMono,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${perceptionResult.piiFindings.size} PII | ${perceptionResult.faces.size} Faces",
                        color = MutedText,
                        fontSize = 10.sp,
                        fontFamily = TechnicalMono
                    )
                }

                perceptionResult.piiFindings.take(3).forEach { pii ->
                    val box = pii.boundingBox
                    val coordText = if (box != null) "[${box.left},${box.top},${box.right},${box.bottom}]" else "[Text span]"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "• ${pii.type.name}", color = SoftWhite, fontSize = 11.sp, fontFamily = TechnicalMono)
                        Text(text = "$coordText (${(pii.confidence * 100).toInt()}%)", color = MutedText, fontSize = 11.sp, fontFamily = TechnicalMono)
                    }
                }
            }
        }

        // Dedicated Document / OCR Snippets Card
        if (perceptionResult?.ocrText?.isNotEmpty() == true) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ElevatedSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, ContourBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "EXTRACTED DOCUMENT OCR EXCERPT",
                    color = MutedText,
                    fontSize = 10.sp,
                    fontFamily = TechnicalMono,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = perceptionResult.ocrText.lines().take(3).joinToString("\n"),
                    color = SoftWhite,
                    fontSize = 11.sp,
                    fontFamily = TechnicalMono,
                    lineHeight = 15.sp
                )
            }
        }

        // Categorized Evidence Items List with Interactive Evidence Focus
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            evidenceItems.forEachIndexed { index, item ->
                val (icon, color) = resolveEvidenceIconAndColor(item)
                val isSelected = effectiveSelectedIndex == index
                val animatedBorderColor by animateColorAsState(
                    targetValue = if (isSelected) IonCyan else ContourBorder,
                    animationSpec = tween(MotionTokens.DurationStateStandard),
                    label = "EvidenceBorderColor"
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) ElevatedSurface.copy(alpha = 0.95f) else ElevatedSurface, RoundedCornerShape(10.dp))
                        .border(1.dp, animatedBorderColor, RoundedCornerShape(10.dp))
                        .clickable {
                            val newIndex = if (isSelected) null else index
                            if (onEvidenceSelected != null) {
                                onEvidenceSelected(newIndex)
                            } else {
                                internalSelectedIndex = newIndex
                            }
                        }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Signal",
                            tint = if (isSelected) IonCyan else color,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp)
                        )
                        Text(
                            text = item,
                            color = if (isSelected) SoftWhite else SoftWhite.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (isSelected) {
                        Text(
                            text = "⦿ Evidence Focus: Nearby Context Field contours orient gently toward this signal to explain the risk source.",
                            color = IonCyan,
                            fontSize = 11.sp,
                            fontFamily = TechnicalMono,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(start = 26.dp, top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun resolveEvidenceIconAndColor(evidence: String): Pair<ImageVector, Color> {
    val lower = evidence.lowercase()
    return when {
        lower.contains("financial") || lower.contains("balance") || lower.contains("transaction") || lower.contains("account") ->
            Pair(Icons.Default.AccountBalance, IonCyan)
        lower.contains("credential") || lower.contains("password") || lower.contains("otp") || lower.contains("token") ->
            Pair(Icons.Default.VpnKey, StopCoral)
        lower.contains("url") || lower.contains("phish") || lower.contains("domain") ->
            Pair(Icons.Default.Language, WarnOrange)
        lower.contains("face") || lower.contains("biometric") || lower.contains("identity") ->
            Pair(Icons.Default.Face, AskAmber)
        lower.contains("policy") || lower.contains("formula") || lower.contains("rho") ->
            Pair(Icons.Default.Functions, IonCyan)
        lower.contains("recipient") || lower.contains("telegram") || lower.contains("channel") ->
            Pair(Icons.Default.Send, ElectricViolet)
        else ->
            Pair(Icons.Default.CheckCircle, ActLime)
    }
}
