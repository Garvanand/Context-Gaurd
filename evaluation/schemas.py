"""
Canonical schemas for the ContextGuard Research Evaluation Framework.

Enforces the Common Evaluation Contract:
- pair_id: string
- prediction: string ("ACT", "ASK", "WARN", "STOP")
- ground_truth: string ("ACT", "ASK", "WARN", "STOP")
- severity: float in [0.0, 1.0]
- reversibility: float in [0.0, 1.0]
- confidence: float in [0.0, 1.0]
- risk_score: float
- intervention: string (identical to prediction)
- latency_ms: float
- evidence: list of items/strings
- model: string identifier
- baseline: optional baseline id ("b1", "b3", "b4", "b5")
- ablation: optional ablation id ("a1", "a2", "a3", "a4", "a5")
- error: optional error description string
- timestamp: ISO 8601 string
"""

from datetime import datetime, timezone
from typing import Dict, Any, List, Optional
from pydantic import BaseModel, Field, field_validator


class EvaluationRecord(BaseModel):
    """
    Standard normalized evaluation row conforming to Common Evaluation Contract.
    """
    pair_id: str = Field(..., description="Unique benchmark pair identifier (e.g. EARB-FIN-001-A)")
    base_artifact_id: Optional[str] = Field(None, description="Identifier of the base artifact for cluster bootstrapping")
    prediction: str = Field(..., description="Predicted safety intervention (ACT, ASK, WARN, STOP)")
    ground_truth: str = Field(..., description="Ground-truth safety intervention (ACT, ASK, WARN, STOP)")
    severity: float = Field(..., description="Estimated or assigned harm severity score in [0.0, 1.0]")
    reversibility: float = Field(..., description="Estimated or assigned consequence reversibility in [0.0, 1.0]")
    confidence: float = Field(..., description="Epistemic/aleatoric confidence score in [0.0, 1.0]")
    risk_score: float = Field(..., description="Composite risk score rho")
    intervention: str = Field(..., description="Canonical intervention action (same as prediction)")
    latency_ms: float = Field(..., description="Inference execution latency in milliseconds")
    evidence: List[Any] = Field(default_factory=list, description="Extracted factual evidence list")
    model: str = Field(..., description="Model or engine identifier executing the inference")
    baseline: Optional[str] = Field(None, description="Baseline identifier (b1, b3, b4, b5)")
    ablation: Optional[str] = Field(None, description="Ablation identifier (a1, a2, a3, a4, a5)")
    error: Optional[str] = Field(None, description="Detailed error/failure message if execution failed")
    timestamp: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat(), description="ISO 8601 execution timestamp")

    @field_validator("prediction", "ground_truth", "intervention")
    @classmethod
    def normalize_intervention(cls, v: str) -> str:
        if not v:
            return "UNKNOWN"
        v_upper = v.upper().strip()
        valid = {"ACT", "ASK", "WARN", "STOP", "UNKNOWN", "ERROR"}
        if v_upper not in valid:
            raise ValueError(f"Invalid intervention: {v}. Must be one of {valid}")
        return v_upper


class LatencyDistribution(BaseModel):
    mean: float
    median: float
    p90: float
    p95: float
    p99: float
    min: float
    max: float
    std: float


class ConfidenceInterval(BaseModel):
    lower: float
    upper: float


class BootstrapCI(BaseModel):
    accuracy: ConfidenceInterval
    macro_f1: ConfidenceInterval
    stop_recall: ConfidenceInterval
    act_false_alarm_rate: ConfidenceInterval


class EvaluationMetrics(BaseModel):
    """
    Standard aggregated evaluation metrics object.
    """
    accuracy: float
    macro_precision: float
    macro_recall: float
    macro_f1: float
    per_class_f1: Dict[str, float]
    stop_recall: float
    act_false_alarm_rate: float
    confusion_matrix: Dict[str, Dict[str, int]]
    latency_distribution: LatencyDistribution
    ece: float
    bootstrap_ci_95: BootstrapCI
    sample_count: int
    failure_count: int = 0
    failure_rate: float = 0.0


class RunConfiguration(BaseModel):
    """
    Execution configuration recorded with every benchmark run.
    """
    system_id: str
    name: str
    category_type: str  # "baseline" or "ablation"
    dataset_path: str
    split: str  # "all", "dev", "test"
    total_samples: int
    parameters: Dict[str, Any]
    timestamp: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    python_version: str
    platform: str


class FailureRecord(BaseModel):
    """
    Explicit failure telemetry conforming to Rule 7 and NO FABRICATION requirement.
    """
    pair_id: str
    base_artifact_id: Optional[str] = None
    system_id: str
    ground_truth: str
    error_type: str
    error_message: str
    timestamp: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
