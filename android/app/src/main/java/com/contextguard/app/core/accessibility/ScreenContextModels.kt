package com.contextguard.app.core.accessibility

import java.util.UUID

/**
 * Canonical pre-action candidate types observable on mobile screens.
 */
enum class ScreenActionType {
    SEND,
    POST,
    UPLOAD,
    LOGIN,
    APPROVE,
    PAY,
    SIGN,
    OPEN;

    companion object {
        fun fromString(action: String?): ScreenActionType {
            if (action == null) return OPEN
            return try {
                valueOf(action.trim().uppercase())
            } catch (e: Exception) {
                OPEN
            }
        }
    }
}

/**
 * Structured screen context metadata produced by ScreenContextExtractor.
 *
 * PRIVACY INVARIANTS:
 * - Does NOT store raw AccessibilityNodeInfo trees.
 * - NEVER contains password contents or sensitive text from password controls.
 * - If root is missing or screen is secure/inaccessible, inspectionUnavailable = true.
 */
data class ScreenContextMetadata(
    val packageName: String,
    val screenIdentifier: String? = null,
    val visibleTextFragments: List<String> = emptyList(),
    val buttonLabels: List<String> = emptyList(),
    val accessibilityRoles: List<String> = emptyList(),
    val screenDimensions: Pair<Int, Int>? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val candidateAction: ScreenActionType = ScreenActionType.OPEN,
    val evidenceSource: String = "ACCESSIBILITY_TREE",
    val inspectionErrors: List<String> = emptyList(),
    val inspectionUnavailable: Boolean = false,
    val activeUrl: String? = null
)

/**
 * Privacy-safe ephemeral event audit record.
 * INVARIANT: Never stores raw text, passwords, or messages. Only metadata.
 */
data class AuditLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val packageName: String,
    val screenIdentifier: String?,
    val candidateAction: String,
    val intervention: String,
    val riskScore: Float,
    val latencyMs: Long,
    val evidenceSummary: String
)
