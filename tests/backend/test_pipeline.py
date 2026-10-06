"""
Comprehensive tests for ContextGuard Six-Stage Reasoning Pipeline.

Specifically verifies:
1. Central Viva Scenario: Same artifact + different action produces ACT, WARN/ASK, STOP.
2. Inferred intent vs. explicit selected intent precedence.
3. All evidence items contain valid evidence_source annotations.
4. Model Failure Safety Rule (never silent ACT on model/pipeline failure).
5. FastAPI REST endpoints POST /analyze and POST /api/v1/analyze.
"""

import pytest
from fastapi.testclient import TestClient
from backend.app.main import app
from backend.pipeline.pipeline import ContextGuardPipeline
from backend.app.models.schemas import (
    AnalysisRequest,
    AnalysisResponse,
    EvidenceSource,
)

client = TestClient(app)

# Canonical synthetic bank statement OCR payload
BANK_STATEMENT_OCR = (
    "NATIONAL RESERVE BANK - ACCOUNT STATEMENT\n"
    "Account Holder: John Doe\n"
    "Account Number: 4092-XXXX-8921\n"
    "Opening Balance: $14,250.00\n"
    "Oct 01 Salary Credit +$4,500.00 $18,750.00\n"
    "Oct 03 Mortgage Payment -$1,800.00 $16,950.00"
)


# =====================================================================
# 1. Central Viva Demonstration: Same Artifact + Different Actions
# =====================================================================

@pytest.mark.asyncio
async def test_central_demo_action_1_save_privately():
    """
    Scenario 1: Save sensitive bank statement to private encrypted vault.
    Condition: Same artifact + SAVE action + Personal Vault destination.
    Expected Result: ACT (Green)
    """
    pipeline = ContextGuardPipeline()
    req = AnalysisRequest(
        artifact_type="IMAGE",
        ocr_text=BANK_STATEMENT_OCR,
        selected_action="SAVE",
        destination="Personal Encrypted Local Vault",
        recipient="Self",
        source_app="BankingApp",
    )
    res = await pipeline.analyze(req)

    assert isinstance(res, AnalysisResponse)
    assert res.intervention == "ACT"
    assert res.severity <= 0.15
    assert res.reversibility == 0.0
    assert res.risk_score < 0.35
    assert "safe" in res.reason.lower()


@pytest.mark.asyncio
async def test_central_demo_action_2_send_unknown_recipient():
    """
    Scenario 2: Send identical bank statement to unverified contact via direct message.
    Condition: Same artifact + SEND action + Unverified Recipient.
    Expected Result: ASK (Yellow) or WARN (Orange)
    """
    pipeline = ContextGuardPipeline()
    req = AnalysisRequest(
        artifact_type="IMAGE",
        ocr_text=BANK_STATEMENT_OCR,
        selected_action="SEND",
        destination="Telegram Chat Direct Message",
        recipient="Unknown Unverified User",
        source_app="BankingApp",
    )
    res = await pipeline.analyze(req)

    assert isinstance(res, AnalysisResponse)
    assert res.intervention in ("ASK", "WARN")
    assert res.intervention != "ACT"
    assert res.intervention != "STOP"
    assert res.severity >= 0.40
    assert res.reversibility > 0.0


@pytest.mark.asyncio
async def test_central_demo_action_3_post_publicly():
    """
    Scenario 3: Post identical bank statement to public social media feed.
    Condition: Same artifact + POST action + Public Twitter destination.
    Expected Result: STOP (Red)
    """
    pipeline = ContextGuardPipeline()
    req = AnalysisRequest(
        artifact_type="IMAGE",
        ocr_text=BANK_STATEMENT_OCR,
        selected_action="POST",
        destination="Public Twitter/X Feed",
        recipient="Public Broadcast Followers",
        source_app="BankingApp",
    )
    res = await pipeline.analyze(req)

    assert isinstance(res, AnalysisResponse)
    assert res.intervention == "STOP"
    assert res.severity >= 0.85
    assert res.reversibility == 1.0
    assert res.risk_score >= 0.65
    assert res.recommended_alternative is not None
    assert "public" in res.reason.lower() or "irreversible" in res.reason.lower()


# =====================================================================
# 2. Intent Precedence Tests
# =====================================================================

@pytest.mark.asyncio
async def test_explicit_intent_never_overridden_by_inferred_intent():
    """
    If the user explicitly selected SAVE, inferred intent (which might infer POST
    due to a public keyword) must NOT silently override it.
    """
    pipeline = ContextGuardPipeline()
    req = AnalysisRequest(
        ocr_text=BANK_STATEMENT_OCR,
        selected_action="SAVE",
        destination="Personal Local Drive with twitter backup notes",
        recipient="Self",
    )
    res = await pipeline.analyze(req)

    assert res.stages is not None
    stage_2 = res.stages["stage_2_intent"]
    assert stage_2["selected_action"] == "SAVE"
    assert stage_2["resolved_action"] == "SAVE"
    assert stage_2["intent_source"] == "EXPLICIT_USER"


