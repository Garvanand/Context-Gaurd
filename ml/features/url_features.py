"""
Deterministic URL Lexical and Structural Feature Extractor (Schema v1.1.0).

Extracts reproducible features directly from raw URLs for training and on-device inference.
Strict feature parity is maintained across Python and Android Kotlin runtimes.
"""

import math
import re
from typing import Dict, List, Any
from urllib.parse import urlparse
import ipaddress

SCHEMA_VERSION = "1.1.0"

# Known high-risk phishing / abuse TLDs
SUSPICIOUS_TLDS = {
    "xyz", "top", "club", "work", "gq", "cf", "tk", "ml", "ga",
    "buzz", "icu", "fit", "rest", "kim", "info", "monster", "live",
    "surf", "cam", "bid", "racing", "win", "stream", "download"
}

# Known URL shortening services
SHORTENER_DOMAINS = {
    "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd",
    "buff.ly", "adf.ly", "bitly.com", "cutt.ly", "rb.gy", "shorturl.at",
    "tiny.cc", "lnkd.in", "t.ly", "cli.gs", "rebrand.ly", "s.id"
}

# Common targeted / deceptive keywords in phishing URLs
SUSPICIOUS_KEYWORDS = [
    "login", "signin", "verify", "verification", "account", "banking",
    "secure", "security", "update", "confirm", "confirmation", "wallet",
    "password", "credential", "support", "auth", "authenticate", "recover",
    "token", "billing", "invoice", "payment", "authenticate", "portal",
    "service", "customer", "admin", "webscr", "ebayisapi"
]

HEX_PATTERN = re.compile(r"%[0-9a-fA-F]{2}")
IP_V4_PATTERN = re.compile(r"^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$")


def calculate_entropy(text: str) -> float:
    """
    Computes Shannon entropy H = -sum(p * log2(p)) for character distribution.
    Deterministic to 4 decimal places.
    """
    if not text:
        return 0.0
    length = len(text)
    freq: Dict[str, int] = {}
    for char in text:
        freq[char] = freq.get(char, 0) + 1
    entropy = 0.0
    for count in freq.values():
        p = count / length
        entropy -= p * math.log2(p)
    return round(entropy, 4)


def max_consecutive_run(text: str, predicate) -> int:
    """
    Returns the maximum run length of consecutive characters matching predicate.
    """
    max_run = 0
    current_run = 0
    for char in text:
        if predicate(char):
            current_run += 1
            if current_run > max_run:
                max_run = current_run
        else:
            current_run = 0
    return max_run


def extract_registered_domain(hostname: str) -> str:
    """
    Extracts registered root domain for domain-grouping evaluation to prevent leakage.
    Handles standard and common ccTLD structures (e.g., .co.uk, .com.au).
    """
    if not hostname:
        return ""
    host = hostname.lower().strip()
    # Check IP
    try:
        ipaddress.ip_address(host)
        return host
    except ValueError:
        pass

    parts = host.split(".")
    if len(parts) <= 2:
        return host
    
    # Common 2-level TLD suffixes
    two_level_tlds = {"co.uk", "com.au", "co.in", "net.au", "org.uk", "gov.in", "ac.uk", "co.jp"}
    joined_last_two = f"{parts[-2]}.{parts[-1]}"
    if joined_last_two in two_level_tlds and len(parts) >= 3:
        return f"{parts[-3]}.{joined_last_two}"
    
    return f"{parts[-2]}.{parts[-1]}"


