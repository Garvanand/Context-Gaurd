"""
Reproducible URL Phishing Risk Detection Pipeline.
Audited & Upgraded for ContextGuard On-Device & Server Serving.

Dataset: PhiUSIIL Phishing URL Dataset (UCI ML Repository ID: 967)
Features: 37 Deterministic Pre-Navigation Lexical and Structural Features (Schema v1.1.0)
Strict Pre-Navigation Invariant: Zero post-navigation HTML/DOM/redirect properties used.

Models Compared:
1. Logistic Regression (StandardScaler + L2 Regularization)
2. Random Forest Classifier
3. XGBoost Classifier

Evaluations:
- Standard Stratified Split (Leakage susceptibility benchmark)
- Registered-Domain Grouped Split (Zero cross-split domain leakage)
- Platt Scaling Probability Calibration (fitted strictly on Dev split)
- Recall at Fixed False Positive Rates (FPR = 0.1%, 0.5%, 1.0%)
- Expected Calibration Error (ECE) and Brier Score
- Inference Latency and Model Storage Size

Artifact Exports:
- ml/artifacts/url_risk_model.joblib (Python server runtime)
- ml/artifacts/url_model_portable.json (Documented JSON tree ensemble for on-device Android Kotlin interpreter)
- ml/artifacts/url_model_metadata.json (Auditable training metrics & provenance)
- ml/artifacts/url_golden_corpus.json (Deterministic Python-Kotlin parity test corpus)
- Synchronized copy to android/app/src/main/assets/models/
"""

import os
import sys
import json
import time
import shutil
import datetime
from pathlib import Path
from typing import Dict, List, Any, Tuple

import joblib
import numpy as np
import pandas as pd
import sklearn
import xgboost as xgb
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    roc_auc_score,
    average_precision_score,
    confusion_matrix,
    brier_score_loss,
    roc_curve,
)
from sklearn.model_selection import train_test_split, GroupShuffleSplit
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

from ml.datasets.acquire_phiusiil import download_dataset
from ml.features.url_features import (
    URLFeatureExtractor,
    extract_registered_domain,
    calculate_entropy,
)

RANDOM_SEED = 42
ARTIFACTS_DIR = Path("ml/artifacts")
ANDROID_ASSETS_MODELS_DIR = Path("android/app/src/main/assets/models")


def compute_ece(y_true: np.ndarray, y_prob: np.ndarray, n_bins: int = 10) -> float:
    """Computes Expected Calibration Error (ECE) across uniform confidence bins."""
    bin_edges = np.linspace(0.0, 1.0, n_bins + 1)
    ece = 0.0
    n = len(y_true)
    for i in range(n_bins):
        mask = (y_prob >= bin_edges[i]) & (y_prob < bin_edges[i + 1] if i < n_bins - 1 else y_prob <= bin_edges[i + 1])
        bin_count = np.sum(mask)
        if bin_count > 0:
            bin_acc = np.mean(y_true[mask])
            bin_conf = np.mean(y_prob[mask])
            ece += (bin_count / n) * abs(bin_acc - bin_conf)
    return round(float(ece), 4)


def recall_at_fpr(y_true: np.ndarray, y_scores: np.ndarray, target_fpr: float) -> float:
    """Calculates phishing recall (TPR) at an exact maximum false positive rate (FPR)."""
    fpr, tpr, _ = roc_curve(y_true, y_scores)
    valid_idx = np.where(fpr <= target_fpr)[0]
    if len(valid_idx) == 0:
        return 0.0
    return round(float(tpr[valid_idx[-1]]), 4)


