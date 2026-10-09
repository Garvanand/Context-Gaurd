"""
Unit and Integration Tests for ContextGuard Action-Aware Decision & Uncertainty Engine.

Verifies:
1. Action-conditioned triad on same synthetic artifact:
   - Private save -> ACT
   - Send to unknown recipient -> ASK or WARN
   - Public posting -> STOP
2. Missing context (unknown recipient remains unknown, no hallucination).
3. Contradictory models (benign URL model vs detected OTP/credentials).
4. Malformed output & invalid metrics (Model Failure Safety Rule -> ASK).
5. Unavailable models (graceful fallback without crashing or silent ACT).
6. Low-confidence evidence (triggers conformal selective prediction abstention -> ASK).
7. Duplicate evidence (bounded accumulation; no artificial certainty inflation).
8. Strict output schema: evidence provenance, policy version, model versions, uncertainty, intervention.
"""

import pytest
from unittest.mock import MagicMock
from backend.policy.cascade_engine import (
    CalibratedCascadeEngine,
    CASCADE_POLICY_VERSION,
    URL_MODEL_VERSION,
)
from backend.app.models.schemas import AnalysisRequest, AnalysisResponse
from backend.app.models.evidence import (
    CanonicalEvidenceRecord,
    EvidenceProvenanceSource,
    EpistemicEvidenceType,
)
from backend.app.models.action_state import (
    ActionConditionedState,
    ContextAction,
    DestinationScope,
)
from backend.policy.conformal import SplitConformalCalibrator


@pytest.fixture
def cascade_engine():
    engine = CalibratedCascadeEngine(auto_calibrate=True)
    return engine


# -----------------------------------------------------------------------------
# 1. Action-Conditioned Triad on Same Synthetic Artifact
# -----------------------------------------------------------------------------
def test_synthetic_bank_statement_triad(cascade_engine):
    """
    Evaluates the same synthetic bank statement artifact against three actions:
    1. Private save -> ACT
    2. Send to unknown recipient -> WARN or ASK
    3. Public posting -> STOP
    """
    bank_ocr = "State Bank of India. Account Balance: INR 4,82,900. Transaction Ref: 8942019."

    # A: Private Save
    req_save = AnalysisRequest(
        ocr_text=bank_ocr,
        selected_action="SAVE",
        destination="Personal Encrypted Vault",
        recipient="Self",
    )
    res_save = cascade_engine.execute(req_save)
    assert res_save.intervention == "ACT"
    assert res_save.severity <= 0.15
    assert res_save.policy_version == CASCADE_POLICY_VERSION
    assert res_save.stages is not None

    # B: Send to Unknown Recipient
    req_send_unknown = AnalysisRequest(
        ocr_text=bank_ocr,
        selected_action="SEND",
        destination="External Chat",
        recipient="Unknown Telegram User",
    )
    res_send_unknown = cascade_engine.execute(req_send_unknown)
    assert res_send_unknown.intervention in ("WARN", "ASK")
    assert res_send_unknown.severity >= 0.35
    assert res_send_unknown.action_state["is_unknown_recipient"] is True

    # C: Public Posting
    req_post = AnalysisRequest(
        ocr_text=bank_ocr,
        selected_action="POST",
        destination="Public Social Feed X.com",
        recipient="Public Broadcast",
    )
    res_post = cascade_engine.execute(req_post)
    assert res_post.intervention == "STOP"
    assert res_post.severity >= 0.85
    assert res_post.reversibility >= 0.90


# -----------------------------------------------------------------------------
# 2. Missing Context & Non-Hallucination Invariant
# -----------------------------------------------------------------------------
def test_missing_context_does_not_hallucinate(cascade_engine):
    """
    Ensures that empty OCR and omitted recipients remain explicitly unknown.
    The engine must not invent recipients, amounts, or destinations.
    """
    req_empty = AnalysisRequest(
        ocr_text=None,
        selected_action="SEND",
        recipient=None,
        destination=None,
    )
    res = cascade_engine.execute(req_empty)

    # Missing context + unknown recipient cannot be affirmative evidence of safety
    assert res.intervention in ("ASK", "WARN")
    assert res.intervention != "ACT"  # Never silent ACT on unknown context
    assert res.action_state["intended_recipient"] == "UNKNOWN_RECIPIENT"
    assert res.action_state["is_unknown_recipient"] is True

    # Check that missing evidence is tracked in canonical evidence records
    canon_ev = res.canonical_evidence
    has_missing_tag = any(
        e.get("epistemic_type") == EpistemicEvidenceType.MISSING_EVIDENCE.value
        for e in canon_ev
    )
    assert has_missing_tag is True


# -----------------------------------------------------------------------------
# 3. Contradictory Models
# -----------------------------------------------------------------------------
def test_contradictory_models_benign_url_with_critical_otp(cascade_engine):
    """
    If URL model predicts benign domain, but OCR extracts unredacted OTP,
    the policy must not grant silent ACT. Critical credential hazard takes precedence.
    """
    # Mock URL service returning benign probability
    mock_url_service = MagicMock()
    mock_url_service.predict.return_value = {
        "is_phishing": False,
        "probability": 0.05,
        "features": {"hostname": "legitimate-bank.com"},
    }
    engine = CalibratedCascadeEngine(url_service=mock_url_service, auto_calibrate=True)

    req = AnalysisRequest(
        url="https://legitimate-bank.com/login",
        ocr_text="Your One-Time Password (OTP) verification code is 849201. Do not share.",
        selected_action="SEND",
        recipient="Unknown Caller",
        destination="SMS Relay",
    )
    res = engine.execute(req)

    # Sending OTP to an unverified caller is high-consequence
    assert res.intervention in ("STOP", "ASK")
    assert res.intervention != "ACT"
    # OTP evidence must be present in canonical evidence records
    categories = [e["category"] for e in res.canonical_evidence]
    assert "otp" in categories


