package com.contextguard.app.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.ActLime
import com.contextguard.app.theme.ContourBorder
import com.contextguard.app.theme.ContourBorderActive
import com.contextguard.app.theme.DeepSurface
import com.contextguard.app.theme.ElectricViolet
import com.contextguard.app.theme.ElevatedSurface
import com.contextguard.app.theme.IonCyan
import com.contextguard.app.theme.MutedText
import com.contextguard.app.theme.SignalLime
import com.contextguard.app.theme.SoftWhite
import com.contextguard.app.theme.SubtleText
import com.contextguard.app.theme.TechnicalMono
import com.contextguard.app.theme.WarnOrange

/**
 * Step 1: Spacious Artifact Intake Surface.
 *
 * Provides:
 * - Animated aperture illustration & subtle contour invitation
 * - Clear intake modes (Link, Message, File/Document, Image)
 * - Authentic artifact preview with type, size, source app
 * - Remove / Change controls
 */
@Composable
fun ArtifactIntakeSurface(
    artifactTitle: String,
    sourceApp: String,
    rawBitmap: Bitmap?,
    previewText: String?,
    fileTypeLabel: String,
    fileSizeLabel: String,
    previewMode: String, // "BEFORE" vs "AFTER"
    onPreviewModeChanged: (String) -> Unit,
    onSelectPreset: (String) -> Unit,
    onClearArtifact: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DeepSurface)
            .border(1.dp, ContourBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Artifact Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ElectricViolet.copy(alpha = 0.15f))
                        .border(1.dp, ContourBorderActive, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (rawBitmap != null) Icons.Default.Image else Icons.Default.Description,
                        contentDescription = "Artifact Type",
                        tint = IonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = artifactTitle,
                        color = SoftWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = fileTypeLabel,
                            color = IonCyan,
                            fontSize = 10.sp,
                            fontFamily = TechnicalMono,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = "•", color = SubtleText, fontSize = 10.sp)
                        Text(
                            text = fileSizeLabel,
                            color = MutedText,
                            fontSize = 10.sp,
                            fontFamily = TechnicalMono
                        )
                        Text(text = "•", color = SubtleText, fontSize = 10.sp)
                        Text(
                            text = sourceApp,
                            color = SoftWhite.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontFamily = TechnicalMono,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onClearArtifact,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear Artifact",
                        tint = MutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Before vs After Toggle Pills
            Row(
                modifier = Modifier
                    .background(ElevatedSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, ContourBorder, RoundedCornerShape(8.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (previewMode == "BEFORE") DeepSurface else Color.Transparent)
                        .clickable { onPreviewModeChanged("BEFORE") }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "BEFORE",
                        color = if (previewMode == "BEFORE") WarnOrange else SubtleText,
                        fontSize = 10.sp,
                        fontFamily = TechnicalMono,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (previewMode == "AFTER") DeepSurface else Color.Transparent)
                        .clickable { onPreviewModeChanged("AFTER") }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "AFTER",
                        color = if (previewMode == "AFTER") ActLime else SubtleText,
                        fontSize = 10.sp,
                        fontFamily = TechnicalMono,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        }

        // Preview Box (Visual Bitmap or Monospace Text)
        if (rawBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(com.contextguard.app.theme.Ink)
                    .border(1.dp, ContourBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = rawBitmap.asImageBitmap(),
                    contentDescription = "Artifact Visual Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(com.contextguard.app.theme.Ink)
                    .border(1.dp, ContourBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = previewText ?: "No artifact content available.",
                    color = if (previewMode == "AFTER") ActLime else SoftWhite,
                    fontSize = 11.5.sp,
                    fontFamily = TechnicalMono,
                    lineHeight = 16.sp
                )
            }
        }

        // Intake Shortcuts & Presets (Quick sample switches)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SELECT INTAKE ARTIFACT",
                    color = SubtleText,
                    fontSize = 10.sp,
                    fontFamily = TechnicalMono,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Sharesheet & In-App Ingestion",
                    color = IonCyan,
                    fontSize = 9.5.sp,
                    fontFamily = TechnicalMono
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IntakePresetChip(
                    label = "Bank Statement",
                    icon = Icons.Default.Description,
                    isSelected = artifactTitle.contains("Statement", true),
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectPreset("STATEMENT") }
                )
                IntakePresetChip(
                    label = "Phishing URL",
                    icon = Icons.Default.Link,
                    isSelected = artifactTitle.contains("Phish", true) || artifactTitle.contains("Link", true),
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectPreset("URL") }
                )
                IntakePresetChip(
                    label = "Urgent SMS",
                    icon = Icons.Default.Message,
                    isSelected = artifactTitle.contains("SMS", true) || artifactTitle.contains("Message", true),
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectPreset("MESSAGE") }
                )
            }
        }
    }
}

@Composable
private fun IntakePresetChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) ElectricViolet.copy(alpha = 0.18f) else ElevatedSurface)
            .border(
                1.dp,
                if (isSelected) ContourBorderActive else ContourBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) IonCyan else SubtleText,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                color = if (isSelected) SoftWhite else MutedText,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
