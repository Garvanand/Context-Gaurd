# ContextGuard Privacy Protocol & Guarantees

Privacy is a primary architectural pillar of ContextGuard, not an afterthought. This document formalizes what data is processed, what data leaves the client device, and the technical protections in place.

---

## 1. Core Privacy Invariants

1. **Zero Raw Persistence:** The Android client does not write incoming Sharesheet images or intercepted documents to permanent disk storage. All perception and redaction operate entirely in volatile RAM buffers.
2. **On-Device Edge Perception:** Sensitive PII scanning, Face Detection, and OCR are conducted on-device using Google ML Kit and localized pattern extractors before any external network socket is opened.
3. **No Unredacted Uploads in Production:** In the standard `REDACTED_LOCAL_BACKEND` mode, detected account numbers, phone numbers, email addresses, and human faces are visually and textually masked prior to HTTP transmission.
4. **Cryptographic Telemetry:** Audit trails and logs record only the `SHA-256` digest of an artifact. Raw images and plaintexts are never persisted to disk or emitted in telemetry streams.
5. **No Third-Party Paid Cloud APIs:** ContextGuard is designed to operate with a fully local backend on the user's workstation. No sensitive data is transferred to proprietary cloud APIs (e.g. OpenAI, Anthropic).

---

## 2. Data Flow by Operational Mode

| Operational Mode | Outbound Network Traffic | Data Transmitted | Privacy Assurance |
| :--- | :--- | :--- | :--- |
| **1. OFFLINE** | **0 Bytes** (Air-gapped) | None. All heuristics, OCR parsing, and policy decisions run locally on the Android device. | Absolute privacy; zero external exposure. |
| **2. REDACTED_LOCAL_BACKEND** (Default) | HTTP POST to local host (`http://10.0.2.2:8000` or local LAN IP) | - Visually redacted bitmap (faces and account fields blacked out)<br>- Redacted OCR text tokens (`[ACCOUNT_REDACTED]`, `[PHONE_REDACTED]`)<br>- Action metadata (`source_app`, `intended_action`, `destination_type`) | High privacy; sensitive entities sanitized on-device prior to network transmission. |
| **3. RAW_EVALUATION_ONLY** | HTTP POST to evaluation test harness | Consented synthetic benchmark artifact pairs (EARB dataset only). | Dedicated to academic ablation studies and baseline evaluation; blocked for normal user files. |

---

## 3. On-Device Redaction Pipeline

```text
[Raw Input Bitmap]
       │
       ├──► [ML Kit Face Detection] ──────► Face Bounding Boxes
       ├──► [ML Kit Text Recognition] ───► Text Blocks & Coordinates
       └──► [Local Regex Engine] ────────► PII Token Spans
                                                   │
                                                   ▼
                                       [In-Memory Masking Engine]
                                       - Draw black rects over face regions
                                       - Draw solid masks over PII text boxes
                                       - Tokenize strings: "Acct 4321..." -> "[REDACTED_ACCT]"
                                                   │
                                                   ▼
                                       [Sanitized Redacted Bitmap + Text]
```

### Supported On-Device PII Patterns
- Bank Account Numbers (IBAN, Indian Account Number heuristics)
- Payment Identifiers & UPI IDs (`user@upi`, `name@okhdfcbank`)
- Synthetic Identification Numbers (Masked Aadhaar, SSN patterns)
- Contact Numbers & Email Addresses
- Human Facial Regions (ML Kit Vision Contours)

---

## 4. Transparent Network Request Auditor
The Android application includes a dedicated **Network Audit Log** screen accessible directly from the UI. For every inference event, the user can inspect:
- Destination URI and port
- Payload size in bytes
- Side-by-side comparison of the Original vs. Redacted payload
- Cryptographic SHA-256 fingerprint
- Transmission timestamp and roundtrip latency
