"""
Comprehensive unit tests for the Multimodal AI Vision Reasoner subsystem:
- In-memory ImageNormalizer (JPEG, PNG, WebP, dimensions, byte limits, corrupt payloads)
- Versioned immutable prompts verification (all 6 v1 prompt files)
- Strict JSON Schema constraints and bounding [0.0, 1.0]
- QwenVisionReasoner execution, parsing, and action-conditioned safe fallbacks
- Ollama service health probing and telemetry contract
"""

import io
import json
import pytest
from pathlib import Path
from unittest.mock import patch, AsyncMock
from PIL import Image

from backend.models.base import (
    ImageNormalizer,
    EvidenceItem,
    StructuredVisionOutput,
    MAX_IMAGE_BYTES,
    MAX_IMAGE_DIMENSION,
)
from backend.models.qwen_vision import QwenVisionReasoner, PROMPTS_DIR


# =====================================================================
# 1. ImageNormalizer Unit Tests
# =====================================================================

def create_synthetic_image(fmt: str, size=(400, 300), mode="RGB") -> bytes:
    """Helper to create dummy in-memory image bytes."""
    img = Image.new(mode, size, color=(100, 150, 200))
    bio = io.BytesIO()
    img.save(bio, format=fmt)
    return bio.getvalue()


def test_image_normalizer_jpeg():
    raw_bytes = create_synthetic_image("JPEG")
    norm_bytes, b64_str, dimensions = ImageNormalizer.normalize_image(raw_bytes)

    assert len(norm_bytes) > 0
    assert isinstance(b64_str, str) and len(b64_str) > 0
    assert dimensions == (400, 300)
    # Check that output is valid JPEG
    out_img = Image.open(io.BytesIO(norm_bytes))
    assert out_img.format == "JPEG"
    assert out_img.mode == "RGB"


def test_image_normalizer_png_rgba_conversion():
    # RGBA PNG should convert smoothly to RGB JPEG without alpha transparency crash
    raw_bytes = create_synthetic_image("PNG", mode="RGBA")
    norm_bytes, b64_str, dimensions = ImageNormalizer.normalize_image(raw_bytes)

    assert dimensions == (400, 300)
    out_img = Image.open(io.BytesIO(norm_bytes))
    assert out_img.format == "JPEG"
    assert out_img.mode == "RGB"


def test_image_normalizer_webp():
    raw_bytes = create_synthetic_image("WEBP")
    norm_bytes, b64_str, dimensions = ImageNormalizer.normalize_image(raw_bytes)

    assert dimensions == (400, 300)
    out_img = Image.open(io.BytesIO(norm_bytes))
    assert out_img.format == "JPEG"


def test_image_normalizer_downscaling():
    # Large image exceeding MAX_IMAGE_DIMENSION
    large_bytes = create_synthetic_image("PNG", size=(3000, 1500))
    norm_bytes, b64_str, (w, h) = ImageNormalizer.normalize_image(large_bytes, max_dimension=1536)

    assert max(w, h) <= 1536
    assert w == 1536
    assert h == 768


def test_image_normalizer_exceeds_byte_limit():
    dummy_bytes = b"0" * (11 * 1024 * 1024)  # 11 MB > 10 MB limit
    with pytest.raises(ValueError, match="Image exceeds size limit"):
        ImageNormalizer.normalize_image(dummy_bytes)


def test_image_normalizer_corrupt_bytes():
    corrupt_bytes = b"not_an_image_file_content_garbage"
    with pytest.raises(ValueError, match="Failed to decode image bytes"):
        ImageNormalizer.normalize_image(corrupt_bytes)


def test_image_normalizer_empty_bytes():
    with pytest.raises(ValueError, match="Image bytes payload is empty"):
        ImageNormalizer.normalize_image(b"")


# =====================================================================
# 2. Immutable Prompt Verification
# =====================================================================

def test_versioned_prompts_exist_and_non_empty():
    required_prompts = [
        "context_v1.txt",
        "intent_v1.txt",
        "evidence_v1.txt",
        "consequence_v1.txt",
        "uncertainty_v1.txt",
        "reasoning_v1.txt",
    ]

    for p_name in required_prompts:
        prompt_path = PROMPTS_DIR / p_name
        assert prompt_path.exists(), f"Missing required prompt: {p_name}"
        content = prompt_path.read_text(encoding="utf-8").strip()
        assert len(content) > 50, f"Prompt {p_name} is too short or empty"


