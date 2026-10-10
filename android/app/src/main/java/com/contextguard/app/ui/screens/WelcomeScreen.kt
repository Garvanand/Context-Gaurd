package com.contextguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.*
import com.contextguard.app.ui.components.ApertureSignalLogoReveal

@Composable
fun WelcomeScreen(
    onEnterClicked: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(DeepSurface, RoundedCornerShape(24.dp))
                    .border(1.dp, ContourBorderActive, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                ApertureSignalLogoReveal(size = 72.dp)
            }

            Text(
                text = "CONTEXTGUARD",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )

            Text(
                text = "A Multimodal AI System for Pre-Action Risk Detection in Everyday Digital Tasks",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }

        // Central Hypothesis Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp)
                .background(SurfaceDark, RoundedCornerShape(20.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "CORE RESEARCH HYPOTHESIS",
                color = CyanAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Text(
                text = "The risk of a digital artifact cannot always be determined from the artifact alone.\n\nThe intended action and surrounding context determine the appropriate safety intervention.",
                color = TextPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            HorizontalDivider(color = DividerColor)

            Text(
                text = "Deterministic Action Policy: rho = s * (1 + lambda * r)",
                color = TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "ACT", color = ActGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "ASK", color = AskYellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "WARN", color = WarnOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "STOP", color = StopRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Button(
            onClick = onEnterClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Launch System",
                    color = BackgroundDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Forward",
                    tint = BackgroundDark
                )
            }
        }
    }
}
