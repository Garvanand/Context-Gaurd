package com.contextguard.app.core.engine

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.regex.Pattern

/**
 * Deterministic URL Lexical and Structural Feature Extractor (Schema v1.1.0).
 *
 * Implements 37 pre-navigation features with 100% mathematical parity against
 * Python `ml.features.url_features.URLFeatureExtractor`.
 */
object UrlFeatureExtractor {

    const val SCHEMA_VERSION: String = "1.1.0"
    const val FEATURE_COUNT: Int = 37

    val SUSPICIOUS_TLDS = setOf(
        "xyz", "top", "club", "work", "gq", "cf", "tk", "ml", "ga",
        "buzz", "icu", "fit", "rest", "kim", "info", "monster", "live",
        "surf", "cam", "bid", "racing", "win", "stream", "download"
    )

    val SHORTENER_DOMAINS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd",
        "buff.ly", "adf.ly", "bitly.com", "cutt.ly", "rb.gy", "shorturl.at",
        "tiny.cc", "lnkd.in", "t.ly", "cli.gs", "rebrand.ly", "s.id"
    )

    val SUSPICIOUS_KEYWORDS = listOf(
        "login", "signin", "verify", "verification", "account", "banking",
        "secure", "security", "update", "confirm", "confirmation", "wallet",
        "password", "credential", "support", "auth", "authenticate", "recover",
        "token", "billing", "invoice", "payment", "authenticate", "portal",
        "service", "customer", "admin", "webscr", "ebayisapi"
    )

    val FEATURE_NAMES = listOf(
        "url_length",
        "hostname_length",
        "path_length",
        "query_length",
        "fragment_length",
        "count_dots",
        "count_hyphens",
        "count_underscores",
        "count_slashes",
        "count_question_marks",
        "count_equal_signs",
        "count_at",
        "count_ampersands",
        "count_percent",
        "count_digits",
        "count_special_chars",
        "num_subdomains",
        "num_path_segments",
        "is_https",
        "is_ip_host",
        "has_at_symbol",
        "has_percent_encoding",
        "has_punycode",
        "is_suspicious_tld",
        "suspicious_keyword_count",
        "shannon_entropy",
        "hostname_entropy",
        "digit_ratio",
        "symbol_ratio",
        "hostname_to_path_ratio",
        "max_consecutive_digits",
        "max_consecutive_special_chars",
        "has_double_slash_path",
        "has_port",
        "has_hex_char",
        "vowel_consonant_ratio",
        "is_shortened_url"
    )

    private val HEX_PATTERN = Pattern.compile("%[0-9a-fA-F]{2}")
    private val IP_V4_PATTERN = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")

    data class ParsedUrl(
        val scheme: String,
        val hostname: String,
        val port: Int?,
        val path: String,
        val query: String,
        val fragment: String
    )

    fun parseUrl(rawUrl: String): ParsedUrl {
        val trimmed = rawUrl.trim()
        val parseTarget = if (!trimmed.contains("://") && !trimmed.startsWith("//")) {
            "http://$trimmed"
        } else if (trimmed.startsWith("//")) {
            "http:$trimmed"
        } else {
            trimmed
        }

        var scheme = ""
        var rest = parseTarget

        val schemeEnd = rest.indexOf("://")
        if (schemeEnd != -1) {
            scheme = rest.substring(0, schemeEnd).lowercase()
            rest = rest.substring(schemeEnd + 3)
        }

        // Split off fragment
        var fragment = ""
        val fragmentIdx = rest.indexOf('#')
        if (fragmentIdx != -1) {
            fragment = rest.substring(fragmentIdx + 1)
            rest = rest.substring(0, fragmentIdx)
        }

        // Split off query
        var query = ""
        val queryIdx = rest.indexOf('?')
        if (queryIdx != -1) {
            query = rest.substring(queryIdx + 1)
            rest = rest.substring(0, queryIdx)
        }

        // Split authority and path
        val pathIdx = rest.indexOf('/')
        val authority: String
        val path: String
        if (pathIdx != -1) {
            authority = rest.substring(0, pathIdx)
            path = rest.substring(pathIdx)
        } else {
            authority = rest
            path = ""
        }

        // Extract hostname and port from authority
        var host = authority
        val atIdx = host.lastIndexOf('@')
        if (atIdx != -1) {
            host = host.substring(atIdx + 1)
        }

        var port: Int? = null
        val colonIdx = host.lastIndexOf(':')
        if (colonIdx != -1 && !host.contains(']')) {
            val portStr = host.substring(colonIdx + 1)
            val parsedPort = portStr.toIntOrNull()
            if (parsedPort != null) {
                port = parsedPort
                host = host.substring(0, colonIdx)
            }
        } else if (host.contains(']')) {
            // IPv6 [::1]:port
            val closeBracket = host.indexOf(']')
            if (closeBracket != -1 && closeBracket + 1 < host.length && host[closeBracket + 1] == ':') {
                val parsedPort = host.substring(closeBracket + 2).toIntOrNull()
                if (parsedPort != null) {
                    port = parsedPort
                    host = host.substring(0, closeBracket + 1)
                }
            }
        }

        return ParsedUrl(
            scheme = scheme,
            hostname = host.lowercase(),
            port = port,
            path = path,
            query = query,
            fragment = fragment
        )
    }

    fun calculateEntropy(text: String): Float {
        if (text.isEmpty()) return 0.0f
        val len = text.length.toDouble()
        val freq = HashMap<Char, Int>()
        for (c in text) {
            freq[c] = (freq[c] ?: 0) + 1
        }
        var entropy = 0.0
        val log2 = Math.log(2.0)
        for (count in freq.values) {
            val p = count / len
            entropy -= p * (Math.log(p) / log2)
        }
        return round4(entropy)
    }

    fun maxConsecutiveRun(text: String, predicate: (Char) -> Boolean): Int {
        var maxRun = 0
        var currentRun = 0
        for (c in text) {
            if (predicate(c)) {
                currentRun++
                if (currentRun > maxRun) {
                    maxRun = currentRun
                }
            } else {
                currentRun = 0
            }
        }
        return maxRun
    }

    private fun round4(value: Double): Float {
        return BigDecimal(value).setScale(4, RoundingMode.HALF_UP).toFloat()
    }

    /**
     * Extracts exactly 37 pre-navigation features matching Python vector order.
     */
    fun extractVector(rawUrl: String): FloatArray {
        val url = rawUrl.trim()
        val parsed = parseUrl(url)

        val urlLen = url.length
        val hostLen = parsed.hostname.length
        val pathLen = parsed.path.length
        val queryLen = parsed.query.length
        val fragmentLen = parsed.fragment.length

        var countDots = 0
        var countHyphens = 0
        var countUnderscores = 0
        var countSlashes = 0
        var countQuestion = 0
        var countEquals = 0
        var countAt = 0
        var countAmpersands = 0
        var countPercent = 0
        var countDigits = 0
        var countSpecial = 0

        for (c in url) {
            when (c) {
                '.' -> countDots++
                '-' -> countHyphens++
                '_' -> countUnderscores++
                '/' -> countSlashes++
                '?' -> countQuestion++
                '=' -> countEquals++
                '@' -> countAt++
                '&' -> countAmpersands++
                '%' -> countPercent++
            }
            if (c.isDigit()) {
                countDigits++
            }
            if (!c.isLetterOrDigit()) {
                countSpecial++
            }
        }

        // Subdomains
        val hostParts = parsed.hostname.split(".").filter { it.isNotEmpty() }
        val numSubdomains = if (hostParts.size >= 2) maxOf(0, hostParts.size - 2) else 0

        // Path segments
        val pathSegments = parsed.path.split("/").filter { it.isNotEmpty() }
        val numPathSegments = pathSegments.size

        // Flags
        val isHttps = if (parsed.scheme == "https") 1.0f else 0.0f

        var isIpHost = 0.0f
        if (IP_V4_PATTERN.matcher(parsed.hostname).matches()) {
            val octets = parsed.hostname.split(".").mapNotNull { it.toIntOrNull() }
            if (octets.size == 4 && octets.all { it in 0..255 }) {
                isIpHost = 1.0f
            }
        }

        val hasAtSymbol = if (countAt > 0) 1.0f else 0.0f
        val hasPercentEncoding = if (countPercent > 0) 1.0f else 0.0f
        val hasPunycode = if (parsed.hostname.contains("xn--")) 1.0f else 0.0f

        val tld = if (hostParts.isNotEmpty()) hostParts.last().lowercase() else ""
        val isSuspiciousTld = if (SUSPICIOUS_TLDS.contains(tld)) 1.0f else 0.0f

        val urlLower = url.lowercase()
        var keywordCount = 0
        for (kw in SUSPICIOUS_KEYWORDS) {
            if (urlLower.contains(kw)) {
                keywordCount++
            }
        }

        val shannonEntropy = calculateEntropy(url)
        val hostnameEntropy = calculateEntropy(parsed.hostname)

        val safeUrlLen = maxOf(1, urlLen)
        val digitRatio = round4(countDigits.toDouble() / safeUrlLen)
        val symbolRatio = round4(countSpecial.toDouble() / safeUrlLen)
        val hostToPathRatio = round4(hostLen.toDouble() / (pathLen + 1.0))

        val maxConsecDigits = maxConsecutiveRun(url) { it.isDigit() }
        val maxConsecSpecial = maxConsecutiveRun(url) { !it.isLetterOrDigit() }

        val hasDoubleSlash = if (parsed.path.contains("//")) 1.0f else 0.0f
        val hasPort = if (parsed.port != null && parsed.port != 80 && parsed.port != 443) 1.0f else 0.0f
        val hasHex = if (HEX_PATTERN.matcher(url).find()) 1.0f else 0.0f

        // Vowel to consonant ratio
        var vowels = 0
        var consonants = 0
        for (c in parsed.hostname) {
            if (c.isLetter()) {
                val lowerC = c.lowercaseChar()
                if (lowerC in "aeiou") {
                    vowels++
                } else {
                    consonants++
                }
            }
        }
        val vowelConsonantRatio = round4(vowels.toDouble() / maxOf(1, consonants))

        // Shortener check
        val isShortened = if (SHORTENER_DOMAINS.any { parsed.hostname == it || parsed.hostname.endsWith(".$it") }) 1.0f else 0.0f

        return floatArrayOf(
            urlLen.toFloat(),                     // 0: url_length
            hostLen.toFloat(),                    // 1: hostname_length
            pathLen.toFloat(),                    // 2: path_length
            queryLen.toFloat(),                   // 3: query_length
            fragmentLen.toFloat(),                // 4: fragment_length
            countDots.toFloat(),                  // 5: count_dots
            countHyphens.toFloat(),               // 6: count_hyphens
            countUnderscores.toFloat(),            // 7: count_underscores
            countSlashes.toFloat(),               // 8: count_slashes
            countQuestion.toFloat(),              // 9: count_question_marks
            countEquals.toFloat(),                // 10: count_equal_signs
            countAt.toFloat(),                    // 11: count_at
            countAmpersands.toFloat(),            // 12: count_ampersands
            countPercent.toFloat(),               // 13: count_percent
            countDigits.toFloat(),                // 14: count_digits
            countSpecial.toFloat(),               // 15: count_special_chars
            numSubdomains.toFloat(),              // 16: num_subdomains
            numPathSegments.toFloat(),            // 17: num_path_segments
            isHttps,                              // 18: is_https
            isIpHost,                             // 19: is_ip_host
            hasAtSymbol,                          // 20: has_at_symbol
            hasPercentEncoding,                   // 21: has_percent_encoding
            hasPunycode,                          // 22: has_punycode
            isSuspiciousTld,                      // 23: is_suspicious_tld
            keywordCount.toFloat(),               // 24: suspicious_keyword_count
            shannonEntropy,                       // 25: shannon_entropy
            hostnameEntropy,                      // 26: hostname_entropy
            digitRatio,                           // 27: digit_ratio
            symbolRatio,                          // 28: symbol_ratio
            hostToPathRatio,                      // 29: hostname_to_path_ratio
            maxConsecDigits.toFloat(),            // 30: max_consecutive_digits
            maxConsecSpecial.toFloat(),           // 31: max_consecutive_special_chars
            hasDoubleSlash,                       // 32: has_double_slash_path
            hasPort,                              // 33: has_port
            hasHex,                               // 34: has_hex_char
            vowelConsonantRatio,                  // 35: vowel_consonant_ratio
            isShortened                           // 36: is_shortened_url
        )
    }
}
