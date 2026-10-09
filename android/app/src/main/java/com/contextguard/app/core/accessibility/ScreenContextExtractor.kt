package com.contextguard.app.core.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.contextguard.app.core.event.AccessibilityEventFilter

/**
 * Extracts structured, ephemeral metadata from accessible Android UI trees.
 *
 * STRICT PRIVACY & SAFETY RULES:
 * - Does NOT store raw AccessibilityNodeInfo trees.
 * - NEVER reads or buffers password fields (isPassword == true or password inputTypes).
 * - Avoids capturing unrelated system elements.
 * - Caps node traversal (max 150 nodes, max depth 15) to guarantee zero UI stutter or ANRs.
 * - Returns explicit inspectionUnavailable = true if root is null or obstructed.
 */
object ScreenContextExtractor {

    private const val MAX_TRAVERSAL_NODES = 150
    private const val MAX_TRAVERSAL_DEPTH = 15
    private const val MAX_TEXT_FRAGMENTS = 50
    private const val MAX_FRAGMENT_LENGTH = 200

    /**
     * Extracts structured ScreenContextMetadata from the active accessibility window.
     */
    fun extract(
        rootNode: AccessibilityNodeInfo?,
        event: AccessibilityEvent?
    ): ScreenContextMetadata {
        val packageName = event?.packageName?.toString() ?: rootNode?.packageName?.toString() ?: "UNKNOWN"
        val timestamp = System.currentTimeMillis()

        if (rootNode == null) {
            return ScreenContextMetadata(
                packageName = packageName,
                screenIdentifier = event?.className?.toString(),
                timestampMs = timestamp,
                inspectionErrors = listOf("Accessible window root is null or protected by FLAG_SECURE."),
                inspectionUnavailable = true
            )
        }

        val textFragments = mutableListOf<String>()
        val buttonLabels = mutableListOf<String>()
        val roles = mutableListOf<String>()
        val errors = mutableListOf<String>()
        var extractedUrl: String? = null

        var nodeCount = 0

        // Breadth-first or depth-limited safe traversal
        fun traverse(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > MAX_TRAVERSAL_DEPTH || nodeCount >= MAX_TRAVERSAL_NODES) return
            nodeCount++

            try {
                // Strict Privacy Invariant: Check if password field
                if (AccessibilityEventFilter.isPasswordNode(node.isPassword, node.inputType)) {
                    errors.add("Skipped password input control (${node.className ?: "password_view"})")
                    return // Do not process children of password control
                }

                // Check className / role
                val classNameStr = node.className?.toString()
                if (!classNameStr.isNullOrBlank()) {
                    roles.add(classNameStr)
                }

                // Extract browser URL if on a browser address bar
                val viewId = node.viewIdResourceName
                val nodeText = node.text?.toString()
                val contentDesc = node.contentDescription?.toString()

                if (extractedUrl == null) {
                    val urlCandidate = AccessibilityEventFilter.extractBrowserUrl(viewId, nodeText ?: contentDesc)
                    if (urlCandidate != null) {
                        extractedUrl = urlCandidate
                    }
                }

                // Collect Button / Action labels
                val isButton = node.isClickable && (
                        classNameStr?.contains("button", ignoreCase = true) == true ||
                        classNameStr?.contains("imageview", ignoreCase = true) == true ||
                        node.isCheckable
                )
                if (isButton) {
                    val label = nodeText ?: contentDesc
                    if (!label.isNullOrBlank() && label.length <= 40) {
                        buttonLabels.add(label.trim())
                    }
                }

                // Collect visible text fragments (non-password)
                val textCandidate = nodeText ?: contentDesc
                if (!textCandidate.isNullOrBlank() && textFragments.size < MAX_TEXT_FRAGMENTS) {
                    val normalized = textCandidate.trim()
                    if (normalized.length > 1 && !textFragments.contains(normalized)) {
                        textFragments.add(normalized.take(MAX_FRAGMENT_LENGTH))
                    }
                }

                // Recurse to children
                val childCount = node.childCount
                for (i in 0 until childCount) {
                    if (nodeCount >= MAX_TRAVERSAL_NODES) break
                    val child = node.getChild(i)
                    if (child != null) {
                        traverse(child, depth + 1)
                    }
                }
            } catch (e: Exception) {
                errors.add("Error traversing node: ${e.message}")
            }
        }

        traverse(rootNode, 0)

        // Resolve candidate action from button labels and context
        val candidateAction = resolveCandidateAction(buttonLabels, textFragments, extractedUrl)

        return ScreenContextMetadata(
            packageName = packageName,
            screenIdentifier = event?.className?.toString(),
            visibleTextFragments = textFragments,
            buttonLabels = buttonLabels.distinct(),
            accessibilityRoles = roles.distinct(),
            screenDimensions = null,
            timestampMs = timestamp,
            candidateAction = candidateAction,
            evidenceSource = "ACCESSIBILITY_TREE",
            inspectionErrors = errors.distinct(),
            inspectionUnavailable = false,
            activeUrl = extractedUrl
        )
    }

    /**
     * Resolves the candidate action based on visible buttons and contextual signals.
     */
    fun resolveCandidateAction(
        buttonLabels: List<String>,
        textFragments: List<String>,
        activeUrl: String?
    ): ScreenActionType {
        // Priority 1: Check buttons
        for (btn in buttonLabels) {
            val mapped = AccessibilityEventFilter.mapButtonToAction(btn)
            if (mapped != "OPEN") {
                return ScreenActionType.fromString(mapped)
            }
        }

        // Priority 2: Check text fragments for prominent action cues
        val allText = textFragments.joinToString(" ").lowercase()
        return when {
            allText.contains("sign in to your account") || allText.contains("login to your bank") -> ScreenActionType.LOGIN
            allText.contains("transfer funds") || allText.contains("send money") || allText.contains("upi pin") -> ScreenActionType.APPROVE
            allText.contains("drafting post") || allText.contains("tweet your reply") -> ScreenActionType.POST
            activeUrl != null -> ScreenActionType.OPEN
            else -> ScreenActionType.OPEN
        }
    }
}
