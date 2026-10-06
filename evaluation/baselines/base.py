"""
ContextGuard Evaluator Base Class.
"""

import time
import traceback
from abc import ABC, abstractmethod
from datetime import datetime, timezone
from typing import Dict, Any, List, Optional

from benchmark.schema.earb_schema import EARBPair
from evaluation.schemas import EvaluationRecord


class BaseEvaluator(ABC):
    """
    Abstract base evaluator for baselines and ablations.
    """
    system_id: str
    name: str
    category_type: str = "baseline"

    @abstractmethod
    async def evaluate_pair(self, pair: EARBPair, perception: Dict[str, Any]) -> EvaluationRecord:
        """
        Evaluates a single EARB pair and returns an EvaluationRecord.
        """
        pass

    async def run(self, enriched_pairs: List[Dict[str, Any]]) -> List[EvaluationRecord]:
        """
        Sequentially executes evaluation across all provided pairs,
        recording precise latency and catching any errors gracefully.
        """
        records: List[EvaluationRecord] = []
        for item in enriched_pairs:
            pair: EARBPair = item["pair"]
            perception: Dict[str, Any] = item["perception"]

            t0 = time.perf_counter()
            try:
                record = await self.evaluate_pair(pair, perception)
            except Exception as e:
                latency = round((time.perf_counter() - t0) * 1000.0, 2)
                record = EvaluationRecord(
                    pair_id=pair.pair_id,
                    base_artifact_id=pair.base_artifact_id,
                    prediction="ERROR",
                    ground_truth=pair.expected_intervention.value,
                    severity=0.5,
                    reversibility=0.5,
                    confidence=0.0,
                    risk_score=0.5,
                    intervention="ERROR",
                    latency_ms=latency,
                    evidence=[f"Execution exception: {e}"],
                    model=self.system_id,
                    baseline=self.system_id if self.category_type == "baseline" else None,
                    ablation=self.system_id if self.category_type == "ablation" else None,
                    error=f"{type(e).__name__}: {str(e)}\n{traceback.format_exc()}",
                    timestamp=datetime.now(timezone.utc).isoformat(),
                )
            records.append(record)
        return records
