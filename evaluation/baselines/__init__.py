"""
ContextGuard Baselines module.
"""

from evaluation.baselines.base import BaseEvaluator
from evaluation.baselines.b1_artifact_only import ArtifactOnlyEvaluator
from evaluation.baselines.b3_no_intent import MultimodalNoIntentEvaluator
from evaluation.baselines.b4_fixed_threshold import FixedThresholdEvaluator
from evaluation.baselines.b5_full_contextguard import FullContextGuardEvaluator
from evaluation.baselines.b6_inferred_intent import InferredIntentEvaluator

BASELINES = {
    "b1": ArtifactOnlyEvaluator,
    "b3": MultimodalNoIntentEvaluator,
    "b4": FixedThresholdEvaluator,
    "b5": FullContextGuardEvaluator,
    "b6": InferredIntentEvaluator,
}

__all__ = [
    "BaseEvaluator",
    "ArtifactOnlyEvaluator",
    "MultimodalNoIntentEvaluator",
    "FixedThresholdEvaluator",
    "FullContextGuardEvaluator",
    "InferredIntentEvaluator",
    "BASELINES",
]