def test_format_prompt_substitution():
    reasoner = QwenVisionReasoner()
    prompt = reasoner.format_prompt(
        ocr_text="Account Balance: $10,000",
        context={
            "source_app": "BankingApp",
            "intended_action": "Share Image",
            "destination": "Public Twitter Feed",
            "recipient": "Public Broadcast",
        },
    )

    assert "BankingApp" in prompt
    assert "Share Image" in prompt
    assert "Public Twitter Feed" in prompt
    assert "Account Balance: $10,000" in prompt
    assert "JSON" in prompt


# =====================================================================
# 3. Strict Structured Output Schema Tests
# =====================================================================

def test_structured_vision_output_clamping():
    evidence = [EvidenceItem(type="text", description="Found bank logo", importance=1.5)]
    # importance should clamp to 1.0
    assert evidence[0].importance == 1.0

    output = StructuredVisionOutput(
        context_summary="Test summary",
        intent_assessment="Test intent",
        evidence=evidence,
        risk_type="Financial",
        risk_subtype="Data Exposure",
        severity=1.2,       # Out of bounds (>1.0)
        reversibility=-0.4, # Out of bounds (<0.0)
        confidence=2.5,     # Out of bounds (>1.0)
        uncertainty_reasons=["Test reason"],
        recommended_action="STOP",
        reason="Test reason",
    )

    assert output.severity == 1.0
    assert output.reversibility == 0.0
    assert output.confidence == 1.0
    assert output.recommended_action == "STOP"


def test_structured_vision_output_missing_field_fails():
    with pytest.raises(Exception):
        # Missing required 'severity' and 'reversibility'
        StructuredVisionOutput(
            context_summary="Test",
            intent_assessment="Test",
            risk_type="Financial",
            risk_subtype="Exposure",
            recommended_action="STOP",
            reason="Test",
        )


# =====================================================================
# 4. QwenVisionReasoner Fallback and Action-Conditioning Tests
# =====================================================================

@pytest.mark.asyncio
async def test_reasoner_fallback_financial_public_blocks():
    """
    Public broadcast of sensitive financial statement must result in STOP.
    """
    reasoner = QwenVisionReasoner()
    context = {
        "intended_action": "post_tweet",
        "destination": "public twitter feed",
        "recipient": "public followers",
    }
    result = await reasoner.analyze(
        image_bytes=None,
        ocr_text="Account statement ending 1234, balance $45,000",
        context=context,
    )

    assert isinstance(result, StructuredVisionOutput)
    assert result.recommended_action == "STOP"
    assert result.severity >= 0.85
    assert result.reversibility >= 0.90
    assert "public" in result.reason.lower()


@pytest.mark.asyncio
async def test_reasoner_fallback_financial_personal_vault_allows():
    """
    Archiving financial statement to private encrypted vault must result in ACT.
    """
    reasoner = QwenVisionReasoner()
    context = {
        "intended_action": "encrypt_and_save",
        "destination": "encrypted personal local vault",
        "recipient": "self",
    }
    result = await reasoner.analyze(
        image_bytes=None,
        ocr_text="Account statement ending 1234, balance $45,000",
        context=context,
    )

    assert isinstance(result, StructuredVisionOutput)
    assert result.recommended_action == "ACT"
    assert result.severity <= 0.10
    assert result.reversibility == 0.0


@pytest.mark.asyncio
async def test_reasoner_fallback_financial_untrusted_asks():
    """
    Sending financial statement to unverified contact must result in ASK.
    """
    reasoner = QwenVisionReasoner()
    context = {
        "intended_action": "send_file",
        "destination": "telegram direct chat",
        "recipient": "unknown unverified user",
    }
    result = await reasoner.analyze(
        image_bytes=None,
        ocr_text="Account statement ending 1234, balance $45,000",
        context=context,
    )

    assert isinstance(result, StructuredVisionOutput)
    assert result.recommended_action == "ASK"
    assert len(result.uncertainty_reasons) > 0


@pytest.mark.asyncio
async def test_reasoner_fallback_ambiguous_context_never_silent_act():
    """
    Model Failure Safety Rule: Ambiguous or unknown context must never silently ACT.
    """
    reasoner = QwenVisionReasoner()
    context = {
        "intended_action": "unknown_action",
        "destination": "unknown_destination",
        "recipient": "unknown",
    }
    result = await reasoner.analyze(
        image_bytes=None,
        ocr_text="Random generic notes",
        context=context,
    )

    assert isinstance(result, StructuredVisionOutput)
    assert result.recommended_action in ("ASK", "WARN", "STOP")
    assert result.recommended_action != "ACT"


