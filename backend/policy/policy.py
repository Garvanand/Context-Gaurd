"""
Deterministic Pre-Action Policy Engine for ContextGuard.

Enforces:
- rho = severity * (1 + lambda * reversibility)
- Gating: STOP, ASK, WARN, ACT
- Configurable & freezeable thresholds for academic benchmarking
- Model Failure Safety Rule: NEVER silent ACT on failure, missing metrics, or invalid confidence
- Grounded explanation synthesis via explain.py
"""

from typing import List, Dict, Any, Optional
from backend.policy.thresholds import PolicyThresholds, default_thresholds
from backend.policy.explain import synthesize_explanation
from backend.app.models.schemas import EvidenceItem, AnalysisResponse


class DeterministicPolicyEngine:
    """
    Mathematical gating engine for pre-action interventions.
    """

    def __init__(self, thresholds: Optional[PolicyThresholds] = None):
        self.thresholds = thresholds or default_thresholds

    def calculate_risk(self, severity: float, reversibility: float) -> float:
        """
        Calculates composite risk:
        rho = severity * (1 + lambda * reversibility)
        """
        s = max(0.0, min(1.0, float(severity)))
        r = max(0.0, min(1.0, float(reversibility)))
        lam = self.thresholds.lambda_reversibility
        return s * (1.0 + lam * r)

    def evaluate(
        self,
        severity: float,
        reversibility: float,
        confidence: float,
        evidence: Optional[List[EvidenceItem]] = None,
        action: Optional[str] = None,
        destination: Optional[str] = None,
        recipient: Optional[str] = None,
        context_notes: Optional[str] = None,
    ) -> AnalysisResponse:
        """
        Evaluates consequences and uncertainty to produce an intervention.
        """
        ev_list = evidence or []

        # Validate numerical inputs against corruption
        try:
            s = float(severity)
            r = float(reversibility)
            c = float(confidence)
            if not (0.0 <= s <= 1.0 and 0.0 <= r <= 1.0 and 0.0 <= c <= 1.0):
                return self.safe_fallback(
                    f"Metrics out of bounds [0, 1]: s={s}, r={r}, c={c}",
                    evidence=ev_list,
                    action=action,
                    destination=destination,
                    recipient=recipient,
                )
        except (ValueError, TypeError) as e:
            return self.safe_fallback(
                f"Invalid numerical metrics: {e}",
                evidence=ev_list,
                action=action,
                destination=destination,
                recipient=recipient,
            )

        # 1. Compute composite risk rho
        rho = self.calculate_risk(s, r)

        # 2. Mathematical Decision Gating
        # Rule A: High severity override (severe/critical risk triggers STOP even with lower confidence)
        if rho >= self.thresholds.stop_threshold:
            if s >= self.thresholds.high_severity_override_threshold:
                intervention = "STOP"
            elif c < self.thresholds.min_confidence_threshold:
                # Borderline stop with low confidence -> ASK
                intervention = "ASK"
            else:
                intervention = "STOP"

        # Rule B: Moderate risk
        elif rho >= self.thresholds.ask_threshold:
            if c < self.thresholds.min_confidence_threshold:
                # Moderate risk + low confidence -> ASK
                intervention = "ASK"
            else:
                # Moderate risk + high confidence -> WARN
                intervention = "WARN"

        # Rule C: Low nominal risk
        else:
            if c < self.thresholds.min_confidence_threshold:
                # Critical safety invariant: Uncertainty is NEVER treated as safety!
                intervention = "ASK"
            else:
                intervention = "ACT"

        # 3. Deterministic Explanation Synthesis
        reason, alternative = synthesize_explanation(
            intervention=intervention,
            risk_score=rho,
            severity=s,
            reversibility=r,
            confidence=c,
            evidence=ev_list,
            action=action,
            destination=destination,
            recipient=recipient,
            fallback_applied=False,
        )

        return AnalysisResponse(
            intervention=intervention,
            risk_score=round(rho, 4),
            severity=s,
            reversibility=r,
            confidence=c,
            evidence=ev_list,
            reason=reason,
            recommended_alternative=alternative,
            can_override=True,
        )

    def safe_fallback(
        self,
        error_message: str,
        evidence: Optional[List[EvidenceItem]] = None,
        action: Optional[str] = None,
        destination: Optional[str] = None,
        recipient: Optional[str] = None,
    ) -> AnalysisResponse:
        """
        Model Failure Safety Rule:
        NEVER silently return ACT when models, metrics, or schema fail.
        Always emits ASK with safe default metrics.
        """
        ev_list = evidence or []
        fallback_ev = [
            EvidenceItem(
                type="system_safeguard",
                description=f"Model Failure Safety Rule triggered: {error_message}",
                importance=1.0,
                evidence_source="RULE",
            )
        ] + ev_list

        safe_s = 0.50
        safe_r = 0.50
        safe_c = 0.10
        rho = self.calculate_risk(safe_s, safe_r)

        reason, alt = synthesize_explanation(
            intervention="ASK",
            risk_score=rho,
            severity=safe_s,
            reversibility=safe_r,
            confidence=safe_c,
            evidence=fallback_ev,
            action=action,
            destination=destination,
            recipient=recipient,
            fallback_applied=True,
            fallback_reason=error_message,
        )

        return AnalysisResponse(
            intervention="ASK",
            risk_score=round(rho, 4),
            severity=safe_s,
            reversibility=safe_r,
            confidence=safe_c,
            evidence=fallback_ev,
            reason=reason,
            recommended_alternative=alt or "Review target context before proceeding manually.",
            can_override=True,
        )
