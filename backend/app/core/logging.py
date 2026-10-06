"""
Structured logging module for ContextGuard backend.
Outputs JSON logs in production, and clean readable logs in development.
"""

import logging
import json
import sys
from datetime import datetime, timezone
from typing import Any, Dict


class StructuredJsonFormatter(logging.Formatter):
    """
    JSON log formatter producing machine-readable structured log events.
    """
    def format(self, record: logging.LogRecord) -> str:
        log_data: Dict[str, Any] = {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
            "module": record.module,
            "line": record.lineno,
        }

        # Include custom extra fields if provided
        if hasattr(record, "artifact_hash"):
            log_data["artifact_hash"] = record.artifact_hash
        if hasattr(record, "intervention"):
            log_data["intervention"] = record.intervention
        if hasattr(record, "latency_ms"):
            log_data["latency_ms"] = record.latency_ms
        if record.exc_info:
            log_data["exception"] = self.formatException(record.exc_info)

        return json.dumps(log_data)


def setup_logging(log_level: str = "INFO", json_format: bool = False) -> logging.Logger:
    """
    Configure the root logger and return a logger instance for ContextGuard.
    """
    logger = logging.getLogger("contextguard")
    logger.setLevel(getattr(logging, log_level.upper(), logging.INFO))
    logger.handlers.clear()

    handler = logging.StreamHandler(sys.stdout)
    if json_format:
        handler.setFormatter(StructuredJsonFormatter())
    else:
        handler.setFormatter(
            logging.Formatter(
                "[%(asctime)s] [%(levelname)s] [%(name)s]: %(message)s",
                datefmt="%Y-%m-%d %H:%M:%S",
            )
        )

    logger.addHandler(handler)
    logger.propagate = False
    return logger


logger = setup_logging()
