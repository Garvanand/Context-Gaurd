"""
Type definitions and schemas for ContextGuard ML inference interfaces.
"""

from enum import Enum
from typing import List, Dict, Any, Optional
from pydantic import BaseModel, Field


class Intervention(str, Enum):
    ACT = "ACT"
    ASK = "ASK"
    WARN = "WARN"
    STOP = "STOP"


class ActionContext(BaseModel):
    """
    Context surrounding the digital artifact and user's intended action.
    """
    source_app: str = Field(..., description="Application origin (e.g. WhatsApp, BankingApp, Chrome)")
    intended_action: str = Field(..., description="Action user intends to perform (e.g. save_vault, post_public, dm_transfer)")
    recipient: Optional[str] = Field(None, description="Target recipient or identity")
    destination: Optional[str] = Field(None, description="Destination medium (e.g. Encrypted Vault, Telegram, Twitter)")
    user_intent_notes: Optional[str] = Field(None, description="Optional user commentary or description")


class ArtifactInput(BaseModel):
    """
    Representation of an artifact presented for pre-action evaluation.
    """
    artifact_id: str = Field(..., description="Unique or transient ID for this analysis session")
    artifact_type: str = Field(..., description="MIME type or category: image/png, text/plain, url")
    artifact_bytes: Optional[bytes] = Field(None, description="In-memory bytes of the redacted artifact")
    extracted_text: Optional[str] = Field(None, description="OCR text extracted on-device via ML Kit")
    detected_urls: List[str] = Field(default_factory=list, description="URLs extracted from text or artifact")
    pii_entities_count: int = Field(0, description="Count of masked PII entities detected on-device")
    detected_faces_count: int = Field(0, description="Count of detected human faces")
    sha256_hash: str = Field(..., description="Cryptographic SHA-256 digest of the artifact")


class URLRiskOutput(BaseModel):
    """
    Output of the URLRiskModel classifier.
    """
    url: str
    risk_score: float = Field(..., ge=0.0, le=1.0, description="Predicted probability of phishing/malicious URL")
    confidence: float = Field(..., ge=0.0, le=1.0, description="Model prediction confidence")
    is_phishing: bool = Field(..., description="Binary classification at operating threshold")
    extracted_features: Dict[str, float] = Field(default_factory=dict, description="Lexical and structural features")
    model_version: str = Field("placeholder_v0", description="Model version identifier")


class VisionReasoningOutput(BaseModel):
    """
    Output of the Multimodal Vision-Language Reasoner.
    """
    severity: float = Field(..., ge=0.0, le=1.0, description="Estimated harm severity s in [0, 1]")
    reversibility: float = Field(..., ge=0.0, le=1.0, description="Consequence reversibility r in [0, 1]")
    confidence: float = Field(..., ge=0.0, le=1.0, description="Epistemic/aleatoric confidence c in [0, 1]")
    hazard_types: List[str] = Field(default_factory=list, description="Detected hazard taxonomy tags")
    evidence: List[str] = Field(default_factory=list, description="Concrete perceptual evidence items")
    rationale: str = Field(..., description="Structured explanation of reasoning")
    model_name: str = Field("placeholder_vlm", description="Underlying VLM identifier")


class PolicyDecision(BaseModel):
    """
    Output of the Deterministic Policy Engine.
    """
    intervention: Intervention = Field(..., description="ACT, ASK, WARN, or STOP")
    risk_score: float = Field(..., description="Calculated composite risk rho = s * (1 + lambda * r)")
    severity: float = Field(..., ge=0.0, le=1.0)
    reversibility: float = Field(..., ge=0.0, le=1.0)
    confidence: float = Field(..., ge=0.0, le=1.0)
    reversibility_weight_lambda: float = Field(0.75, description="Lambda penalty coefficient applied")
    stop_threshold: float = Field(0.65)
    ask_threshold: float = Field(0.35)
    min_confidence_threshold: float = Field(0.70)
    evidence: List[str] = Field(default_factory=list)
    rationale: str = Field(..., description="Explanation of threshold gating")
    fallback_applied: bool = Field(False, description="True if safe fallback triggered via Model Failure Safety Rule")


class ArtifactAnalysisResult(BaseModel):
    """
    Complete end-to-end response synthesizing all ML layers.
    """
    artifact_id: str
    sha256_hash: str
    policy_decision: PolicyDecision
    url_risk: Optional[URLRiskOutput] = None
    vision_reasoning: Optional[VisionReasoningOutput] = None
    latency_ms: float = Field(0.0, description="Total pipeline latency in milliseconds")
    inference_mode: str = Field("placeholder", description="OFFLINE, REDACTED_LOCAL_BACKEND, etc.")
