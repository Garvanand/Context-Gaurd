"""
Reproducible Training Pipeline for URL Phishing Risk Detection.

Dataset: PhiUSIIL Phishing URL Dataset (UCI ML Repository, ID: 967)
Models Compared:
- Logistic Regression (Scaled Baseline)
- Random Forest Classifier
- XGBoost Classifier

Selection Criterion: Top Validation F1 & ROC-AUC
Saves:
- ml/artifacts/url_risk_model.joblib
- ml/artifacts/url_model_metadata.json
"""

import os
import sys
import json
import time
import datetime
from pathlib import Path
from typing import Dict, Any, Tuple

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
    classification_report,
)
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

from ml.datasets.acquire_phiusiil import download_dataset
from ml.features.url_features import URLFeatureExtractor


RANDOM_SEED = 42
ARTIFACTS_DIR = Path("ml/artifacts")
MODEL_SAVE_PATH = ARTIFACTS_DIR / "url_risk_model.joblib"
METADATA_SAVE_PATH = ARTIFACTS_DIR / "url_model_metadata.json"


def load_and_preprocess_data(
    sample_size: int = 60000,
    random_seed: int = RANDOM_SEED
) -> Tuple[np.ndarray, np.ndarray, list]:
    """
    Load raw URLs and labels from PhiUSIIL dataset, extract 36 deterministic features,
    and convert labels: phishing = 1, legitimate = 0.
    """
    csv_path = download_dataset()
    print(f"Reading dataset: {csv_path}")

    # Read URL and raw label
    df = pd.read_csv(csv_path, usecols=["URL", "label"])
    df = df.dropna(subset=["URL", "label"])

    # Label conversion: in PhiUSIIL, 0 = phishing, 1 = legitimate
    # Project contract requires: phishing = 1, legitimate = 0
    df["target"] = (df["label"] == 0).astype(int)

    total_available = len(df)
    print(f"Total dataset records: {total_available} (Phishing: {(df['target'] == 1).sum()}, Legitimate: {(df['target'] == 0).sum()})")

    if sample_size and sample_size < total_available:
        print(f"Sampling {sample_size} records using stratified sampling (random_state={random_seed})...")
        sampled_indices, _ = train_test_split(
            df.index,
            train_size=sample_size,
            stratify=df["target"],
            random_state=random_seed
        )
        df_sample = df.loc[sampled_indices].reset_index(drop=True)
    else:
        df_sample = df

    print(f"Extracting 36 deterministic lexical/structural features for {len(df_sample)} URLs...")
    start_t = time.time()
    feature_names = URLFeatureExtractor.FEATURE_NAMES
    
    # Extract features in batches
    urls = df_sample["URL"].tolist()
    X = np.array([URLFeatureExtractor.extract_vector(u) for u in urls], dtype=np.float32)
    y = df_sample["target"].to_numpy(dtype=np.int32)
    
    elapsed = time.time() - start_t
    print(f"Feature extraction completed in {elapsed:.2f}s ({len(urls)/elapsed:.1f} URLs/sec). Shape: {X.shape}")
    
    return X, y, feature_names


