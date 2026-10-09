"""
Action-Conditioned State Representation for ContextGuard.

Formulates explicit action-aware state representations:
- Selected / inferred user action
- Recipient identity verification state (unknown recipients remain unknown)
- Destination exposure scope (public, private vault, external portal)
- Action-conditioned severity of harm and irreversibility
- Epistemic and aleatoric uncertainty
"""

from enum import Enum
from typing import Dict, Any, List, Optional
from pydantic import BaseModel, Field, field_validator


class ContextAction(str, Enum):
    KEEP_LOCALLY = "KEEP_LOCALLY"
    SAVE = "SAVE"
    SEND_PRIVATELY = "SEND_PRIVATELY"
    SEND_UNKNOWN_RECIPIENT = "SEND_UNKNOWN_RECIPIENT"
    SEND = "SEND"
    POST_PUBLICLY = "POST_PUBLICLY"
    POST = "POST"
    OPEN_URL = "OPEN_URL"
    OPEN = "OPEN"
    ENTER_CREDENTIALS = "ENTER_CREDENTIALS"
    LOGIN = "LOGIN"
    APPROVE_PAYMENT = "APPROVE_PAYMENT"
    APPROVE = "APPROVE"
    UPLOAD_DOCUMENT = "UPLOAD_DOCUMENT"
    UPLOAD = "UPLOAD"
    SIGN_DOCUMENT = "SIGN_DOCUMENT"
    SIGN = "SIGN"
    DECLINE = "DECLINE"
    ASK_VERIFY = "ASK_VERIFY"


class DestinationScope(str, Enum):
    PRIVATE_VAULT = "PRIVATE_VAULT"
    DIRECT_MESSAGE_KNOWN = "DIRECT_MESSAGE_KNOWN"
    DIRECT_MESSAGE_UNKNOWN = "DIRECT_MESSAGE_UNKNOWN"
    PUBLIC_FEED = "PUBLIC_FEED"
    EXTERNAL_PORTAL = "EXTERNAL_PORTAL"
    PAYMENT_GATEWAY = "PAYMENT_GATEWAY"
    UNKNOWN = "UNKNOWN"


