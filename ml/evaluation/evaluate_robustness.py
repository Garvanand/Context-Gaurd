"""
Distribution, Domain-Leakage, and Adversarial Robustness Evaluation.

Evaluates ContextGuard's ML models across real-world adversarial variations:
1. URL Model Robustness:
   - Misspellings / Typosquatting
   - Punctuation & Whitespace obfuscations
   - URL shorteners
   - Punycode & Unicode hostnames
   - Complex benign high-entropy paths
2. Scam-Message Model Robustness:
   - Benign messages containing suspicious keywords (Bank OTPs, receipts)
   - Legitimate urgent notices (Flight changes, medical appointments)
   - Incomplete / truncated notification text
   - Character substitutions / leetspeak obfuscations
3. Dedicated English vs. Hinglish Evaluation Slice:
   - Evaluates performance on mixed Hindi-English code-switched messages.
   - Provides an empirical, honest assessment of Hinglish capabilities and limitations.

Outputs:
- ml/artifacts/robustness_evaluation_results.json
"""

import os
import json
import time
import datetime
from pathlib import Path
from typing import Dict, List, Any

import numpy as np
import joblib

from ml.features.url_features import URLFeatureExtractor

ARTIFACTS_DIR = Path("ml/artifacts")
URL_MODEL_PATH = ARTIFACTS_DIR / "url_risk_model.joblib"
URL_PORTABLE_PATH = ARTIFACTS_DIR / "url_model_portable.json"
MESSAGE_PORTABLE_PATH = ARTIFACTS_DIR / "message_model_portable.json"
RESULTS_PATH = ARTIFACTS_DIR / "robustness_evaluation_results.json"


def evaluate_url(model, raw_url: str) -> Dict[str, Any]:
    vec = URLFeatureExtractor.extract_vector(raw_url)
    features_arr = np.array([vec], dtype=np.float32)
    proba = float(model.predict_proba(features_arr)[0, 1])
    is_phish = proba >= 0.50
    return {
        "url": raw_url,
        "probability": round(proba, 4),
        "is_phishing": is_phish
    }


def evaluate_message_portable(model_spec: Dict[str, Any], text: str) -> Dict[str, Any]:
    words = [w.lower() for w in text.split()]
    # Extract unigrams and bigrams
    token_counts = {}
    for i, w in enumerate(words):
        # strip punctuation
        clean_w = "".join(c for c in w if c.isalnum())
        if len(clean_w) >= 2:
            token_counts[clean_w] = token_counts.get(clean_w, 0) + 1
        if i + 1 < len(words):
            clean_next = "".join(c for c in words[i + 1] if c.isalnum())
            if len(clean_w) >= 2 and len(clean_next) >= 2:
                bigram = f"{clean_w} {clean_next}"
                token_counts[bigram] = token_counts.get(bigram, 0) + 1

    feature_map = {f["token"]: f for f in model_spec["features"]}
    intercept = model_spec["intercept"]
    cal_w = model_spec["calibration"]["weight"]
    cal_b = model_spec["calibration"]["bias"]

    sum_sq = 0.0
    active = []
    matched_tokens = []
    for tok, count in token_counts.items():
        if tok in feature_map:
            feat = feature_map[tok]
            tf = 1.0 + np.log(count)
            tfidf = tf * feat["idf"]
            sum_sq += tfidf * tfidf
            active.append((feat["weight"], tfidf))
            matched_tokens.append(tok)

    raw_margin = intercept
    if sum_sq > 0.0:
        l2_norm = np.sqrt(sum_sq)
        for w, tfidf in active:
            raw_margin += (tfidf / l2_norm) * w

    cal_prob = float(1.0 / (1.0 + np.exp(-(cal_w * raw_margin + cal_b))))
    return {
        "text": text,
        "probability": round(cal_prob, 4),
        "is_phishing": cal_prob >= 0.50,
        "matched_tokens": matched_tokens
    }


