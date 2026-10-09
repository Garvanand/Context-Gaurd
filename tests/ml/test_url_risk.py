"""
Unit tests for URL Feature Extraction and ML Phishing Risk Inference Service.

Includes 20+ deterministic unit test URLs covering:
- Legitimate HTTPS domains
- IP-based URLs
- Localhost development URLs
- Phishing indicators and suspicious TLDs
- Malformed and edge-case URLs
"""

import json
from pathlib import Path
import pytest

from ml.features.url_features import URLFeatureExtractor, extract_features
from backend.services.url_risk import URLRiskService, predict_url_risk, get_url_risk_service


# 25 Deterministic Test URLs across diverse threat and structural profiles
DETERMINISTIC_TEST_URLS = [
    # 1-5: Legitimate Mainstream HTTPS Domains
    ("https://www.google.com", False, "legitimate_standard"),
    ("https://en.wikipedia.org/wiki/Machine_learning", False, "legitimate_path"),
    ("https://github.com/torvalds/linux/commit/123456", False, "legitimate_deep_path"),
    ("https://docs.python.org/3/library/urllib.parse.html", False, "legitimate_subdomain"),
    ("https://developer.android.com/jetpack/compose", False, "legitimate_dev_portal"),

    # 6-8: Localhost & Loopback Addresses
    ("http://localhost:8000/api/v1/health", False, "localhost_named"),
    ("http://127.0.0.1:3000/dashboard", False, "localhost_ip4"),
    ("http://localhost:8080/swagger-ui/index.html", False, "localhost_custom_port"),

    # 9-11: Raw IP Host URLs
    ("http://192.168.1.1/router/login.html", None, "private_ip_router"),
    ("http://10.0.0.1/admin/config.php", None, "private_ip_admin"),
    ("http://185.220.101.5/drop/payload.exe", True, "public_ip_executable"),

    # 12-18: Suspicious & Phishing Keyword / TLD URLs
    ("http://paypa1-security-verification.xyz/login?user=victim&auth=token", True, "phishing_typo_tld"),
    ("http://secure-banking-alert.top/verify-account?client=10923", True, "phishing_banking_tld"),
    ("http://appleid-confirm-support.work/account/recover", True, "phishing_brand_tld"),
    ("http://netflix-billing-update.club/signin.php?id=99", True, "phishing_streaming_tld"),
    ("http://login-microsoft-online.icu/auth/oauth2", True, "phishing_cloud_tld"),
    ("http://amazon-prime-reward.gq/claim-now?code=win", True, "phishing_free_tld"),
    ("http://secure.wellsfargo.com.account-update.xyz/login", True, "phishing_subdomain_spoof"),

    # 19-25: Edge Case, Punycode, Percent-Encoded & Malformed URLs
    ("http://user:password@malicious-redirect.com/index", True, "userinfo_at_symbol"),
    ("http://xn--e1afmkfd.xn--p1ai/path", None, "punycode_cyrillic"),
    ("https://sub1.sub2.sub3.sub4.sub5.suspicious-domain.biz/verify", True, "deep_subdomain_chain"),
    ("https://legit-site.com/path/%20with%20spaces%20and%20hex", False, "percent_encoded_path"),
    ("google.com/search?q=contextguard", False, "missing_scheme"),
    ("https://example.com:8443/custom/port?query=1#fragment", False, "custom_port_and_fragment"),
    ("http://phish-site.com/login//double//slash//path", True, "double_slash_path"),
]


def test_twenty_deterministic_urls_count():
    """Ensure at least 20 deterministic URLs are tested."""
    assert len(DETERMINISTIC_TEST_URLS) >= 20


def test_feature_extraction_vector_length_and_types():
    """Every URL must extract exactly 37 finite float features (Schema v1.1.0)."""
    for url, _, _ in DETERMINISTIC_TEST_URLS:
        features = URLFeatureExtractor.extract(url)
        assert len(features) == 37
        assert len(URLFeatureExtractor.FEATURE_NAMES) == 37

        vector = URLFeatureExtractor.extract_vector(url)
        assert len(vector) == 37
        assert all(isinstance(v, (int, float)) for v in vector)
        assert all(not (v != v) for v in vector)  # Check not NaN


