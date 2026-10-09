"""
ContextGuard Research Evaluation Framework.

Provides rigorous, reproducible benchmarking across:
- Baselines: B1 (Artifact-only), B3 (Multimodal no-intent), B4 (Fixed threshold), B5 (Full ContextGuard)
- Ablations: A1 (No intent), A2 (No multimodality), A3 (Fixed policy), A4 (Adaptive policy), A5 (Warn everything)
- Metrics: Multi-class accuracy, Macro F1, STOP Recall, ACT False Alarm Rate, ECE, Clustered Bootstrap 95% CIs.
"""

from evaluation.metrics_cac import compute_cac_metrics, CACMetricsResult

__version__ = "1.1.0"

__all__ = [
    "compute_cac_metrics",
    "CACMetricsResult",
]
