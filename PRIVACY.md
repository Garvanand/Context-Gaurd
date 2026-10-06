# ContextGuard Privacy Protocol & Technical Guarantees

Privacy is a primary architectural pillar of ContextGuard, not an afterthought. This document formalizes what data is processed, what data leaves the client device, and the technical protections in place.

---

## 1. End-to-End Privacy Pipeline

The ContextGuard privacy pipeline enforces strict serial containment:

```text
Raw artifact
   ↓
On-device perception (Google ML Kit Text Recognition & Face Detection)
   ↓
Sensitive-region detection (Luhn algorithm, contextual OTPs, Aadhaar, accounts, phones, emails)
   ↓
Redaction (In-memory Blackout or Pixelated Blur canvas masking + text token masking)
   ↓
Privacy decision (Offline air-gap check vs. Local workstation backend routing)
   ↓
Redacted artifact only (Raw artifact strictly excluded from payload construction)
   ↓
Local backend (HTTP POST to localhost:8000/api/v1/analyze)
   ↓
Structured result (Grounded decision trace + cryptographic SHA-256 telemetry)
```

---

## 2. Core Privacy Invariants

1. **Zero Raw Persistence:** The Android client does not write incoming Sharesheet images or intercepted documents to permanent disk storage. All perception and redaction operate entirely in volatile RAM buffers.
2. **On-Device Edge Perception:** Sensitive PII scanning, Face Detection, and OCR are conducted on-device using Google ML Kit and localized pattern extractors before any external network socket is opened.
3. **No Unredacted Uploads in Production:** In the standard `LOCAL_BACKEND` mode, detected account numbers, phone numbers, email addresses, OTP codes, and human faces are visually masked (`BLACKOUT` or `BLUR`) and textually replaced with `[REDACTED_...]` tokens prior to HTTP transmission.
4. **Metadata-Only Network Audit Logging:** Audit trails and logs record only metadata:
   - Request ID (UUID)
   - Timestamp (Epoch ms)
   - Endpoint category
   - Payload type
   - Payload size (bytes)
   - Redaction status (`isRedacted = true`)
   - HTTP response status code
   - Latency (ms)
   - Artifact correlation via `SHA-256` digest only.
   **NEVER stored:** Raw artifacts, extracted OCR text, user secrets, or unmasked model prompts.
5. **No Third-Party Paid Cloud APIs:** ContextGuard is designed to operate with a fully local backend on the user's workstation. No sensitive data is transferred to proprietary cloud APIs (e.g. OpenAI, Anthropic).

---

## 3. Data Flow by Network Mode

| Network Mode | Outbound Network Traffic | Data Transmitted | Privacy Assurance |
| :--- | :--- | :--- | :--- |
| **1. OFFLINE** | **0 Bytes** (Air-gapped) | **None.** The application layer strictly disables backend network calls. All heuristics, OCR parsing, and policy decisions run locally on the Android device. | Absolute privacy; zero external exposure. |
| **2. LOCAL_BACKEND** (Default) | HTTP POST to local host (`http://10.0.2.2:8000` or local LAN IP) | - Visually redacted bitmap (faces and account fields blacked out or blurred)<br>- Sanitized OCR text tokens (`[ACCOUNT_REDACTED]`, `[PHONE_REDACTED]`)<br>- Action metadata (`source_app`, `intended_action`, `destination_type`) | High privacy; sensitive entities sanitized on-device prior to network transmission. Raw artifact excluded. |
| **3. RESTRICTED_EVALUATION** | HTTP POST to evaluation test harness | Consented synthetic benchmark artifact pairs (EARB dataset only). | Dedicated to academic ablation studies and baseline evaluation; blocked for normal user files. |

---

## 4. On-Device Redaction Engine

### Supported Visual Styles
- **`BLACKOUT`**: Solid opaque fill drawn over the bounding boxes of sensitive entities and facial contours.
- **`BLUR`**: Fine-grained block pixelation blur over sensitive bounding boxes, obscuring high-frequency text and facial features while preserving general document layout.

### Supported On-Device PII Patterns
- Credit / Debit Cards (13–19 digits validated via Luhn checksum)
- One-Time Passwords (OTPs with bi-directional contextual keyword grounding)
- Bank Account Numbers (9–18 digits with contextual banking keywords)
- Contact Numbers (Indian mobile + international E.164 formats)
- Email Addresses (RFC-compliant regex)
- Aadhaar Numbers (12-digit national identity format)
- Human Facial Regions (ML Kit Vision Contours)
- URLs and Web Domains
- Calendar Dates and Postal PIN / ZIP codes

---

## 5. Transparent Privacy Center UI

The Android application provides a dedicated **Privacy Center** screen accessible directly from the UI displaying:
- Notice: *"Your artifact stays on this device until ContextGuard decides cloud/local-backend reasoning is necessary."*
- On-device detection count (PII count, Face count).
- Number of redacted spatial regions.
- Outbound byte transmission state (e.g. 0 Bytes in Offline mode).
- Active destination channel and intended action.
- Payload type, payload size, and timestamp.
- Cryptographic SHA-256 fingerprint of the current artifact.
- Interactive Network Mode selector (`OFFLINE`, `LOCAL_BACKEND`, `RESTRICTED_EVALUATION`).
- Visual Redaction Style selector (`BLACKOUT`, `BLUR`).
- Live chronological network audit trail (metadata only).
- Interactive Before vs. After Redaction preview in the analysis workflow.