@pytest.mark.asyncio
async def test_inferred_intent_used_when_selected_action_is_omitted():
    """
    When selected_action is omitted (research evaluation mode),
    pipeline infers action and flags it as INFERRED_RESEARCH.
    """
    pipeline = ContextGuardPipeline()
    req = AnalysisRequest(
        ocr_text="Random generic meeting notes",
        selected_action=None,
        destination="Public Community Forum",
        recipient="Public",
    )
    res = await pipeline.analyze(req)

    stage_2 = res.stages["stage_2_intent"]
    assert stage_2["selected_action"] is None
    assert stage_2["resolved_action"] == "POST"
    assert stage_2["intent_source"] == "INFERRED_RESEARCH"


# =====================================================================
# 3. Evidence Sources & Types Verification
# =====================================================================

@pytest.mark.asyncio
async def test_evidence_items_have_valid_sources():
    """
    Every evidence item extracted across stages must declare a valid evidence_source.
    """
    pipeline = ContextGuardPipeline()
    req = AnalysisRequest(
        ocr_text="Your OTP is 849201. Never share this with anyone.",
        detected_faces=2,
        detected_pii=["SSN: ***-**-1234"],
        selected_action="SEND",
        destination="Public Forum",
        recipient="Unknown Caller",
    )
    res = await pipeline.analyze(req)

    valid_sources = {s.value for s in EvidenceSource}
    assert len(res.evidence) > 0
    for ev in res.evidence:
        assert ev.evidence_source in valid_sources
        assert 0.0 <= ev.importance <= 1.0


# =====================================================================
# 4. Critical Safety Rule Tests
# =====================================================================

@pytest.mark.asyncio
async def test_model_failure_safety_rule_never_silent_act():
    """
    If any model, parser, or metric fails, the pipeline MUST emit ASK (never ACT).
    """
    pipeline = ContextGuardPipeline()
    # Malformed metric injection by modifying consequence stage
    broken_req = AnalysisRequest(
        ocr_text="Harmless test",
        selected_action="SAVE",
        destination="Personal Vault",
        recipient="Self",
    )

    # Force a failure in the pipeline
    with pytest.MonkeyPatch.context() as mp:
        mp.setattr(pipeline, "stage_3_evidence", None)  # triggers exception
        res = await pipeline.analyze(broken_req)

        assert res.intervention == "ASK"
        assert res.intervention != "ACT"
        assert "Safeguard" in res.reason or "anomaly" in res.reason.lower()


# =====================================================================
# 5. Phishing and Credential Specific Tests
# =====================================================================

@pytest.mark.asyncio
async def test_phishing_url_detection_blocks_action():
    """
    Phishing URL detected via XGBoost model produces STOP.
    """
    pipeline = ContextGuardPipeline()
    req = AnalysisRequest(
        url="http://secure-online-login.xyz/verify?id=99",
        ocr_text="SECURITY ALERT: ACCOUNT SUSPENDED",
        selected_action="LOGIN",
        destination="Phishing Landing Page",
        recipient="Untrusted External Server",
    )
    res = await pipeline.analyze(req)

    assert res.intervention == "STOP"
    assert res.severity >= 0.90
    assert any("phishing" in e.type.lower() or "suspicious" in e.type.lower() for e in res.evidence)


# =====================================================================
# 6. REST API Endpoints Integration Tests
# =====================================================================

def test_api_analyze_endpoint_post():
    payload = {
        "artifact_type": "IMAGE",
        "ocr_text": BANK_STATEMENT_OCR,
        "selected_action": "SAVE",
        "destination": "Personal Encrypted Vault",
        "recipient": "Self",
        "source_app": "BankingApp",
    }
    response = client.post("/analyze", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["intervention"] == "ACT"
    assert "risk_score" in data
    assert "severity" in data
    assert "reversibility" in data
    assert "confidence" in data
    assert "evidence" in data
    assert "reason" in data
    assert data["can_override"] is True


def test_api_v1_analyze_endpoint_post():
    payload = {
        "artifact_type": "IMAGE",
        "ocr_text": BANK_STATEMENT_OCR,
        "selected_action": "POST",
        "destination": "Public Twitter Feed",
        "recipient": "Public Followers",
        "source_app": "BankingApp",
    }
    response = client.post("/api/v1/analyze", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["intervention"] == "STOP"
    assert data["risk_score"] >= 0.65
    assert len(data["evidence"]) > 0
    assert data["recommended_alternative"] is not None
