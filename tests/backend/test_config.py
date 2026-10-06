"""
Tests for configuration parsing and validation.
"""

import pytest
from pydantic import ValidationError
from backend.app.core.config import Settings


def test_default_settings():
    s = Settings()
    assert s.app_name == "ContextGuard"
    assert s.storage_mode == "memory_only"
    assert s.stop_threshold == 0.65
    assert s.ask_threshold == 0.35
    assert s.min_confidence_threshold == 0.70
    assert s.lambda_reversibility == 0.75


def test_invalid_threshold_relationship():
    """
    ask_threshold must be strictly less than stop_threshold.
    """
    with pytest.raises(ValidationError):
        Settings(ask_threshold=0.80, stop_threshold=0.65)


def test_threshold_out_of_bounds():
    with pytest.raises(ValidationError):
        Settings(stop_threshold=1.5)  # must be <= 1.0

    with pytest.raises(ValidationError):
        Settings(lambda_reversibility=-0.1)  # must be >= 0.0
