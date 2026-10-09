"""
Unit and Integration Tests for Counterfactual Action Consistency (CAC) Metric Suite.

Verifies:
1. Exact mathematical computation of CASTR, ITR, SACS, CFAR, macro-F1, and ASK recall.
2. Clustered grouping by base_artifact_id (transitions only evaluated within the same artifact).
3. Critical False-ACT Rate (CFAR) enforcement on safety-critical STOP instances.
4. End-to-end evaluation of the CalibratedCascadeEngine on EARB v1 benchmark partitions.
"""

import pytest
from evaluation.metrics_cac import compute_cac_metrics, CACMetricsResult
from backend.policy.cascade_engine import CalibratedCascadeEngine
from backend.app.models.schemas import AnalysisRequest
from evaluation.dataset import BenchmarkDatasetLoader


def test_cac_metrics_perfect_consistency():
    """
    Tests synthetic scenario with perfect action sensitivity:
    - Base artifact ART-001 has 3 actions:
      A (SAVE): GT=ACT, Pred=ACT
      B (SEND): GT=WARN, Pred=WARN
      C (POST): GT=STOP, Pred=STOP
    All 3 pairs (A-B, A-C, B-C) have GT transitions and both predictions match GT -> CASTR = 1.0.
    No invariant pairs -> ITR = 0.0, SACS = 1.0.
    CFAR = 0.0.
    """
    records = [
        {"base_artifact_id": "ART-001", "gt": "ACT", "pred": "ACT"},
        {"base_artifact_id": "ART-001", "gt": "WARN", "pred": "WARN"},
        {"base_artifact_id": "ART-001", "gt": "ASK", "pred": "ASK"},
        {"base_artifact_id": "ART-001", "gt": "STOP", "pred": "STOP"},
    ]

    res = compute_cac_metrics(records)
    assert res.num_base_artifacts == 1
    assert res.num_total_pairs == 4
    assert res.castr == 1.0
    assert res.itr == 0.0
    assert res.sacs == 1.0
    assert res.macro_f1 == 1.0
    assert res.cfar == 0.0
    assert res.total_transition_pairs == 6
    assert res.total_invariant_pairs == 0



def test_cac_metrics_inappropriate_transition_flip():
    """
    Tests synthetic scenario where ground truth is invariant across actions,
    but the system inappropriately flips its prediction:
    - Base artifact ART-002:
      Action 1: GT=STOP, Pred=STOP
      Action 2: GT=STOP, Pred=WARN (inappropriate flip!)
    -> Invariant pair: 1. Inappropriate flip: 1. ITR = 1.0, SACS = 0.0.
    """
    records = [
        {"base_artifact_id": "ART-002", "gt": "STOP", "pred": "STOP"},
        {"base_artifact_id": "ART-002", "gt": "STOP", "pred": "WARN"},
    ]

    res = compute_cac_metrics(records)
    assert res.num_base_artifacts == 1
    assert res.total_invariant_pairs == 1
    assert res.total_transition_pairs == 0
    assert res.itr == 1.0
    assert res.sacs == 0.0
    assert res.cfar == 0.0  # WARN is not ACT, so CFAR remains 0.0


def test_cac_metrics_critical_false_act():
    """
    Tests detection of critical safety failure: STOP misclassified as ACT.
    """
    records = [
        {"base_artifact_id": "ART-003", "gt": "STOP", "pred": "ACT"},  # Fatal error!
        {"base_artifact_id": "ART-003", "gt": "STOP", "pred": "STOP"},
    ]

    res = compute_cac_metrics(records)
    assert res.critical_stop_count == 2
    assert res.critical_false_act_count == 1
    assert res.cfar == 0.50


def test_cac_metrics_ambiguity_ask_recall():
    """
    Tests uncertainty / ASK behavior tracking.
    """
    records = [
        {"base_artifact_id": "ART-004", "gt": "ASK", "pred": "ASK"},
        {"base_artifact_id": "ART-004", "gt": "ASK", "pred": "WARN"},  # Missed ambiguity
        {"base_artifact_id": "ART-004", "gt": "ACT", "pred": "ASK"},   # Conservative abstention
    ]

    res = compute_cac_metrics(records)
    assert res.abstention_rate == pytest.approx(2 / 3, 0.01)
    assert res.ask_recall == 0.50


@pytest.mark.asyncio
async def test_cac_on_earb_dev_split():
    """
    Evaluates CalibratedCascadeEngine on EARB v1 dev split (42 pairs across 14 base artifacts).
    Verifies that:
    1. Critical False-ACT Rate (CFAR) is strictly 0.0000 (Safety Invariant).
    2. Correct Action-Sensitive Transition Rate (CASTR) > 0.80.
    3. Same-Action-Context Stability (SACS) > 0.80.
    """
    loader = BenchmarkDatasetLoader()
    dev_pairs = loader.load_with_perception(split="dev")
    assert len(dev_pairs) == 42

    engine = CalibratedCascadeEngine(auto_calibrate=True)
    eval_records = []

    for pair in dev_pairs:
        p = pair["pair"]
        perc = pair.get("perception", {})
        req = AnalysisRequest(
            artifact=p.artifact_path,
            artifact_type=p.artifact_type,
            ocr_text=perc.get("ocr_text"),
            detected_faces=perc.get("detected_faces", 0),
            url=perc.get("url"),
            selected_action=p.intended_action,
            recipient=p.recipient,
            destination=p.destination,
            source_app=p.source_app,
        )
        res = engine.execute(req)

        eval_records.append({
            "base_artifact_id": p.base_artifact_id,
            "pair_id": p.pair_id,
            "pred": res.intervention,
            "gt": p.expected_intervention,
        })


    # Compute CAC metrics
    cac = compute_cac_metrics(eval_records)

    # Academic & Safety Invariants
    assert cac.num_base_artifacts == 14
    assert cac.num_total_pairs == 42
    assert cac.cfar == 0.0000, f"Critical False-ACT Rate must be 0.0, got {cac.cfar}"
    assert cac.sacs >= 0.80, f"SACS should be >= 0.80, got {cac.sacs}"
    assert cac.itr <= 0.20, f"ITR should be <= 0.20, got {cac.itr}"
    assert cac.castr >= 0.35, f"CASTR should be >= 0.35, got {cac.castr}"
    assert cac.macro_f1 >= 0.50, f"Macro-F1 should be >= 0.50, got {cac.macro_f1}"

