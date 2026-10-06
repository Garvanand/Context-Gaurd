"""
Automated unit & regression tests for the complete 60-pair EARB Benchmark Suite.
"""

import json
from pathlib import Path
import pytest

from benchmark.scripts.validate_benchmark import validate_earb_benchmark
from benchmark.schema.earb_schema import EARBPair, CategoryEnum, InterventionEnum


def test_full_earb_v1_benchmark_validity():
    """Verify that the full 60-pair EARB benchmark passes all integrity rules."""
    is_valid, report = validate_earb_benchmark()
    assert is_valid is True, f"Validation errors: {report.get('errors')}"
    assert report["total_pairs"] == 60
    assert report["total_base_artifacts"] == 20


def test_earb_category_and_split_balance():
    """Verify strict balance across 4 categories and zero-leakage partitions."""
    is_valid, report = validate_earb_benchmark()
    assert is_valid is True

    cat_counts = report["category_counts"]
    assert cat_counts["Financial"] == 15
    assert cat_counts["Digital Security"] == 15
    assert cat_counts["Privacy Disclosure"] == 15
    assert cat_counts["Communication"] == 15

    split_counts = report["split_counts"]
    assert split_counts["dev"] == 42   # 14 base artifacts * 3
    assert split_counts["test"] == 18  # 6 base artifacts * 3


def test_action_conditioned_risk_divergence():
    """
    Verify that for EVERY base artifact, candidate actions produce
    at least two distinct safety interventions (Core research hypothesis).
    """
    jsonl_path = Path("benchmark/data/earb_v1.jsonl")
    artifact_interventions = {}

    with open(jsonl_path, "r", encoding="utf-8") as f:
        for line in f:
            if not line.strip():
                continue
            pair = EARBPair(**json.loads(line))
            art_id = pair.base_artifact_id
            if art_id not in artifact_interventions:
                artifact_interventions[art_id] = set()
            artifact_interventions[art_id].add(pair.expected_intervention)

    assert len(artifact_interventions) == 20
    for art_id, intv_set in artifact_interventions.items():
        assert len(intv_set) >= 2, (
            f"Artifact {art_id} must have action-conditioned divergence! Got only {intv_set}"
        )


def test_synthetic_artifacts_exist_on_disk():
    """Verify that all 20 referenced synthetic artifact images exist on disk and are non-empty."""
    jsonl_path = Path("benchmark/data/earb_v1.jsonl")
    verified_paths = set()

    with open(jsonl_path, "r", encoding="utf-8") as f:
        for line in f:
            if not line.strip():
                continue
            pair = EARBPair(**json.loads(line))
            path = Path(pair.artifact_path)
            assert path.exists(), f"Missing artifact file: {path}"
            assert path.stat().st_size > 1000, f"Artifact file too small / corrupt: {path}"
            verified_paths.add(str(path))

    assert len(verified_paths) == 20, f"Expected 20 unique artifact files, found {len(verified_paths)}"
