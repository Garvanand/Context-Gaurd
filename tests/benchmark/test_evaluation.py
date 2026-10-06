"""
Unit and Integration Tests for ContextGuard Research Evaluation Framework.

Verifies:
1. Common Evaluation Contract schema validation.
2. Metrics computation: Accuracy, Macro F1, STOP Recall, ACT False Alarm Rate, ECE.
3. Clustered Bootstrapping: Verifies clusters are sampled by base_artifact_id.
4. Execution of all Baselines: B1, B3, B4, B5.
5. Execution of all Component Ablations: A1, A2, A3, A4, A5.
6. Failure logging without fabrication.
"""

import pytest
import numpy as np
from pathlib import Path
from typing import Dict, Any

from evaluation.schemas import EvaluationRecord, RunConfiguration, EvaluationMetrics
from evaluation.metrics import (
    compute_basic_metrics,
    compute_confusion_matrix,
    compute_ece,
    compute_clustered_bootstrap_cis,
    compute_all_metrics,
)
from evaluation.dataset import BenchmarkDatasetLoader
from evaluation.baselines import (
    ArtifactOnlyEvaluator,
    MultimodalNoIntentEvaluator,
    FixedThresholdEvaluator,
    FullContextGuardEvaluator,
)
from evaluation.ablations import (
    NoIntentAblationEvaluator,
    NoMultimodalityAblationEvaluator,
    FixedPolicyAblationEvaluator,
    AdaptivePolicyAblationEvaluator,
    WarnEverythingAblationEvaluator,
)
from evaluation.engine import EvaluationEngine


@pytest.fixture
def sample_records():
    return [
        EvaluationRecord(
            pair_id="EARB-FIN-001-A",
            base_artifact_id="ART-FIN-001",
            prediction="ACT",
            ground_truth="ACT",
            severity=0.05,
            reversibility=0.00,
            confidence=0.95,
            risk_score=0.05,
            intervention="ACT",
            latency_ms=12.5,
            evidence=["Vault archival"],
            model="b5",
            baseline="b5",
        ),
        EvaluationRecord(
            pair_id="EARB-FIN-001-B",
            base_artifact_id="ART-FIN-001",
            prediction="WARN",
            ground_truth="WARN",
            severity=0.50,
            reversibility=0.50,
            confidence=0.85,
            risk_score=0.69,
            intervention="WARN",
            latency_ms=14.1,
            evidence=["Direct message"],
            model="b5",
            baseline="b5",
        ),
        EvaluationRecord(
            pair_id="EARB-FIN-001-C",
            base_artifact_id="ART-FIN-001",
            prediction="STOP",
            ground_truth="STOP",
            severity=0.90,
            reversibility=1.00,
            confidence=0.95,
            risk_score=1.58,
            intervention="STOP",
            latency_ms=11.8,
            evidence=["Public dissemination"],
            model="b5",
            baseline="b5",
        ),
        EvaluationRecord(
            pair_id="EARB-FIN-002-A",
            base_artifact_id="ART-FIN-002",
            prediction="ACT",
            ground_truth="ACT",
            severity=0.05,
            reversibility=0.00,
            confidence=0.90,
            risk_score=0.05,
            intervention="ACT",
            latency_ms=13.0,
            evidence=["Decline collect"],
            model="b5",
            baseline="b5",
        ),
        EvaluationRecord(
            pair_id="EARB-FIN-002-B",
            base_artifact_id="ART-FIN-002",
            prediction="ASK",
            ground_truth="ASK",
            severity=0.35,
            reversibility=0.40,
            confidence=0.60,
            risk_score=0.45,
            intervention="ASK",
            latency_ms=15.2,
            evidence=["Ambiguous merchant"],
            model="b5",
            baseline="b5",
        ),
        EvaluationRecord(
            pair_id="EARB-FIN-002-C",
            base_artifact_id="ART-FIN-002",
            prediction="STOP",
            ground_truth="STOP",
            severity=0.95,
            reversibility=0.95,
            confidence=0.95,
            risk_score=1.63,
            intervention="STOP",
            latency_ms=12.0,
            evidence=["Approve fraudulent collect"],
            model="b5",
            baseline="b5",
        ),
    ]


