"""
Everyday Action Risk Benchmark (EARB) - Interactive Annotation CLI Tool.

Allows researchers to:
- Preview base artifacts and existing pairs
- Interactively create or edit artifact-action pairs
- Select severity, reversibility, intervention
- Add evidence snippets and academic rationales
- Mark negative controls and ambiguous edge cases
- Validate immediately against Pydantic EARBPair schema
- Append or export validated JSONL files
"""

import json
import sys
from pathlib import Path
from typing import List, Optional

# Ensure project root is in sys.path
sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from benchmark.schema.earb_schema import (
    EARBPair,
    CategoryEnum,
    SeverityEnum,
    ReversibilityEnum,
    InterventionEnum,
)


def list_existing_artifacts() -> List[Path]:
    artifact_dir = Path("benchmark/artifacts")
    if not artifact_dir.exists():
        return []
    return sorted(list(artifact_dir.glob("*.png")) + list(artifact_dir.glob("*.jpg")))


def interactive_annotate():
    print("=" * 65)
    print("  CONTEXTGUARD: EARB INTERACTIVE ANNOTATION CLI TOOL")
    print("=" * 65)

    artifacts = list_existing_artifacts()
    if not artifacts:
        print("No artifacts found in benchmark/artifacts! Please generate them first.")
        return

    print("\nAvailable Synthetic Base Artifacts:")
    for i, p in enumerate(artifacts, 1):
        print(f"  [{i:2d}] {p.name}")

    print("\nChoose an option:")
    print("  [1] Annotate a new artifact-action pair")
    print("  [2] Inspect an existing artifact's pairs from benchmark/data/earb_v1.jsonl")
    print("  [3] Export benchmark summary statistics")
    print("  [4] Exit")

    choice = input("\nEnter choice [1-4]: ").strip()

    if choice == "1":
        create_new_annotation(artifacts)
    elif choice == "2":
        inspect_existing_pairs()
    elif choice == "3":
        show_benchmark_stats()
    else:
        print("Exiting annotator.")


def create_new_annotation(artifacts: List[Path]):
    print("\n--- NEW ANNOTATION ENTRY ---")
    art_idx = input(f"Select artifact index (1-{len(artifacts)}): ").strip()
    try:
        selected_art = artifacts[int(art_idx) - 1]
    except Exception:
        print("Invalid selection.")
        return

    print(f"\nSelected Artifact: {selected_art.name}")
    pair_id = input("Enter unique Pair ID (e.g. EARB-CUSTOM-001): ").strip()
    base_id = input("Enter Base Artifact ID (e.g. ART-CUS-001): ").strip()

    print("\nCategories: [1] Financial [2] Digital Security [3] Privacy Disclosure [4] Communication")
    cat_sel = input("Select category [1-4]: ").strip()
    cat_map = {
        "1": CategoryEnum.FINANCIAL,
        "2": CategoryEnum.DIGITAL_SECURITY,
        "3": CategoryEnum.PRIVACY_DISCLOSURE,
        "4": CategoryEnum.COMMUNICATION,
    }
    category = cat_map.get(cat_sel, CategoryEnum.FINANCIAL)

    context = input("Enter Context description: ").strip()
    source_app = input("Enter Source App (e.g. Chrome, WhatsApp): ").strip()
    recipient = input("Enter Recipient (e.g. Unknown Contact, Self): ").strip()
    destination = input("Enter Destination (e.g. Public Feed, Drive): ").strip()
    intended_action = input("Enter Intended Action (SAVE, SEND, POST, etc.): ").strip()
    risk_type = input("Enter High-level Risk Type: ").strip()
    risk_subtype = input("Enter Risk Subtype: ").strip()

    print("\nSeverity: [1] negligible [2] minor [3] moderate [4] severe [5] critical")
    sev_sel = input("Select Severity [1-5]: ").strip()
    sev_map = {
        "1": SeverityEnum.NEGLIGIBLE,
        "2": SeverityEnum.MINOR,
        "3": SeverityEnum.MODERATE,
        "4": SeverityEnum.SEVERE,
        "5": SeverityEnum.CRITICAL,
    }
    severity = sev_map.get(sev_sel, SeverityEnum.MODERATE)

    print("\nReversibility: [1] fully [2] partially [3] irreversible")
    rev_sel = input("Select Reversibility [1-3]: ").strip()
    rev_map = {
        "1": ReversibilityEnum.FULLY,
        "2": ReversibilityEnum.PARTIALLY,
        "3": ReversibilityEnum.IRREVERSIBLE,
    }
    reversibility = rev_map.get(rev_sel, ReversibilityEnum.PARTIALLY)

    print("\nExpected Intervention: [1] ACT [2] ASK [3] WARN [4] STOP")
    int_sel = input("Select Intervention [1-4]: ").strip()
    int_map = {
        "1": InterventionEnum.ACT,
        "2": InterventionEnum.ASK,
        "3": InterventionEnum.WARN,
        "4": InterventionEnum.STOP,
    }
    intervention = int_map.get(int_sel, InterventionEnum.WARN)

    is_neg = input("Is this a Negative Control (benign safe task)? (y/n): ").strip().lower() == "y"
    is_amb = input("Is this an Ambiguous case (uncertainty requiring ASK)? (y/n): ").strip().lower() == "y"

    evidence_raw = input("Enter comma-separated Evidence snippets: ").strip()
    evidence = [e.strip() for e in evidence_raw.split(",") if e.strip()]

    alt_action = input("Enter Acceptable Alternative action (or press enter for none): ").strip() or None
    rationale = input("Enter Academic Rationale for intervention: ").strip()

    # Build dictionary
    candidate = {
        "pair_id": pair_id,
        "base_artifact_id": base_id,
        "artifact_type": "image",
        "artifact_path": str(selected_art).replace("\\", "/"),
        "category": category,
        "context": context,
        "source_app": source_app,
        "recipient": recipient,
        "destination": destination,
        "intended_action": intended_action,
        "risk_type": risk_type,
        "risk_subtype": risk_subtype,
        "severity": severity,
        "reversibility": reversibility,
        "evidence": evidence,
        "uncertainty": 0.65 if is_amb else 0.05,
        "expected_intervention": intervention,
        "acceptable_alternative": alt_action,
        "is_negative_control": is_neg,
        "is_ambiguous": is_amb,
        "split": "dev",
        "rationale": rationale,
    }

    try:
        validated = EARBPair(**candidate)
        print("\n[SUCCESS] Pair validated cleanly against Pydantic schema!")
        print(f"Calculated Severity Score: {validated.severity_score}")
        print(f"Calculated Reversibility Score: {validated.reversibility_score}")

        save_confirm = input("Save pair to benchmark/data/earb_custom.jsonl? (y/n): ").strip().lower()
        if save_confirm == "y":
            out_file = Path("benchmark/data/earb_custom.jsonl")
            with open(out_file, "a", encoding="utf-8") as f:
                dump = validated.model_dump()
                dump["category"] = validated.category.value
                dump["severity"] = validated.severity.value
                dump["reversibility"] = validated.reversibility.value
                dump["expected_intervention"] = validated.expected_intervention.value
                f.write(json.dumps(dump, ensure_ascii=False) + "\n")
            print(f"Saved to {out_file} successfully.")
    except Exception as e:
        print(f"\n[VALIDATION FAILED] Schema error: {e}")


