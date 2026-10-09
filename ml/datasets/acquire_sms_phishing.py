"""
Dataset Acquisition and Ingestion for SMS Phishing and Benign Mobile Messages.

Primary Sources:
1. NCSU SMS Phishing Data Releases (WSPR Lab, North Carolina State University)
   - Repository: https://github.com/wspr-ncsu/sms-phishing
   - Citation: Aleksandr Nahapetyan, Sathvik Prasad, Kevin Childs, Adam Oest, Yeganeh Ladwig
     "On SMS Phishing Tactics and Infrastructure"
   - License: MIT License (Copyright (c) 2023 wspr-ncsu)
   - Permitted Usage: Research, modification, and redistribution with copyright notice.

2. UCI Machine Learning Repository SMS Spam Collection
   - Source: Almeida & Hidalgo (2012)
   - Permitted Usage: Open research benchmark, CC-BY / public academic domain.
   - Used for ground-truth benign mobile communication messages (ham).

Output:
- ml/datasets/sms_phishing/sms_phishing_dataset.csv
- ml/datasets/sms_phishing/METADATA.json
- ml/datasets/sms_phishing/LICENSE_NCSU.txt
"""

import os
import re
import json
import urllib.request
from pathlib import Path
import pandas as pd

SMS_DATASET_DIR = Path("ml/datasets/sms_phishing")
SMS_DATASET_CSV = SMS_DATASET_DIR / "sms_phishing_dataset.csv"
METADATA_JSON = SMS_DATASET_DIR / "METADATA.json"
LICENSE_TXT = SMS_DATASET_DIR / "LICENSE_NCSU.txt"

NCSU_RAW_URL = "https://raw.githubusercontent.com/wspr-ncsu/sms-phishing/master/phishing_messages.csv"
NCSU_LICENSE_URL = "https://raw.githubusercontent.com/wspr-ncsu/sms-phishing/master/LICENSE"
UCI_HAM_URL = "https://raw.githubusercontent.com/justmarkham/DAT8/master/data/sms.tsv"


def acquire_sms_phishing_dataset(force_download: bool = False) -> Path:
    SMS_DATASET_DIR.mkdir(parents=True, exist_ok=True)

    if SMS_DATASET_CSV.exists() and not force_download:
        print(f"SMS Phishing dataset already exists at {SMS_DATASET_CSV}")
        return SMS_DATASET_CSV

    print("Fetching NCSU SMS Phishing dataset and license...")
    # 1. Download License
    urllib.request.urlretrieve(NCSU_LICENSE_URL, LICENSE_TXT)

    # 2. Download NCSU phishing messages
    raw_ncsu_path = SMS_DATASET_DIR / "ncsu_phishing_raw.csv"
    if not raw_ncsu_path.exists():
        urllib.request.urlretrieve(NCSU_RAW_URL, raw_ncsu_path)

    # Parse raw NCSU lines robustly (accounting for unquoted commas in message payloads)
    phishing_records = []
    with open(raw_ncsu_path, "r", encoding="utf-8", errors="replace") as f:
        _ = f.readline()  # Skip header
        for line in f:
            line = line.strip()
            if not line:
                continue
            parts = line.split(",")
            if len(parts) >= 6:
                # destination is parts[2], time is parts[-2], error is parts[-1]
                time_val = None
                try:
                    time_val = float(parts[-2])
                except ValueError:
                    time_val = None

                # Text is between destination (parts[2]) and time/sender
                raw_text = ",".join(parts[3:-2]).strip('"').strip()
                # Clean trailing sender artifact if present
                if '",' in raw_text:
                    raw_text = raw_text.split('",')[0]
                elif ", " in raw_text and ("Google" in raw_text or "VIP Club" in raw_text or "Telegram" in raw_text):
                    raw_text = raw_text.rsplit(",", 1)[0].strip()

                # Ensure message is predominantly Latin/ASCII script
                ascii_ratio = sum(1 for c in raw_text if ord(c) < 128) / max(1, len(raw_text))
                if len(raw_text) >= 10 and ascii_ratio >= 0.85 and not re.match(r"^\+?\d+$", raw_text):
                    phishing_records.append({
                        "text": raw_text,
                        "timestamp": time_val,
                        "label": 1,
                        "source": "NCSU_WSPR_SMS_Phishing_v2023"
                    })

    df_phish = pd.DataFrame(phishing_records).drop_duplicates(subset=["text"])
    print(f"Extracted {len(df_phish)} unique real phishing messages from NCSU dataset.")

    # 3. Download Benign Ham messages
    raw_uci_path = SMS_DATASET_DIR / "uci_sms_raw.tsv"
    if not raw_uci_path.exists():
        urllib.request.urlretrieve(UCI_HAM_URL, raw_uci_path)

    df_uci = pd.read_csv(raw_uci_path, sep="\t", header=None, names=["raw_label", "message"])
    df_ham = df_uci[df_uci["raw_label"] == "ham"].copy()
    df_ham_clean = pd.DataFrame()
    df_ham_clean["text"] = df_ham["message"].dropna().astype(str).str.strip()
    df_ham_clean["timestamp"] = None
    df_ham_clean["label"] = 0
    df_ham_clean["source"] = "UCI_SMS_Collection_Ham"
    df_ham_clean = df_ham_clean[df_ham_clean["text"].str.len() >= 5].drop_duplicates(subset=["text"])
    print(f"Extracted {len(df_ham_clean)} unique benign messages from UCI dataset.")

    # 4. Balanced sampling for clean training
    # Sample balanced set: all ~4,500 benign messages and ~4,500 phishing messages
    sample_phish_count = min(len(df_phish), int(len(df_ham_clean) * 1.2))
    df_phish_sample = df_phish.sample(n=sample_phish_count, random_state=42)

    df_combined = pd.concat([df_ham_clean, df_phish_sample], ignore_index=True)
    df_combined = df_combined.sample(frac=1.0, random_state=42).reset_index(drop=True)

    df_combined.to_csv(SMS_DATASET_CSV, index=False)
    print(f"Saved consolidated SMS dataset to {SMS_DATASET_CSV} ({len(df_combined)} rows).")

    # 5. Metadata
    metadata = {
        "dataset_name": "Consolidated SMS Phishing & Benign Mobile Messages Dataset",
        "created_at": "2026-10-10",
        "total_samples": len(df_combined),
        "benign_samples": int((df_combined["label"] == 0).sum()),
        "phishing_samples": int((df_combined["label"] == 1).sum()),
        "sources": [
            {
                "name": "NCSU SMS Phishing Data Releases",
                "repository": "https://github.com/wspr-ncsu/sms-phishing",
                "license": "MIT License (Copyright (c) 2023 wspr-ncsu)",
                "authors": "Nahapetyan et al. (WSPR Lab, NC State University)",
                "phishing_extracted_count": int(sample_phish_count)
            },
            {
                "name": "UCI SMS Spam Collection (Benign Partition)",
                "repository": "https://archive.ics.uci.edu/dataset/228/sms+spam+collection",
                "license": "Creative Commons Attribution 4.0 International (CC BY 4.0)",
                "authors": "Almeida & Hidalgo (2012)",
                "benign_count": int(len(df_ham_clean))
            }
        ]
    }
    with open(METADATA_JSON, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)

    return SMS_DATASET_CSV


if __name__ == "__main__":
    acquire_sms_phishing_dataset(force_download=True)
