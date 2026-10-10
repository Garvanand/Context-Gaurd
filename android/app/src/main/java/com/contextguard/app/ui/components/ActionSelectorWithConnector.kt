package com.contextguard.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.ActLime
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
import com.contextguard.app.theme.StopCoral
import com.contextguard.app.theme.SubtleText
import com.contextguard.app.theme.TechnicalMono
import com.contextguard.app.theme.WarnOrange

data class ActionDescriptor(
    val name: String,
    val icon: ImageVector,
    val perimeterLabel: String,
    val supportedApps: String,
    val inherentReversibility: String,
    val defaultDestination: String
)

val ACTION_CATALOG = listOf(
    ActionDescriptor(
        name = "SAVE",
        icon = Icons.Default.Save,
        perimeterLabel = "Local Vault",
        supportedApps = "Files, Keep, Vault",
        inherentReversibility = "r = 0.00 (Fully Reversible)",
        defaultDestination = "Personal Encrypted Drive"
    ),
    ActionDescriptor(
        name = "SEND",
        icon = Icons.Default.Send,
        perimeterLabel = "Direct Channel",
        supportedApps = "Messages, Telegram, WhatsApp",
        inherentReversibility = "r = 0.50 (Unverified Recipient)",
        defaultDestination = "Unverified Telegram Contact"
    ),
    ActionDescriptor(
        name = "UPLOAD",
        icon = Icons.Default.CloudUpload,
        perimeterLabel = "Cloud Perimeter",
        supportedApps = "Drive, Dropbox, S3",
        inherentReversibility = "r = 0.40 - 0.95 (Host Dependent)",
        defaultDestination = "Cloud Storage Perimeter"
    ),
    ActionDescriptor(
        name = "POST",
        icon = Icons.Default.Public,
        perimeterLabel = "Public Broadcast",
        supportedApps = "X/Twitter, LinkedIn, Forums",
        inherentReversibility = "r = 1.00 (Irreversible)",
        defaultDestination = "Public Twitter/X Feed"
    ),
    ActionDescriptor(
        name = "SIGN",
        icon = Icons.Default.Draw,
        perimeterLabel = "Legal Identity",
        supportedApps = "DocuSign, Adobe Acrobat",
        inherentReversibility = "r = 0.85 (High Commitment)",
        defaultDestination = "Contract Recipient"
    ),
    ActionDescriptor(
        name = "LOGIN",
        icon = Icons.Default.Lock,
        perimeterLabel = "Credential Channel",
        supportedApps = "Browsers, Auth Portals",
        inherentReversibility = "r = 0.90 (Session Binding)",
        defaultDestination = "Authentication Gateway"
    ),
    ActionDescriptor(
        name = "APPROVE",
        icon = Icons.Default.AssignmentTurnedIn,
        perimeterLabel = "Authorization Gate",
        supportedApps = "Banking, UPI, Authenticator",
        inherentReversibility = "r = 0.90 (Fund Transfer)",
        defaultDestination = "Payment Gateway"
    ),
    ActionDescriptor(
        name = "OPEN",
        icon = Icons.Default.OpenInBrowser,
        perimeterLabel = "External Link/URI",
        supportedApps = "Chrome, Firefox, Link Handlers",
        inherentReversibility = "r = 0.00 (Navigation Only)",
        defaultDestination = "Web Browser URL"
    )
)

/**
 * Step 2: Action Selection with Animated Signal Connector.
 *
 * Implements:
 * - 8 deliberate, visually strong action selectors: SAVE, SEND, UPLOAD, POST, SIGN, LOGIN, APPROVE, OPEN.
 * - Dynamic Vector Connector: traces an animated contour from the selected action into the Context Field anchor.
 * - App capability honesty: explicitly notes which apps support the action, preventing false universal claims.
 * - Routing context (Recipient, Destination, Channel visibility).
 */