def inspect_existing_pairs():
    jsonl_path = Path("benchmark/data/earb_v1.jsonl")
    if not jsonl_path.exists():
        print(f"File not found: {jsonl_path}")
        return

    with open(jsonl_path, "r", encoding="utf-8") as f:
        lines = [json.loads(line) for line in f if line.strip()]

    print(f"\nTotal existing benchmark pairs: {len(lines)}")
    query = input("Enter search term (e.g. bank, OTP, STOP, ART-FIN-001): ").strip().lower()

    matches = [
        p for p in lines
        if query in p["pair_id"].lower()
        or query in p["base_artifact_id"].lower()
        or query in p["intended_action"].lower()
        or query in p["expected_intervention"].lower()
        or query in p["context"].lower()
    ]

    print(f"Found {len(matches)} matching pairs:\n")
    for m in matches[:10]:
        print(f"  [{m['pair_id']}] {m['base_artifact_id']} | Action: {m['intended_action']} -> {m['expected_intervention']}")
        print(f"     Context: {m['context']}")
        print(f"     Rationale: {m['rationale']}")
        print("  " + "-" * 60)


def show_benchmark_stats():
    jsonl_path = Path("benchmark/data/earb_v1.jsonl")
    if not jsonl_path.exists():
        print(f"File not found: {jsonl_path}")
        return

    with open(jsonl_path, "r", encoding="utf-8") as f:
        pairs = [json.loads(line) for line in f if line.strip()]

    categories = {}
    interventions = {}
    splits = {}
    base_arts = set()
    neg_controls = 0
    ambiguous = 0

    for p in pairs:
        base_arts.add(p["base_artifact_id"])
        categories[p["category"]] = categories.get(p["category"], 0) + 1
        interventions[p["expected_intervention"]] = interventions.get(p["expected_intervention"], 0) + 1
        splits[p.get("split", "unknown")] = splits.get(p.get("split", "unknown"), 0) + 1
        if p.get("is_negative_control"):
            neg_controls += 1
        if p.get("is_ambiguous"):
            ambiguous += 1

    print("\n" + "=" * 50)
    print("  EARB BENCHMARK SUITE SUMMARY TELEMETRY")
    print("=" * 50)
    print(f"Total Base Artifacts:         {len(base_arts)}")
    print(f"Total Action-Conditioned Pairs: {len(pairs)}")
    print(f"Negative Controls (ACT only): {neg_controls}")
    print(f"Ambiguous Cases (ASK):        {ambiguous}")
    print("\nIntervention Breakdown:")
    for k, v in interventions.items():
        print(f"  {k:5s}: {v:2d} pairs ({v / len(pairs) * 100:.1f}%)")
    print("\nCategory Breakdown:")
    for k, v in categories.items():
        print(f"  {k:20s}: {v:2d} pairs")
    print("\nSplit Breakdown:")
    for k, v in splits.items():
        print(f"  {k:10s}: {v:2d} pairs")
    print("=" * 50)


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--stats":
        show_benchmark_stats()
    else:
        interactive_annotate()