def evaluate_model(model, X_eval: np.ndarray, y_eval: np.ndarray) -> Dict[str, Any]:
    """
    Compute comprehensive classification metrics.
    """
    t0 = time.time()
    y_pred = model.predict(X_eval)
    
    if hasattr(model, "predict_proba"):
        y_proba = model.predict_proba(X_eval)[:, 1]
    elif hasattr(model, "decision_function"):
        scores = model.decision_function(X_eval)
        y_proba = 1.0 / (1.0 + np.exp(-scores))
    else:
        y_proba = y_pred.astype(float)
        
    latency_ms = (time.time() - t0) * 1000.0 / len(X_eval)

    acc = float(accuracy_score(y_eval, y_pred))
    prec = float(precision_score(y_eval, y_pred, zero_division=0))
    rec = float(recall_score(y_eval, y_pred, zero_division=0))
    f1 = float(f1_score(y_eval, y_pred, zero_division=0))
    roc_auc = float(roc_auc_score(y_eval, y_proba))
    pr_auc = float(average_precision_score(y_eval, y_proba))

    cm = confusion_matrix(y_eval, y_pred).tolist()
    tn, fp, fn, tp = int(cm[0][0]), int(cm[0][1]), int(cm[1][0]), int(cm[1][1])

    # Per-class metrics
    legit_prec = float(precision_score(y_eval, y_pred, pos_label=0, zero_division=0))
    legit_rec = float(recall_score(y_eval, y_pred, pos_label=0, zero_division=0))
    legit_f1 = float(f1_score(y_eval, y_pred, pos_label=0, zero_division=0))

    return {
        "accuracy": round(acc, 4),
        "precision": round(prec, 4),
        "recall": round(rec, 4),
        "f1": round(f1, 4),
        "roc_auc": round(roc_auc, 4),
        "pr_auc": round(pr_auc, 4),
        "confusion_matrix": {
            "tn": tn, "fp": fp, "fn": fn, "tp": tp
        },
        "per_class": {
            "legitimate_0": {
                "precision": round(legit_prec, 4),
                "recall": round(legit_rec, 4),
                "f1": round(legit_f1, 4),
            },
            "phishing_1": {
                "precision": round(prec, 4),
                "recall": round(rec, 4),
                "f1": round(f1, 4),
            }
        },
        "inference_latency_ms_per_sample": round(latency_ms, 4)
    }


