package com.contextguard.app.core.perception

import android.graphics.Rect
import java.util.regex.Pattern

/**
 * Local on-device PII detector using multi-pattern regex and algorithmic validation (e.g. Luhn).
 *
 * NOTE: Regex and heuristic detectors do not guarantee 100% PII recall.
 * ContextGuard employs defense-in-depth, combining edge regex detection with
 * downstream multimodal vision-language reasoning.
 */
object PiiDetector {

    // 1. Email Address Pattern
    private val EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+",
        Pattern.CASE_INSENSITIVE
    )

    // 2. Phone Numbers: Indian mobile + International E.164 formats
    private val PHONE_INDIAN_PATTERN = Pattern.compile(
        "\\b(?:(?:\\+|0{0,2})91[\\s-]*)?[6789]\\d{9}\\b"
    )
    private val PHONE_INTL_PATTERN = Pattern.compile(
        "\\b\\+?[1-9]\\d{0,2}[-.\\s]?\\(?\\d{2,4}\\)?[-.\\s]?\\d{3,4}[-.\\s]?\\d{3,4}\\b"
    )

    // 3. One-Time Passwords (OTP) with contextual keyword grounding
    private val OTP_WITH_CONTEXT_PATTERN = Pattern.compile(
        "(?i)(?:otp|code|one[- ]time[- ]password|verification|auth code)[^0-9\\n]{0,25}\\b([0-9]{4,8})\\b"
    )
    private val OTP_REVERSE_CONTEXT_PATTERN = Pattern.compile(
        "(?i)\\b([0-9]{4,8})\\b[^0-9\\n]{0,35}(?:is (?:your )?(?:otp|code|verification)|valid for|never share)"
    )

    // 4. Payment Cards (13 to 19 digits, with or without spaces/dashes)
    private val CARD_PATTERN = Pattern.compile(
        "\\b(?:\\d[ -]*?){13,19}\\b"
    )

    // 5. Aadhaar Number (12 digits, often in 4-4-4 format)
    private val AADHAAR_PATTERN = Pattern.compile(
        "\\b[2-9]{1}\\d{3}[ -]?\\d{4}[ -]?\\d{4}\\b"
    )

    // 6. Bank Account Numbers (9-18 digits with contextual keywords)
    private val BANK_ACCOUNT_PATTERN = Pattern.compile(
        "(?i)(?:account|a/c|acct|acc no|savings|current)[^0-9\\n]{0,20}\\b([0-9]{9,18})\\b"
    )

    // 7. URLs and Web Domains
    private val URL_PATTERN = Pattern.compile(
        "\\b(?:https?://|www\\.)[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)+(?:/[^\\s]*)?",
        Pattern.CASE_INSENSITIVE
    )

    // 8. Dates (DD/MM/YYYY, YYYY-MM-DD, etc.)
    private val DATE_PATTERN = Pattern.compile(
        "\\b(?:\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})\\b"
    )

    // 9. Postal PIN Code (Indian 6-digit) and US 5-digit ZIP
    private val PIN_CODE_PATTERN = Pattern.compile(
        "(?i)(?:pin|pincode|postal|zip)[^0-9\\n]{0,15}\\b([1-9][0-9]{5}|[0-9]{5}(?:-[0-9]{4})?)\\b"
    )

    /**
     * Scans text and cross-references OCR bounding boxes to extract PiiFindings.
     */
    fun detectPii(
        text: String,
        blocks: List<TextBlockResult> = emptyList(),
        pageIndex: Int = 0
    ): List<PiiFinding> {
        val findings = mutableListOf<PiiFinding>()
        val seenSpans = mutableSetOf<String>()

        // Helper to record finding and resolve bounding box from OCR blocks
        fun addFinding(
            type: PiiType,
            patternId: String,
            raw: String,
            confidence: Float,
            source: String = "REGEX_HEURISTIC"
        ) {
            val key = "${type.name}:$raw"
            if (seenSpans.contains(key)) return
            seenSpans.add(key)

            val box = findBoundingBoxForText(raw, blocks)
            val masked = maskValue(raw, type)
            findings.add(
                PiiFinding(
                    type = type,
                    patternId = patternId,
                    rawValue = raw,
                    maskedValue = masked,
                    confidence = confidence,
                    boundingBox = box,
                    source = source,
                    pageIndex = pageIndex
                )
            )
        }

        // 1. Email Detection
        val emailMatcher = EMAIL_PATTERN.matcher(text)
        while (emailMatcher.find()) {
            val email = emailMatcher.group()
            addFinding(PiiType.EMAIL_ADDRESS, "PII_EMAIL_STANDARD", email, 0.95f)
        }

        // 2. OTP Detection (Contextual)
        val otpMatcher1 = OTP_WITH_CONTEXT_PATTERN.matcher(text)
        while (otpMatcher1.find()) {
            val code = otpMatcher1.group(1) ?: otpMatcher1.group()
            addFinding(PiiType.OTP_CODE, "PII_OTP_CONTEXT_PREFIX", code, 0.95f)
        }
        val otpMatcher2 = OTP_REVERSE_CONTEXT_PATTERN.matcher(text)
        while (otpMatcher2.find()) {
            val code = otpMatcher2.group(1) ?: otpMatcher2.group()
            addFinding(PiiType.OTP_CODE, "PII_OTP_CONTEXT_SUFFIX", code, 0.95f)
        }

        // 3. Payment Card Detection (with Luhn algorithm check)
        val cardMatcher = CARD_PATTERN.matcher(text)
        while (cardMatcher.find()) {
            val rawCard = cardMatcher.group()
            val digitsOnly = rawCard.replace("[ -]".toRegex(), "")
            if (digitsOnly.length in 13..19) {
                if (validateLuhn(digitsOnly)) {
                    addFinding(PiiType.PAYMENT_CARD, "PII_CARD_LUHN_VERIFIED", rawCard, 0.98f, "LUHN_ALGORITHM")
                } else if (digitsOnly.length == 16 && (rawCard.contains(" ") || rawCard.contains("-"))) {
                    addFinding(PiiType.PAYMENT_CARD, "PII_CARD_FORMAT_HEURISTIC", rawCard, 0.70f)
                }
            }
        }

        // 4. Aadhaar Detection
        val aadhaarMatcher = AADHAAR_PATTERN.matcher(text)
        while (aadhaarMatcher.find()) {
            val rawAadhaar = aadhaarMatcher.group()
            val digitsOnly = rawAadhaar.replace("[ -]".toRegex(), "")
            if (digitsOnly.length == 12 && digitsOnly[0] in '2'..'9') {
                addFinding(PiiType.AADHAAR_NUMBER, "PII_AADHAAR_12DIGIT", rawAadhaar, 0.88f)
            }
        }

        // 5. Phone Detection (Indian Mobile & International)
        val phoneInMatcher = PHONE_INDIAN_PATTERN.matcher(text)
        while (phoneInMatcher.find()) {
            val phone = phoneInMatcher.group()
            val digitsOnly = phone.replace("[^0-9]".toRegex(), "")
            if (digitsOnly.length >= 10) {
                addFinding(PiiType.PHONE_NUMBER, "PII_PHONE_IN_MOBILE", phone, 0.92f)
            }
        }
        val phoneIntlMatcher = PHONE_INTL_PATTERN.matcher(text)
        while (phoneIntlMatcher.find()) {
            val phone = phoneIntlMatcher.group()
            val digitsOnly = phone.replace("[^0-9]".toRegex(), "")
            if (digitsOnly.length in 10..15 && !seenSpans.contains("${PiiType.PHONE_NUMBER.name}:$phone")) {
                addFinding(PiiType.PHONE_NUMBER, "PII_PHONE_INTL_E164", phone, 0.80f)
            }
        }

        // 6. Bank Account Detection
        val bankMatcher = BANK_ACCOUNT_PATTERN.matcher(text)
        while (bankMatcher.find()) {
            val acct = bankMatcher.group(1) ?: bankMatcher.group()
            addFinding(PiiType.BANK_ACCOUNT, "PII_BANK_ACCOUNT_CONTEXT", acct, 0.85f)
        }

        // 7. URL Detection
        val urlMatcher = URL_PATTERN.matcher(text)
        while (urlMatcher.find()) {
            val url = urlMatcher.group()
            addFinding(PiiType.URL_STRING, "PII_URL_EXTRACTED", url, 0.95f)
        }

        // 8. Date Detection
        val dateMatcher = DATE_PATTERN.matcher(text)
        while (dateMatcher.find()) {
            val date = dateMatcher.group()
            addFinding(PiiType.DATE_STRING, "PII_DATE_NUMERIC", date, 0.80f)
        }

        // 9. PIN / ZIP Code Detection
        val pinMatcher = PIN_CODE_PATTERN.matcher(text)
        while (pinMatcher.find()) {
            val pin = pinMatcher.group(1) ?: pinMatcher.group()
            addFinding(PiiType.STREET_ADDRESS, "PII_POSTAL_PIN_CODE", pin, 0.85f)
        }

        return findings
    }

    /**
     * Validates card number against standard Luhn mod-10 algorithm.
     */
    fun validateLuhn(numberStr: String): Boolean {
        var sum = 0
        var alternate = false
        for (i in numberStr.length - 1 downTo 0) {
            val c = numberStr[i]
            if (!c.isDigit()) return false
            var n = c - '0'
            if (alternate) {
                n *= 2
                if (n > 9) n = (n % 10) + 1
            }
            sum += n
            alternate = !alternate
        }
        return (sum % 10 == 0)
    }

    /**
     * Safely masks sensitive values for on-device display and logging.
     */
    fun maskValue(raw: String, type: PiiType): String {
        val clean = raw.trim()
        return when (type) {
            PiiType.PAYMENT_CARD -> {
                val digits = clean.replace("[ -]".toRegex(), "")
                if (digits.length >= 4) "****-****-****-${digits.takeLast(4)}" else "****************"
            }
            PiiType.AADHAAR_NUMBER -> {
                val digits = clean.replace("[ -]".toRegex(), "")
                if (digits.length >= 4) "XXXX-XXXX-${digits.takeLast(4)}" else "XXXXXXXXXXXX"
            }
            PiiType.PHONE_NUMBER -> {
                val digits = clean.replace("[^0-9]".toRegex(), "")
                if (digits.length >= 4) "***-***-${digits.takeLast(4)}" else "**********"
            }
            PiiType.EMAIL_ADDRESS -> {
                val parts = clean.split("@")
                if (parts.size == 2 && parts[0].isNotEmpty()) {
                    val user = parts[0]
                    val maskedUser = if (user.length > 2) "${user.first()}***${user.last()}" else "${user.first()}***"
                    "$maskedUser@${parts[1]}"
                } else "***@***.***"
            }
            PiiType.OTP_CODE -> "******"
            PiiType.BANK_ACCOUNT -> {
                if (clean.length >= 4) "****${clean.takeLast(4)}" else "********"
            }
            else -> clean
        }
    }

    /**
     * Replaces detected sensitive PII substrings in text with masked tokens.
     */
    fun maskPiiInText(text: String, findings: List<PiiFinding>): String {
        var result = text
        val sorted = findings.sortedByDescending { it.rawValue.length }
        for (f in sorted) {
            if (f.rawValue.isNotEmpty()) {
                val masked = if (f.maskedValue.isNotEmpty()) f.maskedValue else maskValue(f.rawValue, f.type)
                result = result.replace(f.rawValue, masked)
            }
        }
        return result
    }

    /**
     * Resolves the spatial bounding box in OCR results corresponding to a PII snippet.
     */
    private fun findBoundingBoxForText(targetText: String, blocks: List<TextBlockResult>): Rect? {
        val targetClean = targetText.replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()
        if (targetClean.isEmpty()) return null

        for (block in blocks) {
            for (line in block.lines) {
                val lineClean = line.text.replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()
                if (lineClean.contains(targetClean)) {
                    return line.boundingBox ?: block.boundingBox
                }
            }
        }
        return null
    }
}
