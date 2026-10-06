"""
Ablation A1: No Intent.

Evaluates the ContextGuard pipeline without user-selected action,
stripping recipient, destination, and explicit intent signals.
"""

import time
from datetime import datetime, timezone
from typing import Dict, Any

from benchmark.schema.earb_schema import EARBPair
from evaluation.schemas import EvaluationRecord
from evaluation.baselines.base import BaseEvaluator
from backend.app.models.schemas import AnalysisRequest
from backend.pipeline.pipeline import ContextGuardPipeline


class NoIntentAblationEvaluator(BaseEvaluator):
    """
    A1: Intent-Ablated Evaluator.
    """
    system_id = "a1"
    name = "A1: No Intent (Intent-Ablated)"
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
            selected_action=None,  # Stripped intent
            recipient=None,        # Stripped recipient
            destination=None,      # Stripped destination
            source_app=pair.source_app,
            network_state="WIFI",
            user_context={"ambient_context": pair.context},
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
            ablation="a1",
            error=None,
            timestamp=datetime.now(timezone.utc).isoformat(),
        )
