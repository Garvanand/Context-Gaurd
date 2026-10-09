package com.contextguard.app.core.network

import android.content.Context
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * On-Device Domain Risk Evaluator.
 *
 * Evaluates destination hostnames using purely local heuristics and portable model features.
 *
 * Strict Privacy & Architectural Invariants:
 * 1. Metadata visibility is strictly restricted to hostnames / domain names.
 *    Does NOT attempt or claim to inspect URL paths, query parameters, or HTTPS encrypted payloads.
 * 2. All processing is 100% on-device; zero domains or telemetry forwarded off-device.
 * 3. Zero retention: benign domains are evaluated in transient memory and discarded immediately.
 * 4. User-controlled safe-domain allowlist takes strict precedence.
 */
class DomainRiskEvaluator(
    private val context: Context? = null
) {
    // Thread-safe in-memory safe domains (user configurable allowlist)
    private val safeDomains = ConcurrentHashMap.newKeySet<String>().apply {
        addAll(
            listOf(
                "google.com",
                "android.com",
                "github.com",
                "microsoft.com",
                "apple.com",
                "wikipedia.org",
                "cloudflare.com",
                "mozilla.org"
            )
        )
    }

    // High-confidence suspicious TLDs and phishing patterns (portable heuristic layer)
    private val suspiciousTlds = setOf(
        ".top", ".xyz", ".buzz", ".work", ".click", ".link", ".fit", ".rest", ".gq", ".cf", ".tk", ".ml"
    )

    private val suspiciousKeywords = listOf(
        "verify-sso", "login-auth", "secure-account", "update-kyc", "portal-login",
        "banking-alert", "paypal-security", "refund-desk", "upi-collect", "claim-reward"
    )

    data class DomainEvaluation(
        val hostname: String,
        val isThreat: Boolean,
        val riskScore: Float,
        val rationale: String,
        val isAllowlisted: Boolean
    )

    /**
     * Normalizes a raw hostname string.
     */
    fun sanitizeHostname(raw: String): String {
        return raw.trim()
            .lowercase(Locale.ROOT)
            .removePrefix("http://")
            .removePrefix("https://")
            .split("/")[0]
            .split(":")[0]
    }

    /**
     * Checks if a domain is on the user allowlist.
     */
    fun isSafeDomain(hostname: String): Boolean {
        val sanitized = sanitizeHostname(hostname)
        if (safeDomains.contains(sanitized)) return true
        // Check wildcard / parent domain e.g. mail.google.com -> google.com
        return safeDomains.any { safe -> sanitized.endsWith(".$safe") }
    }

    /**
     * Adds a user-trusted domain to the allowlist.
     */
    fun addSafeDomain(hostname: String) {
        val sanitized = sanitizeHostname(hostname)
        if (sanitized.isNotEmpty()) {
            safeDomains.add(sanitized)
        }
    }

    /**
     * Removes a domain from the allowlist.
     */
    fun removeSafeDomain(hostname: String) {
        val sanitized = sanitizeHostname(hostname)
        safeDomains.remove(sanitized)
    }

    fun getSafeDomains(): Set<String> = Collections.unmodifiableSet(safeDomains)

    /**
     * Evaluates a hostname using genuine on-device domain features.
     * Enforces that only visible domain metadata is used.
     */
    fun evaluate(hostname: String): DomainEvaluation {
        val clean = sanitizeHostname(hostname)
        if (clean.isEmpty()) {
            return DomainEvaluation(
                hostname = clean,
                isThreat = false,
                riskScore = 0.0f,
                rationale = "Empty hostname",
                isAllowlisted = false
            )
        }

        // 1. Check user allowlist
        if (isSafeDomain(clean)) {
            return DomainEvaluation(
                hostname = clean,
                isThreat = false,
                riskScore = 0.05f,
                rationale = "Domain is explicitly allowlisted by user policy",
                isAllowlisted = true
            )
        }

        var score = 0.10f
        val reasons = mutableListOf<String>()

        // 2. IP Literal Check (e.g. 192.168.1.1 or 45.33.32.156)
        val isIpLiteral = clean.matches(Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$"""))
        if (isIpLiteral) {
            score += 0.55f
            reasons.add("Hostname is a raw IP literal rather than a registered domain")
        }

        // 3. Suspicious TLD check
        val hasSuspiciousTld = suspiciousTlds.any { clean.endsWith(it) }
        if (hasSuspiciousTld) {
            score += 0.35f
            reasons.add("Domain uses a high-abuse top-level domain")
        }

        // 4. Phishing keywords in hostname
        val matchedKeywords = suspiciousKeywords.filter { clean.contains(it) }
        if (matchedKeywords.isNotEmpty()) {
            score += 0.45f
            reasons.add("Hostname contains suspicious deceptive tokens: ${matchedKeywords.joinToString()}")
        }

        // 5. Excessive subdomains (e.g. login.portal.security.update.example.com)
        val dotCount = clean.count { it == '.' }
        if (dotCount >= 4) {
            score += 0.20f
            reasons.add("Excessive subdomain depth ($dotCount levels)")
        }

        val clampedScore = score.coerceIn(0.0f, 1.0f)
        val isThreat = clampedScore >= 0.65f

        val rationale = if (reasons.isNotEmpty()) {
            val prefix = if (isThreat) "Suspicious domain detected: " else "Caution: "
            prefix + reasons.joinToString("; ")
        } else {
            "Domain verified as benign under on-device heuristic evaluation"
        }


        return DomainEvaluation(
            hostname = clean,
            isThreat = isThreat,
            riskScore = clampedScore,
            rationale = rationale,
            isAllowlisted = false
        )
    }
}