@Composable
fun ActionSelectorWithConnector(
    selectedAction: String,
    onActionSelected: (ActionDescriptor) -> Unit,
    recipient: String,
    onRecipientChanged: (String) -> Unit,
    destination: String,
    onDestinationChanged: (String) -> Unit,
    sourceApp: String,
    onSourceAppChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeDescriptor = ACTION_CATALOG.firstOrNull { it.name.equals(selectedAction, ignoreCase = true) }
        ?: ACTION_CATALOG.first()

    val infiniteTransition = rememberInfiniteTransition(label = "ActionConnectorPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseProgress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DeepSurface)
            .border(1.dp, ContourBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "INTENDED ACTION",
                    color = IonCyan,
                    fontSize = 11.sp,
                    fontFamily = TechnicalMono,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Action conditions the entire safety calculation",
                    color = MutedText,
                    fontSize = 12.sp,
                    fontFamily = BodyFont
                )
            }

            Box(
                modifier = Modifier
                    .background(ElevatedSurface, RoundedCornerShape(6.dp))
                    .border(1.dp, ContourBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = activeDescriptor.perimeterLabel.uppercase(),
                    color = IonCyan,
                    fontSize = 10.sp,
                    fontFamily = TechnicalMono,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 8 Action Buttons Grid (2 rows of 4)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val row1 = ACTION_CATALOG.take(4)
            val row2 = ACTION_CATALOG.drop(4)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row1.forEach { descriptor ->
                    val isSelected = descriptor.name.equals(selectedAction, ignoreCase = true)
                    ActionCardItem(
                        descriptor = descriptor,
                        isSelected = isSelected,
                        modifier = Modifier.weight(1f),
                        onClick = { onActionSelected(descriptor) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row2.forEach { descriptor ->
                    val isSelected = descriptor.name.equals(selectedAction, ignoreCase = true)
                    ActionCardItem(
                        descriptor = descriptor,
                        isSelected = isSelected,
                        modifier = Modifier.weight(1f),
                        onClick = { onActionSelected(descriptor) }
                    )
                }
            }
        }

        // ==========================================
        // DYNAMIC SIGNAL CONNECTOR TO CONTEXT FIELD
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ElevatedSurface)
                .border(1.dp, ContourBorderActive.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height
                val midY = height / 2f

                // Draw background dashed guide line
                drawLine(
                    color = ContourBorder,
                    start = Offset(20f, midY),
                    end = Offset(width - 20f, midY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Animated signal pulse wave travelling towards Context Field
                val pulseX = 20f + (width - 40f) * pulseProgress
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(IonCyan, ElectricViolet, Color.Transparent),
                        center = Offset(pulseX, midY),
                        radius = 28f
                    ),
                    radius = 18f,
                    center = Offset(pulseX, midY)
                )

                // Leading node
                drawCircle(
                    color = IonCyan,
                    radius = 3.5.dp.toPx(),
                    center = Offset(pulseX, midY)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(IonCyan)
                    )
                    Text(
                        text = "INTENT: ${activeDescriptor.name}",
                        color = SoftWhite,
                        fontSize = 11.sp,
                        fontFamily = TechnicalMono,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "SIGNAL BUS CONNECTED ⟼ CONTEXT FIELD",
                    color = IonCyan,
                    fontSize = 9.sp,
                    fontFamily = TechnicalMono,
                    letterSpacing = 1.sp
                )
            }
        }

        // App Capability Disclosure Note
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepSurface.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                .border(1.dp, ContourBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(WarnOrange)
            )
            Text(
                text = "Supported apps: ${activeDescriptor.supportedApps} • ${activeDescriptor.inherentReversibility}",
                color = SubtleText,
                fontSize = 11.sp,
                fontFamily = TechnicalMono,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ==========================================
        // ROUTING & DESTINATION CONTEXT FIELDS
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "ROUTING & BOUNDARY CONTEXT",
                color = IonCyan,
                fontSize = 10.sp,
                fontFamily = TechnicalMono,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Destination Field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Destination Channel",
                    color = SoftWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = destination,
                    onValueChange = onDestinationChanged,
                    placeholder = {
                        Text(
                            activeDescriptor.defaultDestination,
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IonCyan,
                        unfocusedBorderColor = ContourBorder,
                        focusedTextColor = SoftWhite,
                        unfocusedTextColor = SoftWhite,
                        cursorColor = IonCyan
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                // Quick Destination Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Personal Encrypted Drive",
                        "Unverified Telegram Contact",
                        "Public Twitter/X Feed",
                        "Internal Company Slack"
                    ).forEach { preset ->
                        DestinationPill(
                            label = preset,
                            isSelected = destination == preset,
                            onClick = { onDestinationChanged(preset) }
                        )
                    }
                }
            }

            // Recipient Field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Recipient / Target",
                    color = SoftWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = recipient,
                    onValueChange = onRecipientChanged,
                    placeholder = {
                        Text(
                            "e.g., Self / Personal Vault, +91 98765 43210, support@service.io",
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IonCyan,
                        unfocusedBorderColor = ContourBorder,
                        focusedTextColor = SoftWhite,
                        unfocusedTextColor = SoftWhite,
                        cursorColor = IonCyan
                    ),
                )
            }

            // Source App Field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Originating App",
                    color = SoftWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = sourceApp,
                    onValueChange = onSourceAppChanged,
                    placeholder = {
                        Text(
                            "e.g., HDFC Mobile Banking, WhatsApp, Chrome",
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IonCyan,
                        unfocusedBorderColor = ContourBorder,
                        focusedTextColor = SoftWhite,
                        unfocusedTextColor = SoftWhite,
                        cursorColor = IonCyan
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionCardItem(
    descriptor: ActionDescriptor,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) ContourBorderActive else ContourBorder,
        label = "BorderColor"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) ElevatedSurface else DeepSurface,
        label = "BgColor"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .semantics { contentDescription = "Select action ${descriptor.name}" },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = descriptor.icon,
                contentDescription = descriptor.name,
                tint = if (isSelected) IonCyan else MutedText,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = descriptor.name,
                color = if (isSelected) SoftWhite else MutedText,
                fontSize = 11.sp,
                fontFamily = TechnicalMono,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DestinationPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) ElectricViolet.copy(alpha = 0.25f) else ElevatedSurface)
            .border(
                1.dp,
                if (isSelected) ContourBorderActive else ContourBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) IonCyan else SubtleText,
            fontSize = 10.sp,
            fontFamily = TechnicalMono,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