def convert_xgb_to_portable_format(
    model: xgb.XGBClassifier,
    calibrator: LogisticRegression,
    feature_names: List[str],
    metrics_summary: Dict[str, Any]
) -> Dict[str, Any]:
    """
    Exports XGBoost booster to a self-contained, lightweight JSON tree format
    with calibrated Platt scaling sigmoid parameters.
    """
    booster = model.get_booster()
    tree_dumps = [json.loads(t) for t in booster.get_dump(dump_format="json")]
    
    # Extract trees
    parsed_trees = []
    for tree_idx, raw_tree in enumerate(tree_dumps):
        nodes_list = []
        
        def traverse(node):
            node_id = int(node["nodeid"])
            if "leaf" in node:
                nodes_list.append({
                    "node_id": node_id,
                    "is_leaf": True,
                    "leaf_value": round(float(node["leaf"]), 6)
                })
            else:
                f_idx = int(node["split"].replace("f", ""))
                cond = round(float(node["split_condition"]), 6)
                yes_id = int(node["yes"])
                no_id = int(node["no"])
                nodes_list.append({
                    "node_id": node_id,
                    "is_leaf": False,
                    "feature_idx": f_idx,
                    "feature_name": feature_names[f_idx],
                    "threshold": cond,
                    "left_child": yes_id,
                    "right_child": no_id
                })
                for child in node.get("children", []):
                    traverse(child)
                    
        traverse(raw_tree)
        # Sort nodes by node_id
        nodes_list.sort(key=lambda n: n["node_id"])
        parsed_trees.append({
            "tree_id": tree_idx,
            "nodes": nodes_list
        })

    # Calibrator parameters
    cal_w = float(calibrator.coef_[0][0])
    cal_b = float(calibrator.intercept_[0])

    return {
        "model_type": "xgboost_tree_ensemble",
        "schema_version": URLFeatureExtractor.SCHEMA_VERSION,
        "feature_count": len(feature_names),
        "feature_names": feature_names,
        "num_trees": len(parsed_trees),
        "base_score": 0.5,
        "base_margin": 0.0,
        "calibration": {
            "method": "platt_scaling_sigmoid",
            "weight": round(cal_w, 6),
            "bias": round(cal_b, 6),
            "formula": "1.0 / (1.0 + exp(-(weight * raw_margin + bias)))"
        },
        "performance": metrics_summary,
        "trees": parsed_trees
    }


def load_dataset_and_extract_features(sample_size: int = 80000) -> Tuple[pd.DataFrame, np.ndarray, np.ndarray, np.ndarray, int]:
    """
    Acquires PhiUSIIL dataset, cleans nulls, derives registered domain,
    and extracts 37 pre-navigation features.
    """
    csv_path = download_dataset()
    print(f"Loading PhiUSIIL Phishing URL Dataset: {csv_path}")

    # Inspect raw columns and label
    df = pd.read_csv(csv_path, usecols=["URL", "Domain", "label"])
    initial_len = len(df)
    df = df.dropna(subset=["URL", "label"]).reset_index(drop=True)
    
    # Check duplicate URLs
    duplicate_count = int(df.duplicated(subset=["URL"]).sum())
    print(f"Total rows: {initial_len} | Dropped nulls: {initial_len - len(df)} | Duplicate URLs: {duplicate_count}")

    # PhiUSIIL label convention: 0 = phishing, 1 = legitimate
    # Project contract requires: 1 = phishing, 0 = legitimate
    df["target"] = (df["label"] == 0).astype(int)
    df["reg_domain"] = df["Domain"].astype(str).apply(extract_registered_domain)

    phish_total = (df["target"] == 1).sum()
    legit_total = (df["target"] == 0).sum()
    print(f"Class distribution: Phishing={phish_total} ({phish_total/len(df)*100:.1f}%), Legitimate={legit_total} ({legit_total/len(df)*100:.1f}%)")

    if sample_size and sample_size < len(df):
        print(f"Stratified sampling of {sample_size} records (seed={RANDOM_SEED})...")
        sampled_indices, _ = train_test_split(
            df.index,
            train_size=sample_size,
            stratify=df["target"],
            random_state=RANDOM_SEED
        )
        df_sample = df.loc[sampled_indices].reset_index(drop=True)
    else:
        df_sample = df.reset_index(drop=True)

    print(f"Extracting {len(URLFeatureExtractor.FEATURE_NAMES)} deterministic pre-navigation features for {len(df_sample)} URLs...")
    t0 = time.time()
    urls = df_sample["URL"].tolist()
    X = np.array([URLFeatureExtractor.extract_vector(u) for u in urls], dtype=np.float32)
    y = df_sample["target"].to_numpy(dtype=np.int32)
    groups = df_sample["reg_domain"].to_numpy()

    elapsed = time.time() - t0
    print(f"Feature extraction done in {elapsed:.2f}s ({len(urls)/elapsed:.1f} URLs/sec). Shape: {X.shape}")

    return df_sample, X, y, groups, duplicate_count


