"""
Evaluation Engine for ContextGuard Benchmark.

Manages execution lifecycle for Baselines and Ablations:
- Loads pairs and perceptual signals
- Executes evaluator while tracking wall-clock latency
- Enforces NO FABRICATION: Catches and logs explicit failure reports without inventing predictions
- Computes complete academic metrics with clustered bootstrap 95% CIs
- Writes configuration.json, predictions.jsonl, metrics.json, and README.md into results/
"""

import os
import sys
import json
import platform
from pathlib import Path
from datetime import datetime, timezone
from typing import Dict, Any, List, Optional, Tuple

from evaluation.dataset import BenchmarkDatasetLoader
from evaluation.schemas import (
    EvaluationRecord,
    RunConfiguration,
    EvaluationMetrics,
    FailureRecord,
)
from evaluation.metrics import compute_all_metrics
from evaluation.baselines.base import BaseEvaluator

RESULTS_DIR = Path("results")


class EvaluationEngine:
    """
    Orchestrates benchmark runs and output artifacts generation.
    """

    def __init__(self, output_root: Optional[Path] = None):
        self.output_root = output_root or RESULTS_DIR
        self.loader = BenchmarkDatasetLoader()

    def get_run_directory(self, evaluator: BaseEvaluator) -> Path:
        """
        Determines the target output directory:
        results/b1/, results/b3/, results/b4/, results/b5/ or results/ablations/a1/, etc.
        """
        sys_id = evaluator.system_id.lower()
        if evaluator.category_type == "ablation":
            run_dir = self.output_root / "ablations" / sys_id
        else:
            run_dir = self.output_root / sys_id

        run_dir.mkdir(parents=True, exist_ok=True)
        return run_dir

    async def execute_run(
        self,
        evaluator: BaseEvaluator,
        split: str = "all",
        num_bootstraps: int = 1000,
        seed: int = 42,
    ) -> Tuple[EvaluationMetrics, Path]:
        """
        Executes a benchmark run for an evaluator across the specified partition.
        """
        run_dir = self.get_run_directory(evaluator)
        enriched_pairs = self.loader.load_with_perception(split=split)

        print(f"\n============================================================")
        print(f"RUNNING: {evaluator.name}")
        print(f"Split: {split.upper()} ({len(enriched_pairs)} pairs) | Directory: {run_dir}")
        print(f"============================================================")

        # 1. Execute evaluator
        records: List[EvaluationRecord] = await evaluator.run(enriched_pairs)

        # 2. Check and log failures explicitly (No Fabrication rule)
        failures: List[FailureRecord] = []
        for r in records:
            if r.error or r.prediction == "ERROR":
                failures.append(
                    FailureRecord(
                        pair_id=r.pair_id,
                        base_artifact_id=r.base_artifact_id,
                        system_id=evaluator.system_id,
                        ground_truth=r.ground_truth,
                        error_type="ExecutionAnomaly",
                        error_message=r.error or "Unknown evaluation error",
                    )
                )

        if failures:
            failures_path = run_dir / "failures.json"
            failures_path.write_text(
                json.dumps([f.model_dump() for f in failures], indent=2),
                encoding="utf-8"
            )
            print(f"[WARNING] {len(failures)} failures recorded in {failures_path} (No fabrication).")

        # 3. Compute Metrics
        metrics = compute_all_metrics(records, num_bootstraps=num_bootstraps, seed=seed)

        # 4. Write configuration.json
        config = RunConfiguration(
            system_id=evaluator.system_id,
            name=evaluator.name,
            category_type=evaluator.category_type,
            dataset_path=str(self.loader.dataset_path),
            split=split,
            total_samples=len(records),
            parameters={
                "num_bootstraps": num_bootstraps,
                "bootstrap_seed": seed,
                "cluster_grouping": "base_artifact_id",
            },
            python_version=sys.version,
            platform=f"{platform.system()} {platform.release()}",
        )
        (run_dir / "configuration.json").write_text(
            config.model_dump_json(indent=2),
            encoding="utf-8"
        )

        # 5. Write predictions.jsonl
        predictions_path = run_dir / "predictions.jsonl"
        with open(predictions_path, "w", encoding="utf-8") as f:
            for r in records:
                f.write(r.model_dump_json() + "\n")

        # 6. Write metrics.json
        metrics_path = run_dir / "metrics.json"
        metrics_path.write_text(
            metrics.model_dump_json(indent=2),
            encoding="utf-8"
        )

        # 7. Write README.md
        readme_path = run_dir / "README.md"
        readme_content = self.generate_run_readme(evaluator, config, metrics, records)
        readme_path.write_text(readme_content, encoding="utf-8")

        print(f"Completed run: Accuracy={metrics.accuracy:.4f}, Macro F1={metrics.macro_f1:.4f}, "
              f"STOP Recall={metrics.stop_recall:.4f}, ACT FAR={metrics.act_false_alarm_rate:.4f}")
        print(f"Artifacts saved to: {run_dir.resolve()}")

        return metrics, run_dir

    def generate_run_readme(
        self,
        evaluator: BaseEvaluator,
        config: RunConfiguration,
        metrics: EvaluationMetrics,
        records: List[EvaluationRecord],
    ) -> str:
        """
        Generates a publication-grade Markdown README summarizing the run.
        """
        run_dir = self.get_run_directory(evaluator)
        cm = metrics.confusion_matrix
        lat = metrics.latency_distribution
        cis = metrics.bootstrap_ci_95

        return f"""# Evaluation Run Report: {evaluator.name}

**System Identifier:** `{evaluator.system_id}`  
**Category:** `{evaluator.category_type.upper()}`  
**Dataset Split:** `{config.split}` ({config.total_samples} samples)  
**Execution Timestamp:** `{config.timestamp}`  
**Platform:** `{config.platform}`  
**Python Runtime:** `{config.python_version.split()[0]}`  

---

## 1. Primary Academic Benchmark Metrics

| Metric | Empirical Score | 95% Confidence Interval (Clustered) |
| :--- | :---: | :---: |
| **Multi-Class Accuracy** | **{metrics.accuracy * 100:.2f}%** | [{cis.accuracy.lower * 100:.2f}%, {cis.accuracy.upper * 100:.2f}%] |
| **Macro Precision** | **{metrics.macro_precision:.4f}** | — |
| **Macro Recall** | **{metrics.macro_recall:.4f}** | — |
| **Macro F1 Score** | **{metrics.macro_f1:.4f}** | [{cis.macro_f1.lower:.4f}, {cis.macro_f1.upper:.4f}] |
| **STOP Recall** (Safety Critical) | **{metrics.stop_recall * 100:.2f}%** | [{cis.stop_recall.lower * 100:.2f}%, {cis.stop_recall.upper * 100:.2f}%] |
| **ACT False Alarm Rate** | **{metrics.act_false_alarm_rate * 100:.2f}%** | [{cis.act_false_alarm_rate.lower * 100:.2f}%, {cis.act_false_alarm_rate.upper * 100:.2f}%] |
| **Expected Calibration Error (ECE)** | **{metrics.ece:.4f}** | — |

---

## 2. Per-Class F1 Breakdown

| Safety Intervention | Ground Truth Count | Per-Class F1 Score |
| :--- | :---: | :---: |
| **ACT** (Benign Routine Task) | {sum(cm['ACT'].values())} | `{metrics.per_class_f1['ACT']:.4f}` |
| **ASK** (Clarification / Ambiguity) | {sum(cm['ASK'].values())} | `{metrics.per_class_f1['ASK']:.4f}` |
| **WARN** (Cautionary Hazard) | {sum(cm['WARN'].values())} | `{metrics.per_class_f1['WARN']:.4f}` |
| **STOP** (High-Consequence Hazard) | {sum(cm['STOP'].values())} | `{metrics.per_class_f1['STOP']:.4f}` |

---

## 3. Empirical Confusion Matrix

| Ground Truth \\ Prediction | ACT | ASK | WARN | STOP | Total |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **ACT** | **{cm['ACT']['ACT']}** | {cm['ACT']['ASK']} | {cm['ACT']['WARN']} | {cm['ACT']['STOP']} | {sum(cm['ACT'].values())} |
| **ASK** | {cm['ASK']['ACT']} | **{cm['ASK']['ASK']}** | {cm['ASK']['WARN']} | {cm['ASK']['STOP']} | {sum(cm['ASK'].values())} |
| **WARN** | {cm['WARN']['ACT']} | {cm['WARN']['ASK']} | **{cm['WARN']['WARN']}** | {cm['WARN']['STOP']} | {sum(cm['WARN'].values())} |
| **STOP** | {cm['STOP']['ACT']} | {cm['STOP']['ASK']} | {cm['STOP']['WARN']} | **{cm['STOP']['STOP']}** | {sum(cm['STOP'].values())} |

---

## 4. Latency Distribution (Wall-Clock ms)

- **Mean Latency:** `{lat.mean:.2f} ms`
- **Median Latency:** `{lat.median:.2f} ms`
- **P90 Latency:** `{lat.p90:.2f} ms`
- **P95 Latency:** `{lat.p95:.2f} ms`
- **P99 Latency:** `{lat.p99:.2f} ms`
- **Min / Max:** `{lat.min:.2f} ms` / `{lat.max:.2f} ms`
- **Standard Deviation:** `{lat.std:.2f} ms`

---

## 5. Artifact Verification & Integrity

- **Configuration:** [`configuration.json`](file://{Path(run_dir / "configuration.json").resolve()})
- **Normalized Predictions:** [`predictions.jsonl`](file://{Path(run_dir / "predictions.jsonl").resolve()})
- **Metrics JSON:** [`metrics.json`](file://{Path(run_dir / "metrics.json").resolve()})
- **Total Valid Pairs:** `{metrics.sample_count}`
- **Failures / Anomalies:** `{metrics.failure_count}` ({metrics.failure_rate * 100:.1f}%)
"""
