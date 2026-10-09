package com.contextguard.app.core.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Privacy-safe telemetry record for Just-in-Time overlay events.
 *
 * PRIVACY INVARIANT:
 * Strictly forbidden from recording raw screen text, passwords, or message contents.
 * Only records timing, latency, intervention class, target package, and user choice.
 */
data class OverlayTelemetryRecord(
    val id: String = UUID.randomUUID().toString(),
    val eventTimestamp: Long,
    val riskDecisionTimestamp: Long,
    val overlayShownTimestamp: Long,
    val decisionToOverlayLatencyMs: Long,
    val interventionType: String,
    val targetPackage: String,
    val candidateAction: String,
    var userChoice: String? = null,
    var choiceTimestamp: Long? = null
)

/**
 * Ephemeral telemetry manager tracking intervention overlay metrics and user choices.
 */
object OverlayTelemetryManager {

    private const val MAX_RECORDS = 100
    private val _records = MutableStateFlow<List<OverlayTelemetryRecord>>(emptyList())
    val records: StateFlow<List<OverlayTelemetryRecord>> = _records.asStateFlow()

    /**
     * Records the presentation of an intervention overlay.
     */
    fun recordOverlayShown(
        eventTimestamp: Long,
        riskDecisionTimestamp: Long,
        overlayShownTimestamp: Long,
        interventionType: String,
        targetPackage: String,
        candidateAction: String
    ): String {
        val latency = overlayShownTimestamp - riskDecisionTimestamp
        val record = OverlayTelemetryRecord(
            eventTimestamp = eventTimestamp,
            riskDecisionTimestamp = riskDecisionTimestamp,
            overlayShownTimestamp = overlayShownTimestamp,
            decisionToOverlayLatencyMs = latency,
            interventionType = interventionType,
            targetPackage = targetPackage,
            candidateAction = candidateAction
        )

        _records.update { current ->
            (listOf(record) + current).take(MAX_RECORDS)
        }

        return record.id
    }

    /**
     * Records the user's action/choice on the overlay.
     */
    fun recordUserChoice(recordId: String, choice: String) {
        val now = System.currentTimeMillis()
        _records.update { current ->
            current.map {
                if (it.id == recordId) {
                    it.copy(userChoice = choice, choiceTimestamp = now)
                } else {
                    it
                }
            }
        }
    }

    fun clearTelemetry() {
        _records.value = emptyList()
    }
}
