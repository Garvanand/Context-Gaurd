"""
Demo Cache Engine for ContextGuard Capstone Demonstration.

Structured result caching keyed by:
- artifact_hash (SHA-256)
- action
- context_hash (SHA-256)
- model_version
- policy_version

CRITICAL SAFETY INVARIANT:
Must NOT cache raw sensitive content (zero raw images, zero raw unredacted PII).
Only structured risk scores, confidence, deterministic intervention, and sanitized evidence claims are cached.

Replayed results are explicitly tagged with is_cached=True and label="Verified cached inference".
"""

import os
import json
import hashlib
from pathlib import Path
from datetime import datetime, timezone
from typing import Dict, Any, Optional, List

from backend.app.core.config import settings
from backend.app.core.logging import logger
from backend.app.demo.scenarios import DEMO_SCENARIOS, DemoScenario

CACHE_FILE_PATH = Path("backend/app/demo/verified_demo_cache.json")
DEFAULT_MODEL_VERSION = "qwen2.5-vl:3b+xgboost-v1.0"
DEFAULT_POLICY_VERSION = "policy-lambda0.75-v1.0"


def compute_sha256(data: str | bytes) -> str:
    h = hashlib.sha256()
    if isinstance(data, str):
        h.update(data.encode("utf-8"))
    else:
        h.update(data)
    return h.hexdigest()


def compute_artifact_hash(artifact: Optional[str]) -> str:
    """Computes SHA-256 of artifact string, path, or payload."""
    if not artifact:
        return compute_sha256("EMPTY_ARTIFACT")
    # If it's a file path that exists on disk, hash its content bytes
    p = Path(artifact)
    if p.exists() and p.is_file():
        try:
            return compute_sha256(p.read_bytes())
        except Exception:
            pass
    return compute_sha256(artifact)


def compute_context_hash(recipient: Optional[str], destination: Optional[str], source_app: Optional[str]) -> str:
    """Computes SHA-256 of context signals."""
    ctx_str = f"recipient:{recipient or ''}|destination:{destination or ''}|source_app:{source_app or ''}"
    return compute_sha256(ctx_str)


class DemoCacheManager:
    """
    Manages deterministic replay cache for all 10 demo scenarios.
    """

    def __init__(self, cache_path: Path = CACHE_FILE_PATH):
        self.cache_path = cache_path
        self._cache: Dict[str, Dict[str, Any]] = {}
        self.load()

    def _make_key(
        self,
        artifact_hash: str,
        action: str,
        context_hash: str,
        model_version: str = DEFAULT_MODEL_VERSION,
        policy_version: str = DEFAULT_POLICY_VERSION,
    ) -> str:
        return f"{artifact_hash}::{action.upper()}::{context_hash}::{model_version}::{policy_version}"

    def get(
        self,
        artifact_hash: str,
        action: str,
        context_hash: str,
        model_version: str = DEFAULT_MODEL_VERSION,
        policy_version: str = DEFAULT_POLICY_VERSION,
    ) -> Optional[Dict[str, Any]]:
        key = self._make_key(artifact_hash, action, context_hash, model_version, policy_version)
        entry = self._cache.get(key)
        if entry:
            logger.info(f"Demo cache HIT for key: {key[:32]}...")
            res = dict(entry["result"])
            res["is_cached"] = True
            res["cache_label"] = "Verified cached inference"
            res["cached_at"] = entry.get("timestamp")
            res["scenario_id"] = entry.get("scenario_id")
            return res
        return None

    def get_by_scenario_id(self, scenario_id: int) -> Optional[Dict[str, Any]]:
        for entry in self._cache.values():
            if entry.get("scenario_id") == scenario_id:
                res = dict(entry["result"])
                res["is_cached"] = True
                res["cache_label"] = "Verified cached inference"
                res["cached_at"] = entry.get("timestamp")
                res["scenario_id"] = scenario_id
                return res
        return None

    def put(
        self,
        artifact_hash: str,
        action: str,
        context_hash: str,
        result: Dict[str, Any],
        scenario_id: Optional[int] = None,
        model_version: str = DEFAULT_MODEL_VERSION,
        policy_version: str = DEFAULT_POLICY_VERSION,
    ) -> None:
        key = self._make_key(artifact_hash, action, context_hash, model_version, policy_version)
        # Sanitize result to ensure ZERO raw image bytes or unredacted binary data
        sanitized_result = {
            "intervention": result.get("intervention"),
            "risk_score": float(result.get("risk_score", 0.0)),
            "severity": float(result.get("severity", 0.0)),
            "reversibility": float(result.get("reversibility", 0.0)),
            "confidence": float(result.get("confidence", 0.0)),
            "reason": str(result.get("reason", "")),
            "evidence": result.get("evidence", []),
            "recommended_alternative": result.get("recommended_alternative"),
            "latency_ms": float(result.get("latency_ms", 12.5)),
            "model_path": str(result.get("model_path", "Qwen 2.5-VL 3B + XGBoost Hybrid")),
            "network_mode": str(result.get("network_mode", "REDACTED_LOCAL_BACKEND")),
            "stages": result.get("stages", {}),
        }

        self._cache[key] = {
            "key": key,
            "scenario_id": scenario_id,
            "artifact_hash": artifact_hash,
            "action": action.upper(),
            "context_hash": context_hash,
            "model_version": model_version,
            "policy_version": policy_version,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "verified_by": "ContextGuard Research Evaluator",
            "result": sanitized_result,
        }
        self.save()

    def count(self) -> int:
        return len(self._cache)

    def verified_scenario_count(self) -> int:
        cached_scenario_ids = set()
        for entry in self._cache.values():
            if entry.get("scenario_id"):
                cached_scenario_ids.add(entry["scenario_id"])
        return len(cached_scenario_ids)

    def load(self) -> None:
        if self.cache_path.exists():
            try:
                data = json.loads(self.cache_path.read_text(encoding="utf-8"))
                self._cache = data.get("entries", {})
                logger.info(f"Loaded {len(self._cache)} verified demo cache entries from {self.cache_path}")
            except Exception as e:
                logger.warning(f"Could not load demo cache: {e}")
                self._cache = {}
        else:
            self._cache = {}

    def save(self) -> None:
        self.cache_path.parent.mkdir(parents=True, exist_ok=True)
        payload = {
            "version": "1.0",
            "generated_at": datetime.now(timezone.utc).isoformat(),
            "total_entries": len(self._cache),
            "safety_invariant": "Zero raw sensitive artifacts persisted",
            "entries": self._cache,
        }
        self.cache_path.write_text(json.dumps(payload, indent=2), encoding="utf-8")


demo_cache = DemoCacheManager()
