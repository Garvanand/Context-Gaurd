"""
Comparison Module for ContextGuard Baselines and Ablations.

Usage:
  python -m evaluation.compare
  python -m evaluation.compare --auto-run

Generates:
  results/tables/baseline_comparison.csv
  results/tables/baseline_comparison.md
  results/tables/ablation_study.csv
  results/tables/ablation_study.md
"""

import sys
import json
import asyncio
import argparse
from pathlib import Path
from typing import Dict, Any, List, Optional

from evaluation.schemas import EvaluationMetrics
from evaluation.engine import EvaluationEngine
from evaluation.baselines import BASELINES
from evaluation.ablations import ABLATIONS

RESULTS_DIR = Path("results")
TABLES_DIR = RESULTS_DIR / "tables"


def load_metrics(metrics_path: Path) -> Optional[Dict[str, Any]]:
    if not metrics_path.exists():
        return None
    try:
        return json.loads(metrics_path.read_text(encoding="utf-8"))
    except Exception:
        return None


async def ensure_runs(auto_run: bool = False, split: str = "all"):
    """
    If any baseline or ablation run is missing and auto_run is True, executes it.
    """
    engine = EvaluationEngine()
    targets = [
        ("b1", BASELINES["b1"](), RESULTS_DIR / "b1" / "metrics.json"),
        ("b3", BASELINES["b3"](), RESULTS_DIR / "b3" / "metrics.json"),
        ("b4", BASELINES["b4"](), RESULTS_DIR / "b4" / "metrics.json"),
        ("b5", BASELINES["b5"](), RESULTS_DIR / "b5" / "metrics.json"),
        ("b6", BASELINES["b6"](), RESULTS_DIR / "b6" / "metrics.json"),
        ("a1", ABLATIONS["a1"](), RESULTS_DIR / "ablations" / "a1" / "metrics.json"),
        ("a2", ABLATIONS["a2"](), RESULTS_DIR / "ablations" / "a2" / "metrics.json"),
        ("a3", ABLATIONS["a3"](), RESULTS_DIR / "ablations" / "a3" / "metrics.json"),
        ("a4", ABLATIONS["a4"](), RESULTS_DIR / "ablations" / "a4" / "metrics.json"),
        ("a5", ABLATIONS["a5"](), RESULTS_DIR / "ablations" / "a5" / "metrics.json"),
    ]

    for sys_id, evaluator, path in targets:
        if not path.exists():
            if auto_run:
                print(f"[AUTO-RUN] Missing metrics for {sys_id.upper()}. Executing evaluation...")
                await engine.execute_run(evaluator=evaluator, split=split)
            else:
                print(f"[INFO] Metrics missing for {sys_id.upper()} at {path}. (Pass --auto-run to compute automatically)")


def build_markdown_table(headers: List[str], rows: List[List[str]]) -> str:
    col_widths = [len(h) for h in headers]
    for row in rows:
        for i, val in enumerate(row):
            col_widths[i] = max(col_widths[i], len(str(val)))

    header_line = "| " + " | ".join(h.ljust(col_widths[i]) for i, h in enumerate(headers)) + " |"
    separator_line = "| " + " | ".join(":" + "-" * (col_widths[i] - 1) for i in range(len(headers))) + " |"
    data_lines = []
    for row in rows:
        line = "| " + " | ".join(str(val).ljust(col_widths[i]) for i, val in enumerate(row)) + " |"
        data_lines.append(line)

    return "\n".join([header_line, separator_line] + data_lines)


def build_csv_content(headers: List[str], rows: List[List[str]]) -> str:
    lines = [",".join(f'"{h}"' for h in headers)]
    for row in rows:
        lines.append(",".join(f'"{val}"' for val in row))
    return "\n".join(lines)


