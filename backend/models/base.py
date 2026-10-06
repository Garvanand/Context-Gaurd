"""
Base abstractions, Pydantic schemas, and image processing for Vision Reasoner.
"""

from abc import ABC, abstractmethod
import io
import base64
from typing import List, Dict, Any, Optional, Tuple
from pydantic import BaseModel, Field, field_validator
from PIL import Image

# Supported MIME / format extensions
SUPPORTED_IMAGE_FORMATS = {"JPEG", "JPG", "PNG", "WEBP"}
MAX_IMAGE_BYTES = 10 * 1024 * 1024  # 10 MB limit
MAX_IMAGE_DIMENSION = 1536  # Max width/height to normalize for VLM token limits


class EvidenceItem(BaseModel):
    type: str = Field(..., description="Evidence category: text, visual, recipient, destination")
    description: str = Field(..., description="Clear description of the concrete observation")
    importance: float = Field(..., ge=0.0, le=1.0, description="Significance weight between 0.0 and 1.0")

    @field_validator("importance", mode="before")
    @classmethod
    def clamp_importance(cls, v: Any) -> float:
        return max(0.0, min(1.0, float(v)))


class InferredIntentOutput(BaseModel):
    predicted_action: str = Field(..., description="The most likely intended action")
    probabilities: Dict[str, float] = Field(..., description="Probabilities for candidate actions")
    confidence: float = Field(..., ge=0.0, le=1.0, description="Confidence in the inferred action")
    ambiguity: List[str] = Field(default_factory=list, description="Reasons for ambiguity if evidence is insufficient")

    @field_validator("confidence", mode="before")
    @classmethod
    def clamp_confidence(cls, v: Any) -> float:
        return max(0.0, min(1.0, float(v)))


class StructuredVisionOutput(BaseModel):
    """
    Strict JSON output contract for multimodal vision reasoning.
    No free-form prose. All numeric fields strictly bounded in [0.0, 1.0].
    """
    context_summary: str = Field(..., description="Summary of the artifact and visual domain")
    intent_assessment: str = Field(..., description="Evaluation of intended action and destination channel")
    evidence: List[EvidenceItem] = Field(default_factory=list, description="Extracted evidence pieces")
    risk_type: str = Field(..., description="Taxonomy classification: Financial, Digital Security, Privacy Disclosure, Communication")
    risk_subtype: str = Field(..., description="Fine-grained hazard classification")
    severity: float = Field(..., ge=0.0, le=1.0, description="Harm severity magnitude s in [0.0, 1.0]")
    reversibility: float = Field(..., ge=0.0, le=1.0, description="Harm irreversibility r in [0.0, 1.0]")
    confidence: float = Field(..., ge=0.0, le=1.0, description="Epistemic confidence c in [0.0, 1.0]")
    uncertainty_reasons: List[str] = Field(default_factory=list, description="Factors contributing to uncertainty")
    recommended_action: str = Field(..., description="Safety intervention: ACT, ASK, WARN, or STOP")
    alternative_action: Optional[str] = Field(None, description="Safe alternative recommendation if available")
    reason: str = Field(..., description="Structured explanation of evaluation and gating")

    @field_validator("severity", "reversibility", "confidence", mode="before")
    @classmethod
    def clamp_metrics(cls, v: Any) -> float:
        return max(0.0, min(1.0, float(v)))


class ImageNormalizer:
    """
    Secure in-memory image validator and normalizer.
    Enforces format constraints, dimension bounds, and memory-only processing.
    """

    @classmethod
    def normalize_image(
        cls,
        image_bytes: bytes,
        max_dimension: int = MAX_IMAGE_DIMENSION,
        max_bytes: int = MAX_IMAGE_BYTES
    ) -> Tuple[bytes, str, Tuple[int, int]]:
        """
        Validate, normalize format to RGB JPEG, and return (normalized_bytes, base64_str, (width, height)).
        Does NOT write any data to disk.
        """
        if not image_bytes:
            raise ValueError("Image bytes payload is empty.")

        if len(image_bytes) > max_bytes:
            raise ValueError(f"Image exceeds size limit of {max_bytes / (1024 * 1024):.1f} MB (got {len(image_bytes) / (1024 * 1024):.1f} MB)")

        try:
            bio = io.BytesIO(image_bytes)
            img = Image.open(bio)
            img_format = (img.format or "").upper()
        except Exception as e:
            raise ValueError(f"Failed to decode image bytes: {e}")

        if img_format not in SUPPORTED_IMAGE_FORMATS:
            raise ValueError(f"Unsupported image format: '{img_format}'. Supported formats: {SUPPORTED_IMAGE_FORMATS}")

        # Convert palette/transparency to RGB
        if img.mode != "RGB":
            img = img.convert("RGB")

        # Downscale if exceeding max_dimension
        w, h = img.size
        if max(w, h) > max_dimension:
            scale = max_dimension / float(max(w, h))
            new_w = max(1, int(w * scale))
            new_h = max(1, int(h * scale))
            img = img.resize((new_w, new_h), Image.Resampling.LANCZOS)
            w, h = new_w, new_h

        # Re-encode in memory as optimized JPEG
        out_bio = io.BytesIO()
        img.save(out_bio, format="JPEG", quality=88, optimize=True)
        normalized_bytes = out_bio.getvalue()
        b64_str = base64.b64encode(normalized_bytes).decode("utf-8")

        return normalized_bytes, b64_str, (w, h)


class VisionReasoner(ABC):
    """
    Abstract interface for multimodal vision reasoning.
    """

    @abstractmethod
    async def analyze(
        self,
        image_bytes: Optional[bytes],
        ocr_text: Optional[str],
        context: Dict[str, Any],
    ) -> StructuredVisionOutput:
        """
        Perform multimodal action-conditioned reasoning.
        """
        pass

    @abstractmethod
    async def infer_intent(
        self,
        image_bytes: Optional[bytes],
        ocr_text: Optional[str],
        context: Dict[str, Any],
    ) -> InferredIntentOutput:
        """
        Infer the user's intent based on contextual evidence and artifact.
        """
        pass

    @abstractmethod
    async def check_health(self) -> Dict[str, Any]:
        """
        Verify provider status and return telemetry.
        """
        pass
