"""
Unit tests for ContextGuard Policy Engine, Thresholds, and Explanation Generation.
"""

import pytest
from backend.policy.thresholds import PolicyThresholds
from backend.policy.explain import synthesize_explanation
from backend.policy.policy import DeterministicPolicyEngine
from backend.app.models.schemas import EvidenceItem, EvidenceSource


def test_thresholds_configuration_and_freezing():
    thresholds = PolicyThresholds(
        lambda_reversibility=0.75,
        stop_threshold=0.65,
        ask_threshold=0.35,
        min_confidence_threshold=0.70,
    )
    assert not thresholds.is_frozen

    # Can update when unfrozen
    thresholds.update(stop_threshold=0.70)
    assert thresholds.stop_threshold == 0.70

    # Freeze thresholds
    thresholds.freeze()
    assert thresholds.is_frozen

    # Updating while frozen must raise RuntimeError
    with pytest.raises(RuntimeError, match="frozen for evaluation"):
        thresholds.update(stop_threshold=0.60)

    # Unfreeze and update succeeds
    thresholds.unfreeze()
    thresholds.update(stop_threshold=0.65)
    assert thresholds.stop_threshold == 0.65


def test_thresholds_invalid_values():
    thresholds = PolicyThresholds()
    with pytest.raises(ValueError, match="strictly less than stop_threshold"):
        thresholds.update(ask_threshold=0.80, stop_threshold=0.65)

    with pytest.raises(ValueError, match="non-negative"):
        thresholds.update(lambda_reversibility=-0.1)


def test_calculate_risk_formula():
    engine = DeterministicPolicyEngine(PolicyThresholds(lambda_reversibility=0.75))
    # rho = s * (1 + lambda * r)
    # s = 0.50, r = 0.80 -> 0.50 * (1 + 0.75 * 0.80) = 0.50 * 1.6 = 0.80
    rho = engine.calculate_risk(0.50, 0.80)
    assert pytest.approx(rho, 1e-4) == 0.80

    # s = 0.05, r = 0.0 -> 0.05 * (1 + 0.0) = 0.05
    assert pytest.approx(engine.calculate_risk(0.05, 0.0), 1e-4) == 0.05

    # s = 0.90, r = 1.0 -> 0.90 * (1 + 0.75 * 1.0) = 0.90 * 1.75 = 1.575
    assert pytest.approx(engine.calculate_risk(0.90, 1.0), 1e-4) == 1.575


def test_policy_high_severity_override():
    """
    If severe/critical risk (s >= 0.80), STOP triggers even with lower confidence.
    """
    engine = DeterministicPolicyEngine(
        PolicyThresholds(
            lambda_reversibility=0.75,
            stop_threshold=0.65,
            high_severity_override_threshold=0.80,
            min_confidence_threshold=0.70,
        )
    )
    # High severity s=0.90, reversibility r=1.0, lower confidence c=0.55
    res = engine.evaluate(severity=0.90, reversibility=1.0, confidence=0.55)
    assert res.intervention == "STOP"
    assert res.risk_score >= 0.65


def test_policy_moderate_risk_confidence_gating():
    """
    If moderate risk:
    - Low confidence -> ASK
    - High confidence -> WARN
    """
    engine = DeterministicPolicyEngine()
    # s=0.40, r=0.50 -> rho = 0.40 * 1.375 = 0.55 (between ask=0.35 and stop=0.65)
    # 1. Low confidence (< 0.70)
    res_ask = engine.evaluate(severity=0.40, reversibility=0.50, confidence=0.50)
    assert res_ask.intervention == "ASK"

    # 2. High confidence (>= 0.70)
    res_warn = engine.evaluate(severity=0.40, reversibility=0.50, confidence=0.85)
    assert res_warn.intervention == "WARN"


def test_policy_low_risk_uncertainty_is_not_safety():
    """
    CRITICAL PRINCIPLE: Uncertainty is NEVER treated as safety.
    If nominal risk is low (rho < 0.35) but confidence is low (< 0.70), MUST return ASK!
    """
    engine = DeterministicPolicyEngine()
    # Nominal low risk s=0.10, r=0.10 -> rho = 0.1075 (< 0.35)
    # But confidence is low (c=0.40)
    res = engine.evaluate(severity=0.10, reversibility=0.10, confidence=0.40)
    assert res.intervention == "ASK"

    # With high confidence (c=0.95), it returns ACT
    res_act = engine.evaluate(severity=0.10, reversibility=0.10, confidence=0.95)
    assert res_act.intervention == "ACT"


def test_model_failure_safety_rule():
    """
    Model Failure Safety Rule:
    Never produce ACT on invalid metrics or errors. Always return ASK.
    """
    engine = DeterministicPolicyEngine()

    # Out of bounds severity
    res_oob = engine.evaluate(severity=2.5, reversibility=0.5, confidence=0.9)
    assert res_oob.intervention == "ASK"
    assert "Safeguard" in res_oob.reason or "Model Failure" in res_oob.reason

    # Negative confidence
    res_neg = engine.evaluate(severity=0.1, reversibility=0.1, confidence=-0.5)
    assert res_neg.intervention == "ASK"

    # Direct fallback call
    fallback = engine.safe_fallback("VLM connection timed out")
    assert fallback.intervention == "ASK"
    assert fallback.intervention != "ACT"
    assert fallback.can_override is True


def test_deterministic_explanation_synthesis():
    evidence = [
        EvidenceItem(
            type="financial_information",
            description="Routing number and account balance visible",
            importance=0.90,
            evidence_source=EvidenceSource.OCR,
        ),
        EvidenceItem(
            type="public_destination",
            description="Public Twitter/X Feed",
            importance=0.95,
            evidence_source=EvidenceSource.CONTEXT,
        ),
    ]

    reason, alternative = synthesize_explanation(
        intervention="STOP",
        risk_score=1.575,
        severity=0.90,
        reversibility=1.00,
        confidence=0.95,
        evidence=evidence,
        action="POST",
        destination="Public Twitter/X Feed",
        recipient="Public Followers",
    )

    assert "Publicly broadcasting" in reason or "Irreversible" in reason
    assert alternative is not None
    assert "encrypted vault" in alternative.lower() or "redact" in alternative.lower()