def compare_systems():
    TABLES_DIR.mkdir(parents=True, exist_ok=True)

    baseline_entries = [
        ("B1: Artifact-Only", RESULTS_DIR / "b1" / "metrics.json"),
        ("B3: Multimodal (No Intent)", RESULTS_DIR / "b3" / "metrics.json"),
        ("B4: Intent + Fixed Threshold", RESULTS_DIR / "b4" / "metrics.json"),
        ("B5: ContextGuard (Oracle Intent)", RESULTS_DIR / "b5" / "metrics.json"),
        ("B6: ContextGuard (Inferred Intent)", RESULTS_DIR / "b6" / "metrics.json"),
    ]

    ablation_entries = [
        ("A1: No Intent", RESULTS_DIR / "ablations" / "a1" / "metrics.json"),
        ("A2: No Multimodality", RESULTS_DIR / "ablations" / "a2" / "metrics.json"),
        ("A3: Fixed Policy (lambda=0)", RESULTS_DIR / "ablations" / "a3" / "metrics.json"),
        ("A4: Adaptive Policy (lambda=0.75)", RESULTS_DIR / "ablations" / "a4" / "metrics.json"),
        ("A5: Warn Everything", RESULTS_DIR / "ablations" / "a5" / "metrics.json"),
    ]

    headers = [
        "System",
        "Accuracy (%)",
        "Macro F1",
        "STOP Recall (%)",
        "ACT FAR (%)",
        "ECE",
        "Mean Latency (ms)",
        "Macro F1 95% CI",
    ]

    # 1. Process Baselines
    b_rows = []
    for name, path in baseline_entries:
        data = load_metrics(path)
        if not data:
            b_rows.append([name, "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "Not evaluated"])
            continue

        acc = f"{data['accuracy'] * 100:.2f}%"
        f1 = f"{data['macro_f1']:.4f}"
        stop_rec = f"{data['stop_recall'] * 100:.2f}%"
        act_far = f"{data['act_false_alarm_rate'] * 100:.2f}%"
        ece = f"{data['ece']:.4f}"
        lat = f"{data['latency_distribution']['mean']:.2f}"
        ci = f"[{data['bootstrap_ci_95']['macro_f1']['lower']:.4f}, {data['bootstrap_ci_95']['macro_f1']['upper']:.4f}]"
        b_rows.append([name, acc, f1, stop_rec, act_far, ece, lat, ci])

    b_md = build_markdown_table(headers, b_rows)
    b_csv = build_csv_content(headers, b_rows)

    (TABLES_DIR / "baseline_comparison.md").write_text(
        f"# ContextGuard Baseline Comparison Table\n\n{b_md}\n", encoding="utf-8"
    )
    (TABLES_DIR / "baseline_comparison.csv").write_text(b_csv, encoding="utf-8")

    # 2. Process Ablations
    a_rows = []
    for name, path in ablation_entries:
        data = load_metrics(path)
        if not data:
            a_rows.append([name, "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "Not evaluated"])
            continue

        acc = f"{data['accuracy'] * 100:.2f}%"
        f1 = f"{data['macro_f1']:.4f}"
        stop_rec = f"{data['stop_recall'] * 100:.2f}%"
        act_far = f"{data['act_false_alarm_rate'] * 100:.2f}%"
        ece = f"{data['ece']:.4f}"
        lat = f"{data['latency_distribution']['mean']:.2f}"
        ci = f"[{data['bootstrap_ci_95']['macro_f1']['lower']:.4f}, {data['bootstrap_ci_95']['macro_f1']['upper']:.4f}]"
        a_rows.append([name, acc, f1, stop_rec, act_far, ece, lat, ci])

    a_md = build_markdown_table(headers, a_rows)
    a_csv = build_csv_content(headers, a_rows)

    (TABLES_DIR / "ablation_study.md").write_text(
        f"# ContextGuard Component Ablation Study\n\n{a_md}\n", encoding="utf-8"
    )
    (TABLES_DIR / "ablation_study.csv").write_text(a_csv, encoding="utf-8")

    print("\n=========================================================================================")
    print("CONTEXTGUARD EMPIRICAL BASELINE COMPARISON")
    print("=========================================================================================")
    print(b_md)
    print("\n=========================================================================================")
    print("CONTEXTGUARD COMPONENT ABLATION STUDY")
    print("=========================================================================================")
    print(a_md)
    print(f"\nSaved tables to: {TABLES_DIR.resolve()}")


async def main_async():
    parser = argparse.ArgumentParser(description="ContextGuard Baseline and Ablation Comparison")
    parser.add_argument("--auto-run", action="store_true", help="Execute any missing runs before comparing")
    parser.add_argument("--split", type=str, default="all", choices=["all", "dev", "test"], help="Dataset split")
    args = parser.parse_args()

    if args.auto_run:
        await ensure_runs(auto_run=True, split=args.split)

    compare_systems()


def main():
    asyncio.run(main_async())


if __name__ == "__main__":
    main()