def evaluate_candidate(
    model,
    X_eval: np.ndarray,
    y_eval: np.ndarray,
    calibrator: LogisticRegression = None
) -> Dict[str, Any]:
    """Evaluates comprehensive metrics including AUCs, calibrated ECE, Brier, and recall@FPR."""
    t0 = time.time()
    
    if hasattr(model, "predict_proba"):
        raw_prob = model.predict_proba(X_eval)[:, 1]
    elif hasattr(model, "decision_function"):
        scores = model.decision_function(X_eval)
        raw_prob = 1.0 / (1.0 + np.exp(-scores))
    else:
        raw_prob = model.predict(X_eval).astype(float)

    # Raw margin for tree models or calibrated evaluation
    if isinstance(model, xgb.XGBClassifier):
        dmat = xgb.DMatrix(X_eval)
        raw_margins = model.get_booster().predict(dmat, output_margin=True)
    else:
        # Convert prob to logit margin
        eps = 1e-6
        clipped = np.clip(raw_prob, eps, 1.0 - eps)
        raw_margins = np.log(clipped / (1.0 - clipped))

    # Calibrated probability
    if calibrator is not None:
        cal_prob = calibrator.predict_proba(raw_margins.reshape(-1, 1))[:, 1]
    else:
        cal_prob = raw_prob

    latency_ms = (time.time() - t0) * 1000.0 / len(X_eval)
    y_pred = (cal_prob >= 0.50).astype(int)

    acc = float(accuracy_score(y_eval, y_pred))
    prec = float(precision_score(y_eval, y_pred, zero_division=0))
    rec = float(recall_score(y_eval, y_pred, zero_division=0))
    f1 = float(f1_score(y_eval, y_pred, zero_division=0))
    roc_auc = float(roc_auc_score(y_eval, cal_prob))
    pr_auc = float(average_precision_score(y_eval, cal_prob))

    cm = confusion_matrix(y_eval, y_pred).tolist()
    tn, fp, fn, tp = int(cm[0][0]), int(cm[0][1]), int(cm[1][0]), int(cm[1][1])

    brier = float(brier_score_loss(y_eval, cal_prob))
    ece = compute_ece(y_eval, cal_prob)

    rec_fpr_001 = recall_at_fpr(y_eval, cal_prob, 0.001)
    rec_fpr_005 = recall_at_fpr(y_eval, cal_prob, 0.005)
    rec_fpr_010 = recall_at_fpr(y_eval, cal_prob, 0.010)

    return {
        "accuracy": round(acc, 4),
        "precision": round(prec, 4),
        "recall": round(rec, 4),
        "f1": round(f1, 4),
        "roc_auc": round(roc_auc, 4),
        "pr_auc": round(pr_auc, 4),
        "recall_at_fpr_0.1%": rec_fpr_001,
        "recall_at_fpr_0.5%": rec_fpr_005,
        "recall_at_fpr_1.0%": rec_fpr_010,
        "brier_score": round(brier, 4),
        "expected_calibration_error": ece,
        "confusion_matrix": {"tn": tn, "fp": fp, "fn": fn, "tp": tp},
        "latency_ms_per_sample": round(latency_ms, 4)
    }


def fit_calibrator_on_dev(model, X_dev: np.ndarray, y_dev: np.ndarray) -> LogisticRegression:
    """Fits Platt scaling (univariate logistic regression) on dev margins."""
    if isinstance(model, xgb.XGBClassifier):
        dmat = xgb.DMatrix(X_dev)
        margins = model.get_booster().predict(dmat, output_margin=True)
    elif hasattr(model, "predict_proba"):
        prob = model.predict_proba(X_dev)[:, 1]
        eps = 1e-6
        clipped = np.clip(prob, eps, 1.0 - eps)
        margins = np.log(clipped / (1.0 - clipped))
    else:
        margins = model.decision_function(X_dev)

    calibrator = LogisticRegression(C=1.0, solver="lbfgs", max_iter=1000)
    calibrator.fit(margins.reshape(-1, 1), y_dev)
    return calibrator


