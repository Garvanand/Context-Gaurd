# ContextGuard Relay Security & Privacy Assurance Architecture

**Document Version:** 1.0.0  
**Target Milestone:** Phase 6 Real-Time Integration Milestone  
**Classification:** Security Architecture & Privacy Guarantee Specification  

---

## 1. Relay Threat Model & Attack Surface

The introduction of the real-time Mobile $\leftrightarrow$ Web Relay establishes an external synchronization bridge across the network. This document formalizes the threat vectors analyzed and the defensive controls implemented:

```
┌────────────────────────────────────────────────────────┐
│ UNTRUSTED ZONE: Local Wi-Fi / Transit Network          │
│  - Eavesdropping on local LAN packets                   │
│  - Unauthorized connection attempts to port 8000       │
│  - Cross-device event subscription spoofing            │
│  - Malicious command injection                         │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ DEFENSIVE RELAY SECURITY CONTROLS                      │
│  - 1. Scoped Bearer Credentials (devtok_ / dshtok_)    │
│  - 2. One-Way Cryptographic Token Hashing (SHA-256)    │
│  - 3. Strict Command Type Allowlist (No Remote Shell)  │
│  - 4. Deep Privacy Inspection & Raw Artifact Rejection │
│  - 5. Single-Use Pairing Handshake with Mobile Approval│
│  - 6. Tamper-Evident Audit Logging Ledger              │
└────────────────────────────────────────────────────────┘
```

---

## 2. In-Scope Threats & Mitigations

### 2.1. Threat: Unauthenticated Telemetry Ingestion or Eavesdropping
- **Risk:** An unauthorized browser or script connects to the relay and views sensitive user activity or injects fake events.
- **ContextGuard Mitigation:**
  - Every REST and WebSocket call is authenticated using separate scoped Bearer tokens: `device_token` for the phone, `dashboard_token` for the supervisor.
  - WebSocket handshakes require an immediate `AUTH` message within 5 seconds; unauthenticated connections are closed with code 1008.
  - Client subscription scoping: Dashboards cannot subscribe to arbitrary device IDs; events are delivered only for the paired device.

### 2.2. Threat: Arbitrary Remote Command Execution
- **Risk:** An adversary compromises the dashboard and attempts to execute malicious shell commands, install apps, read files, or remotely click payment controls on the phone.
- **ContextGuard Mitigation:**
  - **No Remote Shell:** Arbitrary code execution or remote shell execution is strictly prohibited by design.
  - **Immutable Command Allowlist:** The Android client only parses and executes 4 explicit pre-defined command types:
    1. `REQUEST_STATUS` (Gathers memory, battery, and monitoring active state)
    2. `REQUEST_DIAGNOSTICS` (Reports latency, heap usage, and ML model readiness)
    3. `RUN_SYNTHETIC_DEMO` (Executes a built-in pre-canned benchmark scenario; never touches screen pixels)
    4. `REQUEST_CONFIG_REFRESH` (Reloads local allowlist rules)
  - Any unknown or unauthorized command is rejected immediately with status `FAILED`.

### 2.3. Threat: Silent Data Exfiltration of Raw Screenshots or Passwords
- **Risk:** A compromised mobile process attempts to upload entire screenshots, OTPs, or passwords to the supervisor dashboard.
- **ContextGuard Mitigation:**
  - **On-Device In-Memory Redaction:** Faces and sensitive PII are blacked out or blurred in volatile RAM before network serialization.
  - **Backend Privacy Invariant Enforcement:** `validate_event_privacy` inspects every incoming payload. Payloads containing keys such as `screenshot`, `raw_image`, `raw_ocr`, `password`, `otp` or unmasked 16-digit payment card numbers are **rejected immediately with HTTP 400**.
  - **Zero Raw Persistence:** The relay event ledger only stores metadata, summarized evidence strings, and mathematical risk scores ($\rho, s, r, c$).

### 2.4. Threat: Pairing Session Replay & Brute Force
- **Risk:** An adversary intercepts an expired pairing code or attempts a brute-force scan of 6-character codes.
- **ContextGuard Mitigation:**
  - Pairing codes have a strict 5-minute (300s) time-to-live.
  - Single-use claim constraint: Once claimed, a code is invalidated immediately.
  - Entering the code does NOT grant access; the mobile phone user must physically confirm the connection via an interactive prompt.

---

## 3. Cryptographic Storage & Revocation

1. **Token Hashing:**
   - Raw tokens (`devtok_<32_urlsafe_bytes>` and `dshtok_<32_urlsafe_bytes>`) are generated using `secrets.token_urlsafe(32)`.
   - The backend computes `hashlib.sha256(raw_token.encode()).hexdigest()` and stores only the resulting 64-character hex hash.
   - Database theft does not expose usable credentials.
2. **Instant Revocation:**
   - Either the supervisor or mobile user can invoke `/pairing/revoke` or tap "Unpair Device".
   - Revocation updates `revoked_at` in the database, invalidating the session immediately.
   - Any active WebSocket for that device is closed and disconnected.
