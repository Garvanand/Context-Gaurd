package com.contextguard.app.core.state

/**
 * Explicit, lifecycle-safe entry-point state machine for ContextGuard.
 * Required states per Mobile Product Contract:
 * - NORMAL_LAUNCH
 * - SETUP_REQUIRED
 * - PROTECTION_ACTIVE
 * - PROTECTION_PAUSED
 * - SHARED_ARTIFACT_INGESTION
 * - ACTION_CONTEXT_SELECTION
 * - ANALYSIS_IN_PROGRESS
 * - INTERVENTION_RESULT
 * - RECOVERABLE_ERROR
 */
sealed interface AppEntryState {

    /**
     * Normal application launch from Android home launcher.
     * Evaluates live OS permissions and service binding to resolve
     * to SETUP_REQUIRED, PROTECTION_ACTIVE, or PROTECTION_PAUSED.
     */
    object NormalLaunch : AppEntryState

    /**
     * Neither Accessibility nor Notification listener is enabled in Android Settings.
     * Directs user to explicit in-app disclosure and system settings.
     */
    object SetupRequired : AppEntryState

    /**
     * Background monitoring is actively bound and processing permitted UI/notification events.
     */
    object ProtectionActive : AppEntryState

    /**
     * User engaged immediate kill-switch to pause background monitoring.
     */
    object ProtectionPaused : AppEntryState

    /**
     * Processing an incoming ACTION_SEND or ACTION_SEND_MULTIPLE payload.
     * Ingestion and local ML Kit perception start immediately in volatile RAM.
     */
    data class SharedArtifactIngestion(
        val title: String,
        val mimeType: String,
        val sourceApp: String
    ) : AppEntryState

    /**
     * Preprocessing completed. Prompts user for intended action/recipient context
     * without forcing an unnecessary 'Analyze Now' button.
     */
    data class ActionContextSelection(
        val artifactTitle: String,
        val candidateActions: List<String>,
        val preselectedAction: String,
        val recipient: String,
        val destination: String
    ) : AppEntryState

    /**
     * Deterministic risk evaluation in progress across 6 stages.
     */
    data class AnalysisInProgress(
        val stage: Int,
        val stageDescription: String
    ) : AppEntryState

    /**
     * Risk decision ready (ACT, ASK, WARN, STOP) rendered in ResultScreen.
     */
    data class InterventionResult(
        val intervention: String,
        val riskScore: Float
    ) : AppEntryState

    /**
     * A recoverable error occurred during ingestion or analysis.
     */
    data class RecoverableError(
        val message: String,
        val canRetry: Boolean = true
    ) : AppEntryState
}