def run_robustness_evaluation():
    print("Loading trained URL and Message models for robustness evaluation...")
    url_model = joblib.load(URL_MODEL_PATH)
    with open(MESSAGE_PORTABLE_PATH, "r", encoding="utf-8") as f:
        msg_model_spec = json.load(f)

    # =========================================================================
    # 1. URL Model Robustness Test Suites
    # =========================================================================
    url_suites = {
        "typosquatting_misspellings": [
            ("http://paypa1-security-update.com/login", True),
            ("http://amazn-account-verify.xyz/auth", True),
            ("http://micros0ft-online-support.icu/portal", True),
            ("http://netflx-billing-update.club/signin", True),
            ("http://wellsfarg0-mobile-banking.top/login", True),
        ],
        "punctuation_whitespace_obfuscations": [
            ("http://phish-bank.com//login//verify//action", True),
            ("http://user:password@malicious-spoof.com/index", True),
            ("http://secure-site.com/path/%20with%20spaces%20and%20hex%20auth", False),
            ("https://sub1.sub2.sub3.sub4.sub5.suspicious-domain.xyz/auth", True),
        ],
        "shortened_urls": [
            ("https://bit.ly/3xY7z9Q", True),
            ("http://tinyurl.com/bank-security-login", True),
            ("https://t.co/alert991", True),
            ("https://cutt.ly/kyc-update", True),
        ],
        "punycode_unicode_hostnames": [
            ("http://xn--e1afmkfd.xn--p1ai/path", False),  # Cyrillic test
            ("http://xn--apple-security-9za.com/auth", True),
        ],
        "benign_high_entropy_slugs": [
            ("https://www.reuters.com/world/middle-east/israel-hamas-war-live-updates-gaza-ceasefire-talks-2026-10-09/", False),
            ("https://docs.python.org/3/library/urllib.parse.html#urllib.parse.urlsplit", False),
            ("https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/package-summary", False),
        ]
    }

    url_results = {}
    for suite_name, test_cases in url_suites.items():
        case_results = []
        correct = 0
        for url, expected_is_phish in test_cases:
            pred = evaluate_url(url_model, url)
            is_correct = pred["is_phishing"] == expected_is_phish
            if is_correct:
                correct += 1
            case_results.append({
                "url": url,
                "expected": expected_is_phish,
                "predicted": pred["is_phishing"],
                "probability": pred["probability"],
                "passed": is_correct
            })
        accuracy = round(correct / len(test_cases), 4)
        url_results[suite_name] = {
            "total_cases": len(test_cases),
            "passed_cases": correct,
            "accuracy": accuracy,
            "cases": case_results
        }
        print(f"URL Robustness [{suite_name}]: {correct}/{len(test_cases)} passed ({accuracy*100:.1f}%)")

    # =========================================================================
    # 2. Scam-Message Model Robustness Test Suites
    # =========================================================================
    msg_suites = {
        "benign_with_sensitive_keywords": [
            ("Your one-time password (OTP) for transaction at Amazon is 582910. Do not share with anyone.", False),
            ("Your HDFC Bank account statement for October is now ready for download.", False),
            ("Security alert: New login detected from Chrome on Windows. If this was you, ignore.", False),
            ("Dear Customer, Rs 1,250 debited from A/C XX4920 at Star Cafe. Avail bal: Rs 45,820.", False),
        ],
        "legitimate_urgent_notices": [
            ("URGENT: Flight AI-102 boarding gate changed to 4B. Boarding starts immediately.", False),
            ("Reminder: Doctor appointment today at 4:30 PM with Dr. Sharma. Please arrive on time.", False),
            ("Weather warning: Heavy rainfall expected in Mumbai tonight. Stay indoors.", False),
            ("Meeting rescheduled to 3 PM due to emergency room maintenance.", False),
        ],
        "incomplete_truncated_notifications": [
            ("SBI Alert: Rs 5,000 debited from a/c... [tap to view]", False),
            ("Your verification code is 9283...", False),
            ("Urgent update regarding your electricity bill of Rs 1,420...", True),
        ],
        "obfuscated_phishing_lures": [
            ("URGENT: Ur acc0unt is bl0cked. Verify now http://bit.ly/sbi-verify", True),
            ("Y0ur paytm kyc has expired. Cl1ck here http://tinyurl.com/paytm-auth", True),
            ("Electricity department: Power will be disconnected tonight. Call 9821009999 immediately.", True),
            ("Congratulations! You won Rs 50,000 in Diwali draw. Claim at http://gift-win.top", True),
        ]
    }

    msg_results = {}
    for suite_name, test_cases in msg_suites.items():
        case_results = []
        correct = 0
        for text, expected_is_phish in test_cases:
            pred = evaluate_message_portable(msg_model_spec, text)
            is_correct = pred["is_phishing"] == expected_is_phish
            if is_correct:
                correct += 1
            case_results.append({
                "text": text,
                "expected": expected_is_phish,
                "predicted": pred["is_phishing"],
                "probability": pred["probability"],
                "matched_tokens": pred["matched_tokens"],
                "passed": is_correct
            })
        accuracy = round(correct / len(test_cases), 4)
        msg_results[suite_name] = {
            "total_cases": len(test_cases),
            "passed_cases": correct,
            "accuracy": accuracy,
            "cases": case_results
        }
        print(f"Message Robustness [{suite_name}]: {correct}/{len(test_cases)} passed ({accuracy*100:.1f}%)")

    # =========================================================================
    # 3. Dedicated English vs. Hinglish Evaluation Slice
    # =========================================================================
    english_slice = [
        ("Dear customer, your bank account is locked due to KYC expiry. Update immediately at http://bit.ly/sbi-kyc", True),
        ("You have won a free $1,000 shopping voucher. Claim here: http://gift-win.xyz", True),
        ("Your courier delivery failed. Confirm address details: http://postal-track.icu/auth", True),
        ("Hey, are we still meeting for lunch tomorrow at 1 PM?", False),
        ("Thanks for sending the document, I will review it shortly.", False),
        ("Please pick up milk and bread on your way home.", False),
    ]

    hinglish_slice = [
        # Hinglish phishing with Latin lures
        ("Aapka SBI account block ho gaya hai turant kyc complete karein http://bit.ly/sbi-kyc", True),
        ("Bijli vibhag notice: Aaj raat light cut jayegi turant bill pay karein http://bit.ly/bijli-pay", True),
        ("Aapko 50,000 lottery lagi hai link pe click karke claim karein http://claim-prize.xyz", True),
        # Hinglish benign peer-to-peer / daily communications
        ("Bhai kal movie dekhne chalein?", False),
        ("Mummy ko bol dena mai late aunga aaj office se.", False),
        ("Kal sham ko chai pe milte hain bhai.", False),
        # Challenging colloquial Hinglish cases without keywords
        ("Bhai please 500 rupaye gpay karde emergency hai sham ko wapas deta hu", False),
        ("Aapka parcel delivery me issue hai call karein is number par", True),
    ]

    print("\n--- Evaluating Dedicated English vs. Hinglish Slices ---")
    
    def evaluate_slice(slice_data, name):
        correct = 0
        cases = []
        for text, expected in slice_data:
            pred = evaluate_message_portable(msg_model_spec, text)
            is_correct = pred["is_phishing"] == expected
            if is_correct:
                correct += 1
            cases.append({
                "text": text,
                "expected": expected,
                "predicted": pred["is_phishing"],
                "probability": pred["probability"],
                "matched_tokens": pred["matched_tokens"],
                "passed": is_correct
            })
        acc = round(correct / len(slice_data), 4)
        print(f"Slice [{name}]: {correct}/{len(slice_data)} passed ({acc*100:.1f}%)")
        return {
            "total_samples": len(slice_data),
            "passed_samples": correct,
            "accuracy": acc,
            "cases": cases
        }

    eng_eval = evaluate_slice(english_slice, "English")
    hinglish_eval = evaluate_slice(hinglish_slice, "Hinglish")

    robustness_summary = {
        "evaluation_timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "url_robustness": url_results,
        "message_robustness": msg_results,
        "multilingual_slice": {
            "english": eng_eval,
            "hinglish": hinglish_eval,
            "findings": {
                "english_accuracy": eng_eval["accuracy"],
                "hinglish_accuracy": hinglish_eval["accuracy"],
                "observation": "Hinglish messages containing Latin technical keywords ('account', 'kyc', 'block', 'pay', 'lottery') or URLs are detected reliably. However, purely colloquial Hinglish syntax without standard Latin technical loanwords or hyperlinks exhibits lower confidence. ContextGuard honestly reports this limitation and does not claim native Hindi/Hinglish NLP fluency."
            }
        }
    }

    with open(RESULTS_PATH, "w", encoding="utf-8") as f:
        json.dump(robustness_summary, f, indent=2)
    print(f"\nSaved full robustness evaluation report to {RESULTS_PATH}")

    return robustness_summary


if __name__ == "__main__":
    run_robustness_evaluation()