# -----------------------------------------------------------------------------
# 4. Malformed Input & Invalid Metrics (Model Failure Safety Rule)
# -----------------------------------------------------------------------------
def test_malformed_input_triggers_safe_fallback(cascade_engine):
    """
    Tests that corrupt or out-of-bounds parameters never cause silent ACT.
    """
    req_corrupt = AnalysisRequest(
        ocr_text="Normal text",
        selected_action="SEND",
        user_context={"corrupt_flag": True},
    )

    # Simulate an internal crash in stage_c
    original_stage_c = cascade_engine.stage_c_action_aware_fusion
    cascade_engine.stage_c_action_aware_fusion = MagicMock(side_effect=RuntimeError("Corrupt tensor format"))

    res = cascade_engine.execute(req_corrupt)
    assert res.intervention == "ASK"
    assert res.confidence <= 0.20
    assert "Model Failure Safety Rule triggered" in res.reason

    # Restore
    cascade_engine.stage_c_action_aware_fusion = original_stage_c


# -----------------------------------------------------------------------------
# 5. Unavailable Model Sensor Fallback
# -----------------------------------------------------------------------------
def test_unavailable_url_model_fallback(cascade_engine):
    """
    If the URL classifier throws an exception, the system must log missing evidence
    and abstain to ASK rather than crashing or returning ACT.
    """
    mock_url_service = MagicMock()
    mock_url_service.predict.side_effect = ConnectionError("Model endpoint unreachable")
    engine = CalibratedCascadeEngine(url_service=mock_url_service, auto_calibrate=True)

    req = AnalysisRequest(
        url="https://suspicious-unknown-link.com/auth",
        selected_action="OPEN_URL",
        destination="Web Browser",
    )
    res = engine.execute(req)

    # Uncertainty should elevate, yielding ASK or WARN, never silent ACT
    assert res.intervention in ("ASK", "WARN")
    assert res.intervention != "ACT"
    err_evidence = [e for e in res.canonical_evidence if e["category"] == "url_risk"]
    assert len(err_evidence) > 0
    assert err_evidence[0]["epistemic_type"] == EpistemicEvidenceType.MISSING_EVIDENCE.value


# -----------------------------------------------------------------------------
# 6. Low-Confidence Evidence Escalates to ASK (Selective Prediction)
# -----------------------------------------------------------------------------
def test_low_confidence_escalates_to_ask(cascade_engine):
    """
    When epistemic uncertainty is elevated (confidence < 0.70), selective prediction
    abstains to ASK rather than guessing.
    """
    req_ambiguous = AnalysisRequest(
        ocr_text="Verify your details with our refund desk.",
        selected_action="ASK_VERIFY",
        recipient="Unverified Support Agent",
        destination="Chat Room",
    )
    res = cascade_engine.execute(req_ambiguous)

    assert res.confidence < 0.70
    assert res.intervention == "ASK"
    assert res.abstention_applied is True


# -----------------------------------------------------------------------------
# 7. Duplicate Evidence Handled Boundedly
# -----------------------------------------------------------------------------
def test_duplicate_evidence_handling(cascade_engine):
    """
    Tests that repeating text tokens does not artificially inflate model confidence to 1.0.
    Real-world certainty remains bounded.
    """
    repeated_ocr = "Account balance: INR 5000. Account balance: INR 5000. Account balance: INR 5000."
    req = AnalysisRequest(
        ocr_text=repeated_ocr,
        selected_action="SAVE",
        destination="Personal Vault",
    )
    res = cascade_engine.execute(req)
    assert res.confidence <= 0.99
    assert res.intervention == "ACT"


# -----------------------------------------------------------------------------
# 8. Strict Canonical Provenance and Versioning Contract
# -----------------------------------------------------------------------------
def test_complete_provenance_and_versioning_contract(cascade_engine):
    """
    Every output must contain:
    - evidence provenance
    - policy version
    - model versions
    - confidence and uncertainty
    - intervention
    """
    req = AnalysisRequest(
        url="https://paypal-security-update.com/login",
        ocr_text="Immediate action required: Enter your password to unlock account.",
        selected_action="LOGIN",
        destination="External Phishing Portal",
    )
    res = cascade_engine.execute(req)

    assert isinstance(res, AnalysisResponse)
    assert res.policy_version == CASCADE_POLICY_VERSION
    assert URL_MODEL_VERSION in res.model_path
    assert res.intervention == "STOP"
    assert len(res.canonical_evidence) > 0

    first_ev = res.canonical_evidence[0]
    assert "evidence_id" in first_ev
    assert "provenance_source" in first_ev
    assert "extractor_version" in first_ev
    assert "epistemic_type" in first_ev
    assert "summary" in first_ev
    assert first_ev["provenance_source"] in [s.value for s in EvidenceProvenanceSource]

    # Verify that raw secrets are never persisted unredacted
    assert "password" not in first_ev.get("summary", "").lower() or "[REDACTED" in first_ev.get("summary", "")
