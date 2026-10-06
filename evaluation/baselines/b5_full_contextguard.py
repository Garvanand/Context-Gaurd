"""
Baseline B5: Full Proposed ContextGuard System.

Executes the complete six-stage reasoning pipeline:
1. Context Aggregation
2. Intent Resolution (Explicit user precedence)
3. Multi-source Action-relevant Evidence Extraction (ML Kit + XGBoost URL + Rules + VLM)
4. Independent Consequence Estimation (Severity s and Reversibility r)
5. Epistemic Uncertainty Estimation (Confidence c)
6. Deterministic Policy Intervention: rho = s * (1 + lambda * r)
"""

import time
from datetime import datetime, timezone
from typing import Dict, Any, List

from benchmark.schema.earb_schema import EARBPair
from evaluation.schemas import EvaluationRecord
from evaluation.baselines.base import BaseEvaluator
from backend.app.models.schemas import AnalysisRequest
from backend.pipeline.pipeline import ContextGuardPipeline


class FullContextGuardEvaluator(BaseEvaluator):
    """
    B5: Full Proposed ContextGuard System.
    """
    system_id = "b5"
    name = "B5: ContextGuard (Full Proposed System)"
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
            baseline="b5",
            ablation=None,
            error=None,
            timestamp=datetime.now(timezone.utc).isoformat(),
        )
