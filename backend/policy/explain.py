"""
Deterministic Explanation Generator for ContextGuard.

Synthesizes user-facing rationale and recommended alternatives directly
from mathematical decision fields, evidence items, and contextual attributes.
NO secondary LLM is used, guaranteeing faithful, non-hallucinatory explanations.
"""

from typing import List, Dict, Any, Optional, Tuple
from backend.app.models.schemas import EvidenceItem


def synthesize_explanation(
    intervention: str,
    risk_score: float,
    severity: float,
    reversibility: float,
    confidence: float,
    evidence: List[EvidenceItem],
    action: Optional[str] = None,
    destination: Optional[str] = None,
    recipient: Optional[str] = None,
    fallback_applied: bool = False,
    fallback_reason: Optional[str] = None,
) -> Tuple[str, Optional[str]]:
    """
    Deterministically constructs (reason, recommended_alternative).
    """
    act = (action or "selected action").upper()
    dest = destination or "target destination"
    recip = recipient or "unspecified recipient"

    # 1. Fallback or safeguard triggered
    if fallback_applied:
        reason = (
            f"ContextGuard Safeguard Rule engaged: {fallback_reason or 'Model evaluation unavailable'}. "
            f"System policy strictly prohibits automated execution when epistemic confidence is compromised."
        )
        alternative = "Verify the artifact and target manually before proceeding."
        return reason, alternative

    # Extract high-importance evidence summaries
    top_evidence = [e.description for e in sorted(evidence, key=lambda x: x.importance, reverse=True)[:3]]
    evidence_str = "; ".join(top_evidence) if top_evidence else "contextual risk indicators"

    # Analyze key hazard types
    ev_types = [e.type.lower() for e in evidence]
    is_credential = any("credential" in t or "otp" in t or "password" in t for t in ev_types)
    is_phishing = any("phishing" in t or "url" in t or "suspicious" in t for t in ev_types)
    is_financial = any("financial" in t or "bank" in t or "payment" in t for t in ev_types)
    is_public = any(k in dest.lower() for k in ["public", "twitter", "social", "forum", "broadcast"])
    is_untrusted = any(k in recip.lower() or k in dest.lower() for k in ["unknown", "unverified", "stranger"])

    # 2. STOP (Catastrophic or Irreversible Harm Blocked)
    if intervention == "STOP":
        if is_phishing:
            reason = (
                f"Critical security hazard detected. Attempting to {act} against an unverified or phishing endpoint "
                f"({evidence_str}). Immediate risk of credential theft or account compromise."
            )
            alternative = "Abort interaction immediately. Access the service only through official, verified applications."
        elif is_credential and is_untrusted:
            reason = (
                f"Authentication credential transmission blocked. Sharing sensitive credentials or OTP with an "
                f"unverified recipient ({evidence_str}) enables immediate account takeover."
            )
            alternative = "Never disclose one-time passwords, secret tokens, or private keys to any third party."
        elif is_public and (is_financial or is_credential):
            reason = (
                f"Irreversible data exposure blocked. Publicly broadcasting confidential financial or identity artifacts "
                f"to '{dest}' ({evidence_str}) causes permanent, uncontainable disclosure (reversibility={reversibility:.2f})."
            )
            alternative = "Save artifact to a personal encrypted vault or redact sensitive account numbers before publishing."
        else:
            reason = (
                f"High-consequence action blocked (risk_score={risk_score:.2f}, severity={severity:.2f}, "
                f"reversibility={reversibility:.2f}). Grounded evidence: {evidence_str}."
            )
            alternative = "Store artifact in a secure local vault or consult a security supervisor before proceeding."
        return reason, alternative

    # 3. ASK (Ambiguity or Low Confidence)
    elif intervention == "ASK":
        if is_untrusted:
            reason = (
                f"Unverified recipient advisory. Attempting to {act} sensitive artifact to unverified recipient '{recip}' "
                f"presents potential risk with high uncertainty (confidence={confidence:.2f}). Evidence: {evidence_str}."
            )
            alternative = "Verify the recipient's phone number or address via an independent trusted channel before sending."
        elif confidence < 0.70:
            reason = (
                f"Context uncertainty detected. The system detected potential risk (risk_score={risk_score:.2f}) "
                f"but epistemic confidence ({confidence:.2f}) is below the safety threshold. Evidence: {evidence_str}."
            )
            alternative = "Inspect the destination and artifact contents manually to verify safe delivery."
        else:
            reason = (
                f"Action confirmation required. Attempting to {act} to '{dest}' involves sensitive artifacts "
                f"({evidence_str}). User verification mandated by safety policy."
            )
            alternative = "Review recipient permissions or apply on-device redaction before confirmation."
        return reason, alternative

    # 4. WARN (Known Moderate Risk, Proceed with Caution)
    elif intervention == "WARN":
        reason = (
            f"Moderate risk advisory (risk_score={risk_score:.2f}, severity={severity:.2f}). "
            f"Action '{act}' directed to '{dest}' will expose sensitive elements ({evidence_str}). "
            f"Proceed only if you intentionally intend this disclosure."
        )
        alternative = "Consider redacting sensitive entities or restricting audience access."
        return reason, alternative

    # 5. ACT (Safe to Proceed)
    else:
        reason = (
            f"Action '{act}' evaluated as safe (risk_score={risk_score:.2f}, severity={severity:.2f}, "
            f"reversibility={reversibility:.2f}). Scoped to trusted environment without hazardous exposure."
        )
        alternative = None
        return reason, alternative
