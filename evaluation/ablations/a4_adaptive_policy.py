"""
Ablation A4: Adaptive Policy (Full ContextGuard Policy).

Evaluates the ContextGuard pipeline with dynamic reversibility coupling
(lambda = 0.75), epistemic confidence gating, and high-severity override.
"""

import time
from datetime import datetime, timezone
from typing import Dict, Any

from benchmark.schema.earb_schema import EARBPair
from evaluation.schemas import EvaluationRecord
from evaluation.baselines.base import BaseEvaluator
from backend.app.models.schemas import AnalysisRequest
from backend.pipeline.pipeline import ContextGuardPipeline


class AdaptivePolicyAblationEvaluator(BaseEvaluator):
    """
    A4: Adaptive Policy Evaluator.
    """
    system_id = "a4"
    name = "A4: Adaptive Policy (Full ContextGuard Policy)"
    category_type = "ablation"

    def __init__(self):
        self.pipeline = ContextGuardPipeline()

    async def evaluate_pair(self, pair: EARBPair, perception: Dict[str, Any]) -> EvaluationRecord:
        t0 = time.perf_counter()

        req = AnalysisRequest(
            artifact=pair.artifact_path,
            artifact_type=pair.artifact_type.upper(),
            ocr_text=perception.get("ocr_text"),
            detected_faces=perception.get("detected_faces", 0),
            detected_pii=perception.get("detected_pii", []),
            url=perception.get("url"),
            selected_action=pair.intended_action,
            recipient=pair.recipient,
            destination=pair.destination,
            source_app=pair.source_app,
            network_state="WIFI",
            user_context={"scenario_context": pair.context},
        )

        resp = await self.pipeline.analyze(req)
        latency = round((time.perf_counter() - t0) * 1000.0, 2)
        evidence_descriptions = [e.description for e in resp.evidence]

        return EvaluationRecord(
            pair_id=pair.pair_id,
            base_artifact_id=pair.base_artifact_id,
            prediction=resp.intervention,
            ground_truth=pair.expected_intervention.value,
            severity=round(resp.severity, 4),
            reversibility=round(resp.reversibility, 4),
            confidence=round(resp.confidence, 4),
            risk_score=round(resp.risk_score, 4),
            intervention=resp.intervention,
            latency_ms=latency,
            evidence=evidence_descriptions,
            model=self.system_id,
            baseline=None,
            ablation="a4",
            error=None,
            timestamp=datetime.now(timezone.utc).isoformat(),
        )
