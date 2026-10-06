"""
Synthetic Test Artifacts Generator and Inference Evaluation Suite.

Generates 8 academic synthetic test artifacts across defined risk archetypes:
1. benign photo
2. bank statement
3. phishing bank screenshot
4. OTP screenshot
5. suspicious KYC message
6. ordinary document
7. sensitive document
8. QR/payment-like artifact

Runs multimodal inference through VisionReasoner and records only structured
evaluations and SHA-256 hashes.
"""

import io
import json
import hashlib
import asyncio
from pathlib import Path
from typing import Dict, Any, List, Tuple
from PIL import Image, ImageDraw

from backend.models.base import ImageNormalizer
from backend.models.qwen_vision import QwenVisionReasoner

OUTPUT_DIR = Path("artifacts/evaluations")
OUTPUT_FILE = OUTPUT_DIR / "synthetic_test_outputs.json"


def sha256_digest(b: bytes) -> str:
    return hashlib.sha256(b).hexdigest()


def create_benign_photo() -> Tuple[bytes, str, Dict[str, Any]]:
    img = Image.new("RGB", (400, 300), color=(135, 206, 235))  # Sky blue
    draw = ImageDraw.Draw(img)
    draw.polygon([(0, 300), (150, 150), (300, 300)], fill=(34, 139, 34))  # Green hill
    draw.polygon([(180, 300), (300, 120), (400, 300)], fill=(46, 139, 87))  # Hill 2
    draw.ellipse([300, 40, 360, 100], fill=(255, 223, 0))  # Sun
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    raw = buf.getvalue()
    context = {
        "source_app": "GalleryApp",
        "intended_action": "Share vacation photo with family",
        "destination": "Family WhatsApp Group",
        "recipient": "Family Members"
    }
    return raw, "Scenic vacation mountain landscape photo", context


def create_bank_statement() -> Tuple[bytes, str, Dict[str, Any]]:
    img = Image.new("RGB", (500, 400), color=(255, 255, 255))
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, 500, 50], fill=(20, 40, 80))
    draw.text((20, 18), "NATIONAL RESERVE BANK - ACCOUNT STATEMENT", fill=(255, 255, 255))
    draw.text((20, 70), "Account Holder: John Doe", fill=(0, 0, 0))
    draw.text((20, 95), "Account Number: 4092-XXXX-8921", fill=(0, 0, 0))
    draw.text((20, 120), "Opening Balance: $14,250.00", fill=(0, 0, 0))
    draw.rectangle([20, 150, 480, 350], outline=(180, 180, 180), width=1)
    draw.text((30, 165), "Date        Description               Amount     Balance", fill=(60, 60, 60))
    draw.text((30, 195), "Oct 01      Salary Credit            +$4,500.00  $18,750.00", fill=(0, 100, 0))
    draw.text((30, 225), "Oct 03      Mortgage Payment         -$1,800.00  $16,950.00", fill=(100, 0, 0))
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    raw = buf.getvalue()
    context = {
        "source_app": "BankingApp",
        "intended_action": "Broadcast financial statement on Twitter timeline",
        "destination": "Public Twitter/X Feed",
        "recipient": "Public Followers"
    }
    ocr = "NATIONAL RESERVE BANK ACCOUNT STATEMENT Account Holder John Doe Account Number 4092-XXXX-8921 Balance $16,950.00"
    return raw, ocr, context


def create_phishing_bank_screenshot() -> Tuple[bytes, str, Dict[str, Any]]:
    img = Image.new("RGB", (450, 320), color=(255, 240, 240))
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, 450, 45], fill=(180, 20, 20))
    draw.text((15, 15), "SECURITY ALERT: ACCOUNT SUSPENDED", fill=(255, 255, 255))
    draw.text((20, 65), "Immediate Action Required!", fill=(180, 0, 0))
    draw.text((20, 95), "Your online banking access has been temporarily locked", fill=(30, 30, 30))
    draw.text((20, 120), "due to unauthorized sign-in attempts.", fill=(30, 30, 30))
    draw.text((20, 160), "Restore access immediately via secure link:", fill=(30, 30, 30))
    draw.rectangle([20, 190, 430, 230], fill=(220, 220, 240), outline=(0, 0, 180))
    draw.text((30, 202), "http://secure-online-login.xyz/verify?id=99", fill=(0, 0, 180))
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    raw = buf.getvalue()
    context = {
        "source_app": "Browser",
        "intended_action": "Open link and enter login credentials",
        "destination": "Phishing Landing Page",
        "recipient": "Untrusted External Server"
    }
    ocr = "SECURITY ALERT ACCOUNT SUSPENDED Immediate Action Required Restore access: http://secure-online-login.xyz/verify?id=99"
    return raw, ocr, context


