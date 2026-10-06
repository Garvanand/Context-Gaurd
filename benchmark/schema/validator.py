"""
EARB Benchmark Schema Validator.

Validates EARB dataset JSON files against the Pydantic schema, ensuring structural
integrity, value bounds, taxonomy consistency, and academic benchmark rigor.
"""

import json
import sys
from pathlib import Path
from typing import Dict, Any, List, Tuple, Optional
from pydantic import ValidationError

from benchmark.schema.earb_schema import EARBPair, EARBDataset


def validate_earb_pair(data: Dict[str, Any]) -> Tuple[bool, Optional[EARBPair], List[str]]:
    """
    Validate a single dictionary representing an EARB pair.
    Returns (is_valid, parsed_pair_or_none, error_messages).
    """
    try:
        pair = EARBPair(**data)
        return True, pair, []
    except ValidationError as e:
        errors = [f"{err['loc']}: {err['msg']}" for err in e.errors()]
        return False, None, errors
    except Exception as ex:
        return False, None, [str(ex)]


def validate_earb_file(file_path: Path) -> Tuple[bool, Optional[EARBDataset], List[str]]:
    """
    Validate an entire JSON file containing an EARB dataset or a list of pairs.
    """
    if not file_path.exists():
        return False, None, [f"File not found: {file_path}"]

    try:
        with open(file_path, "r", encoding="utf-8") as f:
            content = json.load(f)
    except Exception as e:
        return False, None, [f"JSON parsing error: {e}"]

    # Support either top-level {"version": ..., "pairs": [...]} or direct list of pairs
    if isinstance(content, list):
        dataset_dict = {"version": "1.0.0", "description": file_path.stem, "pairs": content}
    elif isinstance(content, dict) and "pairs" in content:
        dataset_dict = content
    else:
        return False, None, ["Input JSON must be an EARBDataset object with 'pairs' or a list of EARBPair objects"]

    try:
        dataset = EARBDataset(**dataset_dict)
        return True, dataset, []
    except ValidationError as e:
        errors = [f"Pair index {err['loc']}: {err['msg']}" for err in e.errors()]
        return False, None, errors


def cli_main():
    """Command-line validation entry point."""
    if len(sys.argv) < 2:
        print("Usage: python -m benchmark.schema.validator <path_to_earb_json>")
        sys.exit(1)

    target_path = Path(sys.argv[1])
    is_valid, dataset, errors = validate_earb_file(target_path)

    if is_valid and dataset is not None:
        print(f"VALIDATION SUCCESS: {target_path.name}")
        print(f"Total evaluated pairs: {dataset.total_count}")
        print(f"Category breakdown: {dataset.category_counts}")
        sys.exit(0)
    else:
        print(f"VALIDATION FAILED: {target_path.name}")
        print(f"Found {len(errors)} validation errors:")
        for err in errors[:20]:
            print(f"  - {err}")
        if len(errors) > 20:
            print(f"  ... and {len(errors) - 20} more errors.")
        sys.exit(1)


if __name__ == "__main__":
    cli_main()
