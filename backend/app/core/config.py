"""
Core application configuration management using pydantic-settings.
"""

from typing import List
from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    # General
    app_name: str = Field("ContextGuard", description="Application service name")
    app_version: str = Field("0.1.0", description="Semantic service version")
    environment: str = Field("development", description="Runtime environment: development, testing, production")
    debug: bool = Field(True, description="Enable debug logging and OpenAPI documentation")
    host: str = Field("0.0.0.0", description="Bind host address")
    port: int = Field(8000, description="Bind port")

    # Policy Parameters
    lambda_reversibility: float = Field(0.75, ge=0.0, le=2.0, description="Reversibility penalty multiplier")
    stop_threshold: float = Field(0.65, ge=0.0, le=1.0, description="Threshold for STOP intervention")
    ask_threshold: float = Field(0.35, ge=0.0, le=1.0, description="Threshold for ASK/WARN intervention")
    min_confidence_threshold: float = Field(0.70, ge=0.0, le=1.0, description="Minimum confidence for WARN over ASK")

    # Inference Providers
    ollama_base_url: str = Field("http://localhost:11434", description="Ollama API endpoint")
    ollama_model: str = Field("qwen2.5-vl:3b", description="Model tag for VLM inference")
    vlm_timeout_seconds: int = Field(15, ge=1, le=60, description="Timeout for VLM queries")

    # Models & Artifacts
    phishing_model_path: str = Field("ml/artifacts/url_risk_model.joblib", description="Path to trained URL risk model artifact")
    phishing_feature_extractor_path: str = Field("ml/artifacts/url_model_metadata.json", description="Path to model metadata")

    # Privacy & Safety
    storage_mode: str = Field("memory_only", description="Must be memory_only for zero raw persistence")
    allow_raw_eval_mode: bool = Field(False, description="Disables raw input evaluation in production")
    network_audit_enabled: bool = Field(True, description="Enables network telemetry audit logging")
    log_level: str = Field("INFO", description="Log level: DEBUG, INFO, WARNING, ERROR")

    # Relay & Connectivity
    relay_db_path: str = Field("data/relay.db", description="Path to SQLite persistence store for device relay")
    relay_pairing_code_expiry_seconds: int = Field(300, description="Pairing code expiry time in seconds")
    relay_device_offline_timeout_seconds: int = Field(30, description="Heartbeat timeout for marking device OFFLINE")
    relay_max_message_size_bytes: int = Field(65536, description="Max allowed WebSocket message size in bytes")

    # CORS
    allowed_origins: List[str] = Field(default_factory=lambda: ["*"], description="Allowed CORS origins")

    @field_validator("ask_threshold")
    @classmethod
    def ask_must_be_less_than_stop(cls, v: float, info) -> float:
        # Note: In Pydantic V2, info.data has previously validated fields
        stop = info.data.get("stop_threshold", 0.65)
        if v >= stop:
            raise ValueError(f"ask_threshold ({v}) must be strictly less than stop_threshold ({stop})")
        return v


settings = Settings()