def run_pipeline(sample_size: int = 80000):
    ARTIFACTS_DIR.mkdir(parents=True, exist_ok=True)
    ANDROID_ASSETS_MODELS_DIR.mkdir(parents=True, exist_ok=True)
    start_time_iso = datetime.datetime.now(datetime.timezone.utc).isoformat()

    df, X, y, groups, duplicate_count = load_dataset_and_extract_features(sample_size=sample_size)
    feature_names = URLFeatureExtractor.FEATURE_NAMES

    # =========================================================================
    # EXPERIMENT 1: Standard Stratified Split (Random)
    # =========================================================================
    print("\n" + "=" * 70)
    print("EXPERIMENT 1: STANDARD STRATIFIED SPLIT (70% Train / 15% Dev / 15% Test)")
    print("=" * 70)

    X_train_r, X_temp_r, y_train_r, y_temp_r = train_test_split(
        X, y, test_size=0.30, random_state=RANDOM_SEED, stratify=y
    )
    X_dev_r, X_test_r, y_dev_r, y_test_r = train_test_split(
        X_temp_r, y_temp_r, test_size=0.50, random_state=RANDOM_SEED, stratify=y_temp_r
    )

    models_def = {
        "LogisticRegression": lambda: Pipeline([
            ("scaler", StandardScaler()),
            ("clf", LogisticRegression(max_iter=1000, random_state=RANDOM_SEED))
        ]),
        "RandomForest": lambda: RandomForestClassifier(
            n_estimators=100,
            max_depth=16,
            min_samples_split=5,
            n_jobs=-1,
            random_state=RANDOM_SEED
        ),
        "XGBoost": lambda: xgb.XGBClassifier(
            n_estimators=120,
            max_depth=6,
            learning_rate=0.1,
            subsample=0.8,
            colsample_bytree=0.8,
            eval_metric="logloss",
            base_score=0.5,
            random_state=RANDOM_SEED,
            n_jobs=-1
        )
    }

    stratified_results = {}
    fitted_models_r = {}
    calibrators_r = {}

    for name, model_factory in models_def.items():
        print(f"\n[Stratified] Training {name}...")
        clf = model_factory()
        t0 = time.time()
        clf.fit(X_train_r, y_train_r)
        fit_time = time.time() - t0

        calibrator = fit_calibrator_on_dev(clf, X_dev_r, y_dev_r)
        fitted_models_r[name] = clf
        calibrators_r[name] = calibrator

        # Evaluate on Test set
        metrics = evaluate_candidate(clf, X_test_r, y_test_r, calibrator)
        metrics["training_time_s"] = round(fit_time, 2)
        stratified_results[name] = metrics
        print(f"  -> {name} | ROC-AUC: {metrics['roc_auc']:.4f} | PR-AUC: {metrics['pr_auc']:.4f} | F1: {metrics['f1']:.4f} | ECE: {metrics['expected_calibration_error']:.4f} | Rec@0.1%FPR: {metrics['recall_at_fpr_0.1%']:.4f}")

    # =========================================================================
    # EXPERIMENT 2: Domain-Grouped Split (Leakage-Controlled)
    # =========================================================================
    print("\n" + "=" * 70)
    print("EXPERIMENT 2: DOMAIN-GROUPED SPLIT (Zero Domain Leakage)")
    print("=" * 70)

    gss = GroupShuffleSplit(n_splits=1, test_size=0.30, random_state=RANDOM_SEED)
    train_idx_g, temp_idx_g = next(gss.split(X, y, groups=groups))

    X_train_g, y_train_g = X[train_idx_g], y[train_idx_g]
    X_temp_g, y_temp_g, groups_temp = X[temp_idx_g], y[temp_idx_g], groups[temp_idx_g]

    gss_sub = GroupShuffleSplit(n_splits=1, test_size=0.50, random_state=RANDOM_SEED)
    dev_idx_g, test_idx_g = next(gss_sub.split(X_temp_g, y_temp_g, groups=groups_temp))

    X_dev_g, y_dev_g = X_temp_g[dev_idx_g], y_temp_g[dev_idx_g]
    X_test_g, y_test_g = X_temp_g[test_idx_g], y_temp_g[test_idx_g]

    print(f"Domain-grouped split sizes: Train={len(X_train_g)}, Dev={len(X_dev_g)}, Test={len(X_test_g)}")

    grouped_results = {}
    for name, model_factory in models_def.items():
        print(f"\n[Domain-Grouped] Training {name}...")
        clf = model_factory()
        t0 = time.time()
        clf.fit(X_train_g, y_train_g)
        fit_time = time.time() - t0

        calibrator = fit_calibrator_on_dev(clf, X_dev_g, y_dev_g)
        metrics = evaluate_candidate(clf, X_test_g, y_test_g, calibrator)
        metrics["training_time_s"] = round(fit_time, 2)
        grouped_results[name] = metrics
        print(f"  -> {name} | Grouped ROC-AUC: {metrics['roc_auc']:.4f} | PR-AUC: {metrics['pr_auc']:.4f} | F1: {metrics['f1']:.4f} | ECE: {metrics['expected_calibration_error']:.4f} | Rec@0.1%FPR: {metrics['recall_at_fpr_0.1%']:.4f}")

    # =========================================================================
    # MODEL SELECTION & PORTABLE EXPORT
    # =========================================================================
    champion_name = "XGBoost"
    champion_model = fitted_models_r[champion_name]
    champion_calibrator = calibrators_r[champion_name]
    champion_metrics = stratified_results[champion_name]

    print("\n" + "=" * 70)
    print(f"EXPORTING CHAMPION: {champion_name}")
    print("=" * 70)

    # 1. Save standard python joblib model for backend
    joblib_path = ARTIFACTS_DIR / "url_risk_model.joblib"
    joblib.dump(champion_model, joblib_path)
    joblib_size_kb = round(os.path.getsize(joblib_path) / 1024, 2)
    print(f"Saved Python model artifact: {joblib_path} ({joblib_size_kb} KB)")

    # 2. Export portable JSON tree model for Kotlin tree interpreter
    portable_payload = convert_xgb_to_portable_format(
        champion_model,
        champion_calibrator,
        feature_names,
        {
            "stratified_test": stratified_results[champion_name],
            "domain_grouped_test": grouped_results[champion_name]
        }
    )
    portable_path = ARTIFACTS_DIR / "url_model_portable.json"
    with open(portable_path, "w", encoding="utf-8") as f:
        json.dump(portable_payload, f, indent=2)
    portable_size_kb = round(os.path.getsize(portable_path) / 1024, 2)
    print(f"Saved Portable Tree Model: {portable_path} ({portable_size_kb} KB)")

    # Copy portable model to Android assets directory
    android_model_dest = ANDROID_ASSETS_MODELS_DIR / "url_model_portable.json"
    shutil.copyfile(portable_path, android_model_dest)
    print(f"Synchronized portable model to Android assets: {android_model_dest}")

    # 3. Create Shared Golden Corpus for Python-Kotlin Deterministic Parity Testing
    golden_test_urls = [
        # Benign mainstream
        "https://www.google.com",
        "https://en.wikipedia.org/wiki/Machine_learning",
        "https://github.com/torvalds/linux/commit/123456",
        "https://docs.python.org/3/library/urllib.parse.html",
        "https://developer.android.com/jetpack/compose",
        "https://reuters.com/world/india",
        "https://bbc.com/news",
        "https://microsoft.com/en-us",
        "https://apple.com/iphone",
        "https://amazon.in/dp/B08N5WRWNW",
        # Localhost & Ports
        "http://localhost:8000/api/v1/health",
        "http://127.0.0.1:3000/dashboard",
        "http://localhost:8080/swagger-ui/index.html",
        # IP literals & Malicious paths
        "http://192.168.1.1/router/login.html",
        "http://10.0.0.1/admin/config.php",
        "http://185.220.101.5/drop/payload.exe",
        "http://194.26.29.112/secure/login",
        # Phishing keywords & suspicious TLDs
        "http://paypa1-security-verification.xyz/login?user=victim&auth=token",
        "http://secure-banking-alert.top/verify-account?client=10923",
        "http://appleid-confirm-support.work/account/recover",
        "http://netflix-billing-update.club/signin.php?id=99",
        "http://login-microsoft-online.icu/auth/oauth2",
        "http://amazon-prime-reward.gq/claim-now?code=win",
        "http://secure.wellsfargo.com.account-update.xyz/login",
        # Shorteners
        "https://bit.ly/3xY7z9Q",
        "http://tinyurl.com/bank-security-login",
        "https://t.co/alert991",
        # Punycode & Hex obfuscation
        "http://xn--e1afmkfd.xn--p1ai/path",
        "https://legit-site.com/path/%20with%20spaces%20and%20hex",
        "http://user:password@malicious-redirect.com/index",
        "http://phish-site.com/login//double//slash//path",
        "google.com/search?q=contextguard"
    ]

    print(f"\nGenerating Shared Golden Corpus ({len(golden_test_urls)} URLs)...")
    golden_corpus = []
    dmat_golden = xgb.DMatrix(np.array([URLFeatureExtractor.extract_vector(u) for u in golden_test_urls], dtype=np.float32))
    raw_margins_golden = champion_model.get_booster().predict(dmat_golden, output_margin=True)
    cal_probs_golden = champion_calibrator.predict_proba(raw_margins_golden.reshape(-1, 1))[:, 1]

    for idx, u in enumerate(golden_test_urls):
        feat_dict = URLFeatureExtractor.extract(u)
        feat_vec = [feat_dict[fn] for fn in feature_names]
        margin = float(raw_margins_golden[idx])
        cal_prob = float(cal_probs_golden[idx])
        is_phish = bool(cal_prob >= 0.50)
        golden_corpus.append({
            "id": idx + 1,
            "url": u,
            "feature_vector": [round(f, 6) for f in feat_vec],
            "raw_margin": round(margin, 6),
            "calibrated_probability": round(cal_prob, 6),
            "is_phishing": is_phish
        })

    golden_path = ARTIFACTS_DIR / "url_golden_corpus.json"
    with open(golden_path, "w", encoding="utf-8") as f:
        json.dump(golden_corpus, f, indent=2)
    print(f"Saved Golden Corpus: {golden_path}")

    android_golden_dest = ANDROID_ASSETS_MODELS_DIR / "url_golden_corpus.json"
    shutil.copyfile(golden_path, android_golden_dest)
    print(f"Synchronized Golden Corpus to Android assets: {android_golden_dest}")

    # 4. Save Complete Metadata Payload
    metadata = {
        "model_name": champion_name,
        "model_version": "xgboost-phiusiil-url-v1.1",
        "schema_version": URLFeatureExtractor.SCHEMA_VERSION,
        "dataset": {
            "name": "PhiUSIIL Phishing URL Dataset",
            "source": "UCI Machine Learning Repository ID: 967",
            "license": "Creative Commons Attribution 4.0 International (CC BY 4.0)",
            "total_sampled_records": len(df),
            "duplicate_urls_detected": int(duplicate_count),
            "label_encoding": {"legitimate": 0, "phishing": 1}
        },
        "feature_list": feature_names,
        "feature_count": len(feature_names),
        "split_summary": {
            "stratified": {
                "train_samples": len(X_train_r),
                "dev_samples": len(X_dev_r),
                "test_samples": len(X_test_r)
            },
            "domain_grouped": {
                "train_samples": len(X_train_g),
                "dev_samples": len(X_dev_g),
                "test_samples": len(X_test_g),
                "leakage_mitigation": "GroupShuffleSplit on registered root domain"
            }
        },
        "calibration": {
            "technique": "Platt Scaling (Univariate Sigmoid Logistic Regression)",
            "split": "Development/Validation Split Only",
            "parameters": {
                "weight": round(float(champion_calibrator.coef_[0][0]), 6),
                "bias": round(float(champion_calibrator.intercept_[0]), 6)
            }
        },
        "experiments": {
            "experiment_1_stratified_split": stratified_results,
            "experiment_2_domain_grouped_split": grouped_results
        },
        "model_sizes_kb": {
            "joblib_binary": joblib_size_kb,
            "portable_json": portable_size_kb
        },
        "training_timestamp": start_time_iso,
        "environment": {
            "python_version": sys.version.split()[0],
            "xgboost_version": xgb.__version__,
            "scikit_learn_version": sklearn.__version__,
            "joblib_version": joblib.__version__,
            "numpy_version": np.__version__
        }
    }

    meta_path = ARTIFACTS_DIR / "url_model_metadata.json"
    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)
    print(f"Saved Metadata payload: {meta_path}")

    print("\nURL Risk Pipeline Execution Complete!")
    return metadata


if __name__ == "__main__":
    run_pipeline(sample_size=80000)
