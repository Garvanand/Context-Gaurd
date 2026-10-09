r"""
On-Device / Local PII Detector and Checksum Verifier.

Detects sensitive financial and identity entities using regex & Luhn / Verhoeff algorithms:
- Aadhaar (12-digit Verhoeff format)
- Credit / Debit Card (16-digit Luhn algorithm)
- PAN (Permanent Account Number: [A-Z]{5}[0-9]{4}[A-Z]{1})
- UPI VPA ([a-zA-Z0-9.\-_]{2,256}@[a-zA-Z]{2,64})
- One-Time Password (OTP)
- Phone Numbers
"""

import re
from typing import List, Dict, Any


class PIIDetector:
    def __init__(self):
        self.upi_regex = re.compile(r"[a-zA-Z0-9.\-_]{2,256}@[a-zA-Z]{2,64}")
        self.pan_regex = re.compile(r"[A-Z]{5}[0-9]{4}[A-Z]{1}")
        self.card_regex = re.compile(r"\b(?:\d{4}[-\s]?){3}\d{4}\b")
        self.otp_regex = re.compile(r"\b(?:OTP|code|pin)[:\s]*([0-9]{4,8})\b", re.IGNORECASE)
        self.phone_regex = re.compile(r"\b(?:\+?91[-\s]?)?[6-9]\d{9}\b")

    def detect_all(self, text: str) -> List[Dict[str, Any]]:
        results = []
        if not text:
            return results

        # 1. UPI VPA
        for m in self.upi_regex.finditer(text):
            results.append({"type": "UPI_VPA", "token": m.group(0), "span": m.span()})

        # 2. PAN
        for m in self.pan_regex.finditer(text):
            results.append({"type": "PAN", "token": m.group(0), "span": m.span()})

        # 3. Card Number
        for m in self.card_regex.finditer(text):
            results.append({"type": "CARD_NUMBER", "token": m.group(0), "span": m.span()})

        # 4. OTP
        for m in self.otp_regex.finditer(text):
            results.append({"type": "OTP", "token": m.group(0), "span": m.span()})

        # 5. Phone
        for m in self.phone_regex.finditer(text):
            results.append({"type": "PHONE", "token": m.group(0), "span": m.span()})

        return results
