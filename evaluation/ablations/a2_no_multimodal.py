"""
Ablation A2: No Multimodality (Text & Metadata Only).

Evaluates the ContextGuard pipeline when visual image features,
image payloads, and facial detection contours are stripped.
Demonstrates blindness to visual-only hazards (e.g. biometric facial privacy).
"""

import time
from datetime import datetime, timezone
from typing import Dict, Any

from benchmark.schema.earb_schema import EARBPair
from evaluation.schemas import EvaluationRecord
from evaluation.baselines.base import BaseEvaluator
from backend.app.models.schemas import AnalysisRequest
from backend.pipeline.pipeline import ContextGuardPipeline


class NoMultimodalityAblationEvaluator(BaseEvaluator):
    """
    A2: Vision-Ablated Evaluator (Text/Metadata only).
    """
    system_id = "a2"
    name = "A2: No Multimodality (Text & Metadata Only)"
    category_type = "ablation"

    def __init__(self):
        self.pipeline = ContextGuardPipeline()

    async def evaluate_pair(self, pair: EARBPair, perception: Dict[str, Any]) -> EvaluationRecord:
        t0 = time.perf_counter()

        # Text-only request with no image payload and 0 facial contours
        req = AnalysisRequest(
            artifact=None,  # Stripped image payload
            artifact_type="TEXT",
            ocr_text=perception.get("ocr_text"),
            detected_faces=0,  # Stripped visual facial contours
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
            ablation="a2",
            error=None,
            timestamp=datetime.now(timezone.utc).isoformat(),
        )