def test_malformed_urls_do_not_crash_extractor():
    """Malformed strings must yield safe deterministic features without exceptions."""
    malformed_cases = [
        "",
        "   ",
        "http://",
        "://incomplete",
        "https://",
        "javascript:alert(1)",
        "http:///no-host-path",
        "http://[::1]:8080/ipv6",
        "http://foo.bar?query=без-схемы&русский=текст",
        "A" * 2048,  # Extremely long URL
    ]
    for bad_url in malformed_cases:
        feats = URLFeatureExtractor.extract(bad_url)
        assert isinstance(feats, dict)
        assert len(feats) == 37
        vec = URLFeatureExtractor.extract_vector(bad_url)
        assert len(vec) == 37


def test_ip_host_detection():
    ip_url = "http://192.168.1.1/login"
    feats = URLFeatureExtractor.extract(ip_url)
    assert feats["is_ip_host"] == 1.0

    domain_url = "http://www.google.com/login"
    feats_domain = URLFeatureExtractor.extract(domain_url)
    assert feats_domain["is_ip_host"] == 0.0


def test_shortener_detection():
    assert URLFeatureExtractor.extract("https://bit.ly/3xY7z9Q")["is_shortened_url"] == 1.0
    assert URLFeatureExtractor.extract("http://tinyurl.com/bank-security")["is_shortened_url"] == 1.0
    assert URLFeatureExtractor.extract("https://reuters.com/news")["is_shortened_url"] == 0.0


def test_https_indicator():
    assert URLFeatureExtractor.extract("https://secure.com")["is_https"] == 1.0
    assert URLFeatureExtractor.extract("http://insecure.com")["is_https"] == 0.0


def test_suspicious_tld_indicator():
    assert URLFeatureExtractor.extract("http://test.xyz")["is_suspicious_tld"] == 1.0
    assert URLFeatureExtractor.extract("http://test.top")["is_suspicious_tld"] == 1.0
    assert URLFeatureExtractor.extract("http://test.com")["is_suspicious_tld"] == 0.0
    assert URLFeatureExtractor.extract("http://test.org")["is_suspicious_tld"] == 0.0


def test_model_artifact_and_metadata_exist():
    model_path = Path("ml/artifacts/url_risk_model.joblib")
    meta_path = Path("ml/artifacts/url_model_metadata.json")

    assert model_path.exists(), f"Model artifact must exist at {model_path}"
    assert meta_path.exists(), f"Metadata artifact must exist at {meta_path}"

    with open(meta_path, "r", encoding="utf-8") as f:
        meta = json.load(f)

    assert meta["model_name"] in ["XGBoost", "RandomForest", "LogisticRegression"]
    assert meta["feature_count"] == 37
    assert "experiments" in meta
    strat_test = meta["experiments"]["experiment_1_stratified_split"]["XGBoost"]
    assert strat_test["accuracy"] > 0.90
    assert strat_test["roc_auc"] > 0.90


def test_inference_service_output_schema():
    service = URLRiskService()
    assert service.is_ready() is True

    test_url = "https://www.google.com"
    output = service.predict(test_url)

    # Required output keys
    assert "is_phishing" in output
    assert "probability" in output
    assert "confidence" in output
    assert "risk_level" in output
    assert "model" in output
    assert "features_used" in output

    assert isinstance(output["is_phishing"], bool)
    assert isinstance(output["probability"], float)
    assert 0.0 <= output["probability"] <= 1.0
    assert isinstance(output["confidence"], float)
    assert 0.0 <= output["confidence"] <= 1.0
    assert output["risk_level"] in ["LOW", "MODERATE", "HIGH", "CRITICAL"]
    assert output["model"] == "xgboost-phiusiil-url-v1"
    assert len(output["features_used"]) == 37


def test_inference_classification_accuracy_on_known_urls():
    """Verify that known benign URLs score low and phishing URLs score high."""
    service = get_url_risk_service()

    benign_res = service.predict("https://www.google.com")
    assert benign_res["is_phishing"] is False
    assert benign_res["probability"] < 0.20
    assert benign_res["risk_level"] == "LOW"

    phishing_res = service.predict("http://paypa1-security-verification.xyz/login?user=victim")
    assert phishing_res["is_phishing"] is True
    assert phishing_res["probability"] > 0.80
    assert phishing_res["risk_level"] in ["HIGH", "CRITICAL"]