def create_otp_screenshot() -> Tuple[bytes, str, Dict[str, Any]]:
    img = Image.new("RGB", (420, 250), color=(245, 245, 248))
    draw = ImageDraw.Draw(img)
    draw.rectangle([15, 15, 405, 235], fill=(255, 255, 255), outline=(200, 200, 200))
    draw.text((30, 30), "SMS Notification - BankAlert", fill=(100, 100, 100))
    draw.text((30, 65), "Your One-Time Password (OTP) is:", fill=(20, 20, 20))
    draw.text((30, 95), "8 4 9 2 0 1", fill=(0, 50, 180))
    draw.text((30, 140), "Valid for 10 minutes. NEVER share this OTP", fill=(180, 0, 0))
    draw.text((30, 165), "with anyone, including bank representatives.", fill=(180, 0, 0))
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    raw = buf.getvalue()
    context = {
        "source_app": "MessagingApp",
        "intended_action": "Send OTP screenshot to unknown caller on WhatsApp",
        "destination": "Chat Direct Message",
        "recipient": "Unknown Caller claiming to be Bank Support"
    }
    ocr = "Your One-Time Password (OTP) is: 849201. NEVER share this OTP with anyone, including bank representatives."
    return raw, ocr, context


def create_suspicious_kyc_message() -> Tuple[bytes, str, Dict[str, Any]]:
    img = Image.new("RGB", (440, 260), color=(250, 250, 250))
    draw = ImageDraw.Draw(img)
    draw.rectangle([15, 15, 425, 245], fill=(255, 255, 255), outline=(210, 210, 210))
    draw.text((25, 25), "URGENT NOTICE: KYC UPDATE PENDING", fill=(190, 40, 0))
    draw.text((25, 60), "Dear customer, your PAN verification has expired.", fill=(20, 20, 20))
    draw.text((25, 85), "Complete KYC within 24 hours to prevent account debit freeze.", fill=(20, 20, 20))
    draw.text((25, 125), "Update here: http://kyc-update-portal.top/pan-link", fill=(0, 0, 200))
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    raw = buf.getvalue()
    context = {
        "source_app": "SMSApp",
        "intended_action": "Forward message and upload identity document",
        "destination": "External Web Portal",
        "recipient": "Unknown SMS Sender"
    }
    ocr = "URGENT NOTICE KYC UPDATE PENDING PAN verification expired. Update here: http://kyc-update-portal.top/pan-link"
    return raw, ocr, context


def create_ordinary_document() -> Tuple[bytes, str, Dict[str, Any]]:
    img = Image.new("RGB", (420, 300), color=(255, 255, 255))
    draw = ImageDraw.Draw(img)
    draw.text((30, 30), "ENGINEERING TEAM SYNC - AGENDA", fill=(0, 0, 0))
    draw.text((30, 70), "1. Sprint 24 Progress Review", fill=(40, 40, 40))
    draw.text((30, 100), "2. Database migration schedule", fill=(40, 40, 40))
    draw.text((30, 130), "3. QA test automation updates", fill=(40, 40, 40))
    draw.text((30, 160), "4. Open questions & announcements", fill=(40, 40, 40))
    draw.text((30, 220), "Location: Room 4B / Google Meet", fill=(100, 100, 100))
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    raw = buf.getvalue()
    context = {
        "source_app": "NotesApp",
        "intended_action": "Share meeting notes with internal engineering team",
        "destination": "Internal Slack Channel",
        "recipient": "Core Engineering Team"
    }
    ocr = "ENGINEERING TEAM SYNC AGENDA 1. Sprint 24 Progress Review 2. Database migration schedule"
    return raw, ocr, context


