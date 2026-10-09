"""
Canonical schemas for ContextGuard Six-Stage Reasoning Pipeline.
"""

from enum import Enum
from typing import Dict, Any, List, Optional
from pydantic import BaseModel, Field, field_validator


class ActionType(str, Enum):
    SEND = "SEND"
    UPLOAD = "UPLOAD"
    POST = "POST"
    SIGN = "SIGN"
    LOGIN = "LOGIN"
    APPROVE = "APPROVE"
    SAVE = "SAVE"
    OPEN = "OPEN"


class EvidenceSource(str, Enum):
    OCR = "OCR"
    ML_KIT = "ML_KIT"
    URL_MODEL = "URL_MODEL"
    VLM = "VLM"
    CONTEXT = "CONTEXT"
    RULE = "RULE"


class InterventionType(str, Enum):
    STOP = "STOP"
    ASK = "ASK"
    WARN = "WARN"
    ACT = "ACT"


class EvidenceItem(BaseModel):
    type: str = Field(..., description="Evidence category: financial, credential, url_risk, face, etc.")
    description: str = Field(..., description="Specific grounded factual observation")
    importance: float = Field(..., ge=0.0, le=1.0, description="Significance weight in [0.0, 1.0]")
    evidence_source: EvidenceSource = Field(..., description="Source subsystem that produced the evidence")

    @field_validator("importance", mode="before")
    @classmethod
    def clamp_importance(cls, v: Any) -> float:
        return max(0.0, min(1.0, float(v)))


class AnalysisRequest(BaseModel):
    """
    Canonical Input Contract for ContextGuard.
    Explicitly represents unavailable values as None or UNKNOWN without inventing missing fields.
    """
    artifact: Optional[str] = Field(None, description="Optional raw or base64 image / document payload")
    artifact_type: str = Field("IMAGE", description="Type of artifact: IMAGE, URL, DOCUMENT, TEXT, QR")
    ocr_text: Optional[str] = Field(None, description="Extracted on-device OCR text from ML Kit")
    detected_faces: Optional[int] = Field(0, description="Number of detected facial contours from ML Kit")
    detected_pii: Optional[List[str]] = Field(default_factory=list, description="Locally detected PII entities or tokens")
    redaction_metadata: Optional[Dict[str, Any]] = Field(None, description="Client-side redaction bounding boxes and masks")
    url: Optional[str] = Field(None, description="Extracted or clicked URL if present")
    selected_action: Optional[str] = Field(None, description="User-selected action: SEND, UPLOAD, POST, SIGN, LOGIN, APPROVE, SAVE, OPEN")
    recipient: Optional[str] = Field(None, description="Target recipient identity, handle, or contact")
    destination: Optional[str] = Field(None, description="Target destination channel: personal vault, chat DM, public feed, web portal")
    source_app: Optional[str] = Field(None, description="Originating mobile application name / package")
    network_state: Optional[str] = Field(None, description="Network status: WIFI, CELLULAR, VPN, OFFLINE, UNKNOWN")
    user_context: Optional[Dict[str, Any]] = Field(default_factory=dict, description="Additional client context signals")


class Stage1ContextOutput(BaseModel):
    artifact_summary: str
    ocr_available: bool
    ocr_text: Optional[str]
    source_app: str
    recipient: str
    destination: str
    network_state: str
    is_public_channel: bool
    is_trusted_channel: bool
    is_unknown_recipient: bool


class Stage2IntentOutput(BaseModel):
    selected_action: Optional[str]
    inferred_action: Optional[str]
    resolved_action: str
    intent_source: str  # "EXPLICIT_USER" or "INFERRED_RESEARCH"
    consequence_tier: str  # "LOW", "MODERATE", "CRITICAL"


class Stage3EvidenceOutput(BaseModel):
    evidence: List[EvidenceItem]
    total_importance: float
    detected_risk_categories: List[str]


class Stage4ConsequenceOutput(BaseModel):
    severity: float = Field(..., ge=0.0, le=1.0)
    reversibility: float = Field(..., ge=0.0, le=1.0)
    harm_description: str
    reversibility_rationale: str

    @field_validator("severity", "reversibility", mode="before")
    @classmethod
    def clamp_metrics(cls, v: Any) -> float:
        return max(0.0, min(1.0, float(v)))


class Stage5UncertaintyOutput(BaseModel):
    confidence: float = Field(..., ge=0.0, le=1.0)
    uncertainty_reasons: List[str]
    is_epistemic_uncertain: bool

    @field_validator("confidence", mode="before")
    @classmethod
    def clamp_confidence(cls, v: Any) -> float:
        return max(0.0, min(1.0, float(v)))


class AnalysisResponse(BaseModel):
    """
    Canonical Output Contract for ContextGuard.
    """
    intervention: str = Field(..., description="Action-conditioned intervention: STOP, ASK, WARN, ACT")
    risk_score: float = Field(..., description="Composite risk rho = severity * (1 + lambda * reversibility)")
    severity: float = Field(..., ge=0.0, le=1.0, description="Predicted harm severity magnitude in [0.0, 1.0]")
    reversibility: float = Field(..., ge=0.0, le=1.0, description="Harm irreversibility in [0.0, 1.0]")
    confidence: float = Field(..., ge=0.0, le=1.0, description="Confidence in [0.0, 1.0]")
    evidence: List[EvidenceItem] = Field(default_factory=list, description="Grounding evidence items with sources")
    reason: str = Field(..., description="Deterministic, user-facing rationale synthesized from decision fields")
    recommended_alternative: Optional[str] = Field(None, description="Recommended safe alternative action")
    can_override: bool = Field(True, description="Whether intentional user emergency override is permissible")
    stages: Optional[Dict[str, Any]] = Field(None, description="Granular outputs from the six stages for supervision/audit")
    latency_ms: Optional[float] = Field(None, description="Pipeline latency in milliseconds")
    model_path: Optional[str] = Field("Qwen 2.5-VL 3B + XGBoost Hybrid", description="Active model path")
    network_mode: Optional[str] = Field("REDACTED_LOCAL_BACKEND", description="Network privacy transmission mode")
    policy_version: Optional[str] = Field("2.1.0-action-cascade", description="Pre-action policy engine version")
    canonical_evidence: Optional[List[Dict[str, Any]]] = Field(default_factory=list, description="Typed canonical evidence records with full provenance")
    action_state: Optional[Dict[str, Any]] = Field(None, description="Explicit action-conditioned state feature representation")
    conformal_set: Optional[List[str]] = Field(None, description="Split conformal prediction set C(x)")
    abstention_applied: bool = Field(False, description="True if selective prediction abstention triggered ASK")

    @field_validator("severity", "reversibility", "confidence", mode="before")
    @classmethod
    def clamp_metrics(cls, v: Any) -> float:
        return max(0.0, min(1.0, float(v)))
