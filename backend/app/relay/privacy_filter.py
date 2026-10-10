"""
Privacy Filter & Invariant Enforcement for ContextGuard Relay.

Enforces zero-leakage constraints:
- Rejects or strips raw screenshots / base64 image blobs.
- Rejects raw passwords, OTP tokens, unmasked credit cards.
- Rejects complete raw OCR text dumps and raw notification message bodies.
- Only allows privacy-safe summarized evidence, redacted tokens, and metadata.
"""

import re
from typing import Any, Dict, List, Tuple

# Disallowed keys that must NEVER appear in relay ingestion payloads
FORBIDDEN_PAYLOAD_KEYS = {
    "artifact_base64",
    "screenshot",
    "raw_image",
    "raw_bytes",
    "raw_ocr",
    "raw_ocr_text",
    "full_ocr",
    "password",
    "pin",
    "otp",
    "raw_notification",
    "accessibility_tree",
    "auth_token",
    "private_key",
    "secret",
}

# Regex for sensitive raw numbers that should have been masked on device
CREDIT_CARD_REGEX = re.compile(r"\b(?:\d{4}[ -]?){3}\d{4}\b")
INDIAN_AADHAAR_REGEX = re.compile(r"\b\d{4}\s\d{4}\s\d{4}\b")
RAW_OTP_REGEX = re.compile(r"\b(?:otp|code|pin)[:=\s]+(\d{4,8})\b", re.IGNORECASE)


class PrivacyViolationError(ValueError):
    """Raised when an inbound relay payload violates the privacy invariant."""
    pass


def inspect_for_forbidden_keys(data: Any, path: str = "") -> List[str]:
    """Recursively checks for forbidden payload keys."""
    violations = []
    if isinstance(data, dict):
        for k, v in data.items():
            current_path = f"{path}.{k}" if path else k
            if k.lower() in FORBIDDEN_PAYLOAD_KEYS:
                violations.append(f"Forbidden key '{k}' detected at '{current_path}'")
            # If a base64-like massive string is detected
            if isinstance(v, str) and len(v) > 5000:
                violations.append(f"Excessive payload string length ({len(v)} chars) at '{current_path}' - suspected raw image/artifact dump")
            violations.extend(inspect_for_forbidden_keys(v, current_path))
    elif isinstance(data, list):
        for idx, item in enumerate(data):
            violations.extend(inspect_for_forbidden_keys(item, f"{path}[{idx}]"))
    return violations


def inspect_text_for_sensitive_entities(text: str) -> List[str]:
    """Scans evidence or metadata text for unredacted sensitive patterns."""
    violations = []
    if CREDIT_CARD_REGEX.search(text):
        violations.append("Unmasked 16-digit payment card number detected in text")
    if INDIAN_AADHAAR_REGEX.search(text):
        violations.append("Unmasked 12-digit Aadhaar identity number detected in text")
    if RAW_OTP_REGEX.search(text):
        violations.append("Raw OTP credential detected in text")
    return violations


def validate_event_privacy(event_dict: Dict[str, Any]) -> Tuple[bool, List[str]]:
    """
    Validates that an event dictionary conforms strictly to the privacy guarantee.
    Returns (is_valid, list_of_violations).
    """
    violations = inspect_for_forbidden_keys(event_dict)

    # Check evidence summaries
    evidence_list = event_dict.get("evidence_summary", [])
    if isinstance(evidence_list, list):
        for idx, ev in enumerate(evidence_list):
            if isinstance(ev, str):
                text_violations = inspect_text_for_sensitive_entities(ev)
                for tv in text_violations:
                    violations.append(f"Evidence item {idx}: {tv}")

    # Check source_app or other fields
    for field in ["source_app", "risk_category"]:
        val = event_dict.get(field)
        if isinstance(val, str):
            text_violations = inspect_text_for_sensitive_entities(val)
            for tv in text_violations:
                violations.append(f"Field '{field}': {tv}")

    return len(violations) == 0, violations


def enforce_privacy_or_raise(event_dict: Dict[str, Any]) -> None:
    """Raises PrivacyViolationError if any sensitive content is present."""
    is_valid, violations = validate_event_privacy(event_dict)
    if not is_valid:
        error_msg = "; ".join(violations)
        raise PrivacyViolationError(f"Privacy Invariant Breached: {error_msg}")
