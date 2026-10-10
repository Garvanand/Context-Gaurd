package com.contextguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.ActLime
import com.contextguard.app.theme.AskAmber
import com.contextguard.app.theme.StopCoral
import com.contextguard.app.theme.TechnicalMono
import com.contextguard.app.theme.WarnOrange
import com.contextguard.app.ui.viewmodel.InterventionType

@Composable
fun InterventionBadge(
    intervention: InterventionType,
    modifier: Modifier = Modifier,
    large: Boolean = false
) {
    val (bgColor, borderColor, textColor, icon, label) = when (intervention) {
        InterventionType.ACT -> Tuple5(
            ActLime.copy(alpha = 0.15f),
            ActLime.copy(alpha = 0.6f),
            ActLime,
            Icons.Default.CheckCircle,
            "ACT // CLEAR SIGNAL"
        )
        InterventionType.ASK -> Tuple5(
            AskAmber.copy(alpha = 0.15f),
            AskAmber.copy(alpha = 0.6f),
            AskAmber,
            Icons.Default.Help,
            "ASK // UNRESOLVED GAP"
        )
        InterventionType.WARN -> Tuple5(
            WarnOrange.copy(alpha = 0.15f),
            WarnOrange.copy(alpha = 0.6f),
            WarnOrange,
            Icons.Default.Warning,
            "WARN // ELEVATED RISK"
        )
        InterventionType.STOP -> Tuple5(
            StopCoral.copy(alpha = 0.18f),
            StopCoral.copy(alpha = 0.7f),
            StopCoral,
            Icons.Default.Block,
            "STOP // ACTION BLOCKED"
        )
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(if (large) 10.dp else 6.dp))
            .border(1.dp, borderColor, RoundedCornerShape(if (large) 10.dp else 6.dp))
            .padding(
                horizontal = if (large) 14.dp else 8.dp,
                vertical = if (large) 8.dp else 4.dp
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (large) 8.dp else 5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(if (large) 18.dp else 13.dp)
            )
            Text(
                text = label,
                color = textColor,
                fontSize = if (large) 12.sp else 10.sp,
                fontFamily = TechnicalMono,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            )
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A, val b: B, val c: C, val d: D, val e: E
)
