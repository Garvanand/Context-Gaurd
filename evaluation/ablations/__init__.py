"""
ContextGuard Ablations module.
"""

from evaluation.ablations.a1_no_intent import NoIntentAblationEvaluator
from evaluation.ablations.a2_no_multimodal import NoMultimodalityAblationEvaluator
from evaluation.ablations.a3_fixed_policy import FixedPolicyAblationEvaluator
from evaluation.ablations.a4_adaptive_policy import AdaptivePolicyAblationEvaluator
from evaluation.ablations.a5_warn_everything import WarnEverythingAblationEvaluator

ABLATIONS = {
    "a1": NoIntentAblationEvaluator,
    "a2": NoMultimodalityAblationEvaluator,
    "a3": FixedPolicyAblationEvaluator,
    "a4": AdaptivePolicyAblationEvaluator,
    "a5": WarnEverythingAblationEvaluator,
}

__all__ = [
    "NoIntentAblationEvaluator",
    "NoMultimodalityAblationEvaluator",
    "FixedPolicyAblationEvaluator",
    "AdaptivePolicyAblationEvaluator",
    "WarnEverythingAblationEvaluator",
    "ABLATIONS",
]