def train_and_evaluate(sample_size: int = 60000):
    ARTIFACTS_DIR.mkdir(parents=True, exist_ok=True)
    start_time_iso = datetime.datetime.now(datetime.timezone.utc).isoformat()

    X, y, feature_names = load_and_preprocess_data(sample_size=sample_size, random_seed=RANDOM_SEED)

    # Stratified Train (70%) / Temp (30%)
    X_train, X_temp, y_train, y_temp = train_test_split(
        X, y, test_size=0.30, random_state=RANDOM_SEED, stratify=y
    )
    # Split Temp into Validation (15%) and Test (15%)
    X_val, X_test, y_val, y_test = train_test_split(
        X_temp, y_temp, test_size=0.50, random_state=RANDOM_SEED, stratify=y_temp
    )

    print(f"\nData Splits: Train={len(X_train)}, Validation={len(X_val)}, Test={len(X_test)}")
    print(f"Class distribution (Train): Phishing={sum(y_train == 1)}, Legitimate={sum(y_train == 0)}")

    # 1. Candidate Models Definition
    models: Dict[str, Any] = {
        "LogisticRegression": Pipeline([
            ("scaler", StandardScaler()),
            ("clf", LogisticRegression(max_iter=1000, random_state=RANDOM_SEED))
        ]),
        "RandomForest": RandomForestClassifier(
            n_estimators=100,
            max_depth=16,
            min_samples_split=5,
            n_jobs=-1,
            random_state=RANDOM_SEED
        ),
        "XGBoost": xgb.XGBClassifier(
            n_estimators=150,
            max_depth=6,
            learning_rate=0.1,
            subsample=0.8,
            colsample_bytree=0.8,
            eval_metric="logloss",
            random_state=RANDOM_SEED,
            n_jobs=-1
        )
    }

    # 2. Train and Validate Candidates
    val_results: Dict[str, Dict[str, Any]] = {}
    print("\n" + "=" * 60)
    print("MODEL COMPARISON (VALIDATION SET)")
    print("=" * 60)

    for name, model in models.items():
        print(f"\nTraining candidate model: {name}...")
        t0 = time.time()
        
        if name == "XGBoost":
            model.fit(
                X_train, y_train,
                eval_set=[(X_val, y_val)],
                verbose=False
            )
        else:
            model.fit(X_train, y_train)
            
        fit_time = time.time() - t0
        metrics = evaluate_model(model, X_val, y_val)
        metrics["training_time_seconds"] = round(fit_time, 2)
        val_results[name] = metrics

        print(f"  {name} | Val ROC-AUC: {metrics['roc_auc']:.4f} | PR-AUC: {metrics['pr_auc']:.4f} | F1: {metrics['f1']:.4f} | Acc: {metrics['accuracy']:.4f} | Time: {fit_time:.2f}s")

    # 3. Model Selection
    # Select candidate with highest Validation F1 score (and ROC-AUC as tie-breaker)
    best_model_name = max(
        val_results.keys(),
        key=lambda k: (val_results[k]["f1"], val_results[k]["roc_auc"])
    )
    best_model = models[best_model_name]
    print("\n" + "=" * 60)
    print(f"SELECTED CHAMPION MODEL: {best_model_name} (Highest Validation F1: {val_results[best_model_name]['f1']:.4f})")
    print("=" * 60)

    # 4. Final Evaluation on Held-Out Test Set
    print(f"\nEvaluating champion ({best_model_name}) on Held-Out Test Split ({len(X_test)} samples)...")
    test_metrics = evaluate_model(best_model, X_test, y_test)
    print(f"Test Accuracy:  {test_metrics['accuracy']:.4f}")
    print(f"Test Precision: {test_metrics['precision']:.4f}")
    print(f"Test Recall:    {test_metrics['recall']:.4f}")
    print(f"Test F1:        {test_metrics['f1']:.4f}")
    print(f"Test ROC-AUC:   {test_metrics['roc_auc']:.4f}")
    print(f"Test PR-AUC:    {test_metrics['pr_auc']:.4f}")
    print(f"Confusion Matrix: TN={test_metrics['confusion_matrix']['tn']}, FP={test_metrics['confusion_matrix']['fp']}, FN={test_metrics['confusion_matrix']['fn']}, TP={test_metrics['confusion_matrix']['tp']}")

    # 5. Save Model Artifact
    print(f"\nSaving model artifact to {MODEL_SAVE_PATH}...")
    joblib.dump(best_model, MODEL_SAVE_PATH)

    # 6. Build Metadata Payload
    metadata = {
        "model_name": best_model_name,
        "model_version": "xgboost-phiusiil-url-v1",
        "dataset": {
            "name": "PhiUSIIL Phishing URL Dataset",
            "source": "UCI Machine Learning Repository",
            "uci_id": 967,
            "total_sampled_records": len(X),
            "label_encoding": {"legitimate": 0, "phishing": 1}
        },
        "feature_list": feature_names,
        "feature_count": len(feature_names),
        "split": {
            "train_samples": len(X_train),
            "validation_samples": len(X_val),
            "test_samples": len(X_test),
            "random_seed": RANDOM_SEED,
            "stratified": True
        },
        "hyperparameters": getattr(best_model, "get_params", lambda: {})(),
        "candidate_comparison_validation": val_results,
        "test_metrics": test_metrics,
        "training_timestamp": start_time_iso,
        "environment": {
            "python_version": sys.version.split()[0],
            "xgboost_version": xgb.__version__,
            "scikit_learn_version": sklearn.__version__,
            "joblib_version": joblib.__version__,
            "numpy_version": np.__version__,
        }
    }

    # Clean non-serializable objects in hyperparameters
    clean_params = {}
    for k, v in metadata["hyperparameters"].items():
        if isinstance(v, (int, float, str, bool, list, dict)) or v is None:
            clean_params[k] = v
        else:
            clean_params[k] = str(v)
    metadata["hyperparameters"] = clean_params

    print(f"Writing metadata to {METADATA_SAVE_PATH}...")
    with open(METADATA_SAVE_PATH, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)

    print("Training pipeline complete.")
    return best_model_name, val_results, test_metrics


if __name__ == "__main__":
    train_and_evaluate(sample_size=60000)