# =====================================================================
# 5. Ollama Response Parsing and Mock Inference Tests
# =====================================================================

@pytest.mark.asyncio
async def test_reasoner_successful_mock_ollama_json():
    mock_payload = {
        "context_summary": "Bank statement with account balances.",
        "intent_assessment": "User is attempting to share via email.",
        "evidence": [
            {"type": "text", "description": "Routing number visible", "importance": 0.9}
        ],
        "risk_type": "Financial",
        "risk_subtype": "Sensitive Data Leak",
        "severity": 0.8,
        "reversibility": 0.9,
        "confidence": 0.85,
        "uncertainty_reasons": [],
        "recommended_action": "STOP",
        "alternative_action": "Redact account numbers first",
        "reason": "Financial data exposed to external destination.",
    }

    from unittest.mock import MagicMock
    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.json.return_value = {
        "message": {"content": json.dumps(mock_payload)}
    }

    with patch("httpx.AsyncClient.post", new_callable=AsyncMock, return_value=mock_resp):
        reasoner = QwenVisionReasoner()
        result = await reasoner.analyze(
            image_bytes=create_synthetic_image("JPEG"),
            ocr_text="Account 9876",
            context={"intended_action": "email", "destination": "external"},
        )

        assert result.recommended_action == "STOP"
        assert result.severity == 0.8
        assert result.risk_type == "Financial"
        assert len(result.evidence) == 1
        assert result.evidence[0].importance == 0.9


@pytest.mark.asyncio
async def test_reasoner_markdown_codeblock_parsing():
    raw_json_str = json.dumps({
        "context_summary": "Photo with credentials.",
        "intent_assessment": "Sharing on social network.",
        "evidence": [],
        "risk_type": "Digital Security",
        "risk_subtype": "Credential Exposure",
        "severity": 0.95,
        "reversibility": 1.0,
        "confidence": 0.9,
        "uncertainty_reasons": [],
        "recommended_action": "STOP",
        "alternative_action": None,
        "reason": "Credentials cannot be shared publicly.",
    })
    markdown_wrapped = f"```json\n{raw_json_str}\n```"

    from unittest.mock import MagicMock
    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.json.return_value = {"message": {"content": markdown_wrapped}}

    with patch("httpx.AsyncClient.post", new_callable=AsyncMock, return_value=mock_resp):
        reasoner = QwenVisionReasoner()
        result = await reasoner.analyze(
            image_bytes=None,
            ocr_text="password: 123",
            context={"intended_action": "post", "destination": "twitter"},
        )

        assert result.recommended_action == "STOP"
        assert result.severity == 0.95


@pytest.mark.asyncio
async def test_reasoner_malformed_json_triggers_safe_fallback():
    from unittest.mock import MagicMock
    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.json.return_value = {"message": {"content": "Not a valid json response at all!"}}

    with patch("httpx.AsyncClient.post", new_callable=AsyncMock, return_value=mock_resp):
        reasoner = QwenVisionReasoner()
        result = await reasoner.analyze(
            image_bytes=None,
            ocr_text="bank statement balance $500",
            context={"intended_action": "share", "destination": "public"},
        )

        # Must invoke safe fallback and never crash or silently ACT
        assert isinstance(result, StructuredVisionOutput)
        assert result.recommended_action == "STOP"


# =====================================================================
# 6. Real Health Probe Contract Tests
# =====================================================================

@pytest.mark.asyncio
async def test_health_check_service_offline():
    reasoner = QwenVisionReasoner()
    # When Ollama is offline or uncontactable
    health = await reasoner.check_health()

    assert health["model_name"] == reasoner.model_name
    assert "ollama_reachable" in health
    assert "model_installed" in health
    assert "test_inference_status" in health
    assert "startup_latency_ms" in health
    assert "fallback_active" in health


@pytest.mark.asyncio
async def test_health_check_model_not_pulled_mock():
    from unittest.mock import MagicMock
    mock_tags_resp = MagicMock()
    mock_tags_resp.status_code = 200
    mock_tags_resp.json.return_value = {
        "models": [{"name": "llama3:latest"}, {"name": "mistral:latest"}]
    }

    with patch("httpx.AsyncClient.get", new_callable=AsyncMock, return_value=mock_tags_resp):
        reasoner = QwenVisionReasoner()
        health = await reasoner.check_health()

        assert health["ollama_reachable"] is True
        assert health["model_installed"] is False
        assert health["test_inference_status"] == "model_not_pulled"
        assert health["fallback_active"] is True
