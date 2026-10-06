"""
Health and telemetry endpoints for ContextGuard backend.
"""

import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Any
from fastapi import APIRouter
import httpx

from backend.app.core.config import settings

router = APIRouter(tags=["Health & Diagnostics"])

_START_TIME = time.time()


@router.get("/health", summary="Basic System Health")
async def get_health() -> Dict[str, Any]:
    """
    Returns overall system operational status, uptime, and operating environment.
    """
    uptime_seconds = time.time() - _START_TIME
    return {
        "status": "healthy",
        "app_name": settings.app_name,
        "environment": settings.environment,
        "uptime_seconds": round(uptime_seconds, 2),
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "storage_mode": settings.storage_mode,
    }


@router.get("/health/models", summary="ML Subsystem Health Status")
async def get_models_health() -> Dict[str, Any]:
    """
    Returns granular status of the 3 AI/ML sublayers and policy engine:
    1. ML Kit (On-device perception contract)
    2. XGBoost Phishing URL Classifier
    3. Multimodal Vision-Language Reasoner (Qwen2.5-VL-3B / Ollama)
    4. Deterministic Policy Engine
    """
    # 1. XGBoost URL model presence check
    phishing_model_path = Path(settings.phishing_model_path)
    phishing_model_ready = phishing_model_path.exists()

    # 2. Ollama / VLM reachability probe
    ollama_reachable = False
    ollama_message = "Not checked"
    try:
        async with httpx.AsyncClient(timeout=1.5) as client:
            resp = await client.get(f"{settings.ollama_base_url}/api/tags")
            if resp.status_code == 200:
                ollama_reachable = True
                ollama_message = "Ollama endpoint active"
            else:
                ollama_message = f"HTTP {resp.status_code}"
    except Exception as e:
        ollama_message = f"Ollama offline ({type(e).__name__}); fallback enabled"

    return {
        "status": "operational",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "layers": {
            "ml_kit_perception": {
                "type": "on_device_edge",
                "ocr": "ML Kit Text Recognition v2",
                "face_detection": "ML Kit Face Detection",
                "redaction_layer": "In-Memory Client Canvas Masking",
                "status": "active_on_device",
            },
            "xgboost_url_model": {
                "type": "gradient_boosted_classifier",
                "dataset": "PhiUSIIL Phishing URL Dataset (UCI)",
                "feature_count": 35,
                "model_path": str(phishing_model_path),
                "is_loaded": phishing_model_ready,
                "status": "ready" if phishing_model_ready else "placeholder_pending_training",
            },
            "multimodal_vlm": {
                "type": "vision_language_reasoner",
                "model_tag": settings.ollama_model,
                "provider_url": settings.ollama_base_url,
                "is_reachable": ollama_reachable,
                "fallback_active": not ollama_reachable,
                "message": ollama_message,
            },
            "policy_engine": {
                "type": "deterministic_mathematical_gating",
                "formula": "rho = s * (1 + lambda * r)",
                "lambda": settings.lambda_reversibility,
                "stop_threshold": settings.stop_threshold,
                "ask_threshold": settings.ask_threshold,
                "min_confidence_threshold": settings.min_confidence_threshold,
                "model_failure_safety_rule": "Enforced (never silent ACT)",
                "status": "ready",
            },
        },
    }


@router.get("/health/version", summary="Version and Runtime Telemetry")
async def get_version() -> Dict[str, Any]:
    """
    Returns semantic version, Python runtime details, and framework dependencies.
    """
    return {
        "app_name": settings.app_name,
        "version": settings.app_version,
        "environment": settings.environment,
        "python_version": sys.version.split()[0],
        "platform": sys.platform,
        "debug": settings.debug,
    }
