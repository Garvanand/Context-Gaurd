"""
Unit tests for Deterministic Policy Engine and safety invariants.
"""

import pytest
from ml.inference.policy_engine import DeterministicPolicyEngine
from ml.inference.types import Intervention


@pytest.fixture
def policy():
    return DeterministicPolicyEngine(
        reversibility_weight_lambda=0.75,
        stop_threshold=0.65,
        ask_threshold=0.35,
        min_confidence_threshold=0.70,
    )


def test_central_demo_action_1_save_privately(policy):
    """
    Scenario 1: Save bank statement to private encrypted storage.
    Severity: 0.05, Irreversibility: 0.0, Confidence: 0.95
    Expected: ACT (Green)
    """
    decision = policy.evaluate(
        severity=0.05,
        reversibility=0.0,
        confidence=0.95,
        evidence=["Personal encrypted drive", "Self-owned container"],
    )
    assert decision.intervention == Intervention.ACT
    assert decision.risk_score == pytest.approx(0.05, rel=1e-2)
    assert not decision.fallback_applied


def test_central_demo_action_2_send_unknown_recipient_elevated_risk(policy):
    """
    Scenario 2: Send bank statement to unverified recipient with uncertainty.
    Severity: 0.40, Irreversibility: 0.50, Confidence: 0.55 (< 0.70)
    rho = 0.40 * (1 + 0.75 * 0.50) = 0.40 * 1.375 = 0.55
    Expected: ASK (Yellow) because rho >= 0.35 and confidence < 0.70
    """
    decision = policy.evaluate(
        severity=0.40,
        reversibility=0.50,
        confidence=0.55,
        evidence=["Unverified recipient", "Moderate exposure"],
    )
    assert decision.intervention == Intervention.ASK
    assert decision.risk_score == pytest.approx(0.55, rel=1e-2)
    assert not decision.fallback_applied


def test_central_demo_action_2_send_unknown_recipient_known_risk(policy):
    """
    Scenario 2 variant: Send to unknown recipient with high confidence of moderate hazard.
    Severity: 0.40, Irreversibility: 0.50, Confidence: 0.85 (>= 0.70)
    Expected: WARN (Orange)
    """
    decision = policy.evaluate(
        severity=0.40,
        reversibility=0.50,
        confidence=0.85,
        evidence=["Unverified recipient phone", "Direct chat channel"],
    )
    assert decision.intervention == Intervention.WARN
    assert decision.risk_score == pytest.approx(0.55, rel=1e-2)


def test_central_demo_action_3_post_publicly(policy):
    """
    Scenario 3: Post bank statement publicly to social media.
    Severity: 0.90, Irreversibility: 1.0, Confidence: 0.95
    rho = 0.90 * (1 + 0.75 * 1.0) = 0.90 * 1.75 = 1.575
    Expected: STOP (Red)
    """
    decision = policy.evaluate(
        severity=0.90,
        reversibility=1.0,
        confidence=0.95,
        evidence=["Public broadcast feed", "Irreversible scraping"],
    )
    assert decision.intervention == Intervention.STOP
    assert decision.risk_score >= 0.65
    assert not decision.fallback_applied


def test_model_failure_safety_rule_on_invalid_numeric_values(policy):
    """
    CRITICAL SAFETY INVARIANT:
    If AI returns invalid, negative, or NaN-like metrics, system must NEVER silently return ACT.
    Must return ASK or safe fallback.
    """
    # Out of bounds severity (> 1.0)
    decision1 = policy.evaluate(severity=2.5, reversibility=0.5, confidence=0.9)
    assert decision1.intervention == Intervention.ASK
    assert decision1.fallback_applied is True

    # Negative reversibility
    decision2 = policy.evaluate(severity=0.1, reversibility=-0.5, confidence=0.9)
    assert decision2.intervention == Intervention.ASK
    assert decision2.fallback_applied is True


def test_model_failure_safety_rule_direct_fallback(policy):
    fallback_decision = policy.safe_fallback("VLM response unparseable JSON")
    assert fallback_decision.intervention == Intervention.ASK
    assert fallback_decision.fallback_applied is True
    assert "Model Failure Safety Rule triggered" in fallback_decision.rationale
