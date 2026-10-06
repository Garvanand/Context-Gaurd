"""
URL Phishing Risk Inference Service.

Directly imports the exact same URLFeatureExtractor used during training.
Loads the trained XGBoost model from ml/artifacts/url_risk_model.joblib.
"""

from pathlib import Path
from typing import Dict, Any, List, Optional
import joblib
import numpy as np

from ml.features.url_features import URLFeatureExtractor
from ml.inference.interfaces import URLRiskModel
from ml.inference.types import URLRiskOutput


DEFAULT_MODEL_PATH = Path("ml/artifacts/url_risk_model.joblib")
MODEL_IDENTIFIER = "xgboost-phiusiil-url-v1"


class URLRiskService(URLRiskModel):
    """
    Production inference service wrapping trained XGBoost URL classifier.
    """

    def __init__(self, model_path: Path = DEFAULT_MODEL_PATH):
        self.model_path = model_path
        self._model = None
        self._load_model()

    def _load_model(self):
        if self.model_path.exists():
            try:
                self._model = joblib.load(self.model_path)
            except Exception as e:
                self._model = None
                print(f"Warning: Failed to load model from {self.model_path}: {e}")
        else:
            self._model = None

    def is_ready(self) -> bool:
        return self._model is not None

    def predict(self, raw_url: str) -> Dict[str, Any]:
        """
        Input: raw URL string.
        Output:
        {
          "is_phishing": boolean,
          "probability": float,
          "confidence": float,
          "risk_level": string,
          "model": "xgboost-phiusiil-url-v1",
          "features_used": [...]
        }
        """
        # Extract features using exact same extractor as training
        features_dict = URLFeatureExtractor.extract(raw_url)
        feature_names = URLFeatureExtractor.FEATURE_NAMES
        feature_vector = np.array([[features_dict[name] for name in feature_names]], dtype=np.float32)

        if self._model is not None:
            try:
                probs = self._model.predict_proba(feature_vector)[0]
                # Label 1 is phishing
                phish_prob = float(probs[1])
            except Exception:
                # Fallback to deterministic heuristic if model scoring errors
                phish_prob = 0.50
        else:
            # Safe heuristic fallback when model artifact is missing
            suspicious_kw = features_dict.get("suspicious_keyword_count", 0.0)
            is_ip = features_dict.get("is_ip_host", 0.0)
            is_susp_tld = features_dict.get("is_suspicious_tld", 0.0)
            score = (suspicious_kw * 0.25) + (is_ip * 0.40) + (is_susp_tld * 0.35)
            phish_prob = min(0.99, max(0.01, score))

        is_phishing = phish_prob >= 0.50
        # Confidence score in [0.5, 1.0] normalized to [0.0, 1.0] distance from decision boundary
        confidence = round(float(abs(phish_prob - 0.50) * 2.0), 4)

        if phish_prob >= 0.85:
            risk_level = "CRITICAL"
        elif phish_prob >= 0.65:
            risk_level = "HIGH"
        elif phish_prob >= 0.35:
            risk_level = "MODERATE"
        else:
            risk_level = "LOW"

        return {
            "is_phishing": is_phishing,
            "probability": round(phish_prob, 4),
            "confidence": confidence,
            "risk_level": risk_level,
            "model": MODEL_IDENTIFIER,
            "features_used": feature_names,
        }

    def predict_risk(self, url: str) -> URLRiskOutput:
        """
        Implementation of the abstract ML interface URLRiskModel.
        """
        res = self.predict(url)
        feats = URLFeatureExtractor.extract(url)
        return URLRiskOutput(
            url=url,
            risk_score=res["probability"],
            confidence=res["confidence"],
            is_phishing=res["is_phishing"],
            extracted_features=feats,
            model_version=MODEL_IDENTIFIER,
        )


# Singleton instance
_service_instance: Optional[URLRiskService] = None


def get_url_risk_service() -> URLRiskService:
    global _service_instance
    if _service_instance is None:
        _service_instance = URLRiskService()
    return _service_instance


def predict_url_risk(raw_url: str) -> Dict[str, Any]:
    """Top-level helper function for direct invocation."""
    service = get_url_risk_service()
    return service.predict(raw_url)
