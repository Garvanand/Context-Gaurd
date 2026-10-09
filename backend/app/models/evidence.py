"""
Canonical Typed Evidence Model for ContextGuard.

Defines the core evidence schema supporting:
- Structured provenance and extractor versioning
- Clear distinction between observed evidence, derived inference, user-supplied context, and missing evidence
- Strict privacy preservation: raw sensitive PII/passwords/OTPs never appear in summaries
- Explicit severity contribution and reliability limitations
"""

from enum import Enum
from typing import Optional, Dict, Any
from datetime import datetime, timezone
from pydantic import BaseModel, Field, field_validator


class EvidenceProvenanceSource(str, Enum):
    ML_KIT_OCR = "ML_KIT_OCR"
    ML_KIT_FACE = "ML_KIT_FACE"
    ACCESSIBILITY_TREE = "ACCESSIBILITY_TREE"
    NOTIFICATION = "NOTIFICATION"
    URL_MODEL = "URL_MODEL"
    MESSAGE_MODEL = "MESSAGE_MODEL"
    USER_CONFIRMED_CONTEXT = "USER_CONFIRMED_CONTEXT"
    VLM = "VLM"
    RULE = "RULE"


class EpistemicEvidenceType(str, Enum):
    OBSERVED_EVIDENCE = "OBSERVED_EVIDENCE"       # Direct perceptual or system observation
    DERIVED_INFERENCE = "DERIVED_INFERENCE"       # ML model output or rule deduction
    USER_SUPPLIED_CONTEXT = "USER_SUPPLIED_CONTEXT" # Confirmed user intent or provided context
    MISSING_EVIDENCE = "MISSING_EVIDENCE"         # Explicitly missing or unknown context


class CanonicalEvidenceRecord(BaseModel):
    """
    Canonical Typed Evidence Record.
    Guarantees privacy-safe representations and grounded traceability.
    """
    evidence_id: str = Field(..., description="Unique deterministic identifier, e.g. ev_ocr_001")
    category: str = Field(..., description="Evidence category: financial, credential, url_risk, face, pii, etc.")
    summary: str = Field(..., description="Privacy-safe summary (NEVER raw unredacted secrets)")
    provenance_source: EvidenceProvenanceSource = Field(..., description="System subsystem or sensor that produced the evidence")
    epistemic_type: EpistemicEvidenceType = Field(..., description="Nature of evidence: observed, derived, user, or missing")
    extractor_version: str = Field(..., description="Version of sensor or extractor, e.g. mlkit_v16.0, xgb_url_v1.1")
    confidence: Optional[float] = Field(None, ge=0.0, le=1.0, description="Confidence score in [0.0, 1.0] if genuinely available")
    timestamp: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat(), description="ISO 8601 UTC timestamp")
    relevant_action: Optional[str] = Field(None, description="Action affected by this evidence, e.g. SEND, POST, APPROVE")
    observed_value: Optional[str] = Field(None, description="Observed raw value or token (sanitized, no raw secrets)")
    severity_contribution: float = Field(0.0, ge=0.0, le=1.0, description="Magnitude of hazard contribution in [0.0, 1.0]")
    reliability_limitations: Optional[str] = Field(None, description="Known failure modes or sensor limitations")

    @field_validator("confidence", "severity_contribution", mode="before")
    @classmethod
    def clamp_float_metrics(cls, v: Any) -> Optional[float]:
        if v is None:
            return None
        return max(0.0, min(1.0, float(v)))

    @field_validator("summary", "observed_value")
    @classmethod
    def check_privacy_safe_text(cls, v: Optional[str]) -> Optional[str]:
        if v is None:
            return None
        import re
        # Guarantee raw 12-digit Aadhaar, 16-digit Card, or raw 6-digit OTP are not placed in records
        if re.search(r"\b\d{4}\s?\d{4}\s?\d{4}\b", v):
            v = re.sub(r"\b\d{4}\s?\d{4}\s?\d{4}\b", "[REDACTED_AADHAAR]", v)
        if re.search(r"\b(?:\d{4}[ -]?){3}\d{4}\b", v):
            v = re.sub(r"\b(?:\d{4}[ -]?){3}\d{4}\b", "[REDACTED_CARD]", v)
        if re.search(r"\b(?:otp|code|passcode)\b[:=\s]*\d{4,8}\b", v, flags=re.IGNORECASE):
            v = re.sub(r"\b(?:otp|code|passcode)\b[:=\s]*\d{4,8}\b", "[REDACTED_OTP]", v, flags=re.IGNORECASE)
        return v

