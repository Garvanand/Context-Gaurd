"""
Baseline B4: Multimodal + Intent + Fixed Threshold Baseline.

Multimodal inputs with explicit intent, but uses a naive fixed threshold policy
WITHOUT consequence reversibility coupling (lambda = 0.0) and without dynamic epistemic gating.
Demonstrates failure to capture irreversible exposure amplification.
"""

import time
from datetime import datetime, timezone
from typing import Dict, Any, List

from benchmark.schema.earb_schema import EARBPair
from evaluation.schemas import EvaluationRecord
from evaluation.baselines.base import BaseEvaluator
from backend.app.models.schemas import AnalysisRequest
from backend.pipeline.pipeline import ContextGuardPipeline
from backend.policy.thresholds import PolicyThresholds
from backend.policy.policy import DeterministicPolicyEngine


class FixedThresholdEvaluator(BaseEvaluator):
    """
    B4: Multimodal + Intent with fixed threshold (lambda = 0.0).
    """
    system_id = "b4"
    name = "B4: Multimodal + Intent + Fixed Threshold"
    category_type = "baseline"

    def __init__(self):
        # lambda = 0.0: No reversibility penalty, fixed cutoffs
        fixed_thresholds = PolicyThresholds(
            lambda_reversibility=0.0,
            stop_threshold=0.65,
            ask_threshold=0.35,
            min_confidence_threshold=0.0,  # Disabled epistemic gating
            high_severity_override_threshold=0.85,
        )
        self.policy_engine = DeterministicPolicyEngine(thresholds=fixed_thresholds)
        self.pipeline = ContextGuardPipeline(policy_engine=self.policy_engine)

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
            user_context={"user_intent": pair.context},
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
            baseline="b4",
            ablation=None,
            error=None,
            timestamp=datetime.now(timezone.utc).isoformat(),
        )
