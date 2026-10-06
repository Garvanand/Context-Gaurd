"""
Ablation A3: Fixed Policy (lambda = 0.0, Uniform Static Thresholds).

Evaluates the pipeline with full multimodal perception and explicit intent,
but disabling reversibility coupling (lambda = 0.0) and epistemic confidence gating.
Demonstrates the mathematical contribution of reversibility-aware gating.
"""

import time
from datetime import datetime, timezone
from typing import Dict, Any

from benchmark.schema.earb_schema import EARBPair
from evaluation.schemas import EvaluationRecord
from evaluation.baselines.base import BaseEvaluator
from backend.app.models.schemas import AnalysisRequest
from backend.pipeline.pipeline import ContextGuardPipeline
from backend.policy.thresholds import PolicyThresholds
from backend.policy.policy import DeterministicPolicyEngine


class FixedPolicyAblationEvaluator(BaseEvaluator):
    """
    A3: Fixed Policy Ablated Evaluator.
    """
    system_id = "a3"
    name = "A3: Fixed Policy (lambda = 0.0)"
    category_type = "ablation"

    def __init__(self):
        fixed_thresholds = PolicyThresholds(
            lambda_reversibility=0.0,
            stop_threshold=0.65,
            ask_threshold=0.35,
            min_confidence_threshold=0.0,
            high_severity_override_threshold=0.85,
        )
        self.policy = DeterministicPolicyEngine(thresholds=fixed_thresholds)
        self.pipeline = ContextGuardPipeline(policy_engine=self.policy)

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
            ablation="a3",
            error=None,
            timestamp=datetime.now(timezone.utc).isoformat(),
        )
