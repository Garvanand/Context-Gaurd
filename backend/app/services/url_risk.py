from backend.services.url_risk import (
    URLRiskService,
    predict_url_risk,
    get_url_risk_service,
    DEFAULT_MODEL_PATH,
    MODEL_IDENTIFIER,
)

__all__ = [
    "URLRiskService",
    "predict_url_risk",
    "get_url_risk_service",
    "DEFAULT_MODEL_PATH",
    "MODEL_IDENTIFIER",
]
