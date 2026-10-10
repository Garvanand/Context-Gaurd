# ContextGuard Connection Setup & Network Topology Guide

**Document Version:** 1.0.0  
**Target Milestone:** Phase 6 Real-Time Integration Milestone  
**Classification:** Network Configuration & Deployment Standard  

---

## 1. Supported Connection Profiles

ContextGuard provides two distinct, first-class connection profiles:

| Profile Attribute | `LOCAL_LAN_DEMO` (Viva / Lab Workstation) | `REMOTE_SERVER` (Production / Staging) |
| :--- | :--- | :--- |
| **Primary Use Case** | Oral viva demonstration, local lab testing, evaluation benchmarks | Production deployment, remote supervision |
| **Backend Host Binding** | `0.0.0.0` (All interfaces) | Public reverse-proxy / container |
| **Transport Protocol** | HTTP / WS (Development cleartext restricted to local LAN) | HTTPS / WSS strictly enforced |
| **Emulator Target** | `http://10.0.2.2:8000` (Emulator host loopback) | `https://api.contextguard.org` |
| **Physical Phone Target**| Laptop's LAN IPv4 (e.g. `http://192.168.1.50:8000`) | `https://api.contextguard.org` |
| **Supervisor Dashboard**| `http://localhost:5173` | `https://supervisor.contextguard.org` |
| **Zero Egress Offline** | Enforced when user selects `OFFLINE` mode | Enforced when user selects `OFFLINE` mode |

---

## 2. Profile A: LOCAL_LAN_DEMO Setup (Laptop + Phone)

### 2.1. Crucial Networking Concept: `localhost` on Mobile
> [!IMPORTANT]
> When testing on a physical Android phone, `localhost` or `127.0.0.1` refers **strictly to the phone itself**, NOT your laptop!
> - If you enter `127.0.0.1` on the phone, connections fail immediately.
> - On the standard **Android Emulator**, use `10.0.2.2`, which is the special alias provided by QEMU to reach the development machine's `127.0.0.1`.
> - On a **physical phone**, you must connect both laptop and phone to the same Wi-Fi network and enter the laptop's LAN IPv4 address (e.g., `192.168.1.50`).

### 2.2. Discovering Your Laptop's LAN Address (Windows)
Open PowerShell and run:
```powershell
ipconfig
```
Locate your active Wi-Fi adapter:
```text
Wireless LAN adapter Wi-Fi:
   IPv4 Address. . . . . . . . . . . : 192.168.1.50
   Subnet Mask . . . . . . . . . . . : 255.255.255.0
   Default Gateway . . . . . . . . . : 192.168.1.1
```
Your laptop LAN IP is `192.168.1.50`.

### 2.3. Windows Firewall Configuration
To permit the physical Android phone to reach the FastAPI backend on port 8000, ensure inbound port 8000 is allowed:
```powershell
# Run in Administrative PowerShell:
New-NetFirewallRule -DisplayName "ContextGuard Relay Port 8000" -Direction Inbound -LocalPort 8000 -Protocol TCP -Action Allow -Profile Private
```

### 2.4. Starting the Services

1. **Start FastAPI Relay (bound to `0.0.0.0`):**
   ```powershell
   cd backend
   python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
   ```
2. **Start Supervisor Web Dashboard:**
   ```powershell
   cd supervisor-dashboard
   npm run dev -- --host
   ```
   Dashboard opens at `http://localhost:5173`.

### 2.5. Configuring the Android Client
1. Open ContextGuard on Android.
2. Navigate to **System Settings**.
3. Under **Local Backend Endpoint**:
   - For **Android Emulator**: Set Host to `10.0.2.2`, Port to `8000`.
   - For **Physical Phone**: Set Host to your laptop's LAN IP (e.g. `192.168.1.50`), Port to `8000`.
4. Tap **Save Configuration**.

---

## 3. Profile B: REMOTE_SERVER Setup

1. **Backend Origin:** Configure an HTTPS/WSS origin (e.g. `https://relay.contextguard.org`).
2. **Android Network Security:** The app automatically enforces `network_security_config.xml`, which mandates TLS/HTTPS for all remote domains and prevents cleartext transmission.
3. **WebSockets:** Reverse proxy (e.g. Nginx or Cloudflare) must support WebSocket upgrade headers (`Upgrade: websocket`, `Connection: Upgrade`).

---

## 4. End-to-End Diagnostic Path Verification

### Path 1: Mobile Telemetry to Dashboard
```
Android Event Trigger
   ↓
POST /api/v1/relay/events/ingest (or WS frame)
   ↓
FastAPI Relay validates privacy invariant -> Inserts SQLite
   ↓
WebSocket fan-out to Supervisor Dashboard
   ↓
Dashboard Live Telemetry Feed updates instantly
```

### Path 2: Dashboard Command to Mobile
```
Supervisor selects "Run Synthetic Demo"
   ↓
POST /api/v1/relay/commands/queue
   ↓
Relay pushes COMMAND_DELIVER frame over WebSocket
   ↓
Android executes synthetic test -> Returns COMMAND_ACK frame
   ↓
Relay updates command status to SUCCEEDED
   ↓
Dashboard Command Center renders real returned result
```
