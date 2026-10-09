package com.contextguard.app.core.notification

import java.util.UUID

/**
 * Transient feature representation extracted from a system notification.
 *
 * PRIVACY INVARIANT:
 * Strictly ephemeral in volatile RAM.
 * Raw notification payloads and sensitive message drafts are NEVER persisted to disk or logs.
 */
data class NotificationFeatures(
    val sourcePackage: String,
    val notificationCategory: String?,
    val messageLength: Int,
    val extractedUrls: List<String> = emptyList(),
    val suspiciousUrls: List<String> = emptyList(),
    val urgencyScore: Float = 0.0f,
    val impersonationEntity: String? = null,
    val requestsCredentialsOrOtp: Boolean = false,
    val requestsImmediatePayment: Boolean = false,
    val domainMismatches: List<String> = emptyList(),
    val linguisticRiskScore: Float = 0.0f,
    val isTruncated: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Result of local URL analysis on an extracted URL candidate.
 */
data class UrlAnalysisResult(
    val url: String,
    val domain: String,
    val isSuspicious: Boolean,
    val severity: Float,
    val reason: String
)

/**
 * Known organization / entity definition for impersonation detection.
 */
data class OrganizationEntity(
    val name: String,
    val keywords: List<String>,
    val officialDomains: List<String>,
    val category: String // BANK, UTILITY, GOV, TECH, TELECOM
)

/**
 * Privacy-safe audit entry for notification triage.
 * Strictly forbidden from recording raw text or message content.
 */
data class NotificationAuditRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val sourcePackage: String,
    val intervention: String,
    val riskScore: Float,
    val evidenceSummary: String,
    val userAction: String? = null
)