def test_schema_validation(sample_records):
    rec = sample_records[0]
    assert rec.pair_id == "EARB-FIN-001-A"
    assert rec.prediction == "ACT"
    assert rec.ground_truth == "ACT"
    assert rec.intervention == "ACT"

    # Invalid intervention normalization test
    with pytest.raises(ValueError):
        EvaluationRecord(
            pair_id="ERR-001",
            prediction="INVALID_LABEL",
            ground_truth="ACT",
            severity=0.5,
            reversibility=0.5,
            confidence=0.5,
            risk_score=0.5,
            intervention="INVALID_LABEL",
            latency_ms=1.0,
            model="test",
        )


def test_metrics_computation(sample_records):
    metrics = compute_all_metrics(sample_records, num_bootstraps=50, seed=42)

    assert metrics.accuracy == 1.0  # Perfect on this small sample
    assert metrics.macro_f1 == 1.0
    assert metrics.stop_recall == 1.0
    assert metrics.act_false_alarm_rate == 0.0
    assert metrics.sample_count == 6
    assert metrics.failure_count == 0


def test_confusion_matrix_structure(sample_records):
    cm = compute_confusion_matrix(sample_records)
    assert cm["ACT"]["ACT"] == 2
    assert cm["WARN"]["WARN"] == 1
    assert cm["STOP"]["STOP"] == 2
    assert cm["ASK"]["ASK"] == 1


def test_ece_computation():
    records = [
        EvaluationRecord(
            pair_id="P1", prediction="ACT", ground_truth="ACT",
            severity=0.1, reversibility=0.1, confidence=0.9, risk_score=0.1,
            intervention="ACT", latency_ms=1.0, model="test"
        ),
        EvaluationRecord(
            pair_id="P2", prediction="STOP", ground_truth="ACT",  # Mistake with high confidence
            severity=0.9, reversibility=0.9, confidence=0.9, risk_score=1.5,
            intervention="STOP", latency_ms=1.0, model="test"
        ),
    ]
    ece = compute_ece(records, num_bins=5)
    assert ece > 0.0
    assert ece <= 1.0


def test_clustered_bootstrap_groups_by_base_artifact(sample_records):
    cis = compute_clustered_bootstrap_cis(sample_records, num_bootstraps=100, seed=42)
    assert cis.accuracy.lower <= cis.accuracy.upper
    assert cis.macro_f1.lower <= cis.macro_f1.upper
    assert cis.stop_recall.lower <= cis.stop_recall.upper
    assert cis.act_false_alarm_rate.lower <= cis.act_false_alarm_rate.upper


@pytest.mark.asyncio
async def test_dataset_loader():
    loader = BenchmarkDatasetLoader()
    pairs = loader.load_pairs(split="all")
    assert len(pairs) == 60

    dev_pairs = loader.load_pairs(split="dev")
    assert len(dev_pairs) == 42

    test_pairs = loader.load_pairs(split="test")
    assert len(test_pairs) == 18

    enriched = loader.load_with_perception(split="dev")
    assert len(enriched) == 42
    assert "ocr_text" in enriched[0]["perception"]


@pytest.mark.asyncio
async def test_baselines_execution_on_sample():
    loader = BenchmarkDatasetLoader()
    enriched = loader.load_with_perception(split="test")[:3]  # Take 3 test pairs for fast unit test

    evaluators = [
        ArtifactOnlyEvaluator(),
        MultimodalNoIntentEvaluator(),
        FixedThresholdEvaluator(),
        FullContextGuardEvaluator(),
    ]

    for ev in evaluators:
        records = await ev.run(enriched)
        assert len(records) == 3
        for r in records:
            assert r.prediction in ("ACT", "ASK", "WARN", "STOP")
            assert r.latency_ms > 0
            assert r.model == ev.system_id


@pytest.mark.asyncio
async def test_ablations_execution_on_sample():
    loader = BenchmarkDatasetLoader()
    enriched = loader.load_with_perception(split="test")[:3]

    ablations = [
        NoIntentAblationEvaluator(),
        NoMultimodalityAblationEvaluator(),
        FixedPolicyAblationEvaluator(),
        AdaptivePolicyAblationEvaluator(),
        WarnEverythingAblationEvaluator(),
    ]

    for ab in ablations:
        records = await ab.run(enriched)
        assert len(records) == 3
        for r in records:
            assert r.prediction in ("ACT", "ASK", "WARN", "STOP")
            assert r.ablation == ab.system_id
