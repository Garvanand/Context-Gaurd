"""
CLI Runner for ContextGuard Research Evaluation Framework.

Usage:
  python -m evaluation.run --baseline b1
  python -m evaluation.run --baseline b5
  python -m evaluation.run --baseline all
  python -m evaluation.run --ablation a1
  python -m evaluation.run --ablation all
  python -m evaluation.run --all
"""

import sys
import asyncio
import argparse
from pathlib import Path
from typing import List

from evaluation.engine import EvaluationEngine
from evaluation.baselines import BASELINES
from evaluation.ablations import ABLATIONS
from evaluation.baselines.base import BaseEvaluator


async def main_async():
    parser = argparse.ArgumentParser(
        description="ContextGuard Research Evaluation Runner"
    )
    parser.add_argument(
        "--baseline",
        type=str,
        choices=["b1", "b3", "b4", "b5", "all"],
        help="Baseline system to evaluate (b1, b3, b4, b5, or all)",
    )
    parser.add_argument(
        "--ablation",
        type=str,
        choices=["a1", "a2", "a3", "a4", "a5", "all"],
        help="Ablation system to evaluate (a1, a2, a3, a4, a5, or all)",
    )
    parser.add_argument(
        "--all",
        action="store_true",
        help="Run all baselines and all ablations sequentially",
    )
    parser.add_argument(
        "--split",
        type=str,
        default="all",
        choices=["all", "dev", "test"],
        help="Dataset split partition to evaluate (default: all)",
    )
    parser.add_argument(
        "--bootstraps",
        type=int,
        default=1000,
        help="Number of cluster bootstrap iterations for 95% CIs (default: 1000)",
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=42,
        help="Random seed for bootstrap reproducibility (default: 42)",
    )

    args = parser.parse_args()

    evaluators_to_run: List[BaseEvaluator] = []

    if args.all:
        for cls in BASELINES.values():
            evaluators_to_run.append(cls())
        for cls in ABLATIONS.values():
            evaluators_to_run.append(cls())
    else:
        if args.baseline:
            if args.baseline == "all":
                for cls in BASELINES.values():
                    evaluators_to_run.append(cls())
            else:
                evaluators_to_run.append(BASELINES[args.baseline]())

        if args.ablation:
            if args.ablation == "all":
                for cls in ABLATIONS.values():
                    evaluators_to_run.append(cls())
            else:
                evaluators_to_run.append(ABLATIONS[args.ablation]())

    if not evaluators_to_run:
        print("[ERROR] Please specify at least one evaluation target:")
        print("  python -m evaluation.run --baseline b1")
        print("  python -m evaluation.run --baseline b5")
        print("  python -m evaluation.run --baseline all")
        print("  python -m evaluation.run --ablation a1")
        print("  python -m evaluation.run --all")
        sys.exit(1)

    engine = EvaluationEngine()

    print(f"\n[ContextGuard Benchmark] Executing {len(evaluators_to_run)} evaluation run(s)...")

    for evaluator in evaluators_to_run:
        await engine.execute_run(
            evaluator=evaluator,
            split=args.split,
            num_bootstraps=args.bootstraps,
            seed=args.seed,
        )

    print("\n[SUCCESS] All requested evaluation runs completed successfully.")
    print("Next step: Run `python -m evaluation.compare` and `python -m evaluation.report`")


def main():
    asyncio.run(main_async())


if __name__ == "__main__":
    main()