def create_sensitive_document() -> Tuple[bytes, str, Dict[str, Any]]:
    img = Image.new("RGB", (460, 320), color=(255, 255, 255))
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, 460, 40], fill=(40, 40, 40))
    draw.text((20, 12), "STRICTLY CONFIDENTIAL - EXECUTIVE CONTRACT", fill=(255, 255, 255))
    draw.text((25, 60), "Position: Senior Director of Engineering", fill=(0, 0, 0))
    draw.text((25, 90), "Base Salary: $280,000 / year + Equity Grant", fill=(0, 0, 0))
    draw.text((25, 120), "Non-Disclosure: Binding 3-year IP covenant", fill=(0, 0, 0))
    draw.text((25, 150), "Dispute Resolution: New York Jurisdiction", fill=(0, 0, 0))
    draw.text((25, 220), "[CONFIDENTIAL INTERNAL USE ONLY]", fill=(180, 0, 0))
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    raw = buf.getvalue()
    context = {
        "source_app": "FilesApp",
        "intended_action": "Save contract privately to encrypted personal vault",
        "destination": "Personal Encrypted Drive",
        "recipient": "Self"
    }
    ocr = "STRICTLY CONFIDENTIAL EXECUTIVE CONTRACT Position Senior Director Base Salary $280,000 / year"
    return raw, ocr, context


def create_qr_payment_artifact() -> Tuple[bytes, str, Dict[str, Any]]:
    img = Image.new("RGB", (380, 380), color=(255, 255, 255))
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, 380, 50], fill=(0, 110, 200))
    draw.text((20, 16), "DIGITAL PAYMENTS - INSTANT TRANSFER", fill=(255, 255, 255))
    
    # Simulate stylized QR pattern
    draw.rectangle([70, 80, 310, 300], fill=(245, 245, 245), outline=(0, 0, 0), width=2)
    draw.rectangle([90, 100, 150, 160], fill=(0, 0, 0))
    draw.rectangle([105, 115, 135, 145], fill=(255, 255, 255))
    draw.rectangle([230, 100, 290, 160], fill=(0, 0, 0))
    draw.rectangle([245, 115, 275, 145], fill=(255, 255, 255))
    draw.rectangle([90, 220, 150, 280], fill=(0, 0, 0))
    draw.rectangle([105, 235, 135, 265], fill=(255, 255, 255))
    
    draw.text((80, 320), "Amount: INR 4,500.00 | merchant@upi", fill=(0, 0, 0))
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    raw = buf.getvalue()
    context = {
        "source_app": "UPIPaymentsApp",
        "intended_action": "Scan and authorize payment transfer",
        "destination": "Payment Gateway",
        "recipient": "Unverified Merchant QR"
    }
    ocr = "DIGITAL PAYMENTS INSTANT TRANSFER Amount INR 4500.00 merchant@upi"
    return raw, ocr, context


SYNTHETIC_GENERATORS = [
    ("benign_photo", create_benign_photo),
    ("bank_statement", create_bank_statement),
    ("phishing_bank_screenshot", create_phishing_bank_screenshot),
    ("otp_screenshot", create_otp_screenshot),
    ("suspicious_kyc_message", create_suspicious_kyc_message),
    ("ordinary_document", create_ordinary_document),
    ("sensitive_document", create_sensitive_document),
    ("qr_payment_artifact", create_qr_payment_artifact),
]


async def run_synthetic_evaluations() -> List[Dict[str, Any]]:
    reasoner = QwenVisionReasoner()
    results = []

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    print("=" * 65)
    print("RUNNING MULTIMODAL INFERENCE ON 8 SYNTHETIC ARTIFACTS")
    print("=" * 65)

    for name, gen_fn in SYNTHETIC_GENERATORS:
        raw_bytes, ocr_text, context = gen_fn()
        digest = sha256_digest(raw_bytes)
        
        print(f"\nEvaluating: '{name}' | SHA-256: {digest[:16]}...")
        print(f"  Intended Action: {context['intended_action']}")
        print(f"  Destination: {context['destination']}")

        output = await reasoner.analyze(
            image_bytes=raw_bytes,
            ocr_text=ocr_text,
            context=context,
        )

        record = {
            "artifact_name": name,
            "sha256_digest": digest,
            "byte_size": len(raw_bytes),
            "context": context,
            "structured_output": output.model_dump(),
        }
        results.append(record)

        print(f"  Intervention: {output.recommended_action} | Severity: {output.severity} | Reversibility: {output.reversibility} | Confidence: {output.confidence}")
        print(f"  Reason: {output.reason[:80]}...")

    # Write only structured outputs and hashes (no raw images persisted)
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2)

    print(f"\nEvaluations saved to: {OUTPUT_FILE}")
    return results


if __name__ == "__main__":
    asyncio.run(run_synthetic_evaluations())
