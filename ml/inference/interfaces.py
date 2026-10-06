"""
Abstract base class interfaces for ContextGuard ML and decision components.

These define strict architectural contracts for:
1. URLRiskModel (trained XGBoost phishing classifier)
2. VisionReasoner (multimodal Qwen2.5-VL-3B reasoner)
3. ArtifactAnalyzer (inference orchestrator)
4. PolicyEngine (deterministic mathematical safety gating)
"""

from abc import ABC, abstractmethod
from typing import List, Optional

from ml.inference.types import (
    ActionContext,
    ArtifactInput,
    ArtifactAnalysisResult,
    URLRiskOutput,
    VisionReasoningOutput,
    PolicyDecision,
    Intervention,
)


class URLRiskModel(ABC):
    """
    Interface for URL-level phishing and malicious link assessment.
    Production implementation will load trained XGBoost model from PhiUSIIL dataset.
    """

    @abstractmethod
    def predict_risk(self, url: str) -> URLRiskOutput:
        """
        Extract lexical/structural features and evaluate phishing probability.
        """
        pass

    @abstractmethod
    def is_ready(self) -> bool:
        """
        Check if model weights and feature extractor are loaded.
        """
        pass


class VisionReasoner(ABC):
    """
    Interface for multimodal vision-language reasoning over redacted artifacts.
    Production implementation connects to local Ollama/Qwen2.5-VL-3B.
    """

    @abstractmethod
    def reason(
        self,
        image_bytes: Optional[bytes],
        extracted_text: Optional[str],
        context: ActionContext,
    ) -> VisionReasoningOutput:
        """
        Analyze visual tokens, OCR text, and action context to estimate severity,
        reversibility, confidence, and hazard rationale.
        """
        pass

    @abstractmethod
    def is_ready(self) -> bool:
        """
        Check if VLM endpoint is reachable and responsive.
        """
        pass


class PolicyEngine(ABC):
    """
    Interface for deterministic pre-action safety policy calculation.
    Enforces rho = s * (1 + lambda * r) and threshold gating.
    Enforces Model Failure Safety Rule (never silently emit ACT).
    """

    @abstractmethod
    def evaluate(
        self,
        severity: float,
        reversibility: float,
        confidence: float,
        evidence: Optional[List[str]] = None,
        context_notes: Optional[str] = None,
    ) -> PolicyDecision:
        """
        Calculate composite risk and map to ACT, ASK, WARN, or STOP.
        """
        pass

    @abstractmethod
    def safe_fallback(self, error_message: str) -> PolicyDecision:
        """
        Model Failure Safety Rule: Emit ASK or safe fallback upon model or parse failure.
        """
        pass


class ArtifactAnalyzer(ABC):
    """
    Interface for orchestrating multi-layer perception, inference, and policy gating.
    """

    @abstractmethod
    def analyze(
        self,
        artifact: ArtifactInput,
        context: ActionContext,
        inference_mode: str = "REDACTED_LOCAL_BACKEND",
    ) -> ArtifactAnalysisResult:
        """
        Run end-to-end evaluation: URL risk + Vision reasoning + Deterministic policy.
        """
        pass

    @abstractmethod
    def get_health_status(self) -> dict:
        """
        Report availability of underlying models and policy components.
        """
        pass