class URLFeatureExtractor:
    """
    Deterministic feature extractor producing 37 pre-navigation lexical/structural features.
    Schema Version: 1.1.0
    """

    SCHEMA_VERSION: str = SCHEMA_VERSION

    FEATURE_NAMES: List[str] = [
        "url_length",
        "hostname_length",
        "path_length",
        "query_length",
        "fragment_length",
        "count_dots",
        "count_hyphens",
        "count_underscores",
        "count_slashes",
        "count_question_marks",
        "count_equal_signs",
        "count_at",
        "count_ampersands",
        "count_percent",
        "count_digits",
        "count_special_chars",
        "num_subdomains",
        "num_path_segments",
        "is_https",
        "is_ip_host",
        "has_at_symbol",
        "has_percent_encoding",
        "has_punycode",
        "is_suspicious_tld",
        "suspicious_keyword_count",
        "shannon_entropy",
        "hostname_entropy",
        "digit_ratio",
        "symbol_ratio",
        "hostname_to_path_ratio",
        "max_consecutive_digits",
        "max_consecutive_special_chars",
        "has_double_slash_path",
        "has_port",
        "has_hex_char",
        "vowel_consonant_ratio",
        "is_shortened_url",
    ]

    @classmethod
    def extract(cls, raw_url: str) -> Dict[str, float]:
        """
        Extract deterministic features from a raw URL string.
        Safe against malformed strings, nulls, and unusual schemes.
        """
        if not raw_url or not isinstance(raw_url, str):
            url = ""
        else:
            url = raw_url.strip()

        # Normalize prefix if scheme is absent for proper urlparse
        parse_target = url
        if "://" not in parse_target and not parse_target.startswith("//"):
            parse_target = "http://" + parse_target

        try:
            parsed = urlparse(parse_target)
            hostname = parsed.hostname or ""
            path = parsed.path or ""
            query = parsed.query or ""
            fragment = parsed.fragment or ""
            port = parsed.port
            scheme = parsed.scheme.lower()
        except Exception:
            hostname = ""
            path = ""
            query = ""
            fragment = ""
            port = None
            scheme = ""

        url_len = len(url)
        host_len = len(hostname)
        path_len = len(path)
        query_len = len(query)
        fragment_len = len(fragment)

        count_dots = url.count(".")
        count_hyphens = url.count("-")
        count_underscores = url.count("_")
        count_slashes = url.count("/")
        count_question = url.count("?")
        count_equals = url.count("=")
        count_at = url.count("@")
        count_ampersands = url.count("&")
        count_percent = url.count("%")
        count_digits = sum(c.isdigit() for c in url)
        count_special = sum(not c.isalnum() for c in url)

        # Subdomain count
        host_parts = [p for p in hostname.split(".") if p]
        num_subdomains = max(0, len(host_parts) - 2) if len(host_parts) >= 2 else 0

        # Path segments
        path_segments = [p for p in path.split("/") if p]
        num_path_segments = len(path_segments)

        # Flags
        is_https = 1.0 if scheme == "https" else 0.0

        # IP host check
        is_ip_host = 0.0
        try:
            ipaddress.ip_address(hostname)
            is_ip_host = 1.0
        except ValueError:
            if IP_V4_PATTERN.match(hostname):
                is_ip_host = 1.0

        has_at_symbol = 1.0 if count_at > 0 else 0.0
        has_percent_encoding = 1.0 if count_percent > 0 else 0.0
        has_punycode = 1.0 if "xn--" in hostname.lower() else 0.0

        # TLD check
        tld = host_parts[-1].lower() if host_parts else ""
        is_suspicious_tld = 1.0 if tld in SUSPICIOUS_TLDS else 0.0

        # Suspicious keyword check
        url_lower = url.lower()
        keyword_count = sum(1.0 for kw in SUSPICIOUS_KEYWORDS if kw in url_lower)

        # Entropies
        shannon_entropy = calculate_entropy(url)
        hostname_entropy = calculate_entropy(hostname)

        # Ratios
        safe_url_len = max(1, url_len)
        digit_ratio = round(count_digits / safe_url_len, 4)
        symbol_ratio = round(count_special / safe_url_len, 4)
        hostname_to_path_ratio = round(host_len / (path_len + 1.0), 4)

        # Consecutive runs
        max_consec_digits = max_consecutive_run(url, lambda c: c.isdigit())
        max_consec_special = max_consecutive_run(url, lambda c: not c.isalnum())

        # Structural flags
        has_double_slash_path = 1.0 if "//" in path else 0.0
        has_port = 1.0 if (port is not None and port not in (80, 443)) else 0.0
        has_hex_char = 1.0 if bool(HEX_PATTERN.search(url)) else 0.0

        # Vowel to consonant ratio in hostname
        host_alpha = [c.lower() for c in hostname if c.isalpha()]
        vowels = sum(1 for c in host_alpha if c in "aeiou")
        consonants = len(host_alpha) - vowels
        vowel_consonant_ratio = round(vowels / max(1, consonants), 4)

        # Shortener check
        hostname_lower = hostname.lower()
        is_shortened = 1.0 if any(
            hostname_lower == s or hostname_lower.endswith("." + s)
            for s in SHORTENER_DOMAINS
        ) else 0.0

        return {
            "url_length": float(url_len),
            "hostname_length": float(host_len),
            "path_length": float(path_len),
            "query_length": float(query_len),
            "fragment_length": float(fragment_len),
            "count_dots": float(count_dots),
            "count_hyphens": float(count_hyphens),
            "count_underscores": float(count_underscores),
            "count_slashes": float(count_slashes),
            "count_question_marks": float(count_question),
            "count_equal_signs": float(count_equals),
            "count_at": float(count_at),
            "count_ampersands": float(count_ampersands),
            "count_percent": float(count_percent),
            "count_digits": float(count_digits),
            "count_special_chars": float(count_special),
            "num_subdomains": float(num_subdomains),
            "num_path_segments": float(num_path_segments),
            "is_https": float(is_https),
            "is_ip_host": float(is_ip_host),
            "has_at_symbol": float(has_at_symbol),
            "has_percent_encoding": float(has_percent_encoding),
            "has_punycode": float(has_punycode),
            "is_suspicious_tld": float(is_suspicious_tld),
            "suspicious_keyword_count": float(keyword_count),
            "shannon_entropy": float(shannon_entropy),
            "hostname_entropy": float(hostname_entropy),
            "digit_ratio": float(digit_ratio),
            "symbol_ratio": float(symbol_ratio),
            "hostname_to_path_ratio": float(hostname_to_path_ratio),
            "max_consecutive_digits": float(max_consec_digits),
            "max_consecutive_special_chars": float(max_consec_special),
            "has_double_slash_path": float(has_double_slash_path),
            "has_port": float(has_port),
            "has_hex_char": float(has_hex_char),
            "vowel_consonant_ratio": float(vowel_consonant_ratio),
            "is_shortened_url": float(is_shortened),
        }

    @classmethod
    def extract_vector(cls, raw_url: str) -> List[float]:
        """
        Returns feature vector strictly ordered by FEATURE_NAMES.
        """
        feats = cls.extract(raw_url)
        return [feats[name] for name in cls.FEATURE_NAMES]


def extract_features(raw_url: str) -> Dict[str, float]:
    """Helper alias for single URL extraction."""
    return URLFeatureExtractor.extract(raw_url)
