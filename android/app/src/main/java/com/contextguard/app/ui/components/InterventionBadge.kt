package com.contextguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.*
import com.contextguard.app.ui.viewmodel.InterventionType

@Composable
fun InterventionBadge(
    intervention: InterventionType,
    modifier: Modifier = Modifier,
    large: Boolean = false
) {
    val (bgColor, borderColor, textColor, icon, label) = when (intervention) {
        InterventionType.ACT -> Tuple5(
            ActGreen.copy(alpha = 0.15f),
            ActGreen,
            ActGreen,
            Icons.Default.CheckCircle,
            "ACT - SAFE TO PROCEED"
        )
        InterventionType.ASK -> Tuple5(
            AskYellow.copy(alpha = 0.15f),
            AskYellow,
            AskYellow,
            Icons.Default.Help,
            "ASK - CONFIRMATION NEEDED"
        )
        InterventionType.WARN -> Tuple5(
            WarnOrange.copy(alpha = 0.15f),
            WarnOrange,
            WarnOrange,
            Icons.Default.Warning,
            "WARN - HAZARD DETECTED"
        )
        InterventionType.STOP -> Tuple5(
            StopRed.copy(alpha = 0.15f),
            StopRed,
            StopRed,
            Icons.Default.Block,
            "STOP - ACTION BLOCKED"
        )
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(
                horizontal = if (large) 20.dp else 12.dp,
                vertical = if (large) 12.dp else 6.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(if (large) 24.dp else 18.dp)
            )
            Text(
                text = if (large) label else intervention.name,
                color = textColor,
                fontSize = if (large) 16.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A, val b: B, val c: C, val d: D, val e: E
)
