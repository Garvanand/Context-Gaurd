"""
Tests for backend health and version endpoints.
"""

from fastapi.testclient import TestClient
from backend.app.main import app

client = TestClient(app)


def test_root_endpoint():
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert data["system"] == "ContextGuard"
    assert data["status"] == "operational"


def test_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["app_name"] == "ContextGuard"
    assert "uptime_seconds" in data
    assert "timestamp" in data
    assert data["storage_mode"] == "memory_only"


def test_health_models_endpoint():
    response = client.get("/health/models")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "operational"
    assert "layers" in data
    layers = data["layers"]
    assert "ml_kit_perception" in layers
    assert "xgboost_url_model" in layers
    assert "multimodal_vlm" in layers
    assert "policy_engine" in layers
    assert layers["policy_engine"]["formula"] == "rho = s * (1 + lambda * r)"


def test_health_version_endpoint():
    response = client.get("/health/version")
    assert response.status_code == 200
    data = response.json()
    assert data["app_name"] == "ContextGuard"
    assert data["version"] == "0.1.0"
    assert "python_version" in data


def test_v1_prefixed_health_endpoints():
    res_v1_health = client.get("/api/v1/health")
    assert res_v1_health.status_code == 200

    res_v1_models = client.get("/api/v1/health/models")
    assert res_v1_models.status_code == 200

    res_v1_version = client.get("/api/v1/health/version")
    assert res_v1_version.status_code == 200