class ActionConditionedState(BaseModel):
    """
    Explicit feature representation of the evaluated pre-action state.
    """
    artifact_id: Optional[str] = Field(None, description="Optional base artifact identifier")
    selected_action: str = Field(..., description="Action evaluated, e.g. SAVE, SEND, POST, APPROVE")
    intended_recipient: str = Field("UNKNOWN_RECIPIENT", description="Designated recipient persona or entity")
    destination_scope: DestinationScope = Field(DestinationScope.UNKNOWN, description="Target destination channel classification")
    is_public_destination: bool = Field(False, description="True if destination is a public broadcast channel")
    is_unknown_recipient: bool = Field(True, description="True if recipient is unverified or unknown")
    sensitivity: float = Field(0.0, ge=0.0, le=1.0, description="Intrinsic sensitivity of artifact payload")
    severity_of_harm: float = Field(0.0, ge=0.0, le=1.0, description="Action-conditioned potential harm magnitude")
    reversibility: float = Field(0.0, ge=0.0, le=1.0, description="Irreversibility score (0.0=reversible, 1.0=permanent)")
    evidence_reliability: float = Field(1.0, ge=0.0, le=1.0, description="Aggregate extractor reliability score")
    uncertainty: float = Field(0.0, ge=0.0, le=1.0, description="Epistemic/aleatoric uncertainty measure")
    signals: Dict[str, Any] = Field(default_factory=dict, description="Underlying perceptual and model signals")

    @field_validator("sensitivity", "severity_of_harm", "reversibility", "evidence_reliability", "uncertainty", mode="before")
    @classmethod
    def clamp_metrics(cls, v: Any) -> float:
        return max(0.0, min(1.0, float(v)))

    @classmethod
    def from_context(
        cls,
        action: Optional[str],
        recipient: Optional[str],
        destination: Optional[str],
        sensitivity: float = 0.5,
        signals: Optional[Dict[str, Any]] = None
    ) -> "ActionConditionedState":
        """
        Constructs an action-conditioned state enforcing non-hallucination invariants:
        - Unknown recipients remain unknown
        - A visible name does not imply trust without independent verification
        """
        sig = signals or {}
        act_raw = (action or "SAVE").upper().strip()

        # Map common action synonyms
        action_map = {
            "KEEP_LOCALLY": ContextAction.KEEP_LOCALLY.value,
            "SAVE_PRIVATE": ContextAction.SAVE.value,
            "SAVE": ContextAction.SAVE.value,
            "SEND_PRIVATELY": ContextAction.SEND_PRIVATELY.value,
            "SEND_UNKNOWN_RECIPIENT": ContextAction.SEND_UNKNOWN_RECIPIENT.value,
            "SEND": ContextAction.SEND.value,
            "POST_PUBLIC": ContextAction.POST_PUBLICLY.value,
            "POST_PUBLICLY": ContextAction.POST_PUBLICLY.value,
            "POST": ContextAction.POST.value,
            "OPEN_URL": ContextAction.OPEN_URL.value,
            "OPEN": ContextAction.OPEN.value,
            "ENTER_CREDENTIALS": ContextAction.ENTER_CREDENTIALS.value,
            "LOGIN": ContextAction.LOGIN.value,
            "APPROVE_PAYMENT": ContextAction.APPROVE_PAYMENT.value,
            "APPROVE": ContextAction.APPROVE.value,
            "UPLOAD_DOCUMENT": ContextAction.UPLOAD_DOCUMENT.value,
            "UPLOAD": ContextAction.UPLOAD.value,
            "SIGN_DOCUMENT": ContextAction.SIGN_DOCUMENT.value,
            "SIGN": ContextAction.SIGN.value,
            "DECLINE": ContextAction.DECLINE.value,
            "ASK_VERIFY": ContextAction.ASK_VERIFY.value,
        }
        selected_action = action_map.get(act_raw, act_raw)

        # Invariant: Unknown recipients must remain unknown
        recip_str = (recipient or "UNKNOWN_RECIPIENT").strip()
        recip_lower = recip_str.lower()
        dest_str = (destination or "UNKNOWN_DESTINATION").strip()
        dest_lower = dest_str.lower()

        # Invariant: Unknown context is not affirmative evidence of safety.
        # Recipient is only trusted if explicitly verified within a closed/local vault or confirmed contact.
        is_trusted_channel = any(k in dest_lower or k in recip_lower for k in [
            "vault", "personal", "encrypted", "local", "self", "device storage", "my drive", "family",
            "compliance", "official", "verified bank", "internal"
        ])

        is_unknown_recip = (
            not is_trusted_channel
            or recip_str == "UNKNOWN_RECIPIENT"
            or any(k in recip_lower or k in dest_lower for k in [
                "unknown", "unverified", "stranger", "telegram user", "broker",
                "random contact", "external", "contractor", "reporter", "media",
                "third-party", "biz", "consultant", "counterparty", "fake representative",
                "stream", "attachment"
            ])
        )
        if is_trusted_channel and not any(k in recip_lower or k in dest_lower for k in ["unknown", "stranger", "fake", "external", "contractor", "reporter"]):
            is_unknown_recip = False


        # Determine destination scope
        if any(k in dest_lower for k in ["vault", "personal", "encrypted", "local", "self", "device storage", "my drive"]):
            scope = DestinationScope.PRIVATE_VAULT
            is_public = False
        elif any(k in dest_lower for k in ["public", "twitter", "x.com", "social", "forum", "broadcast", "reddit", "facebook"]):
            scope = DestinationScope.PUBLIC_FEED
            is_public = True
        elif any(k in dest_lower for k in ["payment", "gateway", "settlement", "upi"]):
            scope = DestinationScope.PAYMENT_GATEWAY
            is_public = False
        elif any(k in dest_lower for k in ["portal", "http", "www", "web", "cloud"]):
            scope = DestinationScope.EXTERNAL_PORTAL
            is_public = False
        elif is_unknown_recip:
            scope = DestinationScope.DIRECT_MESSAGE_UNKNOWN
            is_public = False
        else:
            scope = DestinationScope.DIRECT_MESSAGE_KNOWN
            is_public = False


        # Compute Action-Conditioned Irreversibility and Severity
        # Reversibility: 0.0 (reversible local action) to 1.0 (irreversible public or financial settlement)
        if selected_action in ["SAVE", "KEEP_LOCALLY", "DECLINE"]:
            reversibility = 0.0
            severity_harm = max(0.05, sensitivity * 0.10)
        elif selected_action in ["POST", "POST_PUBLICLY"]:
            reversibility = 1.0
            severity_harm = max(0.50, min(1.0, sensitivity * 1.30))
        elif selected_action in ["APPROVE", "APPROVE_PAYMENT"]:
            reversibility = 1.0
            severity_harm = max(0.60, min(1.0, sensitivity * 1.20))
        elif selected_action in ["ENTER_CREDENTIALS", "LOGIN"]:
            reversibility = 0.9
            severity_harm = max(0.50, min(1.0, sensitivity * 1.25))
        elif is_unknown_recip:
            reversibility = 0.7
            severity_harm = max(0.40, min(1.0, sensitivity * 1.10))
        else:
            # Send privately to known recipient
            reversibility = 0.4
            severity_harm = max(0.20, sensitivity * 0.70)

        # Epistemic Uncertainty
        uncertainty = 0.05
        if is_unknown_recip:
            uncertainty += 0.30
        if scope == DestinationScope.UNKNOWN:
            uncertainty += 0.25
        if selected_action == "ASK_VERIFY":
            uncertainty = max(uncertainty, 0.70)
        uncertainty = min(1.0, uncertainty)

        return cls(
            selected_action=selected_action,
            intended_recipient=recip_str,
            destination_scope=scope,
            is_public_destination=is_public,
            is_unknown_recipient=is_unknown_recip,
            sensitivity=sensitivity,
            severity_of_harm=severity_harm,
            reversibility=reversibility,
            evidence_reliability=1.0,
            uncertainty=uncertainty,
            signals=sig
        )
