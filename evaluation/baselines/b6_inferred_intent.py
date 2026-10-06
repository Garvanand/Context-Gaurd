"""
Baseline B6: ContextGuard (Inferred Intent).

Executes the complete six-stage reasoning pipeline, but without explicit user intent.
The system uses the VLM (Qwen2.5-VL-3B) to infer intent from artifact and context.
"""

import time
from datetime import datetime, timezone
from typing import Dict, Any

from benchmark.schema.earb_schema import EARBPair
from evaluation.schemas import EvaluationRecord
from evaluation.baselines.base import BaseEvaluator
from backend.app.models.schemas import AnalysisRequest
from backend.pipeline.pipeline import ContextGuardPipeline


class InferredIntentEvaluator(BaseEvaluator):
    """
    B6: ContextGuard (Inferred Intent Mode).
    """
    system_id = "b6"
    name = "B6: ContextGuard (Inferred Intent)"
    category_type = "baseline"

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
            selected_action=None,  # explicitly setting this to None triggers VLM intent inference
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
            baseline="b6",
            ablation=None,
            error=None,
            timestamp=datetime.now(timezone.utc).isoformat(),
        )
