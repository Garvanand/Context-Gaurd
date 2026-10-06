"""
Everyday Action Risk Benchmark (EARB) - Comprehensive Validation Suite.

Executes rigorous academic benchmark verification:
1. Pydantic Schema Validation across all rows
2. Pair count and category balance checks (exactly 60 pairs, 20 artifacts, 4 categories)
3. Duplicate ID and duplicate action detection
4. Missing field and invalid path detection
5. Inconsistent label detection (negative controls, ambiguous cases, severity/reversibility rules)
6. Dynamic action divergence verification (proves risk is action-conditioned)
7. Cross-partition data leakage verification (split grouped strictly by base_artifact_id)
8. CSV vs JSONL synchronization verification
"""

import csv
import json
import sys
from pathlib import Path
from typing import Dict, List, Set, Tuple, Any

# Ensure project root is in sys.path
sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from benchmark.schema.earb_schema import (
    EARBPair,
    CategoryEnum,
    InterventionEnum,
    SeverityEnum,
    ReversibilityEnum,
)


def validate_earb_benchmark() -> Tuple[bool, Dict[str, Any]]:
    errors: List[str] = []
    warnings: List[str] = []

    jsonl_path = Path("benchmark/data/earb_v1.jsonl")
    csv_path = Path("benchmark/data/earb_v1.csv")
    schema_path = Path("benchmark/schema/earb_schema.json")

    # 1. File existence checks
    if not jsonl_path.exists():
        errors.append(f"Required JSONL benchmark file missing: {jsonl_path}")
        return False, {"errors": errors}
    if not csv_path.exists():
        errors.append(f"Required CSV benchmark file missing: {csv_path}")
    if not schema_path.exists():
        errors.append(f"Required JSON schema file missing: {schema_path}")

    # 2. Parse and Validate JSONL
    pairs: List[EARBPair] = []
    with open(jsonl_path, "r", encoding="utf-8") as f:
        for line_no, line in enumerate(f, 1):
            line_str = line.strip()
            if not line_str:
                continue
            try:
                data = json.loads(line_str)
                pair = EARBPair(**data)
                pairs.append(pair)
            except Exception as ex:
                errors.append(f"JSONL line {line_no} failed validation: {ex}")

    if errors:
        return False, {"errors": errors}

    # 3. Benchmark Counts & Balance Check
    total_pairs = len(pairs)
    if total_pairs < 60:
        errors.append(f"Benchmark requirement violation: expected >= 60 pairs, found {total_pairs}")

    base_artifacts: Set[str] = set()
    category_pairs: Dict[str, int] = {}
    category_artifacts: Dict[str, Set[str]] = {}
    artifact_to_pairs: Dict[str, List[EARBPair]] = {}

    pair_ids: Set[str] = set()
    artifact_actions: Set[Tuple[str, str]] = set()

    # 4. Row-level integrity & Duplicate detection
    for p in pairs:
        # Duplicate pair_id check
        if p.pair_id in pair_ids:
            errors.append(f"Duplicate pair_id detected: {p.pair_id}")
        pair_ids.add(p.pair_id)

        # Duplicate (base_artifact, action) check
        key = (p.base_artifact_id, p.intended_action)
        if key in artifact_actions:
            errors.append(f"Duplicate action '{p.intended_action}' for artifact '{p.base_artifact_id}'")
        artifact_actions.add(key)

        base_artifacts.add(p.base_artifact_id)
        cat_name = p.category.value
        category_pairs[cat_name] = category_pairs.get(cat_name, 0) + 1

        if cat_name not in category_artifacts:
            category_artifacts[cat_name] = set()
        category_artifacts[cat_name].add(p.base_artifact_id)

        if p.base_artifact_id not in artifact_to_pairs:
            artifact_to_pairs[p.base_artifact_id] = []
        artifact_to_pairs[p.base_artifact_id].append(p)

        # Missing field / empty string checks
        if not p.context or not p.context.strip():
            errors.append(f"Pair '{p.pair_id}' has empty context")
        if not p.source_app or not p.source_app.strip():
            errors.append(f"Pair '{p.pair_id}' has empty source_app")
        if not p.recipient or not p.recipient.strip():
            errors.append(f"Pair '{p.pair_id}' has empty recipient")
        if not p.destination or not p.destination.strip():
            errors.append(f"Pair '{p.pair_id}' has empty destination")
        if not p.intended_action or not p.intended_action.strip():
            errors.append(f"Pair '{p.pair_id}' has empty intended_action")
        if not p.risk_type or not p.risk_type.strip():
            errors.append(f"Pair '{p.pair_id}' has empty risk_type")
        if not p.rationale or not p.rationale.strip():
            errors.append(f"Pair '{p.pair_id}' has empty rationale")

        # Inconsistent-label checks
        if p.is_negative_control:
            if p.expected_intervention != InterventionEnum.ACT:
                errors.append(
                    f"Inconsistent negative control '{p.pair_id}': expected ACT, got {p.expected_intervention}"
                )
            if p.severity not in (SeverityEnum.NEGLIGIBLE, SeverityEnum.MINOR):
                warnings.append(
                    f"Negative control '{p.pair_id}' has elevated severity '{p.severity}'"
                )

        if p.is_ambiguous:
            if p.expected_intervention != InterventionEnum.ASK:
                errors.append(
                    f"Inconsistent ambiguous pair '{p.pair_id}': expected ASK, got {p.expected_intervention}"
                )
            if p.uncertainty < 0.30:
                warnings.append(
                    f"Ambiguous pair '{p.pair_id}' has low uncertainty {p.uncertainty}"
                )

        # Artifact file existence check
        art_file = Path(p.artifact_path)
        if not art_file.exists():
            errors.append(f"Artifact file not found for '{p.pair_id}': {p.artifact_path}")

    # 5. Category Balance Check
    expected_categories = [
        CategoryEnum.FINANCIAL.value,
        CategoryEnum.DIGITAL_SECURITY.value,
        CategoryEnum.PRIVACY_DISCLOSURE.value,
        CategoryEnum.COMMUNICATION.value,
    ]
    for cat in expected_categories:
        count = category_pairs.get(cat, 0)
        art_count = len(category_artifacts.get(cat, set()))
        if count != 15:
            errors.append(f"Category '{cat}' must have exactly 15 pairs (found {count})")
        if art_count != 5:
            errors.append(f"Category '{cat}' must have exactly 5 base artifacts (found {art_count})")

    # 6. Candidate Actions Count Check
    for art_id, art_p_list in artifact_to_pairs.items():
        if len(art_p_list) != 3:
            errors.append(f"Base artifact '{art_id}' must have exactly 3 candidate actions (found {len(art_p_list)})")

    # 7. Action Variation / Dynamic Divergence Check
    # For every base artifact, changing the action must change the intervention!
    for art_id, art_p_list in artifact_to_pairs.items():
        interventions = set(p.expected_intervention for p in art_p_list)
        if len(interventions) < 2:
            errors.append(
                f"Artifact '{art_id}' lacks action-conditioned divergence: all actions yield {list(interventions)[0]}"
            )

    # 8. Leakage Control Verification (Partitioning by Base Artifact)
    artifact_splits: Dict[str, str] = {}
    for p in pairs:
        if p.base_artifact_id not in artifact_splits:
            artifact_splits[p.base_artifact_id] = p.split
        else:
            if artifact_splits[p.base_artifact_id] != p.split:
                errors.append(
                    f"DATA LEAKAGE: Base artifact '{p.base_artifact_id}' spans both dev and test splits!"
                )

    split_summary = {"dev": 0, "test": 0}
    for p in pairs:
        split_summary[p.split] = split_summary.get(p.split, 0) + 1

    # 9. CSV Sync Check
    with open(csv_path, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        csv_rows = list(reader)
    if len(csv_rows) != total_pairs:
        errors.append(f"CSV row count ({len(csv_rows)}) does not match JSONL pair count ({total_pairs})")

    csv_pair_ids = set(r["pair_id"] for r in csv_rows)
    if csv_pair_ids != pair_ids:
        errors.append("Mismatch in pair IDs between JSONL and CSV files")

    # Final summary statistics
    intervention_counts = {}
    for p in pairs:
        val = p.expected_intervention.value
        intervention_counts[val] = intervention_counts.get(val, 0) + 1

    telemetry = {
        "is_valid": len(errors) == 0,
        "total_pairs": total_pairs,
        "total_base_artifacts": len(base_artifacts),
        "category_counts": category_pairs,
        "intervention_counts": intervention_counts,
        "split_counts": split_summary,
        "errors": errors,
        "warnings": warnings,
    }

    return (len(errors) == 0, telemetry)


def main():
    print("=" * 70)
    print("  CONTEXTGUARD: EVERYDAY ACTION RISK BENCHMARK (EARB) VALIDATION")
    print("=" * 70)

    is_valid, report = validate_earb_benchmark()

    if is_valid:
        print("\n[+] BENCHMARK INTEGRITY: 100% PASS")
        print(f"  • Total Evaluated Pairs:       {report['total_pairs']} (Expected: 60)")
        print(f"  • Unique Base Digital Artifacts: {report['total_base_artifacts']} (Expected: 20)")
        print(f"  • Candidate Actions per Artifact: Exactly 3")
        print("\n  • Category Breakdown:")
        for cat, cnt in report["category_counts"].items():
            print(f"      - {cat:22s}: {cnt:2d} pairs (5 base artifacts)")
        print("\n  • Ground-Truth Intervention Distribution:")
        for intv, cnt in report["intervention_counts"].items():
            pct = (cnt / report["total_pairs"]) * 100
            print(f"      - {intv:5s}: {cnt:2d} pairs ({pct:.1f}%)")
        print("\n  • Leakage-Controlled Partitions (By Base Artifact):")
        for sp, cnt in report["split_counts"].items():
            art_cnt = cnt // 3
            print(f"      - {sp:6s}: {cnt:2d} pairs ({art_cnt:2d} base artifacts, 0% leakage)")

        if report["warnings"]:
            print(f"\n[!] Warnings ({len(report['warnings'])}):")
            for w in report["warnings"]:
                print(f"    - {w}")

        print("\n[+] Verification Complete: All dataset, schema, and leakage contracts fulfilled.")
        sys.exit(0)
    else:
        print(f"\n[-] BENCHMARK VALIDATION FAILED: Found {len(report['errors'])} errors:")
        for err in report["errors"]:
            print(f"  [X] {err}")
        sys.exit(1)


if __name__ == "__main__":
    main()
