"""
Deterministic Pre-Action Policy Engine implementation.

Faithfully implements the research contract:
rho = s * (1 + lambda * r)

Thresholds:
- STOP: rho >= STOP_THRESHOLD (default 0.65)
- ASK: rho >= ASK_THRESHOLD and confidence < MIN_CONFIDENCE (default 0.35, 0.70)
- WARN: rho >= ASK_THRESHOLD (default 0.35)
- ACT: rho < ASK_THRESHOLD

Model Failure Safety Rule:
If input is malformed, missing, unparseable, or inconsistent:
NEVER silently return ACT. Return ASK or safe fallback.
"""

from typing import List, Optional

from ml.inference.types import PolicyDecision, Intervention
from ml.inference.interfaces import PolicyEngine


class DeterministicPolicyEngine(PolicyEngine):
    def __init__(
        self,
        reversibility_weight_lambda: float = 0.75,
        stop_threshold: float = 0.65,
        ask_threshold: float = 0.35,
        min_confidence_threshold: float = 0.70,
    ):
        self.reversibility_weight_lambda = reversibility_weight_lambda
        self.stop_threshold = stop_threshold
        self.ask_threshold = ask_threshold
        self.min_confidence_threshold = min_confidence_threshold

    def calculate_risk(self, severity: float, reversibility: float) -> float:
        """
        rho = s * (1 + lambda * r)
        """
        s = max(0.0, min(1.0, float(severity)))
        r = max(0.0, min(1.0, float(reversibility)))
        return s * (1.0 + self.reversibility_weight_lambda * r)

    def evaluate(
        self,
        severity: float,
        reversibility: float,
        confidence: float,
        evidence: Optional[List[str]] = None,
        context_notes: Optional[str] = None,
    ) -> PolicyDecision:
        # Check input bounds / validity
        try:
            s = float(severity)
            r = float(reversibility)
            c = float(confidence)
            if not (0.0 <= s <= 1.0 and 0.0 <= r <= 1.0 and 0.0 <= c <= 1.0):
                return self.safe_fallback(f"Out of bounds metrics: s={s}, r={r}, c={c}")
        except (ValueError, TypeError) as e:
            return self.safe_fallback(f"Invalid numeric metrics: {e}")

        rho = self.calculate_risk(s, r)
        ev_list = evidence or []

        if rho >= self.stop_threshold:
            intervention = Intervention.STOP
            rationale = (
                f"Severe or irreversible risk detected (rho={rho:.3f} >= {self.stop_threshold}). "
                f"Action blocked by default to prevent catastrophic harm."
            )
        elif rho >= self.ask_threshold and c < self.min_confidence_threshold:
            intervention = Intervention.ASK
            rationale = (
                f"Elevated risk with high uncertainty (rho={rho:.3f}, confidence={c:.2f} < {self.min_confidence_threshold}). "
                f"User confirmation and clarification required."
            )
        elif rho >= self.ask_threshold:
            intervention = Intervention.WARN
            rationale = (
                f"Moderate or reversible risk detected (rho={rho:.3f} >= {self.ask_threshold}). "
                f"Proceed with caution under explicit user acknowledgement."
            )
        else:
            intervention = Intervention.ACT
            rationale = (
                f"Negligible risk bounded within safe parameters (rho={rho:.3f} < {self.ask_threshold}). "
                f"Safe to proceed."
            )

        return PolicyDecision(
            intervention=intervention,
            risk_score=round(rho, 4),
            severity=s,
            reversibility=r,
            confidence=c,
            reversibility_weight_lambda=self.reversibility_weight_lambda,
            stop_threshold=self.stop_threshold,
            ask_threshold=self.ask_threshold,
            min_confidence_threshold=self.min_confidence_threshold,
            evidence=ev_list,
            rationale=rationale,
            fallback_applied=False,
        )

    def safe_fallback(self, error_message: str) -> PolicyDecision:
        """
        Model Failure Safety Rule:
        NEVER silently return ACT. Returns ASK with safe default metrics.
        """
        return PolicyDecision(
            intervention=Intervention.ASK,
            risk_score=0.50,
            severity=0.50,
            reversibility=0.50,
            confidence=0.10,
            reversibility_weight_lambda=self.reversibility_weight_lambda,
            stop_threshold=self.stop_threshold,
            ask_threshold=self.ask_threshold,
            min_confidence_threshold=self.min_confidence_threshold,
            evidence=[f"System safeguard: {error_message}"],
            rationale=f"Model Failure Safety Rule triggered: {error_message}. Fallback to ASK.",
            fallback_applied=True,
        )
