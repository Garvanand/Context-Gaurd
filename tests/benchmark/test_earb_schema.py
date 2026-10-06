"""
Unit tests for EARB schema validation and integrity rules.
"""

from pathlib import Path
import pytest
from pydantic import ValidationError

from benchmark.schema.earb_schema import (
    EARBPair,
    CategoryEnum,
    SeverityEnum,
    ReversibilityEnum,
    InterventionEnum,
    SEVERITY_SCORES,
    REVERSIBILITY_SCORES,
)
from benchmark.schema.validator import validate_earb_file, validate_earb_pair


def test_valid_earb_pair():
    pair_data = {
        "pair_id": "TEST-001",
        "base_artifact_id": "BASE-001",
        "artifact_type": "image",
        "artifact_path": "artifacts/test.png",
        "category": "Financial",
        "context": "Context description",
        "source_app": "BankApp",
        "recipient": "Self",
        "destination": "Internal Vault",
        "intended_action": "Save to vault",
        "risk_type": "Privacy",
        "risk_subtype": "Storage",
        "severity": "negligible",
        "reversibility": "fully",
        "evidence": ["Safe token"],
        "uncertainty": 0.05,
        "expected_intervention": "ACT",
        "acceptable_alternative": None,
        "is_negative_control": True,
        "is_ambiguous": False,
        "rationale": "Benign local archival.",
    }
    is_valid, pair, errors = validate_earb_pair(pair_data)
    assert is_valid is True
    assert pair is not None
    assert len(errors) == 0
    assert pair.severity_score == SEVERITY_SCORES[SeverityEnum.NEGLIGIBLE]
    assert pair.reversibility_score == REVERSIBILITY_SCORES[ReversibilityEnum.FULLY]


def test_negative_control_cannot_be_stop():
    """
    Negative control tasks must not have expected intervention STOP or WARN.
    """
    pair_data = {
        "pair_id": "TEST-002",
        "base_artifact_id": "BASE-002",
        "artifact_type": "image",
        "artifact_path": "artifacts/test.png",
        "category": "Financial",
        "context": "Benign task",
        "source_app": "App",
        "recipient": "Self",
        "destination": "Drive",
        "intended_action": "Save",
        "risk_type": "None",
        "risk_subtype": "None",
        "severity": "negligible",
        "reversibility": "fully",
        "evidence": [],
        "uncertainty": 0.0,
        "expected_intervention": "STOP",  # Contradiction!
        "acceptable_alternative": None,
        "is_negative_control": True,
        "is_ambiguous": False,
        "rationale": "Invalid test.",
    }
    is_valid, pair, errors = validate_earb_pair(pair_data)
    assert is_valid is False
    assert any("Negative control" in err for err in errors)


def test_validate_sample_earb_file():
    sample_file = Path("benchmark/data/sample_earb_pairs.json")
    assert sample_file.exists(), "Sample EARB dataset must exist"

    is_valid, dataset, errors = validate_earb_file(sample_file)
    assert is_valid is True
    assert dataset is not None
    assert dataset.total_count >= 5
    assert "Financial" in dataset.category_counts
