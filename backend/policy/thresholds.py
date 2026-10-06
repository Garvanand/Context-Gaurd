"""
Threshold configurations for ContextGuard Deterministic Policy Engine.

Thresholds are configurable during development and calibration,
then frozen for evaluation and academic benchmarking.
"""

from dataclasses import dataclass
from typing import Optional


@dataclass
class PolicyThresholds:
    """
    Mathematical gating thresholds for pre-action intervention.
    rho = severity * (1 + lambda * reversibility)
    """
    lambda_reversibility: float = 0.75
    stop_threshold: float = 0.65
    ask_threshold: float = 0.35
    min_confidence_threshold: float = 0.70
    high_severity_override_threshold: float = 0.80
    _frozen: bool = False

    def freeze(self) -> None:
        """Freeze thresholds to guarantee reproducible evaluation."""
        self._frozen = True

    def unfreeze(self) -> None:
        """Unfreeze thresholds for development or calibration."""
        self._frozen = False

    @property
    def is_frozen(self) -> bool:
        return self._frozen

    def update(
        self,
        lambda_reversibility: Optional[float] = None,
        stop_threshold: Optional[float] = None,
        ask_threshold: Optional[float] = None,
        min_confidence_threshold: Optional[float] = None,
        high_severity_override_threshold: Optional[float] = None,
    ) -> None:
        if self._frozen:
            raise RuntimeError("Policy thresholds are frozen for evaluation and cannot be modified.")

        if lambda_reversibility is not None:
            if lambda_reversibility < 0.0:
                raise ValueError("lambda_reversibility must be non-negative.")
            self.lambda_reversibility = float(lambda_reversibility)

        if stop_threshold is not None:
            if not (0.0 <= stop_threshold <= 2.0):
                raise ValueError("stop_threshold must be between 0.0 and 2.0.")
            self.stop_threshold = float(stop_threshold)

        if ask_threshold is not None:
            if not (0.0 <= ask_threshold <= 1.0):
                raise ValueError("ask_threshold must be between 0.0 and 1.0.")
            if ask_threshold >= self.stop_threshold:
                raise ValueError(f"ask_threshold ({ask_threshold}) must be strictly less than stop_threshold ({self.stop_threshold}).")
            self.ask_threshold = float(ask_threshold)

        if min_confidence_threshold is not None:
            if not (0.0 <= min_confidence_threshold <= 1.0):
                raise ValueError("min_confidence_threshold must be between 0.0 and 1.0.")
            self.min_confidence_threshold = float(min_confidence_threshold)

        if high_severity_override_threshold is not None:
            if not (0.0 <= high_severity_override_threshold <= 1.0):
                raise ValueError("high_severity_override_threshold must be between 0.0 and 1.0.")
            self.high_severity_override_threshold = float(high_severity_override_threshold)


# Default shared singleton instance
default_thresholds = PolicyThresholds()
