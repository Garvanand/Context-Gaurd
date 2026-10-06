"""
Dataset acquisition script for the PhiUSIIL Phishing URL Dataset.
Source: UCI Machine Learning Repository (Dataset ID: 967)
URL: https://archive.ics.uci.edu/static/public/967/phiusiil+phishing+url+dataset.zip
"""

import os
import sys
import time
import zipfile
import urllib.request
from pathlib import Path

DATASET_URL = "https://archive.ics.uci.edu/static/public/967/phiusiil+phishing+url+dataset.zip"
TARGET_DIR = Path("ml/datasets")
ZIP_PATH = TARGET_DIR / "phiusiil_dataset.zip"
EXTRACT_DIR = TARGET_DIR / "phiusiil"


def download_dataset(force: bool = False) -> Path:
    TARGET_DIR.mkdir(parents=True, exist_ok=True)
    EXTRACT_DIR.mkdir(parents=True, exist_ok=True)

    # Check if already extracted
    csv_candidates = list(EXTRACT_DIR.glob("*.csv")) + list(TARGET_DIR.glob("*.csv"))
    if csv_candidates and not force:
        print(f"Found existing CSV dataset at: {csv_candidates[0]}")
        return csv_candidates[0]

    if not ZIP_PATH.exists() or force:
        print(f"Downloading PhiUSIIL Phishing URL Dataset from UCI ML Repository: {DATASET_URL}")
        req = urllib.request.Request(DATASET_URL, headers={"User-Agent": "ContextGuard-ML-Pipeline/1.0"})
        start_time = time.time()
        
        with urllib.request.urlopen(req, timeout=30) as response, open(ZIP_PATH, "wb") as out_file:
            downloaded = 0
            while True:
                chunk = response.read(64 * 1024)
                if not chunk:
                    break
                out_file.write(chunk)
                downloaded += len(chunk)
                if downloaded % (2 * 1024 * 1024) < (64 * 1024):
                    print(f"  Downloaded {downloaded / (1024 * 1024):.1f} MB...")
        
        elapsed = time.time() - start_time
        print(f"Download complete: {downloaded / (1024 * 1024):.2f} MB in {elapsed:.1f}s")

    print(f"Extracting zip archive: {ZIP_PATH} -> {EXTRACT_DIR}")
    with zipfile.ZipFile(ZIP_PATH, "r") as zip_ref:
        zip_ref.extractall(EXTRACT_DIR)

    csv_candidates = list(EXTRACT_DIR.glob("*.csv"))
    if not csv_candidates:
        # Check subdirectories
        csv_candidates = list(EXTRACT_DIR.rglob("*.csv"))

    if not csv_candidates:
        raise FileNotFoundError(f"No CSV file found in extracted directory: {EXTRACT_DIR}")

    print(f"Extracted CSV dataset: {csv_candidates[0]} (Size: {csv_candidates[0].stat().st_size / (1024 * 1024):.2f} MB)")
    return csv_candidates[0]


if __name__ == "__main__":
    csv_file = download_dataset()
    print(f"Dataset successfully prepared at: {csv_file}")
