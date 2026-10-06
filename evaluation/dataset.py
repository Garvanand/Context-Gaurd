"""
Dataset Loader for Everyday Action Risk Benchmark (EARB).

Loads and partitions benchmark pairs from benchmark/data/earb_v1.jsonl.
Strictly respects base artifact clustering and split separation (dev vs test).
"""

import json
from pathlib import Path
from typing import List, Dict, Any, Optional

from benchmark.schema.earb_schema import EARBPair
from evaluation.perception import PerceptionService

DEFAULT_EARB_DATASET_PATH = Path("benchmark/data/earb_v1.jsonl")


class BenchmarkDatasetLoader:
    """
    Loads and serves EARB evaluation pairs with associated perceptual data.
    """

    def __init__(self, dataset_path: Optional[Path] = None):
        self.dataset_path = dataset_path or DEFAULT_EARB_DATASET_PATH

    def load_pairs(self, split: str = "all") -> List[EARBPair]:
        """
        Loads validated EARB pairs filtered by split ('all', 'dev', 'test').
        """
        if not self.dataset_path.exists():
            raise FileNotFoundError(f"EARB dataset not found at: {self.dataset_path.resolve()}")

        pairs: List[EARBPair] = []
        with open(self.dataset_path, "r", encoding="utf-8") as f:
            for line_num, line in enumerate(f, 1):
                clean = line.strip()
                if not clean:
                    continue
                try:
                    data = json.loads(clean)
                    pair = EARBPair(**data)
                    if split == "all" or pair.split.lower() == split.lower():
                        pairs.append(pair)
                except Exception as e:
                    raise ValueError(f"Corrupt EARB record at line {line_num}: {e}")

        return pairs

    def load_with_perception(self, split: str = "all") -> List[Dict[str, Any]]:
        """
        Loads pairs enriched with on-disk artifact perception metadata.
        """
        pairs = self.load_pairs(split=split)
        enriched: List[Dict[str, Any]] = []

        for p in pairs:
            perc = PerceptionService.get_perception_for_artifact(p.base_artifact_id)
            enriched.append({
                "pair": p,
                "perception": perc,
            })

        return enriched

    def get_base_artifact_clusters(self, split: str = "all") -> Dict[str, List[EARBPair]]:
        """
        Groups pairs by base_artifact_id for clustered statistical bootstrapping.
        """
        pairs = self.load_pairs(split=split)
        clusters: Dict[str, List[EARBPair]] = {}
        for p in pairs:
            clusters.setdefault(p.base_artifact_id, []).append(p)
        return clusters
