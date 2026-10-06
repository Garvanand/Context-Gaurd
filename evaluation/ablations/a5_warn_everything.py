"""
Ablation A5: Warn Everything (Trivial Conservative Baseline).

Trivial baseline that emits WARN unconditionally for every artifact and action.
Establishes the lower bound on precision, STOP recall (0.0), and ACT false alarm rate (1.0).
"""

import time
from datetime import datetime, timezone
from typing import Dict, Any

from benchmark.schema.earb_schema import EARBPair
from evaluation.schemas import EvaluationRecord
from evaluation.baselines.base import BaseEvaluator


class WarnEverythingAblationEvaluator(BaseEvaluator):
    """
    A5: Trivial Conservative Warn-Everything Evaluator.
    """
    system_id = "a5"
    name = "A5: Warn Everything (Conservative Baseline)"
    category_type = "ablation"

    async def evaluate_pair(self, pair: EARBPair, perception: Dict[str, Any]) -> EvaluationRecord:
        t0 = time.perf_counter()

        prediction = "WARN"
        latency = round((time.perf_counter() - t0) * 1000.0, 2)

        return EvaluationRecord(
            pair_id=pair.pair_id,
            base_artifact_id=pair.base_artifact_id,
            prediction=prediction,
            ground_truth=pair.expected_intervention.value,
            severity=0.50,
            reversibility=0.50,
            confidence=0.50,
            risk_score=0.50,
            intervention=prediction,
            latency_ms=latency,
            evidence=["Trivial conservative baseline: Emits WARN unconditionally on every artifact."],
            model=self.system_id,
            baseline=None,
            ablation="a5",
            error=None,
            timestamp=datetime.now(timezone.utc).isoformat(),
        )
